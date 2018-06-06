package vstore.framework.communication.upload;

/**
 * Class which wraps information about an upload.
 */
public class UploadInfo {

	/**
	 * The elapsed time in milliseconds
	 */
	long mElapsedTime;
	/**
	 * The upload rate in kbit/s.
	 */
	long mUploadRate;

	/**
	 * Constructs a new upload information object.
	 * @param elapsedTime The elapsed time in milliseconds
	 * @param uploadRate The upload rate in kbit/s.
	 */
	public UploadInfo(long elapsedTime, long uploadRate) {
		mElapsedTime = elapsedTime;
		mUploadRate = uploadRate;
	}

	/**
	 * @return The elapsed time for the upload in milliseconds
	 */
	public long getElapsedTime() {
		return mElapsedTime;
	}

	/**
	 * @return the upload rate of the upload in kbit/s.
	 */
	public long getUploadRate() {
		return mUploadRate;
	}
}
