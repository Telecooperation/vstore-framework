package vstore.framework.communication;

import org.greenrobot.eventbus.EventBus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import vstore.framework.context.SearchContextDescription;
import vstore.framework.error.ErrorCode;
import vstore.framework.error.ErrorMessages;
import vstore.framework.exceptions.VStoreException;
import vstore.framework.file.MatchingResultRow;
import vstore.framework.file.MetaData;
import vstore.framework.file.events.NewFilesMatchingContextEvent;
import vstore.framework.matching.FileNodeMapper;
import vstore.framework.node.NodeInfo;
import vstore.framework.node.NodeManager;
import vstore.framework.utils.FrameworkUtils;

import static vstore.framework.communication.ApiConstants.ROUTE_FILES_MATCHING_CONTEXT;

public class RequestFilesMatchingContextThread extends Thread {
	private String mNodeId;
    private SearchContextDescription mUsageContext;
    private String mRequestId;
    
    private final OkHttpClient httpClient;
    
    public RequestFilesMatchingContextThread(String nodeUUID,
                                             SearchContextDescription usageContext,
                                             String requestId)
            throws Exception
    {
    	if(nodeUUID == null || nodeUUID.equals("") || requestId == null
                || requestId.equals("") || usageContext == null)
    	{
    		throw new VStoreException(ErrorCode.PARAMETERS_MUST_NOT_BE_NULL, ErrorMessages.PARAMETERS_MUST_NOT_BE_NULL);
    	}
    	
        mNodeId = nodeUUID;
        mUsageContext = usageContext;
        mRequestId = requestId;
        this.httpClient = new OkHttpClient();
    }
    
    @Override
    public void run() {
    	final NodeManager manager = NodeManager.getInstance();
        final NodeInfo node = manager.getNode(mNodeId);
        if(node == null) return;

        //Assemble the request address using node information (address, port) and the
        //route for requesting files matching context
        final String address = node.getAddress() + ":" + node.getPort() + ROUTE_FILES_MATCHING_CONTEXT;
        
        //Build the request
        RequestBody formBody = new FormBody.Builder()
                .add("context", mUsageContext.getJson().toString())
                .add("phoneID", FrameworkUtils.getDeviceIdentifier())
                .build();
        
        Request request = new Request.Builder()
    	        .url(address)
    	        .post(formBody)
    	        .build();
        
        try (Response response = httpClient.newCall(request).execute()) 
        {
            if (!response.isSuccessful()) 
            {
            	System.err.println("Request failed.");
            	return;
            }

            
            JSONParser p = new JSONParser();
            JSONObject result;
            try
            {
                result = (JSONObject) p.parse(response.body().string());
            }
            catch(NullPointerException e)
            {
                e.printStackTrace();
                return;
            }
            
            //Check if an error occurred on the server side
            if(!result.containsKey("error") || (boolean) result.get("error"))
            {
            	//Do nothing on error
        		postEmptyEvent(node.getUUID());
        		return;
            }
            
            //Take the response and put the file addresses and UUIDs into a list
            //Post an event with this list.
            //The vStore Framework provides methods for downloading a thumbnail
            //or the whole file, or just metadata. Apps can use these methods.
            
            if(result.containsKey("reply") && ((JSONObject)result.get("reply")).containsKey("files")) 
            {
            	JSONArray files = (JSONArray) ((JSONObject)result.get("reply")).get("files");
            	if(files.size() <= 0)
        		{
            		postEmptyEvent(node.getUUID());
            		return;
        		}
            	
            	//Write the node ID for each received uuid into the file<->node mapping
                FileNodeMapper mapper = FileNodeMapper.getMapper();
            	
                //Create a new event
                NewFilesMatchingContextEvent evt = new NewFilesMatchingContextEvent(node.getUUID(), mRequestId);
                for(Object o : files) 
                {
                    try 
                    {
                        JSONObject row = (JSONObject) o;
                        if(row.containsKey("uuid") && row.containsKey("creationTimestamp")
                                && row.containsKey("mimetype") && row.containsKey("descriptiveName")
                                && row.containsKey("filesize")) 
                        {
                            String uuid = (String)row.get("uuid");
                            String mimetype = (String)row.get("mimetype");
                            String filename = (String)row.get("descriptiveName");
                            long timestamp = (long)row.get("creationTimestamp");
                            long filesize = (long)row.get("filesize");

                            MetaData meta = new MetaData(filename, filesize, mimetype);
                            meta.setUUID(uuid);
                            meta.setNodeType(node.getNodeType());
                            meta.setCreationDate(timestamp);
                            MatchingResultRow resultRow = new MatchingResultRow(uuid, meta);
                            evt.addResult(resultRow);
                            
                            //Save mapping file id <-> node id to mapper 
                            mapper.storeNewMapping(uuid, mNodeId);
                        }
                    } 
                    catch(Exception ignored) { ignored.printStackTrace(); }
                }
                mapper.apply();
                //Post an event that we have a reply from a node with a list of matching files
                EventBus.getDefault().postSticky(evt);
                return;
            }
            postEmptyEvent(node.getUUID());
        } 
        catch (IOException | ParseException e)
        {
			// Error when sending the request to the node
            // or error when parsing the response as JSON
			e.printStackTrace();
			postEmptyEvent(node.getUUID());
		}
    }
    
    private void postEmptyEvent(String nodeId) {
        //Post the same event, but without content
        EventBus.getDefault().postSticky(
                new NewFilesMatchingContextEvent(nodeId, mRequestId));
    }
    
}
