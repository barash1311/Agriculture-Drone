package com.agridrone.safety.system.spray;

import android.util.Log;

import com.agridrone.safety.config.SafetyConfig;

public final class SprayInterlockController {

    private static final String TAG = "SprayInterlockController";

    private final SprayControlGateway gateway;
    private volatile boolean spraySafetyEnabled = true;
    private volatile boolean cutoffDispatched = false;

    public SprayInterlockController(SprayControlGateway gateway) {
        this.gateway = gateway;
    }

    public synchronized void setSpraySafetyEnabled(boolean enabled) {
        this.spraySafetyEnabled = enabled;
        Log.i(TAG, "Spray safety interlock enabled: " + enabled);
        if (!enabled) {
            cutoffDispatched = false;
        }
    }

    public boolean isSpraySafetyEnabled() {
        return spraySafetyEnabled;
    }

    public boolean isCutoffDispatched() {
        return cutoffDispatched;
    }

    /**
     * Evaluates incoming battery percentage against cutoff threshold (20.0%).
     * Ensures command is dispatched exactly once upon crossing threshold.
     */
    public synchronized void evaluate(Double batteryPercent) {
        if (!spraySafetyEnabled || batteryPercent == null || Double.isNaN(batteryPercent)) {
            return;
        }

        if (batteryPercent <= SafetyConfig.SPRAY_CUTOFF_BATTERY_PERCENT) {
            if (!cutoffDispatched) {
                cutoffDispatched = true;
                String reason = String.format("Battery reached %.1f%% (<= %.1f%% cutoff threshold)",
                        batteryPercent, SafetyConfig.SPRAY_CUTOFF_BATTERY_PERCENT);
                Log.w(TAG, "EMERGENCY INTERLOCK ENGAGED: Disagreeing pump power. " + reason);
                gateway.requestPumpCutoff(reason);
            }
        } else if (batteryPercent > SafetyConfig.SPRAY_CUTOFF_BATTERY_PERCENT + 5.0) {
            // Hysteresis reset if fresh pack installed
            cutoffDispatched = false;
            gateway.resetCutoff();
        }
    }

    public synchronized void reset() {
        cutoffDispatched = false;
        gateway.resetCutoff();
    }
}
