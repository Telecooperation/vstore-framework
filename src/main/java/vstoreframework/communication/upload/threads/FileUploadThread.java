package vstoreframework.communication.upload.threads;

import java.io.IOException;
import java.sql.SQLException;

import org.greenrobot.eventbus.EventBus;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import vstoreframework.communication.upload.UploadQueueObject;
import vstoreframework.communication.upload.events.SingleUploadDoneEvent;
import vstoreframework.communication.upload.events.UploadFailedEvent;
import vstoreframework.communication.upload.events.UploadFailedPermanentlyEvent;
import vstoreframework.db.table_helper.FileDBHelper;
import vstoreframework.error.ErrorMessages;
import vstoreframework.exceptions.DatabaseException;
import vstoreframework.logging.log_events.LogUploadDoneEvent;

public class FileUploadThread extends Thread {
	private static int MAX_NUMBER_OF_ATTEMPTS = 3;
	private static int SEC_SLEEP_BETWEEN_ATTEMPTS = 5;
	
	OkHttpClient httpClient;
	
	private UploadQueueObject queueObject;
	
	public FileUploadThread(UploadQueueObject file) throws Exception {
		if(file == null)
    	{
    		throw new Exception(ErrorMessages.PARAMETERS_MUST_NOT_BE_NULL);
    	}
		httpClient = new OkHttpClient();
		queueObject = file;
		
        EventBus.getDefault().register(this);
	}
	
	@Override
	public void run() {
		
		doUpload();
        EventBus.getDefault().unregister(this);
	}
	
	public void doUpload() {
		Request request = new Request.Builder()
	        .url(queueObject.uploadUrl)
	        .post(queueObject.requestBody)
	        .build();
		    
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
		
		//TODO: Somehow publish upload state
		/*EventBus.getDefault().post(new UploadStateEvent(
            uploadInfo.getProgressPercent(),
            uploadInfo.getUploadId(),
            false));*/
		
		while(queueObject.numberOfAttempts < MAX_NUMBER_OF_ATTEMPTS)
		{	
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
					catch (ParseException e) 
					{
						//Invalid response received.
						e.printStackTrace();
						invalidResponse = true;
					}
		    		
		            if (!invalidResponse && (int)j.get("error") == 0) 
		            {
		                //Upload successful.
		                //Update the row in the database:
		                //(upload_pending = false, upload_failed = false)
		                try 
		                {
							dbHelper.updateFile(queueObject.fileId, false, false, false);
						} 
		                catch (SQLException e) 
		                {
		                	//Upload successful, but updating the database failed.
		                	//Should never happen
		                	e.printStackTrace();
						}
				    	
			    		EventBus.getDefault().post(new SingleUploadDoneEvent(queueObject.fileId));
		                
		                //Post event for the logger
		                LogUploadDoneEvent logEvt = new LogUploadDoneEvent();
		                logEvt.fileUUID = queueObject.fileId;
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
		                try 
		                {
							dbHelper.updateFile(queueObject.fileId, false, true, false);
						} 
		                catch (SQLException e) {
							//Upload failed, and updating database failed.
		                	e.printStackTrace();
						}
				    	
			    		EventBus.getDefault().post(new SingleUploadDoneEvent(queueObject.fileId));
		                
		                //Post event that upload failed permanently
			    		String strResponse = (invalidResponse) ? 
			    				("(Invalid response)") : ((String)j.get("error_msg"));
		                UploadFailedPermanentlyEvent evt
		                        = new UploadFailedPermanentlyEvent("Node replied: " + strResponse);
		                EventBus.getDefault().postSticky(evt);
		                
		                //Post event for the logger
		                LogUploadDoneEvent logEvt = new LogUploadDoneEvent();
		                logEvt.fileUUID = queueObject.fileId;
		                //TODO 
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
			sleepBetweenAttempts();
		}
		
		//Post event that the upload failed, even after multiple attempts
		UploadFailedEvent eFailed 
			= new UploadFailedEvent(queueObject.fileId, false, 0);
		EventBus.getDefault().post(eFailed);
	}
	
	private void sleepBetweenAttempts() {
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
		queueObject.numberOfAttempts++;
	}
}
