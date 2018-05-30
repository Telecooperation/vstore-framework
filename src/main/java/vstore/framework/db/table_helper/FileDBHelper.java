package vstore.framework.db.table_helper;

import java.util.ArrayList;
import java.util.List;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import vstore.framework.db.DBHelper;
import vstore.framework.db.DBSchema;
import vstore.framework.db.row_wrapper.FileRowWrapper;
import vstore.framework.exceptions.DatabaseException;
import vstore.framework.file.VStoreFile;


public class FileDBHelper {
    public static final String SORT_BY_DATE_DESCENDING = DBSchema.FilesTable.DATE_CREATION + " DESC";
    public static final String SORT_BY_DATE_ASCENDING = DBSchema.FilesTable.DATE_CREATION + " ASC";

    private DBHelper mDbHelper;

    /**
     * Creates a new FileDBHelper instance.
     * @throws DatabaseException 
     */
    public FileDBHelper() throws DatabaseException {
    	mDbHelper = DBHelper.getInstance();
    }

    /**
     * Inserts the given file into the local sqlite database.
     * 
     * @param f The file.
     * @throws SQLException 
     */
    public void insertFile(VStoreFile f) throws SQLException {
        String sql = "INSERT INTO " 
        		+ DBSchema.FilesTable.__NAME + " " 
        		+ getInsertClause(f);
        
        Statement stmt = mDbHelper.getConnection().createStatement();
        stmt.executeQuery(sql);
        stmt.close();
    }

    /**
     * Reads a file from the local sqlite database.
     * 
     * @param uuid The UUID of the file to read from the database.
     * @return The file, if available. Null, if no file was found for the given UUID.
     * @throws SQLException 
     */
    public VStoreFile getFile(String uuid) throws SQLException {
    	String sql = "SELECT * FROM "
    			+ DBSchema.FilesTable.__NAME
    			+ " WHERE " + DBSchema.FilesTable.UUID + " = ? ";
    	
    	VStoreFile f = null;
    	PreparedStatement pstmt = mDbHelper.getConnection().prepareStatement(sql);
		
    	pstmt.setString(1, uuid);
    	ResultSet rs  = pstmt.executeQuery();
    	
    	while (rs.next()) 
    	{
    		FileRowWrapper wrp = new FileRowWrapper(rs);
    		f = wrp.getFile();
    		break;
    	}
    	pstmt.close();
		
        return f;
    }

    /**
     * Returns a list of the files currently in the local database.
     * Will not return files marked for deletion.
     * 
     * @param ordering The ordering of the list.
     * @return A list containing the files.
     * @throws SQLException 
     */
    public List<VStoreFile> getFiles(String ordering) throws SQLException {
    	String sql = "SELECT * FROM "
    			+ DBSchema.FilesTable.__NAME
    			+ " WHERE " + DBSchema.FilesTable.DELETE_PENDING + " = ? "
    			+ "ORDER BY " + ordering;

    	List<VStoreFile> files = new ArrayList<>();
		PreparedStatement pstmt = mDbHelper.getConnection().prepareStatement(sql);
		pstmt.setInt(1, 0);
    	ResultSet rs  = pstmt.executeQuery();
    	
    	while (rs.next()) 
    	{
    		FileRowWrapper wrp = new FileRowWrapper(rs);
    		files.add(wrp.getFile());
    	}
    	pstmt.close();
    	
    	return files;
    }
    
    /**
     * Updates flags of a file in the database.
     * 
     * @param f The file to update
     * @param uploadPending Set this to false, if an upload is not pending anymore.
     * @param uploadFailed Set this to true, if an upload failed because of a server error.
     * @param deletePending Set this to true, if the file should be marked for deletion.
     * @throws SQLException 
     */
    public void updateFile(VStoreFile f, 
    		boolean uploadPending, 
    		boolean uploadFailed, 
    		boolean deletePending) throws SQLException {
        f.setUploadPending(uploadPending);
        f.setUploadFailed(uploadFailed);
        f.setDeletePending(deletePending);
        
        String sql = "UPDATE " + DBSchema.FilesTable.__NAME + " "
     		   + getSetClause(f)
     		   + " WHERE " + DBSchema.FilesTable.UUID + " = ? ";
        
        PreparedStatement pstmt = mDbHelper.getConnection().prepareStatement(sql); 
    	pstmt.setString(1, f.getUUID());
    	pstmt.executeUpdate();
    	pstmt.close();
    }

    /**
     * Updates the upload pending flag of a file with the given id in the database.
     * 
     * @param fileUUID The file id for which to update the fields.
     * @param uploadPending Set this to false, if the update is not pending anymore.
     * @param uploadFailed Set this to true, if the upload failed because of a server error.
     * @throws SQLException 
     */
    public void updateFile(String fileUUID, 
    		boolean uploadPending, 
    		boolean uploadFailed, 
    		boolean deletePending) throws SQLException {
        VStoreFile f = getFile(fileUUID);
        updateFile(f, uploadPending, uploadFailed, deletePending);
    }

    /**
     * Returns a list of files that are still in the 'upload pending' state.
     * 
     * @param ordering The ordering of the list.
     * @return A list containing the files.
     * @throws SQLException 
     */
    public List<VStoreFile> getFilesToUpload(String ordering) throws SQLException {
    	String sql = "SELECT * FROM "
    			+ DBSchema.FilesTable.__NAME
    			+ " WHERE " + DBSchema.FilesTable.UPLOAD_PENDING + "= ? AND "
                + DBSchema.FilesTable.UPLOAD_FAILED + " = ? AND "
                + DBSchema.FilesTable.DELETE_PENDING + " = ? "
    			+ "ORDER BY " + ordering;

        List<VStoreFile> files = new ArrayList<>();
		PreparedStatement pstmt = mDbHelper.getConnection().prepareStatement(sql);
		pstmt.setInt(1, 1);
    	pstmt.setInt(1, 0);
    	pstmt.setInt(1, 0);
    	
    	ResultSet rs  = pstmt.executeQuery();
    	while (rs.next()) 
    	{
    		FileRowWrapper wrp = new FileRowWrapper(rs);
    		files.add(wrp.getFile());
    	}
    	pstmt.close();
    	
    	return files;
    }

    /**
     * @return The number of files that are still to be uploaded by the framework.
     */
    public int getNumberOfFilesToUpload() throws SQLException {
    	String sql = "SELECT COUNT(*) AS rowcount FROM "
    			+ DBSchema.FilesTable.__NAME
    			+ " WHERE " + DBSchema.FilesTable.UPLOAD_PENDING + "= ? AND "
                + DBSchema.FilesTable.UPLOAD_FAILED + " = ? AND "
                + DBSchema.FilesTable.DELETE_PENDING + " = ? ";

        try(PreparedStatement pstmt = mDbHelper.getConnection().prepareStatement(sql))
		{
	    	pstmt.setInt(1, 1);
	    	pstmt.setInt(1, 0);
	    	pstmt.setInt(1, 0);
	    	
	    	ResultSet rs  = pstmt.executeQuery();
	    	return rs.getInt("rowcount");
		} 
    }

    /**
     * Reads only files from the local sqlite database that are flagged as private.
     * @param c The Android context
     * @param ordering The ordering of the list.
     * @return A list of private files.
     */
    public List<VStoreFile> getPrivateFilesOnly( String ordering) {
    	String sql = "SELECT * FROM " + DBSchema.FilesTable.__NAME
    			+ " WHERE " + DBSchema.FilesTable.PRIVATE + " = ? AND " +
                DBSchema.FilesTable.DELETE_PENDING + " = ? "
    			+ "ORDER BY " + ordering;

        List<VStoreFile> files = new ArrayList<>();
		try(PreparedStatement pstmt = mDbHelper.getConnection().prepareStatement(sql))
		{
	    	pstmt.setInt(1, 1);
	    	pstmt.setInt(1, 0);
	    	
	    	ResultSet rs  = pstmt.executeQuery();
	    	while (rs.next()) 
	    	{
	    		FileRowWrapper wrp = new FileRowWrapper(rs);
	    		files.add(wrp.getFile());
	    	}
    	
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	return files;
    }

    /**
     * Reads only those files from the database that have the DELETE_PENDING flag set.
     * @param c The Android context.
     * @return A list of files that are still pending to be deleted.
     */
    public List<VStoreFile> getFilesForDeletion() {
    	String sql = "SELECT * FROM " + DBSchema.FilesTable.__NAME + " "
    			+ DBSchema.FilesTable.DELETE_PENDING + " = ? "
    			+ "ORDER BY " + SORT_BY_DATE_DESCENDING;

        List<VStoreFile> files = new ArrayList<>();
		try(PreparedStatement pstmt = mDbHelper.getConnection().prepareStatement(sql))
		{
	    	pstmt.setInt(1, 1);
	    	
	    	ResultSet rs  = pstmt.executeQuery();
	    	while (rs.next()) 
	    	{
	    		FileRowWrapper wrp = new FileRowWrapper(rs);
	    		files.add(wrp.getFile());
	    	}
    	
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	return files;
    }

    /**
     * Deletes the file with the given UUID from the database
     * @param uuid The UUID of the file to delete.
     */
    public void deleteFile(String uuid) {
    	String sql = "DELETE FROM "
    				+ DBSchema.FilesTable.__NAME 
    				+ "WHERE " + DBSchema.FilesTable.UUID + " = ?";
    	try(PreparedStatement pstmt = mDbHelper.getConnection().prepareStatement(sql))
		{
	    	pstmt.setString(1, uuid);
	    	pstmt.executeQuery();
		} 
    	catch (SQLException e) 
    	{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    /**
     * Sets the DELETE_PENDING flag in the database. The background service will then try to
     * delete all files marked as DELETE_PENDING in the background.
     * @param uuid The UUID of the file to delete.
     * @throws SQLException 
     */
    public void markForDeletion(String uuid) throws SQLException {
    	updateFile(uuid, false, false, true);
    }

    /**
     * Checks if the file with the given UUID was published by this phone.
     * @param uuid The UUID to check
     * @return True, if this phone uploaded the file.
     * @throws SQLException 
     */
    public boolean isMyFile( String uuid) throws SQLException {
        if(getFile(uuid) != null) {
            return true;
        }
        return false;
    }

    /**
     * Checks the given md5 hash against the database.
     * @param md5_hash The md5 hash to search for in the database.
     * @return True, if a file with the hash is already in the database.
     */
    public boolean isAlreadyStored(String md5_hash) {
    	String sql = "SELECT COUNT(*) AS rowcount FROM " 
    			+ DBSchema.FilesTable.__NAME + " "
    			+ DBSchema.FilesTable.MD5_HASH + " = ? ";

        try(PreparedStatement pstmt = mDbHelper.getConnection().prepareStatement(sql))
		{
	    	pstmt.setString(1, md5_hash);
	    	
	    	ResultSet rs  = pstmt.executeQuery();
	    	if(rs.getInt("rowcount") > 0) 
	    	{
	            return true;
	        }
		} 
        catch (SQLException e) 
        {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
        return false;
    }
    
    /**
     * @return Returns the content values of this object for the database.
     */
    public String getSetClause(VStoreFile f) {
        String set = "SET "
        + DBSchema.FilesTable.UUID + " = " + f.getUUID()
        + DBSchema.FilesTable.MD5_HASH + " = " + f.getMD5Hash()
        + DBSchema.FilesTable.DESCRIPTIVE_NAME + " = " + f.getDescriptiveName()
        + DBSchema.FilesTable.MIME + " = " + f.getFileType()
        + DBSchema.FilesTable.EXTENSION + " = " + f.getFileExtension()
        + DBSchema.FilesTable.DATE_CREATION + " = " + f.getCreationDateUnix()
        + DBSchema.FilesTable.SIZE + " = " + f.getFileSize()
        + DBSchema.FilesTable.UPLOAD_PENDING + " = " + f.isUploadPending()
        + DBSchema.FilesTable.UPLOAD_FAILED + " = " + f.isUploadFailed()
        + DBSchema.FilesTable.PRIVATE + " = " + f.isPrivate()
        + DBSchema.FilesTable.NODEUUID + " = " + f.getNodeID()
        + DBSchema.FilesTable.CONTEXTJSON + " = " + f.getContext().getJson().toString()
        + DBSchema.FilesTable.DELETE_PENDING + " = " + f.isDeletePending();
        return set;
    }
    
    public String getInsertClause(VStoreFile f) {
        String insert = "("
		+ DBSchema.FilesTable.UUID + ", "
        + DBSchema.FilesTable.MD5_HASH + ", "
        + DBSchema.FilesTable.DESCRIPTIVE_NAME + ", " 
        + DBSchema.FilesTable.MIME + ", "
        + DBSchema.FilesTable.EXTENSION + ", " 
        + DBSchema.FilesTable.DATE_CREATION + ", " 
        + DBSchema.FilesTable.SIZE + ", "
        + DBSchema.FilesTable.UPLOAD_PENDING + ", " 
        + DBSchema.FilesTable.UPLOAD_FAILED + ", " 
        + DBSchema.FilesTable.PRIVATE + ", "
        + DBSchema.FilesTable.NODEUUID + ", " 
        + DBSchema.FilesTable.CONTEXTJSON + ", " 
        + DBSchema.FilesTable.DELETE_PENDING + ") " 
        + "VALUES ("
        + f.getUUID() + ", "
        + f.getMD5Hash() + ", "
        + f.getDescriptiveName() + ", "
        + f.getFileType() + ", "
		+ f.getFileExtension() + ", "
		+ f.getCreationDateUnix() + ", "
		+ f.getFileSize() + ", "
		+ f.isUploadPending() + ", "
		+ f.isUploadFailed() + ", "
		+ f.isPrivate() + ", "
		+ f.getNodeID() + ", "
		+ f.getContext().getJson().toString() + ", "
		+ f.isDeletePending() + ")";
        
        return insert;
    }
}
