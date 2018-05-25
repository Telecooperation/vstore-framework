package vstoreframework.rule;

/**
 * This class defines possible constraints for selecting a node using a rule.
 * (Currently not used in the framework!!)
 */
public class NodeConstraints {

    public NodeConstraints() {
        maxUploadDuration = 0;
    }

    /**
     * The maximum time in seconds an upload should take.
     */
    public int maxUploadDuration;

}
