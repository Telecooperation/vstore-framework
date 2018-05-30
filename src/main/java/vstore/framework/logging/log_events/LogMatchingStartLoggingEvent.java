package vstore.framework.logging.log_events;

import vstore.framework.file.VStoreFile;
import vstore.framework.matching.Matching;

/**
 * This event should be published, if the Logger should start logging for a new matching.
 */
public class LogMatchingStartLoggingEvent {
    /**
     * The file information of the file to start logging for.
     */
    public VStoreFile file;
    /**
     * The matching mode this file will be processed with.
     */
    public Matching.MatchingMode matchingMode;

    public LogMatchingStartLoggingEvent() {
        file = null;
        matchingMode = null;
    }

}
