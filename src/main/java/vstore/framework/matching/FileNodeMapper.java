package vstore.framework.matching;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This class is used for retrieving a node identifier based on
 * the id of a given file that has been stored before.
 * 
 * Mappings are stored in a hashmap which is read from a database 
 * when the mapper is instantiated.
 *
 * TODO Make it more dynamic to keep a subset from master node file-node-mapping which
 * TODO is updated regularly
 */
public class FileNodeMapper {
	
	private static FileNodeMapper mInstance;
	
	private static HashMap<String, List<String>> mMappings;
	
	private FileNodeMapper() {
		//TODO Read hash-map from database
		mMappings = new HashMap<>();
	}
	
	/**
	 * @return Gets the instance of the file-node mapper class.
	 */
	public static FileNodeMapper getMapper() {
		if(mInstance == null) 
		{
			mInstance = new FileNodeMapper();
		}
		return mInstance;
	}
	
	/**
	 * Stores a new mapping in the hashmap. 
	 * If the changes should be written to disk afterwards, use
	 * the method {@link FileNodeMapper#apply()}.
	 * 
	 * @param file_id The unique identifier of the file.
	 * @param node_ids The list of unique identifiers of the nodes where the file is stored.
	 * @return True if the entry was added successfully.
	 */
	public boolean storeNewMapping(String file_id, List<String> node_ids) {
		if((file_id == null) || (node_ids == null)
                || file_id.trim().equals("") || node_ids.size() == 0)
		{
			return false;
		}
		
		mMappings.put(file_id, node_ids);
		return true;
	}

    /**
     * Adds a new mapping to the hashmap.
     * If the changes should be written to disk afterwards, use
     * the method {@link FileNodeMapper#apply()}.
     *
     * @param file_id The unique identifier of the file.
     * @param node_id The unique identifier of the node where the file is stored.
     * @return True if the entry was added successfully.
     */
	public boolean addNewValueToMapping(String file_id, String node_id) {
        if((file_id == null) || (node_id == null)
                || file_id.trim().equals("") || node_id.trim().equals(""))
        {
            return false;
        }

        //First, get current node ids for the file
        List<String> nodeIds = getNodeIds(file_id);
        if(nodeIds == null) {
            //Create new list if it was null
            nodeIds = new ArrayList<>();
        }
        //Then check if node id is already in it. If not, add it and store the whole thing.
        if(!nodeIds.contains(node_id)) {
            nodeIds.add(node_id);
            storeNewMapping(file_id, nodeIds);
        }
        return true;
    }
	
	/**
	 * Reads the corresponding node identifiers (of the nodes where the file was stored)
	 * for the given file identifier.
	 * 
	 * @param file_id The identifier of the file you want to retrieve the storage nodes for.
	 * @return The identifiers of the storage nodes where the file is stored.
	 *         An empty list in case an error occurred.
	 */
	public List<String> getNodeIds(String file_id) {
		if( (file_id == null) || (file_id != null && file_id.trim().equals(""))
				|| !mMappings.containsKey(file_id))
		{
			return new ArrayList<>();
		}
		
		return mMappings.get(file_id);
	}

    /**
     * Removes the mapping with the given file id from the mapper.
     * @param file_id The file id
     */
	public void removeMapping(String file_id) {
        if( (file_id == null) || (file_id != null && file_id.trim().equals(""))
                || !mMappings.containsKey(file_id))
        {
            return;
        }
        mMappings.remove(file_id);
    }
	
	/**
	 * Writes the contained hashmap to disk.
	 * 
	 * @return True in case of success.
	 */
	public boolean apply() {
		//TODO
		return true;
	}
	
	/**
	 * Clears the file-node mapping.
	 */
	public void clear() {
		mMappings.clear();
		apply();
	}
	
	
	
}
