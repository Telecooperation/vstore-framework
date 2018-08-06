package vstore.framework.matching;

/**
 *  Describes the result of a single decision layer test.
 *  <ul>
 *      <li>{@link #DONE_TRUE}</li>
 *      <li>{@link #DONE_FALSE}</li>
 *      <li>{@link #NEXT_TEST}</li>
 *      <li>{@link #NEXT_LAYER}</li>
 *  </ul>
 */
enum NodeSelectionResult {
    /**
     * Is returned, when the evaluation of decision layers should be stopped
     * (e.g. because rule is in "single node" mode).
     * A target was found.
     */
    DONE_TRUE,

    /**
     * Is returned, when the evaluation of decision layers should be stopped
     * (e.g. because rule is in "single node" mode).
     * But no target was found.
     */
    DONE_FALSE,

    /**
     * Is returned, when the next test for this decision layer should be evaluated.
     */
    NEXT_TEST,

    /**
     * Is returned, when the next decision layer should be evaluated.
     */
    NEXT_LAYER
}