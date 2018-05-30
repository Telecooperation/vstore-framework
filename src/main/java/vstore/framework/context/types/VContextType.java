package vstore.framework.context.types;

import java.io.Serializable;

import org.json.simple.JSONObject;

@SuppressWarnings({ "rawtypes", "serial" })
public abstract class VContextType<MatchingParam extends VContextType> implements Serializable {
    /**
     * The timestamp of the acquisition of this context (in seconds).
     */
    protected long mTimestamp;

    /**
     * Sets the timestamp for this activity.
     * 
     * @param timestamp The timestamp in milliseconds.
     */
    public void setTimestamp(long timestamp) {
        mTimestamp = timestamp / 1000;
    }

    /**
     * @return The timestamp in milliseconds.
     */
    public long getTimestamp() {
        return mTimestamp * 1000;
    }
    
    public abstract JSONObject getJson();
    public abstract boolean matches(MatchingParam other);
}
