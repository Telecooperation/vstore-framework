package vstore.framework.context.types;

import org.json.simple.JSONObject;

import java.io.Serializable;

/**
 * Base class for the definition of a context type.
 *
 * @param <ContextType> Please put the name of your new context type class here again.
 */
@SuppressWarnings({ "rawtypes", "serial" })
public abstract class VContextType<ContextType extends VContextType> implements Serializable {
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

    public String getJsonString() {
        return getJson().toJSONString();
    }

    public abstract JSONObject getJson();
    public abstract boolean matches(ContextType other);
}
