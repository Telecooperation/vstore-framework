package vstore.framework.communication.download.threads;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import vstore.framework.communication.download.DownloadMode;
import vstore.framework.communication.download.PersistentDownloadList;
import vstore.framework.communication.download.events.DownloadFailedEvent;
import vstore.framework.communication.download.events.DownloadedFileReadyEvent;
import vstore.framework.communication.master_node.MasterNode;
import vstore.framework.file.VStoreFile;
import vstore.framework.logging.LogHandler;
import vstore.framework.matching.FileNodeMapper;
import vstore.framework.node.NodeDistanceMetric;
import vstore.framework.node.NodeInfo;
import vstore.framework.node.NodeManager;

/**
 * This thread is responsible for handling the download of a file, based on the given download mode.
 *
 * For mode {@link DownloadMode#FROM_SPECIFIED_NODE},
 * please use {@link DownloadHandler#setNodeInfo(NodeInfo)} before.
 */
public class DownloadHandler extends Thread {

    private DownloadMode mode;
    private String requestId;
    private String fileId;
    private NodeInfo nodeInfo;
    private File targetDir;

    public DownloadHandler(DownloadMode mode, String fileId, File targetDir) {
        this.mode = mode;
        this.fileId = fileId;
        this.targetDir = targetDir;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public void setNodeInfo(NodeInfo n) {
        this.nodeInfo = n;
    }

    @Override
    public void run() {
        //Don't start download again if it is currently downloading
        if (PersistentDownloadList.isFileDownloading(fileId)) return;

        VStoreFile dlFile = null;
        switch(mode) {
            case FROM_SPECIFIED_NODE:
                dlFile = downloadFromSpecifiedNode();
                break;

            case BASED_ON_METRIC:
                dlFile = downloadBasedOnMetric();
                break;
        }

        if(dlFile == null) {
            downloadFailed(null);
            return;
        }

        //Publish event about the finished download
        DownloadedFileReadyEvent evt = new DownloadedFileReadyEvent();
        evt.file = dlFile;
        evt.requestId = requestId;
        EventBus.getDefault().postSticky(evt);
    }

    private VStoreFile downloadBasedOnMetric() {
        //First: Check local file<->node mapping
        List<String> nodeIds = FileNodeMapper.getMapper().getNodeIds(fileId);

        if(nodeIds.size() == 0) {
            //Contact master node for mapping
            nodeIds = MasterNode.getFileNodeMapping(fileId);
            if(nodeIds == null || nodeIds.size() == 0) {
                downloadFailed(new Exception("No node found for the file."));
                return null;
            }
        }

        if(nodeIds.size() == 1)
        {
            //Start download from the only node available
            NodeManager mgr = NodeManager.get();
            NodeInfo nodeinfo = mgr.getNode(nodeIds.get(0));
            if(nodeinfo != null) {
                this.nodeInfo = nodeinfo;
            }
            return downloadFromSpecifiedNode();
        }

        //List is larger than one element.
        //Use distance metric to decide which storage node to use.
        //First get node information for all nodes.
        List<NodeInfo> nodeInfos = new ArrayList<>();
        NodeManager mgr = NodeManager.get();
        for(String nodeId : nodeIds)
        {
            NodeInfo nodeInfo = mgr.getNode(nodeId);
            if(nodeInfo == null) { /* TODO get nodeinfo from master node*/ continue; }
            nodeInfos.add(nodeInfo);
        }
        //Sort nodes by distance metric
        List<NodeInfo> sortedNodes = NodeDistanceMetric.sortNodesByDistanceMetric(nodeInfos);
        //Then go one by one
        for(int i = 0; i <sortedNodes.size(); ++i)
        {
            NodeInfo n = sortedNodes.get(i);

            VStoreFile dlFile = tryDownload(n);
            if(dlFile == null) {
                continue;
            }

            //Publish event about the finished download
            DownloadedFileReadyEvent evt = new DownloadedFileReadyEvent();
            evt.file = dlFile;
            evt.requestId = requestId;
            EventBus.getDefault().postSticky(evt);
            return dlFile;
        }
        return null;
    }

    /**
     * Tries to download the file from the given node.
     * @param n The node from which to try the download.
     * @return True if download was successful, false if not.
     */
    private VStoreFile tryDownload(NodeInfo n) {
        try
        {
            FileDownloadCallable callable = new FileDownloadCallable(fileId, n, targetDir, requestId, false);
            return callable.call();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }

    private VStoreFile downloadFromSpecifiedNode() {
        try {
            FileDownloadCallable callable = new FileDownloadCallable(fileId, nodeInfo, targetDir, requestId, false);
            return callable.call();
        } catch(Exception e) {
            return null;
        }
    }

    private void downloadFailed(Exception e) {
        if(e != null) e.printStackTrace();
        PersistentDownloadList.deleteFileDownloading(fileId);
        //Log that the download failed
        LogHandler.logDownloadDone(fileId, true);
        EventBus.getDefault().postSticky(new DownloadFailedEvent(fileId));
    }


}
