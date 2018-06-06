package vstore.framework.communication;

import java.sql.SQLException;

import vstore.framework.communication.download.Downloader;
import vstore.framework.communication.download.events.MetadataEvent;
import vstore.framework.communication.threads.DeleteFilesThread;
import vstore.framework.db.DBResultOrdering;
import vstore.framework.db.table_helper.FileDBHelper;
import vstore.framework.exceptions.DatabaseException;
import vstore.framework.file.FileManager;
import vstore.framework.file.events.FilesReadyEvent;

public class CommunicationManager {
	
	private static CommunicationManager instance;
	private static DeleteFilesThread thDelete;
	
	private CommunicationManager() {
		
	}

	public static void initialize() {
        if(instance == null) {
            instance = new CommunicationManager();
        }
    }
	
	public static synchronized CommunicationManager get() {
		initialize();
		return instance;
	}
	
	public void runDeletions() {
		if(thDelete != null && thDelete.isAlive()) { return; }
		
		//Start a new thread for deletion of files
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
		FileManager.getInstance().getLocalFileList(resultOrdering, true, false);
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
            FileDBHelper dbHelper = new FileDBHelper();
            return dbHelper.getNumberOfFilesToUpload();
        }
        catch(DatabaseException | SQLException e)
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
