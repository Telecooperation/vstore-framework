package vstore.framework.communication.upload.events;

/**
 * This event gets published once a single file is done uploading.
 */
public class SingleUploadDoneEvent {
    private String mFileUUID;

    /**
     * Constructs a new SingleUploadDoneEvent.
     * @param uuid The uuid of the file that has finished uploading.
     */
    public SingleUploadDoneEvent(String uuid) {
        mFileUUID = uuid;
    }

    public String getFileId() {
        return mFileUUID;
    }
}
