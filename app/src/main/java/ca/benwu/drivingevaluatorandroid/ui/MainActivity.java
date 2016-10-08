package ca.benwu.drivingevaluatorandroid.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.openxc.VehicleManager;
import com.openxc.interfaces.network.NetworkVehicleInterface;
import com.openxc.messages.SimpleVehicleMessage;
import com.openxc.messages.VehicleMessage;
import com.openxc.remote.VehicleServiceException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import ca.benwu.drivingevaluatorandroid.R;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private String BASE_URL;
    private final int PORT = 3000;

    private VehicleManager mVehicleManager;
    private TextView textView;

    private JSONArray dataPointsToSend = new JSONArray();

    private int userId = 1;
    private int tripId = 1;

    private boolean inTrip = false;

    private static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");

    private OkHttpClient client = new OkHttpClient();

    private final String PREF_TRIP_ID = "pref_trip_id";
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // grab a reference to the engine speed text object in the UI, so we can
        // manipulate its value later from Java code
        textView = (TextView) findViewById(R.id.text);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        tripId = preferences.getInt(PREF_TRIP_ID, 1);

        BASE_URL = "http://" + getResources().getString(R.string.api_url);
    }

    @Override
    public void onPause() {
        super.onPause();
        // When the activity goes into the background or exits, we want to make
        // sure to unbind from the service to avoid leaking memory
        if(mVehicleManager != null) {
            Log.i(TAG, "Unbinding from Vehicle Manager");
            // Remember to remove your listeners, in typical Android
            // fashion.
            mVehicleManager.removeListener(SimpleVehicleMessage.class, mListener);
            unbindService(mConnection);
            mVehicleManager = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // When the activity starts up or returns from the background,
        // re-connect to the VehicleManager so we can receive updates.
        if(mVehicleManager == null) {
            Intent intent = new Intent(this, VehicleManager.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    private VehicleMessage.Listener mListener = new VehicleMessage.Listener() {
        @Override
        public void receive(final VehicleMessage message) {
            if(!((SimpleVehicleMessage) message).getName().equals("ignition_status")
                    && !((SimpleVehicleMessage) message).getName().equals("accelerator_pedal_position")) {
                return;
            }
            if(((SimpleVehicleMessage) message).getName().equals("ignition_status")) {
                if(((SimpleVehicleMessage) message).getValue().equals("start")) {
                    inTrip = true;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            textView.setText("In Trip");
                        }
                    });
                }
            }
            if(!inTrip) {
                return;
            }
            //Log.i(TAG, message.toString());
            try {
                JSONObject object = new JSONObject();
                object.put("timestamp", message.getTimestamp());
                object.put("name", ((SimpleVehicleMessage) message).getName());
                object.put("value", ((SimpleVehicleMessage) message).getValue());
                dataPointsToSend.put(object);
            } catch (JSONException e) {

            }
            if(dataPointsToSend.length() > 200) {
                sendDataPoints();
            }

            if(((SimpleVehicleMessage) message).getName().equals("ignition_status")) {
                if(((SimpleVehicleMessage) message).getValue().equals("off")) {
                    sendDataPoints();
                    sendTripEnd();
                    inTrip = false;
                    preferences.edit().putInt(PREF_TRIP_ID, ++tripId).commit();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            textView.setText("Trip Ended");
                        }
                    });
                }
            }

        }
    };

    private void sendTripEnd() {
        try {
            JSONObject tripInfo = new JSONObject();
            tripInfo.put("userId", userId);
            tripInfo.put("tripId", tripId);
            String response = post(BASE_URL + ":" + PORT + "/trip/end", tripInfo.toString());
            Log.i(TAG, "Trip end sent: " + response);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendDataPoints() {
        try {
            JSONObject dataSet = new JSONObject();
            dataSet.put("userId", userId);
            dataSet.put("tripId", tripId);
            dataSet.put("data", dataPointsToSend);
            String response = post(BASE_URL + ":" + PORT + "/trip/data", dataSet.toString());
            dataPointsToSend = new JSONArray();
            Log.i(TAG, "Data points sent: " + response);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        // Called when the connection with the VehicleManager service is
        // established, i.e. bound.
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            Log.i(TAG, "Bound to VehicleManager");
            // When the VehicleManager starts up, we store a reference to it
            // here in "mVehicleManager" so we can call functions on it
            // elsewhere in our code.
            mVehicleManager = ((VehicleManager.VehicleBinder) service)
                    .getService();

            try {
                mVehicleManager.setVehicleInterface(NetworkVehicleInterface.class, BASE_URL + ":50001");
            } catch (VehicleServiceException e) {
                Log.e(TAG, "Unable to add network vehicle interface");
            }

            mVehicleManager.addListener(SimpleVehicleMessage.class, mListener);
        }

        // Called when the connection with the service disconnects unexpectedly
        public void onServiceDisconnected(ComponentName className) {
            Log.w(TAG, "VehicleManager Service  disconnected unexpectedly");
            mVehicleManager = null;
        }
    };

    private String post(String url, String json) throws IOException {
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        Response response = client.newCall(request).execute();
        return response.body().string();
    }
}
