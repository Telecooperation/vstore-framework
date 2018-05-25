package vstoreframework.logging.log_events;

import vstoreframework.communication.upload.UploadInfo;

/**
 * This event gets published if a file has finished uploading. It contains information
 * that will be logged by the logger.
 */
public class LogUploadDoneEvent {

    /**
     * The UUID of the file that finished uploading.
     */
    public String fileUUID;
    /**
     * The upload info of the finished upload.
     */
    public UploadInfo uploadInfo;
    /**
     * True, if the file has been uploaded successfully. False otherwise.
     */
    public boolean success;
}
