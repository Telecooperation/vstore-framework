package vstore.framework.communication.download.threads;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;
import vstore.framework.communication.ProgressResponseBody;
import vstore.framework.communication.download.PersistentDownloadList;
import vstore.framework.communication.download.ProgressListener;
import vstore.framework.communication.download.events.DownloadFailedEvent;
import vstore.framework.communication.download.events.DownloadProgressEvent;
import vstore.framework.communication.download.events.DownloadedFileReadyEvent;
import vstore.framework.communication.download.events.MetadataDownloadFailedEvent;
import vstore.framework.communication.download.events.MetadataEvent;
import vstore.framework.error.ErrorMessages;
import vstore.framework.exceptions.VStoreException;
import vstore.framework.file.MetaData;
import vstore.framework.file.VStoreFile;
import vstore.framework.logging.LogHandler;
import vstore.framework.node.NodeInfo;
import vstore.framework.utils.FrameworkUtils;

/**
 * A simple thread for handling the download of a file.
 */
@SuppressWarnings("unused")
public class FileDownloadThread extends Thread {
	
	private final String fileUuid;
	private NodeInfo node;
    private File targetDir;
	private final String requestId;
	private MetaData meta;

    private final Object metaWaitLock;

    /**
     * @param fileUuid The identifier of the file to download.
     * @param node The NodeInfo object of the node to download the file from.
     * @param targetDirectory The target directory where to save the data.
	 * @param requestId An identifier of the request.
     */
    public FileDownloadThread(String fileUuid, NodeInfo node, 
    		File targetDirectory, String requestId) 
    		throws Exception 
    {
		if(fileUuid == null || fileUuid.equals("") || node == null || targetDirectory == null)
    	{
    		throw new Exception(ErrorMessages.PARAMETERS_MUST_NOT_BE_NULL);
    	}
    	if(requestId == null || requestId.equals("")) 
    	{
    		requestId = "FileDownload" + ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE);
		}

    	this.fileUuid = fileUuid;
    	this.node = node;
        this.targetDir = targetDirectory;
    	this.requestId = requestId;
        
    	metaWaitLock = new Object();
        
        EventBus.getDefault().register(this);
    }

    @Override
    public void run() {
    	//First request the metadata of the file
    	MetadataDownloadThread t = new MetadataDownloadThread(fileUuid, node);
    	t.start();
    	
    	//Wait for metadata download to finish
    	synchronized (metaWaitLock) {
    		try {
				metaWaitLock.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
    	
    	//If something did not work as expected, quit the thread.
    	if(meta == null) 
    	{ 
    		downloadFailed(null);
    		return; 
		}
    	
    	//Add file to the list of current downloads
    	PersistentDownloadList.addFileDownloading(fileUuid);
    	
    	//Log event that we are about to start a download
        LogHandler.logDownloadStart(meta.getUUID(), meta.getFilesize(), 
        		meta.toJSON().toJSONString(), 
        		node.getUUID(), meta.getNodeType());
        
        //Download the actual file
    	downloadFile();
        EventBus.getDefault().unregister(this);
    }
    
    private void downloadFile() {
    	//Create download progress listener 
    	final ProgressListener progressListener = new ProgressListener() {
    	    //boolean firstUpdate = true;

    	    @Override
    	    public void update(long bytesRead, long contentLength, boolean done)
    	    {
				if (done)
				{
					System.out.println("completed");
				}
				else
				{
					if (contentLength != -1)
					{
						int progress = (int) ((float) bytesRead / contentLength * 100);
						if (progress > 100) {
							progress = 100;
						}
						DownloadProgressEvent evt =
    		                  new DownloadProgressEvent(fileUuid, requestId, progress, false, null, meta);
						EventBus.getDefault().postSticky(evt);
					}
				}
	    	}
    	};

    	//Create new HTTP client with progress interceptor
		OkHttpClient client = new OkHttpClient.Builder()
	        .addNetworkInterceptor(new Interceptor() 
	        {
	        	@Override public Response intercept(Chain chain) throws IOException 
	        	{
	        		Response originalResponse = chain.proceed(chain.request());
	        		return originalResponse.newBuilder()
	        				.body(new ProgressResponseBody(originalResponse.body(), progressListener))
	        				.build();
	        	}
	        })
	        .build();
		
		Request request = new Request.Builder()
		        .url(node.getDownloadUri(fileUuid, FrameworkUtils.getDeviceIdentifier()))
		        .build();
		
		try (Response response = client.newCall(request).execute()) 
		{
			//Check if we received a wrong http status code
			if (!response.isSuccessful()) 
			{ 
				downloadFailed(new IOException("Unexpected response code: " + response)); 
				return;
			}

            BufferedSource binFile;
            try
            {
                binFile = response.body().source();
            }
            catch(NullPointerException ex)
            {
                //Should happen rarely!
                downloadFailed(new IOException("Empty response body!"));
                return;
            }

			if (binFile != null) 
			{
				//Sink binary into the file
				File outputFile = new File(targetDir, meta.getFilename());
				BufferedSink sink = Okio.buffer(Okio.sink(outputFile));
				sink.writeAll(binFile);
				sink.close();

				//Remove file from download list
		    	PersistentDownloadList.deleteFileDownloading(fileUuid);
		    	
				//Publish event about the finished download
		    	DownloadedFileReadyEvent evt = new DownloadedFileReadyEvent();
		    	try 
		    	{
					evt.file = new VStoreFile(fileUuid, outputFile, meta);
				} 
		    	catch (VStoreException e) 
		    	{
					downloadFailed(e);
					return;
				} 
		    	evt.requestId = requestId;
		    	EventBus.getDefault().postSticky(evt);
                
                //Log that the download is done
                LogHandler.logDownloadDone(fileUuid, false);
            } 
			else 
			{
                downloadFailed(null);
            }
	    } 
		catch (IOException e) 
		{
			downloadFailed(e);
		}
	}
    
    private void downloadFailed(Exception e) {
    	if(e != null) e.printStackTrace();
    	
    	PersistentDownloadList.deleteFileDownloading(fileUuid);
		EventBus.getDefault().postSticky(new DownloadFailedEvent(fileUuid));
        //Log that the download failed
        LogHandler.logDownloadDone(fileUuid, true);
    }
    
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MetadataEvent event) {
    	//Check if file id of the event matches
    	if(!event.getFileUUID().equals(this.fileUuid)) { return; }
    	
    	meta = event.getMetadata();
    	EventBus.getDefault().removeStickyEvent(event);
    	metaWaitLock.notify();
    }
    
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MetadataDownloadFailedEvent event) {
    	//Check if file id of the event matches
    	if(!event.fileId.equals(this.fileUuid)) { return; }
    	
    	meta = null;
    	EventBus.getDefault().removeStickyEvent(event);
    	metaWaitLock.notify();
    }
 }