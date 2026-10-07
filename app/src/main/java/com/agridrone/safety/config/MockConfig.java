package com.agridrone.safety.config;

public final class MockConfig {

    private MockConfig() {
    }

    public static final double DEFAULT_HOME_LAT = 37.7749;

    public static final double DEFAULT_HOME_LON = -122.4194;

    public static final double DEFAULT_HOME_ALT_METERS = 50.0;

    public static final int DEFAULT_CELL_COUNT = 12;

    public static final double DEFAULT_NOMINAL_CAPACITY_MAH = 22000.0;

    public static final long MOCK_TICK_INTERVAL_MS = 200L;
}
