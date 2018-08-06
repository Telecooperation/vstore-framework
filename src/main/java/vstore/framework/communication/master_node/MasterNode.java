package vstore.framework.communication.master_node;

import java.util.List;

import vstore.framework.communication.master_node.file_node_mapping.DeleteFileNodeMappingCallable;
import vstore.framework.communication.master_node.file_node_mapping.PostFileNodeMappingCallable;
import vstore.framework.communication.master_node.file_node_mapping.RequestFileNodeMappingCallable;
import vstore.framework.node.NodeInfo;

/**
 * Provides simple calls for communicating with the master node.
 * The methods should be called from an own thread, or else the call will block the current thread,
 * since it involves network operations.
 */
public class MasterNode {

    public static List<String> getFileNodeMapping(String fileId) {
        RequestFileNodeMappingCallable callable = new RequestFileNodeMappingCallable(fileId);
        return callable.call();
    }

    public static boolean postFileNodeMapping(String fileId, String nodeId) {
        PostFileNodeMappingCallable callable = new PostFileNodeMappingCallable(fileId, nodeId);
        return callable.call();
    }

    public static Boolean deleteFileNodeMapping(String fileId) {
        DeleteFileNodeMappingCallable callable = new DeleteFileNodeMappingCallable(fileId);
        return callable.call();
    }

    public static Void getConfigurationFile() {
        ConfigurationDownloadCallable callable = new ConfigurationDownloadCallable(true);
        return callable.call();
    }

    public static List<NodeInfo> getStorageNodeList() {
        NodeListDownloadCallable callable = new NodeListDownloadCallable();
        return callable.call();
    }
}
