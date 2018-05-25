package vstoreframework.communication.download;

import java.io.File;
import java.util.Map;

import vstoreframework.communication.download.threads.FileDownloadThread;
import vstoreframework.communication.download.threads.MetadataDownloadThread;
import vstoreframework.communication.events.MetadataEvent;
import vstoreframework.config.ConfigManager;
import vstoreframework.matching.FileNodeMapper;
import vstoreframework.node.NodeInfo;
import vstoreframework.node.NodeManager;
import vstoreframework.utils.FrameworkUtils;

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
			FileDownloadThread t 
				= new FileDownloadThread(uuid, node, 
						ConfigManager.getDownloadedFilesDir(), requestId);
		} 
        catch (Exception e) 
        {
			e.printStackTrace();
			return false;
		}
        return true;
    }

    /**
     * This method downloads a thumbnail directly into the given image view.
     * @param c The Android context.
     * @param uuid The uuid of the file to download the thumbnail for.
     * @param view The view to load the thumbnail into.
     */
    /*public static void downloadThumbnail( String uuid, ImageView view) {
        if(c != null && uuid != null && view != null) {
            //Trigger the download of the URL asynchronously into the image view.
            Ion.with(view)
                    .placeholder(R.mipmap.file_download)
                    .error(R.drawable.ic_unknown_file)
                    .load(getUriForThumbnail( uuid));
        }
    }*/

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
     * @return 
     */
    public static boolean queryAllNodesForFile(String fileId, File outputDir) {
    	if(fileId == null || fileId.equals("") || outputDir == null) { return false; }
    	
    	final NodeManager manager = NodeManager.getInstance();
        Map<String, NodeInfo> nodes = manager.getNodeList();
        
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
}
