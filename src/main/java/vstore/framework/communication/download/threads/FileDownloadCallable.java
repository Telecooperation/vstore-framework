package vstore.framework.communication.download.threads;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

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
import vstore.framework.communication.download.events.DownloadStartEvent;
import vstore.framework.communication.download.events.DownloadedFileReadyEvent;
import vstore.framework.error.ErrorMessages;
import vstore.framework.exceptions.VStoreException;
import vstore.framework.file.MetaData;
import vstore.framework.file.VStoreFile;
import vstore.framework.logging.LogHandler;
import vstore.framework.node.NodeInfo;
import vstore.framework.utils.IdentifierUtils;

/**
 * A simple callable for handling the download of a file.
 * Does nothing but downloading the file from the given storage node.
 */
@SuppressWarnings("unused")
public class FileDownloadCallable implements Callable<VStoreFile> {
	
	private final String fileUuid;
	private NodeInfo node;
    private File targetDir;
	private final String requestId;
	private MetaData meta;

	private boolean publishEvents;

	private int lastUploadProgress = 0;

    /**
     * @param fileUuid The identifier of the file to download.
     * @param node The NodeInfo object of the node to download the file from.
     * @param targetDirectory The target directory where to save the data.
	 * @param requestId An identifier of the request.
	 * @param publishEvents Set this to true, if this function should publish events when the status changes.
	 *
	 * @throws Exception in case an error occurred.
     */
    public FileDownloadCallable(String fileUuid, NodeInfo node,
								File targetDirectory, String requestId, boolean publishEvents)
    		throws Exception 
    {
		if(fileUuid == null || fileUuid.equals("") || targetDirectory == null || node == null)
    	{
    		throw new Exception(ErrorMessages.PARAMETERS_MUST_NOT_BE_NULL);
    	}
    	if(requestId == null || requestId.equals("")) 
    	{
    		requestId = "FileDownload";
		}

    	this.fileUuid = fileUuid;
    	this.node = node;
        this.targetDir = targetDirectory;
    	this.requestId = requestId;
    	this.publishEvents = publishEvents;
    }

    @Override
    public VStoreFile call() {
    	//First request the metadata of the file
		requestMetadata();
    	//If something did not work as expected, quit the thread.
    	if(meta == null) 
    	{ 
    		downloadFailed(null);
    		return null;
		}
    	
    	//Add file to the list of current downloads
    	PersistentDownloadList.addFileDownloading(fileUuid);

    	//Log event that we are about to start a download
        LogHandler.logDownloadStart(meta.getUUID(), meta.getFilesize(), 
        		meta.toJSON().toJSONString(), 
        		node.getIdentifier(), meta.getNodeType());

        //Post event that we are about to start a download
        DownloadStartEvent dlStartEvt = new DownloadStartEvent(meta, node);
        EventBus.getDefault().post(dlStartEvt);
        
        //Download the actual file
    	return downloadFile();
    }

    private void requestMetadata() {
        MetadataDownloadCallable callable = new MetadataDownloadCallable(fileUuid, node);
        meta = callable.call();
    }

    private VStoreFile downloadFile() {
    	//Create download progress listener 
    	final ProgressListener progressListener = new ProgressListener() {
    	    @Override
    	    public void update(long bytesRead, long contentLength, boolean done)
    	    {
    	    	if ((meta == null) || (meta.getFilesize() <= 0)) { return; }
                //Calculate percentage
    	    	int progress = (int) ((float) bytesRead / meta.getFilesize() * 100);
                //Do not post event if progress was not significant
    	    	if(progress <= lastUploadProgress) { return; }
    	    	lastUploadProgress = progress;
    	    	//Limit to 100
    	    	if (progress > 100) { progress = 100; }
    	    	//Post event
                DownloadProgressEvent evt =
                      new DownloadProgressEvent(fileUuid, requestId, progress, false, null, meta);
                EventBus.getDefault().post(evt);
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
		        .url(node.getDownloadUri(fileUuid, IdentifierUtils.getDeviceIdentifier()))
		        .build();
		
		try (Response response = client.newCall(request).execute()) 
		{
			return parseResponse(response);
	    } 
		catch (IOException e) 
		{
			downloadFailed(e);
		}
		return null;
	}

	private VStoreFile parseResponse(Response response) throws IOException {
        //Check if we received a wrong http status code
        if (!response.isSuccessful())
        {
            downloadFailed(new IOException("Unexpected response code: " + response));
            return null;
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
            return null;
        }

        if (binFile != null)
        {
            //Sink binary into the file
            File outputFile = new File(targetDir, fileUuid + "." + meta.getFileExtension());
            BufferedSink sink = Okio.buffer(Okio.sink(outputFile));
            sink.writeAll(binFile);
            sink.close();

            System.out.println("vStore: Finished downloading file " + fileUuid);

            //Remove file from download list
            PersistentDownloadList.deleteFileDownloading(fileUuid);
            //Log that the download is done
            LogHandler.logDownloadDone(fileUuid, false);

            VStoreFile downloadedFile = null;
            try
            {
                downloadedFile = new VStoreFile(fileUuid, outputFile, meta);
            }
            catch (VStoreException e)
            {
                downloadFailed(e);
                return null;
            }

            if(!publishEvents) { return downloadedFile; }

            //Publish event about the finished download
            DownloadedFileReadyEvent evt = new DownloadedFileReadyEvent();
            evt.file = downloadedFile;
            evt.requestId = requestId;
            EventBus.getDefault().postSticky(evt);
            return downloadedFile;
        }
        downloadFailed(null);
        return null;
    }
    
    private void downloadFailed(Exception e) {
    	if(e != null) e.printStackTrace();
        PersistentDownloadList.deleteFileDownloading(fileUuid);
        //Log that the download failed
        LogHandler.logDownloadDone(fileUuid, true);

    	if(!publishEvents) { return; }
		EventBus.getDefault().postSticky(new DownloadFailedEvent(fileUuid));
    }
 }