package vstore.framework.communication.upload;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.RequestBody;

/**
 * An object of the upload queue.
 * Contains information which is necessary for performing the upload.
 */
public class UploadQueueObject {
	public String fileId;
	public List<String> nodeIds;
	public RequestBody requestBody;
	
	public Map<String, Integer> attemptsPerNode;
	
	public UploadQueueObject() {
		attemptsPerNode = new HashMap<>();
	}
}
