package vstore.framework.file.threads;

import org.greenrobot.eventbus.EventBus;

import java.sql.SQLException;
import java.util.List;

import vstore.framework.db.table_helper.FileDBHelper;
import vstore.framework.file.VStoreFile;
import vstore.framework.file.events.FilesReadyEvent;

/**
 * This thread is responsible for asynchronously fetching files from the database.
 * If doing this in the main thread, things will take too long and app is not smooth anymore.
 */
public class FetchFilesFromDBThread extends Thread {
    public String mOrdering;
    public boolean mOnlyPendingFiles;
    public boolean mOnlyPrivateFiles;

    /**
     * Constructor for creating a new thread. 
	 *
     * @param resultOrdering The order that the results should be provided in. Must be either
     *                       {@link FileDBHelper#SORT_BY_DATE_DESCENDING} or
     *                       {@link FileDBHelper#SORT_BY_DATE_ASCENDING}.
     * @param onlyPending Set this to true, if only files should be fetched that have an upload pending.
     * @param onlyPrivate Set this to true, if only private files should be fetched from the database.
     */
    public FetchFilesFromDBThread(String resultOrdering, boolean onlyPending, boolean onlyPrivate) {
        mOrdering = resultOrdering;
        mOnlyPendingFiles = onlyPending;
        mOnlyPrivateFiles = onlyPrivate;
    }
   
    @Override
    public void run() {
    	List<VStoreFile> files;
        try
        {
	        if(mOnlyPendingFiles) 
	        {
				files = FileDBHelper.getFilesToUpload(mOrdering);
	        } 
	        else if(mOnlyPrivateFiles) 
	        {
	            files = FileDBHelper.getPrivateFilesOnly(mOrdering);
	        } 
	        else 
	        {
				files = FileDBHelper.getFiles(mOrdering);
	        }
        } 
        catch (SQLException e) 
        {
			e.printStackTrace();
			return;
		}
        
        EventBus.getDefault().postSticky(
                new FilesReadyEvent(files, mOnlyPendingFiles, mOnlyPrivateFiles));
    }
    
}
