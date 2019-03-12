package vstore.framework.logging;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

import vstore.framework.file.VStoreFile;
import vstore.framework.logging.log_events.LogCancelLoggingEvent;
import vstore.framework.logging.log_events.LogDownloadDoneEvent;
import vstore.framework.logging.log_events.LogDownloadStartEvent;
import vstore.framework.logging.log_events.LogMatchingAddNodeEvent;
import vstore.framework.logging.log_events.LogMatchingAddRuleEvent;
import vstore.framework.logging.log_events.LogMatchingDoneEvent;
import vstore.framework.logging.log_events.LogMatchingStartLoggingEvent;
import vstore.framework.matching.Matching.MatchingMode;
import vstore.framework.node.NodeInfo;
import vstore.framework.node.NodeType;
import vstore.framework.rule.VStoreRule;

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
        logEvt2.fileId = f.getUuid();
        logEvt2.node = targetNode;
        EventBus.getDefault().post(logEvt2);
        if(targetNode == null) 
        {
            //Post event that logging is done
            LogMatchingDoneEvent logEvt3 = new LogMatchingDoneEvent();
            logEvt3.fileUUID = f.getUuid();
            EventBus.getDefault().post(logEvt3);
        }
	}

    public static void logDecidedNode(VStoreFile f, NodeInfo targetNode, long time) {
        LogMatchingAddNodeEvent logEvt2 = new LogMatchingAddNodeEvent();
        logEvt2.fileId = f.getUuid();
        logEvt2.node = targetNode;
        logEvt2.matchingTime = time;
        EventBus.getDefault().post(logEvt2);
        if(targetNode == null)
        {
            //Post event that logging is done
            LogMatchingDoneEvent logEvt3 = new LogMatchingDoneEvent();
            logEvt3.fileUUID = f.getUuid();
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

	public static void logMatchingAddRule(VStoreFile f, VStoreRule usedRule, List<NodeInfo> nodes) {
        LogMatchingAddRuleEvent logEvt = new LogMatchingAddRuleEvent();
        logEvt.fileId = f.getUuid();
        logEvt.rule = usedRule;
        ///Will hold the index of the used decision layer of the rule.
        List<Integer> usedDecisionLayerIndices;
        logEvt.decidedNodePerLayer = nodes;
        EventBus.getDefault().post(logEvt);
    }
}
