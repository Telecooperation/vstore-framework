package vstore.framework.communication.download.events;

/**
 * This event gets published by the framework if the download for a file failed.
 */
public class DownloadFailedEvent {

    private String mFileUUID;

    /**
     * Constructs a new DownloadFailed event.
     * @param fileUUID The UUID of the file that failed to download.
     */
    public DownloadFailedEvent(String fileUUID) {
        mFileUUID = fileUUID;
    }

    /**
     * @return The UUID of the file that failed to download.
     */
    public String getFileUUID() {
        return mFileUUID;
    }
}
