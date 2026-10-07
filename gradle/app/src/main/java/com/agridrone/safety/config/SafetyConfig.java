package com.agridrone.safety.config;

public final class SafetyConfig {

    private SafetyConfig() {
    }

    /** Trigger threshold for NOTICE safety state: Battery remaining <= 30.0% */
    public static final double NOTICE_BATTERY_PERCENT = 30.0;

    /** Trigger threshold for WARNING safety state: Battery remaining <= 20.0% */
    public static final double WARNING_BATTERY_PERCENT = 20.0;

    /** Trigger threshold for WARNING safety state: Min cell voltage <= 3.65 V */
    public static final double WARNING_CELL_VOLTAGE = 3.65;

    /** Trigger threshold for CRITICAL safety state: Min cell voltage <= 3.50 V */
    public static final double CRITICAL_CELL_VOLTAGE = 3.50;

    /** Trigger threshold for EMERGENCY safety state: Min cell voltage <= 3.40 V */
    public static final double EMERGENCY_CELL_VOLTAGE = 3.40;

    /** Trigger threshold for CELL_FAULT safety state: Max cell - Min cell > 0.08 V */
    public static final double CELL_DELTA_FAULT_VOLTS = 0.08;

    /** Dynamic Return-To-Launch energy safety margin: 15.0% */
    public static final double RTL_SAFETY_MARGIN_PERCENT = 15.0;

    /** Periodic spoken status announcement interval: 60 seconds */
    public static final long TTS_INTERVAL_SECONDS = 60L;

    /** Agricultural spray safety interlock auto-shutdown battery threshold: <= 20.0% */
    public static final double SPRAY_CUTOFF_BATTERY_PERCENT = 20.0;

    /** Default nominal return cruising speed (m/s) if autopilot does not report ground speed */
    public static final double DEFAULT_CRUISE_SPEED_MPS = 10.0;

    /**
     * Estimated internal resistance (Ohms) used for voltage sag compensation.
     * Hardware Ri is unspecified; this represents a documented simulation baseline.
     */
    public static final double DEFAULT_INTERNAL_RESISTANCE_OHMS = 0.015;

    /** Rapid voltage sag threshold in Volts per second evaluated over a sliding window */
    public static final double RAPID_SAG_THRESHOLD_VOLTS_PER_SEC = 0.50;

    /** Rapid voltage sag detection window in milliseconds */
    public static final long RAPID_SAG_WINDOW_MS = 2000L;

    /** Alert hysteresis to prevent threshold oscillation */
    public static final double BATTERY_HYSTERESIS_PERCENT = 0.5;
    public static final double CELL_HYSTERESIS_VOLTS = 0.02;
}
