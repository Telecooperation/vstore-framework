package vstore.framework.error;

public class ErrorMessages {
    public static String BASE_DIRECTORY_DOES_NOT_EXIST  = "The provided base directory does not exist";
    public static String CONFIG_DOWNLOAD_FAILED         = "The download of the configuration file was not successful";
    public static String COPYING_INTO_FRAMEWORK_FAILED 	= "Copying the file into the framework failed. Please try again!";
    public static String COPIED_FILE_NOT_FOUND 			= "Copied file not found.";
    public static String URI_SCHEME_NOT_SUPPORTED 		= "The given uri scheme is not supported by the framework.";
    public static String FILE_ALREADY_EXISTS 			= "This file is already saved. Why save it twice?";
    public static String PARAMETERS_MUST_NOT_BE_NULL 	= "Given parameters must not be null or empty!";
    public static String USAGE_CONTEXT_MUST_NOT_BE_NULL = "Usage context must not be null!";
    public static String REQUEST_FAILED                 = "The request to the given url failed.";
    public static String RESPONSE_WRONG_STATUS_CODE     = "Wrong HTTP status code received!";
    public static String CANNOT_FETCH_RULES 			= "Cannot fetch rules.";
    public static String DB_CREATE_TABLES_EXCEPTION 	= "Error creating the database tables.";
    public static String DB_CONNECT_ERROR 				= "Error while connecting to the database.";
    public static String DB_LOCAL_ERROR					= "Local database error.";
    public static String JSON_PARSING_FAILED            = "Response not in valid JSON format.";

    public static String MASTERPEER_WRONG_REPLY         = "Received a wrong reply from the master peer!";
}
