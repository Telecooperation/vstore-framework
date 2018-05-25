package vstoreframework.db.row_wrapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import vstoreframework.context.types.location.VLatLng;
import vstoreframework.db.DBSchema;
import vstoreframework.node.NodeInfo;
import vstoreframework.node.NodeType;

/**
 * A wrapper class for the decision rules database cursor.
 */
public class NodeRowWrapper {
    
	private ResultSet mRes;
	
    public NodeRowWrapper(ResultSet res) {
        mRes = res;
    }

    /**
     * Returns a vStoreRule object for the current cursor position.
     * @return The vStoreRule object describing a decision rule stored in the framework.
     */
    public NodeInfo getNode() {
    	
    	NodeInfo node = null;
    	
    	try 
    	{
	        String id = mRes.getString(DBSchema.NodesTable.UUID);
	        String address = mRes.getString(DBSchema.NodesTable.ADDRESS);
	        int port = mRes.getInt(DBSchema.NodesTable.PORT);
	        double latitude = mRes.getDouble(DBSchema.NodesTable.LATITUDE);
	        double longitude = mRes.getDouble(DBSchema.NodesTable.LONGITUDE);
	        String typeStr = mRes.getString(DBSchema.NodesTable.TYPE);
	        int bandwidth_up = mRes.getInt(DBSchema.NodesTable.BANDWIDTH_UP);
	        int bandwidth_down = mRes.getInt(DBSchema.NodesTable.BANDWIDTH_DOWN);
	
	        NodeType type;
	        try {
	            type = NodeType.valueOf(typeStr);
	        } catch(IllegalArgumentException e) {
	            type = NodeType.UNKNOWN;
	        }
	
	        node = new NodeInfo(id,
	                address,
	                port,
	                type,
	                new VLatLng(latitude, longitude));
	        node.setBandwidthUp(bandwidth_up);
	        node.setBandwidthDown(bandwidth_down);
    	}
    	catch (SQLException e) 
    	{
			// TODO: handle exception
    		e.printStackTrace();
		}
    	return node;
    }
}
