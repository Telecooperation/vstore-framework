package vstore.framework.node;

/**
 * Models the reply of a storage node which is sent in response to an id request.
 */
public class NodeReply {

    private String nodeId;
    private NodeType nodeType;

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public void setNodeType(NodeType nodeType) {
        this.nodeType = nodeType;
    }

    public String getNodeId() {
        return nodeId;
    }

    public NodeType getNodeType() {
        return nodeType;
    }
}
