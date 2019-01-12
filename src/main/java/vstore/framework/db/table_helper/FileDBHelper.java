package vstore.framework.db.table_helper;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import vstore.framework.db.DBHelper;
import vstore.framework.db.DBSchema;
import vstore.framework.db.row_wrapper.FileRowWrapper;
import vstore.framework.file.VStoreFile;


public class FileDBHelper {
    public static final String SORT_BY_DATE_DESCENDING = DBSchema.FilesTable.DATE_CREATION + " DESC";
    public static final String SORT_BY_DATE_ASCENDING = DBSchema.FilesTable.DATE_CREATION + " ASC";

    private FileDBHelper() {}

    /**
     * Inserts the given file into the local sqlite database.
     * 
     * @param f The file.
     * @throws SQLException in case something went wrong during the query.
     */
    public static void insertFile(VStoreFile f) throws SQLException {
        String sql = "INSERT INTO " 
        		+ DBSchema.FilesTable.__NAME + " " 
        		+ "(" + getFieldList(false) + ") "
				+ "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

        String ctx;
        if(f.getContext() == null || f.getContext().getJson() == null) {
            ctx = "";
        }
        else {
            ctx = f.getContext().getJson().toString();
        }


        try(PreparedStatement pstmt = DBHelper.get().getConnection().prepareStatement(sql)) {
            pstmt.setString(1, f.getUuid());
            pstmt.setString(2, f.getMD5Hash());
            pstmt.setString(3, f.getDescriptiveName());
            pstmt.setString(4, f.getFileType());
            pstmt.setString(5, f.getFileExtension());
            pstmt.setLong(6, f.getCreationDateUnix());
            pstmt.setLong(7, f.getFileSize());
            pstmt.setBoolean(8, f.isUploadPending());
            pstmt.setBoolean(9, f.isUploadFailed());
            pstmt.setBoolean(10, f.isPrivate());
            pstmt.setString(11, f.getMainNodeId());
            pstmt.setString(12, f.getStoredNodeIdsJson().toJSONString());
            pstmt.setString(13, ctx);
            pstmt.setBoolean(14, f.isDeletePending());

            pstmt.execute();
            pstmt.close();
        }
    }

    /**
     * Reads a file from the local sqlite database.
     * 
     * @param uuid The UUID of the file to read from the database.
     * @return The file, if available. Null, if no file was found for the given UUID.
     * @throws SQLException in case something went wrong during the query.
     */
    public static VStoreFile getFile(String uuid) throws SQLException {
    	String sql = "SELECT * FROM "
    			+ DBSchema.FilesTable.__NAME
    			+ " WHERE " + DBSchema.FilesTable.UUID + " = ? ";
    	
    	VStoreFile f = null;
    	try(PreparedStatement pstmt = DBHelper.get().getConnection().prepareStatement(sql)) {

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
    }

    /**
     * Returns a list of the files currently in the local database.
     * Will not return files marked for deletion.
     * 
     * @param ordering The ordering of the list.
     * @return A list containing the files.
     * @throws SQLException in case something went wrong during the query.
     */
    public static List<VStoreFile> getFiles(String ordering) throws SQLException {
		List<VStoreFile> files = new ArrayList<>();
		String sql = "SELECT * FROM "
    			+ DBSchema.FilesTable.__NAME
    			+ " WHERE " + DBSchema.FilesTable.DELETE_PENDING + " = ? "
    			+ "ORDER BY " + ordering;

		try(PreparedStatement pstmt = DBHelper.get().getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, 0);
            ResultSet rs;
            try {
                rs = pstmt.executeQuery();
            }
            catch(NullPointerException e) {
                e.printStackTrace();
                return files;
            }
            while (rs.next())
            {
                FileRowWrapper wrp = new FileRowWrapper(rs);
                files.add(wrp.getFile());
            }
            pstmt.close();

            return files;
        }
    }
    
    /**
     * Updates flags of a file in the database.
     * 
     * @param f The file to update
     * @param uploadPending Set this to false, if an upload is not pending anymore.
     * @param uploadFailed Set this to true, if an upload failed because of a server error.
     * @param deletePending Set this to true, if the file should be marked for deletion.
     * @throws SQLException in case something went wrong during the query.
     */
    public static void updateFile(VStoreFile f,
    		boolean uploadPending, 
    		boolean uploadFailed, 
    		boolean deletePending) throws SQLException {
        f.setUploadPending(uploadPending);
        f.setUploadFailed(uploadFailed);
        f.setDeletePending(deletePending);
        
        String sql = "UPDATE " + DBSchema.FilesTable.__NAME + " "
     		   + "SET " + getFieldList(true)
     		   + " WHERE " + DBSchema.FilesTable.UUID + " = ? ";

        try(PreparedStatement pstmt = DBHelper.get().getConnection().prepareStatement(sql)) {
            pstmt.setString(1, f.getUuid());
            pstmt.setString(2, f.getMD5Hash());
            pstmt.setString(3, f.getDescriptiveName());
            pstmt.setString(4, f.getFileType());
            pstmt.setString(5, f.getFileExtension());
            pstmt.setLong(6, f.getCreationDateUnix());
            pstmt.setLong(7, f.getFileSize());
            pstmt.setBoolean(8, f.isUploadPending());
            pstmt.setBoolean(9, f.isUploadFailed());
            pstmt.setBoolean(10, f.isPrivate());
            pstmt.setString(11, f.getMainNodeId());
            pstmt.setString(12, f.getStoredNodeIdsJson().toJSONString());
            pstmt.setString(13, f.getContext().getJson().toString());
            pstmt.setBoolean(14, f.isDeletePending());
            pstmt.setString(15, f.getUuid());
            pstmt.executeUpdate();
            pstmt.close();
        }
    }

    /**
     * Updates the upload pending flag of a file with the given id in the database.
     * 
     * @param fileUUID The file id for which to update the fields.
     * @param uploadPending Set this to false, if the update is not pending anymore.
     * @param uploadFailed Set this to true, if the upload failed because of a server error.
     * @param deletePending Set this to true, if the file should be marked for deletion.
     * @throws SQLException in case something went wrong during the query.
     */
    public static void updateFile(String fileUUID,
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
     * @throws SQLException in case something went wrong during the query.
     */
    public static List<VStoreFile> getFilesToUpload(String ordering) throws SQLException {
    	String sql = "SELECT * FROM "
    			+ DBSchema.FilesTable.__NAME
    			+ " WHERE " + DBSchema.FilesTable.UPLOAD_PENDING + "= ? AND "
                + DBSchema.FilesTable.UPLOAD_FAILED + " = ? AND "
                + DBSchema.FilesTable.DELETE_PENDING + " = ? "
    			+ "ORDER BY " + ordering;

        List<VStoreFile> files = new ArrayList<>();
		try(PreparedStatement pstmt = DBHelper.get().getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, 1);
            pstmt.setInt(2, 0);
            pstmt.setInt(3, 0);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                FileRowWrapper wrp = new FileRowWrapper(rs);
                files.add(wrp.getFile());
            }
            pstmt.close();
        }
        return files;
    }

    /**
     * @return The number of files that are still to be uploaded by the framework.
     *
     * @throws SQLException in case something went wrong during the query.
     */
    public static int getNumberOfFilesToUpload() throws SQLException {
    	String sql = "SELECT COUNT(*) AS rowcount FROM "
    			+ DBSchema.FilesTable.__NAME
    			+ " WHERE " + DBSchema.FilesTable.UPLOAD_PENDING + "= ? AND "
                + DBSchema.FilesTable.UPLOAD_FAILED + " = ? AND "
                + DBSchema.FilesTable.DELETE_PENDING + " = ? ";

        try(PreparedStatement pstmt = DBHelper.get().getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, 1);
            pstmt.setInt(2, 0);
            pstmt.setInt(3, 0);
            ResultSet rs = pstmt.executeQuery();
            if (rs.first()) {
                return rs.getInt("rowcount");
            }
        }
		return 0;
    }

    /**
     * Reads only files from the local sqlite database that are flagged as private.
	 *
     * @param ordering The ordering of the list.
     * @return A list of private files.
     */
    public static List<VStoreFile> getPrivateFilesOnly( String ordering) {
    	String sql = "SELECT * FROM " + DBSchema.FilesTable.__NAME
    			+ " WHERE " + DBSchema.FilesTable.PRIVATE + " = ? AND " +
                DBSchema.FilesTable.DELETE_PENDING + " = ? "
    			+ "ORDER BY " + ordering;

        List<VStoreFile> files = new ArrayList<>();
		try(PreparedStatement pstmt = DBHelper.get().getConnection().prepareStatement(sql))
		{
	    	pstmt.setInt(1, 1);
	    	pstmt.setInt(2, 0);
	    	
	    	ResultSet rs  = pstmt.executeQuery();
	    	while (rs.next()) 
	    	{
	    		FileRowWrapper wrp = new FileRowWrapper(rs);
	    		files.add(wrp.getFile());
	    	}
    	
		} catch (SQLException e) {
			e.printStackTrace();
		}
    	return files;
    }

    /**
     * Reads only those files from the database that have the DELETE_PENDING flag set.
     *
     * @return A list of files that are still pending to be deleted.
     */
    public static List<VStoreFile> getFilesForDeletion() {
    	String sql = "SELECT * FROM " + DBSchema.FilesTable.__NAME + " "
    			+ "WHERE " + DBSchema.FilesTable.DELETE_PENDING + " = ? "
    			+ "ORDER BY " + SORT_BY_DATE_DESCENDING;

        List<VStoreFile> files = new ArrayList<>();
		try(PreparedStatement pstmt = DBHelper.get().getConnection().prepareStatement(sql))
		{
	    	pstmt.setInt(1, 1);

	    	ResultSet rs  = pstmt.executeQuery();
	    	while (rs.next())
	    	{
	    		FileRowWrapper wrp = new FileRowWrapper(rs);
	    		files.add(wrp.getFile());
	    	}

		} catch (SQLException e) {
			e.printStackTrace();
		}
    	
    	return files;
    }

    /**
     * Deletes the file with the given UUID from the database
     * @param uuid The UUID of the file to delete.
     */
    public static void deleteFile(String uuid) {
    	String sql = "DELETE FROM "
    				+ DBSchema.FilesTable.__NAME 
    				+ " WHERE " + DBSchema.FilesTable.UUID + " = ?";
    	try(PreparedStatement pstmt = DBHelper.get().getConnection().prepareStatement(sql))
		{
	    	pstmt.setString(1, uuid);
	    	pstmt.execute();
		} 
    	catch (SQLException e)
    	{
			e.printStackTrace();
		}
    }

    /**
     * Sets the DELETE_PENDING flag in the database. The background service will then try to
     * delete all files marked as DELETE_PENDING in the background.
     * @param uuid The UUID of the file to delete.
     * @throws SQLException in case something went wrong during the query.
     */
    public static void markForDeletion(String uuid) throws SQLException {
    	updateFile(uuid, false, false, true);
    }

    /**
     * Checks if the file with the given UUID was published by this phone.
     * @param uuid The UUID to check
     * @return True, if this phone uploaded the file.
     * @throws SQLException in case something went wrong during the query.
     */
    public static boolean isMyFile( String uuid) throws SQLException {
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
    public static boolean isAlreadyStored(String md5_hash) {
    	String sql = "SELECT COUNT(*) AS rowcount FROM " 
    			+ DBSchema.FilesTable.__NAME + " WHERE "
    			+ DBSchema.FilesTable.MD5_HASH + " = ? ";

		try(PreparedStatement pstmt = DBHelper.get().getConnection().prepareStatement(sql))
		{
	    	pstmt.setString(1, md5_hash);
	    	
	    	ResultSet rs  = pstmt.executeQuery();
            if(!rs.next()) {
                return false;
            }
	    	if(rs.getInt("rowcount") > 0) 
	    	{
	            return true;
	        }
		} 
        catch (SQLException e)
        {
			e.printStackTrace();
		}
    	
        return false;
    }

    /**
     * Workaround for simplifying the sql field list
     * @param q True, if a questionmark should be included in the list (for prepared statements)
     * @return True: " = ?, "
     *         False: ", "
     */
    private static String q(boolean q) {
        return ((q) ? " = ?, " : ", ");
    }
    private static String getFieldList(boolean q) {
        String returnStr = DBSchema.FilesTable.UUID + q(q)
                + DBSchema.FilesTable.MD5_HASH + q(q)
                + DBSchema.FilesTable.DESCRIPTIVE_NAME + q(q)
                + DBSchema.FilesTable.MIME + q(q)
                + DBSchema.FilesTable.EXTENSION + q(q)
                + DBSchema.FilesTable.DATE_CREATION + q(q)
                + DBSchema.FilesTable.SIZE + q(q)
                + DBSchema.FilesTable.UPLOAD_PENDING + q(q)
                + DBSchema.FilesTable.UPLOAD_FAILED + q(q)
                + DBSchema.FilesTable.PRIVATE + q(q)
                + DBSchema.FilesTable.NODEUUID + q(q)
                + DBSchema.FilesTable.STORED_NODES + q(q)
                + DBSchema.FilesTable.CONTEXTJSON + q(q);

        returnStr += DBSchema.FilesTable.DELETE_PENDING + ((q) ? " = ?" : "");
        return returnStr;
    }
}
