package vstore.framework.communication.download;

import java.io.File;
import java.util.Map;

import vstore.framework.communication.download.events.DownloadFailedEvent;
import vstore.framework.communication.download.events.DownloadProgressEvent;
import vstore.framework.communication.download.events.MetadataDownloadFailedEvent;
import vstore.framework.communication.download.events.MetadataEvent;
import vstore.framework.communication.download.threads.FileDownloadThread;
import vstore.framework.communication.download.threads.MetadataDownloadThread;
import vstore.framework.communication.download.threads.ThumbnailDownloadThread;
import vstore.framework.file.FileManager;
import vstore.framework.matching.FileNodeMapper;
import vstore.framework.node.NodeInfo;
import vstore.framework.node.NodeManager;
import vstore.framework.utils.FrameworkUtils;

/**
 * This class wraps some functions for downloading.
 */
@SuppressWarnings("unused")
public class Downloader {
	
    private Downloader() {}
    
    /**
     * This method starts a download for a full file, if a download for this 
     * file is not running yet.
     * 
     * @param uuid The uuid of the file to download
     * @param nodeId The node id of the node where the file is saved.
     * @param requestId The request id to publish with the events.
     * @param dir The directory that the output file should be copied into.
     * @return True, if the download was started.
     */
    public static boolean downloadFullFileFromNode(final String uuid, final String nodeId, 
    											   final String requestId, final File dir) {
    	//Don't start download again if it is currently downloading
        if(PersistentDownloadList.isFileDownloading(uuid)) return true;
       
    	//Get the node information corresponding to the node id
        NodeManager manager = NodeManager.getInstance();
        NodeInfo node = manager.getNode(nodeId);

        //Start download thread
        try 
        {
            File outputDir = FileManager.getDownloadedFilesDir();
            if(dir != null) { outputDir = dir; }

			FileDownloadThread t 
				= new FileDownloadThread(uuid, node, 
						outputDir, requestId);
            t.start();
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
     * @param uuid The file's UUID.
     */
    public static void downloadMetadata(final String uuid) {
    	if(uuid == null) { return; }
    	String nodeId = FileNodeMapper.getMapper().getNodeId(uuid);
        if(nodeId.equals("")) return;
        NodeManager m = NodeManager.getInstance();
        NodeInfo node = m.getNode(nodeId);
        if(node == null) 
        { 
        	System.err.println("The node with id '" + nodeId + "' cannot be found in the node manager.");
        	return; 
        }
        try 
        {
        	MetadataDownloadThread t = new MetadataDownloadThread(uuid, node);
            t.start();
        }
        catch(Exception e) { e.printStackTrace(); }
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
    	
    	final NodeManager manager = NodeManager.getInstance();
        Map<String, NodeInfo> nodes = manager.getNodeList();

        if(nodes.size() == 0) return false;

        for(NodeInfo n : nodes.values()) 
        {
        	downloadFullFileFromNode(fileId, n.getUUID(), "", outputDir);
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
        
    	String nodeId = FileNodeMapper.getMapper().getNodeId(uuid);
        if (nodeId.equals("")) return null;
    
        NodeManager manager = NodeManager.getInstance();
        return manager.getNode(nodeId).getThumbnailUri(uuid, FrameworkUtils.getDeviceIdentifier());
    }

    /**
     * This method downloads a thumbnail for a given file.
     * @param fileId The UUID of the file to request a thumbnail for.
     */
    public static void downloadThumbnail(String fileId) {
        String uri = getUriForThumbnail(fileId);
        try {
            String nodeId = FileNodeMapper.getMapper().getNodeId(fileId);
            if (nodeId.equals("")) return;
            NodeManager mgr = NodeManager.getInstance();

            ThumbnailDownloadThread thumbThread = new ThumbnailDownloadThread(
                    fileId,
                    mgr.getNode(nodeId),
                    FileManager.getThumbnailsDir());
            thumbThread.start();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}
