package vstore.framework.communication.download;

import java.io.File;
import java.util.List;
import java.util.Map;

import vstore.framework.communication.download.events.DownloadFailedEvent;
import vstore.framework.communication.download.events.DownloadProgressEvent;
import vstore.framework.communication.download.events.MetadataDownloadFailedEvent;
import vstore.framework.communication.download.events.MetadataEvent;
import vstore.framework.communication.download.threads.DownloadHandler;
import vstore.framework.communication.download.threads.MetadataHandlerThread;
import vstore.framework.communication.download.threads.ThumbnailDownloadThread;
import vstore.framework.file.FileManager;
import vstore.framework.matching.FileNodeMapper;
import vstore.framework.node.NodeInfo;
import vstore.framework.node.NodeManager;
import vstore.framework.utils.IdentifierUtils;

/**
 * This class wraps some functions for downloading.
 */
@SuppressWarnings("unused")
public class Downloader {
	
    private Downloader() {}

    /**
     * This method starts a download for a full file, if a download for this
     * file is not running yet. Will choose the best download strategy for you.
     *
     * @param uuid The uuid of the file to download
     * @param requestId The request id to publish with the events.
     * @param dir The directory that the output file should be copied into.
     * @return True, if the download was started.
     */
    public static boolean downloadFile(final String uuid, final String requestId, final File dir) {
        if(uuid == null || uuid.trim().equals("")) { return false; }

        File outputDir;
        if(dir != null && dir.exists()) { outputDir = dir; }
        else { outputDir = FileManager.get().getDownloadedFilesDir(); }

        DownloadHandler handler = new DownloadHandler(DownloadMode.BASED_ON_METRIC, uuid, outputDir);
        handler.setRequestId(requestId);
        handler.start();
        return true;
    }

    /**
     * Use this method if you want to specify the storage node from which the file should be downloaded.
     *
     * @param uuid The uuid of the file to download
     * @param nodeId The node id of the node where the file is saved (set this to null if not known).
     * @param requestId The request id to publish with the events.
     * @param dir The directory that the output file should be copied into.
     * @return True, if the download was started.
     */
    public static boolean downloadFile(final String uuid, final String nodeId,
                                       final String requestId, final File dir)
    {
        if(uuid == null || uuid.trim().equals("")) { return false; }
        NodeInfo node;
        if (nodeId == null || nodeId.trim().equals("")) {
            return downloadFile(uuid, requestId, dir);
        }
        else
        {
            //Get the node information corresponding to the node id
            NodeManager manager = NodeManager.get();
            node = manager.getNode(nodeId);
            if(node == null) {
                return false;
            }
        }

        //Start download thread
        try
        {
            File outputDir;
            if(dir != null && dir.exists()) { outputDir = dir; }
            else { outputDir = FileManager.get().getDownloadedFilesDir(); }

            DownloadHandler handler = new DownloadHandler(DownloadMode.FROM_SPECIFIED_NODE, uuid, outputDir);
            handler.setNodeInfo(node);
            handler.setRequestId(requestId);
            handler.start();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * This method downloads metadata for the given file UUID. Will only work if
     * you are the owner of the file or if the file is public.
     * You will be notified via {@link MetadataEvent} or {@link MetadataDownloadFailedEvent}
     * when the request has finished.
     *
     * @param uuid The file's unique identifier.
     */
    public static void downloadMetadata(final String uuid) {
        MetadataHandlerThread t = new MetadataHandlerThread(uuid);
        t.start();
    }
    
    /**
     * Queries all storage nodes currently in the node manager for a given
     * file uuid.
     * 
     * To monitor the download progress, subscribe to the events 
     * {@link DownloadProgressEvent} and {@link DownloadFailedEvent}.
     * 
     * @param fileId The identifier of the file.
     * @param outputDir The directory where to place the downloaded file.
     * @return True, if a request was started for one or more storage nodes.
     *         False, if no nodes available.
     */
    public static boolean queryAllNodesForFile(String fileId, File outputDir) {
    	if(fileId == null || fileId.equals("") || outputDir == null) { return false; }
    	
    	final NodeManager manager = NodeManager.get();
        Map<String, NodeInfo> nodes = manager.getNodeList();

        if(nodes.size() == 0) return false;

        for(NodeInfo n : nodes.values()) 
        {
        	downloadFile(fileId, n.getIdentifier(), "", outputDir);
        }
        return true;
    }

    /**
     * Use this method to request the thumbnail address for a given file UUID.
     * 
     * @param uuid The UUID of the file to request a thumbnail for.
     * @return The address to contact to retrieve the thumbnail for the given file.
     */
	private static String getUriForThumbnail(String uuid) {
    	if(uuid == null) return null;
        
    	List<String> nodeIds = FileNodeMapper.getMapper().getNodeIds(uuid);
        if (nodeIds.size() == 0) return null;

        //TODO: Get the thumbnail from the fastest node (?)

        NodeManager manager = NodeManager.get();
        return manager.getNode(nodeIds.get(0)).getThumbnailUri(uuid, IdentifierUtils.getDeviceIdentifier());
    }

    /**
     * This method downloads a thumbnail for a given file.
     * @param fileId The UUID of the file to request a thumbnail for.
     */
    public static void downloadThumbnail(String fileId) {
        String uri = getUriForThumbnail(fileId);
        //TODO: Get the thumbnail from the fastest node (?)
        try {
            List<String> nodeIds = FileNodeMapper.getMapper().getNodeIds(fileId);
            if (nodeIds.size() == 0) return;
            NodeManager mgr = NodeManager.get();

            ThumbnailDownloadThread thumbThread = new ThumbnailDownloadThread(
                    fileId,
                    mgr.getNode(nodeIds.get(0)),
                    FileManager.get().getThumbnailsDir());
            thumbThread.start();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}
