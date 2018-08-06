package vstore.framework.communication.master_node;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import vstore.framework.communication.ApiConstants;
import vstore.framework.communication.CommunicationManager;
import vstore.framework.config.ConfigParser;
import vstore.framework.node.NodeInfo;

/**
 * This callable downloads the complete list of storage nodes from the master node.
 */
public class NodeListDownloadCallable implements Callable<List<NodeInfo>> {
    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");

    final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(2, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .cache(null)
            .build();

    @Override
    public List<NodeInfo> call() {
        Request request = buildRequest();
        if(request == null) { return null; }
        return doDownload(request);
    }

    private Request buildRequest() {
        //Build the request
        try {
            return new Request.Builder()
                    .url(new URL(CommunicationManager.get().getMasterNodeAddress()
                            + ApiConstants.MasterNode.ROUTE_NODES_INFORMATION))
                    .get()
                    .build();
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private List<NodeInfo> doDownload(Request request) {
        try (Response response = client.newCall(request).execute())
        {
            if (!response.isSuccessful() || response.body() == null) {
                return null;
            }
            return ConfigParser.parseNodeListJson(response.body().string());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return null;
    }
}
