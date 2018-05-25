package vstoreframework.logging.log_events;

import vstoreframework.rule.VStoreRule;

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
     * The index of the decision layer used for matching.
     */
    public int mDecisionLayerIndex;
}
