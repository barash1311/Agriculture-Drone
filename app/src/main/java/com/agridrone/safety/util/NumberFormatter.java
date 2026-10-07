package com.agridrone.safety.util;

import java.util.Locale;

public final class NumberFormatter {

    public static final String PLACEHOLDER = "\u2014\u2014";

    private NumberFormatter() {
    }

    public static String formatPercent(Double percent) {
        if (percent == null || Double.isNaN(percent) || Double.isInfinite(percent)) {
            return PLACEHOLDER;
        }
        return String.format(Locale.US, "%.0f%%", percent);
    }

    public static String formatPercentPlain(Double percent) {
        if (percent == null || Double.isNaN(percent) || Double.isInfinite(percent)) {
            return PLACEHOLDER;
        }
        return String.format(Locale.US, "%.0f", percent);
    }

    public static String formatVoltage(Double volts) {
        if (volts == null || Double.isNaN(volts) || Double.isInfinite(volts) || volts <= 0.0) {
            return PLACEHOLDER;
        }
        return String.format(Locale.US, "%.2f\u00a0V", volts);
    }

    public static String formatCellVoltage(Double volts) {
        if (volts == null || Double.isNaN(volts) || Double.isInfinite(volts) || volts <= 0.0) {
            return "-- V";
        }
        return String.format(Locale.US, "%.2f\u00a0V", volts);
    }

    public static String formatCurrent(Double amps) {
        if (amps == null || Double.isNaN(amps) || Double.isInfinite(amps)) {
            return PLACEHOLDER;
        }
        return String.format(Locale.US, "%.1f A", amps);
    }

    public static String formatTemperature(Double celsius) {
        if (celsius == null || Double.isNaN(celsius) || Double.isInfinite(celsius)) {
            return PLACEHOLDER;
        }
        return String.format(Locale.US, "%.0f °C", celsius);
    }

    public static String formatCellDelta(Double deltaVolts) {
        if (deltaVolts == null || Double.isNaN(deltaVolts) || Double.isInfinite(deltaVolts)) {
            return PLACEHOLDER;
        }
        return String.format(Locale.US, "%.2f\u00a0V", deltaVolts);
    }

    public static String formatDistance(Double meters) {
        if (meters == null || Double.isNaN(meters) || Double.isInfinite(meters) || meters < 0.0) {
            return PLACEHOLDER;
        }
        if (meters >= 1000.0) {
            return String.format(Locale.US, "%.2f km", meters / 1000.0);
        }
        return String.format(Locale.US, "%.0f m", meters);
    }

    public static String formatSpeed(Double mps) {
        if (mps == null || Double.isNaN(mps) || Double.isInfinite(mps) || mps < 0.0) {
            return PLACEHOLDER;
        }
        return String.format(Locale.US, "%.1f m/s", mps);
    }
}
