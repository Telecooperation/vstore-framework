package vstore.framework.config;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.sql.SQLException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import vstore.framework.context.types.noise.VNoise;
import vstore.framework.db.table_helper.RulesDBHelper;
import vstore.framework.error.ErrorCode;
import vstore.framework.error.ErrorMessages;
import vstore.framework.exceptions.DatabaseException;
import vstore.framework.exceptions.VStoreException;
import vstore.framework.matching.Matching;
import vstore.framework.matching.Matching.MatchingMode;
import vstore.framework.node.NodeInfo;
import vstore.framework.node.NodeManager;
import vstore.framework.rule.VStoreRule;

/**
 * This class loads the configuration for the framework.
 */
@SuppressWarnings("unused")
public class ConfigManager {
    private int mDefaultRMSThreshold;
    private int mDefaultDBThreshold;
    
    private boolean mMultipleNodesPerRule;
    
    private MatchingMode mMatchingMode;
    
    private NodeManager mNodeManager;
    private static ConfigManager mConfMgrInstance;
    private static Config mConfig;

    /**
     * Creates a vStoreConfig object. Reads the settings from the last configuration saved on disk.
     */
    private ConfigManager() {
        //Start NodeManager
        mNodeManager = NodeManager.getInstance();
        readConfig();
    }

    public static void initialize() {
        if (mConfMgrInstance == null)
        {
            mConfMgrInstance = new ConfigManager();
        }
    }

    /**
     * Gets the instance of the vStore config manager.
     * To refresh / re-download the configuration, use {@link ConfigManager#download(String)}.
     *
     * @return The instance of the configuration manager.
     */
    public static ConfigManager get() {
        initialize();
        return mConfMgrInstance;
    }

    /**
     * Reads the configuration from the local file.
     */
    private void readConfig() {
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
     * Warning: This will block until the download is finished or has failed.
     * 
     * @param url The url to the the configuration file (must be in JSON format, see
     *            documentation).
     * @throws VStoreException if the download of the config file failed
     */
    public void download(String url) throws VStoreException {
        if(url == null || url.equals(""))
        {
            throw new VStoreException(ErrorCode.CONFIG_DOWNLOAD_FAILED,
                    ErrorMessages.CONFIG_DOWNLOAD_FAILED);
        }
    	final OkHttpClient client = new OkHttpClient();
    	Request request = new Request.Builder()
    	        .url(url)
    	        .build();
    	try (Response response = client.newCall(request).execute()) 
    	{
    		if (!response.isSuccessful())
    		    throw new VStoreException(ErrorCode.CONFIG_DOWNLOAD_FAILED,
                        ErrorMessages.RESPONSE_WRONG_STATUS_CODE + response);

    		try 
    		{
				parseJsonConfig(response.body().string());
			} 
    		catch (ParseException e) 
    		{
    			//JSON parsing failed. This is bad.
    			e.printStackTrace();
                throw new VStoreException(ErrorCode.CONFIG_DOWNLOAD_FAILED,
                        ErrorMessages.JSON_PARSING_FAILED);
			}
	    } 
    	catch (IOException e) 
    	{
			e.printStackTrace();
            throw new VStoreException(ErrorCode.CONFIG_DOWNLOAD_FAILED,
                    ErrorMessages.REQUEST_FAILED);
		}
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
     * @param dbThresh The desired threshold.
     */
    static void setDefaultDBThresh(int dbThresh) {
        ConfigFile.putInt(ConfigConstants.DEFAULT_DB_THRESHOLD_KEY, dbThresh);
    }

    /**
     * Sets the matching mode setting and stores it.
     * @param mode The matching mode
     */
    public void setMatchingMode(Matching.MatchingMode mode) {
        ConfigFile.putString(ConfigConstants.MATCHING_MODE_KEY, mode.name());
        mMatchingMode = mode;
    }

}
