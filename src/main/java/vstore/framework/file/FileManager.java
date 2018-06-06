package vstore.framework.file;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.UUID;

import vstore.framework.communication.CommunicationManager;
import vstore.framework.communication.download.Downloader;
import vstore.framework.communication.download.events.DownloadFailedEvent;
import vstore.framework.communication.download.events.DownloadProgressEvent;
import vstore.framework.communication.download.events.DownloadedFileReadyEvent;
import vstore.framework.db.DBResultOrdering;
import vstore.framework.db.table_helper.FileDBHelper;
import vstore.framework.error.ErrorMessages;
import vstore.framework.exceptions.DatabaseException;
import vstore.framework.file.events.FileDeletedEvent;
import vstore.framework.file.events.FilesReadyEvent;
import vstore.framework.file.threads.FetchFilesFromDBThread;
import vstore.framework.matching.FileNodeMapper;
import vstore.framework.node.NodeManager;
import vstore.framework.utils.FrameworkUtils;

public class FileManager {
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

    private static FileManager mInstance;
    private FileManager() {
        createDirectories();
    }

    public static FileManager getInstance() {
        if(mInstance == null) {
            mInstance = new FileManager();
        }
        return mInstance;
    }

    private void createDirectories() {
        getStoredFilesDir();
        getDownloadedFilesDir();
        getThumbnailsDir();
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
     * Checks if the file with the given UUID was published before
     * by this device.
     *
     * @param fileUuid The UUID to check
     * @return True, if this phone uploaded the file.
     */
    public boolean isMyFile(String fileUuid) {
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
        return VFileType.isMimeTypeSupported(mimetype);
    }

    /**
     * Get a list of file types that are supported by the vStore framework.
     * @return Returns a HashMap where the keys represent file extensions and the values
     *         represent the corresponding MIME type.
     */
    public HashMap<String, String> getSupportedTypes() {
        return VFileType.getSupportedTypes();
    }


    public static String getVStoreDir() {
        return System.getProperty("user.dir") + "vstore";
    }

    /**
     * Returns the File object of the directory where the framework stores user files.
     *
     * @return A File object describing the stored files directory.
     */
    public static File getStoredFilesDir() {
        File dir = new File(getVStoreDir(), STORED_FILES_DIR);
        dir.mkdirs();
        return dir;
    }
    /**
     * Returns the File object of the directory where the framework stores downloaded files.
     *
     * @return A File object describing the download directory.
     */
    public static File getDownloadedFilesDir() {
        File dir = new File(getVStoreDir(), DOWNLOADED_FILES_DIR);
        dir.mkdirs();
        return dir;
    }
    /**
     * Returns the File object of the directory where the framework stores thumbnail files.
     *
     * @return A File object describing the thumbnail directory.
     */
    public static File getThumbnailsDir() {
        File dir = new File(getVStoreDir(), THUMBNAILS_DIR);
        dir.mkdirs();
        return dir;
    }
}
