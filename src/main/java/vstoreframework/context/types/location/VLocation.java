package vstoreframework.context.types.location;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import vstoreframework.context.types.VContextType;

/**
 * A representation for a location (latitude, longitude, accuracy, timestamp).
 */
@SuppressWarnings({ "serial", "unchecked" })
public class VLocation extends VContextType<VLocation> {
    /**
     * The coordinates of this location context.
     */
    private VLatLng mLatLng;
    /**
     * The accuracy of this location context.
     */
    private float mAccuracy;
    /**
     * A human-readable description of the location (if available).
     */
    private String mDescription;

    /**
     *
     * @param latlng The coordinates of this location context.
     * @param accuracy The accuracy of this location context.
     * @param time The timestamp of the acquisition of this location context in milliseconds.
     * @param description A human-readable description of the location (if available).
     */
    public VLocation(VLatLng latlng, float accuracy, long time, String description) {
        setLatLng(latlng);
        setAccuracy(accuracy);
        setTimestamp(time);
        setDescription(description);
    }

    /**
     * Creates a new VLocation object from a json string.
     * @param json The json string
     * @throws ParseException in case the given JSON is malformed.
     */
    public VLocation(String json) throws ParseException {
        JSONObject j = (JSONObject)(new JSONParser().parse(json));
        setLatLng(new VLatLng((double)j.get("lat"), (double)j.get("lng")));
        setAccuracy((float) j.get("acc"));
        setTimestamp((long)j.get("time"));
        setDescription((String)j.get("description"));
    }

    public VLatLng getLatLng() { return mLatLng; }
    public float getAccuracy() { return mAccuracy; }
    public String getDescription() { return mDescription; }

    public void setLatLng(VLatLng latlng) { mLatLng = latlng; }
    public void setAccuracy(float acc) { mAccuracy = acc; }
    public void setDescription(String description) { mDescription = description; }

    /**
     * @return This location context as a JSON String.
     */
    public JSONObject getJson() {
        JSONObject locJson = new JSONObject();
        if(getLatLng() != null) 
        {
            locJson.put("lat", getLatLng().getLatitude());
            locJson.put("lng", getLatLng().getLongitude());
        } 
        else 
        {
            locJson.put("lat", 0);
            locJson.put("lng", 0);
        }
        locJson.put("acc", getAccuracy());
        locJson.put("time", getTimestamp());
        locJson.put("description", getDescription());
        return locJson;
    }

	@Override
	public boolean matches(VLocation other) {
		if(other == null) { return false; }
		return (other.getLatLng().matches(mLatLng));
	}

}
