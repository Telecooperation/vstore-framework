package vstore.framework.file.threads;

import java.sql.SQLException;
import java.util.List;

import org.greenrobot.eventbus.EventBus;

import vstore.framework.db.table_helper.FileDBHelper;
import vstore.framework.exceptions.DatabaseException;
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
    	FileDBHelper helper;
		try 
		{
			helper = new FileDBHelper();
		} 
		catch (DatabaseException e) 
		{
			e.printStackTrace();
			return;
		}
		
        List<VStoreFile> files;
        
        try 
        {
	        if(mOnlyPendingFiles) 
	        {
				files = helper.getFilesToUpload(mOrdering);
	        } 
	        else if(mOnlyPrivateFiles) 
	        {
	            files = helper.getPrivateFilesOnly(mOrdering);
	        } 
	        else 
	        {
				files = helper.getFiles(mOrdering);
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
