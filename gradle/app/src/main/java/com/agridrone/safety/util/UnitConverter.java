package com.agridrone.safety.util;

public final class UnitConverter {

    private UnitConverter() {
    }

    /**
     * Converts current (Amps) and duration (milliseconds) into consumed capacity in milliampere-hours (mAh).
     * 1 A for 1 hour (3600000 ms) = 1000 mAh.
     */
    public static double currentToMah(double currentAmps, long elapsedMillis) {
        if (elapsedMillis <= 0 || currentAmps <= 0.0) {
            return 0.0;
        }
        double elapsedHours = (double) elapsedMillis / 3600000.0;
        return currentAmps * elapsedHours * 1000.0;
    }

    /**
     * Converts meters to kilometers.
     */
    public static double metersToKilometers(double meters) {
        return meters / 1000.0;
    }
}
