package vstoreframework.logging.log_events;

import vstoreframework.node.NodeType;

/**
 * This event gets published by the {@link vstoreframework.communication.download.Downloader}
 * if a new download has been started.
 */
public class LogDownloadStartEvent {

    public String fileId;
    public long fileSize;
    public String metadata;
    public String nodeId;
    public NodeType nodeType;
}
