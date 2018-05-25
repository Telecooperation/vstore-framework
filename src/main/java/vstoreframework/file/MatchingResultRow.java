package vstoreframework.file;

/**
 * A MatchingResultRow is actually not really a row. But it represents a single result from
 * a context file request.
 */
public class MatchingResultRow {
    private String mUUID;
    private MetaData mMeta;

    /**
     * Constructs a new result row.
     * @param uuid The file UUID of this result row.
     * @param meta The metadata of the file.
     */
    public MatchingResultRow(String uuid, MetaData meta) {
        mUUID = uuid;
        mMeta = meta;
    }

    /**
     * @return The file UUID of this result row.
     */
    public String getUUID() {
        return mUUID;
    }

    public MetaData getMetaData() {
        return mMeta;
    }
}
