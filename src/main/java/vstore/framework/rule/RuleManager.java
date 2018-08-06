package vstore.framework.rule;

import java.sql.SQLException;

import vstore.framework.db.table_helper.RulesDBHelper;
import vstore.framework.error.ErrorMessages;
import vstore.framework.rule.threads.FetchRulesFromDBThread;

public class RuleManager {
    private static RuleManager instance;

    private RuleManager() {}

    public static void initialize() {
        if(instance == null) {
            instance = new RuleManager();
        }
    }

    /**
     * @return Gets the instance of the rule manager.
     */
    public static RuleManager get() {
        initialize();
        return instance;
    }
	
	/**
     * Initializes a job in background to fetch the list of decision rules that are currently
     * saved in the VStore framework. The order is always newest rules first.
     *
     * The following event will notify you when done:
     * - RulesReadyEvent
     */
    public void getRules() {
        FetchRulesFromDBThread t =  new FetchRulesFromDBThread(RulesDBHelper.SORT_BY_DATE_DESCENDING);
        t.start();
    }
    
	/**
     * Stores a newly created rule in the framework. Make sure that the rule has an id.
     * @param rule The VStoreRule object. See {@link VStoreRule}.
     */
    public void storeNewRule(VStoreRule rule) {
        if(rule == null) {
            throw new RuntimeException(ErrorMessages.PARAMETERS_MUST_NOT_BE_NULL);
        }
        try
		{
			RulesDBHelper.insertRule(rule);
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		}
    }

    /**
     * Deletes the rule with the given ID from the local database.
     *
     * @param ruleID The identifier of the rule to delete.
     */
    public void deleteRule(String ruleID) {
        if(ruleID == null)
        {
            throw new RuntimeException(ErrorMessages.PARAMETERS_MUST_NOT_BE_NULL);
        }
        try 
        {
	        RulesDBHelper.deleteRule(ruleID);
        }
        catch(SQLException e)
        {
        	e.printStackTrace();
        }
    }

    /**
     * Updates the decision rule entry to the state of the rule given as parameter.
     * @param rule The rule object containing the updated information.
     */
    public void updateRule(VStoreRule rule) {
        try {
            RulesDBHelper.updateRule(rule);
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Deletes all rules currently stored in the framework.
     *
     * @return Returns the RuleManager for method chaining.
     */
    public RuleManager clearRules() {
        RulesDBHelper.deleteAllRules();
        return this;
    }
    
    
}
