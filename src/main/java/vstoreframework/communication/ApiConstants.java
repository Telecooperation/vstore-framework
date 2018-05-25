package vstoreframework.communication;

/**
 * The REST API routes that can be appended to a node uri to fetch or send data.
 * Make sure they match with the uri in the server script.
 */
public final class ApiConstants {
    public static final String ROUTE_FILE = "/file/data";
    public static final String ROUTE_FILE_DELETE = "/file";
    public static final String ROUTE_FILE_METADATA_FULL = "/file/metadata/full";
    public static final String ROUTE_FILE_METADATA_LIGHT = "/file/metadata/light";
    public static final String ROUTE_FILE_MIMETYPE = "/file/mimetype";
    public static final String ROUTE_FILES_MATCHING_CONTEXT = "/file/search";
    public static final String ROUTE_NODE_UUID = "/uuid";
    public static final String ROUTE_THUMBNAIL = "/thumbnail";
}
