package vstore.framework.db;

import vstore.framework.error.ErrorMessages;
import vstore.framework.exceptions.DatabaseException;
import vstore.framework.utils.FileUtils;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Helper class for the internal vStore SQLite database.
 * It is used by the framework to access the database.
 * The database contains uploaded files and user-defined rules.
 */
public class DBHelper {
    private static final String DATABASE_NAME = "vstorage.db";
    private static String path = FileUtils.getVStoreDir();
    private static String db_url = "jdbc:sqlite:" + path + "/" + DATABASE_NAME;
    
    private static Connection dbConn = null;
    private static DBHelper mInstance;

    private DBHelper() throws DatabaseException 
    {
    	new File(path).mkdirs();
        try 
        {
        	Class.forName("org.sqlite.JDBC");
			dbConn = DriverManager.getConnection(db_url);
	    	if(!createTablesIfNotExist())
	    	{
	    		closeDatabase();
	    		throw new DatabaseException(ErrorMessages.DB_CREATE_TABLES_EXCEPTION);
	    	}
		} 
        catch (SQLException | ClassNotFoundException | DatabaseException e) 
        {
			e.printStackTrace();
    		throw new DatabaseException(ErrorMessages.DB_CONNECT_ERROR);
		} 
    }
    
    /**
     * Gets the instance of the DBHelper.
     * 
     * @return The instance of the DBHelper, or null if an error occurred;
     * @throws DatabaseException 
     */
    public static synchronized DBHelper getInstance() throws DatabaseException {
        if(mInstance == null) 
        {
        	mInstance = new DBHelper();
        }
        return mInstance;
    }
    
    /**
     * For the database layout, you can also see {@link DBSchema.java}.
     * @return true, if no error occurred.
     *         false, if an error occurred.
     */
    private static synchronized boolean createTablesIfNotExist() {
    	if(dbConn == null) return false;
    	
    	try(Statement stmt = dbConn.createStatement()) 
    	{
	    	stmt.execute("CREATE TABLE IF NOT EXISTS " + DBSchema.FilesTable.__NAME + "(" +
	                DBSchema.FilesTable.UUID + " TEXT PRIMARY KEY NOT NULL," +
	                DBSchema.FilesTable.MD5_HASH + " TEXT NOT NULL," +
	                DBSchema.FilesTable.DESCRIPTIVE_NAME + " TEXT NOT NULL," +
	                DBSchema.FilesTable.MIME + " TEXT," +
	                DBSchema.FilesTable.EXTENSION + " TEXT," +
	                DBSchema.FilesTable.DATE_CREATION + " INTEGER," +
	                DBSchema.FilesTable.SIZE + " INTEGER, " +
	                DBSchema.FilesTable.UPLOAD_PENDING + " INTEGER, " +
	                DBSchema.FilesTable.UPLOAD_FAILED + " INTEGER, " +
	                DBSchema.FilesTable.PRIVATE + " INTEGER, " +
	                DBSchema.FilesTable.NODEUUID + " TEXT, " +
	                DBSchema.FilesTable.CONTEXTJSON + " TEXT," +
	                DBSchema.FilesTable.DELETE_PENDING + " INTEGER)");
	
	        stmt.execute("CREATE TABLE IF NOT EXISTS " + DBSchema.RulesTable.__NAME + "(" +
	                DBSchema.RulesTable.ID + " TEXT PRIMARY KEY NOT NULL," +
	                DBSchema.RulesTable.NAME + " TEXT NOT NULL," +
	                DBSchema.RulesTable.DATE_CREATION + " INTEGER," +
	                DBSchema.RulesTable.FILE_SIZE + " INTEGER, " +
	                DBSchema.RulesTable.CONTEXTJSON + " TEXT NOT NULL," +
	                DBSchema.RulesTable.SHARING_DOMAIN + " INTEGER NOT NULL," +
	                DBSchema.RulesTable.IS_USER_RULE + " INTEGER," +
	                DBSchema.RulesTable.WEEKDAYS + " TEXT," +
	                DBSchema.RulesTable.TIME_START + " TEXT," +
	                DBSchema.RulesTable.TIME_END + " TEXT)");
	
	        stmt.execute("CREATE TABLE IF NOT EXISTS " + DBSchema.DecisionsPerRuleTable.__NAME + "(" +
	                DBSchema.DecisionsPerRuleTable.ID + " INTEGER PRIMARY KEY NOT NULL," +
	                DBSchema.DecisionsPerRuleTable.RULE_ID + " TEXT NOT NULL," +
	                DBSchema.DecisionsPerRuleTable.POSITION + " INTEGER NOT NULL," +
	                DBSchema.DecisionsPerRuleTable.IS_SPECIFIC + " INTEGER NOT NULL," +
	                DBSchema.DecisionsPerRuleTable.SPECIFIC_NODE_ID + " TEXT," +
	                DBSchema.DecisionsPerRuleTable.SELECTED_TYPE + " TEXT NOT NULL," +
	                DBSchema.DecisionsPerRuleTable.MIN_RADIUS + " REAL," +
	                DBSchema.DecisionsPerRuleTable.MAX_RADIUS + " REAL," +
	                DBSchema.DecisionsPerRuleTable.MIN_BW_UP + " INTEGER," +
	                DBSchema.DecisionsPerRuleTable.MIN_BW_DOWN + " INTEGER)");
	
	        stmt.execute("CREATE TABLE IF NOT EXISTS " + DBSchema.MimesPerRuleTable.__NAME + "(" +
	                DBSchema.MimesPerRuleTable.ID + " INTEGER PRIMARY KEY NOT NULL," +
	                DBSchema.MimesPerRuleTable.RULE_ID + " TEXT NOT NULL," +
	                DBSchema.MimesPerRuleTable.MIME + " TEXT NOT NULL)");
	
	        stmt.execute("CREATE TABLE IF NOT EXISTS " + DBSchema.NodesTable.__NAME + "(" +
	                DBSchema.NodesTable.UUID + " TEXT PRIMARY KEY NOT NULL," +
	                DBSchema.NodesTable.ADDRESS + " TEXT NOT NULL," +
	                DBSchema.NodesTable.PORT + " INTEGER NOT NULL," +
	                DBSchema.NodesTable.LATITUDE + " REAL NOT NULL," +
	                DBSchema.NodesTable.LONGITUDE + " REAL NOT NULL," +
	                DBSchema.NodesTable.TYPE + " TEXT NOT NULL," +
	                DBSchema.NodesTable.BANDWIDTH_UP + " INTEGER," +
	                DBSchema.NodesTable.BANDWIDTH_DOWN + " INTEGER)");
	
	        stmt.execute("CREATE TABLE IF NOT EXISTS " + DBSchema.CurrentDownloads.__NAME + "(" +
	                DBSchema.CurrentDownloads.FILE_UUID + " TEXT PRIMARY KEY NOT NULL)");
    	} 
    	catch (SQLException e) 
    	{
    		System.out.println(e.getMessage());
    		return false;
    	}
    	return true;
    }
    
    public synchronized final Connection getConnection() {
    	return dbConn;
    }

    /**
     * Close the database connection.
     */
    public synchronized void closeDatabase() {
        if(dbConn != null) 
        {
            try 
            {
				dbConn.close();
			} 
            catch (SQLException e) 
            {
				System.out.println(e.getMessage());
			}
        }
    }
}