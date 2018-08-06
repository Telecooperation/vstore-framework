package vstore.framework.communication.download.threads;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.concurrent.Callable;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import vstore.framework.communication.download.events.MetadataDownloadFailedEvent;
import vstore.framework.communication.download.events.MetadataEvent;
import vstore.framework.file.MetaData;
import vstore.framework.node.NodeInfo;
import vstore.framework.utils.IdentifierUtils;

/**
 * Callable for downloading metadata of a file from a storage node.
 * Publishes {@link MetadataDownloadFailedEvent} and {@link MetadataEvent}.
 */
public class MetadataDownloadCallable implements Callable<MetaData> {
	
	private final String fileUuid;
	private NodeInfo node;
    private final OkHttpClient httpClient; 
    
    public MetadataDownloadCallable(String fileUuid, NodeInfo node) {
    	this.fileUuid = fileUuid;
    	this.node = node;
    	
    	httpClient = new OkHttpClient();
    }

	@Override
	public MetaData call() {
		//Request the metadata of the file
		return downloadMetadata();
	}
    
    private MetaData downloadMetadata() {
    	Request request = new Request.Builder()
    	        .url(node.getMetadataUri(fileUuid, IdentifierUtils.getDeviceIdentifier(), true))
    	        .build();
    	
    	try (Response response = httpClient.newCall(request).execute()) 
    	{
    		if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

			return parseResponse(response);
        }
    	catch (IOException e)
    	{
    		e.printStackTrace();
		}
		return null;
    }

    private MetaData parseResponse(Response response) {
        String responseBody;
        try
        {
            responseBody = response.body().string();
        }
        catch(IOException | NullPointerException ex)
        {
            ex.printStackTrace();
            return null;
        }

        try {
            JSONParser jP = new JSONParser();
            JSONObject result = (JSONObject) jP.parse(responseBody);

            //Check if reply is in correct form
            if (result.containsKey("error")
                    && ((long) result.get("error") == 0)
                    && result.containsKey("reply")
                    && ((JSONObject)result.get("reply")).containsKey("metadata"))
            {
                JSONObject reply = (JSONObject) result.get("reply");
                if (!reply.containsKey("metadata"))
                {
                    return null;
                }
                JSONObject metadata = (JSONObject) reply.get("metadata");
                MetaData meta = new MetaData(metadata);
                meta.setNodeType(node.getNodeType());
                return meta;
            }
        } catch(ParseException e) {
            e.printStackTrace();
        }
        return null;
    }
}
