package vstore.framework.config.events;

import vstore.framework.error.ErrorCode;

/**
 * This event gets published when the download of the configuration file failed.
 */
public class ConfigDownloadFailedEvent {

    public ErrorCode errorCode;
    public String errorMsg;
}
