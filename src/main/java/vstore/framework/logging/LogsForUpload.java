package vstore.framework.logging;

import java.util.HashMap;
import java.util.Map;

import vstore.framework.exceptions.VStoreException;
import vstore.framework.file.SimplePrefFile.PrefFile;
import vstore.framework.file.SimplePrefFile.PrefFileManager;

/**
 * Implements a simple and easy to use "log file".
 */
public class LogsForUpload {
	
	/**
	 * Avoid instantiation
	 */
	private LogsForUpload() {}
	
	/**
	 * Gets the Preferences file
	 * @return The preferences file for the log file
	 */
	private static PrefFile getPrefs() {
		try {
			return PrefFileManager.getPrefFile("LogsForUpload");
		} catch (VStoreException e) {
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
	 * @param data The data to add for the given uuid key.
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
    
    public static synchronized Map<String, ?> getAll() {
        if(getPrefs() == null) { return new HashMap<String, String>(); }

    	String[] keys = getPrefs().keys();
		HashMap<String, String> myMap = new HashMap<String, String>();
        for(int count = 0; count < keys.length; ++count)
        {
            myMap.put(keys[count], getEntry(keys[count]));
        }
        return myMap;
    }

}
