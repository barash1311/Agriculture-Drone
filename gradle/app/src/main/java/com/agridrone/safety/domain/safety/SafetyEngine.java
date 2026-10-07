package com.agridrone.safety.domain.safety;

import com.agridrone.safety.config.SafetyConfig;
import com.agridrone.safety.data.model.SafetyState;
import com.agridrone.safety.domain.battery.BatteryHealthResult;
import com.agridrone.safety.domain.rtl.RtlResult;

public final class SafetyEngine {

    private final RapidSagDetector rapidSagDetector;

    public SafetyEngine() {
        this.rapidSagDetector = new RapidSagDetector();
    }

    public SafetyEngine(RapidSagDetector rapidSagDetector) {
        this.rapidSagDetector = rapidSagDetector;
    }

    public SafetyEvaluation evaluate(
            BatteryHealthResult batteryHealth,
            RtlResult rtlResult,
            long currentTimestampMillis) {

        if (batteryHealth == null) {
            return SafetyEvaluation.normal();
        }

        Double batteryPercent = batteryHealth.getBatteryPercent();
        Double packVoltage = batteryHealth.getPackVoltage();
        Double minCell = batteryHealth.getCellHealth().getMinCell();
        Double cellDelta = batteryHealth.getCellHealth().getCellDelta();

        // Check for rapid voltage collapse
        boolean isRapidSag = rapidSagDetector.evaluate(packVoltage, currentTimestampMillis);

        // Min cell <= 3.40 V OR Rapid voltage collapse
        if (minCell != null && minCell <= SafetyConfig.EMERGENCY_CELL_VOLTAGE) {
            return new SafetyEvaluation(
                    SafetyState.EMERGENCY,
                    String.format("Critical cell exhaustion (Min cell: %.2f V <= %.2f V)", minCell, SafetyConfig.EMERGENCY_CELL_VOLTAGE),
                    "LAND IMMEDIATELY",
                    "Emergency battery failure"
            );
        }
        if (isRapidSag) {
            return new SafetyEvaluation(
                    SafetyState.EMERGENCY,
                    "Rapid voltage sag under load",
                    "LAND IMMEDIATELY",
                    "Sudden voltage collapse detected"
            );
        }

        // Battery <= Required RTL Battery OR Min cell <= 3.50 V
        if (rtlResult != null && rtlResult.isCritical()) {
            return new SafetyEvaluation(
                    SafetyState.CRITICAL,
                    String.format("Insufficient battery for Return-To-Launch (Current: %.0f%% <= Required: %.0f%%)",
                            rtlResult.getCurrentBatteryPercent(), rtlResult.getRequiredBatteryPercent()),
                    "RETURN TO LAUNCH",
                    "Mandatory RTL recommended"
            );
        }
        if (minCell != null && minCell <= SafetyConfig.CRITICAL_CELL_VOLTAGE) {
            return new SafetyEvaluation(
                    SafetyState.CRITICAL,
                    String.format("Cell depleted below safe threshold (Min cell: %.2f V <= %.2f V)", minCell, SafetyConfig.CRITICAL_CELL_VOLTAGE),
                    "RETURN TO LAUNCH",
                    "Mandatory RTL recommended"
            );
        }

        // Cell Delta > 0.08 V (Strictly greater than)
        if (cellDelta != null && cellDelta > SafetyConfig.CELL_DELTA_FAULT_VOLTS) {
            return new SafetyEvaluation(
                    SafetyState.CELL_FAULT,
                    String.format("Cell voltage imbalance (Delta: %.2f V > %.2f V)", cellDelta, SafetyConfig.CELL_DELTA_FAULT_VOLTS),
                    "CELL VOLTAGE IMBALANCE",
                    "Land & inspect battery"
            );
        }

        // Battery <= 20.0% OR Min cell <= 3.65 V
        if (batteryPercent != null && batteryPercent <= SafetyConfig.WARNING_BATTERY_PERCENT) {
            return new SafetyEvaluation(
                    SafetyState.WARNING,
                    String.format("Low battery (%.0f%% <= %.0f%%)", batteryPercent, SafetyConfig.WARNING_BATTERY_PERCENT),
                    "Warning — Low battery",
                    "Return to landing zone"
            );
        }
        if (minCell != null && minCell <= SafetyConfig.WARNING_CELL_VOLTAGE) {
            return new SafetyEvaluation(
                    SafetyState.WARNING,
                    String.format("Low cell voltage (%.2f V <= %.2f V)", minCell, SafetyConfig.WARNING_CELL_VOLTAGE),
                    "Warning — Low cell voltage",
                    "Prepare to land"
            );
        }

        // Battery <= 30.0%
        if (batteryPercent != null && batteryPercent <= SafetyConfig.NOTICE_BATTERY_PERCENT) {
            return new SafetyEvaluation(
                    SafetyState.NOTICE,
                    String.format("Approaching reserve (%.0f%% <= %.0f%%)", batteryPercent, SafetyConfig.NOTICE_BATTERY_PERCENT),
                    "Plan to return soon",
                    "Begin wrap-up of mission"
            );
        }

        return SafetyEvaluation.normal();
    }

    public void reset() {
        rapidSagDetector.reset();
    }
}
