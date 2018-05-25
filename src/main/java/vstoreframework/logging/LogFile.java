package vstoreframework.logging;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Misuses a Java Preference file for implementing a simple and easy to use "log file".
 */
public class LogFile {
	
	/**
	 * Avoid instantiation
	 */
	private LogFile() {}
	
	/**
	 * Gets the Preferences node
	 * @return The preferences node for the log file
	 */
	private static Preferences getPrefs() {
		return Preferences.userNodeForPackage(LogFile.class);
	}
	
	/**
	 * Checks if the log file contains an entry with the given key.
	 * 
	 * @param uuid The key of the entry to search for.
	 * @return True, if an entry is found in the log file
	 */
	public static synchronized boolean contains(String uuid) {
		if(uuid == null || uuid.equals("")) return false;
		return !getPrefs().get(uuid, "default").equals("default");
	}
	
	/**
     * Adds the given information to the logfile
     * @param uuid The uuid to add.
     */
    public static synchronized void logEntry(String uuid, String data) {
		if(uuid == null || uuid.equals("")) return;
    	getPrefs().put(uuid, data);
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
     * Gets the entry with the given key from the log file.
     * 
     * @param uuid The key of the entry to read.
     * @return The corresponding entry of the log file.
     */
    public static synchronized String getEntry(String uuid) {
    	if(uuid == null || uuid.equals("")) return "";
    	return getPrefs().get(uuid, "");
    }
    
    /**
     * Deletes the log entry from the log file
     * @param uuid The uuid of the entry to delete.
     */
    public static synchronized void deleteEntry(String uuid) {
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
