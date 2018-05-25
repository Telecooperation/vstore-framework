package vstoreframework.communication.events;

/**
 * This event gets published if an upload failed permanently
 * (e.g. because server replied with an error).
 */
public class UploadFailedPermanentlyEvent {
    private String mErrorMsg;

    /**
     * Constructs a new UploadFailedPermanently event with an error message.
     * @param errorMsg The error message.
     */
    public UploadFailedPermanentlyEvent(String errorMsg) {
        mErrorMsg = errorMsg;
    }

    /**
     * @return The error message explaining while the upload failed permanently.
     */
    public String getErrorMsg() {
        return mErrorMsg;
    }
}
