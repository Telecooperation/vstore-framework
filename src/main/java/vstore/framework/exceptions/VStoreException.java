package vstore.framework.exceptions;

import vstore.framework.error.ErrorCode;

/**
 * This exception is thrown in case an error occured in the framework.
 * You can read the error code using {@link VStoreException#getErrorCode()} to find out what went wrong.
 */
@SuppressWarnings("serial")
public class VStoreException extends Exception {

	/**
	 * Possible error codes are listed in {@link ErrorCode}.
	 */
	private ErrorCode errorCode;

	public VStoreException(ErrorCode errorCode, String msg) {
		super(msg);
		errorCode = errorCode;
	}

    /**
     * @return An error code from {@link ErrorCode} enum.
     */
	public ErrorCode getErrorCode() {
		return errorCode;
	}
}
