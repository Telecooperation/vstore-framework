package vstore.framework.context.types.location;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import vstore.framework.context.types.VContextType;
import vstore.framework.utils.ContextUtils;

/**
 * A representation for latitude/longitude
 */
@SuppressWarnings({ "serial", "unchecked" })
public class VLatLng extends VContextType<VLatLng> {
	
	private static double MAX_EQUALS_DIFF_KM = 0.01;

    private double mLat;
    private double mLng;

    /**
     * Constructs a new VLatLng object from the given parameters.
     * 
     * @param lat The latitude for the location.
     * @param lng The longitude for the location.
     */
    public VLatLng(double lat, double lng) {
        mLat = lat;
        mLng = lng;
    }

    /**
     * Constructs this object from the given JSON string.
     * 
     * @param json Json object should contain two 
     * 		 	   doubles with keys "lat" and "long".  
     * @throws ParseException in case the given JSON is malformed.
     */
    public VLatLng(String json) throws ParseException {
    	JSONParser jP = new JSONParser();
        JSONObject j = (JSONObject)jP.parse(json);
        this.mLat = (double)j.get("lat");
        this.mLng = (double)j.get("lng");
    }

    /**
     * Sets the latitude and longitude for the location.
     * @param lat The latitude.
     * @param lng The longitude.
     */
    public void setLatLng(double lat, double lng) { mLat = lat; mLng = lng; }

    /**
     * Sets only the latitude for this location.
     * @param lat The latidude.
     */
    public void setLatitude(double lat) { mLat = lat; }

    /**
     * Sets only the longitude for this location.
     * @param lng The longitude.
     */
    public void setLongitude(double lng) { mLat = lng; }

    /**
     * @return The latitude for this location.
     */
    public double getLatitude() { return mLat; }

    /**
     * @return The longitude for this location.
     */
    public double getLongitude() { return mLng; }

    /**
     * @return This VLatLng object as a JSON string.
     */
    public JSONObject getJson() {
        JSONObject j = new JSONObject();
        j.put("lat", getLatitude());
        j.put("lng", getLongitude());
        return j;
    }

	@Override
	public boolean matches(VLatLng other) {
		if(other == null) { return false; }
		return (ContextUtils.distanceBetween(this, other) < MAX_EQUALS_DIFF_KM);
	}
}
