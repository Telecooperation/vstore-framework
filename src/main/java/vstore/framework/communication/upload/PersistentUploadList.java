package vstore.framework.communication.upload;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import vstore.framework.exceptions.VStoreException;
import vstore.framework.file.SimplePrefFile.PrefFile;
import vstore.framework.file.SimplePrefFile.PrefFileManager;

/**
 * Misuses a Java Preference file for implementing a simple and easy to use 
 * persistent list of current uploads.
 */
public class PersistentUploadList {
	
	/**
	 * Avoid instantiation
	 */
	private PersistentUploadList() {}
	
	/**
	 * Gets the Preferences file
	 * @return The preferences file for the upload list
	 */
	private static PrefFile getPrefs() {
		try {
			return PrefFileManager.getPrefFile("PersistentUploadList");
		} catch(VStoreException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * @return A list containing the UUIDs of current uploads.
	 *         In case of an error, an empty list is returned.   
	 */
	public static synchronized List<String> getCurrentUploads() {
		if(getPrefs() == null) { return new ArrayList<>(); }
		//Get all uploads from the pref
		String[] list = getPrefs().keys();
		return new ArrayList<>(Arrays.asList(list));
	}
	
	/**
     * Determines, if a file with the given UUID is currently uploading.
     * 
     * @param uuid The UUID of the file.
     * @return True, if the file is currently uploading.
     */
	public static synchronized boolean isFileUploading(String uuid) {
		if(uuid == null || uuid.equals("") || getPrefs() == null) return true;
		return !getPrefs().get(uuid, "default").equals("default");
	}
	
	/**
     * Adds the given file to the uploads list.
     * @param uuid The uuid to add.
     */
    public static synchronized void addFileUploading(String uuid) {
		if(uuid == null || uuid.equals("") || getPrefs() == null) return;
		getPrefs().put(uuid, "is_uploading");
    	getPrefs().flush();
    }
    
    /**
     * Deletes the file uuid from the current uploads list.
     * @param uuid The uuid to delete.
     */
    public static synchronized void deleteFileUploading(String uuid) {
		if(uuid == null || uuid.equals("") || getPrefs() == null) return;
		getPrefs().remove(uuid);
    	getPrefs().flush();
    }

}
