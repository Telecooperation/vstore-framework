package vstore.framework.communication.master_node.file_node_mapping;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import vstore.framework.communication.ApiConstants;
import vstore.framework.communication.CommunicationManager;
import vstore.framework.error.ErrorMessages;
import vstore.framework.exceptions.VStoreException;
import vstore.framework.matching.FileNodeMapper;
import vstore.framework.utils.JsonUtils;

import static vstore.framework.error.ErrorCode.MASTERNODE_WRONG_RESPONSE;

/**
 * Fetches the identifier of a storage node for a file id from the master peer.
 */
public class RequestFileNodeMappingCallable implements Callable<List<String>> {

    private String fileId;

    /**
     * @param fileId The file id for which to request the node id.
     */
    public RequestFileNodeMappingCallable(String fileId) {
        this.fileId = fileId;
    }

    /**
     * Will try to fetch the storage node id from the master peer.
     *
     * @return A list of identifiers of the storage nodes where the file is stored.
     */
    @Override
    public List<String> call() {
        Response response = sendRequest();
        if(response != null) {
            return parseResponse(response);
        }
        return new ArrayList<>();
    }

    /**
     * Will try to fetch the storage node id from the master peer.
     *
     * @return The identifier of the storage node where the file is stored.
     */
    private Response sendRequest() {
        //Will timeout after 2 seconds.
        final OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(2, TimeUnit.SECONDS)
                .writeTimeout(2, TimeUnit.SECONDS)
                .readTimeout(2, TimeUnit.SECONDS)
                .build();
        Request request = null;
        try {
            request = new Request.Builder()
                    .url(new URL(CommunicationManager.get().getMasterNodeAddress(),
                            ApiConstants.MasterNode.ROUTE_FILE_NODE_MAPPING + "/" + fileId))
                    .build();
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }

        try {
            return client.newCall(request).execute();
        } catch(IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private List<String> parseResponse(Response response) {
        if(response == null) { return null; }
        try
        {
            JSONParser p = new JSONParser();
            JSONObject jResult = (JSONObject) p.parse(response.body().string());
            if (jResult.containsKey("data"))
            {
                JSONObject dataResult = JsonUtils.getJSONObjectFromJson("data", jResult, null);
                if (dataResult != null && dataResult.containsKey("array"))
                {
                    JSONArray nodeArray = JsonUtils.getJSONArrayFromJson("array", dataResult, null);
                    if (nodeArray == null)
                    {
                        throw new VStoreException(MASTERNODE_WRONG_RESPONSE, ErrorMessages.MASTERPEER_WRONG_REPLY);
                    }
                    if (nodeArray.size() > 0)
                    {
                        ArrayList<String> node_ids = new ArrayList<>();
                        node_ids.addAll(nodeArray);
                        //Add it to the local mapping list
                        FileNodeMapper.getMapper().storeNewMapping(fileId, node_ids);
                        return node_ids;
                    }
                }
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }
}
