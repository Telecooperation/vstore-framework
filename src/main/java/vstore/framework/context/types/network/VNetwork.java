package vstore.framework.context.types.network;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import vstore.framework.context.types.VContextType;
import vstore.framework.context.types.network.cellular.CellularNetwork;
import vstore.framework.context.types.network.wifi.WiFi;

/**
 * This class represents an instance of the Network context data.
 * Contains information if WiFi is connected in this context (and what the SSID is),
 * if mobile data is connected and if the type is 2G, 3G, 3.5G or 4G.
 */
@SuppressWarnings({ "serial", "unchecked" })
public class VNetwork extends VContextType<VNetwork> {

    private WiFi wifiContext;
    private CellularNetwork mobileContext;

    /**
     * Creates a new VNetwork object from the json string.
     * @param json The json string.
     * @throws ParseException in case the given JSON is malformed
     */
	public VNetwork(String json) throws ParseException {
	    this((JSONObject)new JSONParser().parse(json));
    }
    public VNetwork(JSONObject json) throws ParseException {
        Object wifiCtx = json.get("wifi");
        if(wifiCtx != null) {
            wifiContext = new WiFi((String)((JSONObject)wifiCtx).toJSONString());
        }

        Object mobileCtx = json.get("mobile");
        if(mobileCtx != null) {
            mobileContext = new CellularNetwork((String)((JSONObject)mobileCtx).toJSONString());
        }

        Object timestamp = json.get("timestamp");
        if(timestamp == null) { timestamp = System.currentTimeMillis(); }
        setTimestamp((long)timestamp);

    }

    /**
     * Constructs a new network context with wifi and/or mobile context.
     * @param wifiCtx The wifi context
     * @param mobileCtx The mobile context
     */
    public VNetwork(WiFi wifiCtx, CellularNetwork mobileCtx) {
        setWiFiContext(wifiCtx);
        setMobileContext(mobileCtx);
        setTimestamp(System.currentTimeMillis());
    }

    public VNetwork() {
        setWiFiContext(new WiFi(false, ""));
        setMobileContext(new CellularNetwork(false, false, CellularNetwork.MobileType.UNKNOWN));
        setTimestamp(System.currentTimeMillis());
    }

    /**
     * Sets the Wifi context for this network context.
     *
     * @param wifiCtx The wifi context object.
     */
    public void setWiFiContext(WiFi wifiCtx) {
	    this.wifiContext = wifiCtx;
    }

    /**
     * Sets the mobile network context for this network context.
     * @param mobileCtx The mobile network context.
     */
    public void setMobileContext(CellularNetwork mobileCtx) {
        this.mobileContext = mobileCtx;
    }

    /**
     * @return The WiFi context of this network context.
     */
    public WiFi getWiFiContext() {
        return wifiContext;
    }

    /**
     * @return The mobile network context of this network context.
     */
    public CellularNetwork getMobileContext() {
        return mobileContext;
    }

    /**
     * Removes the wifi context from this network context.
     */
    public void removeWifiContext() {
        setWiFiContext(null);
    }

    /**
     * Removes the mobile context from this network context.
     */
    public void removeMobileContext() {
        setMobileContext(null);
    }

    /**
     * Turns this Network context object into a JSON representation.
     * @return Json String
     */
    public JSONObject getJson() {
        JSONObject json = new JSONObject();
        if(wifiContext != null)
            json.put("wifi", wifiContext.getJson());
        if(mobileContext != null)
            json.put("mobile", mobileContext.getJson());
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
        if(other == null) { return false; }
        if(wifiContext != null && other.wifiContext != null)
        {
            if( (wifiContext.isWifiConnected() && other.wifiContext.isWifiConnected())
                    && (wifiContext.getWifiSSID().equals(other.wifiContext.getWifiSSID())) )
            {
                return true;
            }

            if( (mobileContext.isMobileConnected() && other.mobileContext.isMobileConnected())
                    && (mobileContext.getMobileNetworkType().equals(other.mobileContext.getMobileNetworkType())))
            {
                return true;
            }
        }
        return false;
    }
}
