package vstore.framework.communication.upload;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.MultipartBody.Builder;
import okhttp3.RequestBody;
import vstore.framework.communication.upload.events.AllUploadsDoneEvent;
import vstore.framework.communication.upload.events.SingleUploadDoneEvent;
import vstore.framework.communication.upload.threads.FileUploadThread;
import vstore.framework.db.table_helper.FileDBHelper;
import vstore.framework.exceptions.DatabaseException;
import vstore.framework.file.VStoreFile;
import vstore.framework.node.NodeManager;
import vstore.framework.utils.FrameworkUtils;

/**
 * This class wraps functions for controlling and managing uploads.
 * Each upload is started in an own thread, thus multiple uploads
 * can occur in parallel.
 */
public class Uploader {
	public static final MediaType JSON
		= MediaType.parse("application/json; charset=utf-8");
	
	private static Uploader mInstance;
	
	HashMap<String, UploadQueueObject> uploadQueue;
	HashMap<String, Thread> uploadThreads;
	
	private Uploader() {
		uploadQueue = new HashMap<String, UploadQueueObject>();
		readPendingUploadsFromDb();
		uploadThreads = new HashMap<>();
		
        EventBus.getDefault().register(this);
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
	 */
    public void enqueueUpload(VStoreFile file) {
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
            .addFormDataPart("phoneID", FrameworkUtils.getDeviceIdentifier())
            .addFormDataPart("context", file.getContext().getJson().toString())
            .build();
    	
    	UploadQueueObject q_obj = new UploadQueueObject();
    	q_obj.fileId = file.getUUID();
    	q_obj.requestBody = body;
    	//Get upload location from node manager for this node
    	q_obj.uploadUrl = NodeManager.getInstance().getNode(file.getNodeID()).getUploadUri();
    	
		uploadQueue.put(q_obj.fileId, q_obj);
    }
    
    private int readPendingUploadsFromDb() {
    	try
    	{
	        FileDBHelper dbHelper = new FileDBHelper();
	        List<VStoreFile> pending 
	        	= dbHelper.getFilesToUpload(FileDBHelper.SORT_BY_DATE_DESCENDING);
	        for(VStoreFile f : pending)
	        {
	        	enqueueUpload(f);
	        }
	        return pending.size();
    	}
    	catch(DatabaseException | SQLException e) 
    	{
    		
    		return 0;
    	}
    }

    /**
     * This method starts an upload thread for each file put into the queue with
     * {@link Uploader#enqueueUpload()}.
     */
    public void startUploads() {
    	Iterator<UploadQueueObject> iter = uploadQueue.values().iterator();
    	while(iter.hasNext())
		{
    		UploadQueueObject qObj = iter.next();
    		//Ignore if upload is already running
    		if(uploadThreads.containsKey(qObj.fileId)) continue;
    		
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
    public void onMessageEvent(SingleUploadDoneEvent event) {
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
