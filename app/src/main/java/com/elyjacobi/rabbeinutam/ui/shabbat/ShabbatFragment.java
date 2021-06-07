package com.elyjacobi.rabbeinutam.ui.shabbat;

import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.elyjacobi.rabbeinutam.GeoLocationData;
import com.elyjacobi.rabbeinutam.R;
import com.elyjacobi.rabbeinutam.RTCalendar;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static com.elyjacobi.rabbeinutam.RTCalendar.ONE_MINUTE_IN_MILLI;

public class ShabbatFragment extends Fragment {

    private ShabbatViewModel mShabbatViewModel;
    private TextView sunset;
    private RTCalendar mRTCalendar;
    private SimpleDateFormat mDateFormatter;
    private SimpleDateFormat mTimeFormatter;
    private String mDate;
    private String mTime;
    private String mSunsetTime;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        mShabbatViewModel = new ShabbatViewModel();
        View root = inflater.inflate(R.layout.fragment_shabbat, container, false);
        final TextView result = root.findViewById(R.id.text_shabbat);
        result.setTypeface(Typeface.DEFAULT_BOLD);
        sunset = root.findViewById(R.id.sunset_shabbat);
        mRTCalendar = new RTCalendar(GeoLocationData.getGeoLocation());//The geolocation data should already be set by the main activity
        Calendar calendar = mRTCalendar.getCalendar();
        while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY){
            calendar.add(Calendar.DATE, 1);
        }
        mRTCalendar.setCalendar(calendar);
        mDateFormatter = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
        mTimeFormatter = new SimpleDateFormat("h:mm aa", Locale.getDefault());
        updateDateAndTime();
        updateViewModelText();
        mShabbatViewModel.getText().observe(getViewLifecycleOwner(), result::setText);
        sunset.setText(String.format("Sunset is at: %s", mSunsetTime));
        return root;
    }

    private void updateViewModelText() {
        mShabbatViewModel.setText("Rabbeinu Tam for this " + "\u202B"//u202B is for R2L formatting
                +" \u05DE\u05D5\u05E6\u05D0\u05D9 \u05E9\u05D1\u05EA "//This says מוצאי שבת
                + "\u202A"//u202A is for L2R formatting
                + "(" + mDate + ") is at: \n\n" + mTime);
    }

    @Override
    public void onResume() {
        super.onResume();
        mRTCalendar = new RTCalendar(GeoLocationData.getGeoLocation());
        Calendar calendar = mRTCalendar.getCalendar();
        while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY){
            calendar.add(Calendar.DATE, 1);
        }
        mRTCalendar.setCalendar(calendar);
        updateDateAndTime();
        updateViewModelText();
        sunset.setText(String.format("Sunset is at: %s", mSunsetTime));
    }

    private void updateDateAndTime() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        String opinion = prefs.getString("opinion", "");
        if (opinion != null) {
            Date userChosenOpinion;
            switch (opinion) {
                case "90 zmaniyot minutes":
                    userChosenOpinion = mRTCalendar.getTzais90Zmanis();
                    break;
                case "96 zmaniyot minutes":
                    userChosenOpinion = mRTCalendar.getTzais96Zmanis();
                    break;
                case "120 zmaniyot minutes":
                    userChosenOpinion = mRTCalendar.getTzais120Zmanis();
                    break;
                case "50 regular minutes (NY Only)":
                    userChosenOpinion = mRTCalendar.getRMosheFeinstein();
                    break;
                case "60 regular minutes":
                    userChosenOpinion = mRTCalendar.getTzais60();
                    break;
                case "72 regular minutes":
                    userChosenOpinion = mRTCalendar.getTzais72();
                    break;
                case "90 regular minutes":
                    userChosenOpinion = mRTCalendar.getTzais90();
                    break;
                case "96 regular minutes":
                    userChosenOpinion = mRTCalendar.getTzais96();
                    break;
                case "120 regular minutes":
                    userChosenOpinion = mRTCalendar.getTzais120();
                    break;
                case "16.1 degrees":
                    userChosenOpinion = mRTCalendar.getTzais16Point1Degrees();
                    break;
                case "18 degrees":
                    userChosenOpinion = mRTCalendar.getTzais18Degrees();
                    break;
                case "19.8 degrees":
                    userChosenOpinion = mRTCalendar.getTzais19Point8Degrees();
                    break;
                case "26 degrees":
                    userChosenOpinion = mRTCalendar.getTzais26Degrees();
                    break;
                default: // by default we refer to 72 zmaniyot minutes
                    userChosenOpinion = mRTCalendar.getTzais72Zmanis();
                    break;
            }
            userChosenOpinion = new Date(userChosenOpinion.getTime() + ONE_MINUTE_IN_MILLI);//round up to the nearest minute
            mDate = mDateFormatter.format(userChosenOpinion);
            mTime = mTimeFormatter.format(userChosenOpinion);
            mSunsetTime = mTimeFormatter.format(mRTCalendar.getSunset());
        }
    }
}