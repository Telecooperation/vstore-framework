package vstore.framework.communication;

/**
 * The REST API routes that can be appended to a node uri to fetch or send data.
 * Make sure they match with the uri in the server script.
 */
public final class ApiConstants {

    public final class StorageNode {
        public static final String ROUTE_FILE = "/file/data";
        public static final String ROUTE_FILE_DELETE = "/file";
        public static final String ROUTE_FILE_METADATA_FULL = "/file/metadata/full";
        public static final String ROUTE_FILE_METADATA_LIGHT = "/file/metadata/light";
        public static final String ROUTE_FILE_MIMETYPE = "/file/mimetype";
        public static final String ROUTE_FILES_MATCHING_CONTEXT = "/file/search";
        public static final String ROUTE_NODE_UUID = "/uuid";
        public static final String ROUTE_THUMBNAIL = "/thumbnail";
    }

    public final class MasterNode {
        public static final String API_V1 = "/v1";
        public static final String ROUTE_FILE_NODE_MAPPING = API_V1 + "/file_node_mapping";
        public static final String ROUTE_VSTORE_CONFIG_URL = API_V1 + "/configuration";
        public static final String ROUTE_NODES_INFORMATION = API_V1 + "/nodes";
    }
}
