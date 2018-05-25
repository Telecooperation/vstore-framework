package vstoreframework.communication.threads;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.greenrobot.eventbus.EventBus;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import vstoreframework.config.ConfigManager;
import vstoreframework.db.table_helper.FileDBHelper;
import vstoreframework.exceptions.DatabaseException;
import vstoreframework.file.VStoreFile;
import vstoreframework.file.events.FileDeletedEvent;
import vstoreframework.node.NodeInfo;
import vstoreframework.node.NodeManager;
import vstoreframework.utils.FileUtils;
import vstoreframework.utils.FrameworkUtils;

/**
 * This thread is responsible for reading all files from the local database 
 * that are marked for deletion and sends a request to the corresponding node
 * to actually delete the file.
 */
public class DeleteFilesThread extends Thread {

	private OkHttpClient httpClient;
	
	public DeleteFilesThread() { }
	
	@Override
	public void run() {
		//Get all files marked for deletion from the database
		FileDBHelper dbHelper;
		try 
		{
			dbHelper = new FileDBHelper();
		} 
		catch (DatabaseException e) 
		{
			e.printStackTrace();
			return;
		}
        List<VStoreFile> files = dbHelper.getFilesForDeletion();

        
        //Fetch the node information for each file and send a delete request to the node
        NodeManager nodemgr = NodeManager.getInstance();
        for(final VStoreFile f : files) 
        {
            if(f.getNodeID() == null || f.getNodeID().equals("")) 
            {
            	//File was only stored on phone. 
            	//Thus we delete it only from disk and do not need to contact a node.
                doDelete(dbHelper, f);
                
                //Post success event
                EventBus.getDefault().post(new FileDeletedEvent(f.getUUID()));
                continue;
            }
            
            NodeInfo node = nodemgr.getNode(f.getNodeID());
            if(node == null) { continue; }
            //Node information found
            
            
            String url = node.getDeleteUri(f.getUUID(), FrameworkUtils.getDeviceIdentifier());
            
            Request request = new Request.Builder()
            		.url(url)
            		.delete()
            		.build();
            
            try (Response response = httpClient.newCall(request).execute()) 
            {
	        	ResponseBody resBody = response.body();
	        	if(resBody == null) return;
	        	String responseBody = resBody.string();
	    		JSONParser jP = new JSONParser();
	    		JSONObject result = (JSONObject)jP.parse(responseBody);
	                       
	            if(result.containsKey("error") && (((int)result.get("error")) != 1)
	                    || response.code() == 404)
	            {
	                doDelete(dbHelper, f);
	                //Post success event
	                EventBus.getDefault().post(new FileDeletedEvent(f.getUUID()));
	            }    
            } 
            catch (IOException e) {
				e.printStackTrace();
				continue;
			} 
            catch (ParseException e) {
				e.printStackTrace();
				continue;
			}
        }
	}
	
	private void doDelete(FileDBHelper dbHelper, VStoreFile f) {
        //Delete entries from database
        dbHelper.deleteFile(f.getUUID());
        //Delete file and thumbnail
        FileUtils.deleteFile(new File(f.getFullPath()));
        FileUtils.deleteFile(
        		new File(ConfigManager.getThumbnailsDir(), f.getUUID() + ".png"));
    }

	
	
}
