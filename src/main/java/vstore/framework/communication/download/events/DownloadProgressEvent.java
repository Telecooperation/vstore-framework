package vstore.framework.communication.download.events;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;

import vstore.framework.file.MetaData;

/**
 * This event gets published regularly during a file download.
 * Contains the progress in percent as well as the request id and the file id.
 * Also contains a flag to check if the download is done.
 *
 * Will be published in sticky mode, so you have to remove it from the event bus with
 * EventBus.getDefault().removeStickyEvent(event).
 */
public class DownloadProgressEvent {

    private static final Logger LOGGER = LogManager.getLogger(DownloadProgressEvent.class);

    private String mFileUUID;
    private String mRequestId;
    private int mProgress;
    private boolean mDone;
    private URI mFileUri;
    private MetaData mMetaData;

    /**
     * Constructs a new DownloadProgressEvent object.
     * @param fileID The uuid of the file for which the progress is reported.
     * @param requestID If necessary, you can provide an ID to identify the request later on.
     * @param progress The download progress in percent.
     * @param done A flag that indicates if the download has finished.
     * @param fileUri The file object representing the file that has been downloaded. Should only be
     *                set if the progress is 100% and done is true.
     * @param meta The metadata for the download.
     */
    public DownloadProgressEvent(String fileID, String requestID, int progress, boolean done,
                                 URI fileUri, MetaData meta) {
        mFileUUID = fileID;
        mProgress = progress;
        mDone = done;
        mFileUri = fileUri;
        mRequestId = requestID;
        mMetaData = meta;
    }

    /**
     * @return The uuid of the file for which the progress is reported.
     */
    public String getFileUUID() {
        return mFileUUID;
    }

    /**
     * This download belongs to the request with the id you specified.
     * @return The request id.
     */
    public String getRequestId() {
        return mRequestId;
    }

    /**
     * @return The download progress in percent.
     */
    public int getProgress() {
        return mProgress;
    }

    /**
     * @return A flag that indicates if the download has finished.
     */
    public boolean isDone() {
        return mDone;
    }

    /**
     * Get the path to the downloaded file.
     * @return The File object representing the downloaded file. Will be null as long the download
     * has not finished yet.
     */
    public URI getFileUri() {
        return mFileUri;
    }

    public String getMimeType() {
        if(mMetaData != null) {
            return mMetaData.getMimeType();
        }
        LOGGER.warn("Meta data is null");
        return null;
    }

    public void setMetaData(MetaData meta) { mMetaData = meta; }
    public MetaData getMetaData() { return mMetaData; }

}
