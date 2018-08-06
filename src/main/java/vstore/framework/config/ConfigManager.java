package vstore.framework.config;

import org.greenrobot.eventbus.EventBus;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import vstore.framework.communication.master_node.MasterNode;
import vstore.framework.config.events.ConfigDownloadFailedEvent;
import vstore.framework.config.events.ConfigDownloadSucceededEvent;
import vstore.framework.context.types.noise.VNoise;
import vstore.framework.error.ErrorCode;
import vstore.framework.matching.Matching;
import vstore.framework.matching.Matching.MatchingMode;
import vstore.framework.node.NodeManager;

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
        mNodeManager = NodeManager.get();
        readConfig();
    }

    public static void initialize() {
        if (mConfMgrInstance == null)
        {
            mConfMgrInstance = new ConfigManager();
            mConfig = new Config();
            mConfig.matchingMode = MatchingMode.RULES_NEXT_ON_NO_MATCH;
            mConfig.multipleNodesPerRule = false;
        }
    }

    /**
     * Gets the instance of the vStore config manager.
     * To refresh / re-download the configuration, use {@link ConfigManager#download(boolean)}.
     *
     * @return The instance of the configuration manager.
     */
    public static ConfigManager get() {
        return mConfMgrInstance;
    }

    /**
     * Reads the configuration from the local file.
     */
    private void readConfig() {
        //Get configured noise thresholds from shared prefs
        mDefaultRMSThreshold  = ConfigPrefFile.getInt(ConfigConstants.DEFAULT_RMS_THRESHOLD_KEY, VNoise.DEFAULT_THRESHOLD_RMS);
        mDefaultDBThreshold = ConfigPrefFile.getInt(ConfigConstants.DEFAULT_DB_THRESHOLD_KEY, VNoise.DEFAULT_TRESHOLD_DB);

        //Get from settings file, if multiple nodes are allowed per rule
        mMultipleNodesPerRule = ConfigPrefFile.getBoolean(ConfigConstants.ALLOW_MULTIPLE_NODES_KEY, true);

        //Get from settings file, what matching mode should be used
        String matchingMode = ConfigPrefFile.getString(ConfigConstants.MATCHING_MODE_KEY, Matching.MatchingMode.RULES_NEXT_ON_NO_MATCH.toString());
        try {
            mMatchingMode = Matching.MatchingMode.valueOf(matchingMode);
        } catch(IllegalArgumentException e) {
            mMatchingMode = Matching.MatchingMode.RULES_NEXT_ON_NO_MATCH;
        }

        if(mNodeManager == null) {
            mNodeManager = NodeManager.get();
        }
        mNodeManager.refreshNodes();
    }

    /**
     * Downloads the configuration for the framework.
     * You can choose if this should block by setting the block parameter.
     * a) false (background): To get notified when config download has finished,
     *    subscribe to {@link ConfigDownloadSucceededEvent} and {@link ConfigDownloadFailedEvent}.
     * b) true (blocking): Will time out after 2 seconds of unsuccessful connection.
     *
     *
     * @param block Set this to true, if the download should block.
     */
    public void download(boolean block) {
        //Try to download file on new thread.
        ExecutorService executor = Executors.newFixedThreadPool(1);
        FutureTask<Void> futureTask = new FutureTask<>(MasterNode::getConfigurationFile);
        executor.execute(futureTask);

        if(!block) { return; }
        try {
            futureTask.get();
        }
        catch (InterruptedException | ExecutionException e)
        {
            e.printStackTrace();
            ConfigDownloadFailedEvent evt = new ConfigDownloadFailedEvent();
            evt.errorCode = ErrorCode.CONFIG_DOWNLOAD_FAILED;
            evt.errorMsg = "";
            EventBus.getDefault().post(evt);
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
        ConfigPrefFile.putBoolean(ConfigConstants.ALLOW_MULTIPLE_NODES_KEY, multipleAllowed);
        mMultipleNodesPerRule = multipleAllowed;
    }

    /**
     * Sets the default dB threshold for a noise context.
     * If the dB value of a noise context is higher than the given threshold, it will be considered
     * as "not silent".
     * @param dbThresh The desired threshold.
     */
    static void setDefaultDBThresh(int dbThresh) {
        ConfigPrefFile.putInt(ConfigConstants.DEFAULT_DB_THRESHOLD_KEY, dbThresh);
    }

    /**
     * Sets the matching mode setting and stores it.
     * @param mode The matching mode
     */
    public void setMatchingMode(Matching.MatchingMode mode) {
        ConfigPrefFile.putString(ConfigConstants.MATCHING_MODE_KEY, mode.name());
        mMatchingMode = mode;
    }

}
