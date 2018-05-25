package vstoreframework;

import static vstoreframework.error.ErrorMessages.COPIED_FILE_NOT_FOUND;
import static vstoreframework.error.ErrorMessages.COPYING_INTO_FRAMEWORK_FAILED;
import static vstoreframework.error.ErrorMessages.FILE_ALREADY_EXISTS;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

import vstoreframework.communication.CommunicationManager;
import vstoreframework.communication.RequestFilesMatchingContextThread;
import vstoreframework.communication.download.Downloader;
import vstoreframework.communication.events.DownloadFailedEvent;
import vstoreframework.communication.events.DownloadProgressEvent;
import vstoreframework.communication.events.MetadataEvent;
import vstoreframework.communication.upload.Uploader;
import vstoreframework.config.ConfigManager;
import vstoreframework.context.ContextDescription;
import vstoreframework.context.ContextFilter;
import vstoreframework.context.SearchContextDescription;
import vstoreframework.db.DBHelper;
import vstoreframework.db.DBResultOrdering;
import vstoreframework.db.table_helper.FileDBHelper;
import vstoreframework.error.ErrorMessages;
import vstoreframework.exceptions.DatabaseException;
import vstoreframework.exceptions.StoreException;
import vstoreframework.exceptions.VStoreException;
import vstoreframework.file.VFileType;
import vstoreframework.file.VStoreFile;
import vstoreframework.file.events.FileDeletedEvent;
import vstoreframework.logging.LogHandler;
import vstoreframework.logging.LoggingService;
import vstoreframework.matching.FileNodeMapper;
import vstoreframework.matching.Matching;
import vstoreframework.node.NodeInfo;
import vstoreframework.node.NodeManager;
import vstoreframework.utils.ContextUtils;
import vstoreframework.utils.FileUtils;
import vstoreframework.utils.FrameworkUtils;
import vstoreframework.utils.Hash;

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
     * Private constructor for creating a new VStore object. It initializes needed elements:
     * - VStoreConfig
     * - DBHelper for creating necessary databases if they are not present yet.
     */
    private VStore() throws VStoreException {
        //Initialize instance of db access
    	try 
    	{
			DBHelper.getInstance();
		} 
    	catch (DatabaseException e) 
    	{
			e.printStackTrace();
			throw new VStoreException(ErrorMessages.DB_LOCAL_ERROR);
		}
		
        //Initialize the current context description
        mCurrentContext = new ContextDescription();
        
        //TODO: Replace with mandatory binding from user application
        //which has to provide context by calling a method
        //provideContext();

        //Start the logging thread
        LoggingService.getThread().start();

        //Initialize the vStore configuration instance
        //TODO: Change parameters according to user input 
        //(if user wants us to download the file)
        ConfigManager.getInstance(false, null);
        
        //Initialize the vStore FileNodeMapper
        FileNodeMapper.getMapper();

        //Clean temporary files created earlier
        //FileUtils.clearTmpDirs();
    }

    /**
     * Gets the instance of the VStore framework (singleton).
     * 
     * @return The instance of VStore framework.
     * @throws VStoreException 
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
     * This method returns the current usage context based on data collected 
     * by the AWARE framework as a Context Monitor.
     * If you want to refresh it, use {@link VStore#updateUsageContext()}
     * 
     * @return A ContextDescription-object containing the current usage 
     *         context description
     */
    public ContextDescription getCurrentContext() {
        
        /*SharedPreferences sharedPref = c.getApplicationContext()
          	.getSharedPreferences(CONTEXT_KEY_VALUE_FILE, Context.MODE_PRIVATE);
        if(sharedPref != null) {
            //LOCATION: Read most recent location from SharedPrefs
            String currentLocStr = sharedPref
            	.getString(CONTEXT_CURRENT_LOCATION_JSON_KEY, null);
            if (currentLocStr != null) {
                mCurrentContext.setLocationContext(new VLocation(currentLocStr));
            } else {
                mCurrentContext.clearLocationContext();
            }

            //PLACES: Read current nearby places from SharedPrefs
            String currentPlacesStr = sharedPref.getString(CONTEXT_PLACES_JSON_KEY, null);
            if(currentPlacesStr != null) {
                if (mCurrentContext.getPlacesList() != null) {
                    mCurrentContext.getPlacesList().clear();
                }
                mCurrentContext.setPlacesContext(new VPlaces(currentPlacesStr));
                mCurrentContext.getPlaces()
                	.calculateDistancesFrom(mCurrentContext.getLocationContext());
            } else {
                mCurrentContext.clearPlacesContext();
            }

            //ACTIVITY: Read current activity from SharedPrefs
            String currentActivityStr 
            	= sharedPref.getString(CONTEXT_ACTIVITY_JSON_KEY, null);
            if(currentActivityStr != null) {
                mCurrentContext.setActivityContext(new VActivity(currentActivityStr));
            } else {
                mCurrentContext.clearActivityContext();
            }

            //NOISE: Read current environment noise from SharedPrefs
            String noiseStr = sharedPref.getString(CONTEXT_NOISE_JSON_KEY, null);
            if(noiseStr != null) {
                mCurrentContext.setNoiseContext(new VNoise(noiseStr));
            } else {
                mCurrentContext.clearNoiseContext();
            }

            //NETWORK: Read current network context from SharedPrefs
            String networkStr = sharedPref.getString(CONTEXT_NETWORK_JSON_KEY, null);
            if(networkStr != null) {
                mCurrentContext.setNetworkContext(new VNetwork(networkStr));
            } else {
                mCurrentContext.clearNetworkContext();
            }
        }

        mCurrentContext.setDayOfWeek(ContextUtils.getDayOfWeek());*/
        return mCurrentContext;
    }

    /**
     * Use this method to provide the framework with new context.
     */
    public void newContext(ContextDescription newContext) {
        
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
            throw new StoreException(ErrorMessages.PARAMETERS_MOST_NOT_BE_NULL);
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
            up.enqueueUpload(f, deviceId);
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
            		ErrorMessages.PARAMETERS_MOST_NOT_BE_NULL);
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
     * Initializes a job in background to fetch files from the database.
     * The following event will notify you when done:
     * - FilesReadyEvent
     *
     * @param resultOrdering Should be a constant from DBResultOrdering
     *                       (see {@link DBResultOrdering}).
     * @param onlyPending If true, only files that are pending to upload 
     * 				      will be returned.
     * @param onlyPrivate If true, only files that are marked private 
     *                    will be returned.
     */
    public void getMyFiles(int resultOrdering, boolean onlyPending, 
    		boolean onlyPrivate) 
    {
        
    	//TODO
    	
    	//Get JobManager
        /*VJobManager.setContext(c.getApplicationContext());
        JobManager jobManager = VJobManager.getJobManager();
        if(jobManager != null) {
            switch (resultOrdering) {
                case DBResultOrdering.NEWEST_FIRST:
                    jobManager.addJobInBackground(
                            new FetchFilesFromDBJob(
                                    c,
                                    FileDBHelper.SORT_BY_DATE_DESCENDING,
                                    onlyPending,
                                    onlyPrivate));
                    break;
                case DBResultOrdering.OLDEST_FIRST:
                    jobManager.addJobInBackground(
                            new FetchFilesFromDBJob(
                                    FileDBHelper.SORT_BY_DATE_ASCENDING,
                                    onlyPending,
                                    onlyPrivate));
                    break;
                default:
                    jobManager.addJobInBackground(
                            new FetchFilesFromDBJob(
                                    FileDBHelper.SORT_BY_DATE_DESCENDING,
                                    onlyPending,
                                    onlyPrivate));
                    break;
            }
        }*/
    }

    /**
     * Fetches a list of all uploads currently pending.
     * The following event will notify you when the list is ready:
     * - FilesReadyEvent
     *
     * @param resultOrdering Should be a constant from DBResultOrdering
     *                       (e.g. DBResultOrdering.NEWEST_FIRST)
     */
    public void getPendingUploads(int resultOrdering) {
        getMyFiles(resultOrdering, true, false);
    }

    /**
     * This method returns the number of files that are still to be uploaded 
     * by the framework.
     * 
     * @return The number of files to upload, or -1 if something went wrong.
     */
    public int getNumberOfPendingUploads() {
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
    public boolean requestFilesMatchingContext(ContextDescription usageContext,
    		ContextFilter filter, String requestId) 
    {
        if(usageContext == null) 
        {
            throw new RuntimeException(ErrorMessages.PARAMETERS_MOST_NOT_BE_NULL);
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
            throw new RuntimeException(ErrorMessages.PARAMETERS_MOST_NOT_BE_NULL);
        }
        
        String nodeId = FileNodeMapper.getMapper().getNodeId(uuid);
        if (nodeId.equals("")) return null;
    
        NodeManager manager = NodeManager.getInstance();
        return manager.getNode(nodeId).getMimeTypeUri(uuid, 
        		FrameworkUtils.getDeviceIdentifier());
    }

    /**
     * This method provides you with the url to request metadata for the file.
     * 
     * @param uuid The UUID of the file to request the type for.
     * @param fullMetadata If set to true, the uri for requesting full metadata 
     *                     is returned (e.g. with context information). If set 
     *                     to false, the uri for requesting lightweight metadata 
     *                     is returned.
     * @return The uri.
     */
    public String getMetadataUriForFile(String uuid, boolean fullMetadata) {
        if(uuid == null || uuid.equals("")) {
            throw new RuntimeException(ErrorMessages.PARAMETERS_MOST_NOT_BE_NULL);
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
            throw new RuntimeException(ErrorMessages.PARAMETERS_MOST_NOT_BE_NULL);
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
            //Own file found locally on device. Provide it
        	//TODO
            //Downloader.provideFile(uuid, requestId, 
            //		new File(f.getFullPath()), f.getMetaData());
            //return true;
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

    /**
     * This method clears the current usage context. And resets it to an empty state.
     */
    public void clearCurrentContext() {
        mCurrentContext = new ContextDescription();
        //TODO
    }
}
