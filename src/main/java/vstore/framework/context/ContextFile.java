package vstore.framework.context;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.json.simple.JSONObject;

/**
 * Used for simple storage of the current usage context as JSON string. 
 */
public class ContextFile {
	
	private static final String ctxKey = "vstore-context";
	
	/**
	 * Avoid instantiation
	 */
	private ContextFile() {}
	
	/**
	 * Gets the Preferences node
	 * @return The preferences node for the config file
	 */
	private static Preferences getPrefs() {
		return Preferences.userNodeForPackage(ContextFile.class);
	}
	
	/**
	 * Checks if the context file contains data.
	 * @return True, if persistent context data is found.
	 */
	public static synchronized boolean hasContext() {
		return !getPrefs().get(ctxKey, "default").equals("default");
	}
	
	/**
     * Adds the given json string to the context file
     * @param uuid The json string to add.
     */
    public static synchronized void write(JSONObject ctxJson) {
		getPrefs().put(ctxKey, ctxJson.toJSONString());
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
     * Gets the current context stored in this context file.
     * 
     * @return The persistently stored context as json.
     */
    public static synchronized ContextDescription getContext() {
    	String jsonCtx = getPrefs().get(ctxKey, "");
    	if(jsonCtx == null || jsonCtx.equals("")) 
    	{
    		return null;
    	}
    	return new ContextDescription(jsonCtx);
    }
    
    /**
     * Clears the persistent context.
     */
    public static synchronized void clearContext() {
		getPrefs().remove(ctxKey);
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
