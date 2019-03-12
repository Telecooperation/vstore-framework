package vstore.framework.communication.master_node.file_node_mapping;

import org.json.simple.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
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

public class DeleteFileNodeMappingCallable implements Callable<Boolean> {

    private static final Logger LOGGER = LogManager.getLogger(DeleteFileNodeMappingCallable.class);

    public static final MediaType JSON_MEDIATYPE = MediaType.parse("application/json; charset=utf-8");

    private String fileId;

    /**
     * Creates a new callable for deleting a file-node-mapping from the master node.
     * @param fileId The file id
     */
    public DeleteFileNodeMappingCallable(String fileId) {
        this.fileId = fileId;
    }

    @Override
    public Boolean call() {
        OkHttpClient httpClient = createClient();
        Request request = buildRequest();
        if(request == null) {
            LOGGER.warn("Request was null");
            return false;
        }
        boolean result = handleResponse(httpClient, request);
        LOGGER.debug("Request returned " + result);
        return result;
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
     * Builds the request to delete a mapping
     * @return The built request.
     */
    private Request buildRequest() {
        JSONObject json = new JSONObject();
        json.put("file_id", fileId);
        json.put("device_id", IdentifierUtils.getDeviceIdentifier());

        RequestBody body = RequestBody.create(JSON_MEDIATYPE, json.toJSONString());
        try {
            return new Request.Builder()
                    .url(new URL(CommunicationManager.get().getMasterNodeAddress(),
                            ApiConstants.MasterNode.ROUTE_FILE_NODE_MAPPING))
                    .delete(body)
                    .build();
        }
        catch (MalformedURLException e) {
            LOGGER.error("Malformed URL");
            e.printStackTrace();
            return null;
        }
    }

    private Boolean handleResponse(OkHttpClient httpClient, Request req) {
        try (Response response = httpClient.newCall(req).execute())
        {
            if(response == null )
            {
                LOGGER.warn("Response is null");
                return false;
            }
            int resonseCode = response.code();
            if (resonseCode != 200) {
                LOGGER.warn("Response code is " + resonseCode);
                return false;
            }

            FileNodeMapper.getMapper().removeMapping(fileId);
            return true;
        }
        catch (IOException e)
        {
            LOGGER.error("IO Exception");
            e.printStackTrace();
        }
        return false;
    }
}
