package vstore.framework.node;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.greenrobot.eventbus.EventBus;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import vstore.framework.communication.master_node.MasterNode;
import vstore.framework.communication.threads.FetchNodeIdentifierCallable;
import vstore.framework.context.types.location.VLatLng;
import vstore.framework.db.table_helper.NodeDBHelper;
import vstore.framework.utils.ContextUtils;

/**
 * This class is responsible for managing information about the storage nodes for the
 * vStore framework.
 * The storage node list is currently just a hard-coded list of nodes.
 */
public class NodeManager {
    /**
     * The modes in this enumeration specify, how a node should be determined.
     * If you pass Mode.RANDOM, a random node of the result set will be selected.
     * If you pass Mode.NEAREST, the node that has the shortest linear distance will be selected.
     */
    public enum Mode { RANDOM, NEAREST }

    private static NodeManager mInstance;
    /**
     * This hash-map stores a hash-map for each node type.
     */
    private HashMap<NodeType, HashMap<String, NodeInfo>> mNodes;

    private int mNodeCount;

    Random mRnd;

    private NodeManager() {
        mRnd = new Random();
        mNodes = new HashMap<>();
        for(NodeType t : NodeType.values()) 
        {
            //Create a hash-map for each type.
            mNodes.put(t, new HashMap<String, NodeInfo>());
        }
        mNodeCount = 0;
        refreshNodes();
    }

    public static void initialize() {
        if (mInstance == null)
        {
            mInstance = new NodeManager();
        }
    }

    /**
     * Gets the instance of the NodeManager.
     * @return The instance of the NodeManager.
     */
    public static NodeManager get() {
        initialize();
        return mInstance;
    }

    /**
     * Refreshes the node list by reading all nodes from the database.
     */
    public void refreshNodes() {
    	List<NodeInfo> nodes;
    	try 
		{
        	nodes = NodeDBHelper.getNodes();
		} 
    	catch (SQLException e) {
			e.printStackTrace();
			return;
		}
		for(HashMap<String, NodeInfo> map : mNodes.values()) 
        {
            map.clear();
        }
        mNodeCount = 0;
        for(NodeInfo n : nodes) 
        {
            mNodes.get(n.getNodeType()).put(n.getIdentifier(), n);
            mNodeCount++;
        }
    }

    /**
     * Downloads only the storage node information from the master node.
     * @param block Set this to true, if the download should block.
     *              Set this to false, if it should not block.
     *              In the latter case, this function will return null since the result is not known yet.
     *              Will also return null in case an error occurred.
     *
     * @return If the block-parameter is true, a list of the retrieved NodeInfos will be returned.
     * Null in other cases, since the task is running in the background.
     */
    public List<NodeInfo> downloadNodeInfoFromMaster(boolean block) {
        //Try to download node information on new thread.
        ExecutorService executor = Executors.newFixedThreadPool(1);
        FutureTask<List<NodeInfo>> futureTask = new FutureTask<>(MasterNode::getStorageNodeList);
        executor.execute(futureTask);

        if(!block) { return null; }
        try {
            return futureTask.get();
        }
        catch (InterruptedException | ExecutionException e)
        {
            e.printStackTrace();
            NodeInformationDownloadFailedEvent evt = new NodeInformationDownloadFailedEvent();
            EventBus.getDefault().post(evt);
        }
        return null;
    }

    /**
     * Adds a new storage node to the node manager.
     * Once the connection to the node has been confirmed, and once the node has replied with its
     * UUID and type, it will be persisted to the local framework database.
     * This method will block until the node has replied or will timeout after 5 seconds.
     *
     * @param n The NodeInfo of the new storage node to add.
     * @return The final configuration of the node, if the node has replied and was successfully added to the framework.
     * Null, if the node did not reply or if a timeout occurred.
     */
    public NodeInfo addNode(NodeInfo n) {
        if (n == null) { return null; }
        if (n.getIdentifier() == null || "".equals(n.getIdentifier())
                || n.getNodeType().equals(NodeType.UNKNOWN))
        {
            //UUID or node-type is unknown, thus we need to contact the node.
            if(n.getAddress() != null && !n.getAddress().equals("") && n.getPort() != 0)
            {
                //Try to contact node. Will block while doing this.
                ExecutorService executor = Executors.newFixedThreadPool(1);
                FetchNodeIdentifierCallable myCallable = new FetchNodeIdentifierCallable(n);
                FutureTask<NodeReply> futureTask = new FutureTask<>(myCallable);
                executor.execute(futureTask);

                NodeReply nodeReply = null;
                try {
                    nodeReply = futureTask.get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }

                if(nodeReply == null)
                {
                    return null;
                }
                else
                {
                    n.setUUID(nodeReply.getNodeId());
                    n.setNodeType(nodeReply.getNodeType());
                    //Node will be added to the node manager below
                }
            }
        }
        try
        {
            //If node is already contained in the list, update its information
            if (mNodes.get(n.getNodeType()).containsKey(n.getIdentifier()))
            {
                NodeDBHelper.updateNode(n);
            }
            else
            {
                NodeDBHelper.insertNode(n);
            }
            mNodes.get(n.getNodeType()).put(n.getIdentifier(), n);
            mNodeCount++;
        }
        catch (SQLException e)
        {
            e.printStackTrace();
            return null;
        }
        return n;
    }

    /**
     * @param uuid Returns the node info for the node with the given UUID
     * @return The node info or null, if node cannot be found.
     */
    public NodeInfo getNode(String uuid) {
        if(mNodes != null && uuid != null) 
        {
            for(HashMap<String, NodeInfo> map : mNodes.values()) 
            {
                if(map.containsKey(uuid)) 
                {
                    return map.get(uuid);
                }
            }
        }
        return null;
    }

    /**
     * @return A mapping of "Node ID" -- "Node info" containing all nodes currently in the manager.
     */
    public final Map<String, NodeInfo> getNodeList() {
        HashMap<String, NodeInfo> result = new HashMap<>();
        for(HashMap<String, NodeInfo> map : mNodes.values()) {
            result.putAll(map);
        }
        return result;
    }

    /**
     * Deletes a node from the node manager and the database.
     * @param uuid The UUID of the node to delete.
     */
    public void deleteNode(String uuid) {
        for(HashMap<String, NodeInfo> map : mNodes.values()) 
        {
            if (!map.containsKey(uuid)) continue;
            
            try 
            {
            	NodeDBHelper.deleteNode(uuid);
	            map.remove(uuid);
            } 
            catch (SQLException e)
            {
				e.printStackTrace();
			}
        }
    }

    /**
     * Updates the configuration for the given node and saves it to the database.
     * @param n The updated node information
     */
    public void updateNode(NodeInfo n) {
    	try
    	{
	        for(HashMap<String, NodeInfo> map : mNodes.values())
	        {
	            if (!map.containsKey(n.getIdentifier())) continue;
	            NodeDBHelper.updateNode(n);
	        }
    	}
    	catch(SQLException e)
    	{
    		e.printStackTrace();
    	}
    }

    /**
     * Deletes all nodes from the node manager and the database.
     */
    public void clearNodes() {
        for(HashMap<String, NodeInfo> map : mNodes.values()) 
        {
            map.clear();
        }
        try
    	{
	        NodeDBHelper.deleteAllNodes();
	        mNodeCount = 0;
    	}
        catch(SQLException e)
    	{
    		e.printStackTrace();
    	}
    }

    /**
     * Returns a list with nodes of the given NodeType.
     * @param type The NodeType to look for in the nodes list.
     * @return A list of nodes that are of the given NodeType.
     */
    public List<NodeInfo> getNodesOfType(NodeType type) {
        return new ArrayList<>(mNodes.get(type).values());
    }

    /**
     * Returns the nearest node that is of the given type.
     * @param type The {@link NodeType} to look for.
     * @param location Each node is compared against this distance.
     * @return The {@link NodeInfo} of the nearest node that is of the given type.
     */
    public NodeInfo getNearestNodeOfType(NodeType type, VLatLng location) {
        double distance = Double.MAX_VALUE;
        NodeInfo node = null;
        for(NodeInfo n : mNodes.get(type).values()) 
        {
            double nodeDist = ContextUtils.distanceBetween(n.getLatLng(), location);
            if(nodeDist < distance) 
            {
                distance = nodeDist;
                node = n;
            }
        }
        return node;
    }

    /**
     * Gets the N closest nodes according to the limit (or less, if only less are available).
     * 
     * @param type The node type to get N nearest nodes for.
     * @param location The location to compare against.
     * @param N The number of nodes N that should be contained in the result.
     * @return A list containing N or less nodes of the given type that are closest.
     */
    public List<NodeInfo> get_N_NearestNodesOfType(NodeType type, VLatLng location, int N) {
        List<NodeInfo> nodes = getNodesOfType(type);
        if(nodes.size() <= N) 
        {
            return nodes;
        }
        Map<Integer, Pair<Integer, Float>> nearestPos = new HashMap<>();
        for(int i = 0; i<nodes.size(); i++) 
        {
            NodeInfo node = nodes.get(i);
            float newDist = ContextUtils.distanceBetween(node.getLatLng(), location);
            if(nearestPos.size() < N) 
            {
                //First N values will be simply put into the list.
                nearestPos.put(i, new ImmutablePair<Integer, Float>(i, newDist));
            } else {
                //Then we need to check.
                for(int j=0; j<nearestPos.size(); ++j) 
                {
                    if(newDist < nearestPos.get(j).getRight()) 
                    {
                        nearestPos.put(j, new ImmutablePair<Integer, Float>(i, newDist));
                        break;
                    }
                }
            }
        }
        List<NodeInfo> result = new ArrayList<>();
        for(int i=0; i<nearestPos.size(); i++) 
        {
            result.add(nodes.get(nearestPos.get(i).getLeft()));
        }
        return result;
    }

    /**
     * Returns the nearest node that is of one of the given types.
     * If you specify the third parameter, one is randomly chosen among the n closest ones.
     *
     * @param types The array of {@link NodeType}s to look for.
     * @param location Each node is compared against this distance.
     * @param randomFactor If this number is greater than 0, we take nodes of the count of this number
     *                     and randomly select one among those.
     * @return The {@link NodeInfo} of the nearest node that is of one of the given types.
     */
    public NodeInfo getNearestNodeOfTypes(NodeType[] types, VLatLng location, int randomFactor) {
        if(randomFactor <= 0) 
        {
            NodeInfo node = null;
            float lastDistance = 0;
            for (NodeType t : types) 
            {
                NodeInfo newNode = getNearestNodeOfType(t, location);
                if (node == null) 
                {
                    node = newNode;
                    lastDistance = ContextUtils.distanceBetween(newNode.getLatLng(), location);
                } 
                else 
                {
                    float dist = ContextUtils.distanceBetween(node.getLatLng(), location);
                    if (dist < lastDistance) 
                    {
                        node = newNode;
                        lastDistance = dist;
                    }
                }
            }
            return node;
        } 
        else 
        {
            //Put n nodes according to the randomFactor into the list for each type.
            //Then select one randomly.
            List<NodeInfo> nodes = new ArrayList<>();
            for(NodeType t : types) 
            {
                nodes.addAll(get_N_NearestNodesOfType(t, location, randomFactor));
            }
            return getRandomNodeFromList(nodes);
        }
    }

    /**
     * Gets the first node that is in the list that is of the given type.
     * 
     * @param type The {@link NodeType} to look for.
     * @return The {@link NodeInfo} describing the node that was found.
     * Or null, if no node of the given type was found.
     */
    public NodeInfo getFirstNodeOfType(NodeType type) {
        if(mNodes.get(type).size() > 0) 
        {
            for(NodeInfo n : mNodes.get(type).values()) 
            {
                return n;
            }
        }
        return null;
    }

    /**
     * Gets a random node from one of the available nodes of the given types.
     * 
     * @param types The {@link NodeType}s to look for.
     * @return The {@link NodeInfo} describing the node that was found.
     * Or null, if no node of the given type was found.
     */
    public NodeInfo getRandomNodeOfTypes(NodeType[] types) {
        List<NodeInfo> list = getNodesOfTypes(types);
        return getRandomNodeFromList(list);
    }

    /**
     * Returns a random node from the given list.
     * 
     * @param nodes The list of nodes.
     * @return A random node information from the given list.
     */
    public NodeInfo getRandomNodeFromList(List<NodeInfo> nodes) {
        int listSize = nodes.size();
        if(listSize > 0) 
        {
            //Get a random number and return the corresponding node information
            return nodes.get(mRnd.nextInt(nodes.size()));
        }
        return null;
    }

    /**
     * Returns the nearest node from the given list
     * 
     * @param nodes The list of nodes
     * @param loc The location
     * @return The nearest node information.
     */
    public NodeInfo getNearestNodeFromList(List<NodeInfo> nodes, VLatLng loc) {
        int listSize = nodes.size();
        if(listSize > 0) 
        {
            NodeInfo node = nodes.get(0);
            float lastDistance = nodes.get(0).getGeographicDistanceTo(loc);
            for (NodeInfo n : nodes) 
            {
                float dist = n.getGeographicDistanceTo(loc);
                if(dist < lastDistance) 
                {
                    node = n;
                    lastDistance = dist;
                }
            }
            return node;
        }
        return null;
    }

    /**
     * Gets any random node.
     * 
     * @return The {@link NodeInfo} describing the node that was found.
     * Or null, if no nodes are available
     */
    public NodeInfo getRandomNode() {
        if(getNodeCount() > 0) 
        {
            List<NodeInfo> nodes = new ArrayList<>(getNodeList().values());
            //Return the corresponding node information
            return nodes.get(mRnd.nextInt(nodes.size()));
        }
        return null;
    }

    /**
     * You can specify a list of types and the result will contain all nodes of these types.
     *
     * @param types The node types you want to get the nodes for.
     * @return The list of nodes for the given types.
     */
    public List<NodeInfo> getNodesOfTypes(NodeType[] types) {
        List<NodeInfo> result = new ArrayList<>();
        for(NodeType t : types) 
        {
            HashMap<String, NodeInfo> nodesOfType = mNodes.get(t);
            result.addAll(nodesOfType.values());
        }
        return result;
    }

    /**
     * Will go through all nodes of different types following the given hierarchy. If no node
     * of the first level is found, it will search the next node type level of the given hierarchy.
     * E.g. if you specify CLOUDLET and CLOUD, at first it is tried to fetch a node of type
     * CLOUDLET. If none is found, the next level (CLOUD) is checked. If still no node has been found,
     * null will be returned.
     * 
     * @param hierarchy The hierarchy of node types.
     * @param mode The mode how to select a node from a hierarchy-level, see {@link Mode}.
     * @param location Has to be specified if you set the mode parameter to {@link Mode#NEAREST}.
     * @return The NodeInfo of a node if one has been found. Null otherwise.
     */
    public NodeInfo getNodeFollowingHierarchy(List<NodeType> hierarchy, Mode mode, VLatLng location) {
        if(hierarchy == null || (mode.equals(Mode.NEAREST) && location == null)) {
            return null;
        }
        NodeInfo n = null;
        //Go through hierarchy
        for(NodeType t : hierarchy) 
        {
            if(mode == Mode.RANDOM) 
            {
                n = getRandomNodeOfTypes(new NodeType[] { t });
            } 
            else if(mode == Mode.NEAREST)
            {
                n = getNearestNodeOfType(t, location);
            }
            //Break if node has been found
            if(n != null) break;
        }
        return n;
    }

    /**
     * Finds all nodes of the given type that meet the required bandwidth and radius constraints.
     * If you specify min and max radius, only nodes in this area are considered.
     *
     * @param t The type of node.
     * @param bw_up The upstream bandwidth. Set it to 0 to ignore it.
     * @param bw_down The downstream bandwidth. Set it to 0 to ignore it.
     * @param min_radius The minimum radius from which to search for matching nodes. Set to 0 to search from here.
     * @param max_radius The upper radius limit.
     * @param loc The location around which the radius must match.
     * @return A list of node infos that match the parameters.
     */
    public List<NodeInfo> getNodesMatchingBandwidthAndRadius(NodeType t, float bw_up, float bw_down, float min_radius,
                                                   float max_radius, VLatLng loc) {
        List<NodeInfo> found = new ArrayList<>();

        //Add all nodes immediately if bandwidth parameters are both 0.
        if (bw_up == 0 && bw_down == 0) 
        {
            found.addAll(mNodes.get(t).values());
        } 
        else
        //Up is zero, so only match by downstream bandwidth
        if (bw_up == 0) 
        {
            for (NodeInfo n : mNodes.get(t).values()) 
            {
                if (n.getBandwidthDown() >= bw_down) 
                {
                    found.add(n);
                }
            }
        } 
        else
        //Down is zero, so only match by upstream bandwidth
        if (bw_down == 0) 
        {
            for (NodeInfo n : mNodes.get(t).values()) 
            {
                if (n.getBandwidthUp() >= bw_up) 
                {
                    found.add(n);
                }
            }
        } 
        else
        //Both are not zero, so match both
        {
            for (NodeInfo n : mNodes.get(t).values()) 
            {
                if (n.getBandwidthDown() >= bw_down
                        && n.getBandwidthUp() >= bw_up) 
                {
                    found.add(n);
                }
            }
        }

        //Filter all by radius if necessary
        if (min_radius >= 0 && max_radius > 0 && max_radius > min_radius && loc != null) 
        {
            List<NodeInfo> filteredSet = new ArrayList<>();
            //Calculate distance for each node
            for(NodeInfo n : found) {
                if(n.getGeographicDistanceTo(loc) >= min_radius && n.getGeographicDistanceTo(loc) <= max_radius)
                {
                    filteredSet.add(n);
                }
            }
            return filteredSet;
        }
        return found;
    }

    /**
     * Finds all nodes of the given type that meet the required bandwidth constraint.
     * If you specify min and max radius, only nodes in this area are considered.
     *
     * @param t The type of node.
     * @param bw_up The upstream bandwidth. Set it to 0 to ignore it.
     * @param bw_down The downstream bandwidth. Set it to 0 to ignore it.
     * @param min_radius The minimum radius from which to search for matching nodes. Set to 0 to search from here.
     * @param max_radius The upper radius limit.
     * @param loc The location around which the radius must match.
     * @return A node info that matches the parameters.
     */
    public NodeInfo getRandomNodeOfTypeMatchingBandwidth(NodeType t, float bw_up, float bw_down, float min_radius,
                                                         float max_radius, VLatLng loc) {
        List<NodeInfo> nodes = getNodesMatchingBandwidthAndRadius(t, bw_up, bw_down, min_radius, max_radius, loc);
        if(nodes.size() > 0) 
        {
            return getRandomNodeFromList(nodes);
        } 
        else 
        {
            return null;
        }
    }

    /**
     * Finds all nodes that meet the required bandwidth constraint.
     * If you specify radius, max and loc, the radius will be increased continually by 500 meters
     * until a node has been found or max is reached.
     *
     * @param bw_up The upstream bandwidth. Set it to 0 to ignore it.
     * @param bw_down The downstream bandwidth. Set it to 0 to ignore it.
     * @param min_radius The minimum radius from which to search for matching nodes. Set to 0 to search from here.
     * @param max_radius The upper radius limit.
     * @param loc The location around which the radius must match.
     * @return A node info that matches the parameters.
     */
    public NodeInfo getRandomNodeMatchingBandwidthAndRadius(float bw_up, float bw_down,
                                                            float min_radius, float max_radius, VLatLng loc) {
        List<NodeInfo> results = new ArrayList<>();
        for(NodeType t : NodeType.values()) 
        {
            results.addAll(getNodesMatchingBandwidthAndRadius(t, bw_up, bw_down, min_radius, max_radius, loc));
        }
        return getRandomNodeFromList(results);
    }

    /**
     * Returns a node within the given radius of the given type (if available).
     * 
     * @param types The node types to apply the search to.
     * @param loc The location to check against
     * @param radius The radius in meters.
     * @param max If set to a higher value than 0, the radius will be increased to deliver a match if
     *            the initial radius was to small. Will only be increased until max is reached.
     * @param multiplier The multiplier for radius (for exponential increase strategy of the radius).
     *                   Is used for increasing until max or until something is found.
     * @param fallback If no node has been found after reaching max, a random node of the specified
     *                 type will be selected from the list regardless of distance.
     * @return NodeInfo if a node is found, or null.
     */
    public NodeInfo getRandomNodeOfTypeWithinRadius(NodeType[] types, VLatLng loc, float radius, float max,
                                                    float multiplier, NodeType fallback) {
        List<NodeInfo> nodes = new ArrayList<>();
        for (NodeType t : types) 
        {
            nodes.addAll(mNodes.get(t).values());
        }
        if(nodes.size() > 0) 
        {
            return getRandomNodeFromListWithinRadius(nodes, loc, radius, max, multiplier, fallback);
        }
        return null;
    }

    /**
     * Returns a random node from the list within the given radius.
     *
     * @param nodes The list of storage nodes from which to select a node.
     * @param loc The location to check against
     * @param radius The radius in meters.
     * @param max If set to a higher value than 0, the radius will be increased to deliver a match if
     *            the initial radius was to small. Will only be increased until max is reached.
     * @param multiplier The multiplier for radius (for exponential increase strategy of the radius).
     *                   Is used for increasing until max or until something is found.
     * @param fallback If no node has been found after reaching max, a random node of the specified
     *                 type will be selected from the list regardless of distance.
     * @return NodeInfo if a node is found, or null.
     */
    public NodeInfo getRandomNodeFromListWithinRadius(List<NodeInfo> nodes, VLatLng loc, float radius,
                                                      float max, float multiplier, NodeType fallback) {
        if(nodes.size() == 0) 
        {
            return null;
        }
        //Increase radius by 100 meters each step
        List<NodeInfo> results = new ArrayList<>();
        while (results.size() == 0) 
        {
            //Add nodes matching the radius
            //Starting at radius
            for (NodeInfo n : nodes) 
            {
                if (n.getGeographicDistanceTo(loc) <= radius)
                {
                    results.add(n);
                }
            }
            //Increasing by step if nothing found and max not reached yet.
            if(results.size() == 0 && radius < max && max > 0) 
            {
                radius *= multiplier;
            } 
            else 
            {
                break;
            }
        }
        if(results.size() > 0) 
        {
            //Return a random one
            return results.get((new Random()).nextInt(results.size()));
        }
        if(fallback != null) 
        {
            return getRandomNodeOfTypeFromList(results, fallback);
        }
        return null;
    }

    /**
     * Finds all nodes of the given type that match the required upload
     * time for the given file size.
     * 
     * @param t The Node type
     * @param filesize The file size in bytes that needs to be uploaded
     * @param seconds The maximum upload duration
     * @return A list containing all nodes of the type that match the constraints.
     */
    public List<NodeInfo> getNodesByUploadTime(NodeType t, long filesize, int seconds) {
        Collection<NodeInfo> nodes = mNodes.get(t).values();
        for(NodeInfo n : nodes) 
        {
            //Filesize in MByte divided by bandwidth in MByte/s
            float duration = filesize / (1024.0f*1024.0f) / (n.getBandwidthUp() / 8.0f);
            if(Math.ceil(duration) > seconds) 
            {
                nodes.remove(n);
            }
        }
        return new ArrayList<>(nodes);
    }

    /**
     * Finds all nodes that match the required upload time for the given file size.
     *
     * @param filesize The file size in bytes that needs to be uploaded
     * @param seconds The maximum upload duration
     * @return A list containing all nodes that match the constraints.
     */
    public List<NodeInfo> getAllNodesByUploadTime(long filesize, int seconds) {
        List<NodeInfo> results = new ArrayList<>();
        for(NodeType t : NodeType.values()) 
        {
            results.addAll(getNodesByUploadTime(t, filesize, seconds));
        }
        return results;
    }

    /**
     * Gets a random node from the list that is of the given type.
     *
     * @param nodes The list of nodes (can contain any type, only the relevant type will be used)
     * @param t The type
     * @return The node info.
     */
    public NodeInfo getRandomNodeOfTypeFromList(List<NodeInfo> nodes, NodeType t) {
        List<NodeInfo> nodesOfType = new ArrayList<>();
        //Add only the nodes of the type
        for(NodeInfo n : nodes) 
        {
            if(n.getNodeType().equals(t)) 
            {
                nodesOfType.add(n);
            }
        }
        //Get a random one
        return getRandomNodeFromList(nodesOfType);
    }

    public int getNodeCount() {
        return mNodeCount;
    }

}
