package vstore.framework.communication.download.events;

/**
 * Published when the download of metadata has failed.
 */
public class MetadataDownloadFailedEvent {

	public String fileId;
	
	public MetadataDownloadFailedEvent(String fileId) {
		this.fileId = fileId;
	}
	
}
