package vstoreframework.file;


import java.io.Serializable;
import java.util.Date;

import org.json.simple.JSONObject;

import vstoreframework.node.NodeType;

/**
 * This class contains meta information about a file.
 */
public class MetaData implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private String mUUID;
    private long mFilesize;
    private String mFilename;
    private String mMimeType;
    private String mFileExtension;
    private Date mDateCreation;
    private NodeType mNodeType;

    /**
     * Constructs a new metadata object.
     * @param filename The file name
     * @param mimetype The file mimetype
     */
    public MetaData(String filename, long filesize, String mimetype) {
        mFilename = filename;
        mFilesize = filesize;
        mMimeType = mimetype;
    }
    
    /**
     * Constructs a new metadata object from the given JSON object.
     * @param json
     */
    public MetaData(JSONObject jMetaData) {
    	String uuid = (String)jMetaData.get("uuid");
        String extension = (String)jMetaData.get("extension");
        String filename;
        if(jMetaData.containsKey("descriptiveName")) {
            filename = (String)jMetaData.get("descriptiveName");
        } else {
            filename = uuid + "." + extension;
        }
        long filesize = (long)jMetaData.get("filesize");
        String mimetype = (String)jMetaData.get("mimetype");
        long creationTimestamp = (long)jMetaData.get("creationTimestamp");
        
        //Return MetaData object
        MetaData meta = new MetaData(
                filename,
                filesize,
                mimetype);
        meta.setCreationDate(creationTimestamp);
        meta.setFileExtension(extension);
    }

    /**
     * Sets the file uuid for this metadata object.
     * @param uuid The id of the file.
     */
    public void setUUID(String uuid) {
        mUUID = uuid;
    }

    /**
     * Sets the file extension for this metadata
     * @param ext The extension (e.g. "jpeg")
     */
    public void setFileExtension(String ext) { mFileExtension = ext; }

    /**
     * @return The id of the file
     */
    public String getUUID() {
        return mUUID;
    }

    /**
     * @return The name of the file that this metadata describes.
     */
    public String getFilename() {
        return mFilename;
    }

    /**
     * @return The size of the file in bytes.
     */
    public long getFilesize() { return mFilesize; }

    /**
     * @return The mimetype of the file that this metadata describes.
     */
    public String getMimeType() {
        return mMimeType;
    }

    /**
     * @return The creation date of the file that this metadata describes.
     */
    public Date getCreationDate() {
        return mDateCreation;
    }

    /**
     * Sets the creation date for this file metadata object.
     * @param timestampInMs The unix timestamp in milliseconds
     */
    public void setCreationDate(long timestampInMs) {
        mDateCreation = new Date(timestampInMs);
    }
    /**
     * Sets the creation date for this file metadata object.
     * @param date The creation date object
     */
    public void setCreationDate(Date date) {
        mDateCreation = date;
    }

    /**
     * @return The unix timestamp in milliseconds of the creation date.
     */
    public long getTimestamp() {
        if(mDateCreation == null) {
            return 0;
        }
        return mDateCreation.getTime();
    }

    /**
     * @return The file extension (e.g. jpeg)
     */
    public String getFileExtension() {
        return mFileExtension;
    }

    /**
     * @return The type of node.
     */
    public NodeType getNodeType() {
        return mNodeType;
    }

    /**
     * Sets the type of node for this file metadata.
     * @param type The type.
     */
    public void setNodeType(NodeType type) {
        mNodeType = type;
    }
    
    public void setFileSize(long size) {
    	mFilesize = size;
    }
    
    /**
     * Returns this metadata object in the JSON format.
     * 
     * @return This object in JSON representation.
     */
    @SuppressWarnings("unchecked")
	public JSONObject toJSON() {
    	JSONObject j = new JSONObject();
    	j.put("uuid", getUUID());
    	j.put("extension", getFileExtension());
    	j.put("filename", getFilename());
    	j.put("descriptiveName", getFilename());
    	j.put("filesize", getFilesize());
    	j.put("mimetype", getMimeType());
    	j.put("creationTimestamp", getCreationDate().getTime());
        return j;
    }
}
