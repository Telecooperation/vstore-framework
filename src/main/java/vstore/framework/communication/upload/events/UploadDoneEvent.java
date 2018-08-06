package vstore.framework.communication.upload.events;

/**
 * This event gets published once a file is done uploading to one storage node.
 */
public class UploadDoneEvent {
    private String mFileUUID;
    private String mNodeId;

    /**
     * Constructs a new UploadDoneEvent.
     * @param fileId The uuid of the file that has finished uploading.
     * @param nodeId The identifier of the node where the file has been uploaded to.
     */
    public UploadDoneEvent(String nodeId, String fileId) {
        mFileUUID = fileId;
        mNodeId = nodeId;
    }

    public String getFileId() {
        return mFileUUID;
    }
    public String getNodeId() { return mNodeId; }
}
