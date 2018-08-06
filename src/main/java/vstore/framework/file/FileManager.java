package vstore.framework.file;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import vstore.framework.communication.CommunicationManager;
import vstore.framework.communication.download.Downloader;
import vstore.framework.communication.download.events.DownloadFailedEvent;
import vstore.framework.communication.download.events.DownloadProgressEvent;
import vstore.framework.communication.download.events.DownloadedFileReadyEvent;
import vstore.framework.db.DBResultOrdering;
import vstore.framework.db.table_helper.FileDBHelper;
import vstore.framework.error.ErrorCode;
import vstore.framework.error.ErrorMessages;
import vstore.framework.exceptions.VStoreException;
import vstore.framework.file.events.FileDeletedEvent;
import vstore.framework.file.events.FilesReadyEvent;
import vstore.framework.file.threads.FetchFilesFromDBThread;
import vstore.framework.matching.FileNodeMapper;
import vstore.framework.node.NodeManager;
import vstore.framework.utils.IdentifierUtils;

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
    /**
     * Directory which stores the simple preference files.
     */
    private static final String PREF_FILE_DIR = "prefs/";

    private File baseDirectory;

    private static FileManager mInstance;
    private FileManager(File baseDir) {
        baseDirectory = baseDir;
        createDirectories();
    }

    public static void initialize(File baseDir) throws VStoreException {
        if(mInstance == null) {
            if(baseDir == null || !baseDir.exists())
            {
                throw new VStoreException(ErrorCode.BASE_DIRECTORY_DOES_NOT_EXIST,
                        ErrorMessages.BASE_DIRECTORY_DOES_NOT_EXIST);
            }
            mInstance = new FileManager(baseDir);
        }
    }

    /**
     * @return Gets the instance of the file manager.
     */
    public static FileManager get() {
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
            FileDBHelper.markForDeletion(fileUUID);
        }
        catch(SQLException e)
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

        List<String> nodeIds = FileNodeMapper.getMapper().getNodeIds(uuid);
        //TODO Ask master node
        if (nodeIds.size() == 0) return null;

        NodeManager manager = NodeManager.get();
        return manager.getNode(nodeIds.get(0)).getMimeTypeUri(uuid,
                IdentifierUtils.getDeviceIdentifier());
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
        List<String> nodeIds = FileNodeMapper.getMapper().getNodeIds(uuid);
        //TODO Ask master node (?)
        if(nodeIds.size() == 0) { return null; }
        NodeManager manager = NodeManager.get();
        return manager.getNode(nodeIds.get(0)).getMetadataUri(uuid,
                IdentifierUtils.getDeviceIdentifier(), fullMetadata);
    }

    /**
     * Use this method to request the full file with the given UUID.
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
        if(uuid == null)
        {
            throw new RuntimeException(ErrorMessages.PARAMETERS_MUST_NOT_BE_NULL);
        }
        final String requestId = UUID.randomUUID().toString();

        VStoreFile f = null;
        //Check if it is my own file. If it is, then serve from local storage.
        if(isMyFile(uuid))
        {
            try
            {
                f = FileDBHelper.getFile(uuid);
                //File found locally.
                if(f != null)
                {
                    DownloadedFileReadyEvent evt = new DownloadedFileReadyEvent();
                    evt.file = f;
                    evt.requestId = requestId;
                    EventBus.getDefault().postSticky(evt);
                    return;
                }
            }
            catch(SQLException e)
            {
                e.printStackTrace();
                return;
            }
        }

        //File not found locally. Use download handler.
        Downloader.downloadFile(uuid, requestId, dir);
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
            return FileDBHelper.isMyFile(fileUuid);
        }
        catch(SQLException e)
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


    /**
     * @return The base directory where vstore will store all its files.
     */
    public File getVStoreDir() {
        File vstoreDir = new File(baseDirectory, "vstore");
        vstoreDir.mkdirs();
        return vstoreDir;
    }

    /**
     * Returns the File object of the directory where the framework stores user files.
     *
     * @return A File object describing the stored files directory.
     */
    public File getStoredFilesDir() {
        File dir = new File(getVStoreDir(), STORED_FILES_DIR);
        dir.mkdirs();
        return dir;
    }
    /**
     * Returns the File object of the directory where the framework stores downloaded files.
     *
     * @return A File object describing the download directory.
     */
    public File getDownloadedFilesDir() {
        File dir = new File(getVStoreDir(), DOWNLOADED_FILES_DIR);
        dir.mkdirs();
        return dir;
    }
    /**
     * Returns the File object of the directory where the framework stores thumbnail files.
     *
     * @return A File object describing the thumbnail directory.
     */
    public File getThumbnailsDir() {
        File dir = new File(getVStoreDir(), THUMBNAILS_DIR);
        dir.mkdirs();
        return dir;
    }

    public File getPrefFileDir() {
        File dir = new File(getVStoreDir(), PREF_FILE_DIR);
        dir.mkdirs();
        return dir;
    }
}
