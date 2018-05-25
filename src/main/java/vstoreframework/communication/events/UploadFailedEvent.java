package vstoreframework.communication.events;

/**
 * This event gets published if an upload failed.
 */
public class UploadFailedEvent {
    /**
     * The UUID of the file that failed.
     */
    private String mUUID;
    /**
     * Denotes if the framework will try reuploading later.
     */
    private boolean mWillRetry;
    /**
     * The number of seconds from sending the event, after which the upload will be retried.
     */
    private int mRetryTime;

    /**
     * Creates a new UploadFailedEvent.
     * @param uuid The uuid of the file that failed to upload.
     * @param willRetry If set to true, this means that the framework will retry the upload
     *                  by itself later on (mostly after all other files are done).
     */
    public UploadFailedEvent(String uuid, boolean willRetry, int retryTime) {
        mUUID = uuid;
        mWillRetry = willRetry;
        mRetryTime = retryTime;
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
}
