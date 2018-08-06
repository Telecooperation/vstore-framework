package vstore.framework.communication.download;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import vstore.framework.exceptions.VStoreException;
import vstore.framework.file.SimplePrefFile.PrefFile;
import vstore.framework.file.SimplePrefFile.PrefFileManager;

/**
 * Implements a simple and easy to use persistent list of current downloads.
 */
public class PersistentDownloadList {
	
	/**
	 * Avoid instantiation
	 */
	private PersistentDownloadList() {}
	
	/**
	 * Gets the Preferences file
	 * @return The preferences file for the download list
	 */
	private static PrefFile getPrefs() {
		try {
			return PrefFileManager.getPrefFile("PersistentDownloadList");
		}
		catch(VStoreException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * @return A list containing the UUIDs of current downloads.
	 *         In case of an error, an empty list is returned.   
	 */
	public static synchronized List<String> getCurrentDownloads() {
		//Get all downloads from the pref
		if(getPrefs() == null) { return new ArrayList<>(); }
		String[] list = getPrefs().keys();
		return new ArrayList<>(Arrays.asList(list));
	}
	
	/**
     * Determines, if a file with the given UUID is currently downloading.
     * 
     * @param uuid The UUID of the file.
     * @return True, if the file is currently downloading.
     */
	public static synchronized boolean isFileDownloading(String uuid) {
		if(uuid == null || uuid.equals("") || getPrefs() == null) return true;
		return !getPrefs().get(uuid, "default").equals("default");
	}
	
	/**
     * Adds the given file to the downloads list.
     * @param uuid The uuid to add.
     */
    public static synchronized void addFileDownloading(String uuid) {
		if(uuid == null || uuid.equals("") || getPrefs() == null) return;
		getPrefs().put(uuid, "is_downloading");
    	getPrefs().flush();
    }
    
    /**
     * Deletes the file uuid from the current downloads list.
     * @param uuid The uuid to delete.
     */
    public static synchronized void deleteFileDownloading(String uuid) {
		if(uuid == null || uuid.equals("") || getPrefs() == null) return;
		getPrefs().remove(uuid);
    	getPrefs().flush();
    }

    public static synchronized void clear() {
    	getPrefs().clear();
	}

}
