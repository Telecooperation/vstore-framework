package vstoreframework.communication.download.events;

import vstoreframework.file.VStoreFile;

/**
 * This event is published, when a requested download is ready.
 */
public class DownloadedFileReadyEvent {
	public VStoreFile file;
	public String requestId;
}
