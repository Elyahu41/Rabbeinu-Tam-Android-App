package com.elyjacobi.rabbeinutam;

import com.kosherjava.zmanim.AstronomicalCalendar;
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
     * @return the <code>Date</code> representing 50 minutes after sunset. If the calculation can't
     * be computed such as in the Arctic Circle where there is at least one day a year where the sun
     * does not rise, and one where it does not set, a null will be returned. See detailed
     * explanation on top of the {@link AstronomicalCalendar} documentation.
     */
    public Date getTzais50() {
        return getTimeOffset(getElevationAdjustedSunset(), 50 * ONE_MINUTE_IN_MILLI);
    }
}
