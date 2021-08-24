package com.elyjacobi.rabbeinutam.Activities;

import static com.elyjacobi.rabbeinutam.Activities.MainActivity.SHARED_PREF;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.elyjacobi.rabbeinutam.ChaiTablesScraper;
import com.elyjacobi.rabbeinutam.R;

import org.jetbrains.annotations.NotNull;

public class SetupActivity extends AppCompatActivity {

    private double mElevation = 0;
    private final String mChaiTablesURL = "http://chaitables.com";
    private WebView mWebView;
    private Button mMishorButton;
    private Button mManualButton;
    private Button mChaitablesButton;
    private TextView setupHeader;
    private TextView elevationInfo;
    private TextView mishorRequest;
    private TextView manualRequest;
    private TextView chaitablesRequest;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);
        setupHeader = findViewById(R.id.setup_header);
        elevationInfo = findViewById(R.id.elevation_info);
        mishorRequest = findViewById(R.id.mishor_request);
        manualRequest = findViewById(R.id.manual_request);
        chaitablesRequest = findViewById(R.id.chaiTables_request);
        mWebView = findViewById(R.id.web_view);


        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        if (!sharedPreferences.getBoolean("introShown",false)) {//if the introduction has not been shown
            new AlertDialog.Builder(this)
                    .setTitle("Introduction")
                    .setMessage("Welcome to the Rabbeinu tam app! The goal of this app is to help" +
                            " you find out when sof zman rabbeinu tam is everyday. However, " +
                            "before the app can show you the time, the app requires some optional "+
                            "information. That information is the elevation of the city that you " +
                            "are located in. Some calculations do not need this information, " +
                            "while other calculations can take elevation into account. Therefore," +
                            " I left it up to the user to decide whether or not to input this " +
                            "information. Elevation changes based on the city, and it is not easy" +
                            " to calculate where the highest point of elevation is in your city" +
                            " without detailed information of the city's elevation. Which is " +
                            "a bit too much data for this app. Therefore, if you do not " +
                            "want to take elevation into account, just use the mishor (sea level)" +
                            " button. If you know the elevation data in meters, you can enter it " +
                            "in manually. And if you do not know the elevation data at all, but " +
                            "you do want to include it in your calculations, you can use the " +
                            "chaitables website to get the elevation data for your city. Note " +
                            "that not every city will be available through chaitables.")
                    .setPositiveButton("Ok", (dialogInterface, i) -> { })
                    .setCancelable(false)
                    .show();
        }

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("introShown",true).apply();
        editor.putBoolean("askagain",true).apply();//if the user is re-running the setup, reset the preferences
        editor.putBoolean("isSetup", false).apply();

        mMishorButton = findViewById(R.id.mishor);
        mMishorButton.setOnClickListener(v -> {
            editor.putFloat("elevation", (float)mElevation).apply();
            editor.putBoolean("isSetup", true).apply();
            Intent returnIntent = new Intent();
            setResult(Activity.RESULT_CANCELED, returnIntent);
            finish();
        });

        mManualButton = findViewById(R.id.manual);
        mManualButton.setOnClickListener(v -> {
            final EditText input = new EditText(this);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Enter elevation in meters:");
            builder.setView(input);
            builder.setPositiveButton("OK", (dialog, which) -> {
                if (input.getText().toString().isEmpty() ||
                        !input.getText().toString().matches("[0-9]+.?[0-9]*")) {//regex to check for a proper number input
                    Toast.makeText(
                            SetupActivity.this,
                            "Please Enter a valid value, for example: 30 or 30.0",
                            Toast.LENGTH_SHORT)
                            .show();
                } else {
                    mElevation = Double.parseDouble(input.getText().toString());
                    editor.putFloat("elevation", (float) mElevation).apply();
                    editor.putBoolean("isSetup", true).apply();
                    Intent returnIntent = new Intent();
                    returnIntent.putExtra("elevation", mElevation);
                    setResult(Activity.RESULT_OK, returnIntent);
                    finish();
                }
            });
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
            builder.create();
            builder.show();
        });

        mChaitablesButton = findViewById(R.id.chaiTables);
        mChaitablesButton.setOnClickListener(v -> {
            showDialogBox();
            setVisibilityOfViews(View.INVISIBLE);
            mWebView.setVisibility(View.VISIBLE);

            WebSettings webSettings = mWebView.getSettings();
            webSettings.setJavaScriptEnabled(true);
            mWebView.loadUrl(mChaiTablesURL);
            mWebView.setWebViewClient(new WebViewClient() {
                @Override public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    if (view.getUrl().startsWith("http://chaitables.com/cgi-bin/")) {//this is enough to know that it is showing the table with the info we need
                        ChaiTablesScraper thread = new ChaiTablesScraper();
                        thread.setUrl(view.getUrl());
                        thread.start();
                        try {
                            thread.join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        double result = thread.getResult();
                        SharedPreferences.Editor editor = getSharedPreferences(
                                SHARED_PREF, MODE_PRIVATE).edit();
                        editor.putFloat("elevation", (float) result).apply();
                        editor.putBoolean("isSetup", true).apply();
                        Intent returnIntent = new Intent();
                        returnIntent.putExtra("elevation", result);
                        setResult(Activity.RESULT_OK, returnIntent);
                        Toast.makeText(SetupActivity.this,
                                "Elevation received from ChaiTables!: " + result,
                                Toast.LENGTH_LONG).show();
                        finish();
                    }
                }
            });
        });
    }

    /**
     * Self explanatory convenience method to hide all the views except the WebView.
     * @param visibility either values of visibilities mentioned in the {@link View} class.
     * @see View
     */
    private void setVisibilityOfViews(int visibility) {
        mMishorButton.setVisibility(visibility);
        mManualButton.setVisibility(visibility);
        mChaitablesButton.setVisibility(visibility);
        setupHeader.setVisibility(visibility);
        elevationInfo.setVisibility(visibility);
        mishorRequest.setVisibility(visibility);
        manualRequest.setVisibility(visibility);
        chaitablesRequest.setVisibility(visibility);
    }

    private void showDialogBox() {
        new AlertDialog.Builder(this)
                .setTitle("How to get info from chaitables.com")
                .setMessage("(I recommend that you visit the website first) Follow the steps of " +
                        "the website until you can generate the times of sunrise/sunset for the " +
                        "year. Choose your area and any of the 6 sunrise/sunset tables to " +
                        "calculate. The app will automatically find the highest point of your" +
                        " city from that page.")
                .setPositiveButton("Ok", (dialogInterface, i) -> { })
                .show();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (mWebView.canGoBack()) {
                    mWebView.goBack();
                } else {
                    setVisibilityOfViews(View.VISIBLE);

                    mWebView.setVisibility(View.INVISIBLE);
                }
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.setup_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull @NotNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.help) {
            new AlertDialog.Builder(this)
                    .setTitle("Help using this app:")
                    .setPositiveButton("ok", null)
                    .setMessage(R.string.helper_text)
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}