package vstore.framework.communication.download;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Misuses a Java Preference file for implementing a simple and easy to use 
 * persistent list of current downloads.
 */
public class PersistentDownloadList {
	
	/**
	 * Avoid instantiation
	 */
	private PersistentDownloadList() {}
	
	/**
	 * Gets the Preferences node
	 * @return The preferences node for the download list
	 */
	private static Preferences getPrefs() {
		return Preferences.userNodeForPackage(PersistentDownloadList.class);
	}
	
	/**
	 * @return A list containing the UUIDs of current downloads.
	 *         In case of an error, an empty list is returned.   
	 */
	public static synchronized List<String> getCurrentDownloads() {
		
		List<String> currentDownloads;
		
		//Get all downloads from the pref
		try 
		{
			String[] list = getPrefs().keys();
			currentDownloads = new ArrayList<>(Arrays.asList(list));
		} 
		catch (BackingStoreException e) 
		{
			e.printStackTrace();
			return new ArrayList<>();
		}
				
		return currentDownloads;
	}
	
	/**
     * Determines, if a file with the given UUID is currently downloading.
     * 
     * @param uuid The UUID of the file.
     * @return True, if the file is currently downloading.
     */
	public static synchronized boolean isFileDownloading(String uuid) {
		if(uuid == null || uuid.equals("")) return false;
		return !getPrefs().get(uuid, "default").equals("default");
	}
	
	/**
     * Adds the given file to the downloads list.
     * @param uuid The uuid to add.
     */
    public static synchronized void addFileDownloading(String uuid) {
		if(uuid == null || uuid.equals("")) return;
    	getPrefs().put(uuid, "is_downloading");
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
     * Deletes the file uuid from the current downloads list.
     * @param uuid The uuid to delete.
     */
    public static synchronized void deleteFileDownloading(String uuid) {
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
