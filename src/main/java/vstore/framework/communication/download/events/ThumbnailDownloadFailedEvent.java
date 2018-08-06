package vstore.framework.communication.download.events;

/**
 * This event gets published by the framework if the download of a thumbnail failed.
 */
public class ThumbnailDownloadFailedEvent {

    private String mFileUUID;

    /**
     * Constructs a new ThumbnailDownloadFailed event.
     * @param fileUUID The UUID of the file whose thumbnail failed to download.
     */
    public ThumbnailDownloadFailedEvent(String fileUUID) {
        mFileUUID = fileUUID;
    }

    /**
     * @return The UUID of the file whose thumbnail failed to download.
     */
    public String getFileUUID() {
        return mFileUUID;
    }
}

