package vstoreframework.rule;

import java.sql.SQLException;

import vstoreframework.db.table_helper.RulesDBHelper;
import vstoreframework.error.ErrorMessages;
import vstoreframework.exceptions.DatabaseException;

public class RuleManager {
	
	/**
     * Initializes a job in background to fetch the list of decision rules that are currently
     * saved in the VStore framework. The order is always newest rules first.
     *
     * The following event will notify you when done:
     * - RulesReadyEvent
     */
    public static void getRules() {
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
    public static void storeNewRule(VStoreRule rule) {
        if(rule == null) {
            throw new RuntimeException(ErrorMessages.PARAMETERS_MOST_NOT_BE_NULL);
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
     * @param c The Android context.
     * @param ruleUUID The UUID of the rule to delete.
     */
    public static void deleteRule(String ruleUUID) {
        if(ruleUUID == null) 
        {
            throw new RuntimeException(ErrorMessages.PARAMETERS_MOST_NOT_BE_NULL);
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
    
    
}
