package vstoreframework.communication.upload;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import org.greenrobot.eventbus.EventBus;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.MultipartBody.Builder;
import vstoreframework.communication.events.AllUploadsDoneEvent;
import vstoreframework.communication.events.SingleUploadDoneEvent;
import vstoreframework.communication.events.UploadFailedEvent;
import vstoreframework.communication.events.UploadFailedPermanentlyEvent;
import vstoreframework.db.table_helper.FileDBHelper;
import vstoreframework.file.VStoreFile;
import vstoreframework.logging.log_events.LogUploadDoneEvent;
import vstoreframework.node.NodeManager;

/**
 * This class wraps some functions for uploading.
 */
public class Uploader {
	public static final MediaType JSON
		= MediaType.parse("application/json; charset=utf-8");
	
	private static Uploader mInstance;
	private boolean uploadsRunning;
	
	private static int defaultBackoffMultiplier = 5;
	
	Queue<UploadQueueObject> requestQueue;
	OkHttpClient httpClient;
	
	private Uploader() {
		requestQueue = new LinkedBlockingQueue<UploadQueueObject>();
		httpClient = new OkHttpClient();
	}
	
	public static Uploader getUploader() {
		if(mInstance == null) 
		{
			return new Uploader();
		}
		return mInstance;
	}

	/**
	 * Add an upload to the uploading queue
	 * 
	 * @param file The file which should be stored.
	 * @param deviceId The unique device id of the source device
	 */
    public void enqueueUpload(VStoreFile file, String deviceId) {
        RequestBody body = new Builder()
    		.setType(MultipartBody.FORM)
            .addFormDataPart("filedata", file.getUUID(),
                RequestBody.create(MediaType.parse(file.getFileType()), file.getFullPath()))
            .addFormDataPart("descriptiveName", file.getDescriptiveName())
            .addFormDataPart("mimetype", file.getFileType())
            .addFormDataPart("extension", file.getFileExtension())
            .addFormDataPart("filesize", Long.toString(file.getFileSize()))
            .addFormDataPart("creationdate", Long.toString(file.getCreationDateUnix()))
            .addFormDataPart("isPrivate", "" + file.isPrivate())
            .addFormDataPart("phoneID", deviceId)
            .addFormDataPart("context", file.getContext().getJson().toString())
            .build();
    	
    	UploadQueueObject q_obj = new UploadQueueObject();
    	q_obj.fileId = file.getUUID();
    	q_obj.requestBody = body;
    	//Get upload location from node manager for this node
    	q_obj.uploadUrl = NodeManager.getInstance().getNode(file.getNodeID()).getUploadUri();
    	
    	if(requestQueue != null)
    	{
        	requestQueue.add(q_obj);
    	}
    }

    /**
     * This method starts uploads in the background if some have the pending state in the database.
     */
    public void startUploads() {
        //FileDBHelper dbHelper = new FileDBHelper();
        //List<VStoreFile> pending = dbHelper.getFilesToUpload(FileDBHelper.SORT_BY_DATE_DESCENDING);
    	
    	if(uploadsRunning) return;
    	
		while(requestQueue.size() > 0)
		{
    		UploadQueueObject nextUpload = requestQueue.peek();
    		
    		Request request = new Request.Builder()
		        .url(nextUpload.uploadUrl)
		        .post(nextUpload.requestBody)
		        .build();
    		    
    		//TODO: Somehow publish upload state
    		/*EventBus.getDefault().post(new UploadStateEvent(
	            uploadInfo.getProgressPercent(),
	            uploadInfo.getUploadId(),
	            false));*/
    		
		    try (Response response = httpClient.newCall(request).execute()) 
		    {
		    	//HTTP request was successful
		    	if(response.isSuccessful())
		    	{
		    		boolean invalidResponse = false;
		    		//Check if upload was also successful 
		    		//(by checking the JSON answer from the storage node)
	    			JSONParser p = new JSONParser();
		    		JSONObject j = null;
					try 
					{
						j = (JSONObject) p.parse(response.body().string());
					} 
					catch (ParseException e) 
					{
						//Invalid response received.
						e.printStackTrace();
						invalidResponse = true;
					}
		    		
		            FileDBHelper dbHelper = new FileDBHelper();
		            if (!invalidResponse && (int)j.get("error") == 0) 
		            {
		                //Upload successful.
		                //Update the row in the database:
		                //(upload_pending = false, upload_failed = false)
		                dbHelper.updateFile(nextUpload.fileId, false, false, false);
				    	
			    		UploadQueueObject head = requestQueue.poll();
			    		EventBus.getDefault().post(new SingleUploadDoneEvent(head.fileId));
		                
		                //Post event for the logger
		                LogUploadDoneEvent logEvt = new LogUploadDoneEvent();
		                logEvt.fileUUID = head.fileId;
		                //TODO Upload speed information etc
		                //logEvt.uploadInfo = uploadInfo;
		                logEvt.success = true;
		                EventBus.getDefault().post(logEvt);
		            }
		            else 
		            {
		                //Upload not successful, node replied with an error.
		                //Update the row in the database:
		                //(upload_pending = false, upload_failed = true, delete_pending = false)
		                dbHelper.updateFile(nextUpload.fileId, false, true, false);
				    	
			    		UploadQueueObject head = requestQueue.poll();
			    		EventBus.getDefault().post(new SingleUploadDoneEvent(head.fileId));
		                
		                //Post event that upload failed permanently
			    		String strResponse = (invalidResponse) ? ("(Invalid response)") : ((String)j.get("error_msg"));
		                UploadFailedPermanentlyEvent evt
		                        = new UploadFailedPermanentlyEvent("Node replied: " + strResponse);
		                EventBus.getDefault().postSticky(evt);
		                
		                //Post event for the logger
		                LogUploadDoneEvent logEvt = new LogUploadDoneEvent();
		                logEvt.fileUUID = head.fileId;
		                //TODO 
		                //logEvt.uploadInfo = uploadInfo;
		                logEvt.success = false;
		                EventBus.getDefault().post(logEvt);
		            }
		    	}
		    	else 
		    	{
		    		int numberOfAttempts = requestQueue.peek().numberOfAttempts++;
		    		
		    		boolean willRetry = true;
		    		if(numberOfAttempts == 5)
		    		{
		    			//TODO: put to beginning of queue
		    			UploadQueueObject head = requestQueue.poll();
		    			//requestQueue.add(head);
		    			willRetry = false;
		    		}
		    		
		    		//Post event that the upload failed
		    		UploadFailedEvent eFailed = new UploadFailedEvent(
		    				nextUpload.fileId,
		    				willRetry,
		    				numberOfAttempts*defaultBackoffMultiplier);
		    		
		    		EventBus.getDefault().post(eFailed);
		    	}
		    } catch (IOException e) {
				e.printStackTrace();
	    		requestQueue.peek().numberOfAttempts++;
			}
		    /*catch(ConnectException e) 
		    {
		    	//TODO
		    }*/
    	}
    	
		EventBus.getDefault().post(new AllUploadsDoneEvent());
		
    }
}
