package vstore.framework.context.types.activity;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.Serializable;
import java.util.HashMap;

import vstore.framework.context.types.VContextType;

/**
 * This class wraps a physical activity context.
 */
@SuppressWarnings("serial")
public class VActivity extends VContextType<VActivity> implements Serializable {

	/**
     * The possible types of an activity.
     */
    public static final HashMap<String, ActivityType> Types;
    static
    {
        Types = new HashMap<String, ActivityType>();
        Types.put("still", ActivityType.STILL);
        Types.put("walking", ActivityType.WALKING);
        Types.put("invehicle", ActivityType.IN_VEHICLE);
        Types.put("unknown", ActivityType.UNKNOWN);
    }

    /**
     * The type of this physical activity.
     */
    private ActivityType mType;
    /**
     * The confidence that this activity is the actual user's activity.
     */
    private int mConfidence;

    /**
     * Constructs a new VActivity object.
     * 
     * @param type The type for the new activity.
     * @param confidence The confidence that this activity is the actual user's activity.
     * @param timestamp The timestamp of the acquisition this context in milliseconds.
     */
    public VActivity(ActivityType type, int confidence, long timestamp) {
        setType(type);
        setConfidence(confidence);
        setTimestamp(timestamp);
    }

    /**
     * Create an activity from a json string.
     * @param json The json string
     * @throws ParseException is thrown, if the given JSON is not valid.
     */
    public VActivity(String json) throws ParseException {
        this((JSONObject)new JSONParser().parse(json));
    }
    public VActivity(JSONObject json) {
        setType(ActivityType.valueOf((String)json.get("activity")));

        setConfidence(Long.valueOf((long)json.get("confidence")).intValue());
        setTimestamp((long)json.get("time"));

    }

    /**
     * Sets the type for this activity.
     * 
     * @param type (see {@link ActivityType})
     */
    public void setType(ActivityType type) {
        mType = type;
    }

    /**
     * Sets the confidence for this activity.
     * @param confidence The confidence in percent (between 0 and 100).
     */
    public void setConfidence(int confidence) {
        mConfidence = confidence;
    }

    /**
     * @return The type of this activity (see {@link ActivityType}).
     */
    public ActivityType getType() { return mType; }

    /**
     * @return The confidence that this detected activity is true (between 0 and 100).
     */
    public int getConfidence() { return mConfidence; }

    /**
     * @return This activity context as a JSON Object.
     */
    @SuppressWarnings("unchecked")
	public JSONObject getJson() {
        JSONObject activityJson = new JSONObject();
        activityJson.put("activity", this.getType().toString());
        activityJson.put("confidence", this.getConfidence());
        activityJson.put("time", this.getTimestamp());
        return activityJson;
    }

    /**
     * A list of the supported types.
     * @return A hashmap containing all possible activity types.
     */
    public static HashMap<String, ActivityType> getTypes() {
        return Types;
    }


    /**
     * Matches the given activity context against this activity context.
     * @param other The other activity context to check against
     * @return True, if the activity context matches. False, if not.
     */
    public boolean matches(VActivity other) {
        if(other == null) return false;
        return matches(other.getType());
    }

    /**
     * Matches the given activity type against the activity type of this activity context.
     * @param otherActivityType The other activity type to check against.
     * @return True, if the activity type matches. False, if not.
     */
    public boolean matches(ActivityType otherActivityType) {
        if(this.getType().equals(otherActivityType)) {
            return true;
        }
        return false;
    }
}
