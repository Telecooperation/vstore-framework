package vstore.framework.logging;


import static vstore.framework.logging.LoggingType.STORE;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import okhttp3.MultipartBody;
import okhttp3.MultipartBody.Builder;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import vstore.framework.context.Connectivity;
import vstore.framework.logging.log_events.LogCancelLoggingEvent;
import vstore.framework.logging.log_events.LogDownloadDoneEvent;
import vstore.framework.logging.log_events.LogDownloadStartEvent;
import vstore.framework.logging.log_events.LogMatchingAddNodeEvent;
import vstore.framework.logging.log_events.LogMatchingAddRuleEvent;
import vstore.framework.logging.log_events.LogMatchingDoneEvent;
import vstore.framework.logging.log_events.LogMatchingStartLoggingEvent;
import vstore.framework.logging.log_events.LogUploadDoneEvent;
import vstore.framework.node.NodeType;
import vstore.framework.utils.FrameworkUtils;

/**
 * This thread is responsible for receiving logging events and handling them in the background.
 * Only one of the last events puts the log entry into the list for log-uploads.
 *
 * The order of logging events for a store+matching operation is as follows:
 * LogMatchingStartLoggingEvent --> LogMatchingAddNodeEvent --> LogMatchingAddRuleEvent --> LogUploadDoneEvent OR LogMatchingDoneEvent
 * The AddRule event is only published when rules are active.
 *
 * The order of logging events for a download operation is as follows:
 *
 */

@SuppressWarnings("unchecked")
public class LoggingService extends Thread {
    /**
     * URL of the log storage server.
     */
    private static final String LOGGING_URL = "http://130.83.163.10:3232/log";
    /**
     * Route for logs of file storing/matching
     */
    private static final String ROUTE_STORE_LOG = "/store";
    /**
     * Route for logs of file downloads
     */
    private static final String ROUTE_DOWNLOAD_LOG = "/download";
    
    private static LoggingService mInstance;

    private boolean mUploadRunning;
    private HashMap<String, JSONObject> mDownloads;
    
    private LoggingService() {
        EventBus.getDefault().register(this);
        mUploadRunning = false;
        mDownloads = new HashMap<>();
        
        //Start log uploads if any
        startLogUploads();
    }
    
    public static LoggingService getThread() {
    	if(mInstance == null) {
    		mInstance = new LoggingService();
    	}
    	return mInstance;
    }
    
    public void run() {
    	
    }

    
	@Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onMessageEvent(LogMatchingStartLoggingEvent evt) {
        JSONObject jNew = new JSONObject();
        if (evt.file != null && evt.matchingMode != null) 
        {
            //Create log information as json object
            jNew.put("fileId", evt.file.getUUID());
            jNew.put("deviceId", FrameworkUtils.getDeviceIdentifier());
            jNew.put("descriptiveName", evt.file.getDescriptiveName());
            jNew.put("filetype", evt.file.getFileType());
            jNew.put("extension", evt.file.getFileExtension());
            jNew.put("dateCreation", evt.file.getCreationDateUnix());
            jNew.put("fileMD5", evt.file.getMD5Hash());
            jNew.put("fileContext", evt.file.getContext().getJson().toString());
            jNew.put("isPrivate", evt.file.isPrivate());
            jNew.put("fileSize", evt.file.getFileSize());
            jNew.put("uploadStartTime", System.currentTimeMillis());
            jNew.put("matchingMode", evt.matchingMode.name());
            //Put json string into logfile.
            LogFile.logEntry(evt.file.getUUID(), jNew.toString());
        }
        
    }

    /**
     * Updates the log file with node information
     * @param evt
     */
	@Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onMessageEvent(LogMatchingAddNodeEvent evt) {
        if(evt.fileId == null) return;
        
        //Check if the logging for this file has started.
        if(LogFile.contains(evt.fileId)) 
        {
        	try 
        	{
        		JSONParser p = new JSONParser();
            	JSONObject j;
				
					j = (JSONObject) p.parse(LogFile.getEntry(evt.fileId));
				
                //Put the new information (node information)
            	//into the json string and save it back to the logfile.
                if(evt.node != null) 
                {
                    j.put("nodeId", evt.node.getUUID());
                    j.put("nodeType", evt.node.getNodeType().name());
                } 
                else 
                {
                    j.put("nodeId", "");
                    j.put("nodeType", NodeType.PHONE.name());
                }
                LogFile.logEntry(evt.fileId, j.toString());
        	} 
        	catch (ParseException e) 
        	{
				//Should never happen!
				e.printStackTrace();
			}
        }
    }

    
	@Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onMessageEvent(LogMatchingAddRuleEvent evt) {
        if(evt.fileId == null) return;
        
        //Check if the logging for this file has started.
        if(LogFile.contains(evt.fileId)) 
        {
            try 
            {
                JSONParser p = new JSONParser();
            	JSONObject j = (JSONObject) p.parse(LogFile.getEntry(evt.fileId));
            	//Put the rule information into the json string and save it back to the sharedpref.
                j.put("rule", evt.rule.getJson().toString());
                j.put("decisionLayerIndex", evt.mDecisionLayerIndex);
                LogFile.logEntry(evt.fileId, j.toString());
            } 
            catch(ParseException ex) 
            {
                LogFile.deleteEntry(evt.fileId);
            }
        }
    }

	/**
	 * If this event is received, the file has finished uploading.
	 * @param evt
	 */
	@Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onMessageEvent(LogUploadDoneEvent evt) {       
        if(evt.fileUUID == null || evt.uploadInfo == null) return; 
            
	    try 
	    {
	        //Put the upload statistics into the json string and save the new string to the upload file.
	        JSONParser p = new JSONParser();
	    	JSONObject j = (JSONObject) p.parse(LogFile.getEntry(evt.fileUUID));
	        j.put("uploadDuration", evt.uploadInfo.getElapsedTime());
	        j.put("uploadSpeed", evt.uploadInfo.getUploadRate());
	        j.put("uploadEndTime", System.currentTimeMillis());
	        writeToUploadFile(STORE, evt.fileUUID, j);
	    } 
	    catch (ParseException ignored) { }
	    LogFile.deleteEntry(evt.fileUUID);
	    //Start to upload the log
	    startLogUploads();
    }

    
	@Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onMessageEvent(LogMatchingDoneEvent evt) {
        //If this event is received, this file has finished without uploading.
        //It has only been saved on the device.
        if(evt.fileUUID == null || LogsForUpload.contains(evt.fileUUID)) return;
            
        try 
        {
        	JSONParser p = new JSONParser();
            JSONObject j = (JSONObject) p.parse(LogFile.getEntry(evt.fileUUID));
            j.put("uploadDuration", 0);
            j.put("uploadSpeed", 0);
            j.put("uploadEndTime", System.currentTimeMillis());
            writeToUploadFile(STORE, evt.fileUUID, j);
        } 
        catch(ParseException ignored) { }
        
        //Remove string from the log file.
        LogFile.deleteEntry(evt.fileUUID);
        //Start to upload the log
        startLogUploads();    
    }

	@Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onMessageEvent(LogDownloadStartEvent evt) {
        //Received, when a download starts
        if(evt.fileId == null) return;
            
        JSONObject j = new JSONObject();
        j.put("fileId", evt.fileId);
        j.put("fileSize", evt.fileSize);
        j.put("metadata", evt.metadata);
        j.put("nodeId", evt.nodeId);
        j.put("nodeType", evt.nodeType.name());
        j.put("starttime", System.currentTimeMillis());
        mDownloads.put(evt.fileId, j);
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onMessageEvent(LogDownloadDoneEvent evt) {
        //Received, when a download finishes (or fails)
        if(evt.fileId == null || !mDownloads.containsKey(evt.fileId)) return;
        
        JSONObject j = mDownloads.get(evt.fileId);
        long endtime = System.currentTimeMillis();
        long duration_ms;
        long fileSize;
        float downloadSpeed = 0;
        
        //Calculate download time
        duration_ms = endtime - (long)j.get("starttime");
        fileSize = (long)j.get("fileSize");
        if(fileSize > 0) 
        {
            downloadSpeed = fileSize / 1024.0f * 8.0f / (duration_ms / 1000.0f);
        }
        if(evt.downloadFailed) 
        {
            duration_ms = 0;
            downloadSpeed = 0;
        }
        //Put values into the json object
        j.put("endtime", endtime);
        j.put("downloadSpeed", downloadSpeed);
        j.put("durationMillis", duration_ms);
        j.put("deviceId", FrameworkUtils.getDeviceIdentifier());
        j.put("networkType", Connectivity.getCurrentConnectionType());
        j.put("wifiSsid", Connectivity.getWifiName());
        writeToUploadFile(LoggingType.DOWNLOAD, evt.fileId, j);
        mDownloads.remove(evt.fileId);
        startLogUploads();
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onMessageEvent(LogCancelLoggingEvent evt) {
    	if(evt.uuid == null || evt.uuid.equals("")) { return; }
    	LogFile.deleteEntry(evt.uuid);
    }

    /**
     * Writes the given string to the sharedPref file that contains the log entries to upload.
     * @param type The type of the log entry.
     * @param fileId The fileId the log entry belongs to.
     * @param j The json object containing the log entry.
     */
    private void writeToUploadFile(LoggingType type, String fileId, JSONObject j) {
        //Create uploadJson to put into the upload shared pref.
        JSONObject uploadJson = new JSONObject();
        uploadJson.put("type", type.name());
        uploadJson.put("value", j.toString());
        //Save the uploadJson into the sharedpref
        LogsForUpload.logEntry(fileId, uploadJson.toString());        
    }

    /**
     * Starts the uploads, if there are currently no running uploads.
     */
    private void startLogUploads() {
        //Only start uploads again if they are not running
        if(mUploadRunning) return;
        mUploadRunning = true;
        
        OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build();
        
        Map<String, ?> map = LogsForUpload.getAll();
        //Start upload for each entry in this file.
        for(final String key : map.keySet()) 
        {
            String url = LOGGING_URL;

            JSONParser p = new JSONParser();
            JSONObject entry = null;
            try 
            {
            	entry = (JSONObject) p.parse(LogsForUpload.getEntry(key));
            } 
            catch(ParseException e)
            {
            	//If failed to read the string, remove it and continue
                LogsForUpload.deleteEntry(key);
                continue;
            }

            //Get the type of the log to upload
            String t = (String)entry.get("type");
            LoggingType type;
            try 
            {
                type = LoggingType.valueOf(t);
            } 
            catch(IllegalArgumentException e) 
            {
                //If failed, remove log entry and continue
                continue;
            }
            String log = (String)entry.get("value");

            //Determine the log upload URL
            switch (type) 
            {
                case STORE:
                    url += ROUTE_STORE_LOG;
                    break;

                case DOWNLOAD:
                    url += ROUTE_DOWNLOAD_LOG;
                    break;

                default:
                    return;
            }
            //Try to upload. Will block for each single upload.
        	RequestBody body = new Builder()
            		.setType(MultipartBody.FORM)
                    .addFormDataPart("log", log)
                    .build();
        	
        	Request request = new Request.Builder()
    		        .url(url)
    		        .post(body)
    		        .build();
        	//Post the log entry to the server
        	try (Response response = httpClient.newCall(request).execute()) 
		    {
        		if (!response.isSuccessful()) {}
        		
        		String result = response.body().string();
        		if (result != null) 
        		{
        			JSONObject jResult = (JSONObject) p.parse(result);
                    if (jResult.containsKey("error") && ((int)jResult.get("error") == 0)
                            && jResult.containsKey("reply")) 
                    {
                    	//Upload sent successfully
                        LogsForUpload.deleteEntry(key);
                    }
                }
		    } 
        	catch (IOException e) 
        	{
        		//Request failed
				e.printStackTrace();
			} 
        	catch (ParseException e) 
        	{
        		//Request json parsing failed
				e.printStackTrace();
			}
                    
            //Wait for 2 seconds
            long startTime = System.currentTimeMillis();
            while(System.currentTimeMillis() - startTime < 2000);
        }
        mUploadRunning = false;
        //Check if new uploads arrived
        if(LogsForUpload.getAll().keySet().size() > 0) 
        {
            startLogUploads();
        }
    }
}
