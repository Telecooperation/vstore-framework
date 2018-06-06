package vstore.framework.communication.upload;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

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
	 * Gets the Preferences node
	 * @return The preferences node for the upload list
	 */
	private static Preferences getPrefs() {
		return Preferences.userNodeForPackage(PersistentUploadList.class);
	}
	
	/**
	 * @return A list containing the UUIDs of current uploads.
	 *         In case of an error, an empty list is returned.   
	 */
	public static synchronized List<String> getCurrentUploads() {
		
		List<String> currentUploads;
		
		//Get all uploads from the pref
		try 
		{
			String[] list = getPrefs().keys();
			currentUploads = new ArrayList<>(Arrays.asList(list));
		} 
		catch (BackingStoreException e) 
		{
			e.printStackTrace();
			return new ArrayList<>();
		}
				
		return currentUploads;
	}
	
	/**
     * Determines, if a file with the given UUID is currently uploading.
     * 
     * @param uuid The UUID of the file.
     * @return True, if the file is currently uploading.
     */
	public static synchronized boolean isFileUploading(String uuid) {
		if(uuid == null || uuid.equals("")) return false;
		return !getPrefs().get(uuid, "default").equals("default");
	}
	
	/**
     * Adds the given file to the uploads list.
     * @param uuid The uuid to add.
     */
    public static synchronized void addFileUploading(String uuid) {
		if(uuid == null || uuid.equals("")) return;
    	getPrefs().put(uuid, "is_uploading");
    	try 
    	{
			getPrefs().flush();
		} 
    	catch (BackingStoreException e) 
    	{
			e.printStackTrace();
		}
    }
    
    /**
     * Deletes the file uuid from the current uploads list.
     * @param uuid The uuid to delete.
     */
    public static synchronized void deleteFileUploading(String uuid) {
		if(uuid == null || uuid.equals("")) return;
    	getPrefs().remove(uuid);
    	try 
    	{
			getPrefs().flush();
		} 
    	catch (BackingStoreException e) 
    	{
			e.printStackTrace();
		}
    }

}
