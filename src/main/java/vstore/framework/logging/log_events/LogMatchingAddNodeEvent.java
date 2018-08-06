package vstore.framework.logging.log_events;

import vstore.framework.node.NodeInfo;

/**
 * Adds the decided node to the logger for a file decision.
 */
public class LogMatchingAddNodeEvent {
    /**
     * The id of the file for which to add the decided node in the logfile.
     */
    public String fileId;
    /**
     * The NodeInfo of the decided node (can be null if only stored on the phone)
     */
    public NodeInfo node;
}
