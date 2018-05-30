package vstore.framework.file.events;

import java.util.ArrayList;
import java.util.List;

import vstore.framework.file.MatchingResultRow;

/**
 * This event gets published by the framework, if a node has responded with files matching the
 * current context. Thus, each event of this type represents the reply of a search from one single node.
 */
public class NewFilesMatchingContextEvent {

    private List<MatchingResultRow> mResults;
    private String mNodeId;
    private String mRequestId;

    /**
     * Constructs a new NewFilesMatchingContext event with the given list of UUIDs that matched
     * the request.
     * @param nodeId The id of the node this event belongs to
     * @param requestId The id of the request this event belongs to
     */
    public NewFilesMatchingContextEvent(String nodeId, String requestId) {
        mNodeId = nodeId;
        mRequestId = requestId;
        mResults = new ArrayList<>();
    }

    /**
     * Adds a new result to this event.
     * @param row A result row containing uuid, mimetype and creation time.
     */
    public void addResult(MatchingResultRow row) {
        if(row != null) {
            mResults.add(row);
        }
    }

    /**
     * @return Gets the number of results in this event.
     */
    public int getCount() {
        return mResults.size();
    }

    /**
     * @return The creation time from the result at the given position.
     */
    public MatchingResultRow getResultRow(int position) {
        if(position < mResults.size()) {
            return mResults.get(position);
        }
        return null;
    }

    /**
     * @return The node id of the storage node that these files belong to.
     */
    public String getNodeId() {
        return mNodeId;
    }

    /**
     * Get the id of the request this event belongs to. This is a convenient way of keeping
     * track of your requests.
     * @return The id of the request this event belongs to.
     */
    public String getRequestId() {
        return mRequestId;
    }
}
