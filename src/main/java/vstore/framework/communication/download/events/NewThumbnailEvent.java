package vstore.framework.communication.download.events;

import java.io.File;

/**
 * This event is published when a thumbnail has finished downloding
 */
public class NewThumbnailEvent {
    private String fileId;
    private File image;

    public NewThumbnailEvent(String fileId, File img) {
        this.fileId = fileId;
        this.image = img;
    }

    /**
     * @return The identifier of the file this thumbnail belongs to.
     */
    public String getFileId() {
        return fileId;
    }

    /**
     * @return The thumbnail image file.
     */
    public File getImageFile() {
        return image;
    }
}
