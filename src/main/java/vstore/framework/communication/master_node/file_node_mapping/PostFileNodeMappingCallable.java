package vstore.framework.communication.master_node.file_node_mapping;

import org.json.simple.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import vstore.framework.communication.ApiConstants;
import vstore.framework.communication.CommunicationManager;
import vstore.framework.matching.FileNodeMapper;
import vstore.framework.utils.IdentifierUtils;

/**
 * Posts a new mapping between file id and node id to the master peer
 */
public class PostFileNodeMappingCallable implements Callable<Boolean>  {
    public static final MediaType JSON_MEDIATYPE
            = MediaType.parse("application/json; charset=utf-8");

    private String fileId;
    private String nodeId;

    /**
     * @param fileId The file id
     * @param nodeId The node id
     */
    public PostFileNodeMappingCallable(String fileId, String nodeId) {
        this.fileId = fileId;
        this.nodeId = nodeId;
    }

    /**
     * Will post the file-node mapping to the master peer.
     *
     * @return True in case of success.
     */
    @Override
    public Boolean call() {
        OkHttpClient httpClient = createClient();
        Request request = buildRequest();
        if(request == null) { return false; }
        return handleResponse(httpClient, request);
    }

    private OkHttpClient createClient() {
        //Will timeout after 2 seconds.
        return new OkHttpClient.Builder()
                .connectTimeout(2, TimeUnit.SECONDS)
                .writeTimeout(2, TimeUnit.SECONDS)
                .readTimeout(2, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Builds the request with a json array
     * @return The built request.
     */
    private Request buildRequest() {
        JSONObject json = new JSONObject();
        json.put("file_id", fileId);
        json.put("node_id", nodeId);
        json.put("device_id", IdentifierUtils.getDeviceIdentifier());

        RequestBody body = RequestBody.create(JSON_MEDIATYPE, json.toJSONString());
        try {
            return new Request.Builder()
                    .url(new URL(CommunicationManager.get().getMasterNodeAddress(),
                            ApiConstants.MasterNode.ROUTE_FILE_NODE_MAPPING))
                    .post(body)
                    .build();
        }
        catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Boolean handleResponse(OkHttpClient httpClient, Request req) {
        try (Response response = httpClient.newCall(req).execute())
        {
            if(response == null || response.code() != 201)
            {
                return false;
            }
            storeLocalMapping();
            return true;
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return false;
    }

    private void storeLocalMapping() {
        List<String> mapping = FileNodeMapper.getMapper().getNodeIds(fileId);
        if(!mapping.contains(nodeId)) {
            mapping.add(nodeId);
        }
        FileNodeMapper.getMapper().storeNewMapping(fileId, mapping);
    }
}
