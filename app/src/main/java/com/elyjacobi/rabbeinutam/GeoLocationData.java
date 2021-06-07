package com.elyjacobi.rabbeinutam;

import com.kosherjava.zmanim.util.GeoLocation;

/**
 * This static class has only one job, and that is to save the GeoLocation data between instances
 * of two activities or fragments.
 */
public class GeoLocationData {

    private static GeoLocation sGeoLocation;

    /**
     * The setter method for the GeoLocation data.
     * @param geoLocation a GeoLocation object
     */
    public static void setGeoLocation(GeoLocation geoLocation) {
        sGeoLocation = geoLocation;
    }

    /**
     * The getter method for the GeoLocation data.
     * @return returns the GeoLocation object
     */
    public static GeoLocation getGeoLocation() {
        return sGeoLocation;
    }
}
