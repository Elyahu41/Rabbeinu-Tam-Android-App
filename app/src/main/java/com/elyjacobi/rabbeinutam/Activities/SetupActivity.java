package com.elyjacobi.rabbeinutam.Activities;

import static com.elyjacobi.rabbeinutam.Activities.MainActivity.SHARED_PREF;
import static com.elyjacobi.rabbeinutam.Activities.MainActivity.sCurrentLocation;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.elyjacobi.rabbeinutam.LocationResolver;
import com.elyjacobi.rabbeinutam.R;

import org.jetbrains.annotations.NotNull;

public class SetupActivity extends AppCompatActivity {

    private String mElevation = "0";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

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
                            "geonames website to get the elevation data for your city.")
                    .setPositiveButton("Ok", (dialogInterface, i) -> dialogInterface.dismiss())
                    .setCancelable(false)
                    .show();
        }

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("introShown",true).apply();
        editor.putBoolean("askagain",true).apply();//if the user is re-running the setup, reset the preferences
        editor.putBoolean("isSetup", false).apply();

        Button mishorButton = findViewById(R.id.mishor);
        mishorButton.setOnClickListener(v -> {
            editor.putString("elevation" + sCurrentLocation, mElevation).apply();
            editor.putBoolean("isSetup", true).apply();
            Intent returnIntent = new Intent();
            setResult(Activity.RESULT_CANCELED, returnIntent);
            finish();
        });

        Button manualButton = findViewById(R.id.manual);
        manualButton.setOnClickListener(v -> {
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
                    mElevation = input.getText().toString();
                    editor.putString("elevation" + sCurrentLocation, mElevation).apply();
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

        Button geonamesButton = findViewById(R.id.geonames);
        geonamesButton.setOnClickListener(v -> {
            LocationResolver locationResolver = new LocationResolver(this);
            try {
                locationResolver.start();
                locationResolver.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            String result = sharedPreferences.getString("elevation" + sCurrentLocation, "");
            editor.putBoolean("isSetup", true).apply();
            Intent returnIntent = new Intent();
            returnIntent.putExtra("elevation", result);
            setResult(Activity.RESULT_OK, returnIntent);
            Toast.makeText(SetupActivity.this, "Elevation received from GeoNames!: " + result, Toast.LENGTH_LONG)
                    .show();
            finish();
        });
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