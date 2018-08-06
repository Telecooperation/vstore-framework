package vstore.framework.config;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import vstore.framework.db.table_helper.RulesDBHelper;
import vstore.framework.matching.Matching;
import vstore.framework.node.NodeInfo;
import vstore.framework.node.NodeManager;
import vstore.framework.rule.VStoreRule;
import vstore.framework.utils.JsonUtils;

public class ConfigParser {

    /**
     * This method should be used for parsing a downloaded configuration file.
     * @param strConfig The string of the configuration file in json notation
     * @throws ParseException in case parsing failed
     */
    public static void parseFullConfig(String strConfig) throws ParseException {
        JSONParser p = new JSONParser();
        JSONObject j = (JSONObject) p.parse(strConfig);

        //Parse the list of nodes into the node manager
        parseNodeList(j);
        parseRules(j);
        parseMatchingMode(j);
    }

    /**
     * Parses the given string as a json string and extracts the node information.
     * Format should be: { nodes: [{node1_object}, {node2_object}], ... }.
     *
     * @param jStr The json string
     * @return A list of the contained node information objects, or null.
     */
    public static List<NodeInfo> parseNodeListJson(String jStr) {
        if(jStr == null || jStr.trim().equals("")) { return null; }
        JSONParser p = new JSONParser();
        JSONObject jNodes;
        try {
            jNodes = (JSONObject) p.parse(jStr);
        } catch(ParseException pe) {
            pe.printStackTrace();
            return null;
        }
        return parseNodeListJson(jNodes);
    }

    /**
     * Parses the given json object and extracts the node information.
     * Format should be: { nodes: [{node1_object}, {node2_object}], ... }.
     *
     * @param j The json object
     * @return A list of the contained node information objects, or null.
     */
    public static List<NodeInfo> parseNodeListJson(JSONObject j) {
        if(!j.containsKey("nodes")) { return null; }
        JSONArray nodesJson = (JSONArray)j.get("nodes");
        ArrayList<NodeInfo> nodes = new ArrayList<>();
        for(int i = 0; i<nodesJson.size(); ++i)
        {
            JSONObject node = (JSONObject)nodesJson.get(i);
            nodes.add(new NodeInfo(node));
        }
        return nodes;
    }

    public static void parseNodeList(JSONObject j) {
        List<NodeInfo> nodes = parseNodeListJson(j);
        if(nodes == null) { return; }
        NodeManager manager = NodeManager.get();
        for(NodeInfo node : nodes)
        {
            manager.addNode(node);
        }
        manager.refreshNodes();
    }

    public static void parseRules(JSONObject j) {
        if(!j.containsKey("rules")) { return; }

        try
        {
            JSONArray rules = (JSONArray)j.get("rules");
            for(int i = 0; i<rules.size(); ++i)
            {
                JSONObject rule = (JSONObject)rules.get(i);
                if(rule.containsKey("delete") && (boolean)rule.get("delete"))
                {
                    //Only delete, if we have a UUID
                    if(rule.containsKey("uuid"))
                    {
                        //Delete-Flag is set, thus remove the rule from the database
                        RulesDBHelper.deleteRule((String)rule.get("uuid"));
                    }
                }
                else
                {
                    //Create a new rule from the json string and put it into
                    //the database. Or update an existing rule with the given ID.
                    try
                    {
                        VStoreRule r = new VStoreRule(rule);
                        RulesDBHelper.insertRule(r);
                    }
                    catch(RuntimeException ex)
                    {
                        //Ignore the rule if the provided data was not sufficient.
                        ex.printStackTrace();
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void parseMatchingMode(JSONObject j) {
        if(!j.containsKey("matchingMode")) {
            //If property is missing from the config file, simply use the default matching
            ConfigManager.get().setMatchingMode(Matching.MatchingMode.RULES_NEXT_ON_NO_MATCH);
            return;
        }

        String matchingMode = JsonUtils.getStringFromJson("matchingMode", j, "");
        try
        {
            ConfigManager.get().setMatchingMode(Matching.MatchingMode.valueOf(matchingMode));
        }
        catch(IllegalArgumentException ex)
        {
            ConfigManager.get().setMatchingMode(Matching.MatchingMode.RULES_NEXT_ON_NO_MATCH);
        }
    }
}
