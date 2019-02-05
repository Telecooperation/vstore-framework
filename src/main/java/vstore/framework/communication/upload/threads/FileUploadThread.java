package vstore.framework.communication.upload.threads;

import org.greenrobot.eventbus.EventBus;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import vstore.framework.communication.CommunicationManager;
import vstore.framework.communication.upload.ProgressRequestBody;
import vstore.framework.communication.upload.UploadInfo;
import vstore.framework.communication.upload.UploadQueueObject;
import vstore.framework.communication.upload.events.UploadDoneCompletelyEvent;
import vstore.framework.communication.upload.events.UploadDoneEvent;
import vstore.framework.communication.upload.events.UploadBeginEvent;
import vstore.framework.communication.upload.events.UploadFailedCompletelyEvent;
import vstore.framework.communication.upload.events.UploadFailedEvent;
import vstore.framework.communication.upload.events.UploadFailedPermanentlyEvent;
import vstore.framework.communication.upload.events.UploadStateEvent;
import vstore.framework.db.table_helper.FileDBHelper;
import vstore.framework.error.ErrorMessages;
import vstore.framework.logging.log_events.LogUploadDoneEvent;
import vstore.framework.node.NodeInfo;
import vstore.framework.node.NodeManager;
import vstore.framework.utils.JsonUtils;

/**
 * Uploads a file to the target node.
 * Then updates the local database and sends the new mapping to the master peer.
 */
public class FileUploadThread extends Thread implements ProgressRequestBody.Listener {
	private static int MAX_NUMBER_OF_ATTEMPTS = 3;
	private static int SEC_SLEEP_BETWEEN_ATTEMPTS = 5;
	
	private OkHttpClient httpClient;
	private UploadQueueObject qObject;
	private long beginTime;
	
	public FileUploadThread(UploadQueueObject file) throws Exception {
		if(file == null)
    	{
    		throw new Exception(ErrorMessages.PARAMETERS_MUST_NOT_BE_NULL);
    	}
		httpClient = new OkHttpClient.Builder()
				.connectTimeout(10, TimeUnit.SECONDS)
				.readTimeout(10, TimeUnit.SECONDS)
				.writeTimeout(300, TimeUnit.SECONDS)
				.build();
		qObject = file;
	}
	
	@Override
	public void run() {
		beginTime = System.currentTimeMillis();
		List<String> node_ids = qObject.nodeIds;
		if(node_ids == null || node_ids.size() == 0) {
		    failedCompletely("No storage nodes to upload to.");
		    return;
		}

		//Upload file to every required storage node
		for(int i = 0; i < node_ids.size(); ++i)
		{
			String node_id = node_ids.get(i);
			if(node_id == null) { continue; }
			NodeInfo node = NodeManager.get().getNode(node_id);
			if(node == null) { continue; }
			String uploadUrl = node.getUploadUri();
			qObject.attemptsPerNode.put(node_id, 0);
			doUpload(node_id, uploadUrl);
		}

		//Post event that upload is done completely
        UploadDoneCompletelyEvent evt = new UploadDoneCompletelyEvent(qObject.fileId, qObject.nodeIds);
		EventBus.getDefault().postSticky(evt);
	}
	
	private void doUpload(String node_id, String uploadUrl) {
		ProgressRequestBody reqBody = new ProgressRequestBody(node_id, qObject.requestBody, this);
		Request request = new Request.Builder()
				.url(uploadUrl)
				.post(reqBody)
				.build();

		while(qObject.attemptsPerNode.get(node_id) < MAX_NUMBER_OF_ATTEMPTS)
		{
		    UploadBeginEvent beginEvt
                    = new UploadBeginEvent(qObject.fileId, node_id, qObject.attemptsPerNode.get(node_id));
            EventBus.getDefault().post(beginEvt);

			try (Response response = httpClient.newCall(request).execute()) 
		    {
		    	//HTTP request was successful (status 2xx)
		    	if(response.isSuccessful())
		    	{
		    		//Check if upload was also successful 
		    		//(checking the JSON answer from the storage node)
	    			JSONParser p = new JSONParser();
		    		JSONObject j = null;
		    		boolean invalidResponse = false;
					try 
					{
						j = (JSONObject) p.parse(response.body().string());
					} 
					catch (ParseException | NullPointerException e)
					{
						//Invalid response received.
						e.printStackTrace();
						invalidResponse = true;
					}
		    		
		            if (!invalidResponse && JsonUtils.getIntFromJson("error", j, 1) == 0)
					{
		                //Upload successful.
		                //Update the row in the database:
		                //(upload_pending = false, upload_failed = false)
		                try 
		                {
							FileDBHelper.updateFile(qObject.fileId, false, false, false);

                            //TODO What to do if the file was uploaded, but the file-node-mapping update failed?
                            CommunicationManager.postFileNodeMapping(qObject.fileId, node_id);
						} 
		                catch (SQLException e) 
		                {
		                	//Upload successful, but updating the database failed.
		                	//Should never happen
		                	e.printStackTrace();
						}
		                uploadDone(node_id);
		                return;
		            }
		            else 
		            {
		                //Upload not successful, node replied with an error.
		                //Update the row in the database:
		                //(upload_pending = false, upload_failed = true, delete_pending = false)
		                try 
		                {
							FileDBHelper.updateFile(qObject.fileId, false, true, false);
						} 
		                catch (SQLException e) {
							//Upload failed, and updating database failed.
		                	e.printStackTrace();
						}
		                
		                //Post event that upload failed permanently
			    		String strResponse = (invalidResponse) ?
			    				("(Invalid response)") : ((String)j.get("error_msg"));
                        failedPermanently(node_id, strResponse);

		                //Post event for the logger
		                LogUploadDoneEvent logEvt = new LogUploadDoneEvent();
		                logEvt.fileUUID = qObject.fileId;
		                //TODO Make upload info work again for eval data
		                //logEvt.uploadInfo = uploadInfo;
		                logEvt.success = false;
		                EventBus.getDefault().post(logEvt);
		            }
	                return;
		    	}
		    }
		    catch (IOException e) 
		    {
				e.printStackTrace();
			}
			sleepBetweenAttempts(node_id);
		}
        failedPermanently(node_id,"");
	}
	
	private void sleepBetweenAttempts(String node_id) {
		uploadFailed(node_id, SEC_SLEEP_BETWEEN_ATTEMPTS);
		long sleepTime = SEC_SLEEP_BETWEEN_ATTEMPTS * 1000;
		long beginTime = System.currentTimeMillis();
		while(System.currentTimeMillis() - beginTime < sleepTime)
		{
    		try 
    		{
				Thread.sleep(sleepTime);
			} 
    		catch (InterruptedException e) 
    		{
				e.printStackTrace();
    			//Go to sleep again until time is reached
    			long elapsedTime = System.currentTimeMillis() - beginTime;
				beginTime = System.currentTimeMillis();
    			sleepTime = sleepTime - elapsedTime;
			}
		}
		qObject.attemptsPerNode.put(node_id, qObject.attemptsPerNode.get(node_id)+1);
	}

	private void uploadDone(String node_id) {
        //Post event for the interested subscribers
        EventBus.getDefault().postSticky(new UploadDoneEvent(node_id,qObject.fileId));

        //Post event for the logger
        //TODO Add information about storage node (the identifier)
        LogUploadDoneEvent logEvt = new LogUploadDoneEvent();
        logEvt.fileUUID = qObject.fileId;
        //Upload speed information etc
        long elapsedTime = System.currentTimeMillis() - beginTime;
        long uploadSpeed;
        try {
            uploadSpeed = qObject.requestBody.contentLength() / elapsedTime;
        }
        catch(IOException e)
        {
            uploadSpeed = 0;
        }
        logEvt.uploadInfo = new UploadInfo(elapsedTime, uploadSpeed);
        logEvt.success = true;
        EventBus.getDefault().post(logEvt);
    }

    private void uploadFailed(String node_id, int sleepTime) {
        //Post event that the upload failed once
        UploadFailedEvent eFailed = new UploadFailedEvent(qObject.fileId, node_id,true, sleepTime,
                qObject.attemptsPerNode.get(node_id)+1);
        EventBus.getDefault().postSticky(eFailed);
    }

    private void failedPermanently(String node_id, String message) {
        UploadFailedPermanentlyEvent evt
                = new UploadFailedPermanentlyEvent(qObject.fileId, node_id,"Node replied: " + message);
        EventBus.getDefault().postSticky(evt);
    }

    private void failedCompletely(String message) {
        UploadFailedCompletelyEvent evt
                = new UploadFailedCompletelyEvent(qObject.fileId, message);
        EventBus.getDefault().postSticky(evt);
    }

	@Override
	public void onProgress(String node_id, int progress) {
		//Publish upload state
		EventBus.getDefault().post(
				new UploadStateEvent(
						progress,
						qObject.fileId,
						node_id,
						false));

	}
}
