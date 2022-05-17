package com.elyjacobi.rabbeinutam;

import static com.elyjacobi.rabbeinutam.Activities.MainActivity.SHARED_PREF;
import static com.elyjacobi.rabbeinutam.Activities.MainActivity.sCurrentLocation;
import static com.elyjacobi.rabbeinutam.Activities.MainActivity.sLatitude;
import static com.elyjacobi.rabbeinutam.Activities.MainActivity.sLongitude;

import android.content.Context;
import android.content.SharedPreferences;

import org.geonames.WebService;

import java.io.IOException;
import java.util.ArrayList;

public class LocationResolver extends Thread {

    private final Context mContext;
    private final SharedPreferences mSharedPreferences;

    public LocationResolver(Context context) {
        mContext = context;
        mSharedPreferences = mContext.getSharedPreferences(SHARED_PREF, Context.MODE_PRIVATE);
    }

    public void getElevationFromWebService() throws IOException {
        WebService.setUserName("Elyahu41");
        ArrayList<Integer> elevations = new ArrayList<>();
        int e1 = WebService.srtm3(sLatitude, sLongitude);
        if (e1 > 0) {
            elevations.add(e1);
        }
        int e2 = WebService.astergdem(sLatitude, sLongitude);
        if (e2 > 0) {
            elevations.add(e2);
        }
        int e3 = WebService.gtopo30(sLatitude, sLongitude);
        if (e3 > 0) {
            elevations.add(e3);
        }
        int sum = 0;
        for (int e : elevations) {
            sum += e;
        }
        int size = elevations.size();
        if (size == 0) {
            size = 1;//edge case if no elevation data is available
        }
        mSharedPreferences.edit().putString("elevation" + sCurrentLocation, String.valueOf(sum / size)).apply();
    }

    @Override
    public void run() {
        try {
            getElevationFromWebService();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
