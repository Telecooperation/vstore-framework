package vstore.framework.context.types.network.wifi;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import vstore.framework.context.types.VContextType;

/**
 * Wifi context data
 */
public class WiFi extends VContextType<WiFi> {
    /**
     * Denotes, if WiFi is connected for this network context.
     */
    private boolean mIsWifiConnected;
    /**
     * The SSID of the WiFi of this network context.
     */
    private String mWifiSSID;

    /**
     * Creates a new Wifi context object from the json string.
     * @param json The json string.
     * @throws ParseException in case the given JSON is malformed
     */
    public WiFi(String json) throws ParseException {
        JSONObject j = (JSONObject)(new JSONParser().parse(json));

        Object wifiConnected = j.get("isWifiConnected");
        if(wifiConnected == null) { wifiConnected = false; }
        setWifiConnected((boolean)wifiConnected);

        Object wifissid = j.get("wifiSsid");
        if(wifissid == null) { wifissid = ""; }
        setWifiSSID((String)wifissid);

        Object timestamp = j.get("timestamp");
        if(timestamp == null) { timestamp = System.currentTimeMillis(); }
        setTimestamp((long)timestamp);
    }

    /**
     * Creates a new WiFi context object from the given parameters.
     * @param wifiConnected Set this to true if wifi is connected.
     * @param wifiSsid SSID of the WiFi network
     */
    public WiFi(boolean wifiConnected, String wifiSsid) {
        setWifiConnected(wifiConnected);
        setWifiSSID(wifiSsid);
        setTimestamp(System.currentTimeMillis());
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
     * Sets the Wifi SSID for this network context.
     * @param ssid The SSID of the WiFi.
     */
    public void setWifiSSID(String ssid) {
        mWifiSSID = ssid;
    }

    /**
     * @return True, if Wifi is connected for this network context.
     */
    public boolean isWifiConnected() {
        return mIsWifiConnected;
    }

    /**
     * @return The SSID of the WiFi of this network context.
     */
    public String getWifiSSID() {
        return mWifiSSID;
    }

    @Override
    public JSONObject getJson() {
        JSONObject json = new JSONObject();
        json.put("isWifiConnected", isWifiConnected());
        if(mWifiSSID != null)
            json.put("wifiSsid", getWifiSSID());
        json.put("timestamp", getTimestamp());
        return json;
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
    @Override
    public boolean matches(WiFi other) {
        if(other == null) { return false; }
        if (this.isWifiConnected() && other.isWifiConnected())
        {
            if (this.getWifiSSID() != null && other.getWifiSSID() != null
                    && (this.getWifiSSID().equals(other.getWifiSSID())
                    || this.getWifiSSID().equals("%")
                    || other.getWifiSSID().equals("%")))
            {
                return true;
            }
        }
        return false;
    }
}
