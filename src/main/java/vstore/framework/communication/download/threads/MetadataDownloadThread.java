package vstore.framework.communication.download.threads;

import org.greenrobot.eventbus.EventBus;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import vstore.framework.communication.download.events.MetadataDownloadFailedEvent;
import vstore.framework.communication.download.events.MetadataEvent;
import vstore.framework.file.MetaData;
import vstore.framework.node.NodeInfo;
import vstore.framework.utils.FrameworkUtils;

public class MetadataDownloadThread extends Thread {
	
	private final String fileUuid;
	private NodeInfo node;
    private final OkHttpClient httpClient; 
    
    public MetadataDownloadThread(String fileUuid, NodeInfo node) {
    	this.fileUuid = fileUuid;
    	this.node = node;
    	
    	httpClient = new OkHttpClient();
    }
    
    @Override
    public void run() {
    	//Request the metadata of the file
    	MetaData meta = downloadMetadata();
    	//If something did not work as expected, quit the thread.
    	if(meta == null) 
    	{ 
    		return; 
		}
    	publishMetadata(meta); 
    }
    
    private MetaData downloadMetadata() {
    	Request request = new Request.Builder()
    	        .url(node.getMetadataUri(fileUuid, FrameworkUtils.getDeviceIdentifier(), true))
    	        .build();
    	
    	try (Response response = httpClient.newCall(request).execute()) 
    	{
    		if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

			String responseBody;
			try
			{
				responseBody = response.body().string();
			}
			catch(NullPointerException ex)
			{
				downloadFailed(ex);
				return null;
			}

    		JSONParser jP = new JSONParser();
    		JSONObject result = (JSONObject)jP.parse(responseBody);
    		
    		//Check if reply is in correct form
            if ( result.containsKey("error") && ((int)result.get("error") == 0)
                 && result.containsKey("reply") && ((JSONObject)result.get("reply")).containsKey("metadata")) 
            {
            	JSONObject reply = (JSONObject)result.get("reply");
            	
            	if(!reply.containsKey("metadata"))
            	{
            		downloadFailed(null);
            	}
            	JSONObject metadata = (JSONObject)reply.get("metadata"); 
                MetaData meta = new MetaData(metadata);
                meta.setNodeType(node.getNodeType());
                return meta;
            }
        }
    	catch (IOException | ParseException e)
    	{
    		downloadFailed(e);
		}
		return null;
    }
    
    private void downloadFailed(Exception e) {
    	if(e != null) e.printStackTrace();
    	EventBus.getDefault().postSticky(new MetadataDownloadFailedEvent(fileUuid));
    }
    
    private void publishMetadata(MetaData meta) {
    	EventBus.getDefault().postSticky(new MetadataEvent(fileUuid, meta));
    }
    
}
