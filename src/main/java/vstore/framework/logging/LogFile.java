package vstore.framework.logging;

import vstore.framework.exceptions.VStoreException;
import vstore.framework.file.SimplePrefFile.PrefFile;
import vstore.framework.file.SimplePrefFile.PrefFileManager;

/**
 * Misuses a Java Preference file for implementing a simple and easy to use "log file".
 */
public class LogFile {
	
	/**
	 * Avoid instantiation
	 */
	private LogFile() {}
	
	/**
	 * Gets the Preferences file
	 * @return The preferences file for the log file
	 */
	private static PrefFile getPrefs() {
		try {
			return PrefFileManager.getPrefFile("LogFile");
		} catch(VStoreException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Checks if the log file contains an entry with the given key.
	 * 
	 * @param uuid The key of the entry to search for.
	 * @return True, if an entry is found in the log file
	 */
	public static synchronized boolean contains(String uuid) {
		if(uuid == null || uuid.equals("") || getPrefs() == null) return false;
		return !getPrefs().get(uuid, "default").equals("default");
	}
	
	/**
     * Adds the given information to the logfile
     * @param uuid The uuid to add.
	 * @param data The data string to add for the uuid key.
     */
    public static synchronized void logEntry(String uuid, String data) {
		if(uuid == null || uuid.equals("") || getPrefs() == null) return;
    	getPrefs().put(uuid, data);
    	getPrefs().flush();
    }
    
    /**
     * Gets the entry with the given key from the log file.
     * 
     * @param uuid The key of the entry to read.
     * @return The corresponding entry of the log file.
     */
    public static synchronized String getEntry(String uuid) {
    	if(uuid == null || uuid.equals("") || getPrefs() == null) return "";
    	return getPrefs().get(uuid, "");
    }
    
    /**
     * Deletes the log entry from the log file
     * @param uuid The uuid of the entry to delete.
     */
    public static synchronized void deleteEntry(String uuid) {
		if(uuid == null || uuid.equals("") || getPrefs() == null) return;
    	getPrefs().remove(uuid);
    	getPrefs().flush();
    }

}
