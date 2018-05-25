package vstoreframework.node;

import static vstoreframework.communication.ApiConstants.ROUTE_FILE;
import static vstoreframework.communication.ApiConstants.ROUTE_FILE_DELETE;
import static vstoreframework.communication.ApiConstants.ROUTE_FILE_METADATA_FULL;
import static vstoreframework.communication.ApiConstants.ROUTE_FILE_METADATA_LIGHT;
import static vstoreframework.communication.ApiConstants.ROUTE_FILE_MIMETYPE;
import static vstoreframework.communication.ApiConstants.ROUTE_NODE_UUID;
import static vstoreframework.communication.ApiConstants.ROUTE_THUMBNAIL;

import java.io.Serializable;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import vstoreframework.context.types.location.VLatLng;
import vstoreframework.utils.ContextUtils;

/**
 * This class holds information about one single storage node.
 * It contains the address, the port, the latitude and longitude of the storage node
 * as well as the login credentials.
 */
@SuppressWarnings("serial")
public class NodeInfo implements Serializable {
    private String mUUID;
    private String mAddress;
    private int mPort;

    private NodeType mNodeType;
    private VLatLng mLatLng;
    private int mBandwidthUp;
    private int mBandwidthDown;

    /**
     * Constructs a new NodeInfo object.
     * @param uuid The UUID of the storage node
     * @param address The ip address or domain name of the storage node.
     * @param port The port number the storage node server runs on
     * @param type The type of the node, for available types see {@link NodeType}
     * @param latlng The geographical location (latitude and longitude) of the node.
     *               See {@link VLatLng}
     */
    public NodeInfo(String uuid, String address, int port, NodeType type, VLatLng latlng) {
        mUUID = uuid;
        mAddress = address;
        mPort = port;
        mNodeType = type;
        mLatLng = latlng;
        if(!mAddress.contains("http://")) {
            mAddress = "http://" + mAddress;
        }
    }

    /**
     * Tries to construct a new NodeInfo object from the given JSON object
     * 
     * @param node
     */
    public NodeInfo(JSONObject node) {
        if(node != null) 
        {
            if(node.containsKey("url") && node.containsKey("port") 
            		&& node.containsKey("type") && node.containsKey("location")) 
            {
                mAddress = (String)node.get("url");
                mPort = (int)node.get("port");
                try 
                {
                    mNodeType = NodeType.valueOf((String)node.get("type"));
                } 
                catch(IllegalArgumentException e) 
                {
                    mNodeType = NodeType.UNKNOWN;
                }
                JSONArray location = (JSONArray)node.get("location");
                if(location.size() == 2) 
                {
                    mLatLng = new VLatLng((double)location.get(0), (double)location.get(1));
                }
                try 
                {
                    setBandwidthDown((int)node.get("bandwidthDown"));
                    setBandwidthUp((int)node.get("bandwidthUp"));
                } 
                catch(NullPointerException e) 
                {
                    setBandwidthDown(0);
                    setBandwidthUp(0);
                }
                return;
            }
        }
        throw new RuntimeException("Given json must not be null and must contain url, port, type and location!");
    }

    /**
     * @return The id of the storage node.
     */
    public String getUUID() { return mUUID; }

    /**
     * @return The address of the storage node
     */
    public String getAddress() { return mAddress; }

    /**
     * @return The port of the storage node
     */
    public int getPort() { return mPort; }

    /**
     * @return The location of the storage node (see {@link VLatLng})
     */
    public VLatLng getLatLng() { return mLatLng; }

    /**
     * @return The type of the node (see {@link NodeType})
     */
    public NodeType getNodeType() { return mNodeType; }

    /**
     * @return The upstream bandwidth of the node in MBit/s.
     */
    public int getBandwidthUp() { return mBandwidthUp; }

    /**
     * @return The downstream bandwidth of the node in MBit/s.
     */
    public int getBandwidthDown() { return mBandwidthDown; }

    /**
     * @return The base uri of this node, consisting of address and port.
     */
    public String getBaseUri() { return getAddress() + ":" + getPort(); }

    /**
     * @return The uri that has to be used to get the UUID of this node.
     * The node will reply with its meta information if you send an HTTP GET request to this uri.
     */
    public String getUUIDUri() { return getBaseUri() + ROUTE_NODE_UUID; }

    /**
     * @return The uri that has to be used to upload a file to the node.
     * Upload a file by sending a multipart POST request to this address consisting
     * of the following fields:
     * fileinfo
     * originalname
     * context
     * mimetype
     * extension
     * isPrivate
     * phoneID
     */
    public String getUploadUri() {
        return getBaseUri() + ROUTE_FILE;
    }

    /**
     * @param uuid The UUID of the file
     * @param phoneID The phone ID of the phone (see {@link AppUtils#getPhoneID(Context)}).
     * @return The uri that has to be used to download a file from the node.
     * Download a file by sending an HTTP GET request to this uri.
     * If the file is private, the node will only reply with the file if you are the owner.
     */
    public String getDownloadUri(String uuid, String phoneID) {
        return getBaseUri() + ROUTE_FILE + "/" + uuid + "/" + phoneID;
    }

    /**
     * @param uuid The UUID of the file
     * @param phoneID The phone ID of the phone (see {@link AppUtils#getPhoneID(Context)}).
     * @return The uri that has to be used to delete a file from the node. You can only delete
     * files that you are the owner of (e.g. that matches your phone ID).
     * To delete a file, send an HTTP DELETE request to this uri.
     */
    public String getDeleteUri(String uuid, String phoneID) {
        return getBaseUri() + ROUTE_FILE_DELETE + "/" + uuid + "/" + phoneID;
    }

    /**
     * @param uuid The UUID of the file
     * @param phoneID The phone ID of the phone (see {@link AppUtils#getPhoneID(Context)}).
     * @return The uri that has to be used to download a thumbnail for a file from the node.
     * Download a thumbnail by sending an HTTP GET request to this uri.
     * If the file is private, the node will only reply with a thumbnail if you are the owner.
     */
    public String getThumbnailUri(String uuid, String phoneID) {
        return getBaseUri() + ROUTE_THUMBNAIL + "/" + uuid + "/" + phoneID;
    }

    /**
     * @param uuid The UUID of the file
     * @param phoneID The phone ID of the phone (see {@link AppUtils#getPhoneID(Context)}).
     * @return The uri that has to be used to fetch the mimetype for a file from this node.
     * To get the mimetype information, send an HTTP GET request to this uri.
     * If the file is private, the node will only reply with the mimetype if you are the owner.
     */
    public String getMimeTypeUri(String uuid, String phoneID) {
        return getBaseUri() + ROUTE_FILE_MIMETYPE + "/" + uuid + "/" + phoneID;
    }

    /**
     * @param uuid The UUID of the file
     * @param phoneID The phone ID of the phone (see {@link AppUtils#getPhoneID(Context)}).
     * @param fullMetadata If set to true, the uri for requesting full metadata is returned (e.g.
     *                     with context information). If set to false, the uri for requesting
     *                     lightweight metadata is returned.
     * @return The uri that has to be used to fetch metadata for a file from this node.
     * To get the metadata, send an HTTP GET request to this uri.
     * If the file is private, the node will only reply with the metadata if you are the owner.
     */
    public String getMetadataUri(String uuid, String phoneID, boolean fullMetadata) {
        if(fullMetadata) {
            return getBaseUri() + ROUTE_FILE_METADATA_FULL + "/" + uuid + "/" + phoneID;
        } else {
            return getBaseUri() + ROUTE_FILE_METADATA_LIGHT + "/" + uuid + "/" + phoneID;
        }
    }

    /**
     * Sets the id for this node.
     * @param uuid The id to set for this node.
     */
    public void setUUID(String uuid) {
        mUUID = uuid;
    }
    /**
     * Sets the node type for this node.
     * @param type The id to set for this node.
     */
    public void setNodeType(NodeType type) {
        mNodeType = type;
    }
    /**
     * Sets the location (latitude, longitude) for this node.
     * @param latlng The VLatLng object containing the location information.
     */
    public void setLatLng(VLatLng latlng) {
        mLatLng = latlng;
    }
    /**
     * Sets the upstream bandwidth value for this node
     * @param bandwidth The upstream bandwidth in MBit/s
     */
    public void setBandwidthUp(int bandwidth) { mBandwidthUp = bandwidth; }
    /**
     * Sets the downstream bandwidth value for this node
     * @param bandwidth The downstream bandwidth in MBit/s
     */
    public void setBandwidthDown(int bandwidth) { mBandwidthDown = bandwidth; }

    /**
     * @return A string array containing the supported node type strings
     */
    public static String[] getSupportedNodeTypes() {
        String[] types = new String[NodeType.values().length];
        int i = 0;
        for(NodeType s : NodeType.values()) {
            types[i] = s.name();
            i++;
        }
        return types;
    }

    /**
     * Returns the distance of the given location to the node in kilometers.
     * @param latlng The location
     * @return The distance. Float.MAX_VALUE if node has no location.
     */
    public float getDistanceTo(VLatLng latlng) {
        if(this.getLatLng() == null || latlng == null) {
            return Float.MAX_VALUE;
        }
        return ContextUtils.distanceBetween(latlng, this.getLatLng()) / 1000.0f;
    }
}
