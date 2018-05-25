package vstoreframework.communication.events;

public class MetadataDownloadFailedEvent {

	public String fileId;
	
	public MetadataDownloadFailedEvent(String fileId) {
		this.fileId = fileId;
	}
	
}
