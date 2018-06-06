package vstore.framework.rule;

import java.sql.SQLException;

import vstore.framework.db.table_helper.RulesDBHelper;
import vstore.framework.error.ErrorMessages;
import vstore.framework.exceptions.DatabaseException;

public class RuleManager {
    private static RuleManager instance;

    private RuleManager() {}

    public static void initialize() {
        if(instance == null) {
            instance = new RuleManager();
        }
    }

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
        //Get JobManager
        /*VJobManager.setContext(c.getApplicationContext());
        JobManager jobManager = VJobManager.getJobManager();

        if (jobManager != null) {
            jobManager.addJobInBackground(
                    new FetchRulesFromDBJob( RulesDBHelper.SORT_BY_DATE_DESCENDING));
        }*/
    }
    
	/**
     * Stores a newly created rule in the framework. Make sure that the rule has an id.
     * @param rule The VStoreRule object. See {@link VStoreRule}.
     */
    public void storeNewRule(VStoreRule rule) {
        if(rule == null) {
            throw new RuntimeException(ErrorMessages.PARAMETERS_MUST_NOT_BE_NULL);
        }
        RulesDBHelper rulesDBHelper;
		try 
		{
			rulesDBHelper = new RulesDBHelper();
			rulesDBHelper.insertRule(rule);
		} 
		catch (DatabaseException | SQLException e) 
		{
			e.printStackTrace();
		}
    }

    /**
     * Deletes the rule with the given UUID from the local database.
     *
     * @param ruleUUID The UUID of the rule to delete.
     */
    public void deleteRule(String ruleUUID) {
        if(ruleUUID == null) 
        {
            throw new RuntimeException(ErrorMessages.PARAMETERS_MUST_NOT_BE_NULL);
        }
        try 
        {
	        RulesDBHelper rulesDBHelper = new RulesDBHelper();
	        rulesDBHelper.deleteRule(ruleUUID);
        }
        catch(DatabaseException | SQLException e)
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
            RulesDBHelper helper = new RulesDBHelper();
            helper.updateRule(rule);
        }
        catch(DatabaseException | SQLException e)
        {
            e.printStackTrace();
        }
    }
    
    
}
