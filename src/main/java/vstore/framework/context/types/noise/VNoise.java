package vstore.framework.context.types.noise;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import vstore.framework.context.types.VContextType;

/**
 * This class represents an instance of the Noise context data.
 */
@SuppressWarnings({ "serial", "unchecked" })
public class VNoise extends VContextType<VNoise> {
    public static final int DEFAULT_THRESHOLD_RMS = 1000;
    
    /**
     * The default loudness threshold. 0dB is the loudest signal, -90.3dB is the lowest
     * (this limitation comes from AWARE using 16Bit PCM for sampling).
     */
    public static final int DEFAULT_TRESHOLD_DB = -25;

    private double mDb;
    private double mRMS;
    private int mRMSThreshold;
    private int mDBThreshold;

    /**
     * Create a new noise object.
     * If you don't want to specify a timestamp, you can use the constructor
     * {@link VNoise(double, double, double, double)}
     * @param db Current noise in db.
     * @param rms Current noise rms. Currently not used.
     * @param rmsThreshold Currently not used.
     * @param dbThreshold If this value is crossed, noise will be considered as loud.
     *                    A rock concert for example has around 115dB, a vacuum cleaner has
     *                    75dB and whispering has 30dB.
     * @param timestamp Timestamp of measurement in milliseconds
     */
    public VNoise(double db, double rms, int rmsThreshold, int dbThreshold, long timestamp) {
        setDb(db);
        setRMS(rms);
        setRMSThreshold(rmsThreshold);
        setDbThreshold(dbThreshold);
        setTimestamp(timestamp);
    }

    /**
     * Creates a new noise object with the current system time unix timestamp.
     * @param db Current noise in db. (From -90.3dB=lowest to 0dB=loudest)
     * @param rms Current noise rms.
     * @param rmsThreshold Currently not used.
     * @param dbThreshold If this value is crossed, noise will be considered as loud.
     *                    0dB is the loudest signal, and -90.3dB is the lowest
     *                    (this limitation comes from AWARE using 16Bit PCM for sampling).
     */
    public VNoise(double db, double rms, int rmsThreshold, int dbThreshold) {
        this(db, rms, rmsThreshold, dbThreshold, System.currentTimeMillis());
    }

    /**
     * Create a noise object from json string.
     * 
     * @param json The json string.
     * @throws ParseException in case the given JSON is malformed.
     */
	public VNoise(String json) throws ParseException {
        JSONObject j = (JSONObject)(new JSONParser().parse(json));
        //Try to fetch each property from json. If it fails, set a default value.
        setDb((double)j.getOrDefault("sound_db", 0));
        setRMS((double)j.getOrDefault("sound_rms", 0));
        setRMSThreshold((int)j.getOrDefault("sound_rms_thresh", -1));
        setDbThreshold((int)j.getOrDefault("sound_db_thresh", -1));
        setTimestamp((long)j.getOrDefault("time", System.currentTimeMillis()));
    }

    public double getDb() {
        return mDb;
    }
    public double getRMS() {
        return mRMS;
    }
    public int getRMSThreshold() {
        return mRMSThreshold;
    }
    public int getDBThreshold() { return mDBThreshold; }

    /**
     * If the measured dB value is higher than the dB threshold in this object,
     * then noise is considered as loud.
     * Possible scale is from -90.3dB = lowest to 0dB = loudest.
     * @return True, if environment is silent for this context.
     */
    public boolean isSilent() {
        if(mDb < mDBThreshold) {
            return true;
        }
        return false;
    }

    public void setDb(double db) {
        mDb = db;
    }
    public void setRMS(double rms) {
        mRMS = rms;
    }
    public void setRMSThreshold(int rmsThresh) { mRMSThreshold = rmsThresh; }

    /**
     * Sets the dB threshold, that has to be crossed to treat the signal as loud.
     * Possible scale is from -90.3dB = lowest to 0dB = loudest.
     * @param dbThresh The threshold in dB.
     */
    public void setDbThreshold(int dbThresh) { mDBThreshold = dbThresh; }

    /**
     * @return This object as a JSON representation.
     */
    public JSONObject getJson() {
        JSONObject json = new JSONObject();
        json.put("sound_db", getDb());
        json.put("sound_rms", getRMS());
        json.put("sound_rms_thresh", getRMSThreshold());
        json.put("sound_db_thresh", getDBThreshold());
        json.put("time", getTimestamp());
        return json;
    }

    /**
     * Checks if the loudness in dB of the given noise context is over the dB threshold of
     * this noise context.
     * @param other The other noise context to match against the threshold of this noise context.
     * @return True, if the other noise context matches this one.
     */
    public boolean matches(VNoise other) {
        return (other.getDb() > this.getDBThreshold()) == !this.isSilent();
    }
}
