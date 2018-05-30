package vstore.framework.logging.log_events;

/**
 * This event acts as an alternative for LogUploadDoneEvent, since this will not get published,
 * if the file is only stored on the phone.
 */
public class LogMatchingDoneEvent {
    public String fileUUID;
}
