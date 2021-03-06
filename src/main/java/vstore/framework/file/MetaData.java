package vstore.framework.file;


import org.json.simple.JSONObject;

import java.io.Serializable;
import java.util.Date;

import vstore.framework.node.NodeType;

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
    
    private boolean mIsPrivate;

    /**
     * Constructs a new metadata object.
     * @param filename The file name
     * @param filesize The file size.
     * @param mimetype The file mimetype
     */
    public MetaData(String filename, long filesize, String mimetype) {
        mFilename = filename;
        mFilesize = filesize;
        mMimeType = mimetype;
    }
    
    /**
     * Constructs a new metadata object from the given JSON object.
     * @param jMetaData The metadata in a json object.
     */
    public MetaData(JSONObject jMetaData) {
    	mUUID = (String)jMetaData.get("uuid");
    	setFileExtension((String)jMetaData.get("extension"));
        if(jMetaData.containsKey("descriptiveName")) {
            mFilename = (String)jMetaData.get("descriptiveName");
        } else {
            mFilename = mUUID + "." + mFileExtension;
        }
        mFilesize = (long)jMetaData.get("filesize");
        mMimeType = (String)jMetaData.get("mimetype");
        setCreationDate((long)jMetaData.get("creationTimestamp"));
        mIsPrivate = (boolean)jMetaData.get("isPrivate");
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
    
    public boolean isPrivate() {
    	return mIsPrivate;
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
    
    public void setIsPrivate(boolean newValue) {
    	mIsPrivate = newValue;
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
    	j.put("isPrivate", isPrivate());
        return j;
    }
}
