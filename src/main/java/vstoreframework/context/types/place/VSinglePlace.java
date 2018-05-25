package vstoreframework.context.types.place;

import java.io.Serializable;

import org.json.simple.JSONObject;

import vstoreframework.context.types.VContextType;
import vstoreframework.context.types.location.VLatLng;
import vstoreframework.context.types.location.VLocation;
import vstoreframework.utils.ContextUtils;

@SuppressWarnings({ "serial", "unchecked" })
public class VSinglePlace extends VContextType<VSinglePlace> implements Serializable {

    //Thresholds for accepting a place as the current place
    public static final float DISTANCE_THRESHOLD = 100;
    public static final float SAME_PLACE_DISTANCE_THRESHOLD_KM = 0.5f;
    public static final float LIKELIHOOD_THRESHOLD = 0.15f;
    public static final float LIKELIHOOD_THRESHOLD_2 = 0.3f;

    //Keys for Places JSON Object
    public static final String KEY_PLACE_ID = "id";
    public static final String KEY_PLACE_NAME = "name";
    public static final String KEY_PLACE_LAT = "lat";
    public static final String KEY_PLACE_LONG = "lng";
    public static final String KEY_PLACE_TYPE = "type";
    public static final String KEY_PLACE_TYPE_TEXT = "typetext";
    public static final String KEY_PLACE_LIKELIHOOD = "likelihood";
    public static final String KEY_PLACE_IS_LIKELY = "is_likely";

    /**
     * The id of the place
     */
    private String mId;
    /**
     * The type of the place.
     */
    private PlaceType placeType;
    /**
     * The readable string that describes this place's type.
     */
    private String placeTypeText;
    /**
     * The name of the place.
     */
    private String name;
    /**
     * The lat/lng coordinates of the place.
     */
    private VLatLng latLng;
    /**
     * The likelihood that the user is located at this place.
     */
    private double likelihood;
    /**
     * The distance to the place in km.
     */
    private float distance;

    /**
     * This flag is true if it is most likely that the user is here.
     */
    private boolean isLikely = false;

    /**
     * Constructs a new single place object from the given JSON object.
     * @param p
     */
	public VSinglePlace(JSONObject p) {
        setPlaceTypeText((String)p.get(KEY_PLACE_TYPE_TEXT));
        try 
        {
            setPlaceType(PlaceType.valueOf((String)p.getOrDefault(KEY_PLACE_TYPE, "")));
        } 
        catch (IllegalArgumentException ex) 
        {
        	setPlaceType(PlaceType.UNKNOWN);
        }
        setName((String)p.get(KEY_PLACE_NAME));
        setLatLng((double)p.get(KEY_PLACE_LAT), (double)p.get(KEY_PLACE_LONG));
        setLikelihood((double)p.get(KEY_PLACE_LIKELIHOOD));
        setMostLikely((boolean)p.get(KEY_PLACE_IS_LIKELY));
        
    }

    /**
     * Sets the id for this place.
     * @param id The id
     */
    public void setId(String id) { mId = id; }

    /**
     * Sets the name of this place as reported by the Google API.
     * @param name The name of the place.
     */
    public void setName(String name) { this.name = name; }

    /**
     * Sets the latitude and longitude coordinates for this place.
     * @param lat The latitude
     * @param lng The longitude
     */
    public void setLatLng(double lat, double lng) { latLng = new VLatLng(lat, lng); }

    /**
     * Sets the likelihood that the user is located at this place.
     * @param l The likelihood (from 0 to 1).
     */
    public void setLikelihood(double l) { likelihood = l; }

    /**
     * Sets the readable string that describes this place's type.
     * @param text The place type text
     */
    public void setPlaceTypeText(String text) {
        placeTypeText = text;
    }

    /**
     * Sets the vStore place category this place belongs to.
     * @param placetype A category from {@link PlaceConstants.PlaceType}.
     */
    public void setPlaceType(PlaceType placetype) { this.placeType = placetype; }

    /**
     * Sets the flag that this place is most likely that the user is located here.
     * @param mostLikely True, if the user is most likely here.
     */
    public void setMostLikely(boolean mostLikely) { isLikely = mostLikely; }

    /**
     * @return The id of this place.
     */
    public String getId() { return mId; }

    /**
     * @return The readable string that describes this place's type.
     */
    public String getPlaceTypeText() { return placeTypeText; }

    /**
     * @return The name of this place.
     */
    public String getName() { return name; }

    /**
     * @return The latitude and longitude coordinates of this place.
     */
    public VLatLng getLatLng() { return latLng; }

    /**
     * @return The likelihood that the user is located at this place.
     */
    public double getLikelihood() { return likelihood; }

    /**
     * @return The distance to this place in km (call {@link VSinglePlace#calculateDistanceFrom} before).
     */
    public float getDistance() { return distance; }

    /**
     * @return The category this place belongs to ({@link PlaceType}).
     */
    public PlaceType getPlaceType() { return placeType; }

    /**
     * @return True, if it is most likely that the user is located at this place.
     */
    public boolean isLikely() { return isLikely; }

	public JSONObject getJson() {
        JSONObject j = new JSONObject();
        
        j.put(KEY_PLACE_ID, getId());
        j.put(KEY_PLACE_NAME, getName());
        j.put(KEY_PLACE_LAT, getLatLng().getLatitude());
        j.put(KEY_PLACE_LONG, getLatLng().getLongitude());
        j.put(KEY_PLACE_TYPE_TEXT, getPlaceTypeText());
        j.put(KEY_PLACE_TYPE, getPlaceType());
        j.put(KEY_PLACE_LIKELIHOOD, getLikelihood());
        j.put(KEY_PLACE_IS_LIKELY, isLikely());
        
        return j;
    }

    /**
     * This function calculactes the approximate distance in km for this place.
     * @param location The location to take as starting point for the calculation
     */
    public void calculateDistanceFrom(VLocation location) {
        if(location != null && location.getLatLng() != null) {
            //Result is in meters, so divide result by 1000 to get km
            distance = ContextUtils
                    .distanceBetween(location.getLatLng(), this.getLatLng()) / 1000.0f;
        } else {
            distance = 0;
        }
    }
    
    /**
     * Calculates the distance between two places in km.
     * 
     * @param other The other place
     * @return The distance in kilometers (km).
     */
    public float calculateDistanceFrom(VSinglePlace other) {
        if(other == null || other.getLatLng() == null) { return Float.MAX_VALUE; }
        //Result is in meters, so divide result by 1000 to get km
        return ContextUtils.distanceBetween(other.getLatLng(), this.getLatLng()) / 1000.0f;
    
    }

	@Override
	public boolean matches(VSinglePlace other) {
		if(other == null) return false;
		if(other.getPlaceType().equals(this.getPlaceType())
				&& calculateDistanceFrom(other) < SAME_PLACE_DISTANCE_THRESHOLD_KM)
		{
			return true;
		}
		return false;
	}
}
