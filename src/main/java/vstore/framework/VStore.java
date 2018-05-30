package vstore.framework;

import static vstore.framework.error.ErrorMessages.COPIED_FILE_NOT_FOUND;
import static vstore.framework.error.ErrorMessages.COPYING_INTO_FRAMEWORK_FAILED;
import static vstore.framework.error.ErrorMessages.FILE_ALREADY_EXISTS;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

import org.greenrobot.eventbus.EventBus;

import vstore.framework.communication.CommunicationManager;
import vstore.framework.communication.RequestFilesMatchingContextThread;
import vstore.framework.communication.download.Downloader;
import vstore.framework.communication.download.events.DownloadFailedEvent;
import vstore.framework.communication.download.events.DownloadProgressEvent;
import vstore.framework.communication.download.events.DownloadedFileReadyEvent;
import vstore.framework.communication.download.events.MetadataEvent;
import vstore.framework.communication.upload.Uploader;
import vstore.framework.config.ConfigManager;
import vstore.framework.context.ContextDescription;
import vstore.framework.context.ContextFile;
import vstore.framework.context.ContextFilter;
import vstore.framework.context.SearchContextDescription;
import vstore.framework.db.DBHelper;
import vstore.framework.db.DBResultOrdering;
import vstore.framework.db.table_helper.FileDBHelper;
import vstore.framework.error.ErrorMessages;
import vstore.framework.exceptions.DatabaseException;
import vstore.framework.exceptions.StoreException;
import vstore.framework.exceptions.VStoreException;
import vstore.framework.file.VFileType;
import vstore.framework.file.VStoreFile;
import vstore.framework.file.events.FileDeletedEvent;
import vstore.framework.file.events.FilesReadyEvent;
import vstore.framework.file.threads.FetchFilesFromDBThread;
import vstore.framework.logging.LogHandler;
import vstore.framework.logging.LoggingService;
import vstore.framework.matching.FileNodeMapper;
import vstore.framework.matching.Matching;
import vstore.framework.node.NodeInfo;
import vstore.framework.node.NodeManager;
import vstore.framework.utils.ContextUtils;
import vstore.framework.utils.FileUtils;
import vstore.framework.utils.FrameworkUtils;
import vstore.framework.utils.Hash;

/**
 * Main entry point for the vStore virtual storage framework for mobile devices.
 * 
 * The application is in charge of providing new context and connectivity information
 * to enable the framework to stay up to date.
 */
public class VStore {
    /**
     * This field contains the framework instance.
     */
    private static VStore mInstance = null;
    
    /**
     * This field contains the current context description.
     */
    private static ContextDescription mCurrentContext;
        
    /**
     * Private constructor for creating a new VStore object. 
     * It initializes all components necessary for operation of the
     * framework (VStoreConfig, DBHelper).
     */
    private VStore() throws VStoreException {
        //Start the logging thread
        LoggingService.getThread().start();
        
        //Initialize instance of db access
    	initializeDatabase();
        initializeContext();

        //Initialize the vStore configuration instance
        //TODO: Change parameters according to user input 
        //(if user wants us to download the file)
        ConfigManager.getInstance(false, null);
        
        //Initialize the vStore FileNodeMapper
        FileNodeMapper.getMapper();
    }

    /**
     * Gets the instance of the vStore framework.
     * 
     * @return The instance of vStore framework.
     * @throws VStoreException In case some error occured during instantiation.
     */
    public static VStore getInstance() throws VStoreException {
         /**
         * Initialize VStore instance
         */
        if (mInstance == null) 
        {
            mInstance = new VStore();
        }
        return mInstance;
    }
    
    private void initializeDatabase() throws VStoreException {
    	try 
    	{
			DBHelper.getInstance();
		} 
    	catch (DatabaseException e) 
    	{
			e.printStackTrace();
			throw new VStoreException(ErrorMessages.DB_LOCAL_ERROR);
		}
    }
    
    private void initializeContext() {
    	//Initialize the current context description
        mCurrentContext = new ContextDescription();
        //Check if we have persistent context in the context file
        ContextDescription tmpCtx = ContextFile.getContext();
        if(tmpCtx != null)
        {
        	mCurrentContext = tmpCtx;
        }
    }

    /**
     * Returns the current configuration containing application-defined settings.
     * 
     * @return The VStoreConfig configuration object. Will return the default
     * configuration, if you have not configured anything.
     */
    public ConfigManager getConfig() {
        return ConfigManager.getInstance(false, null);
    }
    
    /**
     * Use this method to provide new context information to the framework.
     * If the new information should be persistent after a restart of the
     * framework, {@link persistContext(true)} should be called.
     * 
     * @param context The new context information
     */
    public void provideContext(ContextDescription context) {
    	mCurrentContext = context;
    }
    
    /**
     * Use this method to make the currently configured usage context 
     * persistent after a restart of the framework.
     * 
     * @param makePersistent True if the context should be persistent.
     *                       False if you want to undo this.
     */
    public void persistContext(boolean makePersistent) {
    	if(makePersistent && mCurrentContext != null)
    	{
    		ContextFile.write(mCurrentContext.getJson());
    		return;
    	}
    	if(!makePersistent) 
    	{ 
    		ContextFile.clearContext(); 
		}
    }

    /**
     * This method returns the usage context currently used for matching.
     * If you want to refresh it, use {@link VStore#provideContext()}
     * 
     * @return A ContextDescription-object containing the current usage 
     *         context description
     */
    public final ContextDescription getCurrentContext() {
        return mCurrentContext;
    }
    
    /**
     * This method clears the current usage context and resets it 
     * to an empty state.
     * 
     * @param keepPersistent If set to true, the context currently stored
     * persistently (if any) will not be deleted. 
     */
    public void clearCurrentContext(boolean keepPersistent) {
        mCurrentContext = new ContextDescription();
        if(!keepPersistent)
        {
        	ContextFile.clearContext();
        }
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
     * @param deviceId The unique identifier of the device which wants to 
     *                 store the file 
     *
     * @return Returns "null" if an error occurred. Returns a unique 
     *         identifier if all went well. For a list of supported file types 
     *         see the class VFileType.java. Other file types will be handled 
     *         without any matching rules.
     *         
     * @throws StoreException in case something failed.
     */
    public VStoreFile store(String fileUri, boolean isPrivate, String deviceId) 
    		throws StoreException 
    {
        if(fileUri == null || fileUri.equals("") 
        		|| deviceId == null || deviceId.equals("")) 
        {
            throw new StoreException(ErrorMessages.PARAMETERS_MUST_NOT_BE_NULL);
        }
        
        File file = new File(fileUri);
               
        //Generate new UUID for file
        String uuid = UUID.randomUUID().toString();
        String descriptiveName = file.getName();
        String mimetype = "";
        String extension = "";
        
        //Derive mime type from file extension (is not the best method 
        //but will do for now)
        mimetype = FileUtils.getMimeType(file.getName());
        
        //Get file extension
        int extensionPos = file.getName().lastIndexOf('.');
        if(extensionPos != -1)
        	extension = file.getName().substring(extensionPos + 1);
      
        //Copy file into the framework folder and rename it to UUID.extension
        File fCopied = null;
        try 
        {
            fCopied = FileUtils.copyFile(file, ConfigManager.getStoredFilesDir(),
                    uuid + "." + extension);
        } 
        catch (Exception e) 
        {
        	//Abort in case of copy-error
            throw new StoreException(COPYING_INTO_FRAMEWORK_FAILED);
        }
        
        //Compute hash for file
        //TODO: Replace MD5 with something better
        String md5 = Hash.MD5.calculateMD5(fCopied);
        FileDBHelper fileDbHelper = null;
        try 
        {
        	fileDbHelper = new FileDBHelper();
        } 
        catch(DatabaseException e) 
        {
        	throw new StoreException(e.getMessage());
        }
        
        //Post an error if I already stored the same file before
        if(fileDbHelper.isAlreadyStored(md5)) 
        {
            fCopied.delete();
            //Abort in case the file already exists
            throw new StoreException(FILE_ALREADY_EXISTS);
        }
        
        VStoreFile f;
		try 
		{
			f = new VStoreFile(uuid, fCopied, descriptiveName, 
					mimetype, true, isPrivate);
		} 
		catch (FileNotFoundException e) 
		{
			throw new StoreException(COPIED_FILE_NOT_FOUND);
		}
        f.setMD5Hash(md5);
        f.setContext(mCurrentContext);

        ConfigManager vCfg = ConfigManager.getInstance(false, null);
        //Start logging for this file
        LogHandler.logStartForFile(f, vCfg.getMatchingMode());

        //Make storage matching decision for the file
        Matching matching = new Matching(f, vCfg.getMatchingMode());
        NodeInfo targetNode = matching.getDecidedNode();
        
        if(targetNode != null) 
        {
            f.setNodeUUID(targetNode.getUUID());
            
            //Insert information into local database
            try 
            {
				fileDbHelper.insertFile(f);
			} 
            catch (SQLException e) 
            {
            	//Error while storing the information in the database.
				e.printStackTrace();
				fCopied.delete();
				LogHandler.abortLoggingForFile(f.getUUID());
	            throw new StoreException(e.getMessage());
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
				fileDbHelper.insertFile(f);
			} 
            catch (SQLException e) 
            {
				e.printStackTrace();
				fCopied.delete();
				LogHandler.abortLoggingForFile(f.getUUID());
	            throw new StoreException(e.getMessage());
			}
        }

        //Log the decided node (will log null if stored on phone)
        LogHandler.logDecidedNode(f, targetNode);

        return f;
    }

    /**
     * Marks the given file for deletion.
     * Once the corresponding node has replied that the file was deleted, 
     * you will be notified of the success with the following event: 
     * {@link FileDeletedEvent}
     *
     * @param fileUUID The UUID of the file to delete.
     * @return True, in case of success.
     */
    public boolean deleteFile(String fileUUID) {
        if(fileUUID == null || fileUUID.equals("")) {
            throw new RuntimeException(
            		ErrorMessages.PARAMETERS_MUST_NOT_BE_NULL);
        }
        try
        {
	        FileDBHelper dbHelper = new FileDBHelper();
	        dbHelper.markForDeletion(fileUUID);
        }
        catch(DatabaseException | SQLException e) 
        {
        	//Something went wrong while accessing the local database
        	e.printStackTrace();
        	return false;
        }
        
        CommunicationManager.get().runDeletions();
        return true;
    }

    /**
     * Can be used to fetch a list of all files uploaded (or currently uploading) from this device.
     * Results are published in the {@link FilesReadyEvent}.
     * 
     * @param resultOrdering The order in which to fetch the files.
     *                       (see {@link DBResultOrdering}).
     * @param onlyPending If true, only files that are pending to upload 
     * 				      will be returned.
     * @param onlyPrivate If true, only files that are marked private 
     *                    will be returned.
     */
    public void getLocalFileList(DBResultOrdering resultOrdering, boolean onlyPending, 
    		boolean onlyPrivate) 
    {
    	FetchFilesFromDBThread fT;
    	switch (resultOrdering) 
    	{
            case NEWEST_FIRST:
            	fT = new FetchFilesFromDBThread(
                                    FileDBHelper.SORT_BY_DATE_DESCENDING,
                                    onlyPending,
                                    onlyPrivate);
                break;
            case OLDEST_FIRST:
            	fT = new FetchFilesFromDBThread(
                        FileDBHelper.SORT_BY_DATE_ASCENDING,
                        onlyPending,
                        onlyPrivate);
                break;
            default:
            	fT = new FetchFilesFromDBThread(
                        FileDBHelper.SORT_BY_DATE_ASCENDING,
                        onlyPending,
                        onlyPrivate);
                break;
        }
    	fT.start();
    }

    /**
     * Fetches a list of all uploads currently pending.
     * Results are published in the {@link FilesReadyEvent}.
     *
     * @param resultOrdering The order in which to return the files (e.g. newest or oldest first)
     *                       (see {@link DBResultOrdering}).
     */
    public void getPendingUploads(DBResultOrdering resultOrdering) {
    	getLocalFileList(resultOrdering, true, false);
    }

    /**
     * This method returns the number of files that are still to be uploaded 
     * by the framework.
     * 
     * @return The number of files to upload, or -1 if something went wrong.
     */
    public int getPendingUploadCount() {
    	try
    	{
	        FileDBHelper dbHelper = new FileDBHelper();
	        return dbHelper.getNumberOfFilesToUpload();
    	}
    	catch(DatabaseException | SQLException e) 
    	{
    		e.printStackTrace();
    		return -1;
    	}
    }

    /**
     * This method starts a job that queries every available storage node with 
     * the given usage context. The nodes will then deliver a list with files 
     * matching this context.
     * You can subscribe to the following event to get notified of available 
     * results from a node:
     * {@link VStore.vstoreframework.events.NewFilesMatchingContextEvent}
     *
     * To request files for a custom context, simply create a 
     * {@link ContextDescription} with the desired properties and pass it to 
     * this method.
     *
     * @param usageContext The usage context for which to find matching files.
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
        NodeManager manager = NodeManager.getInstance();
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
            		= new RequestFilesMatchingContextThread(ninfo.getUUID(), ctx, requestId);
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
     * This method returns the request address of a file's mime type from the node.
     * 
     * @param uuid The UUID of the file to request the type for.
     * @return The address to contact to retrieve the mime type of the given file.
     *         Returns null, if no uuid is given.
     */
    public String getMimetypeUriForFile(String uuid) {
        if(uuid == null) {
            throw new RuntimeException(ErrorMessages.PARAMETERS_MUST_NOT_BE_NULL);
        }
        
        String nodeId = FileNodeMapper.getMapper().getNodeId(uuid);
        if (nodeId.equals("")) return null;
    
        NodeManager manager = NodeManager.getInstance();
        return manager.getNode(nodeId).getMimeTypeUri(uuid, 
        		FrameworkUtils.getDeviceIdentifier());
    }

    /**
     * This method provides the url to request metadata for a file.
     * 
     * @param uuid The UUID of the file to request the type for.
     * @param fullMetadata If set to true, the uri for requesting full metadata 
     *                     is returned (e.g. with context information). If set 
     *                     to false, the uri for requesting lightweight metadata 
     *                     is returned.
     * @return The uri.
     */
    public String getMetadataUriForFile(String uuid, boolean fullMetadata) {
        if(uuid == null || uuid.equals("")) 
        {
            throw new RuntimeException(ErrorMessages.PARAMETERS_MUST_NOT_BE_NULL);
        }
        String nodeId = FileNodeMapper.getMapper().getNodeId(uuid);
        if(nodeId.equals("")) { return null; }
        NodeManager manager = NodeManager.getInstance();
        return manager.getNode(nodeId).getMetadataUri(uuid, 
        		FrameworkUtils.getDeviceIdentifier(), fullMetadata);
    }

    /**
     * Use this method to request the full file with the given UUID.
     * Subscribe to the event {@link DownloadProgressEvent} to get notified 
     * about the state of the request for this file UUID.
     * Additionally, listen to the {@link DownloadFailedEvent} to get 
     * notified if a file download failed.
     *
     * @param uuid The uuid of the file to retrieve.
     * @param dir If specified, the file will be placed in this directory.
     * 
     * @return True, if the download has been started. False, if not.
     */
    public boolean getFullFile(final String uuid, File dir) {
        if(uuid == null) 
        {
            throw new RuntimeException(ErrorMessages.PARAMETERS_MUST_NOT_BE_NULL);
        }
        final String requestId = UUID.randomUUID().toString();

        VStoreFile f = null;
        //Check if it is my own file
        if(isMyFile(uuid)) 
        {
        	try 
        	{
	            FileDBHelper dbHelper = new FileDBHelper();
	            f = dbHelper.getFile(uuid);
        	}
        	catch(DatabaseException | SQLException e) 
        	{
        		e.printStackTrace();
        		return false;
        	}
        }
        if(f != null) 
        {
        	//File found locally.
        	DownloadedFileReadyEvent evt = new DownloadedFileReadyEvent();
        	evt.file = f;
        	evt.requestId = requestId;
        	EventBus.getDefault().postSticky(evt);
            return true;
        }

        //File not found locally.
        //Try to get the id of the node corresponding to the file from 
        //the file<->node mapping of the last request
        String nodeId = FileNodeMapper.getMapper().getNodeId(uuid);
        if(!nodeId.equals("")) 
        { 
        	//Start download. Result will be published in DownloadProgressEvent
            return Downloader.downloadFullFileFromNode(uuid, nodeId, requestId, dir);
        } 
        else
        {
        	//No node known for the file.
            //Thus query all storage nodes for the file
            return Downloader.queryAllNodesForFile(uuid, dir);
        }
    }

    /**
     * Loads the thumbnail for the given UUID and loads it into the 
     * given image view. Will display a placeholder while loading and 
     * an error image when loading failed.
     * 
     * @param fileUuid The UUID of the file.
     * @param view The ImageView to load the file into.
     */
    /*public void requestThumbnail(String fileUuid, ImageView view) {
        Downloader.downloadThumbnail(fileUuid, view);
    }*/

    /**
     * This method downloads metadata for the given file UUID. Will only work 
     * if you are the owner of the file or if the file is public.
     * You will be notified via the {@link MetadataEvent} event when the 
     * request has finished.
     * 
     * @param fileUuid The file's UUID.
     */
    public void requestMetadata(final String fileUuid) {
        Downloader.downloadMetadata(fileUuid);
    }

    /**
     * Checks if the file with the given UUID was published before
     * by this device.
     * 
     * @param fileUuid The UUID to check
     * @return True, if this phone uploaded the file.
     */
    public static boolean isMyFile(String fileUuid) {
    	try
    	{
	        FileDBHelper dbHelper = new FileDBHelper();
	        return dbHelper.isMyFile(fileUuid);
    	} 
    	catch(DatabaseException | SQLException e) 
    	{
    		e.printStackTrace();
    		return false;
    	}
    }
    
    /**
     * This method checks, if the given string is a mime type 
     * supported by the framework. For supported types see {@link VFileType}.
     *
     * @param mimetype The mime type string, e.g. "image/jpeg".
     * @return True, if the type is supported. False, if not.
     */
    public static boolean isMimeTypeSupported(String mimetype) {
        if(mimetype == null) {
            return false;
        }
        return VFileType.isMimeTypeSupported(mimetype);
    }
}
