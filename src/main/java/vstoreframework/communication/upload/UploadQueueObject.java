package vstoreframework.communication.upload;

import okhttp3.RequestBody;

public class UploadQueueObject {
	public String fileId;
	public String uploadUrl;
	public RequestBody requestBody;
	
	public int numberOfAttempts; 
	
	public UploadQueueObject() {
		numberOfAttempts = 0;
	}
	
}
