package vstore.framework.logging.log_events;

import java.util.List;

import vstore.framework.node.NodeInfo;
import vstore.framework.rule.VStoreRule;

/**
 * This event gets published, if the decision used a rule for matching.
 */
public class LogMatchingAddRuleEvent {
    /**
     * The id of the file the rule is applied to.
     */
    public String fileId;
    /**
     * The rule that is applied.
     */
    public VStoreRule rule;
    /**
     * The node information for each decision layer during the matching
     */
    public List<NodeInfo> decidedNodePerLayer;
}
