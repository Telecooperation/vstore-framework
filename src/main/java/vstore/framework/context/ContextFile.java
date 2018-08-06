package vstore.framework.context;

import org.json.simple.JSONObject;

import vstore.framework.exceptions.VStoreException;
import vstore.framework.file.SimplePrefFile.PrefFile;
import vstore.framework.file.SimplePrefFile.PrefFileManager;

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
	 * Gets the Preferences file
	 * @return The preferences file for the config file
	 */
	private static PrefFile getPrefFile() {
	    try {
            return PrefFileManager.getPrefFile("ContextFile");
        } catch(VStoreException e) {
	        e.printStackTrace();
        }
        return null;
	}
	
	/**
	 * Checks if the context file contains data.
	 * @return True, if persistent context data is found.
	 */
	public static synchronized boolean hasContext() {
        if(getPrefFile() == null) { return false; }
	    return !getPrefFile().get(ctxKey, "default").equals("default");
	}
	
	/**
     * Adds the given json string to the context file
     * @param ctxJson The json string to add.
     */
    public static synchronized void write(JSONObject ctxJson) {
        if(getPrefFile() == null) { return; }
		getPrefFile().put(ctxKey, ctxJson.toJSONString());
    	getPrefFile().flush();
    }
    
    /**
     * Gets the current context stored in this context file.
     * 
     * @return The persistently stored context as json.
     */
    public static synchronized ContextDescription getContext() {
        if(getPrefFile() == null) { return null; }
    	String jsonCtx = getPrefFile().get(ctxKey, "");
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
        if(getPrefFile() == null) { return; }
		getPrefFile().remove(ctxKey);
    	getPrefFile().flush();
    }
}
