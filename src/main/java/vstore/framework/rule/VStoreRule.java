package vstore.framework.rule;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import vstore.framework.context.RuleContextDescription;
import vstore.framework.context.types.place.PlaceConstants;
import vstore.framework.context.types.place.PlaceType;
import vstore.framework.utils.NumberUtils;
import vstore.framework.utils.TextUtils;

/**
 * This class represents a decision rule for the vStore framework.
 */
public class VStoreRule {

    /**
     * The UUID of this rule.
     */
    private String mUUID;
    /**
     * The name of this rule.
     */
    private String mName;
    /**
     * The creation date of this rule.
     */
    private Date mDateCreation;
    /**
     * The context belonging to this rule.
     */
    private RuleContextDescription mContext;
    /**
     * A list of mime types this rule should be applied to.
     */
    private List<String> mMimeTypes;
    /**
     * The file size in bytes that a file must have for this rule to be taken into account.
     */
    private long mMinFileSize;
    /**
     * The sharing domain (public or private, or both).
     * 1 = private, 0 = public, -1 = match both.
     */
    private int mSharingDomain;
    /**
     * A flag that determines, if the rule was defined by a user.
     */
    private boolean mIsUserRule;
    /**
     * A list of weekdays this rule should be active at.
     */
    private List<Integer> mWeekdays;
    /**
     * The hour of day this rule should start at (0-24).
     */
    private int mStartHour;
    /**
     * The minutes of the hour of day this rule should start at (0-60).
     */
    private int mStartMinutes;
    /**
     * The hour of day this rule should end at (0-24).
     */
    private int mEndHour;
    /**
     * The minutes of the hour of day this rule should end at (0-60).
     */
    private int mEndMinutes;
    /**
     * The node type that will be used if this rule is fulfilled.
     */
    private List<DecisionLayer> mDecisionLayers = new ArrayList<>();

    /**
     * The detail score of the rule.
     */
    private float mDetailScore;

    /**
     * Initializes a new empty vStore decision rule
     */
    public VStoreRule() {
        setUUID(UUID.randomUUID().toString());
        setName("");
        setCreationDate(new Date());
        setRuleContext(new RuleContextDescription());
        setDecisionLayers(new ArrayList<DecisionLayer>());
        setMimeTypes(new ArrayList<String>());
        setMinFileSize(0);
        setSharingDomain(-1);
        setIsUserRule(true);
        setWeekdays(new ArrayList<Integer>());
        setTimeStart(0, 0);
        setTimeEnd(0, 0);
        mDetailScore = calculateDetailScore();
    }

    /**
     * Constructs a new vStoreRule from the given json string
     * @param ruleJson The json string representing a rule.
     */
    @SuppressWarnings("unchecked")
	public VStoreRule(String ruleJson) {
        //First, fill the rule with default values
        this();
        //Try to parse json
        if(ruleJson == null) 
        	throw new RuntimeException("Given json string must not be null.");
            
    	JSONParser jP = new JSONParser();
        JSONObject j;
		try 
		{
			j = (JSONObject) jP.parse(ruleJson);
		
			if(!j.containsKey("uuid")) throw new RuntimeException("Rule has no id.");
	        
	        setUUID((String)j.get("uuid"));
	        setName((String)j.getOrDefault("name", "<noname>"));
	        setCreationDate(new Date((long)j.getOrDefault("dateCreation", new Date()) * 1000));
	   
	        JSONArray decisions = (JSONArray)j.getOrDefault("decisions", new JSONArray());
	        for (int i = 0; i < decisions.size(); ++i) 
	        {
	            JSONObject decision = (JSONObject)decisions.get(i);
	            DecisionLayer layer = new DecisionLayer(decision.toString());
	            addDecisionLayer(layer);
	        }
	        
	        setRuleContext(new RuleContextDescription((String)j.getOrDefault("context", "")));
	        
	       
	        JSONArray mimeJson = (JSONArray)j.getOrDefault("mimetypes", new JSONArray());
	        for(int i = 0; i<mimeJson.size(); ++i) 
	        {
	            addMimeType((String)mimeJson.get(i));
	        }
	
	        setMinFileSize((long)j.getOrDefault("filesize", 0));
	        setSharingDomain((int)j.getOrDefault("sharingDomain", 1));
	        setIsUserRule((boolean)j.getOrDefault("isUserRule", false));
	            
	        String[] weekdays = ((String)j.getOrDefault("weekdays", "")).split(",");
	        for (String weekday : weekdays) 
	        {
	            mWeekdays.add(Integer.parseInt(weekday));
	        }
	        
	        int h, min;
	       
	        //Read start time and set it
	        String[] timeStart = ((String)j.getOrDefault("timeStart", "00:00")).split(":");
	        if (timeStart.length == 2) 
	        {
	            h = Integer.parseInt(timeStart[0]);
	            min = Integer.parseInt(timeStart[1]);
	        } 
	        else 
	        {
	            h = 0;
	            min = 0;
	        }
	        setTimeStart(h, min);
	        
	        //Read end time and set it
	        String[] timeEnd = ((String)j.getOrDefault("timeEnd", "00:00")).split(":");
	        if (timeEnd.length == 2) 
	        {
	            h = Integer.parseInt(timeEnd[0]);
	            min = Integer.parseInt(timeEnd[1]);
	        } 
	        else 
	        {
	            h = 0;
	            min = 0;
	        }
	        setTimeEnd(h, min);
                
		} 
		catch (ParseException e) 
		{
			e.printStackTrace();
		}
    }

    /**
     * Constructs a new vStoreRule object with the given parameters.
     * @param uuid The UUID of the rule.
     * @param name The name of the rule.
     * @param creationDate The creation date of this rule.
     * @param jsonContext The json representation of the context for this rule that has to be fulfilled
     * such that this rule triggers.
     * @param sharingDomain 1 = private, 0 = public, -1 = match both. If none of these values is given,
     *                      the default value of -1 will be used.
     * @param isUserRule Set this to true, if this rule was created by a user.
     * @param weekdays A list of weekdays this rule should be active at (from 1=Monday to 7=Sunday).
     * @param startHour The hour of the time of day at which this rule should start to be active.
     * @param startMinutes The minutes of the hour of the time of day at which this rule should start to be active.
     * @param endHour The hour of the time of day at which this rule should end (stop to be active).
     * @param endMinutes The minutes of the hour of the time of day at which this rule should end (stop to be active).
     */
    public VStoreRule(String uuid, String name, Date creationDate, String jsonContext,
                        int sharingDomain, boolean isUserRule, List<Integer> weekdays,
                        int startHour, int startMinutes, int endHour, int endMinutes) {
        setUUID(uuid);
        setName(name);
        setCreationDate(creationDate);
        setDecisionLayers(new ArrayList<DecisionLayer>());
        try {
			setRuleContext(new RuleContextDescription(jsonContext));
		} catch (ParseException e) {
			e.printStackTrace();
		}
        setMimeTypes(new ArrayList<String>());
        setMinFileSize(0);
        setSharingDomain(sharingDomain);
        setIsUserRule(isUserRule);
        if(weekdays != null) {
            setWeekdays(weekdays);
        } else {
            setWeekdays(new ArrayList<Integer>());
        }
        setTimeStart(startHour, startMinutes);
        setTimeEnd(endHour, endMinutes);
        mDetailScore = calculateDetailScore();
    }

    /**
     * @return The UUID of this rule.
     */
    public String getUUID() {
        return mUUID;
    }

    /**
     * @return The name of this rule.
     */
    public String getName() {
        return mName;
    }

    /**
     * @return The creation date object of this rule.
     */
    public Date getCreationDate() {
        return mDateCreation;
    }

    /**
     * @return The unix timestamp of the creation date of this rule.
     */
    public long getCreationDateUnix() {
        return mDateCreation.getTime()/1000;
    }

    /**
     * @return The configured decision layers that are used for storage decision
     */
    public List<DecisionLayer> getDecisionLayers() {
        return mDecisionLayers;
    }

    /**
     * @param i The index of the desired decision layer.
     * @return Null, if index is out of bounds. A DecisionLayer object otherwise.
     */
    public DecisionLayer getDecisionLayer(int i) {
        if(i < mDecisionLayers.size()) {
            return mDecisionLayers.get(i);
        }
        return null;
    }

    /**
     * @return The rule context description for this rule.
     */
    public RuleContextDescription getRuleContext() {
        return mContext;
    }

    /**
     * @return A list of mime types that trigger this rule.
     */
    public List<String> getMimeTypes() {
        return mMimeTypes;
    }

    /**
     * @return The minimum size of a file in bytes so that this rule is taken into account.
     */
    public long getMinFileSize() { return mMinFileSize; }

    /**
     * Returns the sharing domain this rule should match.
     * @return Returns one of the following values:
     *  1 = private,
     *  0 = public,
     * -1 = match both.
     */
    public int getSharingDomain() {
        return mSharingDomain;
    }

    /**
     * @return True, if this rule was defined by the user.
     */
    public boolean isUserRule() {
        return mIsUserRule;
    }

    /**
     * @return A list of days of the week where this rule should be active.
     * 1 = monday, 7 = sunday.
     */
    public List<Integer> getWeekdays() { return mWeekdays; }

    /**
     * @return The hour of the time of day at which this rule will start to be active. (0-24)
     */
    public int getStartHour() {
        return mStartHour;
    }
    /**
     * @return The minutes of the time of day at which this rule will start to be active. (0-60)
     */
    public int getStartMinutes() {
        return mStartMinutes;
    }
    /**
     * @return The hour of the time of day at which this rule will stop to be active. (0-24)
     */
    public int getEndHour() {
        return mEndHour;
    }
    /**
     * @return The minutes of the time of day at which this rule will stop to be active. (0-60)
     */
    public int getEndMinutes() { return mEndMinutes; }

    /**
     * Sets the id for this rule.
     * @param uuid The uuid for the rule
     */
    public void setUUID(String uuid) {
        mUUID = uuid;
    }

    /**
     * Sets the name for this rule.
     * @param name The name
     */
    public void setName(String name) {
        mName = name;
    }

    /**
     * Sets the creation date for this rule.
     * @param date The creation date
     */
    public void setCreationDate(Date date) {
        mDateCreation = date;
    }

    /**
     * Sets the decision layers for this rule.
     * @param layers The list of layers which are tried to save a file if this rule is fulfilled.
     */
    public void setDecisionLayers(List<DecisionLayer> layers) {
        mDecisionLayers.clear();
        mDecisionLayers.addAll(layers);
    }

    /**
     * Adds a decision layer to the rule.
     * @param layer A decision layer
     */
    public void addDecisionLayer(DecisionLayer layer) {
        mDecisionLayers.add(layer);
    }

    /**
     * Sets the context for this rule.
     * @param context The context (see {@link RuleContextDescription}).
     */
    public void setRuleContext(RuleContextDescription context) {
        mContext = context;
    }

    /**
     * Sets the mime types that should trigger this rule. Only those types that are supported
     * will be added.
     * @param mimetypes A list of mimetypes to add to this rule.
     */
    public void setMimeTypes(List<String> mimetypes) {
        if(mimetypes != null) {
            if(mMimeTypes == null) {
                mMimeTypes = new ArrayList<>();
            }
            for (String m : mimetypes) {
                addMimeType(m);
            }
        }
    }

    /**
     * Adds the given mimetype to this rule.
     * It will only be added if it is not already contained in this rule.
     * @param mimetype The mimetype to add, e.g. "image/jpeg".
     */
    public void addMimeType(String mimetype) {
        if(mMimeTypes == null) {
            mMimeTypes = new ArrayList<>();
        }
        mMimeTypes.add(mimetype);
    }

    /**
     * Remove the given mimetype from this rule.
     * @param mimetype The mime type to remove.
     */
    public void removeMimeType(String mimetype) {
        mMimeTypes.remove(mimetype);
    }

    /**
     * Sets the minimum file size that a file must have so that the rule is taken
     * into account.
     * @param filesize The minimum file size in bytes.
     */
    public void setMinFileSize(long filesize) { mMinFileSize = filesize; }

    /**
     * Sets the identifier of the sharing domain this rule will match.
     * Must be one of the following:
     *  1 = private,
     *  0 = public,
     * -1 = match both.
     * Will use -1 if an invalid option is specified.
     */
    public void setSharingDomain(int sharingDomain) {
        if(sharingDomain == 1 || sharingDomain == 0 || sharingDomain == -1) {
            mSharingDomain = sharingDomain;
            return;
        }
        mSharingDomain = -1;
    }

    /**
     * Sets the property of this rule if it is a user-created rule or a framework-defined rule.
     * @param isUserRule True if this rule was created by a user. False otherwise.
     */
    public void setIsUserRule(boolean isUserRule) {
        mIsUserRule = isUserRule;
    }

    /**
     * Sets the weekdays this rule should be active at.
     * @param weekdays The list of weekdays to assign to this rule.
     */
    public void setWeekdays(List <Integer> weekdays) {
        mWeekdays = weekdays;
    }

    /**
     * Sets the time at which this rule should be active.
     * Invalid numbers will be set to 0.
     * @param hours The hours of the start time (0-24)
     * @param minutes The minutes of the start time (0-60)
     */
    public void setTimeStart(int hours, int minutes) {
        if(hours < 0 || hours > 24) hours = 0;
        if(minutes < 0 || minutes > 60) minutes = 0;
        mStartHour = hours;
        mStartMinutes = minutes;
    }

    /**
     * Sets the time at which this rule should stop to be active.
     * Invalid numbers will be set to 0.
     * @param hours The hours of the end time (0-24)
     * @param minutes The minutes of the end time (0-60)
     */
    public void setTimeEnd(int hours, int minutes) {
        if(hours < 0 || hours > 24) hours = 0;
        if(minutes < 0 || minutes > 60) minutes = 0;
        mEndHour = hours;
        mEndMinutes = minutes;
    }

    /**
     * @return This rule as a JSON object.
     */
    @SuppressWarnings("unchecked")
	public JSONObject getJson() {
        JSONObject ruleJson = new JSONObject();
        
        ruleJson.put("uuid", getUUID());
        ruleJson.put("name", getName());
        ruleJson.put("dateCreation", getCreationDateUnix());
        ruleJson.put("filesize", getMinFileSize());
        JSONArray decisions = new JSONArray();
        for(DecisionLayer l : mDecisionLayers) 
        {
            decisions.add(l.toJsonObject());
        }
        ruleJson.put("decisions", decisions);
        if(getRuleContext() != null) 
        {
            ruleJson.put("context", getRuleContext().getJson().toString());
        }
        ruleJson.put("mimetypes", getMimeTypesJsonString());
        ruleJson.put("sharingDomain", getSharingDomain());
        ruleJson.put("isUserRule", isUserRule());
        ruleJson.put("weekdays", TextUtils.join(",", mWeekdays));
        ruleJson.put("timeStart", getStartHour()+":"+getStartMinutes());
        ruleJson.put("timeEnd", getEndHour()+":"+getEndMinutes());
        
        return ruleJson;
    }

    /**
     * @return The mimetypes of this rule as json string
     */
    @SuppressWarnings("unchecked")
	public String getMimeTypesJsonString() {
        JSONArray mimetypesJson = new JSONArray();
        if (getMimeTypes() != null) 
        {
            int i = 0;
            for (String s : getMimeTypes()) 
            {
                mimetypesJson.add(i, s);
                i++;
            }
        }
        
        return mimetypesJson.toString();
    }

    /**
     * @return True, if this rule has location context configured.
     */
    public boolean hasLocationContext() {
        if(mContext != null) {
            return mContext.hasLocationContext();
        }
        return false;
    }

    /**
     * @return True, if this rule has place type context configured.
     */
    public boolean hasPlaceContext() {
        if((mContext != null) && (mContext.getPlaceTypes() != null)
                && (mContext.getPlaceTypes().size() > 0)) {
            return mContext.hasPlaceContext();
        }
        return false;
    }

    /**
     * @return True, if this rule has activity context configured.
     */
    public boolean hasActivityContext() {
        if(mContext != null) {
            return mContext.hasActivityContext();
        }
        return false;
    }

    /**
     * @return True, if this rule has network context configured.
     */
    public boolean hasNetworkContext() {
        if(mContext != null) {
            return mContext.hasNetworkContext();
        }
        return false;
    }

    /**
     * @return True, if this rule has noise context configured.
     */
    public boolean hasNoiseContext() {
        if(mContext != null) {
            return mContext.hasNoiseContext();
        }
        return false;
    }

    /**
     * @return True, if this rule has a context set.
     */
    public boolean hasContext() {
        if(mContext != null && !mContext.getJson().toString().equals("{}")) {
            return true;
        }
        return false;
    }

    /**
     * @return True, if the rule has a minimum file size configured, that has to be fulfilled
     * for the rule to be taken into account.
     */
    public boolean hasFileSizeConfigured() {
        return mMinFileSize > 0;
    }

    /**
     * Returns true if this rule has a timespan configured.
     * @return True if a timespan is configured, false otherwise.
     */
    public boolean hasTimeSet() {
        if(mStartHour == 0 && mStartMinutes == 0 && mEndHour == 0 && mEndMinutes == 0
                || mStartHour == -1 && mStartMinutes == -1 && mEndHour == -1 && mEndMinutes == -1) {
            return true;
        }
        return false;
    }

    /**
     * Clears the location context from this rule.
     */
    public void clearLocationContext() {
        if(mContext != null) {
            mContext.clearLocationContext();
        }
    }

    /**
     * Clears the place context from this rule.
     */
    public void clearPlaceContext() {
        if(mContext != null) {
            mContext.clearPlaceContext();
        }
    }

    /**
     * Clears the activity context from this rule.
     */
    public void clearActivityContext() {
        if(mContext != null) {
            mContext.clearActivityContext();
        }
    }

    /**
     * Clears the network context from this rule.
     */
    public void clearNetworkContext() {
        if(mContext != null) {
            mContext.clearNetworkContext();
        }
    }

    /**
     * Clears the noise context from this rule.
     */
    public void clearNoiseContext() {
        if(mContext != null) {
            mContext.clearNoiseContext();
        }
    }

    /**
     * Determines a score for this rule based on how much information is configured.
     * Location: 0-20%
     *  --> always 5% if loc. context is configured (regardless of detail)
     *  --> Maximum score: radius of 1m.
     *  --> Minimum score: radius of 1km or larger.
     *
     * Weekdays: 0-15%
     *  --> always 5% if weekday ctx is configured (regardless of detail)
     *  --> Maximum score: Only 1 day configured.
     *  --> Minimum score: All 7 days configured.
     *
     * Timespan: 0-10%
     *  --> Maximum score: Timespan of 1 minute
     *  --> Minimum score: Timespan of 23 hours, 59 minutes
     *
     * Sharing domain: 0-10%
     *  --> Maximum score: One domain configured (e.g. private XOR public)
     *  --> Minimum score: Both domains configured
     *
     * Places: 0-15%
     *  --> always 5% if any number of places is configured
     *  --> Maximum score: Only one place configured
     *  --> Minimum score: All place types configured
     *
     * Activity: 0-10%
     *  --> Maximum score: Activity configured
     *  --> Minimum score: No activity configured.
     * Network: 0-10%
     * Noise: 0-10%
     *
     * @return The score between 0 and 100. 100 means, the rule has 100% detailed
     * information configured.
     */
    public float calculateDetailScore() {
        if(getName().equals("General Large Video Rule")) {
            int x = 10;
            if(x==10) {}
        }
        int maxRadius = 1000;
        float score = 0;
        RuleContextDescription ctx = getRuleContext();
        //Compute location score
        if(hasLocationContext()) {
            score += 5;
            int radius = ctx.getRadius();
            if(radius > 1000) radius = maxRadius;
            score += NumberUtils.mapToRange(radius, 1, maxRadius, 15, 0);
        }
        //Compute weekday score
        if(getWeekdays().size() > 0) {
            score += 5;
            score += NumberUtils.mapToRange(getWeekdays().size(), 1, 7, 10, 0);
            //Compute timespan score
            int diff = (getEndHour() * 60 + getEndMinutes()) - (getStartHour() * 60 + getStartMinutes());
            if(diff < 0) diff = 0;
            int maxSpanLength = 23*60 + 59;
            score += NumberUtils.mapToRange(diff, 1, maxSpanLength, 10, 0);
        }
        //Compute sharing domain score
        if(getSharingDomain() == 1 || getSharingDomain() == 0) {
            score += 10;
        }
        //Compute places score
        if(hasPlaceContext()) {
            score += 5;
            List<PlaceType> pTypes = ctx.getPlaceTypes();
            int maxPlaces = PlaceConstants.getPossiblePlaceTypes().size();
            score += NumberUtils.mapToRange(pTypes.size(), 1, maxPlaces, 10, 0);
        }
        //Compute activity score
        if(hasActivityContext()) {
            score += 10;
        }
        //Compute network score
        if(hasNetworkContext()) {
            score += 10;
        }
        //Compute Noise context
        if(hasNoiseContext()) {
            score += 10;
        }
        mDetailScore = score;
        return score;
    }

    /**
     * @return The score between 0 and 100. 100 means, the rule has 100% detailed
     * information configured.
     */
    public float getDetailScore() {
        return mDetailScore;
    }

    public void refreshDetailScore() {
        mDetailScore = calculateDetailScore();
    }
}
