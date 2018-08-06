package vstore.framework.communication.upload.events;

/**
 * Published when a new upload starts.
 */
public class UploadBeginEvent {
    public String fileId;
    public String nodeId;
    public int numberOfAttempt;

    public UploadBeginEvent(String fileId, String nodeId, int numberOfAttempt) {
        this.fileId = fileId;
        this.numberOfAttempt = numberOfAttempt;
    }
}
