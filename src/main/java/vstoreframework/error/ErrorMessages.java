package vstoreframework.error;

public class ErrorMessages {

    public static String ANDROID_CONTEXT_IS_NULL 		= "Android context must not be null!";
    public static String AWARE_NOT_INSTALLED 			= "Aware Context Middleware is not installed.";
    public static String PERMISSIONS_NOT_GRANTED 		= "Necessary permissions are not granted. Cannot start framework!";
    public static String COPYING_INTO_FRAMEWORK_FAILED 	= "Copying the file into the framework failed. Please try again!";
    public static String COPIED_FILE_NOT_FOUND 			= "Copied file not found.";
    public static String URI_SCHEME_NOT_SUPPORTED 		= "The given uri scheme is not supported by the framework.";
    public static String FILE_ALREADY_EXISTS 			= "This file is already saved. Why save it twice?";
    public static String PARAMETERS_MUST_NOT_BE_NULL 	= "Given parameters must not be null or empty!";
    public static String USAGE_CONTEXT_MUST_NOT_BE_NULL = "Usage context must not be null!";
    public static String REQUEST_NOT_STARTED 			= "The request could not be started.";
    public static String CANNOT_FETCH_RULES 			= "Cannot fetch rules.";
    public static String DB_CREATE_TABLES_EXCEPTION 	= "Error creating the database tables.";
    public static String DB_CONNECT_ERROR 				= "Error while connecting to the database.";
    public static String DB_LOCAL_ERROR					= "Local database error.";
}
