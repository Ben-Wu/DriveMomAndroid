package ca.benwu.drivingevaluatorandroid.utils;

import android.content.Context;
import android.util.Log;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by Ben Wu on 2016-10-08.
 */

public class NetworkHelper {

    private static final String TAG = NetworkHelper.class.getSimpleName();

    private static OkHttpClient client = null;

    private static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");

    private static final String BASE_URL = "http://10.0.2.2";
    private static final int PORT = 3000;

    private static OkHttpClient getInstance() {
        if(client == null) {
            client = new OkHttpClient();
        }
        return client;
    }

    public static String post(String endpoint, String json) throws IOException {
        Log.d(TAG, "POST: " + endpoint);
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(BASE_URL + ":" + PORT + endpoint)
                .post(body)
                .build();
        Response response = getInstance().newCall(request).execute();
        return response.body().string();
    }

}
