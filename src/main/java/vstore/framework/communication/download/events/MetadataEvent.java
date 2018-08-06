package vstore.framework.communication.download.events;

import vstore.framework.file.MetaData;

/**
 * This event gets published once a request for metadata is done.
 */
public class MetadataEvent {

    private String mFileUUID;
    private MetaData mMeta;

    /**
     * Constructs a new MetadataEvent.
     * @param fileUUID The uuid of the file this event will be published for.
     * @param meta The metadata of the file.
     */
    public MetadataEvent(String fileUUID, MetaData meta) {
        mFileUUID = fileUUID;
        mMeta = meta;
        mMeta.setUUID(fileUUID);
    }

    /**
     * @return The uuid of the file this event was published for.
     */
    public String getFileUUID() {
        return mFileUUID;
    }

    /**
     * @return The metadata for the file this event was published for.
     */
    public MetaData getMetadata() {
        return mMeta;
    }
}
