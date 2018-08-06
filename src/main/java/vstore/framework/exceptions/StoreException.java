package vstore.framework.exceptions;

import vstore.framework.error.ErrorCode;

/**
 * This exception will be thrown, when an error occurred during storing a file
 * in the framework.
 */
@SuppressWarnings("serial")
public class StoreException extends Throwable {

    private ErrorCode errCode;

	/**
	 * @param errCode The error code of the error that occurred.
	 * @param message The cause of the exception.
	 */
	public StoreException(ErrorCode errCode, String message) {
		super(message);
		this.errCode = errCode;
	}

    /**
     * @return The error code which informs about what went wrong.
     */
    public ErrorCode getErrCode() {
        return errCode;
    }
}
