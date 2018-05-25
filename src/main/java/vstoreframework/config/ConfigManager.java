package vstoreframework.config;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import vstoreframework.context.types.noise.VNoise;
import vstoreframework.db.table_helper.RulesDBHelper;
import vstoreframework.exceptions.DatabaseException;
import vstoreframework.matching.Matching;
import vstoreframework.matching.Matching.MatchingMode;
import vstoreframework.node.NodeInfo;
import vstoreframework.node.NodeManager;
import vstoreframework.rule.VStoreRule;
import vstoreframework.utils.FileUtils;

/**
 * This class loads the configuration for the framework.
 */
@SuppressWarnings("unused")
public class ConfigManager {
    /**
     * This is the subdirectory where the framework stores user-created files.
     */
    private static final String STORED_FILES_DIR = "vfiles/";
    /**
     * This is the subdirectory where the framework stores thumbnails for user-created files.
     */
    private static final String THUMBNAILS_DIR = STORED_FILES_DIR + "thumbs/";
    /**
     * This is the subdirectory where the framework stores downloaded files
     */
    private static final String DOWNLOADED_FILES_DIR = "dlfiles/";

	private int mDefaultRMSThreshold;
    private int mDefaultDBThreshold;
    
    private boolean mMultipleNodesPerRule;
    
    private MatchingMode mMatchingMode;
    
    private NodeManager mNodeManager;
    private static ConfigManager mConfigManager;
    private static Config mConfig;

    /**
     * Creates a vStoreConfig object. Reads the settings from the shared preferences.
     * @param downloadConfig Set this to true if the node- and rule-configuration 
     * 		  file should be downloaded.
     *        If you set this to false, the second parameter is ignored
     * @param url The url where to download the file.
     */
    private ConfigManager(boolean downloadConfig, String url) {
        //Start NodeManager
        mNodeManager = NodeManager.getInstance();

        refreshConfig();

        if(downloadConfig && url != null) {
            downloadConfig(url);
        }
    }

    /**
     * Notice: This method will only provide a default configuration and will not reflect
     * the user defined settings.
     */
    public static ConfigManager getDefault() {
        return new ConfigManager(false, null);
    }

    /**
     * Gets the instance of the vStore config manager.
     * 
     * @param downloadConfig Set this to true if the node- and rule-configuration 
     * 		  file should be downloaded.
     * @param url The address from which to download the configuration.
     * 
     * @return The instance of the configuration.
     */
    public static ConfigManager getInstance(boolean download, String url) {
        if (mConfigManager == null) 
        {
            mConfigManager = new ConfigManager(true, url);
        }
        else 
        {
            mConfigManager.refreshConfig();
        }
        return mConfigManager;
    }

    private void refreshConfig() {

        //Get configured noise thresholds from shared prefs
        mDefaultRMSThreshold  = ConfigFile.getInt(ConfigConstants.DEFAULT_RMS_THRESHOLD_KEY, VNoise.DEFAULT_THRESHOLD_RMS);
        mDefaultDBThreshold = ConfigFile.getInt(ConfigConstants.DEFAULT_DB_THRESHOLD_KEY, VNoise.DEFAULT_TRESHOLD_DB);

        //Get from settings file, if multiple nodes are allowed per rule
        mMultipleNodesPerRule = ConfigFile.getBoolean(ConfigConstants.ALLOW_MULTIPLE_NODES_KEY, true);

        //Get from settings file, what matching mode should be used
        String matchingMode = ConfigFile.getString(ConfigConstants.MATCHING_MODE_KEY, Matching.MatchingMode.RULES_NEXT_ON_NO_MATCH.toString());
        try {
            mMatchingMode = Matching.MatchingMode.valueOf(matchingMode);
        } catch(IllegalArgumentException e) {
            mMatchingMode = Matching.MatchingMode.RULES_NEXT_ON_NO_MATCH;
        }

        if(mNodeManager == null) {
            mNodeManager = NodeManager.getInstance();
        }
        mNodeManager.refreshNodes();
    }

    /**
     * Downloads the configuration file from the given url. 
     * Warning: This will block!
     * 
     * @param url The url from which to download the configuration
     * @return True, if the configuration was downloaded and processed successfully. False if not. 
     */
    boolean downloadConfig(String url) {
    	final OkHttpClient client = new OkHttpClient();
    	Request request = new Request.Builder()
    	        .url(url)
    	        .build();
    	try (Response response = client.newCall(request).execute()) 
    	{
    		if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

    		try 
    		{
				parseJsonConfig(response.body().string());
			} 
    		catch (ParseException e) 
    		{
    			//JSON parsing failed. This is bad.
    			e.printStackTrace();
    			return false;
			}
	    } 
    	catch (IOException e) 
    	{
			e.printStackTrace();
			return false;
		}
    	return true;
    }
    
    private void parseJsonConfig(String strConfig) throws ParseException {
    	JSONParser p = new JSONParser();
    	JSONObject j = (JSONObject) p.parse(strConfig);
		
    	//Parse the list of nodes into the node manager
    	if(j.containsKey("nodes")) 
    	{
            JSONArray nodes = (JSONArray)j.get("nodes");
            NodeManager manager = NodeManager.getInstance();
            for(int i = 0; i<nodes.size(); ++i) 
            {
            	JSONObject node = (JSONObject)nodes.get(i);
                manager.addNode(new NodeInfo(node));
            }
            manager.refreshNodes();
        }
    	
    	//Parse the list of rules
        if(j.containsKey("rules")) 
        {
        	try
        	{
        
	            JSONArray rules = (JSONArray)j.get("rules");
	            RulesDBHelper helper = new RulesDBHelper();
				
	            
	            for(int i = 0; i<rules.size(); ++i) 
	            {
	                JSONObject rule = (JSONObject)rules.get(i);
	                if(rule.containsKey("delete") && (boolean)rule.get("delete")) 
	                {
	                    //Only delete, if we have a UUID
	                    if(rule.containsKey("uuid")) 
	                    {
	                        //Delete-Flag is set, thus remove the rule from the database
	                        helper.deleteRule((String)rule.get("uuid"));
	                    }
	                } 
	                else 
	                {
	                    //Create a new rule from the json string and put it into
	                    //the database. Or update an existing rule with the given ID.
	                    try 
	                    {
	                        VStoreRule r = new VStoreRule(rule.toString());
	                        helper.insertRule(r);
	                    } 
	                    catch(RuntimeException ex) 
	                    {
	                        //Ignore the rule if the provided data was not sufficient.
	                        //Log.d("vStore", "Error adding rule " + i);
	                    }
	                }
	            }
        	} catch (DatabaseException | SQLException e) {
				e.printStackTrace();
			}
        }
        
        //Parse the rest
        //Check if enableRules is specified in the config file
        if(j.containsKey("matchingMode")) 
        {
            String matchingMode = (String)j.get("matchingMode");
            try 
            {
            	this.setMatchingMode(Matching.MatchingMode.valueOf(matchingMode));
            } 
            catch(IllegalArgumentException ex) 
            {
            	this.setMatchingMode(Matching.MatchingMode.RULES_NEXT_ON_NO_MATCH);
            }
        } 
        else 
        {
            //If property is missing from the config file, simply use the default matching
            this.setMatchingMode(Matching.MatchingMode.RULES_NEXT_ON_NO_MATCH);
        }
    }


    /**
     * @return Returns true, if the framework is currently configured to allow the decision to
     * decide for multiple nodes to store a file on.
     */
    public boolean isMultipleNodesAllowed() {
        return mConfig.multipleNodesPerRule;
    }

    /**
     * @return The matching mode that is currently configured.
     */
    public Matching.MatchingMode getMatchingMode() {
        return mConfig.matchingMode;
    }
    /**
     * Returns the File object of the directory where the framework stores user files.
     *
     * @return A File object describing the stored files directory.
     */
    public static File getStoredFilesDir() {
        File dir = new File(FileUtils.getVStoreDir(), STORED_FILES_DIR);
        dir.mkdirs();
        return dir;
    }

    public static File getDownloadedFilesDir() {
        File dir = new File(FileUtils.getVStoreDir(), DOWNLOADED_FILES_DIR);
        dir.mkdirs();
        return dir;
    }

    /**
     * Returns the File object of the directory where the framework stores thumbnail files.
     * 
     * @return A File object describing the thumbnail directory.
     */
    public static File getThumbnailsDir() {
        File dir = new File(FileUtils.getVStoreDir(), THUMBNAILS_DIR);
        dir.mkdirs();
        return dir;
    }


    /**
     * Sets the flag to allow/disallow a decision rule to decide for storing a file on
     * multiple storage nodes.
     * 
     * @param multipleAllowed True, if multiple nodes should be allowed.
     */
    void setIsMultipleNodesAllowed(boolean multipleAllowed) {
        ConfigFile.putBoolean(ConfigConstants.ALLOW_MULTIPLE_NODES_KEY, multipleAllowed);
        mMultipleNodesPerRule = multipleAllowed;
    }

    /**
     * Sets the default dB threshold for a noise context.
     * If the dB value of a noise context is higher than the given threshold, it will be considered
     * as "not silent".
     * @param c The Android context.
     * @param dbThresh The desired threshold.
     */
    static void setDefaultDBThresh(int dbThresh) {
        ConfigFile.putInt(ConfigConstants.DEFAULT_DB_THRESHOLD_KEY, dbThresh);
    }

    /**
     * Sets the matching mode setting and stores it.
     * @param c The Android context
     * @param mode The matching mode
     */
    public void setMatchingMode(Matching.MatchingMode mode) {
        ConfigFile.putString(ConfigConstants.MATCHING_MODE_KEY, mode.name());
        mMatchingMode = mode;
    }

}
