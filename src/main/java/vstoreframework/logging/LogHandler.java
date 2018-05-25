package vstoreframework.logging;

import org.greenrobot.eventbus.EventBus;

import vstoreframework.file.VStoreFile;
import vstoreframework.logging.log_events.LogCancelLoggingEvent;
import vstoreframework.logging.log_events.LogDownloadDoneEvent;
import vstoreframework.logging.log_events.LogDownloadStartEvent;
import vstoreframework.logging.log_events.LogMatchingAddNodeEvent;
import vstoreframework.logging.log_events.LogMatchingDoneEvent;
import vstoreframework.logging.log_events.LogMatchingStartLoggingEvent;
import vstoreframework.matching.Matching.MatchingMode;
import vstoreframework.node.NodeInfo;
import vstoreframework.node.NodeType;

/**
 * Some code for posting the logging events. 
 * Placed here to clean up the main code.
 *
 */
public class LogHandler {
	private LogHandler() {}
	
	public static void logStartForFile(VStoreFile f, MatchingMode matchMode) {
		LogMatchingStartLoggingEvent logEvt = new LogMatchingStartLoggingEvent();
        logEvt.file = f;
        logEvt.matchingMode = matchMode;
        EventBus.getDefault().post(logEvt);
	}
	
	public static void abortLoggingForFile(String uuid) {
		LogCancelLoggingEvent logEvt = new LogCancelLoggingEvent(uuid);
		EventBus.getDefault().post(logEvt);
	}
	
	public static void logDecidedNode(VStoreFile f, NodeInfo targetNode) {
		LogMatchingAddNodeEvent logEvt2 = new LogMatchingAddNodeEvent();
        logEvt2.fileId = f.getUUID();
        logEvt2.node = targetNode;
        EventBus.getDefault().post(logEvt2);
        if(targetNode == null) 
        {
            //Post event that logging is done
            LogMatchingDoneEvent logEvt3 = new LogMatchingDoneEvent();
            logEvt3.fileUUID = f.getUUID();
            EventBus.getDefault().post(logEvt3);
        }
	}
	
	public static void logDownloadStart(String uuid, long bytes, String metaJson, String nodeId, NodeType nodetype) {
		LogDownloadStartEvent logEvt = new LogDownloadStartEvent();
        logEvt.fileId = uuid;
        logEvt.fileSize = bytes;
        logEvt.metadata = metaJson;
        logEvt.nodeId = nodeId;
        logEvt.nodeType = nodetype;
        EventBus.getDefault().post(logEvt);
	}
	
	public static void logDownloadDone(String fileUuid, boolean dlFailed) {
		LogDownloadDoneEvent logEvt = new LogDownloadDoneEvent();
        logEvt.fileId = fileUuid;
        logEvt.downloadFailed = dlFailed;
        EventBus.getDefault().post(logEvt);
	}
}
