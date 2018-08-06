package vstore.framework.rule;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import vstore.framework.node.NodeType;
import vstore.framework.utils.JsonUtils;

/**
 * A decision layer of a rule.
 * A decision layer can either be configured to store on a specific node,
 * or it can be configured to store on a node that matches certain criteria.
 */
public class DecisionLayer {

    /**
     * Determines, if specific node is configured for this layer or criteria should be used.
     */
    public boolean isSpecific;
    /**
     * The id of the specific node.
     */
    public String specificNodeId;
    /**
     * The type of node of this layer.
     */
    public NodeType targetType;
    /**
     * The lower bound for the radius to search within (only applies if isSpecific is false)
     */
    public float minRadius;
    /**
     * The upper bound for the radius to search within (only applies if isSpecific is false)
     */
    public float maxRadius;
    /**
     * The minimum upstream bandwidth a node must match (only applies if isSpecific is false)
     */
    public int minBwUp;
    /**
     * The minimum downstream bandwidth a node must match (only applies if isSpecific is false)
     */
    public int minBwDown;

    /**
     * Constructs a new DecisionLayer object from the given json string
     * @param json The json string
     */
    @SuppressWarnings("unchecked")
	public DecisionLayer(String json) {
        try 
        {
        	JSONParser jP = new JSONParser();
            JSONObject j = (JSONObject) jP.parse(json);
        
            parseJson(j);
            
        } 
        catch(ParseException e) 
        {
            initEmpty();
        }
    }

    /**
     * Constructs a new DecisionLayer object from the given json
     * @param json The json object
     */
    public DecisionLayer(JSONObject json) {
        if(json == null) {
            initEmpty();
        }
        parseJson(json);
    }

    public DecisionLayer() {

    }

    private void parseJson(JSONObject j) {
        if(j != null) {
            isSpecific = JsonUtils.getBoolFromJson("isSpecific", j, false);
            specificNodeId = JsonUtils.getStringFromJson("specificNodeId", j, "");
            targetType = NodeType.valueOf(JsonUtils.getStringFromJson("targetType", j, "UNKNOWN"));
            minRadius = JsonUtils.getFloatFromJson("minRadius", j, 0);
            maxRadius = JsonUtils.getFloatFromJson("maxRadius", j, 0);
            minBwUp = JsonUtils.getIntFromJson("minBwUp", j, 0);
            minBwDown = JsonUtils.getIntFromJson("minBwDown", j, 0);
        } else {
            initEmpty();
        }
    }

    private void initEmpty() {
        isSpecific = false;
        specificNodeId = "";
        targetType = NodeType.UNKNOWN;
        minRadius = 0;
        maxRadius = 0;
        minBwUp = 0;
        minBwDown = 0;
    }

    /**
     * @return This object in JSON format.
     */
    @SuppressWarnings("unchecked")
	public JSONObject toJsonObject() {
        JSONObject j = new JSONObject();
        j.put("isSpecific", isSpecific);
        j.put("specificNodeId", specificNodeId);
        j.put("targetType", targetType.name());
        j.put("minRadius", minRadius);
        j.put("maxRadius", maxRadius);
        j.put("minBwUp", minBwUp);
        j.put("minBwDown", minBwDown);

        return j;
    }
    
    public String toJsonString() {
    	return toJsonObject().toString();
    }
}
