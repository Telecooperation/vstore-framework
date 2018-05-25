package vstoreframework.file.events;

/**
 * This event will be published once a file has been deleted from the vStore framework.
 * This event only gets published if the node the file was saved on has replied that he deleted
 * the file.
 */
public class FileDeletedEvent {
    private String mFileUUID;

    /**
     * Constructs a new FileDeleted event with the given file UUID.
     * @param fileUUID The UUID of the file that has been deleted.
     */
    public FileDeletedEvent(String fileUUID) {
        mFileUUID = fileUUID;
    }

    public String getFileUUID() {
        return mFileUUID;
    }
}
