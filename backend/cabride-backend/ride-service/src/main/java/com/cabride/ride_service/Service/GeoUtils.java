package com.cabride.ride_service.Service;

public final class GeoUtils {

    // Earth's average radius in kilometers.
    private static final double EARTH_RADIUS_KM = 6371.0;

    // Utility class: prevent object creation.
    private GeoUtils() {
    }

    /**
     * Calculates straight-line distance between two geographical coordinates
     * using the Haversine formula.
     */
    public static double calculateDistance(
            double latitude1,
            double longitude1,
            double latitude2,
            double longitude2) {

        double lat1 = Math.toRadians(latitude1);
        double lon1 = Math.toRadians(longitude1);
        double lat2 = Math.toRadians(latitude2);
        double lon2 = Math.toRadians(longitude2);

        double latitudeDifference = lat2 - lat1;
        double longitudeDifference = lon2 - lon1;

        double a = Math.pow(Math.sin(latitudeDifference / 2), 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.pow(Math.sin(longitudeDifference / 2), 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }
    /**
     * Calculates the initial bearing from the starting point
     * to the destination point.
     *
     * Returns an angle between 0 and 360 degrees.
     * 0 = North, 90 = East, 180 = South, 270 = West.
     */
    public static double calculateBearing(
            double startLatitude,
            double startLongitude,
            double endLatitude,
            double endLongitude) {

        double lat1 = Math.toRadians(startLatitude);
        double lat2 = Math.toRadians(endLatitude);

        double longitudeDifference =
                Math.toRadians(endLongitude - startLongitude);

        double y = Math.sin(longitudeDifference) * Math.cos(lat2);

        double x = Math.cos(lat1) * Math.sin(lat2)
                - Math.sin(lat1) * Math.cos(lat2)
                * Math.cos(longitudeDifference);

        double bearing = Math.toDegrees(Math.atan2(y, x));

        // Normalize the result to the range 0–360 degrees.
        return (bearing + 360) % 360;
    }
    /**
     * Calculates the smallest angular difference between two bearings.
     *
     * Example:
     * 10° and 350° have a difference of 20°, not 340°.
     */
    public static double calculateBearingDifference(
            double bearing1,
            double bearing2) {

        // Normalize both bearings to the range 0–360 degrees.
        double normalizedBearing1 = (bearing1 % 360 + 360) % 360;
        double normalizedBearing2 = (bearing2 % 360 + 360) % 360;

        double difference = Math.abs(normalizedBearing1 - normalizedBearing2);

        // Return the smallest angular difference.
        return Math.min(difference, 360 - difference);
    }

    /**
     * Validates whether a latitude-longitude pair represents
     * a valid geographical coordinate.
     */
    public static boolean isValidCoordinate(Double latitude, Double longitude) {

        return latitude != null
                && longitude != null
                && Double.isFinite(latitude)
                && Double.isFinite(longitude)
                && latitude >= -90.0
                && latitude <= 90.0
                && longitude >= -180.0
                && longitude <= 180.0;
    }

}