package vstore.framework.db.row_wrapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;

import vstore.framework.db.DBSchema;
import vstore.framework.rule.VStoreRule;

/**
 * A wrapper class for the decision rules database cursor.
 */
public class RulesRowWrapper {
    
	private ResultSet mRes;
	
    public RulesRowWrapper(ResultSet res) {
        mRes = res;
    }

    /**
     * Returns a vStoreRule object for the current cursor position.
     * @return The vStoreRule object describing a decision rule stored in the framework.
     * @throws SQLException in case something went wrong during the query.
     */
    public VStoreRule getRule() throws SQLException {
        VStoreRule rule;

        String uuid = mRes.getString(DBSchema.RulesTable.ID);

        String name = mRes.getString(DBSchema.RulesTable.NAME);
        int dateCreation = mRes.getInt(DBSchema.RulesTable.DATE_CREATION);
        long filesize = mRes.getLong(DBSchema.RulesTable.FILE_SIZE);
        String contextString = mRes.getString(DBSchema.RulesTable.CONTEXTJSON);
        int sharingDomain = mRes.getInt(DBSchema.RulesTable.SHARING_DOMAIN);
        String weekdaysStr = mRes.getString(DBSchema.RulesTable.WEEKDAYS);
        String startTimeStr = mRes.getString(DBSchema.RulesTable.TIME_START);
        String endTimeStr = mRes.getString(DBSchema.RulesTable.TIME_END);
        //int nodeBwDown = mRes.getInt(DBSchema.RulesTable.NODE_BANDWIDTH_DOWN);
        //int nodeBwUp = mRes.getInt(DBSchema.RulesTable.NODE_BANDWIDTH_UP);
        //int maxUploadDuration = mRes.getInt(DBSchema.RulesTable.MAX_UPLOAD_DURATION);
        boolean isGlobal = (mRes.getInt(DBSchema.RulesTable.IS_GLOBAL)) == 1;
        boolean isStoreMultiple = (mRes.getInt(DBSchema.RulesTable.STORE_MULTIPLE)) == 1;
        int replicationFactor = mRes.getInt(DBSchema.RulesTable.REPLICATION_FACTOR);
        float detailScore = mRes.getFloat(DBSchema.RulesTable.DETAIL_SCORE);

        ArrayList<Integer> weekdays = new ArrayList<>();
        String[] days = weekdaysStr.split(",");
        for(String day : days)
        {
            try
            {
                weekdays.add(Integer.parseInt(day));
            }
            catch(NumberFormatException e) {}
        }

        int h_start = 0, min_start = 0;
        String[] timeStart = startTimeStr.split(":");
        if(timeStart.length == 2)
        {
            h_start = Integer.parseInt(timeStart[0]);
            min_start = Integer.parseInt(timeStart[1]);
        }
        int h_end = 0, min_end = 0;
        String[] timeEnd = endTimeStr.split(":");
        if(timeEnd.length == 2)
        {
            h_end = Integer.parseInt(timeEnd[0]);
            min_end = Integer.parseInt(timeEnd[1]);
        }

        rule = new VStoreRule(uuid,
                name,
                new Date(dateCreation*1000L),
                contextString,
                sharingDomain,
                weekdays,
                h_start, min_start, h_end, min_end,
                isGlobal,
                isStoreMultiple,
                replicationFactor);

        //rule.setBandwidth(nodeBwDown, nodeBwUp);
        //rule.setMaxUploadDuration(maxUploadDuration);
        rule.setMinFileSize(filesize);
        rule.setDetailScore(detailScore);
        return rule;
    }
}
