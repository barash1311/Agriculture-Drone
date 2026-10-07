package com.agridrone.safety.domain.navigation;

public final class DistanceCalculator {

    private static final double EARTH_RADIUS_METERS = 6371000.0;

    private DistanceCalculator() {
    }

    /**
     * Calculates distance between aircraft and home coordinates in meters.
     *
     * @param lat1 aircraft latitude in decimal degrees
     * @param lon1 aircraft longitude in decimal degrees
     * @param lat2 home latitude in decimal degrees
     * @param lon2 home longitude in decimal degrees
     * @return distance in meters, or Double.NaN if any coordinate is null/NaN
     */
    public static double calculateDistanceMeters(Double lat1, Double lon1, Double lat2, Double lon2) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
            return Double.NaN;
        }
        if (Double.isNaN(lat1) || Double.isNaN(lon1) || Double.isNaN(lat2) || Double.isNaN(lon2)) {
            return Double.NaN;
        }

        // Coordinate identity optimization
        if (Double.compare(lat1, lat2) == 0 && Double.compare(lon1, lon2) == 0) {
            return 0.0;
        }

        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double deltaPhi = Math.toRadians(lat2 - lat1);
        double deltaLambda = Math.toRadians(lon2 - lon1);

        double sinHalfDeltaPhi = Math.sin(deltaPhi / 2.0);
        double sinHalfDeltaLambda = Math.sin(deltaLambda / 2.0);

        double a = (sinHalfDeltaPhi * sinHalfDeltaPhi) +
                Math.cos(phi1) * Math.cos(phi2) * (sinHalfDeltaLambda * sinHalfDeltaLambda);

        // Numerical clamp to guard against floating point inaccuracies
        a = Math.min(1.0, Math.max(0.0, a));

        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));

        return EARTH_RADIUS_METERS * c;
    }
}
