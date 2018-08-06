package vstore.framework.communication;

import java.net.URL;
import java.sql.SQLException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import vstore.framework.communication.download.Downloader;
import vstore.framework.communication.download.events.MetadataEvent;
import vstore.framework.communication.master_node.file_node_mapping.PostFileNodeMappingCallable;
import vstore.framework.communication.threads.DeleteFilesThread;
import vstore.framework.db.DBResultOrdering;
import vstore.framework.db.table_helper.FileDBHelper;
import vstore.framework.file.FileManager;
import vstore.framework.file.events.FilesReadyEvent;

public class CommunicationManager {
	private static CommunicationManager instance;
	private static DeleteFilesThread thDelete;

	private URL masterNodeAddress;

	private CommunicationManager(URL masterNodeAddress) {
		this.masterNodeAddress = masterNodeAddress;
	}

	public static void initialize(URL masterNodeAddress) {
        if(instance == null) {
            instance = new CommunicationManager(masterNodeAddress);
        }
    }

    /**
     * @return Gets the instance of the communication manager.
     */
	public static synchronized CommunicationManager get() {
		return instance;
	}

	public URL getMasterNodeAddress() {
	    return masterNodeAddress;
    }

    /**
     * Posts the given file-node mapping to the master peer.
     *
     * @param file_id The file id
     * @param node_id The node id where the file was stored.
     * @return True, if request was successful. False if not.
     */
	public static boolean postFileNodeMapping(String file_id, String node_id) {
        ExecutorService executor = Executors.newFixedThreadPool(1);
        PostFileNodeMappingCallable callable = new PostFileNodeMappingCallable(file_id, node_id);
        FutureTask<Boolean> futureTask = new FutureTask<>(callable);
        executor.execute(futureTask);

        //Wait for the node_id future
        try {
            return futureTask.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return false;
    }
	
	public void runDeletions() {
		if(thDelete != null && thDelete.isAlive()) { return; }
		
		//If thread is not alive: Start a new thread for deletion of files
		thDelete = new DeleteFilesThread();
		thDelete.start();
	}

	/**
	 * Fetches a list of all uploads currently pending.
	 * Results are published in the {@link FilesReadyEvent}.
	 *
	 * @param resultOrdering The order in which to return the files (e.g. newest or oldest first)
	 *                       (see {@link DBResultOrdering}).
	 */
	public void getPendingUploads(DBResultOrdering resultOrdering) {
		FileManager.get().getLocalFileList(resultOrdering, true, false);
	}

    /**
     * This method returns the number of files that are still to be uploaded
     * by the framework.
     *
     * @return The number of files to upload, or -1 if something went wrong.
     */
    public int getPendingUploadCount() {
        try
        {
            return FileDBHelper.getNumberOfFilesToUpload();
        }
        catch(SQLException e)
        {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * This method downloads metadata for the given file UUID. Will only work
     * if you are the owner of the file or if the file is public.
     * You will be notified via the {@link MetadataEvent} event when the
     * request has finished.
     *
     * @param fileUuid The file's UUID.
     */
    public void requestMetadata(final String fileUuid) {
        Downloader.downloadMetadata(fileUuid);
    }

    /**
     * Requests a thumbnail for the file with the given identifier.
     * @param fileId The identifier of the file.
     */
    public void requestThumbnail(String fileId) {
        Downloader.downloadThumbnail(fileId);
    }
}
