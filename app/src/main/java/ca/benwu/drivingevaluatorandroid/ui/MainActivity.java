package ca.benwu.drivingevaluatorandroid.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.openxc.VehicleManager;
import com.openxc.interfaces.network.NetworkVehicleInterface;
import com.openxc.messages.SimpleVehicleMessage;
import com.openxc.messages.VehicleMessage;
import com.openxc.remote.VehicleServiceException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import ca.benwu.drivingevaluatorandroid.R;
import ca.benwu.drivingevaluatorandroid.utils.NetworkHelper;
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

    private JSONArray dataPointsToSend = new JSONArray();

    private int userId = 1;
    private int tripId = 1;

    private boolean inTrip = false;

    private static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");

    private OkHttpClient client = new OkHttpClient();

    private final String PREF_TRIP_ID = "pref_trip_id";
    private SharedPreferences preferences;

    @BindView(R.id.inTripBackground)
    ViewFlipper inTripBackground;
    @BindView(R.id.inTripText)
    TextView inTripText;
    @BindView(R.id.durationValue)
    TextView durationText;
    @BindView(R.id.hardAccsValue)
    TextView hardAccsText;
    @BindView(R.id.hardBrakingValue)
    TextView hardBrakingText;
    @BindView(R.id.sharpTurnValue)
    TextView sharpTurnText;
    @BindView(R.id.scoreValue)
    TextView scoreText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        inTripBackground.setFlipInterval(2000);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        userId = preferences.getInt(LoginActivity.PREF_USER_ID, 1);

        tripId = preferences.getInt(PREF_TRIP_ID, 1);

        BASE_URL = "http://" + getResources().getString(R.string.api_url);
    }

    @Override
    public void onPause() {
        super.onPause();
        // When the activity goes into the background or exits, we want to make
        // sure to unbind from the service to avoid leaking memory
        if (mVehicleManager != null) {
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
        if (mVehicleManager == null) {
            Intent intent = new Intent(this, VehicleManager.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                preferences.edit().clear().commit();
                Intent intent = new Intent(this, LoginActivity.class);
                startActivity(intent);
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @OnClick(R.id.viewHistory)
    public void openHistory(View view) {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                try {
                    return NetworkHelper.get("/trip");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(String s) {
                if (s != null) {
                    Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
                    intent.putExtra("HISTORY_RESPONSE", s);
                    startActivity(intent);
                }
            }
        }.execute();
    }

    private VehicleMessage.Listener mListener = new VehicleMessage.Listener() {
        @Override
        public void receive(final VehicleMessage message) {
            if (!((SimpleVehicleMessage) message).getName().equals("ignition_status")
                    && !((SimpleVehicleMessage) message).getName().equals("accelerator_pedal_position")
                    && !((SimpleVehicleMessage) message).getName().equals("vehicle_speed")
                    && !((SimpleVehicleMessage) message).getName().equals("steering_wheel_angle")) {
                return;
            }
            if (((SimpleVehicleMessage) message).getName().equals("ignition_status")) {
                if (((SimpleVehicleMessage) message).getValue().equals("start")) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            startTrip();
                        }
                    });
                }
            }
            if (!inTrip) {
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
            if (dataPointsToSend.length() > 100) {
                sendDataPoints();
            }

            if (((SimpleVehicleMessage) message).getName().equals("ignition_status")) {
                if (((SimpleVehicleMessage) message).getValue().equals("off")) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            endTrip();
                        }
                    });
                    sendDataPoints();
                    sendTripEnd();
                    preferences.edit().putInt(PREF_TRIP_ID, ++tripId).commit();
                }
            }

        }
    };

    private void startTrip() {
        inTrip = true;
        inTripBackground.startFlipping();
        inTripText.setVisibility(View.VISIBLE);
        inTripBackground.setVisibility(View.VISIBLE);
    }

    private void endTrip() {
        inTripBackground.stopFlipping();
        inTripText.setVisibility(View.INVISIBLE);
        inTripBackground.setVisibility(View.INVISIBLE);
        durationText.setText("Loading...");
        hardAccsText.setText("Loading...");
        hardBrakingText.setText("Loading...");
        sharpTurnText.setText("Loading...");
        scoreText.setText("Loading...");
        inTrip = false;
    }

    private void sendTripEnd() {
        try {
            JSONObject tripInfo = new JSONObject();
            tripInfo.put("userId", userId);
            tripInfo.put("tripId", tripId);
            String response = NetworkHelper.post("/trip/end", tripInfo.toString());
            Log.i(TAG, "Trip end sent: " + response);
            final JSONObject responseJson = new JSONObject(response);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    int score = (int) Double.parseDouble(responseJson.optString("score", "0"));
                    ((ImageView) findViewById(R.id.mainMom)).setImageDrawable(getResources().getDrawable(getMom(score, preferences.getInt(LoginActivity.PREF_MOM_TYPE, 0))));
                    findViewById(R.id.mainMom).setVisibility(View.VISIBLE);
                    durationText.setText(Integer.parseInt(responseJson.optString("duration", "0")) / 1000 + " seconds");
                    hardAccsText.setText(responseJson.optString("hardAccs", "0"));
                    hardBrakingText.setText(responseJson.optString("hardBrakes", "0"));
                    sharpTurnText.setText(responseJson.optString("sharpTurns", "0"));
                    scoreText.setText(String.valueOf(score));
                }
            });
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
            String response = NetworkHelper.post("/trip/data", dataSet.toString());
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

    private int getMom(int score, int momType) {
        if (score > 50) {
            switch (momType) {
                case 0:
                    return R.drawable.mom_happy_1;
                case 1:
                    return R.drawable.mom_happy_2;
                case 2:
                    return R.drawable.mom_happy_3;
                case 3:
                    return R.drawable.mom_happy_4;
                case 4:
                    return R.drawable.mom_happy_5;
                case 5:
                    return R.drawable.mom_happy_6;
            }
        } else {
            switch (momType) {
                case 0:
                    return R.drawable.mom_very_sad_1;
                case 1:
                    return R.drawable.mom_very_sad_2;
                case 2:
                    return R.drawable.mom_very_sad_3;
                case 3:
                    return R.drawable.mom_very_sad_4;
                case 4:
                    return R.drawable.mom_very_sad_5;
                case 5:
                    return R.drawable.mom_very_sad_6;
            }
        }
        return 0;
    }
}
