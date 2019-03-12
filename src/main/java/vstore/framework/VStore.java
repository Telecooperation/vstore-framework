package vstore.framework;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import vstore.framework.communication.CommunicationManager;
import vstore.framework.communication.RequestFilesMatchingContextThread;
import vstore.framework.communication.download.Downloader;
import vstore.framework.communication.download.PersistentDownloadList;
import vstore.framework.communication.download.events.DownloadFailedEvent;
import vstore.framework.communication.download.events.DownloadProgressEvent;
import vstore.framework.communication.download.events.DownloadedFileReadyEvent;
import vstore.framework.communication.upload.Uploader;
import vstore.framework.config.ConfigManager;
import vstore.framework.context.ContextDescription;
import vstore.framework.context.ContextFilter;
import vstore.framework.context.ContextManager;
import vstore.framework.context.SearchContextDescription;
import vstore.framework.db.DBHelper;
import vstore.framework.db.DBResultOrdering;
import vstore.framework.db.table_helper.FileDBHelper;
import vstore.framework.error.ErrorCode;
import vstore.framework.error.ErrorMessages;
import vstore.framework.exceptions.StoreException;
import vstore.framework.exceptions.VStoreException;
import vstore.framework.file.FileManager;
import vstore.framework.file.VFileType;
import vstore.framework.file.VStoreFile;
import vstore.framework.file.events.FileDeletedEvent;
import vstore.framework.file.events.FilesReadyEvent;
import vstore.framework.logging.LogHandler;
import vstore.framework.logging.LoggingService;
import vstore.framework.matching.FileNodeMapper;
import vstore.framework.matching.Matching;
import vstore.framework.node.NodeInfo;
import vstore.framework.node.NodeManager;
import vstore.framework.rule.RuleManager;
import vstore.framework.utils.ContextUtils;
import vstore.framework.utils.FileUtils;
import vstore.framework.utils.Hash;
import vstore.framework.utils.IdentifierUtils;

import static vstore.framework.error.ErrorMessages.COPIED_FILE_NOT_FOUND;
import static vstore.framework.error.ErrorMessages.COPYING_INTO_FRAMEWORK_FAILED;
import static vstore.framework.error.ErrorMessages.FILE_ALREADY_EXISTS;
import static vstore.framework.error.ErrorMessages.PARAMETERS_MUST_NOT_BE_NULL;

/**
 * Main entry point for the vStore virtual storage framework.
 * 
 * The application is in charge of providing new context and connectivity information
 * to enable the framework to stay up to date.
 */
public class VStore {
    /**
     * This field contains the framework instance.
     */
    private static VStore mInstance = null;


    private static final Logger LOGGER = LogManager.getLogger(VStore.class);

    /**
     * Private constructor for creating a new VStore object. 
     * It initializes all components necessary for operation of the
     * framework (VStoreConfig, DBHelper).
     *
     * @param baseDir The root directory for the framework data.
     * @param masterNodeAddress The address of the master node.
     *
     * @throws VStoreException in case an error occurred.
     */
    private VStore(File baseDir, URL masterNodeAddress) throws VStoreException {
        LOGGER.debug("VStore constructor called");
        LOGGER.debug("Initializing base directory...");
        FileManager.initialize(baseDir);

        if(masterNodeAddress == null) {
            LOGGER.error("No master node address specified");
            throw new VStoreException(
                ErrorCode.PARAMETERS_MUST_NOT_BE_NULL, PARAMETERS_MUST_NOT_BE_NULL);

        }
        LOGGER.debug("Initializing Communication Manager...");
        CommunicationManager.initialize(masterNodeAddress);

        //Generate device identifier if it does not exist
        IdentifierUtils.generateDeviceIdentifier();
        LOGGER.debug("Initializing DB helper...");
        DBHelper.initialize();
        LOGGER.debug("Starting logging service...");
        LoggingService.getThread().start();
        LOGGER.debug("Initializing context manager...");
        ContextManager.initialize();
        LOGGER.debug("Initializing config manager ...");
        ConfigManager.initialize();
        LOGGER.debug("Initializing rule manager...");
        RuleManager.initialize();
        LOGGER.debug("Initializing node manager...");
        NodeManager.initialize();
        LOGGER.debug("Initializing file node mapper...");
        FileNodeMapper.getMapper();
    }

    /**
     * Initializes the vStore framework.
     *
     * @param baseDir The root directory for the framework data.
     * @param masterNodeAddress The address of the master node.
     *
     * @throws VStoreException In case some error occurred during instantiation.
     */
    public static void initialize(File baseDir, URL masterNodeAddress) throws VStoreException {
        //Initialize VStore instance
        LOGGER.debug("VStore initialize called");
        if (mInstance == null)
        {
            mInstance = new VStore(baseDir, masterNodeAddress);
        }
    }

    /**
     * Gets the instance of the vStore framework.
     * 
     * @return The instance of vStore framework.
     */
    public static VStore getInstance() {
        return mInstance;
    }

    /**
     * Returns the current configuration containing application-defined settings.
     * To refresh / re-download the configuration, call {@link ConfigManager#download(boolean)}
     *
     * @return The VStoreConfig configuration object. Will return the default
     * configuration, if you have not configured anything.
     */
    public ConfigManager getConfigManager() {
        return ConfigManager.get();
    }

    /**
     * @return The file manager object for file handling.
     */
    public FileManager getFileManager() {
        return FileManager.get();
    }
    /**
     * @return The communication manager can be used for operations regarding up- and downloads.
     */
    public CommunicationManager getCommunicationManager() {
        return CommunicationManager.get();
    }

    /**
     * @return The rule manager can be used to manage the creation and deletion of rules.
     */
    public RuleManager getRuleManager() {
        return RuleManager.get();
    }
    /**
     * @return The context manager can be used for operations regarding context.
     */
    public ContextManager getContextManager() {
        return ContextManager.get();
    }
    /**
     * @return The storage node manager can be used for operations regarding storage nodes.
     */
    public NodeManager getNodeManager() { return NodeManager.get(); }

    /**
     * This method returns the usage context currently used for matching.
     * If you want to refresh it, use {@link VStore#provideContext(ContextDescription)}
     *
     * @return A ContextDescription-object containing the current usage
     *         context description
     */
    public ContextDescription getCurrentContext() {
        return ContextManager.get().getCurrentContext();
    }

    /**
     * Use this method to provide new context information to the framework.
     * If the new information should be persistent after a restart of the
     * framework, {@link VStore#persistContext(boolean)} should be called.
     *
     * @param ctxDescription The new context information
     *
     * @return Returns the context manager instance again to simplify method-chaining.
     */
    public ContextManager provideContext(ContextDescription ctxDescription) {
        ContextManager.get().provideContext(ctxDescription);
        return ContextManager.get();
    }

    /**
     * Use this method to make the currently configured usage context
     * persistent after a restart of the framework.
     *
     * @param makePersistent True if the context should be persistent.
     *                       False if you want to undo this.
     *
     * @return Returns the context manager instance again to simplify method-chaining.
     */
    public ContextManager persistContext(boolean makePersistent) {
        ContextManager.get().persistContext(makePersistent);
        return ContextManager.get();
    }

    /**
     * This method clears the current usage context and resets it to an empty state.
     *
     * @return Returns the context manager instance again to simplify method-chaining.
     */
    public ContextManager clearCurrentContext() {
        return ContextManager.get().clearCurrentContext();
    }

    /**
     * This method clears all rules and resets the rule set to an empty state.
     *
     * @return Returns the rule manager instance again to simplify method-chaining.
     */
    public RuleManager clearRules() {
        return RuleManager.get().clearRules();
    }

    /**
     * Stores a file in the virtual storage framework. A unique file ID 
     * will be returned immediately and the upload to a storage node will 
     * continue in the background.
     *
     * @param fileUri The absolute path to the file to upload, including 
     *                the filename. The URI must be a valid path to a file.
     *                
     * @param isPrivate Specify whether the file should be publicly available 
     *                  or if it is a personal file that should remain private. 
     *                  True = private, False = public
     *
     * @return Returns "null" if an error occurred. Returns a unique 
     *         identifier if all went well. For a list of supported file types 
     *         see the class VFileType.java. Other file types will be handled 
     *         without any matching rules.
     *         
     * @throws StoreException in case something failed.
     */
    public VStoreFile store(String fileUri, boolean isPrivate)
    		throws StoreException 
    {
        LOGGER.debug("Storing file " + fileUri + ", isPrivate=" + isPrivate);
        if(fileUri == null || fileUri.equals(""))
        {
            throw new StoreException(ErrorCode.PARAMETERS_MUST_NOT_BE_NULL,
                    ErrorMessages.PARAMETERS_MUST_NOT_BE_NULL);
        }
        
        File file = new File(fileUri);
               System.out.println(file.getAbsolutePath());
        //Generate new UUID for file
        String uuid = UUID.randomUUID().toString();
        String descriptiveName = file.getName();
        String mimetype;
        String extension = "";
        
        //Derive mime type from file extension (is not the best method 
        //but will do for now)
        mimetype = FileUtils.getMimeType(file.getName());

        //Get file extension
        int extensionPos = file.getName().lastIndexOf('.');
        if(extensionPos != -1)
        	extension = file.getName().substring(extensionPos + 1);


        //Copy file into the framework folder and rename it to UUID.extension
        File fCopied;
        try 
        {
            fCopied = FileUtils.copyFile(file, FileManager.get().getStoredFilesDir(),
                    uuid + "." + extension);
        } 
        catch (Exception e) 
        {
        	e.printStackTrace();
            //Abort in case of copy-error
            throw new StoreException(ErrorCode.COPYING_INTO_FRAMEWORK_FAILED,
                    COPYING_INTO_FRAMEWORK_FAILED);
        }
      //  System.out.println(fCopied.exists());
        //Compute hash for file
        //TODO: Replace MD5 with something better
        String md5 = Hash.MD5.calculateMD5(fCopied);
        //Post an error if I already stored the same file before
        if(FileDBHelper.isAlreadyStored(md5))
        {
            fCopied.delete();
            //Abort in case the file already exists
            throw new StoreException(ErrorCode.FILE_ALREADY_EXISTS, FILE_ALREADY_EXISTS);
        }
        
        VStoreFile f;
		try 
		{
			f = new VStoreFile(uuid, fCopied, descriptiveName, 
					mimetype, true, isPrivate);
		} 
		catch (FileNotFoundException e) 
		{
			LOGGER.error("Copied file not found!");
		    throw new StoreException(ErrorCode.FILE_NOT_FOUND, COPIED_FILE_NOT_FOUND);
		}
        f.setMD5Hash(md5);
        f.setContext(ContextManager.get().getCurrentContext());

        ConfigManager vCfg = ConfigManager.get();
        //Start logging for this file
        LogHandler.logStartForFile(f, vCfg.getMatchingMode());

        //Make storage matching decision for the file


        long decisionTimeStart = System.nanoTime();
        Matching matching = new Matching(f, vCfg.getMatchingMode());
        List<NodeInfo> targetNodes = matching.getDecidedNodes();
        long decisionTime = (System.nanoTime() - decisionTimeStart) / 1000000 ;
        LOGGER.info("Matching took " + decisionTime + "ms");
        
        if(targetNodes != null && targetNodes.size() > 0)
        {
            for(NodeInfo n : targetNodes) {
                if(n == null) { continue; }
                f.addStoredNodeId(n.getIdentifier());

            }

            //Insert information into local database
            try 
            {
				FileDBHelper.insertFile(f);
			} 
            catch (SQLException e) 
            {
            	//Error while storing the information in the database.
				e.printStackTrace();
				fCopied.delete();
				LogHandler.abortLoggingForFile(f.getUuid());
	            throw new StoreException(ErrorCode.DB_LOCAL_ERROR, e.getMessage());
			}
            //Schedule job for background upload
            Uploader up = Uploader.getUploader();
            up.enqueueUpload(f);
            up.startUploads();
        } 
        else 
        {
            //TODO: No node was decided, so we only store the 
        	//file on the device.
        	f.setUploadPending(false);
            try 
            {
				FileDBHelper.insertFile(f);
			} 
            catch (SQLException e) 
            {
				e.printStackTrace();
				fCopied.delete();
				LogHandler.abortLoggingForFile(f.getUuid());
	            throw new StoreException(ErrorCode.DB_LOCAL_ERROR, e.getMessage());
			}
        }

        //Log the decided node (will log null if stored on phone)

        LogHandler.logDecidedNode(f, ((targetNodes != null) ? targetNodes.get(0) : null), decisionTime);
        //TODO Update logging to handle a list of multiple storage nodes
        return f;
    }

    /**
     * This method starts a job that queries every available storage node with 
     * the given usage context. The nodes will then deliver a list with files 
     * matching this context.
     * You can subscribe to the following event to get notified of available 
     * results from a node:
     * {@link vstore.framework.file.events.NewFilesMatchingContextEvent}
     *
     * To request files for a custom context, simply create a 
     * {@link ContextDescription} with the desired properties and pass it to 
     * this method.
     *
     * @param usageContext The usage context for which to find matching files.
     * @param filter A filter that should be applied to the context.
     * @param requestId Give your request an ID so that you can keep track of 
     *        the replies when they come in with the event.
     * @return True, if the request jobs have been started. Returns false, if not.
     */
    public boolean getFilesMatchingContext(ContextDescription usageContext,
    		ContextFilter filter, String requestId) 
    {
        if(usageContext == null)
        {

            throw new RuntimeException(ErrorMessages.PARAMETERS_MUST_NOT_BE_NULL);
        }
        if(requestId == null || requestId.equals(""))
        {
            requestId = UUID.randomUUID().toString();
        }

        //Apply the user-defined search filter to the current context
        SearchContextDescription ctx
        	= ContextUtils.applyFilter(usageContext, filter);
        //Get node manager instance
        NodeManager manager = NodeManager.get();
        Map<String, NodeInfo> nodelist = manager.getNodeList();
        if(nodelist.size() == 0) return false;

        //Clear the file->node mapping file
        FileNodeMapper.getMapper().clear();
        //Start a request job for each node
        for (NodeInfo ninfo : nodelist.values())
        {
            try
			{
            	RequestFilesMatchingContextThread t
            		= new RequestFilesMatchingContextThread(ninfo.getIdentifier(), ctx, requestId);
	            t.start();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
        }
        return true;
    }

    /**
     * Can be used to fetch a list of all files uploaded (or currently uploading) from this device.
     * Results are published in the {@link FilesReadyEvent}.
     *
     * @param ordering The order in which to fetch the files.
     *                       (see {@link DBResultOrdering}).
     * @param onlyPending If true, only files that are pending to upload
     * 				      will be returned.
     * @param onlyPrivate If true, only files that are marked private
     *                    will be returned.
     */
    public void getFilesUploadedByThisDevice(DBResultOrdering ordering, boolean onlyPending, boolean onlyPrivate) {
        FileManager.get().getLocalFileList(ordering, onlyPending, onlyPrivate);
    }

    /**
     * Use this method to request the full file that has the given UUID.
     * Subscribe to the event {@link DownloadProgressEvent} to get notified 
     * about the state of the request for this file UUID.
     * Additionally, listen to the {@link DownloadFailedEvent} to get 
     * notified if a file download failed.
     * When the download is ready, you will be notified by the {@link DownloadedFileReadyEvent}.
     *
     * @param uuid The uuid of the file to retrieve.
     * @param dir If specified, the file will be placed in this directory.
     */
    public void getFile(final String uuid, File dir) {
        FileManager.get().getFile(uuid, dir);
    }

    /**
     * Loads the thumbnail for the given UUID.
     * Thumbnail will be returned in the {@link vstore.framework.communication.download.events.NewThumbnailEvent}.
     * 
     * @param fileUuid The UUID of the file.
     */
    public void getThumbnail(String fileUuid) {
        Downloader.downloadThumbnail(fileUuid);
    }

    /**
     * Checks if the file with the given UUID was published before
     * by this device.
     * 
     * @param fileUuid The UUID to check
     * @return True, if this phone uploaded the file.
     */
    public boolean isMyFile(String fileUuid) {
    	return FileManager.get().isMyFile(fileUuid);
    }

    /**
     * Marks the given file for deletion.
     * Once the corresponding node has replied that the file was deleted,
     * you will be notified of the success with the following event:
     * {@link FileDeletedEvent}
     *
     * @param fileUuid The UUID of the file to delete.
     * @return True when the file was marked for deletion locally and the deletion has started.
     */
    public boolean deleteFile(String fileUuid) {
        return FileManager.get().deleteFile(fileUuid);
    }

    /**
     * This method checks, if the given string is a mime type 
     * supported by the framework. For supported types see {@link VFileType}.
     *
     * @param mimetype The mime type string, e.g. "image/jpeg".
     * @return True, if the type is supported. False, if not.
     */
    public static boolean isMimeTypeSupported(String mimetype) {
        return FileManager.isMimeTypeSupported(mimetype);
    }

    /**
     * @return The identifier of this device.
     */
    public static String getDeviceIdentifier() {
        return IdentifierUtils.getDeviceIdentifier();
    }

    /**
     * Call this when you exit your application to clean up any mess.
     */
    public void clean() {
        LOGGER.debug("Cleaning up...");
        DBHelper.get().close();
        LoggingService.getThread().askToStop();
        PersistentDownloadList.clear();
    }
}
