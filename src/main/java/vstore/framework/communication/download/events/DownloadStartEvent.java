package vstore.framework.communication.download.events;

import vstore.framework.file.MetaData;
import vstore.framework.node.NodeInfo;

/**
 * This event gets published when a download for a file starts.
 */
public class DownloadStartEvent {

    MetaData meta;
    NodeInfo node;

    public DownloadStartEvent(MetaData meta, NodeInfo node) {
        this.meta = meta;
        this.node = node;
    }

    /**
     * @param meta The metadata of the file which is now being downloaded.
     */
    public void setMetaData(MetaData meta) {
        this.meta = meta;
    }

    /**
     * @return The metadata of the file which is now being downloaded.
     */
    public MetaData getMetaData() {
        return meta;
    }

    /**
     * @param nodeinfo Information about the storage node from which the file is being downloaded.
     */
    public void setNodeInfo(NodeInfo nodeinfo) {
        this.node = nodeinfo;
    }
}
