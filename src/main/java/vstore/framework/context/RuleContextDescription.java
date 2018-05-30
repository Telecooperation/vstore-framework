package vstore.framework.context;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import vstore.framework.context.types.activity.ActivityType;
import vstore.framework.context.types.location.VLatLng;
import vstore.framework.context.types.network.VNetwork;
import vstore.framework.context.types.noise.VNoise;
import vstore.framework.context.types.place.PlaceType;

/**
 * This class encapsulates the context data that a rule can be configured to contain.
 * This does not extend ContextDescription, because the contained context types are somewhat
 * different.
 */
@SuppressWarnings({"unchecked"})
public class RuleContextDescription {
    private VLatLng mLocation;
    private int mRadius;
    private List<PlaceType> mPlaceTypes;
    private ActivityType mActivityContext;
    private VNetwork mNetworkContext;
    private VNoise mNoiseContext;

    /**
     * Constructs an empty RuleContextDescription object that contains no configured context data.
     * Configure the context data by using the .set*Context methods.
     */
    public RuleContextDescription() {
        mLocation = null;
        mRadius = -1;
        mPlaceTypes = null;
        mActivityContext = null;
        mNetworkContext = null;
        mNoiseContext = null;
    }

    /**
     * Constructs a new rule context description based on the given JSON string.
     * @param json The json string
     * @throws ParseException in case the given JSON is malformed.
     */
	public RuleContextDescription(String json) throws ParseException {
        this();
        JSONObject j = (JSONObject)(new JSONParser().parse(json));
        //Get location context from json string
        mLocation = new VLatLng((String)j.getOrDefault("location", "{}"));
        //Get radius from json string
        mRadius = (int)j.getOrDefault("radius", 0);
        //Get place types from json string
        JSONArray jPlaceTypes = (JSONArray)j.getOrDefault("placetypes", "[]");
        for(int i = 0; i<jPlaceTypes.size(); i++) 
        {
            try 
            {
                PlaceType type = PlaceType.valueOf((String)jPlaceTypes.get(i));
                addPlaceType(type);
            } catch(IllegalArgumentException e) { }
        }
        //Get activity context from json string
        setActivityContext(ActivityType.valueOf((String)j.getOrDefault("activity", "unknown")));
        //Get network context from json string
        setNetworkContext(new VNetwork((String)j.getOrDefault("network", "")));
        //Get noise context from json string
        setNoiseContext(new VNoise((String)j.getOrDefault("noise", "")));
    }

    /**
     * Sets the location data for the rule context description object.
     * @param lat The latitude for the location
     * @param lng The longitude for the location
     * @param radius The radius for the location in meters. If location is within the radius,
     *               this context is fulfilled.
     */
    public void setLocationContext(double lat, double lng, int radius) {
        mLocation = new VLatLng(lat, lng);
        mRadius = radius;
    }

    /**
     * Sets the place types for the rule to the given list of place types.
     * 
     * @param types The list containing the new place types.
     */
    public void setPlaceTypes(List<PlaceType> types) {
        if(mPlaceTypes != null) {
            mPlaceTypes.clear();
        } else {
            mPlaceTypes = new ArrayList<>();
        }
        for(PlaceType p : types) { mPlaceTypes.add(p); }
    }

    /**
     * Adds a place type to this rule context description.
     * @param type The place type to add.
     */
    public void addPlaceType(PlaceType type) {
        if(mPlaceTypes != null) {
            if (!mPlaceTypes.contains(type)) {
                mPlaceTypes.add(type);
            }
        } else {
            mPlaceTypes = new ArrayList<>();
            mPlaceTypes.add(type);
        }
    }

    public void removePlaceType(PlaceType type) {
        if(mPlaceTypes != null) {
            mPlaceTypes.remove(type);
        }
    }

    /**
     * Sets the activity for this rule context description. For possible activities see
     * {@link ActivityType}.
     * This activity has to be fulfilled together with the other context so that the rule can
     * trigger.
     * @param activity The activity to be set for the rule.
     */
    public void setActivityContext(ActivityType activity) {
        mActivityContext = activity;
    }

    /**
     * Sets the network context for this rule context description. This condition has to be
     * fulfilled for the rule to trigger (given that all other context is fulfilled).
     * @param network The network context data.
     */
    public void setNetworkContext(VNetwork network) {
        mNetworkContext = network;
    }

    /**
     * Sets the noise context for this rule context description. The rule can only fire, if this
     * context is fulfilled together with the other context that is configured.
     * @param noise The noise context data.
     */
    public void setNoiseContext(VNoise noise) {
        mNoiseContext = noise;
    }

    /**
     * Returns the location context of the rule context description.
     * @return The VLatLng object containing the location that has to be matched for the rule
     *         to trigger.
     */
    public VLatLng getLocationContext() {
        return mLocation;
    }

    /**
     * Returns the radius for the location in meters. If the user's location is within this radius,
     * the location context is fulfilled.
     * @return The radius in meters.
     */
    public int getRadius() {
        return mRadius;
    }

    /**
     * Returns the place types of the rule context description. The rule triggers, if all other
     * context is matched and one place from this list is given in the current context.
     * @return A list containing all place types that are set for the rule.
     */
    public List<PlaceType> getPlaceTypes() {
        if(mPlaceTypes != null) {
            return mPlaceTypes;
        }
        return null;
    }

    /**
     * Returns the activity of the rule context description. The rule triggers, if all other
     * context is matched and the activity is given in the current context.
     * @return The activity for the rule.
     */
    public ActivityType getActivityContext() {
        return mActivityContext;
    }

    /**
     * Returns the network context configured for the rule context description.
     * The rule triggers, if all other context is matched and these network conditions are met.
     * @return The network context for the rule.
     */
    public VNetwork getNetworkContext() {
        return mNetworkContext;
    }

    /**
     * Returns the noise context configured for the rule context description.
     * The rule triggers, if all other context is matched and these noise conditions are met.
     * @return The noise context for the rule.
     */
    public VNoise getNoiseContext() {
        return mNoiseContext;
    }

    public JSONObject getJson() {
        JSONObject j = new JSONObject();
        if(hasLocationContext()) {
            j.put("location", this.getLocationContext().getJson());
            j.put("radius", this.getRadius());
        }
        if(hasPlaceContext()) {
            JSONArray jPlaceTypes = new JSONArray();
            Iterator<PlaceType> iter = this.getPlaceTypes().iterator();
            int i = 0;
            while(iter.hasNext()) {
                jPlaceTypes.add(i, iter.next().name());
                i++;
            }
            j.put("placetypes", jPlaceTypes);
        }
        if(hasActivityContext())
            j.put("activity", this.getActivityContext());
        if(hasNetworkContext())
            j.put("network", this.getNetworkContext().getJson());
        if(hasNoiseContext())
            j.put("noise", this.getNoiseContext().getJson());
        return j;
    }


    /**
     * @return True, if the location context is configured.
     */
    public boolean hasLocationContext() {
        if((mLocation != null) && (mLocation.getLatitude() > 0) && (mLocation.getLongitude() > 0)) {
            return true;
        }
        return false;
    }

    /**
     * @return True, if place type context is configured.
     */
    public boolean hasPlaceContext() {
        if(mPlaceTypes != null) {
            return true;
        }
        return false;
    }

    /**
     * @return True, if activity context is configured.
     */
    public boolean hasActivityContext() {
        if(mActivityContext != null) {
            return true;
        }
        return false;
    }

    /**
     * @return True, if the rule has network context configured.
     */
    public boolean hasNetworkContext() {
        if(mNetworkContext != null) {
            return true;
        }
        return false;
    }

    /**
     * @return True, if noise context is configured.
     */
    public boolean hasNoiseContext() {
        if(mNoiseContext != null) {
            return true;
        }
        return false;
    }

    /**
     * Clears the location context from this rule context description.
     */
    public void clearLocationContext() {
        mLocation = null;
    }

    /**
     * Clears the place context from this rule context description.
     */
    public void clearPlaceContext() {
        mPlaceTypes = null;
    }

    /**
     * Clears the activity context from this rule context description.
     */
    public void clearActivityContext() {
        mActivityContext = null;
    }

    /**
     * Clears the network context from this rule context description.
     */
    public void clearNetworkContext() {
        mNetworkContext = null;
    }

    /**
     * Clears the noise context from this rule context description.
     */
    public void clearNoiseContext() {
        mNoiseContext = null;
    }
}
