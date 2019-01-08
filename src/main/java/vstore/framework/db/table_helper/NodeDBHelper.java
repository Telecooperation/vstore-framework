package vstore.framework.db.table_helper;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import vstore.framework.db.DBHelper;
import vstore.framework.db.DBSchema;
import vstore.framework.db.row_wrapper.NodeRowWrapper;
import vstore.framework.node.NodeInfo;

/**
 * Helper for handling database requests for storage node settings.
 */
public class NodeDBHelper {
    private NodeDBHelper() {}

    /**
     * Inserts the given node information into the local sqlite database.
     * @param node The node information.
     * @throws SQLException in case something went wrong during the query.
     */
    public static void insertNode(NodeInfo node) throws SQLException {
        String sql = "INSERT INTO "
        		+ DBSchema.NodesTable.__NAME + " " 
        		+ "("
                + DBSchema.NodesTable.UUID + ", "
                + DBSchema.NodesTable.ADDRESS + ", "
                + DBSchema.NodesTable.PORT + ", "
                + DBSchema.NodesTable.LATITUDE + ", "
                + DBSchema.NodesTable.LONGITUDE + ", "
                + DBSchema.NodesTable.TYPE + ", "
                + DBSchema.NodesTable.BANDWIDTH_UP + ", "
                + DBSchema.NodesTable.BANDWIDTH_DOWN + ") "
                + "VALUES ("
                + "'" + node.getIdentifier() + "', "
                + "'" + node.getAddress() + "', "
                + "'" + node.getPort() + "', "
                + "'" + node.getLatLng().getLatitude() + "', "
                + "'" + node.getLatLng().getLongitude() + "', "
                + "'" + node.getNodeType().toString() + "', "
                + "'" + node.getBandwidthUp() + "', "
                + "'" + node.getBandwidthDown() + "')";
        try(Statement stmt = DBHelper.get().getConnection().createStatement()) {
            boolean result = stmt.execute(sql);
            stmt.close();
        }
}

    /**
     * Deletes the node with the given UUID from the local sqlite database.
     * 
     * @param uuid The UUID of the node to delete.
     * @throws SQLException in case something went wrong during the query.
     */
    public static void deleteNode(String uuid) throws SQLException {
        if(uuid == null || uuid.equals("")) return;
        
        String sql = "DELETE FROM "
        		+ DBSchema.NodesTable.__NAME
        		+ " WHERE " + DBSchema.NodesTable.UUID + " = ?";

        try(PreparedStatement pstmt = DBHelper.get().getConnection().prepareStatement(sql)) {
            pstmt.setString(1, uuid);
            pstmt.execute();
            pstmt.close();
        }
    }

    /**
     * Reads the node configuration information from the local sqlite database for a given id.
     * 
     * @param uuid The UUID of the node to read from the database.
     * @return The NodeInfo object describing the node of the given UUID.
     * @throws SQLException in case something went wrong during the query.
     */
    public static NodeInfo getNode(String uuid) throws SQLException {
    	String sql = "SELECT * FROM "
    			+ DBSchema.NodesTable.__NAME
    			+ " WHERE " + DBSchema.NodesTable.UUID + " = ? ";
    	
    	NodeInfo n = null;
    	try(PreparedStatement pstmt = DBHelper.get().getConnection().prepareStatement(sql)) {

            pstmt.setString(1, uuid);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                NodeRowWrapper wrp = new NodeRowWrapper(rs);
                n = wrp.getNode();
                break;
            }
            pstmt.close();
        }

        return n;
    }

    /**
     * Updates the node entry in the database to the configuration of the node given as parameter.
     * @param node The node object containing the updated information to be saved to the
     *             internal database.
     * @throws SQLException in case something went wrong during the query.
     */
    public static void updateNode(NodeInfo node) throws SQLException {
    	String sql = "UPDATE " + DBSchema.NodesTable.__NAME + " "
      		   + getSetClause(node)
      		   + " WHERE " + DBSchema.NodesTable.UUID + " = ? ";
    	
    	//Save updated node information
    	try(PreparedStatement pstmt = DBHelper.get().getConnection().prepareStatement(sql)) {
            pstmt.setString(1, node.getIdentifier());
            pstmt.executeUpdate();
        }
    }

    /**
     * Returns a list of the storage nodes currently stored in the local database.
     *
     * @return A list containing the {@link NodeInfo} information.
     * @throws SQLException in case something went wrong during the query.
     */
    public static List<NodeInfo> getNodes() throws SQLException {
    	String sql = "SELECT * FROM " + DBSchema.NodesTable.__NAME;
        List<NodeInfo> nodes = new ArrayList<>();
    	
    	try(PreparedStatement pstmt = DBHelper.get().getConnection().prepareStatement(sql)) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                NodeRowWrapper wrp = new NodeRowWrapper(rs);
                nodes.add(wrp.getNode());
            }
            pstmt.close();
        }
        return nodes;
    }

    /**
     * Deletes all storage nodes from the database.
	 *
     * @throws SQLException in case something went wrong during the query.
     */
    public static void deleteAllNodes() throws SQLException {
    	String sql = "DELETE FROM " + DBSchema.NodesTable.__NAME;
    	try(PreparedStatement pstmt = DBHelper.get().getConnection().prepareStatement(sql)) {
            pstmt.execute();
        }
    }

    public static String getSetClause(NodeInfo n) {
        String set = "SET "
        + DBSchema.NodesTable.UUID + " = '" + n.getIdentifier() + "', "
        + DBSchema.NodesTable.ADDRESS + " = '" + n.getAddress() + "', "
        + DBSchema.NodesTable.PORT + " = '" + n.getPort() + "', "
        + DBSchema.NodesTable.LATITUDE + " = '" + n.getLatLng().getLatitude() + "', "
        + DBSchema.NodesTable.LONGITUDE + " = '" + n.getLatLng().getLongitude() + "', "
        + DBSchema.NodesTable.TYPE + " = '" + n.getNodeType().name() + "', "
        + DBSchema.NodesTable.BANDWIDTH_UP + " = '" + n.getBandwidthUp() + "', "
        + DBSchema.NodesTable.BANDWIDTH_DOWN + " = '" + n.getBandwidthDown() + "'";
        return set;
    }
}
