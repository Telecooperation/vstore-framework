package vstoreframework.logging.log_events;

import vstoreframework.communication.download.Downloader;

/**
 * This event gets published by the {@link Downloader}, if a download is done.
 */
public class LogDownloadDoneEvent {

    public String fileId;
    public boolean downloadFailed;
}
