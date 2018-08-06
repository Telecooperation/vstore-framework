package vstore.framework.communication.threads;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import vstore.framework.node.NodeInfo;
import vstore.framework.node.NodeReply;
import vstore.framework.node.NodeType;

/**
 * This callable is responsible for fetching the identifier of a storage node, if it is not known yet.
 */
public class FetchNodeIdentifierCallable implements Callable<NodeReply> {

    NodeInfo nodeInfo;

    /**
     * @param nodeInfo The NodeInfo object of the node to contact
     */
    public FetchNodeIdentifierCallable(NodeInfo nodeInfo) {
        this.nodeInfo = nodeInfo;
    }

    @Override
    public NodeReply call() throws Exception {
        return contactNode();
    }

    /**
     * Will try to contact the node with the given uri.
     * If a node does not reply, it will not be included in the list.
     *
     * @return The UUID of the node in case of success. Null otherwise.
     */
    private NodeReply contactNode() {
        //Will timeout after 2 seconds.
        final OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(2, TimeUnit.SECONDS)
                .writeTimeout(2, TimeUnit.SECONDS)
                .readTimeout(2, TimeUnit.SECONDS)
                .build();
        Request request = new Request.Builder()
                .url(nodeInfo.getUUIDUri())
                .build();

        try (Response response = client.newCall(request).execute())
        {
            if(response != null)
            {
                JSONParser p = new JSONParser();
                JSONObject jResult = (JSONObject) p.parse(response.body().string());
                if (jResult.containsKey("uuid"))
                {
                    NodeReply reply = new NodeReply();
                    //Read unique identifier of node from the reply
                    reply.setNodeId((String)jResult.get("uuid"));
                    //Read the type of the node from the reply
                    if(jResult.containsKey("type"))
                    {
                        String nodeType = (String) jResult.get("type");
                        reply.setNodeType((nodeType == null) ? NodeType.UNKNOWN : NodeType.valueOf(nodeType));
                    }
                    //Read the UUID the node sent
                    return reply;
                }
            }
            return null;
        }
        catch (IOException | ParseException e)
        {
            e.printStackTrace();
            return null;
        }
    }
}
