package vstore.framework.communication.download.threads;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

import vstore.framework.communication.master_node.MasterNode;
import vstore.framework.communication.download.events.MetadataDownloadFailedEvent;
import vstore.framework.communication.download.events.MetadataEvent;
import vstore.framework.file.MetaData;
import vstore.framework.matching.FileNodeMapper;
import vstore.framework.node.NodeInfo;
import vstore.framework.node.NodeManager;

/**
 * This thread is responsible for downloading the metadata of a file.
 * It will continue with the next storage node a file is stored on, if the previous request
 * to a storage node was not successful.
 */
public class MetadataHandlerThread extends Thread {

    private String fileId;

    public MetadataHandlerThread(String fileId) {
        this.fileId = fileId;
    }

    @Override
    public void run() {
        if(fileId == null) { return; }

        //First, get the identifiers of storage nodes where the file is stored.
        List<String> nodeIds = FileNodeMapper.getMapper().getNodeIds(fileId);
        if(nodeIds.size() == 0)
        {
            //No nodes found, so ask the master node.
            nodeIds = MasterNode.getFileNodeMapping(fileId);
            if(nodeIds.size() == 0)
            {
                //Still no nodeIds found
                downloadFailed();
                return;
            }
        }
        //Download metadata
        MetaData meta = handleMetadataDownload(nodeIds);
        if(meta != null)
        {
            publishMetadata(meta);
        }
        else
        {
            downloadFailed();
        }
    }

    private MetaData handleMetadataDownload(List<String> nodeIds) {
        //Try requesting the metadata of the file, starting with the first node
        for(String nodeId : nodeIds)
        {
            NodeManager m = NodeManager.get();
            NodeInfo node = m.getNode(nodeId);
            if (node == null)
            {
                //TODO ask master peer for node information
                System.err.println("The node with id '" + nodeId + "' cannot be found in the node manager.");
                continue;
            }
            //Execute the callable which handles the metadata download
            try
            {
                MetadataDownloadCallable callable = new MetadataDownloadCallable(fileId, node);
                MetaData result = callable.call();
                //Stop here if the request was successful
                if(result != null)
                {
                    return result;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            //Continue with next storage node
        }
        return null;
    }

    private void downloadFailed() {
        EventBus.getDefault().postSticky(new MetadataDownloadFailedEvent(fileId));
    }

    private void publishMetadata(MetaData meta) {
        EventBus.getDefault().postSticky(new MetadataEvent(fileId, meta));
    }
}
