package vstoreframework.context;

import org.json.simple.JSONObject;

/**
 * A simplified context description that will be sent to the server for requesting matching files.
 */
@SuppressWarnings({ "serial", "unchecked" })
public class SearchContextDescription extends ContextDescription {

    private int mRadius;
    private String mPlace;
    private boolean mTimeOfDay;
    private long mTimespan;

    public void setRadius(int radius) {
        mRadius = radius;
    }
    public int getRadius() {
        return mRadius;
    }

    public void setPlace(String place) {
        mPlace = place;
    }
    public String getPlace() {
        return mPlace;
    }

    public void enableTimeOfDay(boolean timeOfDay) { mTimeOfDay = timeOfDay; }
    public void setTimespanInMs(long timespan) { mTimespan = timespan; }
    public boolean isTimeOfDayEnabled() { return mTimeOfDay; }
    public long getTimespan() { return mTimespan; }

    public SearchContextDescription() {
        super();
    }


    /**
     * Creates a simplified json version for the request of files matching the given context.
     * @return This search context description as json.
     */
    @Override
    public JSONObject getJson() {
        //Create a json with only the necessary parts for a matching request
        JSONObject j = new JSONObject();
        if(super.hasLocationContext()) 
        {
            j.put("location", super.getLocationContext().getJson());
            j.put("radius", mRadius);
        }
        if(mPlace != null) 
        {
            j.put("place", mPlace);
        }
        if(super.hasActivityContext()) 
        {
            j.put("activity", super.getActivityContext().getJson());
        }
        if(super.hasNoiseContext()) 
        {
            j.put("noise", super.getNoiseContext().getJson());
        }
        if(super.hasNetworkContext()) 
        {
            j.put("network", super.getNetworkContext().getJson());
        }
        if(super.hasWeekdayContext()) 
        {
            j.put("weekday", super.getDayOfWeek());
        }
        if(isTimeOfDayEnabled()) 
        {
            j.put("timeOfDay", true);
            j.put("timeSpanMS", getTimespan());
        }
        j.put("timestamp", super.getTimestamp());
        return j;
    }
}
