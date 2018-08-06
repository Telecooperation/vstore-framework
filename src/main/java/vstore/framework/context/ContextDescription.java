package vstore.framework.context;


import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import vstore.framework.context.types.activity.VActivity;
import vstore.framework.context.types.location.VLocation;
import vstore.framework.context.types.network.VNetwork;
import vstore.framework.context.types.noise.VNoise;
import vstore.framework.context.types.place.PlaceType;
import vstore.framework.context.types.place.VPlaces;
import vstore.framework.context.types.place.VSinglePlace;

/**
 * This class represents the description of a context. It contains location, nearby places,
 * network connectivity, time, events.
 * Not every type of context has to be available in every situation, so you should better check if
 * it is null before using it.
 */
@SuppressWarnings("serial")
public class ContextDescription implements Serializable {
	private VLocation mLocation;
    private VPlaces mPlaces;
    private VActivity mActivity;
    private VNetwork mNetwork;
    private VNoise mNoise;

    private int mWeekday;

    /**
     * The timestamp of this context in seconds.
     */
    private long mTimestamp;

    /**
     * Constructs an empty context description containing only a timestamp from the current time.
     */
    public ContextDescription() {
        mLocation = null;
        mPlaces = null;
        mActivity = null;
        mNetwork = null;
        mNetwork = null;
        mNoise = null;
        mWeekday = -1;
        setTimestamp(System.currentTimeMillis());
    }

    /**
     * Create a new context description from a json string
     * @param json The json string containing the context description.
     */
    public ContextDescription(String json) {
        //Predefine strings we will read from the Json object
        JSONObject jLocation = null;
        JSONObject jPlaces = null;
        JSONObject jActivity = null;
        JSONObject jNetwork = null;
        JSONObject jNoise = null;
        int weekday = -1;
        long timestamp = 0;

        try {
        	JSONParser jParser = new JSONParser();
        	
            JSONObject j = (JSONObject) jParser.parse(json);
            
            //Handle fail, if context data is not available in string
            //Necessary because JSONObject methods will throw an exception
            //if getString(key) is not available.
            //Exception can be ignored, it just means that the String remains null as defined above.
            try {
	            jLocation = (JSONObject)j.get("location");
	            jPlaces = (JSONObject)j.get("places");
	            jActivity = (JSONObject)j.get("activity");
	            jNetwork = (JSONObject)j.get("network");
	            jNoise = (JSONObject)j.get("noise");
	            timestamp = (long)j.get("timestamp");
	            Object tmp = j.get("weekday");
	            if(tmp != null) {
                    weekday = Long.valueOf((long)tmp).intValue();
                }
            } catch(Exception ignored) {
                ignored.printStackTrace();
            }
            
            if(jLocation != null)
                setLocationContext(new VLocation(jLocation));
            if(jPlaces != null)
                setPlacesContext(new VPlaces(jPlaces));
            if(jActivity != null)
                setActivityContext(new VActivity(jActivity));
            if(jNetwork != null)
                setNetworkContext(new VNetwork(jNetwork));
            if(jNoise != null)
                setNoiseContext(new VNoise(jNoise));
            if(weekday != -1)
                setDayOfWeek(weekday);
            if(timestamp != 0)
                setTimestamp(timestamp);
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }
    }

    /**
     * @return The Location object containing the location of this usage context.
     * Be careful, it can be null!
     */
    public VLocation getLocationContext() {
        return mLocation;
    }
    public void setLocationContext(VLocation l) {
        mLocation = l;
    }
    public void setLocationTimestamp(long timestamp) {
        if(mLocation != null)
            mLocation.setTimestamp(timestamp);
    }
    public long getLocationTimestamp() {
        if (mLocation != null)
            return mLocation.getTimestamp();
        return 0;
    }
    public void clearLocationContext() {
        mLocation = null;
    }

    /**
     * @return The place the user is most likely located at (according to the Google Places API likelihood
     * and LIKELIHOOD_THRESHOLD and DISTANCE_THRESHOLD configured in @link{VSinglePlace}).
     *
     */
    public VSinglePlace getMostLikelyPlace() {
        if(mPlaces != null) {
            return mPlaces.getMostLikelyPlace();
        }
        return null;
    }

    /**
     * @return A VPlaces object containing places the user is currently close to
     * (according to the Google Places API).
     */
    public VPlaces getPlaces() {
        if(mPlaces != null) {
            return mPlaces;
        }
        return null;
    }

    /**
     * @return A list of places the user is currently close to (according to the Google Places API).
     */
    public ArrayList<VSinglePlace> getPlacesList() {
        if(mPlaces != null) {
            return mPlaces.getPlaceList();
        }
        return null;
    }

    /**
     * Returns a list containing the types of places that are nearby.
     * @return A list with the place types.
     */
    public List<PlaceType> getNearbyPlaceTypes() {
        if(mPlaces == null) { return null; }
        return mPlaces.getNearbyPlaceTypes();
    }

    /**
     * Adds the given place to the places list.
     * @param p The place to add.
     */
    public void putPlace(VSinglePlace p) {
        if(mPlaces == null) 
        {
            mPlaces = new VPlaces(new ArrayList<VSinglePlace>(), System.currentTimeMillis());
        }
        mPlaces.appendPlace(p);
    }

    /**
     * Sets the timestamp for the places context.
     * @param timestamp The timestamp in seconds.
     */
    public void setPlacesTimestamp(long timestamp) {
        if(mPlaces != null) {
            mPlaces.setTimestamp(timestamp);
        }
    }

    /**
     * Gets the timestamp of the places context.
     * @return The unix timestamp in seconds.
     */
    public long getPlacesTimestamp() {
        if(mPlaces != null)
            return mPlaces.getTimestamp();
        return 0;
    }

    /**
     * Sets the places context of this context description to the given places context.
     * @param p The new places context for this description.
     */
    public void setPlacesContext(VPlaces p) {
        mPlaces = p;
    }

    /**
     * Sets the places context to null.
     */
    public void clearPlacesContext() {
        mPlaces = null;
    }

    /**
     * @return The DetectedActivity of this usage context.
     * Be careful, it can be null!
     */
    public VActivity getActivityContext() {
        return mActivity;
    }

    /**
     * Sets the activity context for this context description.
     * @param a The new activity context for this context description.
     */
    public void setActivityContext(VActivity a) {
        mActivity = a;
    }
    /**
     * Sets the timestamp for the activity context.
     * @param timestamp The timestamp in seconds.
     */
    public void setActivityTimestamp(long timestamp) {
        if(mActivity != null)
            mActivity.setTimestamp(timestamp);
    }

    /**
     * Gets the timestamp of the activity context.
     * @return The unix timestamp in seconds.
     */
    public long getActivityTimestamp() {
        if(mActivity != null)
            return mActivity.getTimestamp();
        return 0;
    }

    /**
     * Sets the activity context to null.
     */
    public void clearActivityContext() {
        mActivity = null;
    }

    /**
     * @return The Network context data of this usage context.
     * Be careful, it can be null!
     */
    public VNetwork getNetworkContext() { return mNetwork; }
    public void setNetworkContext(VNetwork network) { mNetwork = network; }
    public void setNetworkTimestamp(long timestamp) {
        if(mNetwork != null)
            mNetwork.setTimestamp(timestamp);
    }
    public long getNetworkTimestamp() {
        if(mNetwork != null) {
            return mNetwork.getTimestamp();
        }
        return 0;
    }
    public void clearNetworkContext() {
        mNetwork = null;
    }

    /**
     * @return The Environment Noise context data of this usage context.
     * Be careful, it can be null!
     */
    public VNoise getNoiseContext() {
        return mNoise;
    }
    public void setNoiseContext(VNoise noise) {
        mNoise = noise;
    }
    public void setNoiseTimestamp(long timestamp) {
        if(mNoise != null)
            mNoise.setTimestamp(timestamp);
    }
    public long getNoiseTimestamp() {
        if(mNoise != null) {
            return mNoise.getTimestamp();
        }
        return 0;
    }
    public void clearNoiseContext() {
        mNoise = null;
    }

    /**
     * Sets the day of the week for this context description.
     * @param weekday 1=Monday, 7=Sunday
     */
    public void setDayOfWeek(int weekday) {
        if(weekday >= 1 && weekday <= 7) {
            mWeekday = weekday;
        } else {
            mWeekday = -1;
        }
    }

    /**
     * @return The day of the week of this context description.
     */
    public int getDayOfWeek() {
        return mWeekday;
    }


    /**
     * Sets the timestamp for this usage context description.
     * @param timestamp The timestamp in milliseconds
     */
    public void setTimestamp(long timestamp) {
        mTimestamp = timestamp/1000;
    }

    /**
     * @return The timestamp for this usage context description in milliseconds.
     */
    public long getTimestamp() {
        return mTimestamp*1000;
    }

    /**
     * Checks, if this context description has location context available.
     * @return True, if location context data is found in this description.
     */
    public boolean hasLocationContext() {
        if(mLocation != null) {
            return true;
        }
        return false;
    }

    /**
     * Checks, if this context description has places context available.
     * @return True, if places context data is found in this description.
     */
    public boolean hasPlacesContext() {
        if(mPlaces != null && mPlaces.getPlaceList() != null && mPlaces.getPlaceList().size() > 0) {
            return true;
        }
        return false;
    }

    /**
     * Checks, if this context description has activity context available.
     * @return True, if activity context data is found in this description.
     */
    public boolean hasActivityContext() {
        if(mActivity != null) {
            return true;
        }
        return false;
    }

    /**
     * Checks, if this context description has network context available.
     * @return True, if network context data is found in this description.
     */
    public boolean hasNetworkContext() {
        if(mNetwork != null) {
            return true;
        }
        return false;
    }

    /**
     * Checks, if this context description has noise context available.
     * @return True, if noise context data is found in this description.
     */
    public boolean hasNoiseContext() {
        if(mNoise != null) {
            return true;
        }
        return false;
    }

    /**
     * Checks, if this context description has weekday context available.
     * @return True, if weekday context data is found in this description.
     */
    public boolean hasWeekdayContext() {
        if(mWeekday != -1) {
            return true;
        }
        return false;
    }



    /**
     * @return The representation of this context in the JSON-Format. See documentation
     * for an illustrated overview of the object.
     */
    @SuppressWarnings("unchecked")
	public JSONObject getJson() {
        JSONObject j = new JSONObject();
        
        if(mLocation != null)
            j.put("location", mLocation.getJson());
        if(mPlaces != null)
            j.put("places", mPlaces.getJson());
        if(mActivity != null)
            j.put("activity", mActivity.getJson());
        if(mNoise != null)
            j.put("noise", mNoise.getJson());
        if(mNetwork != null)
            j.put("network", mNetwork.getJson());
        if(mWeekday != -1)
            j.put("weekday", getDayOfWeek());
        //To add another context here, simply do this:
        //if(mYourContextType != null)
        //    j.put("yourContextType", getYourContext());

        j.put("timestamp", getTimestamp());
        
        return j;
    }
}
