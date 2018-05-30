package vstore.framework.db;

import vstore.framework.VStore;

/**
 * This class represents the database layout for the tables of the framework database.
 *
 * See {@link DBSchema.FilesTable}, {@link DBSchema.RulesTable}, {@link DBSchema.MimesPerRuleTable}.
 */
@SuppressWarnings("unused")
public class DBSchema {
    public static final String NAME = "vStore";

    /**
     * This table contains files that were provided to the framework through the
     * {@link vStore#store(Context, Uri, boolean)} method.
     */
    public static final class FilesTable {
        public static final String __NAME = "files";

        public static final String UUID = "uuid"; // TEXT PRIMARY KEY NOT NULL
        public static final String MD5_HASH = "md5_hash"; //TEXT NOT NULL
        public static final String DESCRIPTIVE_NAME = "descriptive_name"; // TEXT NOT NULL
        public static final String MIME = "mimetype"; // TEXT
        public static final String EXTENSION = "extension"; // TEXT
        public static final String DATE_CREATION = "date_creation"; // INTEGER (unix timestamp in ms)
        public static final String SIZE = "filesize"; // INTEGER
        public static final String UPLOAD_PENDING = "upload_pending"; // INTEGER
        public static final String UPLOAD_FAILED = "upload_failed"; // INTEGER
        public static final String PRIVATE = "is_private"; // INTEGER
        public static final String NODEUUID = "node_id"; // STRING
        public static final String CONTEXTJSON = "context_json"; // TEXT
        public static final String DELETE_PENDING = "delete_pending"; //INTEGER

		private static final String[] COLUMNS = {UUID, DESCRIPTIVE_NAME, MIME, EXTENSION,
                DATE_CREATION, SIZE, UPLOAD_PENDING, PRIVATE, NODEUUID, CONTEXTJSON, DELETE_PENDING};
    }

    /**
     * This table contains rules that were provided to the framework.
     */
    public static final class RulesTable {
        public static final String __NAME = "rules";

        public static final String ID = "id"; // TEXT PRIMARY KEY NOT NULL
        public static final String NAME = "name"; // TEXT NOT NULL
        public static final String DATE_CREATION = "date_creation"; // INTEGER (unix timestamp in ms)
        public static final String CONTEXTJSON = "context_json"; // TEXT NOT NULL
        public static final String SHARING_DOMAIN = "sharing_domain"; // INTEGER NOT NULL
        public static final String IS_USER_RULE = "is_user_rule"; //INTEGER
        public static final String WEEKDAYS = "weekdays"; //TEXT
        public static final String TIME_START = "time_start"; //TEXT
        public static final String TIME_END = "time_end"; //TEXT
        public static final String FILE_SIZE = "file_size"; //INTEGER
        //public static final String NODE_BANDWIDTH_DOWN = "node_bw_down"; //INTEGER
        //public static final String NODE_BANDWIDTH_UP = "node_bw_up"; //INTEGER
        //public static final String MAX_UPLOAD_DURATION = "max_upload_duration"; //INTEGER

        private static final String[] COLUMNS = {ID, NAME, DATE_CREATION, CONTEXTJSON,
                SHARING_DOMAIN, IS_USER_RULE, WEEKDAYS, TIME_START, TIME_END};
                //NODE_BANDWIDTH_DOWN, NODE_BANDWIDTH_UP, MAX_UPLOAD_DURATION
    }

    /**
     * Each row of the table 'MimesPerRule' contains the matching of a mime type for a certain rule.
     * E.g. if a rule with id 1 has the mimetypes "image/jpeg" and "image/png", this table will
     * contain two rows.
     */
    public static final class MimesPerRuleTable {
        public static final String __NAME = "mimes_per_rule";

        public static final String ID = "id"; // INTEGER PRIMARY KEY NOT NULL
        public static final String RULE_ID = "rule_id"; // TEXT NOT NULL
        public static final String MIME = "mime_str"; // TEXT NOT NULL

        private static final String[] COLUMNS = {ID, RULE_ID, MIME};
    }

    /**
     * Each row of the table 'DecisionsPerRule' contains a decision layer for a certain rule.
     */
    public static final class DecisionsPerRuleTable {
        public static final String __NAME = "decisions_per_rule";

        public static final String ID = "id"; // INTEGER PRIMARY KEY NOT NULL
        public static final String RULE_ID = "rule_id"; // TEXT NOT NULL
        public static final String POSITION = "position"; //INTEGER NOT NULL
        public static final String IS_SPECIFIC = "is_specific"; // INTEGER
        public static final String SPECIFIC_NODE_ID = "specific_node_id"; // TEXT
        public static final String SELECTED_TYPE = "selected_type"; // TEXT
        public static final String MIN_RADIUS = "min_radius"; // REAL
        public static final String MAX_RADIUS = "max_radius"; // REAL
        public static final String MIN_BW_UP = "min_bw_up"; // INTEGER
        public static final String MIN_BW_DOWN = "min_bw_down"; // INTEGER


        private static final String[] COLUMNS = {ID, RULE_ID, POSITION, IS_SPECIFIC, SPECIFIC_NODE_ID,
                SELECTED_TYPE, MIN_RADIUS, MAX_RADIUS, MIN_BW_UP, MIN_BW_DOWN};
    }

    /**
     * This table contains the configuration data for storage nodes
     */
    public static final class NodesTable {
        public static final String __NAME = "storage_nodes";

        public static final String UUID = "uuid"; // TEXT PRIMARY KEY NOT NULL
        public static final String ADDRESS = "address"; // TEXT NOT NULL
        public static final String PORT = "port"; // INTEGER NOT NULL
        public static final String LATITUDE = "latitude"; // REAL NOT NULL
        public static final String LONGITUDE = "longitude"; // REAL NOT NULL
        public static final String TYPE = "type"; // STRING NOT NULL
        public static final String BANDWIDTH_UP = "bw_up"; // REAL
        public static final String BANDWIDTH_DOWN = "bw_down"; // REAL

        private static final String[] COLUMNS = {UUID, ADDRESS, PORT, LATITUDE, LONGITUDE, TYPE,
                BANDWIDTH_UP, BANDWIDTH_DOWN};
    }

    public static final class CurrentDownloads {
        public static final String __NAME = "current_downloads";

        public static final String FILE_UUID = "file_uuid"; // TEXT PRIMARY KEY NOT NULL
    }
}
