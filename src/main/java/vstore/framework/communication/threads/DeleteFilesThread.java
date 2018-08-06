package vstore.framework.communication.threads;

import org.greenrobot.eventbus.EventBus;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.IOException;
import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import vstore.framework.communication.master_node.file_node_mapping.DeleteFileNodeMappingCallable;
import vstore.framework.db.table_helper.FileDBHelper;
import vstore.framework.file.FileManager;
import vstore.framework.file.VStoreFile;
import vstore.framework.file.events.FileDeletedEvent;
import vstore.framework.node.NodeInfo;
import vstore.framework.node.NodeManager;
import vstore.framework.utils.FileUtils;
import vstore.framework.utils.IdentifierUtils;
import vstore.framework.utils.JsonUtils;

/**
 * This thread is responsible for reading all files from the local database 
 * that are marked for deletion and sends a request to the corresponding node
 * to actually delete the file.
 *
 * TODO: Handle all deletions in parallel using futures
 */
public class DeleteFilesThread extends Thread {

	private OkHttpClient httpClient;

	public DeleteFilesThread() {
		httpClient = new OkHttpClient();
	}
	
	@Override
	public void run() {
		//Get all files marked for deletion from the database
		List<VStoreFile> files = FileDBHelper.getFilesForDeletion();

        
        //Fetch the node information for each file and send a delete request to the node
        NodeManager nodemgr = NodeManager.get();
        for(final VStoreFile f : files) 
        {
            if(f == null) { continue; }
            if(f.getMainNodeId() == null || f.getMainNodeId().equals(""))
            {
            	//File was only stored on the device locally.
            	//Thus we delete it only from disk and do not need to contact a node.
                doLocalDelete(f);
                //Post success event
                EventBus.getDefault().post(new FileDeletedEvent(f.getUuid()));
                continue;
            }

            //Delete file from all nodes it is stored on
            for(String nodeId : f.getStoredNodeIds())
			{
				NodeInfo node = nodemgr.getNode(nodeId);
				if(node == null) { continue; /* TODO Get node information */}
				//Node information found
				//TODO If a deletion returned false, cache it and retry later
				deleteFromRemote(f, node);
			}

			//Delete file-node-mapping
            //Simply call callable because we are already in background thread
            DeleteFileNodeMappingCallable mappingCallable = new DeleteFileNodeMappingCallable(f.getUuid());
            try {
                mappingCallable.call();
            } catch(Exception e) {
                e.printStackTrace();
            }
            doLocalDelete(f);
			//Post success event
			EventBus.getDefault().post(new FileDeletedEvent(f.getUuid()));
		}
	}

	private boolean deleteFromRemote(VStoreFile f, NodeInfo node) {
		//Build request body
		RequestBody body = new MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart("uuid", f.getUuid())
				.addFormDataPart("phoneID", IdentifierUtils.getDeviceIdentifier())
				.build();

		String url = node.getDeleteUri(f.getUuid(), IdentifierUtils.getDeviceIdentifier());
		Request request = new Request.Builder()
				.url(url)
				.delete(body)
				.build();

		try (Response response = httpClient.newCall(request).execute())
		{
			ResponseBody resBody = response.body();
			if(resBody == null) return false;
			String responseBody = resBody.string();
			JSONParser jP = new JSONParser();
			JSONObject result = (JSONObject)jP.parse(responseBody);

			if(result.containsKey("error") && (JsonUtils.getIntFromJson("error", result, 1) != 1)
					|| response.code() == 404)
			{
				return true;
			}
		}
		catch (IOException | ParseException e) {
			e.printStackTrace();
		}
        return false;
	}
	
	private void doLocalDelete(VStoreFile f) {
        //Delete entries from database
        FileDBHelper.deleteFile(f.getUuid());
        //Delete file and thumbnail
        FileUtils.deleteFile(new File(f.getFullPath()));
        FileUtils.deleteFile(
        		new File(FileManager.get().getThumbnailsDir(), f.getUuid() + ".png"));
    }

	
	
}
