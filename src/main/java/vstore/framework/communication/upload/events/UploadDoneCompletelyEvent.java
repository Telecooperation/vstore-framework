package vstore.framework.communication.upload.events;

import java.util.ArrayList;
import java.util.List;

/**
 * This event gets published, once the file has been uploaded to all required storage nodes.
 */
public class UploadDoneCompletelyEvent {
    private String mFileUUID;
    private List<String> mStorageNodeIds;

    /**
     * Constructs a new UploadDoneCompletelyEvent.
     * @param uuid The id of the file that has finished uploading.
     * @param nodeIds A list of storage node identifiers to which the file has been uploaded.
     */
    public UploadDoneCompletelyEvent(String uuid, List<String> nodeIds) {
        mFileUUID = uuid;
        mStorageNodeIds = new ArrayList<>();
        if(nodeIds != null) {
            mStorageNodeIds.addAll(nodeIds);
        }
    }

    public String getFileId() {
        return mFileUUID;
    }

    public final List<String> getStorageNodeIds() {
        return mStorageNodeIds;
    }
}
