package ca.benwu.drivingevaluatorandroid.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;

import com.openxc.VehicleManager;
import com.openxc.interfaces.network.NetworkVehicleInterface;
import com.openxc.measurements.EngineSpeed;
import com.openxc.measurements.Measurement;
import com.openxc.messages.SimpleVehicleMessage;
import com.openxc.messages.VehicleMessage;
import com.openxc.remote.VehicleServiceException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ca.benwu.drivingevaluatorandroid.R;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private VehicleManager mVehicleManager;
    private TextView mEngineSpeedView;

    private JSONArray dataPointsToSend = new JSONArray();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // grab a reference to the engine speed text object in the UI, so we can
        // manipulate its value later from Java code
        mEngineSpeedView = (TextView) findViewById(R.id.engine_speed);
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
            mVehicleManager.removeListener(EngineSpeed.class,
                    mSpeedListener);
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

    private EngineSpeed.Listener mSpeedListener = new EngineSpeed.Listener() {
        @Override
        public void receive(Measurement measurement) {
            final EngineSpeed speed = (EngineSpeed) measurement;
            runOnUiThread(new Runnable() {
                public void run() {
                    mEngineSpeedView.setText("Engine speed (RPM): " + speed.getValue().doubleValue());
                }
            });
        }
    };

    private VehicleMessage.Listener mListener = new VehicleMessage.Listener() {
        @Override
        public void receive(final VehicleMessage message) {
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

            }
        }
    };

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
                mVehicleManager.setVehicleInterface(NetworkVehicleInterface.class, "23.92.16.201:50001");
            } catch (VehicleServiceException e) {
                Log.e(TAG, "Unable to add network vehicle interface");
            }

            // We want to receive updates whenever the EngineSpeed changes. We
            // have an EngineSpeed.Listener (see above, mSpeedListener) and here
            // we request that the VehicleManager call its receive() method
            // whenever the EngineSpeed changes
            mVehicleManager.addListener(SimpleVehicleMessage.class, mListener);
        }

        // Called when the connection with the service disconnects unexpectedly
        public void onServiceDisconnected(ComponentName className) {
            Log.w(TAG, "VehicleManager Service  disconnected unexpectedly");
            mVehicleManager = null;
        }
    };
}
