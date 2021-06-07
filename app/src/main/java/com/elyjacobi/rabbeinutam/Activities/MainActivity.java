package com.elyjacobi.rabbeinutam.Activities;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.elyjacobi.rabbeinutam.GeoLocationData;
import com.elyjacobi.rabbeinutam.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.kosherjava.zmanim.util.GeoLocation;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

import us.dustinj.timezonemap.TimeZoneMap;

public class MainActivity extends AppCompatActivity implements LocationListener {

    private double mLatitude;
    private double mLongitude;
    private double mElevation = 0;
    private boolean initialized = false;
    private boolean mLocationServiceIsDisabled;
    private String mCurrentTimeZoneID;
    private TimeZoneMap mTimeZoneMap;
    private ActivityResultLauncher<Intent> setupLauncher;
    public static final String SHARED_PREF = "MyPrefsFile";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);//splash screen
        super.onCreate(savedInstanceState);
        initializeSetupResult();
        getLatitudeAndLongitude();
        if (mLocationServiceIsDisabled) {//app will crash without location data
            Toast.makeText(MainActivity.this, "Please Enable GPS", Toast.LENGTH_SHORT)
                    .show();
        } else {
            if (!initialized && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                initializeViews();
            }
        }
    }

    /**
     * This method registers the setupLauncher to receive the data from the user entered in the
     * SetupActivity. This is a new way of getting results back from activities.
     */
    private void initializeSetupResult() {
        setupLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK){
                        if (result.getData() != null) {
                            mElevation = result.getData()
                                    .getDoubleExtra("elevation",0);
                        }
                        SharedPreferences.Editor editor = getSharedPreferences(SHARED_PREF, MODE_PRIVATE).edit();
                        editor.putString("lastLocation", getLocationAsName()).apply();
                    }
                    if (result.getResultCode() == Activity.RESULT_CANCELED) {
                        mElevation = 0;
                        SharedPreferences.Editor editor = getSharedPreferences(SHARED_PREF, MODE_PRIVATE).edit();
                        editor.putBoolean("askagain",false).apply();//If he doesn't care about elevation, we shouldn't bother him
                    }
                    setGeoLocationData();//Resend the GeoLocationData but with the user info for elevation.
                }
        );
    }

    private void initializeViews() {
        initialized = true;
        mTimeZoneMap = TimeZoneMap.forRegion(Math.floor(mLatitude), Math.floor(mLongitude),
                Math.ceil(mLatitude), Math.ceil(mLongitude));//trying to avoid using the forEverywhere() method
        setTimeZoneID();
        startSetupIfNeeded();
        setGeoLocationData();//First time setup will give non-user info, but this is needed to avoid a null pointer exception
        setContentView(R.layout.activity_main);
        BottomNavigationView navView = findViewById(R.id.nav_view);
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_today, R.id.navigation_specify, R.id.navigation_shabbat)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);
    }

    /**
     * uses the TimeZoneMap to get the current timezone ID based on the latitude and longitude
     */
    private void setTimeZoneID() {
        mCurrentTimeZoneID = Objects.requireNonNull(
                mTimeZoneMap.getOverlappingTimeZone(mLatitude, mLongitude)).getZoneId();
    }

    /**
     * This method checks if the user has already setup the elevation from the last time he started
     * the app. If he has not, it will startup the setup activity. If he has setup the elevation
     * amount, then it checks if the user is in the same city as the last time he started the app
     * based on the getLocationAsName method. If the user is in the same city, all is good. If the
     * user is in another city, we make an AlertDialog to warn the user that the elevation data
     * MIGHT not be accurate.
     * @see #getLocationAsName()
     */
    private void startSetupIfNeeded() {
        SharedPreferences prefs = getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        String lastLocation = prefs.getString("lastLocation", "");
        String currentLocation = getLocationAsName();
        if (prefs.getBoolean("isSetup",false)) {
            if (Objects.requireNonNull( lastLocation ).equals( currentLocation ) ) {//if he is in the same place
                mElevation = prefs.getFloat("elevation",0);//get the last value
            } else {//user is in another city, therefore he should update his elevation
                if (prefs.getBoolean("askagain", true)) {
                    new AlertDialog.Builder(this)
                            .setTitle("You are not in the same city as the last time that you setup the app!")
                            .setMessage("Elevation changes depending on which city you are in. " +
                                    "Therefore, it is recommended that you update your elevation data. " +
                                    "\n\n" + "Last Location: " + lastLocation +
                                    "\n" + "Current Location: " + currentLocation + "\n\n" +
                                    "Would you like to rerun the setup now?")
                            .setPositiveButton("Yes", (dialogInterface, i) -> startSetupForElevation())
                            .setNegativeButton("No", (dialogInterface, i) -> Toast.makeText(
                                    getApplicationContext(), "Your current elevation is: " +
                                            prefs.getFloat("elevation", 0), Toast.LENGTH_LONG)
                                    .show())
                            .setNeutralButton("Do not ask again", (dialogInterface, i) ->
                                    prefs.edit().putBoolean("askagain", false).apply())
                            .show();
                }
            }
        } else {
            startSetupForElevation();
        }
    }

    /**
     * Convenience method to start up the setup activity
     */
    private void startSetupForElevation() {
        setupLauncher.launch(new Intent(this, SetupActivity.class));
    }

    /**
     * This method uses the Geocoder class to try and get the current location's name. I have
     * tried to make my results similar to the zmanim app by JGindin on the Play Store. In america,
     * it will get the current location by state and city. Whereas, in other areas of the world, it
     * will get the country and the city. Note that the Geocoder class might give weird results,
     * even in the same city.
     * @return a string containing the name of the current city and state/country that the user
     * is located in.
     */
    private String getLocationAsName() {
        StringBuilder result = new StringBuilder();
        Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
        List<Address> addresses = null;
        try {
            addresses = geocoder.getFromLocation(mLatitude, mLongitude, 1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (addresses != null && addresses.size() > 0) {

            String city = addresses.get(0).getLocality();
            if (city != null) { result.append(city).append(", "); }

            String state = addresses.get(0).getAdminArea();
            if (state != null) { result.append(state); }

            if (result.toString().endsWith(",")) {
                result.deleteCharAt(result.length()-1);
            }

            if (city == null && state == null) {
                String country = addresses.get(0).getCountryName();
                result.append(country);
            }
        }
        return result.toString();
    }

    /**
     * This method sets the GeoLocation object in a static class for use by the fragments. This is
     * the only way I was able to transfer the GeoLocation data between instances of the MainActivity
     * class and the other fragments that were also instances. There might be better ways to
     * transfer the GeoLocation object, but this is the best way that I found that worked.
     */
    private void setGeoLocationData() {
        GeoLocationData.setGeoLocation(new GeoLocation(
                "",//not needed
                mLatitude,
                mLongitude,
                mElevation,
                TimeZone.getTimeZone(mCurrentTimeZoneID)));
    }

    /**
     * This method gets the devices last known latitude and longitude. It will ask for permission
     * if we do not have it, and it will alert the user if location services is disabled.
     *
     * I have since added a more updated and accurate way of getting the current location of the
     * device, however, the process is slower as it needs to actually make a call to the GPS service
     * if the location has not been updated recently. This newer call made the app look slow at
     * startup, therefore, I added a splash screen and a Toast to calm the user down a bit.
     */
    private void getLatitudeAndLongitude() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1);
        } else {
            try {
                LocationManager locationManager =
                        (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                if (locationManager != null) {
                    if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        mLocationServiceIsDisabled = true;
                    } else {
                        locationManager.requestLocationUpdates(
                                LocationManager.GPS_PROVIDER, 0, 2000, this);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {//newer implementation
                            locationManager.getCurrentLocation(LocationManager.GPS_PROVIDER,
                                    new CancellationSignal(),
                                    Runnable::run,
                                    location -> {
                                        if (location != null) {
                                            mLatitude = location.getLatitude();
                                            mLongitude = location.getLongitude();
                                        }
                                    });
                            Toast.makeText(MainActivity.this,
                                    "Trying to acquire your location...", Toast.LENGTH_LONG)
                                    .show();//show a toast in order for the user to know that the app is working
                            long tenSeconds = System.currentTimeMillis() + 10000;
                            while (mLatitude == 0 && mLongitude == 0
                                    && System.currentTimeMillis() < tenSeconds) {
                                Thread.sleep(0);//we MUST wait for the location data to be set or else the app will crash
                            }
                            if (mLatitude == 0 && mLongitude == 0) {//if 10 seconds passed and we still don't have the location, use the older implementation
                                Location location = locationManager
                                        .getLastKnownLocation(LocationManager.GPS_PROVIDER);//location might be old
                                if (location != null) {
                                    mLatitude = location.getLatitude();
                                    mLongitude = location.getLongitude();
                                }
                            }
                        } else {//older implementation
                            Location location = locationManager
                                    .getLastKnownLocation(LocationManager.GPS_PROVIDER);//location might be old
                            if (location != null) {
                                mLatitude = location.getLatitude();
                                mLongitude = location.getLongitude();
                            }
                        }
                        Toast.makeText(MainActivity.this, getLocationAsName(),
                                Toast.LENGTH_LONG).show();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * This method will let us know if the user accepted the location permissions. If not, it will
     * create an Alert Dialog box to ask the user to accept the permission again.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull @NotNull String[] permissions,
                                           @NonNull @NotNull int[] grantResults) {
        if (requestCode == 1) {
            if (permissions.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLatitudeAndLongitude();
                initializeViews();
            } else {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.title_location_permission)
                        .setMessage(R.string.text_location_permission)
                        .setPositiveButton(R.string.ok, (dialogInterface, i) -> {
                            getLatitudeAndLongitude();//restart
                        })
                        .create()
                        .show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.RerunSetup) {
            startSetupForElevation();
            return true;
        } else if (id == R.id.settings) {
            Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        } else if (id == R.id.calc_explanations) {
            Intent intent = new Intent(MainActivity.this, CalcExplanationsActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.about) {
            Intent aboutIntent = new Intent(MainActivity.this, AboutActivity.class);
            startActivity(aboutIntent);
            return true;
        } else if (id == R.id.help) {
            new AlertDialog.Builder(this)
                    .setTitle("Help using this app:")
                    .setPositiveButton("ok", null)
                    .setMessage("Welcome to the Rabbeinu Tam App, this app was made to help you " +
                            "find out when sof zman rabbeinu tam is! \nThere are three steps to " +
                            "setting up the app first. \n\n 1.) Setup elevation for where you are" +
                            " currently located. In this first step is the setup screen that you " +
                            "should have first encountered when starting up the app. Some " +
                            "opinions hold that sunset is when you see the sun set at the highest" +
                            " point of the city. You can enter mishor, or you can enter " +
                            "the amount of elevation manually in meters, or if you don't know the" +
                            " amount for your area, you can find out that info through the chai" +
                            " tables website. Note that you do not need elevation for degree " +
                            "based calculations. \n\n 2.) There are multiple opinions that you can " +
                            "choose from as the user of this app in the settings menu. If you " +
                            "want to know the details of that specific opinion and how the app " +
                            "calculates the times for rabbeinu tam, check out the \"How the " +
                            "calculations work\" menu option. \n\n 3.) There are three screens " +
                            "to choose from in the main view of the app. The middle one is for " +
                            "when rabbeinu tam is for today. The left one is if you want to " +
                            "choose a specific day on the calendar for when rabbeinu tam is/was. " +
                            "Lastly, there is the right one that shows when rabbeinu tam is for " +
                            "this shabbat/shabbos.\n\nIn addition to all of this, there is a " +
                            "feature that will tell you when to update the elevation if you leave" +
                            " the city that you currently set up the app for. The reason for " +
                            "this feature is because elevation will change bases on the city you" +
                            " are located in. You will have to update it every time you move " +
                            "around. However, this feature will only turn on if you use " +
                            "elevation.")
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onLocationChanged(Location location) { }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) { }

    @Override
    public void onProviderEnabled(String provider) { }

    @Override
    public void onProviderDisabled(String provider) {
        Toast.makeText(MainActivity.this, "Please Enable GPS", Toast.LENGTH_SHORT)
                .show();
    }
}