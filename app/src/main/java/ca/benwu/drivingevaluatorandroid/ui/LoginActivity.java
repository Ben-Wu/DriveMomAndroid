package ca.benwu.drivingevaluatorandroid.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import ca.benwu.drivingevaluatorandroid.R;
import ca.benwu.drivingevaluatorandroid.utils.NetworkHelper;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = LoginActivity.class.getSimpleName();

    public static final String EXTRA_USER_ID = "extra.user.id";

    public static final String PREF_USER_ID = "pref.uzer.id";
    public static final String PREF_MOM_TYPE = "pref.mom.type";
    public static final String PREF_USER_NAME = "pref.user.name";

    @BindView(R.id.login_progress)
    ProgressBar loadingCircle;

    @BindView(R.id.username)
    EditText usernameField;
    @BindView(R.id.password)
    EditText passwordField;
    @BindView(R.id.confirmPassword)
    EditText confirmPasswordField;
    @BindView(R.id.confirmPasswordContainer)
    View confirmPasswordContainer;
    @BindView(R.id.loginButton)
    Button loginButton;
    @BindView(R.id.signUpButton)
    Button signUpButton;

    private boolean isLogin = true;

    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        ButterKnife.bind(this);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        int userId = preferences.getInt(PREF_USER_ID, -1);
        if(userId != -1) {
            enterMainActivity(userId);
        }
    }

    @OnClick(R.id.loginButton)
    public void loginClick(View view) {
        if (isLogin) {
            Log.d(TAG, "Log in");
            if (checkValidEntry()) {
                loadingCircle.setVisibility(View.VISIBLE);
                final String username = usernameField.getText().toString();
                final String password = passwordField.getText().toString();
                new AsyncTask<Void,Void,String>(){
                    @Override
                    protected String doInBackground(Void... params) {
                        try {
                            JSONObject body = new JSONObject();
                            body.put("username", username);
                            body.put("password", password);
                            return NetworkHelper.post("/users/login", body.toString());
                        } catch (JSONException|IOException e) {
                            e.printStackTrace();
                            return null;
                        }
                    }

                    @Override
                    protected void onPostExecute(String s) {
                        loadingCircle.setVisibility(View.INVISIBLE);
                        try {
                            if(s == null) {
                                throw new JSONException("");
                            }
                            JSONObject response = new JSONObject(s);
                            if(response.isNull("error")) {
                                enterMainActivity(response.getInt("userId"));
                            } else {
                                Toast.makeText(LoginActivity.this, response.getString("error"), Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }.execute();
            }
        } else {
            Log.d(TAG, "Switch sign up to login");
            isLogin = true;
            confirmPasswordContainer.animate().scaleY(0f).setDuration(200).start();
            loginButton.animate().translationYBy(-loginButton.getHeight() - 30).setDuration(200).start();
            signUpButton.animate().translationYBy(signUpButton.getHeight() + 30).setDuration(200).withEndAction(new Runnable() {
                @Override
                public void run() {
                    confirmPasswordField.animate().scaleY(0.0f).alpha(0.0f).setDuration(200).start();
                }
            }).start();
        }
    }

    @OnClick(R.id.signUpButton)
    public void signUpClick(View view) {
        if (!isLogin) {
            Log.d(TAG, "Sign up");
            if (checkValidEntry()) {
                loadingCircle.setVisibility(View.VISIBLE);
                final String username = usernameField.getText().toString();
                final String password = passwordField.getText().toString();
                new AsyncTask<Void,Void,String>(){
                    @Override
                    protected String doInBackground(Void... params) {
                        try {
                            JSONObject body = new JSONObject();
                            body.put("username", username);
                            body.put("password", password);
                            return NetworkHelper.post("/users/", body.toString());
                        } catch (JSONException|IOException e) {
                            e.printStackTrace();
                            return null;
                        }
                    }

                    @Override
                    protected void onPostExecute(String s) {
                        loadingCircle.setVisibility(View.INVISIBLE);
                        try {
                            if(s == null) {
                                throw new JSONException("");
                            }
                            JSONObject response = new JSONObject(s);
                            if(response.isNull("error")) {
                                enterMainActivity(response.getInt("userId"));
                            } else {
                                Toast.makeText(LoginActivity.this, response.getString("error"), Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }.execute();
            }
        } else {
            confirmPasswordContainer.animate().scaleY(1f).setDuration(200).start();
            confirmPasswordContainer.setVisibility(View.VISIBLE);
            Log.d(TAG, "Switch login to sign up");
            isLogin = false;
            loginButton.animate().translationYBy(loginButton.getHeight() + 30).setDuration(200).start();
            signUpButton.animate().translationYBy(-signUpButton.getHeight() - 30).setDuration(200).withEndAction(new Runnable() {
                @Override
                public void run() {
                    confirmPasswordField.animate().scaleY(1.0f).alpha(1.0f).setDuration(200).start();
                }
            }).start();
        }
    }

    private boolean checkValidEntry() {
        boolean valid = usernameField.getText().length() >= 4
                && passwordField.getText().length() >= 6
                && (isLogin || confirmPasswordField.getText().toString().equals(passwordField.getText().toString()));
        if (!valid) {
            Toast.makeText(this, "Username must be at least four characters and passwords must be at least 6 characters", Toast.LENGTH_SHORT).show();
        }
        return valid;
    }

    private void enterMainActivity(int userId) {
        Toast.makeText(this, "Logged In!", Toast.LENGTH_SHORT).show();
        Intent intent;
        if(preferences.getString(PREF_USER_NAME, null) == null) {
            intent = new Intent(this, WelcomeActivity.class);
        } else {
            intent = new Intent(this, MainActivity.class);
        }
        preferences.edit().putInt(PREF_USER_ID, userId).commit();
        startActivity(intent);
        finish();
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }
}
