package vstore.framework.communication.upload.events;

/**
 * This event gets published when the download failed completely, regardless of the storage node.
 */
public class UploadFailedCompletelyEvent {
    private String fileId;
    private String message;

    public UploadFailedCompletelyEvent(String fileId, String message) {
        this.fileId = fileId;
        this.message = message;
    }

    public String getFileId() {
        return fileId;
    }

    public String getMessage() {
        return message;
    }
}
