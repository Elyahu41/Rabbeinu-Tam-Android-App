package com.elyjacobi.rabbeinutam;

import com.kosherjava.zmanim.ComplexZmanimCalendar;
import com.kosherjava.zmanim.util.GeoLocation;

import java.util.Date;

public class RTCalendar extends ComplexZmanimCalendar {

    public static final int ONE_MINUTE_IN_MILLI = 60_000;

    public RTCalendar(GeoLocation location) {
        super(location);
        setUseElevation(true);
    }

    /**
     * This method returns a Date object containing nightfall according to Rabbi Moshe Feinstein
     * that it occurs at exactly 50 regular minutes after sea level sunset. NOTE: This method should
     * only be used in New York, USA.
     * @return a Date object containing nightfall according to Rabbi Moshe Feinstein that it occurs
     * at exactly 50 regular minutes after sunset.
     */
    public Date getRMosheFeinstein() {
        return new Date(getSunset().getTime() + (50 * ONE_MINUTE_IN_MILLI));
    }
}
