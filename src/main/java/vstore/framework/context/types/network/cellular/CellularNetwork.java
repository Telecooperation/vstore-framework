package vstore.framework.context.types.network.cellular;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import vstore.framework.context.types.VContextType;

public class CellularNetwork extends VContextType<CellularNetwork> {

    public enum MobileType {
        NET_2G, NET_3G, NET_3_5_G, NET_4G, UNKNOWN
    }

    /**
     * Denotes, if mobile data is connected for this network context.
     */
    private boolean mIsMobileConnected;
    /**
     * Denotes, if the mobile network is of type 3G, 3.5G or 4G.
     */
    private boolean mIsMobileNetworkFast;
    /**
     * The mobile network type of the network context (e.g. 2G, 3G, 3.5G, 4G).
     */
    private MobileType mMobileNetworkType;

    /**
     * Creates a new CellularNetwork context object from the json string.
     * @param json The json string.
     * @throws ParseException in case the given JSON is malformed
     */
    public CellularNetwork(String json) throws ParseException {
        JSONObject j = (JSONObject)(new JSONParser().parse(json));

        Object mobileConnected = j.get("isMobileConnected");
        if(mobileConnected == null) { mobileConnected = false; }
        setMobileConnected((boolean)mobileConnected);

        Object mobileNetFast = j.get("isMobileNetworkFast");
        if(mobileNetFast == null) { mobileNetFast = false; }
        setMobileNetworkFast((boolean)mobileNetFast);

        Object mobnettype = j.get("mobileNetworkType");
        if(mobnettype == null) { mobnettype = ""; }
        String mobileNet = (String)mobnettype;
        if(mobileNet.equals("")) { setMobileNetworkType(null); }
        else { setMobileNetworkType(MobileType.valueOf(mobileNet)); }

        Object timestamp = j.get("timestamp");
        if(timestamp == null) { timestamp = System.currentTimeMillis(); }
        setTimestamp((long)timestamp);
    }

    /**
     * Creates a new VNetwork object from the given parameters.
     *
     * @param mobileConnected Set this to true if mobile is connected.
     * @param mobileFast Set this to true if mobile is fast.
     * @param mobileNetType Type of mobile network (2G, 3G, 4G, Unknown).
     */
    public CellularNetwork(boolean mobileConnected, boolean mobileFast, MobileType mobileNetType) {
        setMobileConnected(mobileConnected);
        setMobileNetworkFast(mobileFast);
        setMobileNetworkType(mobileNetType);
        setTimestamp(System.currentTimeMillis());
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
     * @return The mobile network type of the network context (e.g. 2G, 3G, 3.5G, 4G).
     */
    public MobileType getMobileNetworkType() {
        return mMobileNetworkType;
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
     * Sets the mobile network type for this network context.
     * Should be either "2G", "3G", "3.5G", or "4G".
     * @param mobileNetType The mobile network type as string.
     */
    public void setMobileNetworkType(MobileType mobileNetType) { mMobileNetworkType = mobileNetType; }

    /**
     * Turns this CellularNetwork context object into a JSON representation.
     * @return Json String
     */
    @Override
    public JSONObject getJson() {
        JSONObject json = new JSONObject();
        json.put("isMobileConnected", isMobileConnected());
        json.put("isMobileNetworkFast", isMobileNetworkFast());
        json.put("mobileNetworkType", getMobileNetworkType().toString());
        json.put("timestamp", getTimestamp());
        return json;
    }

    /**
     * Matches the given mobile network context against this.
     *
     * @param other The context to match against.
     * @return True, if both contexts match. False, if not
     */
    @Override
    public boolean matches(CellularNetwork other) {
        if(other == null) { return false; }
        if (this.isMobileConnected() && other.isMobileConnected())
        {
            if(this.getMobileNetworkType() == null || other.getMobileNetworkType() == null)
            {
                return true;
            }
            else if (this.getMobileNetworkType().equals(other.getMobileNetworkType()))
            {
                return true;
            }
        }
        return false;
    }
}
