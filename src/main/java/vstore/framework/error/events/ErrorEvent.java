package vstore.framework.error.events;

import vstore.framework.error.ErrorCode;

/**
 * This event gets published if an error in the Framework occurred.
 * The different error codes are listed in the enum {@link ErrorCode}.
 */
public class ErrorEvent {

    /**
     * The error code of this error.
     */
    private ErrorCode mErrorCode;
    /**
     * The message of this error.
     */
    private String mMessage;
    /**
     * The vStore method or class this error occurred in.
     */
    private String mMethod;

    /**
     * Constructs a new error event.
     * @param e The error code.
     * @param message The message.
     * @param method The method in which the error occured.
     */
    public ErrorEvent(ErrorCode e, String message, String method) {
        mErrorCode = e;
        mMessage = message;
        mMethod = method;
    }

    /**
     * @return The error code of this error.
     */
    public ErrorCode getErrorCode() {
        return mErrorCode;
    }

    /**
     * @return The message of this error.
     */
    public String getMessage() {
        return mMessage;
    }

    /**
     * @return The method or class this error occurred in.
     */
    public String getMethod() {
        return mMethod;
    }
}
