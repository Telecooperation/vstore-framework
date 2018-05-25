package vstoreframework.matching;

import java.util.HashMap;

/**
 * This class is used for retrieving a node identifier based on
 * the id of a given file that has been stored before.
 * 
 * Mappings are stored in a hashmap which is read from a database 
 * when the mapper is instantiated. 
 */
public class FileNodeMapper {
	
	private static FileNodeMapper mInstance;
	
	private static HashMap<String, String> mMappings;
	
	private FileNodeMapper() {
		//TODO Read hash-map from database
		mMappings = new HashMap<>();
	}
	
	/**
	 * @return The instance of the file<->node mapper class.
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
	 * @param node_id The unique identifier of the node where the file is stored.
	 * @return True if the entry was added successfully.
	 */
	public boolean storeNewMapping(String file_id, String node_id) {
		if( (file_id == null) || (node_id == null) 
				|| (file_id != null && file_id.trim().equals(""))
				|| (node_id != null && node_id.trim().equals("")) ) 
		{
			return false;
		}
		
		mMappings.put(file_id, node_id);
		return true;
	}
	
	/**
	 * Reads the corresponding node identifier (of the node where the file is stored)
	 * for the given file identifier.
	 * 
	 * @param file_id The identifier of the file you want to retrieve the storage node for.
	 * @return The identifier of the storage node where the file is stored.
	 *         An empty string in other cases.
	 */
	public String getNodeId(String file_id) {
		if( (file_id == null) || (file_id != null && file_id.trim().equals(""))
				|| !mMappings.containsKey(file_id))
		{
			return "";
		}
		
		return mMappings.get(file_id);
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
	 * Clears the file<->node mapping.
	 */
	public void clear() {
		mMappings.clear();
		apply();
	}
	
	
	
}
