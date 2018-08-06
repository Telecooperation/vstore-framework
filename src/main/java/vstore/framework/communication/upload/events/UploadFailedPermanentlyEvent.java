package vstore.framework.communication.upload.events;

/**
 * This event gets published if an upload failed permanently
 * (e.g. because server replied with an error).
 */
public class UploadFailedPermanentlyEvent {
    private String mFileId;
    private String mNodeId;
    private String mErrorMsg;

    /**
     * Constructs a new UploadFailedPermanently event with an error message.
     *
     * @param file_id The id of the file for which the upload failed
     * @param node_id The id of the node to which the upload failed.
     * @param errorMsg The error message.
     */
    public UploadFailedPermanentlyEvent(String file_id, String node_id, String errorMsg) {
        mFileId = file_id;
        mNodeId = node_id;
        mErrorMsg = errorMsg;
    }

    /**
     * @return The error message explaining while the upload failed permanently.
     */
    public String getErrorMsg() {
        return mErrorMsg;
    }

    public String getFileId() { return mFileId; }
    public String getNodeId() { return mNodeId; }
}
