package vstore.framework.logging.log_events;

import vstore.framework.communication.download.Downloader;

/**
 * This event gets published by the {@link Downloader}, if a download is done.
 */
public class LogDownloadDoneEvent {

    public String fileId;
    public boolean downloadFailed;
}
