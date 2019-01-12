package vstore.framework.db.table_helper;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import vstore.framework.db.DBHelper;
import vstore.framework.db.DBSchema;
import vstore.framework.db.DBSchema.DecisionsPerRuleTable;
import vstore.framework.db.DBSchema.MimesPerRuleTable;
import vstore.framework.db.row_wrapper.RulesRowWrapper;
import vstore.framework.node.NodeType;
import vstore.framework.rule.DecisionLayer;
import vstore.framework.rule.VStoreRule;
import vstore.framework.utils.TextUtils;

/**
 * Helper for handling database requests for decision rules.
 * Inside it also handles the join with the decision layer and mimetype tables.
 */
public class RulesDBHelper {
    public static final String SORT_BY_DATE_DESCENDING = DBSchema.RulesTable.DATE_CREATION + " DESC";
    public static final String SORT_BY_DATE_ASCENDING = DBSchema.RulesTable.DATE_CREATION + " ASC";
    public static final String SORT_BY_NAME_DESCENDING = DBSchema.RulesTable.NAME + " DESC";
    
    /**
     * Creates a new RulesDBHelper instance.
     */
    private RulesDBHelper() { }

    /**
     * Inserts the given decision rule into the local sqlite database. Will simply update
     * an entry, if the given id is already in the database.
     * 
     * @param rule The rule.
     * @throws SQLException in case of a database error.
     */
    public static void insertRule(VStoreRule rule) throws SQLException {
    	if(getRule(rule.getUUID()) != null) { updateRule(rule); return; }

        String sql = "INSERT INTO "
                + DBSchema.RulesTable.__NAME + " "
                + "("
            + getFieldList(false)
            + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?);";

        PreparedStatement pstmt = DBHelper.get().getConnection().prepareStatement(sql);
        pstmt.setString(1, rule.getUUID());
        pstmt.setString(2, rule.getName());
        pstmt.setLong(3, rule.getCreationDateUnix());
        pstmt.setLong(4, rule.getMinFileSize());
        pstmt.setString(5, rule.getRuleContext().getJson().toJSONString());
        pstmt.setInt(6, rule.getSharingDomain());
        pstmt.setString(7, TextUtils.join(",", rule.getWeekdays()));
        pstmt.setString(8, rule.getStartHour()+":"+rule.getStartMinutes());
        pstmt.setString(9, rule.getEndHour()+":"+rule.getEndMinutes());
        pstmt.setBoolean(10, rule.isGlobal());
        pstmt.setBoolean(11, rule.isStoreMultiple());
        pstmt.setInt(12, rule.getReplicationFactor());
        pstmt.setFloat(13, rule.getDetailScore());

        pstmt.execute();

        //Save updated mime type information for the rule.
        insertRuleMimeTypes(rule);
        //Save updated decision layers for the rule.
        insertDecisionLayers(rule);
    }

    /**
     * Updates the decision rule entry in the database to the state of the rule given as parameter.
     * @param rule The rule object containing the updated information to be saved to the
     *             internal database.
     * @throws SQLException in case of a database error.
     */
    public static void updateRule(VStoreRule rule) throws SQLException {
        String sql = "UPDATE " + DBSchema.RulesTable.__NAME + " "
       		   + "SET "
                + getFieldList(true)
       		    + " WHERE " + DBSchema.RulesTable.ID + " = ?";
    	PreparedStatement pstmt = DBHelper.get().getConnection().prepareStatement(sql);
        pstmt.setString(1, rule.getUUID());
        pstmt.setString(2, rule.getName());
        pstmt.setLong(3, rule.getCreationDateUnix());
        pstmt.setLong(4, rule.getMinFileSize());
        pstmt.setString(5, rule.getRuleContext().getJson().toJSONString());
        pstmt.setInt(6, rule.getSharingDomain());
        pstmt.setString(7, TextUtils.join(",", rule.getWeekdays()));
        pstmt.setString(8, rule.getStartHour()+":"+rule.getStartMinutes());
        pstmt.setString(9, rule.getEndHour()+":"+rule.getEndMinutes());
        pstmt.setBoolean(10, rule.isGlobal());
        pstmt.setInt(11, rule.getReplicationFactor());
        pstmt.setBoolean(12, rule.isStoreMultiple());
        pstmt.setFloat(13, rule.getDetailScore());
        pstmt.setString(14, rule.getUUID());

        pstmt.executeUpdate();

        //Save updated mime type information for the rule.
        insertRuleMimeTypes(rule);
        //Save updated decision layers for the rule.
        insertDecisionLayers(rule);

    }

    /**
     * Reads a decision rule from the local sqlite database.
     * @param uuid The UUID of the rule to read from the database.
     * @return A {@link VStoreRule} object.
     * @throws SQLException in case of a database error.
     */
    public static VStoreRule getRule(String uuid) throws SQLException {
    	String sql = "SELECT * FROM "
    			+ DBSchema.RulesTable.__NAME
    			+ " WHERE " + DBSchema.RulesTable.ID + " = ? ";
    	
    	PreparedStatement pstmt = DBHelper.get().getConnection().prepareStatement(sql);
        pstmt.setString(1, uuid);
        ResultSet rs  = pstmt.executeQuery();

        if (!rs.next()) {return null; }

        RulesRowWrapper wrp = new RulesRowWrapper(rs);
        VStoreRule rule = wrp.getRule();
    	
        rule.setMimeTypes(getMimetypesForRule(uuid));
        rule.setDecisionLayers(getDecisionLayersForRule(uuid));
        
        return rule;
    }

    /**
     * Returns a list of the decision rules currently in the local database.
     * 
     * @param ordering The ordering of the rule list.
     *                 Should be either {@link RulesDBHelper#SORT_BY_DATE_DESCENDING}, or
     *                 {@link RulesDBHelper#SORT_BY_DATE_ASCENDING} or
     *                 {@link RulesDBHelper#SORT_BY_NAME_DESCENDING}.
     * @return A list containing the {@link VStoreRule} decision rules.
     * @throws SQLException in case of a database error.
     */
    public static List<VStoreRule> getRules(String ordering) throws SQLException {
        if(ordering == null) {
            ordering = SORT_BY_DATE_DESCENDING;
        }
    	String sql = "SELECT * FROM "
    			+ DBSchema.RulesTable.__NAME
    			+ " ORDER BY " + ordering;
    	
    	Statement stmt = DBHelper.get().getConnection().createStatement();
    	ResultSet rs = stmt.executeQuery(sql);
        
        List<VStoreRule> rules = new ArrayList<>();
        while (rs.next()) {
            RulesRowWrapper wrp = new RulesRowWrapper(rs);
            rules.add(wrp.getRule());

        }
        
        //For each rule, get the corresponding mime types from the second table.
        //A Join operation in SQLite would not do what we want because we need them as a
        //separate list.
        for(VStoreRule r : rules) 
        {
            //Get corresponding mime types for the rule
            r.setMimeTypes(getMimetypesForRule(r.getUUID()));
            //Get corresponding decision layers for the rule
            r.setDecisionLayers(getDecisionLayersForRule(r.getUUID()));
        }
        return rules;
    }

    /**
     * Returns a list of mime types that correspond to this rule from the database.
     * @param ruleId The rule id for which to get the mime types.
     * @return The list with the mime type strings.
     * @throws SQLException in case of a database error.
     */
    private static List<String> getMimetypesForRule(String ruleId) throws SQLException {
    	String sql = "SELECT * FROM "
    			+ DBSchema.MimesPerRuleTable.__NAME
    			+ " WHERE " + DBSchema.MimesPerRuleTable.RULE_ID + " = ?";
    	
    	PreparedStatement pstmt = DBHelper.get().getConnection().prepareStatement(sql);
        pstmt.setString(1, ruleId);
        ResultSet rs  = pstmt.executeQuery();
    	
        List<String> mimes = new ArrayList<>();
        while(rs.next()) 
        {
        	mimes.add(rs.getString(DBSchema.MimesPerRuleTable.MIME));
        }
        
        return mimes;
    }

    /**
     * Returns a list of decision layers that correspond to this rule from the database.
     * @param ruleId The rule id for which to get decision layers.
     * @return The list with the decision layers.
     * @throws SQLException in case of a database error.
     */
    private static List<DecisionLayer> getDecisionLayersForRule(String ruleId) throws SQLException {
    	String sql = "SELECT * FROM "
    			+ DBSchema.DecisionsPerRuleTable.__NAME
    			+ " WHERE " + DBSchema.DecisionsPerRuleTable.RULE_ID + " = ?";
    	
    	PreparedStatement pstmt = DBHelper.get().getConnection().prepareStatement(sql);
        pstmt.setString(1, ruleId);
        ResultSet rs  = pstmt.executeQuery();
    	
        List<DecisionLayer> layers = new ArrayList<>();
        if(rs.next()) {
            do {
                DecisionLayer d = new DecisionLayer();
                d.isSpecific = (rs.getInt(DBSchema.DecisionsPerRuleTable.IS_SPECIFIC) == 1);
                d.specificNodeId = rs.getString(DBSchema.DecisionsPerRuleTable.SPECIFIC_NODE_ID);
                try {
                    d.targetType = NodeType.valueOf(
                            rs.getString(DBSchema.DecisionsPerRuleTable.SELECTED_TYPE));
                } catch (IllegalArgumentException e) {
                    d.targetType = NodeType.UNKNOWN;
                }
                d.minRadius = rs.getFloat(DBSchema.DecisionsPerRuleTable.MIN_RADIUS);
                d.maxRadius = rs.getFloat(DBSchema.DecisionsPerRuleTable.MAX_RADIUS);
                d.minBwUp = rs.getInt(DBSchema.DecisionsPerRuleTable.MIN_BW_UP);
                d.minBwDown = rs.getInt(DBSchema.DecisionsPerRuleTable.MIN_BW_DOWN);
                layers.add(d);
            } while (rs.next());
        }
        return layers;
    }

    /**
     * Gets all rules from the database that have the given mimetype configured.
     * 
     * @param mimetype The mimetype to fetch rules for.
     * @return A list of all matched rules.
     * @throws SQLException in case of a database error.
     */
    public static List<VStoreRule> getRulesMatchingFileType(String mimetype) throws SQLException {
        List<VStoreRule> results = new ArrayList<>();
        if(mimetype == null || mimetype.equals("")) return results;
        
        String sql = "SELECT " + DBSchema.RulesTable.__NAME + "."
                + getFieldList(false)
                + " FROM " + DBSchema.RulesTable.__NAME + ", " + DBSchema.MimesPerRuleTable.__NAME
                + " WHERE " +
                DBSchema.MimesPerRuleTable.MIME + " = ? " +
                " AND " +
                DBSchema.MimesPerRuleTable.RULE_ID + " = " +
                DBSchema.RulesTable.__NAME + "." + DBSchema.RulesTable.ID;
        
        PreparedStatement pstmt = DBHelper.get().getConnection().prepareStatement(sql);
        pstmt.setString(1, mimetype);
        ResultSet rs  = pstmt.executeQuery();
        
        while(rs.next()) 
        {
        	RulesRowWrapper wrp = new RulesRowWrapper(rs);
            VStoreRule r = wrp.getRule();
            //Get corresponding decision layers for the rule
            r.setDecisionLayers(getDecisionLayersForRule(r.getUUID()));
            results.add(r);
        }
        return results;
    }

    /**
     * Deletes the rule with the given UUID from the local database.
     * @param ruleUUID The UUID of the rule to delete.
     * @throws SQLException in case of a database error.
     */
    public static void deleteRule(String ruleUUID) throws SQLException {
    	//First, delete the rule itself from the database
    	String sql = "DELETE FROM "
    			+ DBSchema.RulesTable.__NAME
    			+ " WHERE " + DBSchema.RulesTable.ID + " = ?";
    	
        PreparedStatement pstmt = DBHelper.get().getConnection().prepareStatement(sql);
        pstmt.setString(1, ruleUUID);
        pstmt.execute();
        
        //Then, delete all mimetypes for this rule from the database
        deleteMimetypes(ruleUUID);
        deleteDecisionLayers(ruleUUID);
    }
    
    /**
     * Deletes all mimetypes that correspond to the rule with the given id.
     * 
     * @param ruleId The id of the rule of which you want to delete the mimetypes.
     * @throws SQLException in case of a database error.
     */
    public static void deleteMimetypes(String ruleId) throws SQLException {
    	String sql = "DELETE FROM "
    			+ DBSchema.MimesPerRuleTable.__NAME
    			+ " WHERE " + MimesPerRuleTable.RULE_ID + " = ?";
        
    	PreparedStatement pstmt = DBHelper.get().getConnection().prepareStatement(sql);
        pstmt.setString(1, ruleId);
        pstmt.execute();
    }
    
    /**
     * Deletes all decision layers that correspond to the rule with the given id.
     * 
     * @param ruleId The id of the rule of which you want to delete the decision layers.
     * @throws SQLException in case of a database error.
     */
    public static void deleteDecisionLayers(String ruleId) throws SQLException {
    	String sql = "DELETE FROM "
    			+ DBSchema.DecisionsPerRuleTable.__NAME
    			+ " WHERE " + DecisionsPerRuleTable.RULE_ID + " = ?";
        
    	PreparedStatement pstmt = DBHelper.get().getConnection().prepareStatement(sql);
        pstmt.setString(1, ruleId);
        pstmt.execute();
    }

    /**
     * Inserts all mime types for the rule into the database.
     * 
     * @param rule The rule for which the mimetypes should be added.
     * @throws SQLException in case of a database error.
     */
    private static void insertRuleMimeTypes(VStoreRule rule) throws SQLException {
        //First, delete all old mime type information from the table.
    	deleteMimetypes(rule.getUUID());

        //Then, save all mime types for this rule in the database
        for(String mime : rule.getMimeTypes()) 
        {
        	String sql = "INSERT INTO " + DBSchema.MimesPerRuleTable.__NAME + 
        			"("
        			+ DBSchema.MimesPerRuleTable.RULE_ID + ", "
        			+ DBSchema.MimesPerRuleTable.MIME + ") "
        			+ "VALUES(?, ?);";
        	
        	PreparedStatement pstmt = DBHelper.get().getConnection().prepareStatement(sql);
        	pstmt.setString(1, rule.getUUID());
        	pstmt.setString(2, mime);

        	pstmt.execute();
        }
    }

    /**
     * Inserts all decision layers for the rule into the database.
     * 
     * @param rule The rule for which to insert the decision layers.
     * @throws SQLException in case of a database error.
     */
    private static void insertDecisionLayers(VStoreRule rule) throws SQLException {
        //First, delete all old decision layers for this rule from the table.
        deleteDecisionLayers(rule.getUUID());
        
        //Then, save all decision layers for this rule in the database
        for(int i = 0; i < rule.getDecisionLayers().size(); ++i) 
        {
            DecisionLayer layer = rule.getDecisionLayer(i);

            String sql = "INSERT INTO "
                    + DBSchema.DecisionsPerRuleTable.__NAME + " ("
                    + DecisionsPerRuleTable.RULE_ID + ", "
                    + DecisionsPerRuleTable.POSITION + ", "
                    + DecisionsPerRuleTable.IS_SPECIFIC + ", "
                    + DecisionsPerRuleTable.SPECIFIC_NODE_ID + ", "
                    + DecisionsPerRuleTable.SELECTED_TYPE + ", "
                    + DecisionsPerRuleTable.MIN_RADIUS + ", "
                    + DecisionsPerRuleTable.MAX_RADIUS + ", "
                    + DecisionsPerRuleTable.MIN_BW_UP + ", "
                    + DecisionsPerRuleTable.MIN_BW_DOWN + ") "
                    + "VALUES (?,?,?,?,?,?,?,?,?)";

            PreparedStatement stmt = DBHelper.get().getConnection().prepareStatement(sql);
            stmt.setString(1, rule.getUUID());
            stmt.setInt(2, i);
            stmt.setBoolean(3, layer.isSpecific);
            stmt.setString(4, layer.specificNodeId);
            stmt.setString(5, layer.targetType.name());
            stmt.setFloat(6, layer.minRadius);
            stmt.setFloat(7, layer.maxRadius);
            stmt.setInt(8, layer.minBwUp);
            stmt.setInt(9, layer.minBwDown);
            stmt.execute();
        }
    }

    public static void deleteAllRules() {
        String sql1 = "DELETE * FROM " + DBSchema.RulesTable.NAME;
        String sql2 = "DELETE * FROM " + MimesPerRuleTable.__NAME;
        String sql3 = "DELETE * FROM " + DecisionsPerRuleTable.__NAME;

        try {
            Statement stmt = DBHelper.get().getConnection().createStatement();
            stmt.executeQuery(sql1);
            stmt.executeQuery(sql2);
            stmt.executeQuery(sql3);
        } catch(SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Workaround for simplifying the sql field list
     * @param q True, if a questionmark should be included in the list (for prepared statements)
     * @return True: " = ?, "
     *         False: ", "
     */
    private static String q(boolean q) {
        return ((q) ? " = ?, " : ", ");
    }

    private static String getFieldList(boolean q) {
        String returnStr = DBSchema.RulesTable.ID + q(q)
                + DBSchema.RulesTable.NAME + q(q)
                + DBSchema.RulesTable.DATE_CREATION + q(q)
                + DBSchema.RulesTable.FILE_SIZE + q(q)
                + DBSchema.RulesTable.CONTEXTJSON + q(q)
                + DBSchema.RulesTable.SHARING_DOMAIN + q(q)
                + DBSchema.RulesTable.WEEKDAYS + q(q)
                + DBSchema.RulesTable.TIME_START + q(q)
                + DBSchema.RulesTable.TIME_END + q(q)
                + DBSchema.RulesTable.IS_GLOBAL + q(q)
                + DBSchema.RulesTable.REPLICATION_FACTOR + q(q)
                + DBSchema.RulesTable.STORE_MULTIPLE + q(q);
        returnStr += DBSchema.RulesTable.DETAIL_SCORE + ((q) ? " = ?" : "");
        return returnStr;
    }
}
