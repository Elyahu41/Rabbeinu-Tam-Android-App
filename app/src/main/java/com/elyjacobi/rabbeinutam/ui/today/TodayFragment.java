package com.elyjacobi.rabbeinutam.ui.today;

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

import static com.elyjacobi.rabbeinutam.RTCalendar.ONE_MINUTE_IN_MILLI;

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
        mTodayViewModel.setText("Rabbeinu Tam for today " + "(" + mDate + ") is at: \n" + mTime);
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