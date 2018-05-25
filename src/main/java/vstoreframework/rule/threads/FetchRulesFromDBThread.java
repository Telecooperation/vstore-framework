package vstoreframework.rule.threads;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.greenrobot.eventbus.EventBus;

import vstoreframework.db.table_helper.RulesDBHelper;
import vstoreframework.exceptions.DatabaseException;
import vstoreframework.rule.VStoreRule;
import vstoreframework.rule.events.RulesReadyEvent;

/**
 * This thread is responsible for asynchronously fetching rules from the database.
 * If doing this in the main thread, things will take too long and app is not smooth anymore.
 */
public class FetchRulesFromDBThread extends Thread {
    
	public String mOrdering;
	
	public FetchRulesFromDBThread(String resultOrdering) {
        mOrdering = resultOrdering;
    }
	
	@Override
	public void run() {
		List<VStoreRule> rules = new ArrayList<VStoreRule>();
		try 
		{
			RulesDBHelper helper = new RulesDBHelper();
	        rules = helper.getRules(mOrdering);
		} 
		catch (DatabaseException | SQLException e) 
		{
			e.printStackTrace();
			return;
		}
		
        EventBus.getDefault().postSticky(new RulesReadyEvent(rules));
	}
	

}
