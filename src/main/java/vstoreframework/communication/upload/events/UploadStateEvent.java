package vstoreframework.communication.upload.events;

import java.io.Serializable;

/**
 * This class represents the event that gets published regularly to notify about the current
 * state of a file being uploaded.
 * If you want your app to display the progress of the upload, you need to subscribe to this event.
 */
public class UploadStateEvent implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private String mUUID;
    private int mProgress;
    private boolean mFinished;

    /**
     * Constructs a new UploadState-Event
     * @param percent The current progress of the upload (in percent)
     * @param uuid The UUID of the file currently being uploaded.
     * @param finished true if upload is finished
     */
    public UploadStateEvent(int percent, String uuid, boolean finished) {
        mProgress = percent;
        mUUID = uuid;
        mFinished = finished;
    }

    public int getProgress() {
        return mProgress;
    }
    public String getUUID() { return mUUID; }
    public boolean isFinished() { return mFinished; }

    public void setProgress(int percent) { mProgress = percent; }
    public void setFinished(boolean finished) { mFinished = finished; }
}
