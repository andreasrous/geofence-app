package gr.hua.dit.android.geofenceapp;

import com.google.android.gms.maps.model.LatLng;

public class HaversineDistanceCalculator {

    private static final double EARTH_RADIUS = 6371e3; // meters

    public static double calculateHaversineDistance(LatLng point1, LatLng point2) {
        // Convert latitude and longitude from degrees to radians
        double f1 = Math.toRadians(point1.latitude);
        double f2 = Math.toRadians(point2.latitude);
        double Df = Math.toRadians(point2.latitude - point1.latitude);
        double Dl = Math.toRadians(point2.longitude - point1.longitude);

        // Haversine formula
        double a = Math.sin(Df / 2) * Math.sin(Df / 2) +
                Math.cos(f1) * Math.cos(f2) *
                        Math.sin(Dl / 2) * Math.sin(Dl / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        // Calculate the distance in meters
        return EARTH_RADIUS * c;
    }

    // Uses the Haversine formula to check if the first argument is inside the circle defined by the second argument
    public static boolean isPointInsideCircle(LatLng point, LatLng center) {
        double distance = calculateHaversineDistance(point, center);
        if (distance <= 100) { // the circle has a radius of 100m
            return true;
        }
        return false;
    }
}
