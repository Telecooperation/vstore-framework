package vstoreframework.rule.events;

import java.util.ArrayList;
import java.util.List;

import vstoreframework.rule.VStoreRule;

/**
 * This event gets published once the framework has fetched the rules from the database.
 * If you want your app to get notified, you need to subscribe to this event.
 */
public class RulesReadyEvent {
    private List<VStoreRule> mRules;

    /**
     * Creates a new RulesReady event.
     * @param rules The rules to publish with this event.
     */
    public RulesReadyEvent(List<VStoreRule> rules) {
        mRules = new ArrayList<>(rules);
    }

    /**
     * @return The rules that were fetched from the framework with this event.
     */
    public final List<VStoreRule> getRules() {
        return mRules;
    }
}
