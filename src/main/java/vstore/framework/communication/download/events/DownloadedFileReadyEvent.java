package vstore.framework.communication.download.events;

import vstore.framework.file.VStoreFile;

/**
 * This event is published, when a requested download is ready.
 */
public class DownloadedFileReadyEvent {
	public VStoreFile file;
	public String requestId;
}
