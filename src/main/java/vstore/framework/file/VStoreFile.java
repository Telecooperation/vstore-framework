package vstore.framework.file;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.util.Date;

import vstore.framework.context.ContextDescription;
import vstore.framework.error.ErrorMessages;
import vstore.framework.exceptions.VStoreException;

/**
 * This class provides a wrapper for files stored and retrieved by the Virtual Storage framework.
 */
public class VStoreFile implements Serializable {
	private static final long serialVersionUID = 1L;

	private String mUUID;

    private MetaData mMeta;

    private String mPath;
    private ContextDescription mContext;

    private boolean mIsUploadPending;
    private boolean mIsUploadFailed;
    
    private boolean mIsDeletePending;

    private String mMD5Hash;

    /**
     * Stores the UUID of the storage node this file is saved on.
     */
    private String mNodeUUID;

    /**
     * Same as the other constructors, but will use the given filename if it is not null.
     * @param uuid UUID of the file
     * @param f The file object
     * @param descriptiveName The name of the file you want to use.
     * @param fileType The MIME type (should be from VFileType class)
     * @param isUploadPending Set it to true if an upload is currently pending for this file.
     * @throws FileNotFoundException Will be thrown if file is not found.
     */
    public VStoreFile(String uuid, File f, String descriptiveName, String fileType,
                        boolean isUploadPending, boolean isPrivate) throws FileNotFoundException {
            this(uuid,
                    (descriptiveName != null) ? descriptiveName : f.getName(),
                    f.getParent(),
                    fileType,
                    f.getName().substring(f.getName().lastIndexOf('.') + 1),
                    new Date(System.currentTimeMillis()),
                    isUploadPending,
                    isPrivate);
    }

    /**
     * A simpler constructor for a vStoreFile.
     *
     * @param uuid UUID of the file
     * @param f The file object
     * @param fileType The MIME type (should be from VFileType class)
     * @param isUploadPending Set this to true if an upload is currently pending for this file.
     * @param isPrivate Set this to true if the file is private.
     * @throws FileNotFoundException Will be thrown if file is not found.
     */
    public VStoreFile(String uuid, File f, String fileType,
                        boolean isUploadPending, boolean isPrivate)
            throws FileNotFoundException 
    {
        this(uuid,
                f.getName(),
                f.getParent(),
                fileType,
                f.getName().substring(f.getName().lastIndexOf('.') + 1),
                new Date(System.currentTimeMillis()),
                isUploadPending,
                isPrivate);
    }
    
    public VStoreFile(String uuid, File f, MetaData meta) 
    		throws VStoreException 
    {
    	if(uuid == null || uuid.equals("") || f == null || meta == null)
    	{
    		throw new VStoreException(ErrorMessages.PARAMETERS_MUST_NOT_BE_NULL);
    	}
    	mUUID = uuid;
    	mMeta = meta;
    	mPath = f.getAbsolutePath();
    	mIsUploadPending = false;
    	mIsUploadFailed = false;
    	mIsDeletePending = false;
    }

    /**
     * Most flexible constructor for a vStoreFile.
     * @param uuid The UUID of the file
     * @param descriptiveName "Display name" for the file
     * @param path Full Absolute Path to the file (without the file name itself and without trailing /)
     * @param fileType The MIME type (should be from VFileType class)
     * @param fileExtension Extension of the file (e.g. mp4, jpeg, ...)
     * @param creationDate Creation Date of the file. If null, now() will be used.
     * @param isUploadPending Set it to true if an upload is currently pending for this file.
     * @throws FileNotFoundException If the file is not found.
     */
    public VStoreFile(String uuid, String descriptiveName, String path, 
    		String fileType, String fileExtension, Date creationDate, 
    		boolean isUploadPending, boolean isPrivate)
            throws FileNotFoundException 
    {
        
    	File f = new File(path + "/" + uuid + "." + fileExtension);
        if(!f.exists()) throw new FileNotFoundException();
        
        mUUID = uuid;
        mPath = path;
        mIsUploadPending = isUploadPending;
        mIsUploadFailed = false;
        
        mIsDeletePending = false;

        MetaData meta = new MetaData(descriptiveName, f.length(), fileType);
        meta.setFileExtension(fileExtension);
        meta.setIsPrivate(isPrivate);
        
        if (creationDate == null) 
        {
            creationDate = new Date(System.currentTimeMillis());
        }
        meta.setCreationDate(creationDate);

        setMetaData(meta);
    }

    /**
     * @return UUID of this file
     */
    public String getUUID() { return mUUID; }

    /**
     * @return The descriptive filename of this file.
     */
    public String getDescriptiveName() {
        return mMeta.getFilename();
    }

    /**
     * @return The path to this file without the filename. Without trailing '/'.
     */
    public String getPath() {
        return mPath;
    }

    /**
     * @return The mime type of this file (see {@link VFileType}).
     */
    public String getFileType() {
        return mMeta.getMimeType();
    }

    /**
     * @return The file extension (e.g. mp4, jpeg, ...)
     */
    public String getFileExtension() {
        return mMeta.getFileExtension();
    }

    /**
     * @return The context in which this file was created (see {@link ContextDescription}).
     */
    public ContextDescription getContext() { return mContext; }

    /**
     * @return The unix timestamp this file was created at, in milliseconds.
     */
    public long getCreationDateUnix() { return mMeta.getCreationDate().getTime(); }

    /**
     * @return The Date this file was created at.
     */
    public Date getCreationDate() { return mMeta.getCreationDate(); }

    /**
     * @return The size of this file in bytes.
     */
    public long getFileSize() { return mMeta.getFilesize(); }

    /**
     * @return The full path to this file, including the filename itself.
     */
    public String getFullPath() { return mPath + "/" + mUUID + "." + mMeta.getFileExtension(); }

    /**
     * @return True, if an upload is pending for this file.
     */
    public boolean isUploadPending() { return mIsUploadPending; }

    /**
     * @return True, if an upload is pending for this file.
     */
    public boolean isUploadFailed() { return mIsUploadFailed; }

    /**
     * @return True, if this file was created and flagged as private.
     */
    public boolean isPrivate() { 
    	if(mMeta != null) return mMeta.isPrivate();
    	else return false;
	}

    /**
     * @return True, if this file was flagged for deletion.
     */
    public boolean isDeletePending() { return mIsDeletePending; }
    
    /**
     * @return The ID of the node this file was saved on.
     */
    public String getNodeID() { return mNodeUUID; }

    /**
     * @return The MD5 hash of this file.
     */
    public String getMD5Hash() { return mMD5Hash; }

    public MetaData getMetaData() { return mMeta; }

    /**
     * Sets the UUID of the node this file was saved on.
     * @param uuid The uuid of the node this file was saved on.
     */
    public void setNodeUUID(String uuid) { mNodeUUID = uuid; }

    /**
     * Sets the upload pending flag for this file.
     * @param isPending Set this to true, if an upload is pending for this file.
     */
    public void setUploadPending(boolean isPending) { mIsUploadPending = isPending; }

    /**
     * Sets the upload pending flag for this file.
     * @param hasFailed Set this to true, if an upload is pending for this file.
     */
    public void setUploadFailed(boolean hasFailed) { mIsUploadFailed = hasFailed; }
    
    /**
     * Sets the delete pending flag for this file.
     * @param deletePending Set this to true, if the file should be marked for deletion.
     */
	public void setDeletePending(boolean deletePending) { mIsDeletePending = deletePending; }

    /**
     * Sets the private flag for this file.
     * @param isPrivate Set this to true, if the file is private.
     */
    public void setPrivate(boolean isPrivate) { 
    	if(mMeta != null) { mMeta.setIsPrivate(isPrivate); }
	}

    /**
     * Sets the context description for this file
     * @param contextDescription The context description object (see {@link ContextDescription}).
     */
    public void setContext(ContextDescription contextDescription) {
        mContext = contextDescription;
    }

    /**
     * This method sets the context for this vStore file from the given json string.
     * @param json The context as a json string.
     */
    public void setContextFromJson(String json) {
        mContext = new ContextDescription(json);
    }

    /**
     * Sets the md5 hash for this file.
     * @param md5 The md5 hash.
     */
    public void setMD5Hash(String md5) { mMD5Hash = md5; }

    /**
     * Sets the metadata for this file.
     * @param meta The MetaData object
     */
    public void setMetaData(MetaData meta) { mMeta = meta; }
}
