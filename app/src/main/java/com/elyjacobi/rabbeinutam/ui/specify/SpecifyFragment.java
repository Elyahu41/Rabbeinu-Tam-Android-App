package com.elyjacobi.rabbeinutam.ui.specify;

import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
import static java.text.DateFormat.getTimeInstance;

public class SpecifyFragment extends Fragment {

    private SpecifyViewModel mSpecifyViewModel;
    private TextView sunset;
    private RTCalendar mRTCalendar;
    private SimpleDateFormat mDateFormatter;
    private SimpleDateFormat mTimeFormatter;
    private final Calendar mUserChosenDate = Calendar.getInstance();
    private String mDate;
    private String mTime;
    private String mSunsetTime;
    private static boolean sButtonHasBeenClicked; //important to keep this boolean static in order for it not to get replaced

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        mSpecifyViewModel = new SpecifyViewModel();
        View root = inflater.inflate(R.layout.fragment_specify, container, false);
        final TextView result = root.findViewById(R.id.specific_result);

        sunset = root.findViewById(R.id.specify_sunset);
        result.setTypeface(Typeface.DEFAULT_BOLD);
        mRTCalendar = new RTCalendar(GeoLocationData.getGeoLocation());//The geolocation data should already be set by the main activity
        mDateFormatter = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
        mTimeFormatter = new SimpleDateFormat("h:mm aa", Locale.getDefault());
        updateDateAndTime();
        Button specificButton = root.findViewById(R.id.specific_button);
        DatePickerDialog dialog = new DatePickerDialog(requireContext(), (view, year, month, day) -> {
                    mUserChosenDate.set(year, month, day);
                    mRTCalendar.setCalendar(mUserChosenDate);
                    update();
                },
                mUserChosenDate.get(Calendar.YEAR),
                mUserChosenDate.get(Calendar.MONTH),
                mUserChosenDate.get(Calendar.DAY_OF_MONTH));
        specificButton.setOnClickListener(v -> {
            dialog.show();
            sButtonHasBeenClicked = true;
        });
        mSpecifyViewModel.getText().observe(getViewLifecycleOwner(), result::setText);
        return root;
    }

    @Override
    public void onResume() {
        if (sButtonHasBeenClicked) {//only update if the user has chosen a date
            update();
        }
        super.onResume();
    }

    private void update() {
        updateDateAndTime();
        mSpecifyViewModel.setText("Rabbeinu Tam for " + mDate + " is at: \n\n\n" + mTime);//probably should use another text view instead of 3 newlines
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
