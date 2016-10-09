package ca.benwu.drivingevaluatorandroid.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.List;

import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
import ca.benwu.drivingevaluatorandroid.R;

public class WelcomeActivity extends AppCompatActivity {

    private static final String TAG = WelcomeActivity.class.getSimpleName();

    @BindViews({R.id.mom1, R.id.mom2, R.id.mom3, R.id.mom4, R.id.mom5, R.id.mom6})
    List<View> moms;

    @BindView(R.id.user_name)
    EditText userNameField;

    private SharedPreferences preferences;

    private int selectedMom = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        ButterKnife.bind(this);

        moms.get(0).setSelected(true);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        for (int i = 0 ; i < moms.size() ; i++) {
            final int iCopy = i;
            moms.get(i).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ButterKnife.apply(moms, UNSELECT);
                    selectedMom = iCopy;
                    Log.i(TAG, "Selected mom " + selectedMom);
                    v.setSelected(true);
                }
            });
        }
    }

    static final ButterKnife.Action<View> UNSELECT = new ButterKnife.Action<View>() {
        @Override public void apply(View view, int index) {
            view.setSelected(false);
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_welcome, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.next) {
            if (!userNameField.getText().toString().isEmpty()) {
                preferences.edit().putString(LoginActivity.PREF_USER_NAME, userNameField.getText().toString()).commit();
                preferences.edit().putInt(LoginActivity.PREF_MOM_TYPE, selectedMom).commit();
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
                finish();
            } else {
                Toast.makeText(this, "Please enter a name", Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
