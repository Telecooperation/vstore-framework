package vstore.framework.communication.master_node;

import org.greenrobot.eventbus.EventBus;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

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
import vstore.framework.config.ConfigParser;
import vstore.framework.config.events.ConfigDownloadFailedEvent;
import vstore.framework.config.events.ConfigDownloadSucceededEvent;
import vstore.framework.error.ErrorCode;
import vstore.framework.error.ErrorMessages;
import vstore.framework.utils.IdentifierUtils;

/**
 * This callable downloads the configuration file from the given url.
 */
public class ConfigurationDownloadCallable implements Callable<Void> {
    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");

    boolean postEvents = true;

    final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .writeTimeout(2, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .cache(null)
            .build();

    public ConfigurationDownloadCallable(boolean postEvents) {
        this.postEvents = postEvents;
    }

    @Override
    public Void call() {
        Request request = buildRequest();
        if(request == null) { return null; }
        doDownload(request);
        return null;
    }

    private Request buildRequest() {
        JSONObject json = new JSONObject();
        json.put("device_id", IdentifierUtils.getDeviceIdentifier());
        RequestBody body = RequestBody.create(JSON, json.toJSONString());

        //Build the request
        Request request = null;
        try {
            request = new Request.Builder()
                    .url(new URL(CommunicationManager.get().getMasterNodeAddress(),
                            ApiConstants.MasterNode.ROUTE_VSTORE_CONFIG_URL))
                    .post(body)
                    .build();
        }
        catch (MalformedURLException e)
        {
            e.printStackTrace();
            return null;
        }

        return request;
    }

    private void doDownload(Request request) {
        try (Response response = client.newCall(request).execute())
        {
            if (!response.isSuccessful()) {
                postFailedEvent(ErrorCode.CONFIG_DOWNLOAD_FAILED,
                        ErrorMessages.RESPONSE_WRONG_STATUS_CODE + response);
                return;
            }

            try
            {
                ConfigParser.parseFullConfig(response.body().string());
                if(postEvents) {
                    ConfigDownloadSucceededEvent evt = new ConfigDownloadSucceededEvent();
                    EventBus.getDefault().post(evt);
                }
            }
            catch (NullPointerException | ParseException e)
            {
                //JSON parsing failed. This is bad.
                e.printStackTrace();
                postFailedEvent(ErrorCode.CONFIG_PARSE_ERROR, ErrorMessages.JSON_PARSING_FAILED);
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
            postFailedEvent(ErrorCode.CONFIG_CONNECTION_FAILED,
                    ErrorMessages.REQUEST_FAILED);
        }
    }

    private void postFailedEvent(ErrorCode errCode, String msg) {
        if(!postEvents) { return; }
        ConfigDownloadFailedEvent evt = new ConfigDownloadFailedEvent();
        evt.errorCode = errCode;
        evt.errorMsg = msg;
        EventBus.getDefault().post(evt);
    }
}
