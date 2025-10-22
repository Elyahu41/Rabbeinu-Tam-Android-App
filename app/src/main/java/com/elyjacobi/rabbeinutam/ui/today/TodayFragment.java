package com.elyjacobi.rabbeinutam.ui.today;

import static com.elyjacobi.rabbeinutam.RTCalendar.ONE_MINUTE_IN_MILLI;

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
import java.util.Date;
import java.util.Locale;

public class TodayFragment extends Fragment {

    private TodayViewModel mTodayViewModel;
    private TextView mSunset;
    private RTCalendar mRTCalendar;
    private SimpleDateFormat mDateFormatter;
    private SimpleDateFormat mTimeFormatter;
    private String mDate;
    private String mTime;
    private String mSunsetTime;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        mTodayViewModel = new TodayViewModel();
        View root = inflater.inflate(R.layout.fragment_today, container, false);
        final TextView result = root.findViewById(R.id.text_today);
        result.setTypeface(Typeface.DEFAULT_BOLD);
        mSunset = root.findViewById(R.id.today_sunset);
        mRTCalendar = new RTCalendar(GeoLocationData.getGeoLocation());//The geolocation data should already be set by the main activity
        mDateFormatter = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
        mTimeFormatter = new SimpleDateFormat("h:mm aa", Locale.getDefault());
        updateDateAndTime();
        mTodayViewModel.setText("Rabbeinu Tam for today " + "(" + mDate + ") is at: \n\n" + mTime);
        mTodayViewModel.getText().observe(getViewLifecycleOwner(), result::setText);
        mSunset.setText(String.format("Sunset is at: %s", mSunsetTime));
        return root;
    }

    @Override
    public void onResume() {
        mRTCalendar = new RTCalendar(GeoLocationData.getGeoLocation());
        updateDateAndTime();
        mTodayViewModel.setText("Rabbeinu Tam for today " + "(" + mDate + ") is at: \n\n" + mTime);
        mSunset.setText(String.format("Sunset is at: %s", mSunsetTime));
        super.onResume();
    }

    private void updateDateAndTime() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        String opinion = prefs.getString("opinion", "");
        Date userChosenOpinion = switch (opinion) {
            case "90 zmaniyot minutes" -> mRTCalendar.getTzais90Zmanis();
            case "96 zmaniyot minutes" -> mRTCalendar.getTzais96Zmanis();
            case "120 zmaniyot minutes" -> mRTCalendar.getTzais120Zmanis();
            case "50 regular minutes (NY Only)" -> mRTCalendar.getTzais50();
            case "60 regular minutes" -> mRTCalendar.getTzais60();
            case "72 regular minutes" -> mRTCalendar.getTzais72();
            case "90 regular minutes" -> mRTCalendar.getTzais90();
            case "96 regular minutes" -> mRTCalendar.getTzais96();
            case "120 regular minutes" -> mRTCalendar.getTzais120();
            case "16.1 degrees" -> mRTCalendar.getTzais16Point1Degrees();
            case "18 degrees" -> mRTCalendar.getTzais18Degrees();
            case "19.8 degrees" -> mRTCalendar.getTzais19Point8Degrees();
            case "26 degrees" -> mRTCalendar.getTzais26Degrees();
            default -> // by default we refer to 72 zmaniyot minutes
                    mRTCalendar.getTzais72Zmanis();
        };
        Date sunset = mRTCalendar.getSunset();
        mSunsetTime = mTimeFormatter.format(sunset);
        if (userChosenOpinion != null) {
            userChosenOpinion = new Date(userChosenOpinion.getTime() + ONE_MINUTE_IN_MILLI);//round up to the nearest minute
            mDate = mDateFormatter.format(userChosenOpinion);
            mTime = mTimeFormatter.format(userChosenOpinion);
        } else {
            mDate = mDateFormatter.format(sunset);
            mTime = "Not Available";
        }
    }
}