package vstore.framework.communication.upload;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.MultipartBody.Builder;
import okhttp3.RequestBody;
import vstore.framework.communication.upload.events.AllUploadsDoneEvent;
import vstore.framework.communication.upload.events.UploadDoneCompletelyEvent;
import vstore.framework.communication.upload.threads.FileUploadThread;
import vstore.framework.db.table_helper.FileDBHelper;
import vstore.framework.file.VStoreFile;
import vstore.framework.utils.IdentifierUtils;

/**
 * This class wraps functions for controlling and managing uploads.
 * Each upload is started in an own thread, thus multiple uploads
 * can occur in parallel.
 */
public class Uploader {
	private static final Logger LOGGER = LogManager.getLogger(Uploader.class);
	public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
	
	private static Uploader mInstance;
	
	private HashMap<String, UploadQueueObject> uploadQueue;
	private HashMap<String, Thread> uploadThreads;
	
	private Uploader() {
		uploadQueue = new HashMap<>();
		readPendingUploadsFromDb();
		uploadThreads = new HashMap<>();
        EventBus.getDefault().register(this);
	}
	
	public static Uploader getUploader() {
		if(mInstance == null) 
		{
			mInstance = new Uploader();
		}
		return mInstance;
	}

	/**
	 * Add an upload to the uploading queue
	 * 
	 * @param file The file which should be stored.
	 */
    public void enqueueUpload(VStoreFile file) {
    	LOGGER.debug("Enqueuing new upload. File UUID: " + file.getUuid() + ", Name: " + file.getDescriptiveName() +
				     ", Path: " + file.getFullPath() + ", Size: " + file.getFileSize());
        RequestBody body = new Builder()
    		.setType(MultipartBody.FORM)
            .addFormDataPart("filedata", file.getUuid(),
                RequestBody.create(MediaType.parse(file.getFileType()), new File(file.getFullPath())))
            .addFormDataPart("descriptiveName", file.getDescriptiveName())
            .addFormDataPart("mimetype", file.getFileType())
            .addFormDataPart("extension", file.getFileExtension())
            .addFormDataPart("filesize", Long.toString(file.getFileSize()))
            .addFormDataPart("creationdate", Long.toString(file.getCreationDateUnix()))
            .addFormDataPart("isPrivate", "" + file.isPrivate())
            .addFormDataPart("phoneID", IdentifierUtils.getDeviceIdentifier())
            .addFormDataPart("context", file.getContext().getJson().toString())
            .build();
    	
    	UploadQueueObject q_obj = new UploadQueueObject();
    	q_obj.fileId = file.getUuid();
    	q_obj.requestBody = body;
    	//Get upload location from node manager for this node
		q_obj.nodeIds = file.getStoredNodeIds();
		uploadQueue.put(q_obj.fileId, q_obj);
    }
    
    private void readPendingUploadsFromDb() {
    	try
    	{
	        List<VStoreFile> pending
	        	= FileDBHelper.getFilesToUpload(FileDBHelper.SORT_BY_DATE_DESCENDING);
	        for(VStoreFile f : pending)
	        {
	        	enqueueUpload(f);
	        }
            pending.size();
        }
    	catch(SQLException e)
    	{
            e.printStackTrace();
        }
    }

    /**
     * This method starts an upload thread for each file which was put into the queue with
     * {@link Uploader#enqueueUpload(VStoreFile)}.
     */
    public void startUploads() {
        for (UploadQueueObject qObj : uploadQueue.values())
        {
            //Ignore if upload is already running
			if (uploadThreads.containsKey(qObj.fileId)) continue;

			try
            {
                FileUploadThread t = new FileUploadThread(qObj);
                uploadThreads.put(qObj.fileId, t);
                t.start();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }
    
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(UploadDoneCompletelyEvent event) {
    	//Check if file id of the event matches
    	String fileId = event.getFileId(); 
    	if(fileId == null || fileId.equals("")) 
    	{ 
    		return; 
		}
    	
    	uploadThreads.remove(fileId);
    	uploadQueue.remove(fileId);
    	EventBus.getDefault().removeStickyEvent(event);
    	
    	if(uploadQueue.size() == 0)
    	{
    		readPendingUploadsFromDb();
    		//Check if size is still 0 (meaning no pending upload left in database)
    		if(uploadQueue.size() == 0)
    		{
    			EventBus.getDefault().post(new AllUploadsDoneEvent());
    		}
    	}
    }


}
