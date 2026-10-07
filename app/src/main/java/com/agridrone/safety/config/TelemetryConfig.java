package com.agridrone.safety.config;

public final class TelemetryConfig {

    private TelemetryConfig() {
    }

    public static final long TELEMETRY_STALE_TIMEOUT_MS = 3000L;

    public static final long TELEMETRY_LOST_TIMEOUT_MS = 7000L;

    public static final long UI_THROTTLE_MS = 200L; // 5 Hz

    public static final long DB_LOG_INTERVAL_MS = 1000L; // 1 Hz

    public static final int DISCHARGE_RATE_WINDOW_SAMPLES = 10;
}
