package vstore.framework.exceptions;

/**
 * This exception will be thrown, when an error occurred during storing a file
 * in the framework.
 */
@SuppressWarnings("serial")
public class StoreException extends Throwable {

	/**
	 * @param message The cause of the exception.
	 */
	public StoreException(String message) {
		super(message);
	}
}
