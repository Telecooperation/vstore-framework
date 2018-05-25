package vstoreframework.db.table_helper;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import vstoreframework.db.DBHelper;
import vstoreframework.db.DBSchema;
import vstoreframework.db.DBSchema.DecisionsPerRuleTable;
import vstoreframework.db.DBSchema.MimesPerRuleTable;
import vstoreframework.db.row_wrapper.RulesRowWrapper;
import vstoreframework.exceptions.DatabaseException;
import vstoreframework.node.NodeType;
import vstoreframework.rule.DecisionLayer;
import vstoreframework.rule.VStoreRule;
import vstoreframework.utils.TextUtils;

/**
 * Helper for handling database requests for decision rules.
 * Inside it also handles the join with the decision layer and mimetype tables.
 */
public class RulesDBHelper {
    public static final String SORT_BY_DATE_DESCENDING = DBSchema.RulesTable.DATE_CREATION + " DESC";
    public static final String SORT_BY_DATE_ASCENDING = DBSchema.RulesTable.DATE_CREATION + " ASC";
    public static final String SORT_BY_NAME_DESCENDING = DBSchema.RulesTable.NAME + " DESC";

    private DBHelper mDbHelper;
    
    /**
     * Creates a new RulesDBHelper instance.
     */
    public RulesDBHelper() throws DatabaseException {
    	mDbHelper = DBHelper.getInstance();
    }

    /**
     * Inserts the given decision rule into the local sqlite database. Will simply update
     * an entry, if the given id is already in the database.
     * 
     * @param rule The rule.
     * @throws SQLException 
     */
    public void insertRule(VStoreRule rule) throws SQLException {
    	if(getRule(rule.getUUID()) == null) 
    	{
	    	String sql = "INSERT INTO " 
	        		+ DBSchema.RulesTable.__NAME + " " 
	        		+ getRuleInsertClause(rule);
	    	
	    	Statement stmt = mDbHelper.getConnection().createStatement();
		    stmt.executeQuery(sql);
		    stmt.close();
    	
            //Save updated mime type information for the rule.
            insertRuleMimeTypes(rule);
            //Save updated decision layers for the rule.
            insertDecisionLayers(rule);
            mDbHelper.closeDatabase();
        } 
    	else 
    	{
            updateRule(rule);
        }
    }

    /**
     * Updates the decision rule entry in the database to the state of the rule given as parameter.
     * @param rule The rule object containing the updated information to be saved to the
     *             internal database.
     * @throws SQLException 
     */
    public void updateRule(VStoreRule rule) throws SQLException {
    	String sql = "UPDATE " + DBSchema.RulesTable.__NAME + " "
       		   + getSetClause(rule)
       		   + " WHERE " + DBSchema.RulesTable.ID + " = ? ";
    	
    	PreparedStatement pstmt = mDbHelper.getConnection().prepareStatement(sql); 
        pstmt.setString(1, rule.getUUID());
        pstmt.executeUpdate();
    	pstmt.close();
        
        //Save updated mime type information for the rule.
        insertRuleMimeTypes(rule);
        //Save updated decision layers for the rule.
        insertDecisionLayers(rule);

    }

    /**
     * Reads a decision rule from the local sqlite database.
     * @param uuid The UUID of the rule to read from the database.
     * @return A {@link VStoreRule} object.
     * @throws SQLException 
     */
    public VStoreRule getRule(String uuid) throws SQLException {
    	String sql = "SELECT * FROM "
    			+ DBSchema.RulesTable.__NAME
    			+ " WHERE " + DBSchema.RulesTable.ID + " = ? ";
    	
    	PreparedStatement pstmt = mDbHelper.getConnection().prepareStatement(sql); 
        pstmt.setString(1, uuid);
        ResultSet rs  = pstmt.executeQuery();
        
        VStoreRule rule = null;
        while (rs.next()) 
    	{
        	RulesRowWrapper wrp = new RulesRowWrapper(rs);
        	rule = wrp.getRule();
        	break;
    	}
        pstmt.close();
    	
        rule.setMimeTypes(getMimetypesForRule(uuid));
        rule.setDecisionLayers(getDecisionLayersForRule(uuid));
        
        return rule;
    }

    /**
     * Returns a list of the decision rules currently in the local database.
     * 
     * @param ordering The ordering of the rule list.
     * @return A list containing the {@link VStoreRule} decision rules.
     * @throws SQLException 
     */
    public List<VStoreRule> getRules(String ordering) throws SQLException {
    	String sql = "SELECT * FROM "
    			+ DBSchema.RulesTable.__NAME
    			+ "ORDER BY " + ordering;
    	
    	Statement stmt = mDbHelper.getConnection().createStatement();
    	ResultSet rs = stmt.executeQuery(sql);
        stmt.close();
        
        List<VStoreRule> rules = new ArrayList<>();
        while(rs.next())
        {
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
     * @throws SQLException 
     */
    private List<String> getMimetypesForRule(String ruleId) throws SQLException {
    	String sql = "SELECT * FROM "
    			+ DBSchema.MimesPerRuleTable.__NAME
    			+ "WHERE " + DBSchema.MimesPerRuleTable.RULE_ID + " = ?";
    	
    	PreparedStatement pstmt = mDbHelper.getConnection().prepareStatement(sql); 
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
     * @throws SQLException 
     */
    private List<DecisionLayer> getDecisionLayersForRule(String ruleId) throws SQLException {
    	String sql = "SELECT * FROM "
    			+ DBSchema.MimesPerRuleTable.__NAME
    			+ "WHERE " + DBSchema.DecisionsPerRuleTable.RULE_ID + " = ?";
    	
    	PreparedStatement pstmt = mDbHelper.getConnection().prepareStatement(sql); 
        pstmt.setString(1, ruleId);
        ResultSet rs  = pstmt.executeQuery();
    	
        List<DecisionLayer> layers = new ArrayList<>();

        while(rs.next())
        {
            DecisionLayer d = new DecisionLayer();
            d.isSpecific = (rs.getInt(DBSchema.DecisionsPerRuleTable.IS_SPECIFIC) == 1);
            d.specificNodeId = rs.getString(DBSchema.DecisionsPerRuleTable.SPECIFIC_NODE_ID);
            try 
            {
                d.selectedType = NodeType.valueOf(DBSchema.DecisionsPerRuleTable.SELECTED_TYPE);
            } 
            catch(IllegalArgumentException e) 
            { 
            	d.selectedType = NodeType.UNKNOWN; 
        	}
            d.minRadius = rs.getFloat(DBSchema.DecisionsPerRuleTable.MIN_RADIUS);
            d.maxRadius = rs.getFloat(DBSchema.DecisionsPerRuleTable.MAX_RADIUS);
            d.minBwUp = rs.getInt(DBSchema.DecisionsPerRuleTable.MIN_BW_UP);
            d.minBwDown = rs.getInt(DBSchema.DecisionsPerRuleTable.MIN_BW_DOWN);
            layers.add(d);
        }
        pstmt.close();
        return layers;
    }

    /**
     * Gets all rules from the database that have the given mimetype configured.
     * 
     * @param mimetype The mimetype to fetch rules for.
     * @return A list of all matched rules.
     * @throws SQLException 
     */
    public List<VStoreRule> getRulesMatchingFileType(String mimetype) throws SQLException {
        List<VStoreRule> results = new ArrayList<>();
        if(mimetype == null || mimetype.equals("")) return results;
        
        String sql = "SELECT " + DBSchema.RulesTable.__NAME + "." + DBSchema.RulesTable.ID + ", " +
                DBSchema.RulesTable.NAME + ", " +
                DBSchema.RulesTable.DATE_CREATION + ", " +
                DBSchema.RulesTable.FILE_SIZE + ", " +
                DBSchema.RulesTable.CONTEXTJSON + ", " +
                DBSchema.RulesTable.SHARING_DOMAIN + ", " +
                DBSchema.RulesTable.IS_USER_RULE + ", " +
                DBSchema.RulesTable.WEEKDAYS + ", " +
                DBSchema.RulesTable.TIME_START + ", " +
                DBSchema.RulesTable.TIME_END //+ ", " +
                //DBSchema.RulesTable.NODE_BANDWIDTH_DOWN + ", " +
                //DBSchema.RulesTable.NODE_BANDWIDTH_UP + ", " +
                //DBSchema.RulesTable.MAX_UPLOAD_DURATION
                + " FROM " + DBSchema.RulesTable.__NAME + ", " + DBSchema.MimesPerRuleTable.__NAME
                + " WHERE " +
                DBSchema.MimesPerRuleTable.MIME + " = ? " +
                " AND " +
                DBSchema.MimesPerRuleTable.RULE_ID + " = " +
                DBSchema.RulesTable.__NAME + "." + DBSchema.RulesTable.ID;
        
        PreparedStatement pstmt = mDbHelper.getConnection().prepareStatement(sql); 
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
     * @throws SQLException 
     */
    public void deleteRule(String ruleUUID) throws SQLException {
    	//First, delete the rule itself from the database
    	String sql = "DELETE FROM "
    			+ DBSchema.RulesTable.__NAME
    			+ "WHERE " + DBSchema.RulesTable.ID + " = ?";
    	
        PreparedStatement pstmt = mDbHelper.getConnection().prepareStatement(sql); 
        pstmt.setString(1, ruleUUID);
        pstmt.executeQuery();
        
        //Then, delete all mimetypes for this rule from the database
        deleteMimetypes(ruleUUID);
        
        pstmt.close();
    }
    
    /**
     * Deletes all mimetypes that correspond to the rule with the given id.
     * 
     * @param ruleId The id of the rule of which you want to delete the mimetypes.
     * @throws SQLException 
     */
    public void deleteMimetypes(String ruleId) throws SQLException {
    	String sql = "DELETE FROM "
    			+ DBSchema.MimesPerRuleTable.__NAME
    			+ "WHERE " + MimesPerRuleTable.RULE_ID + " = ?";
        
    	PreparedStatement pstmt = mDbHelper.getConnection().prepareStatement(sql);
        pstmt.setString(1, ruleId);
        pstmt.executeQuery();
    }
    
    /**
     * Deletes all decision layers that correspond to the rule with the given id.
     * 
     * @param ruleId The id of the rule of which you want to delete the decision layers.
     * @throws SQLException 
     */
    public void deleteDecisionLayers(String ruleId) throws SQLException {
    	String sql = "DELETE FROM "
    			+ DBSchema.DecisionsPerRuleTable.__NAME
    			+ "WHERE " + DecisionsPerRuleTable.RULE_ID + " = ?";
        
    	PreparedStatement pstmt = mDbHelper.getConnection().prepareStatement(sql);
        pstmt.setString(1, ruleId);
        pstmt.executeQuery();
    }

    /**
     * Inserts all mime types for the rule into the database.
     * 
     * @param rule The rule for which the mimetypes should be added.
     * @param db A reference to the database that has already been opened.
     * @throws SQLException 
     */
    private void insertRuleMimeTypes(VStoreRule rule) throws SQLException {
        //First, delete all old mime type information from the table.
    	deleteMimetypes(rule.getUUID());

        //Then, save all mime types for this rule in the database
        for(String mime : rule.getMimeTypes()) 
        {
        	String sql = "INSERT INTO " + DBSchema.MimesPerRuleTable.__NAME + 
        			"("
        			+ DBSchema.MimesPerRuleTable.RULE_ID + ", "
        			+ DBSchema.MimesPerRuleTable.MIME + ") "
        			+ "VALUES("
        			+ mime + ", " + rule.getUUID() + ");";
        	
        	Statement stmt = mDbHelper.getConnection().createStatement();
        	stmt.executeQuery(sql);
            stmt.close();
        }
    }

    /**
     * Inserts all decision layers for the rule into the database.
     * 
     * @param rule The rule for which to insert the decision layers.
     * @param db A reference to the database that has already been opened.
     * @throws SQLException 
     */
    private void insertDecisionLayers(VStoreRule rule) throws SQLException {
        //First, delete all old decision layers for this rule from the table.
        deleteDecisionLayers(rule.getUUID());
        
        //Then, save all decision layers for this rule in the database
        for(int i = 0; i < rule.getDecisionLayers().size(); ++i) 
        {
            DecisionLayer layer = rule.getDecisionLayer(i);
            
            String sql = "INSERT INTO " 
            		+ DBSchema.MimesPerRuleTable.__NAME + " " 
            		+ getDecisionLayerInsertClause(layer, rule.getUUID(), i);
            Statement stmt = mDbHelper.getConnection().createStatement();
        	stmt.executeQuery(sql);
            stmt.close();
        }
    }
    
    public String getSetClause(VStoreRule rule) {
        String set = "SET "
        + DBSchema.RulesTable.ID + " = " + rule.getUUID()
        + DBSchema.RulesTable.NAME + " = " + rule.getName()
        + DBSchema.RulesTable.DATE_CREATION + " = " + rule.getCreationDateUnix()
        + DBSchema.RulesTable.CONTEXTJSON + " = " + rule.getRuleContext().getJson().toJSONString()
        + DBSchema.RulesTable.SHARING_DOMAIN + " = " + rule.getSharingDomain()
        + DBSchema.RulesTable.IS_USER_RULE + " = " + rule.isUserRule()
        + DBSchema.RulesTable.WEEKDAYS + " = " + rule.getWeekdays()
        + DBSchema.RulesTable.TIME_START + " = " + rule.getStartHour()+":"+rule.getStartMinutes()
        + DBSchema.RulesTable.TIME_END + " = " + rule.getEndHour()+":"+rule.getEndMinutes()
        + DBSchema.RulesTable.FILE_SIZE + " = " + rule.getMinFileSize();
        return set;
    }

    public String getRuleInsertClause(VStoreRule rule) {
        String insert = "("
		+ DBSchema.RulesTable.ID + ", "
        + DBSchema.RulesTable.NAME + ", "
        + DBSchema.RulesTable.DATE_CREATION + ", " 
        + DBSchema.RulesTable.CONTEXTJSON + ", "
        + DBSchema.RulesTable.SHARING_DOMAIN + ", " 
        + DBSchema.RulesTable.IS_USER_RULE + ", " 
        + DBSchema.RulesTable.WEEKDAYS + ", "
        + DBSchema.RulesTable.TIME_START + ", "
        + DBSchema.RulesTable.TIME_END + ", "
        + DBSchema.RulesTable.FILE_SIZE + ") " 
        + "VALUES ("
        + rule.getUUID() + ", "
        + rule.getName() + ", "
        + rule.getCreationDateUnix() + ", "
        + rule.getRuleContext().getJson().toJSONString() + ", "
		+ rule.getSharingDomain() + ", "
		+ rule.isUserRule() + ", "
		+ TextUtils.join(",", rule.getWeekdays()) + ", "
		+ rule.getStartHour()+":"+rule.getStartMinutes() + ", "
		+ rule.getEndHour()+":"+rule.getEndMinutes() + ", "
		+ rule.getMinFileSize() + ");";
        
        return insert;
    }
    
    public String getDecisionLayerInsertClause(DecisionLayer layer, String ruleId, int position) {
    	String insert = "("
			+ DBSchema.DecisionsPerRuleTable.RULE_ID + ", "
	        + DBSchema.DecisionsPerRuleTable.POSITION + ", "
	        + DBSchema.DecisionsPerRuleTable.IS_SPECIFIC + ", " 
	        + DBSchema.DecisionsPerRuleTable.SPECIFIC_NODE_ID + ", "
	        + DBSchema.DecisionsPerRuleTable.SELECTED_TYPE + ", " 
	        + DBSchema.DecisionsPerRuleTable.MIN_RADIUS + ", " 
	        + DBSchema.DecisionsPerRuleTable.MAX_RADIUS + ", "
	        + DBSchema.DecisionsPerRuleTable.MIN_BW_UP + ", "
	        + DBSchema.DecisionsPerRuleTable.MIN_BW_DOWN + ") " 
	        + "VALUES ("
	        + ruleId + ", "
	        + position + ", "
	        + layer.isSpecific + ", "
	        + layer.specificNodeId + ", "
			+ layer.selectedType + ", "
			+ layer.minRadius + ", "
			+ layer.maxRadius + ", "
			+ layer.minBwUp + ", "
			+ layer.minBwDown + ");";
        
        return insert;
    }
    
}
