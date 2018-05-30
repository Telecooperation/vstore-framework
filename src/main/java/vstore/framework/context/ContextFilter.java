package vstore.framework.context;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Holds information about the currently selected filtering of context.
 */
public class ContextFilter {

    private boolean mLocation;
    private int mRadius = 120;
    private boolean mMostLikelyPlace;
    private boolean mActivity;
    private boolean mNetwork;
    private boolean mNoise;
    private boolean mWeekday;
    private boolean mTimeOfDay;
    private long mTimeSpanMS;

    public ContextFilter() {
        mLocation = false;
        mRadius = 120;
        mMostLikelyPlace = false;
        mActivity = false;
        mNetwork = false;
        mNoise = false;
        mWeekday = false;
        mTimeOfDay = false;
        mTimeSpanMS = 0;
    }

    /**
     * Creates a filter config from the given json string.
     * @param jsonStr The json
     */
    public ContextFilter(String jsonStr) {
        //Call default constructor
        this();
        //Build object from json
        
    	JSONParser p = new JSONParser();
    	JSONObject j = null;
		try 
		{
			j = (JSONObject)p.parse(jsonStr);
		} 
		catch (ParseException e) 
		{
			e.printStackTrace();
		}
        
		//Get data from JSON
		Object location = j.get("isLocationEnabled");
		Object radius = j.get("radius");
		Object place = j.get("isPlaceEnabled");
		Object activity = j.get("isActivityEnabled");
		Object network = j.get("isNetworkEnabled");
		Object noise = j.get("isNoiseEnabled");
		Object weekday = j.get("isWeekdayEnabled");
		Object timeOfDay = j.get("isTimeOfDayEnabled");
		Object timespan = j.get("timeSpanMS");
		
		//Parse data to correct type, if contained in JSON
		enableLocation((location != null) ? (boolean)location : false);
        setRadius((radius != null) ? (int)radius : 0);
        enableMostLikelyPlace((place != null) ? (boolean)place : false);
        enableActivity((activity != null) ? (boolean)activity : false);
        enableNetwork((network != null) ? (boolean)network : false);
        enableNoise((noise != null) ? (boolean)noise : false);
        enableWeekday((weekday != null) ? (boolean)weekday : false);
        enableTimeOfDay((timeOfDay != null) ? (boolean)timeOfDay : false);
        setTimeSpanMS((timespan != null) ? (long)timespan : 0);
    }

    /**
     * Enables/disables location context for this filter.
     * @param location True, if location must be within radius for this filter.
     */
    public void enableLocation(boolean location) {
        mLocation = location;
    }

    /**
     * Only has effect if you set {@link ContextFilter#enableLocation(boolean)} to true.
     * @param radius The radius the filtered files must be within.
     */
    public void setRadius(int radius) {
        mRadius = radius;
    }

    /**
     * Enables/disables most likely place context for this filter.
     * @param place True, if the place the user is located at must match for this filter.
     */
    public void enableMostLikelyPlace(boolean place) {
        mMostLikelyPlace = place;
    }

    /**
     * Enables/disables activity context for this filter.
     * @param activity True, if the activity context must match for this filter.
     */
    public void enableActivity(boolean activity) {
        mActivity = activity;
    }

    /**
     * Enables/disables network context for this filter.
     * @param network True, if the network context must match for this filter.
     */
    public void enableNetwork(boolean network) {
        mNetwork = network;
    }

    /**
     * Enables/disables noise context for this filter.
     * @param noise True, if the noise context must match for this filter.
     */
    public void enableNoise(boolean noise) {
        mNoise = noise;
    }

    /**
     * Enables/disables the weekday filter.
     * @param weekday True, if this filter should only match files with the current day of the week.
     */
    public void enableWeekday(boolean weekday) {
        mWeekday = weekday;
    }

    /**
     * Enables/disables the time-of-day filter.
     * @param timeOfDay True, if this filter should only match files that were uploaded around the
     *                  current time of day
     */
    public void enableTimeOfDay(boolean timeOfDay) {
        mTimeOfDay = timeOfDay;
    }

    /**
     * Sets the timespan for this filter in milliseconds.
     * @param timespan The time span in milliseconds.
     */
    public void setTimeSpanMS(long timespan) {
        mTimeSpanMS = timespan;
    }

    /**
     * @return True, if location context is enabled for this filter.
     */
    public boolean isLocationEnabled() {
        return mLocation;
    }

    /**
     * @return The radius for the location filter.
     */
    public int getRadius() {
        return mRadius;
    }

    /**
     * @return True, if the most likely place must match for this filter.
     */
    public boolean isPlaceEnabled() {
        return mMostLikelyPlace;
    }

    /**
     * @return True, if the activity context must match for this filter.
     */
    public boolean isActivityEnabled() {
        return mActivity;
    }

    /**
     * @return True, if the network context must match for this filter.
     */
    public boolean isNetworkEnabled() {
        return mNetwork;
    }

    /**
     * @return True, if the noise context must match for this filter.
     */
    public boolean isNoiseEnabled() {
        return mNoise;
    }

    /**
     * @return True, if the weekday context must match for this filter.
     */
    public boolean isWeekdayEnabled() { return mWeekday; }

    /**
     * @return True, if the time-of-day context must match for this filter.
     */
    public boolean isTimeOfDayEnabled() { return mTimeOfDay; }

    /**
     * @return The timespan of this filter in milliseconds.
     */
    public long getTimeSpanMS() {
        return mTimeSpanMS;
    }

    /**
     * @return This filter in a json object.
     */
    @SuppressWarnings("unchecked")
	public JSONObject getJson() {
        JSONObject j = new JSONObject();
        j.put("isLocationEnabled", isLocationEnabled());
        j.put("radius", getRadius());
        j.put("isPlaceEnabled", isPlaceEnabled());
        j.put("isActivityEnabled", isActivityEnabled());
        j.put("isNetworkEnabled", isNetworkEnabled());
        j.put("isNoiseEnabled", isNoiseEnabled());
        j.put("isWeekdayEnabled", isWeekdayEnabled());
        j.put("isTimeOfDayEnabled", isTimeOfDayEnabled());
        j.put("timeSpanMS", getTimeSpanMS());
        return j;
    }

    public static ContextFilter getDefaultFilter() {
        ContextFilter f = new ContextFilter();
        f.enableLocation(true);
        f.setRadius(120);
        f.enableTimeOfDay(true);
        f.setTimeSpanMS(3*60*60*1000);
        return f;
    }
}
