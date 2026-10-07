package com.agridrone.safety.util;

import java.util.Locale;

public final class TimeFormatter {

    public static final String PLACEHOLDER = "\u2014\u2014";
    public static final String CALCULATING = "Calculating…";

    private TimeFormatter() {
    }

    public static String formatDurationSeconds(Long totalSeconds) {
        if (totalSeconds == null || totalSeconds < 0) {
            return CALCULATING;
        }
        if (totalSeconds > 86400) { // > 24 hours is unrealistic for drone flight
            return PLACEHOLDER;
        }

        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.US, "%02d:%02d", minutes, seconds);
        }
    }
}
