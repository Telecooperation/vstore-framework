package vstore.framework.context.types.network;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import vstore.framework.context.types.VContextType;

/**
 * This class represents an instance of the Network context data.
 * Contains information if WiFi is connected in this context (and what the SSID is),
 * if mobile data is connected and if the type is 2G, 3G, 3.5G or 4G.
 */
@SuppressWarnings({ "serial", "unchecked" })
public class VNetwork extends VContextType<VNetwork> {

    /**
     * Denotes, if WiFi is connected for this network context.
     */
    private boolean mIsWifiConnected;
    /**
     * Denotes, if mobile data is connected for this network context.
     */
    private boolean mIsMobileConnected;
    /**
     * Denotes, if the mobile network is of type 3G, 3.5G or 4G.
     */
    private boolean mIsMobileNetworkFast;
    /**
     * The SSID of the WiFi of this network context.
     */
    private String mWifiSSID;
    /**
     * The mobile network type of the network context (e.g. 2G, 3G, 3.5G, 4G).
     */
    private String mMobileNetworkType;

    /**
     * Creates a new VNetwork object from the json string.
     * @param json The json string.
     * @throws ParseException in case the given JSON is malformed
     */
	public VNetwork(String json) throws ParseException {
        JSONObject j = (JSONObject)(new JSONParser().parse(json));

        Object wifiConnected = j.get("isWifiConnected");
        if(wifiConnected == null) { wifiConnected = false; }
        setWifiConnected((boolean)wifiConnected);

        Object mobileConnected = j.get("isMobileConnected");
        if(mobileConnected == null) { mobileConnected = false; }
        setMobileConnected((boolean)mobileConnected);

        Object mobileNetFast = j.get("isMobileNetworkFast");
        if(mobileNetFast == null) { mobileNetFast = false; }
        setMobileNetworkFast((boolean)mobileNetFast);

        Object wifissid = j.get("wifiSsid");
        if(wifissid == null) { wifissid = ""; }
        setWifiSSID((String)wifissid);

        Object mobnettype = j.get("mobileNetworkType");
        if(mobnettype == null) { mobnettype = ""; }
        String mobileNet = (String)mobnettype;
        if(mobileNet.equals("")) { setMobileNetworkType(null); }
        else { setMobileNetworkType(mobileNet); }

        Object timestamp = j.get("timestamp");
        if(timestamp == null) { timestamp = System.currentTimeMillis(); }
        setTimestamp((long)timestamp);
    }

    /**
     * Creates a new VNetwork object from the given parameters.
     * @param wifiConnected Set this to true if wifi is connected.
     * @param mobileConnected Set this to true if mobile is connected.
     * @param mobileFast Set this to true if mobile is fast.
     * @param wifiSsid SSID of the WiFi network
     * @param mobileNetType Type of mobile network (2G, 3G, 4G, Unknown).
     */
    public VNetwork(boolean wifiConnected, boolean mobileConnected, boolean mobileFast,
                    String wifiSsid, String mobileNetType) {
        setWifiConnected(wifiConnected);
        setMobileConnected(mobileConnected);
        setMobileNetworkFast(mobileFast);
        setWifiSSID(wifiSsid);
        setMobileNetworkType(mobileNetType);
        setTimestamp(System.currentTimeMillis());
    }

    /**
     * @return True, if Wifi is connected for this network context.
     */
    public boolean isWifiConnected() {
        return mIsWifiConnected;
    }

    /**
     * @return True, if mobile data is connected for this network context.
     */
    public boolean isMobileConnected() {
        return mIsMobileConnected;
    }

    /**
     * @return True, if the mobile network is of type 3G, 3.5G or 4G.
     */
    public boolean isMobileNetworkFast() {
        return mIsMobileNetworkFast;
    }

    /**
     * @return The SSID of the WiFi of this network context.
     */
    public String getWifiSSID() {
        return mWifiSSID;
    }

    /**
     * @return The mobile network type of the network context (e.g. 2G, 3G, 3.5G, 4G).
     */
    public String getMobileNetworkType() {
        return mMobileNetworkType;
    }

    /**
     * Sets the flag if wifi is connected in this network context.
     * @param wifiConnected Set this to true, if WiFi should be treated as connected in
     *                      this network context.
     */
    public void setWifiConnected(boolean wifiConnected) {
        mIsWifiConnected = wifiConnected;
    }

    /**
     * Sets the flag if mobile data is connected in this network context.
     * @param mobileConnected Set this to true, if mobile data should be treated as
     *                        connected in this network context.
     */
    public void setMobileConnected(boolean mobileConnected) {
        mIsMobileConnected = mobileConnected;
    }

    /**
     * Sets the flag if mobile data is fast in this network context (e.g. 3G, 3.5G or 4G).
     * @param mobileFast Set this to true, if mobile data should be treated as connected
     *                   in this network context.
     */
    public void setMobileNetworkFast(boolean mobileFast) {
        mIsMobileNetworkFast = mobileFast;
    }

    /**
     * Sets the Wifi SSID for this network context.
     * @param ssid The SSID of the WiFi.
     */
    public void setWifiSSID(String ssid) {
        mWifiSSID = ssid;
    }

    /**
     * Sets the mobile network type for this network context.
     * Should be either "2G", "3G", "3.5G", or "4G".
     * @param mobileNetType The mobile network type as string.
     */
    public void setMobileNetworkType(String mobileNetType) { mMobileNetworkType = mobileNetType; }

    /**
     * Turns this Network context object into a JSON representation.
     * @return Json String
     */
    public JSONObject getJson() {
        JSONObject json = new JSONObject();
        json.put("isWifiConnected", isWifiConnected());
        json.put("isMobileConnected", isMobileConnected());
        json.put("isMobileNetworkFast", isMobileNetworkFast());
        json.put("wifiSsid", getWifiSSID());
        json.put("mobileNetworkType", getMobileNetworkType());
        json.put("timestamp", getTimestamp());
        return json;
    }

    /**
     * Matches the given network context against this network context.
     * Matching WiFi SSIDs are considered a match, and matching mobile connection types are considered
     * a match. But both at the same time is not checked. WiFi has higher priority than mobile.
     * That means, if WiFi is connected and the same SSID is given, mobile is ignored.
     *
     * @param other The context to match against.
     * @return True, if both contexts match. False, if not
     */
    public boolean matches(VNetwork other) {
        if(other != null) {
            if (wifiContextMatches(other)) {
                return true;
            } else if (this.isMobileConnected() && other.isMobileConnected()) {
                if(this.getMobileNetworkType() == null || other.getMobileNetworkType() == null) {
                    return true;
                }
                else if (this.getMobileNetworkType().equals(other.getMobileNetworkType())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Matches the wifi context of the given network against this network context.
     * It is a match if WiFi is connected and has the same SSID for both contexts.
     * It is also a match, if both contexts have connected WiFi and one (or both)
     * have "%" configured as their WiFi SSID.
     *
     * @param other The network context to match WiFi state against.
     * @return True, if both contexts match. False, if not
     */
    public boolean wifiContextMatches(VNetwork other) {
        if(other != null) {
            if (this.isWifiConnected() && other.isWifiConnected()) {
                if (this.getWifiSSID() != null && other.getWifiSSID() != null
                        && (this.getWifiSSID().equals(other.getWifiSSID())
                            || this.getWifiSSID().equals("%")
                            || other.getWifiSSID().equals("%"))) {
                    return true;
                }
            }
        }
        return false;
    }
}
