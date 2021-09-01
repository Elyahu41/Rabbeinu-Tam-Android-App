package com.elyjacobi.rabbeinutam.Activities;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.widget.EditText;
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
import java.util.Objects;
import java.util.TimeZone;

import us.dustinj.timezonemap.TimeZoneMap;

public class MainActivity extends AppCompatActivity implements LocationListener {

    private double mLatitude;
    private double mLongitude;
    private double mElevation = 0;
    private boolean initialized = false;
    private boolean mGPSLocationServiceIsDisabled;
    private boolean mNetworkLocationServiceIsDisabled;
    private String mCurrentLocation;
    private String mCurrentTimeZoneID;
    private ActivityResultLauncher<Intent> setupLauncher;
    private SharedPreferences mSharedPreferences;
    private NavController mNavController;
    private Geocoder geocoder;
    public static final String SHARED_PREF = "MyPrefsFile";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);//splash screen
        super.onCreate(savedInstanceState);
        mSharedPreferences = getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        geocoder = new Geocoder(getApplicationContext());
        initializeSetupResult();
        getLatitudeAndLongitude();
        if (mGPSLocationServiceIsDisabled && mNetworkLocationServiceIsDisabled) {//app will crash without location data
            Toast.makeText(MainActivity.this, "Please Enable GPS", Toast.LENGTH_SHORT)
                    .show();
        } else {
            if ((!initialized
                    && ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION)
                    == PERMISSION_GRANTED)
                    || mSharedPreferences.getBoolean("useZipcode",false)) {
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
                        SharedPreferences.Editor editor = mSharedPreferences.edit();
                        editor.putString("lastLocation", mCurrentLocation).apply();
                    } else if (result.getResultCode() == Activity.RESULT_CANCELED) {
                        mElevation = 0;
                        SharedPreferences.Editor editor = mSharedPreferences.edit();
                        editor.putBoolean("askagain", false).apply();//If he doesn't care about elevation, we shouldn't bother him
                    }
                    setGeoLocationData();//Resend the GeoLocationData but with the user info for elevation.
                }
        );
    }

    /**
     * This method initializes the main views of the app with the objects needed for the views.
     * This method should only be called once! Moreover, the app must know the latitude and
     * longitude before calling this method, otherwise, the app will crash.
     */
    private void initializeViews() {
        initialized = true;
        setTimeZoneID();
        startSetupIfNeeded();
        setGeoLocationData();//First time setup will give non-user info, but this is needed to avoid a null pointer exception
        setContentView(R.layout.activity_main);
        BottomNavigationView navView = findViewById(R.id.nav_view);
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_today, R.id.navigation_specify, R.id.navigation_shabbat)
                .build();
        mNavController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, mNavController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, mNavController);
    }

    /**
     * uses the TimeZoneMap to get the current timezone ID based on the latitude and longitude
     */
    private void setTimeZoneID() {
        if (mLatitude != 0 && mLongitude != 0) {
            TimeZoneMap timeZoneMap = TimeZoneMap.forRegion(
                    Math.floor(mLatitude), Math.floor(mLongitude),
                    Math.ceil(mLatitude), Math.ceil(mLongitude));//trying to avoid using the forEverywhere() method
            mCurrentTimeZoneID = Objects.requireNonNull(
                    timeZoneMap.getOverlappingTimeZone(mLatitude, mLongitude)).getZoneId();
        }
    }

    /**
     * This method checks if the user has already setup the elevation from the last time he started
     * the app. If he has not, it will startup the setup activity. If he has setup the elevation
     * amount, then it checks if the user is in the same city as the last time he setup the app
     * based on the getLocationAsName method. If the user is in the same city, all is good. If the
     * user is in another city, we make an AlertDialog to warn the user that the elevation data
     * MIGHT not be accurate.
     * @see #getLocationAsName()
     */
    private void startSetupIfNeeded() {
        String lastLocation = mSharedPreferences.getString("lastLocation", "");

        if (mSharedPreferences.getBoolean("isSetup",false)) {//make sure elevation has been setup
            mElevation = mSharedPreferences.getFloat("elevation",0);//get the last value
            if (!lastLocation.equals(mCurrentLocation)) {//user should update his elevation in another city
                if (mSharedPreferences.getBoolean("askagain", true)) {
                    new AlertDialog.Builder(this)
                            .setTitle("You are not in the same city as the last time that you " +
                                    "setup the app!")
                            .setMessage("Elevation changes depending on which city you are in. " +
                                    "Therefore, it is recommended that you update your elevation" +
                                    " data. " + "\n\n" +
                                    "Last Location: " + lastLocation + "\n" +
                                    "Current Location: " + mCurrentLocation + "\n\n" +
                                    "Would you like to rerun the setup now?")
                            .setPositiveButton("Yes", (dialogInterface, i) ->
                                    startSetupForElevation())
                            .setNegativeButton("No", (dialogInterface, i) -> Toast.makeText(
                                    getApplicationContext(), "Your current elevation is: " +
                                            mElevation, Toast.LENGTH_LONG)
                                    .show())
                            .setNeutralButton("Do not ask again", (dialogInterface, i) -> {
                                mSharedPreferences.edit().putBoolean("askagain", false).apply();
                                Toast.makeText(getApplicationContext(),
                                        "Your current elevation is: " + mElevation,
                                        Toast.LENGTH_LONG).show();
                            })
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
     * @see Geocoder
     */
    private String getLocationAsName() {
        StringBuilder result = new StringBuilder();
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
     * As of Android 11 (API 30) there is a more accurate way of getting the current location of the
     * device, however, the process is slower as it needs to actually make a call to the GPS service
     * if the location has not been updated recently. This newer call made the app look slow at
     * startup, therefore, I added a splash screen and a Toast to let the user know that the app
     * is working.
     *
     * Since version 2.0 of the app, this method will now first check if the user wants to use a
     * zip code. If the user entered a zip code before, the app will use that zip code for as the
     * current location.
     */
    @SuppressWarnings("BusyWait")
    private void getLatitudeAndLongitude() {
        if (mSharedPreferences.getBoolean("useZipcode",false)) {
            getLatitudeAndLongitudeFromZipcode();
        } else {
            if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{ACCESS_FINE_LOCATION}, 1);
            } else {
                try {
                    LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                    if (locationManager != null) {
                        if (!locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                            mNetworkLocationServiceIsDisabled = true;
                        }
                        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                            mGPSLocationServiceIsDisabled = true;
                        }
                        if (!mNetworkLocationServiceIsDisabled) {
                            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000, 2000, this);
                        } else if (!mGPSLocationServiceIsDisabled) {
                            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 2000, this);
                        }
                        if (!mNetworkLocationServiceIsDisabled || !mGPSLocationServiceIsDisabled) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {//newer implementation
                                locationManager.getCurrentLocation(LocationManager.NETWORK_PROVIDER,
                                        new CancellationSignal(),
                                        Runnable::run,
                                        location -> {
                                            if (location != null) {
                                                mLatitude = location.getLatitude();
                                                mLongitude = location.getLongitude();
                                            }
                                        });
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
                                while (mLatitude == 0 && mLongitude == 0 && System.currentTimeMillis() < tenSeconds) {
                                    Thread.sleep(0);//we MUST wait for the location data to be set or else the app will crash
                                }
                                if (mLatitude == 0 && mLongitude == 0) {//if 10 seconds passed and we still don't have the location, use the older implementation
                                    Location location;//location might be old
                                    if (!mNetworkLocationServiceIsDisabled) {
                                        location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                                    } else {
                                        location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                                    }
                                    if (location != null) {
                                        mLatitude = location.getLatitude();
                                        mLongitude = location.getLongitude();
                                    }
                                }
                            } else {//older implementation
                                Location location;//location might be old
                                if (!mNetworkLocationServiceIsDisabled) {
                                    location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                                } else {
                                    location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                                }
                                if (location != null) {
                                    mLatitude = location.getLatitude();
                                    mLongitude = location.getLongitude();
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        resolveCurrentLocationName();
    }

    private void resolveCurrentLocationName() {
        mCurrentLocation = getLocationAsName();
        if (mCurrentLocation.isEmpty()) {
            if (mLatitude != 0 && mLongitude != 0) {
                String lat = String.valueOf(mLatitude).substring(0, 4);
                String longitude = String.valueOf(mLongitude).substring(0, 5);
                mCurrentLocation = "Lat: " + lat + " Long: " + longitude;
            }
        }
        if (!mCurrentLocation.isEmpty()) {
            Toast.makeText(MainActivity.this, mCurrentLocation, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * This method will let us know if the user accepted the location permissions. If not, it will
     * create an Alert Dialog box to ask the user to accept the permission again or enter a zipcode.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull @NotNull String[] permissions,
                                           @NonNull @NotNull int[] grantResults) {
        if (requestCode == 1) {
            if (permissions.length > 0 && grantResults[0] == PERMISSION_GRANTED) {
                getLatitudeAndLongitude();
                if (!initialized) {
                    initializeViews();
                }
            } else {
                createLocationDialog();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /**
     * This method will create a new AlertDialog that asks the user to use their location and it
     * will also give the option to use a zipcode through the createZipcodeDialog method which
     * will create another dialog. This method will
     *
     * @see #createZipcodeDialog()
     */
    private void createLocationDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.title_location_permission)
                .setMessage(R.string.text_location_permission)
                .setPositiveButton(R.string.ok, (dialogInterface, i) -> {
                    getLatitudeAndLongitude();//restart
                })
                .setNeutralButton(R.string.zipcode, (dialogInterface, i) -> createZipcodeDialog())
                .setCancelable(false)
                .create()
                .show();
    }

    /**
     * This method will create a new AlertDialog that asks the user to use their location and it
     * will also give the option to use a zipcode through the EditText field.
     */
    private void createZipcodeDialog() {
        final EditText input = new EditText(this);
        new AlertDialog.Builder(this)
                .setTitle("Enter a Zipcode")
                .setMessage("Warning!!! Zmanim will NOT be accurate! Using a Zipcode will give " +
                        "you zmanim based on approximately where you are. For more accurate " +
                        "zmanim, please allow the app to see your location.")
                .setView(input)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    if (input.getText().toString().isEmpty()) {// I would have loved to use a regex to validate the zipcode, however, it seems like zip codes are not uniform.
                        Toast.makeText(
                                MainActivity.this,
                                "Please Enter a valid value," +
                                        " for example: 11024",
                                Toast.LENGTH_SHORT)
                                .show();
                        createLocationDialog();
                    } else {
                        SharedPreferences.Editor editor = mSharedPreferences.edit();
                        editor.putBoolean("useZipcode", true).apply();
                        editor.putString("Zipcode", input.getText().toString()).apply();
                        getLatitudeAndLongitudeFromZipcode();
                        if (!initialized) {
                            initializeViews();
                        } else {
                            setTimeZoneID();
                            setGeoLocationData();
                            startSetupIfNeeded();
                        }
                        mNavController.navigate(R.id.navigation_today);
                    }
                })
                .setNeutralButton("Use location", (dialog, which) -> {
                    SharedPreferences.Editor editor = mSharedPreferences.edit();
                    editor.putBoolean("useZipcode", false).apply();
                    getLatitudeAndLongitude();
                    setGeoLocationData();
                    startSetupIfNeeded();
                    mNavController.navigate(R.id.navigation_today);
                })
                .create()
                .show();
    }

    /**
     * This method uses the Geocoder class to get a latitude and longitude coordinate from the user
     * specified zip code. If it can not find am address it will make a toast saying that an error
     * occurred.
     *
     * @see Geocoder
     */
    private void getLatitudeAndLongitudeFromZipcode() {
        String zipcode = mSharedPreferences.getString("Zipcode", "");
        List<Address> address = null;
        try {
            address = geocoder.getFromLocationName(zipcode, 1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if ((address != null ? address.size() : 0) > 0) {
            Address first = address.get(0);
            mLatitude = first.getLatitude();
            mLongitude = first.getLongitude();
            mCurrentLocation = getLocationAsName();
            Toast.makeText(MainActivity.this, mCurrentLocation, Toast.LENGTH_LONG)
                    .show();
            mSharedPreferences.edit().putLong("lat", Double.doubleToRawLongBits(mLatitude))
                    .apply();
            mSharedPreferences.edit().putLong("long", Double.doubleToRawLongBits(mLongitude))
                    .apply();
        } else {
            getOldZipcodeLocation();
        }
    }

    /**
     * This method retrieves the old location data from the devices storage if it has already been
     * setup beforehand.
     * @see #getLatitudeAndLongitudeFromZipcode()
     */
    private void getOldZipcodeLocation() {
        double oldLat = Double.longBitsToDouble(mSharedPreferences.getLong("lat", 0));
        double oldLong = Double.longBitsToDouble(mSharedPreferences.getLong("long",0));

        if (oldLat == mLatitude && oldLong == mLongitude) {
            Toast.makeText(MainActivity.this,
                    "Unable to change location, using old location.", Toast.LENGTH_LONG).show();
        }

        if (oldLat != 0 && oldLong != 0) {
            mLatitude = oldLat;
            mLongitude = oldLong;
        } else {
            Toast.makeText(MainActivity.this,
                    "An error occurred getting zipcode coordinates", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.enterZipcode) {
            createZipcodeDialog();
            return true;
        } else if (id == R.id.RerunSetup) {
            startSetupForElevation();
            return true;
        } else if (id == R.id.settings) {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            return true;
        } else if (id == R.id.calc_explanations) {
            startActivity(new Intent(MainActivity.this,
                    CalcExplanationsActivity.class));
            return true;
        } else if (id == R.id.about) {
            startActivity(new Intent(MainActivity.this, AboutActivity.class));
            return true;
        } else if (id == R.id.help) {
            new AlertDialog.Builder(this)
                    .setTitle("Help using this app:")
                    .setPositiveButton("ok", null)
                    .setMessage(R.string.helper_text)
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