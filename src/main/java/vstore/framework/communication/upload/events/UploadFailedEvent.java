package vstore.framework.communication.upload.events;

/**
 * This event gets published if an upload failed.
 */
public class UploadFailedEvent {
    /**
     * The UUID of the file that failed.
     */
    private String mUUID;

    /**
     * The identifier of the node for which the upload failed.
     */
    private String mNodeId;

    /**
     * Denotes if the framework will try reuploading later.
     */
    private boolean mWillRetry;
    /**
     * The number of seconds from sending the event, after which the upload will be retried.
     */
    private int mRetryTime;

    private int mNumberAttempts;

    /**
     * Creates a new UploadFailedEvent.
     * @param uuid The uuid of the file that failed to upload.
     * @param nodeId The id of the node for which the upload failed.
     * @param willRetry If set to true, this means that the framework will retry the upload
     *                  by itself later on (mostly after all other files are done).
     * @param retryTime The time in seconds, after which we will retry.
     * @param numberOfAttempt The number of the next attempt.
     */
    public UploadFailedEvent(String uuid, String nodeId, boolean willRetry, int retryTime, int numberOfAttempt) {
        mUUID = uuid;
        this.mNodeId = nodeId;
        mWillRetry = willRetry;
        mRetryTime = retryTime;
        mNumberAttempts = numberOfAttempt;
    }

    /**
     * @return The UUID of the file that failed uploading.
     */
    public String getUUID() {
        return mUUID;
    }

    /**
     * @return True, if the framework decided to retry the upload later.
     */
    public boolean willRetry() {
        return mWillRetry;
    }

    /**
     * @return The number of seconds, after which the upload for this file will be retried.
     */
    public int getRetryTime() {
        return mRetryTime;
    }

    /**
     * @return The number of the next attempt.
     */
    public int getNumberOfAttempt() { return mNumberAttempts; }

    /**
     * @return The identifier of the node for which the upload failed.
     */
    public String getNodeId() {
        return mNodeId;
    }
}
