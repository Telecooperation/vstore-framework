package vstore.framework.communication.download.threads;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;
import vstore.framework.communication.download.events.NewThumbnailEvent;
import vstore.framework.communication.download.events.ThumbnailDownloadFailedEvent;
import vstore.framework.error.ErrorMessages;
import vstore.framework.logging.LogHandler;
import vstore.framework.node.NodeInfo;
import vstore.framework.utils.FrameworkUtils;

/**
 * A simple thread for handling the download of a thumbnail of a file.
 */
@SuppressWarnings("unused")
public class ThumbnailDownloadThread extends Thread {

	private final String fileUuid;
	private NodeInfo node;
    private File targetDir;

    /**
     * @param fileUuid The identifier of the file for which a thumbnail should be downloaded.
     * @param node The NodeInfo object of the node to download the file thumbnail from.
     * @param targetDirectory The target directory where to save the data.
     */
    public ThumbnailDownloadThread(String fileUuid, NodeInfo node, File targetDirectory)
    		throws Exception 
    {
		if(fileUuid == null || fileUuid.equals("") || node == null || targetDirectory == null)
    	{
    		throw new Exception(ErrorMessages.PARAMETERS_MUST_NOT_BE_NULL);
    	}
    	this.fileUuid = fileUuid;
    	this.node = node;
        this.targetDir = targetDirectory;

        EventBus.getDefault().register(this);
    }

    @Override
    public void run() {
    	//Download the actual file
    	downloadThumb();
        EventBus.getDefault().unregister(this);
    }
    
    private void downloadThumb() {
    	//Create new HTTP client
		OkHttpClient client = new OkHttpClient();
		
		Request request = new Request.Builder()
		        .url(node.getThumbnailUri(fileUuid, FrameworkUtils.getDeviceIdentifier()))
		        .build();
		
		try (Response response = client.newCall(request).execute()) 
		{
			//Check if we received a wrong http status code
			if (!response.isSuccessful()) 
			{
				thumbDlFailed(new IOException("Unexpected response code: " + response));
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
                thumbDlFailed(new IOException("Empty response body!"));
                return;
            }

			if (binFile != null) 
			{
				//Sink binary into the file
				File outputFile = new File(targetDir, fileUuid+".png");
				BufferedSink sink = Okio.buffer(Okio.sink(outputFile));
				sink.writeAll(binFile);
				sink.close();
		    	
				//Publish event about the finished download
		    	NewThumbnailEvent evt = new NewThumbnailEvent(fileUuid, outputFile);
		    	EventBus.getDefault().postSticky(evt);
            } 
			else 
			{
				thumbDlFailed(null);
            }
	    } 
		catch (IOException e) 
		{
			thumbDlFailed(e);
		}
	}
    
    private void thumbDlFailed(Exception e) {
    	if(e != null) e.printStackTrace();

		EventBus.getDefault().postSticky(new ThumbnailDownloadFailedEvent(fileUuid));
        //Log that the download failed
        LogHandler.logDownloadDone(fileUuid, true);
    }
 }