package com.bitmovin.bitcodin;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Base64;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import com.bitmovin.network.http.JSONRestClient;
import com.bitmovin.network.http.RequestMethod;
import com.bitmovin.network.http.RestClient;
import com.bitmovin.network.http.RestException;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.HashMap;

@SuppressWarnings("FieldCanBeLocal")
public class LoginActivity extends Activity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

  private EditText bitcodinUsernameET;
  private EditText bitcodinPasswordET;
  private CheckBox stayLoggedInCB;
  private Button loginBT;
  private TextView bitcodinHomepage;

  private ErrorPopup mErrorPopup;
  private Intent playerIntent;

  private SharedPreferences mPreferences;
  private SharedPreferences.Editor mPreferencesEditor;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
    StrictMode.setThreadPolicy(policy);

    requestWindowFeature(Window.FEATURE_NO_TITLE);
    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        WindowManager.LayoutParams.FLAG_FULLSCREEN);

    setContentView(R.layout.activity_login);

    this.bitcodinUsernameET = (EditText) findViewById(R.id.login_et_username);
    this.bitcodinPasswordET = (EditText) findViewById(R.id.login_et_password);
    this.stayLoggedInCB = (CheckBox) findViewById(R.id.login_cb_stay_logged_in);
    this.stayLoggedInCB.setOnCheckedChangeListener(this);
    this.loginBT = (Button) findViewById(R.id.login_bt_login);
    this.loginBT.setOnClickListener(this);

    this.mErrorPopup = new ErrorPopup(this);
    this.playerIntent = new Intent(this, PlayerActivity.class);

    this.mPreferences = getSharedPreferences("bitcodin", MODE_PRIVATE);
    this.mPreferencesEditor = this.mPreferences.edit();

    loadPreferences();
  }

  private void loadPreferences() {
    this.bitcodinUsernameET.setText(this.mPreferences.getString("username", ""));
    this.stayLoggedInCB.setChecked(this.mPreferences.getBoolean("stay_logged_in", false));
    if (!this.mPreferences.getString("current_api_key", "").equals("")) {
      loadPlayer(this.mPreferences.getString("current_api_key", ""));
    }
  }

  private void savePreferences(String apiKey) {
    this.mPreferencesEditor.putString("username", this.bitcodinUsernameET.getText().toString());
    this.mPreferencesEditor.putBoolean("stay_logged_in", this.stayLoggedInCB.isChecked());
    if (this.stayLoggedInCB.isChecked() && apiKey != null) {
      this.mPreferencesEditor.putString("current_api_key", apiKey);
    }
    this.mPreferencesEditor.apply();
  }

  private void loadPlayer(String apiKey) {
    this.playerIntent.putExtra("API_KEY", apiKey);
    savePreferences(apiKey);
    startActivity(this.playerIntent);
    this.finish();
  }

  @Override
  public void onClick(View view) {
    if (view == this.loginBT) {

      try {
        String apiKey = getApiKey(
            this.bitcodinUsernameET.getText().toString(),
            this.bitcodinPasswordET.getText().toString()
        );
        savePreferences(apiKey);
        loadPlayer(apiKey);
        startActivity(this.playerIntent);
      } catch (Exception ex) {
        if (ex instanceof RestException && ((RestException) ex).status == 401) {
          this.mErrorPopup.show("We do not recognize your username/password combination. " +
              "If you don't have an account yet, please sign up for free at<br />" +
              "<font color='#B32E3C'>www.bitcodin.com</font>", true);
        } else if (ex instanceof UnknownHostException) {
          this.mErrorPopup.show("could not connect to login server, check your network connection");
        } else {
          this.mErrorPopup.show("login failed");
        }
        ex.printStackTrace();
      }
    }
  }

  @Override
  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    savePreferences(null);
  }

  private String getApiKey(String email, String password) throws JSONException, URISyntaxException,
      IOException, RestException {
    HashMap<String, String> defaultHeaders = new HashMap<>();
    defaultHeaders.put("Content-Type", "application/json");
    defaultHeaders.put("bitcodin-api-version", "v1");
    String requestBody = "{" +
        "\t\"email\": \"" + email + "\"," +
        "\t\"password\": \"" + password + "\"" +
        "}";

    RestClient mRestClient = new JSONRestClient(new URI(Settings.AUTH_BASE_URL));
    String response = mRestClient.request(
        RequestMethod.POST,
        new URI("auth"),
        defaultHeaders,
        requestBody
    );

    return (
        new JSONObject(
            new String(Base64.decode(
                ((String) (new JSONObject(response).get("token"))).split("\\.")[1],
                Base64.DEFAULT)))
            .getString("jti"));
  }
}