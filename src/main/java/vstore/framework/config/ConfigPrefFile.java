package vstore.framework.config;

import vstore.framework.exceptions.VStoreException;
import vstore.framework.file.SimplePrefFile.PrefFile;
import vstore.framework.file.SimplePrefFile.PrefFileManager;

public class ConfigPrefFile {

	/**
	 * Avoid instantiation
	 */
	private ConfigPrefFile() {}
	
	/**
	 * Gets the Preferences file
	 * @return The preferences file for the config file
	 */
	private static PrefFile getPrefs() {
		try {
			return PrefFileManager.getPrefFile("ConfigPrefFile");
		} catch(VStoreException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Checks if the config file contains an entry with the given key.
	 * 
	 * @param key The key of the entry to search for.
	 * @return True, if an entry is found in the config file
	 */
	public static synchronized boolean contains(String key) {
		if(key == null || key.equals("") || getPrefs() == null) return false;
		return !getPrefs().get(key, "default").equals("default");
	}
	
	/**
     * Adds the given information to the config file
     * @param key The key to add.
     * @param data The data to add.
     */
    public static synchronized void putString(String key, String data) {
		if(key == null || key.equals("") || getPrefs() == null) return;
    	getPrefs().put(key, data);
        getPrefs().flush();
    }
    
    /**
     * Adds the given boolean value to the config file.
     * 
     * @param key The key of the entry.
     * @param value The value of the entry.
     */
    public static synchronized void putBoolean(String key, boolean value) {
    	if(key == null || key.equals("") || getPrefs() == null) return;
    	getPrefs().putBoolean(key, value);
        getPrefs().flush();
    }
    
    public static synchronized void putInt(String key, int value) {
    	if(key == null || key.equals("") || getPrefs() == null) return;
    	getPrefs().putInt(key, value);
        getPrefs().flush();
    }
    
    /**
     * Gets the entry with the given key from the config file.
     * 
     * @param key The key of the entry to read.
     * @return The corresponding entry of the config file.
     */
    public static synchronized Object getEntry(String key) {
    	if(key == null || key.equals("") || getPrefs() == null) return "";
    	return getPrefs().get(key, "");
    }
    
    /**
     * Gets the entry with the given key from the config file.
     * 
     * @param key The key of the entry to read.
     * @param defaultVal The value to return if the key is not found.
     * @return The corresponding entry of the config file.
     */
    public static synchronized String getString(String key, String defaultVal) {
    	if(key == null || key.equals("") || getPrefs() == null) return "";
    	return getPrefs().get(key, defaultVal);
    }
    
    /**
     * Reads the entry with the given key from the config file as integer.
     * @param key The key of the entry to read.
     * @param defaultVal The default value that should be returned if the 
     *        key is not found in the config file.
     * @return The value as integer
     */
    public static synchronized int getInt(String key, int defaultVal) {
    	if(key == null || key.equals("") || getPrefs() == null) return defaultVal;
    	return getPrefs().getInt(key, defaultVal);
    }
    
    /**
     * Reads the entry with the given key from the config file as integer.
     * @param key The key of the entry to read.
     * @return The value as integer, or 0 if nothing was found.
     */
    public static synchronized int getInt(String key) {
    	if(key == null || key.equals("") || getPrefs() == null) return 0;
    	return getPrefs().getInt(key, 0);
    }
    
    /**
     * Reads the entry with the given key from the config file as boolean.
     * 
     * @param key The key to read from the config file.
     * @param defaultVal The default value to return if the key is not found.
     * @return The value as boolean.
     */
    public static synchronized boolean getBoolean(String key, boolean defaultVal) {
    	if(key == null || key.equals("") || getPrefs() == null) return defaultVal;
    	return getPrefs().getBoolean(key, defaultVal);
    }
    
    /**
     * Deletes the config entry from the config file
     * @param key The key of the entry to delete.
     */
    public static synchronized void deleteEntry(String key) {
		if(key == null || key.equals("") || getPrefs() == null) return;
    	getPrefs().remove(key);
        getPrefs().flush();
    }
}
