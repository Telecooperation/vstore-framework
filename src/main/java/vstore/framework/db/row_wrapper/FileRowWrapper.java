package vstore.framework.db.row_wrapper;

import java.io.FileNotFoundException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import vstore.framework.db.DBSchema;
import vstore.framework.file.FileManager;
import vstore.framework.file.VStoreFile;

/**
 * A wrapper class for the database cursor.
 */
public class FileRowWrapper {
    
	private ResultSet mRes;
	
	public FileRowWrapper(ResultSet res) {
        mRes = res;
    }

    /**
     * Returns a vStoreFile object for the result set given in the constructor.
     * 
     * @return The vStoreFile object containing information about a file stored in the framework
     */
    public VStoreFile getFile() {
    	
    	VStoreFile f = null;
    	
    	try 
    	{
	        String uuid = mRes.getString(DBSchema.FilesTable.UUID);
	        String md5 = mRes.getString(DBSchema.FilesTable.MD5_HASH);
	        String descriptiveName = mRes.getString(DBSchema.FilesTable.DESCRIPTIVE_NAME);
	        String mime = mRes.getString(DBSchema.FilesTable.MIME);
	        String extension = mRes.getString(DBSchema.FilesTable.EXTENSION);
	        long dateCreation = mRes.getLong(DBSchema.FilesTable.DATE_CREATION);
	        //long size = mRes.getLong(DBSchema.FilesTable.SIZE);
	        boolean uploadPending = (mRes.getInt(DBSchema.FilesTable.UPLOAD_PENDING) == 1);
	        boolean isPrivate = (mRes.getInt(DBSchema.FilesTable.PRIVATE) == 1);
	        String nodeUUID = mRes.getString(DBSchema.FilesTable.NODEUUID);
	        String contextJson = mRes.getString(DBSchema.FilesTable.CONTEXTJSON);
	        boolean deletePending = (mRes.getInt(DBSchema.FilesTable.DELETE_PENDING) == 1);
	    	
	        try 
	        {
	            f = new VStoreFile(uuid,
	                    descriptiveName,
	                    FileManager.getStoredFilesDir().getAbsolutePath(),
	                    mime,
	                    extension,
	                    new Date(dateCreation),
	                    uploadPending,
	                    isPrivate);
	            f.setNodeUUID(nodeUUID);
	            f.setContextFromJson(contextJson);
	            f.setMD5Hash(md5);
	            f.setDeletePending(deletePending);
	        } 
	        catch(FileNotFoundException e) 
	        {
	            f = null;
	        }
    	}
    	catch (SQLException e) 
    	{
			// TODO: handle exception
    		e.printStackTrace();
		}
    	
        return f;
    }
}
