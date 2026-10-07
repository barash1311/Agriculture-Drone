package com.agridrone.safety.ui.main;

import com.agridrone.safety.data.model.ConnectionState;
import com.agridrone.safety.data.model.TelemetryHealth;
import com.agridrone.safety.domain.battery.BatteryHealthResult;
import com.agridrone.safety.domain.rtl.RtlResult;
import com.agridrone.safety.domain.safety.SafetyEvaluation;

public final class MainUiState {

    private final ConnectionState connectionState;
    private final TelemetryHealth telemetryHealth;
    private final String connectionErrorMessage;
    private final BatteryHealthResult batteryHealth;
    private final RtlResult rtlResult;
    private final SafetyEvaluation safetyEvaluation;
    private final boolean isSpraySafetyEnabled;
    private final boolean isSprayCutoffDispatched;
    private final long lastUpdateTimestampMillis;
    private final boolean hasEverConnected;
    private final boolean dashboardSessionActive;
    private final boolean criticalAlertMinimized;
    private final ConnectionMode connectionMode;
    private final String connectionStatusMessage;

    public MainUiState(
            ConnectionState connectionState,
            TelemetryHealth telemetryHealth,
            String connectionErrorMessage,
            BatteryHealthResult batteryHealth,
            RtlResult rtlResult,
            SafetyEvaluation safetyEvaluation,
            boolean isSpraySafetyEnabled,
            boolean isSprayCutoffDispatched,
            long lastUpdateTimestampMillis) {
        this(
                connectionState,
                telemetryHealth,
                connectionErrorMessage,
                batteryHealth,
                rtlResult,
                safetyEvaluation,
                isSpraySafetyEnabled,
                isSprayCutoffDispatched,
                lastUpdateTimestampMillis,
                false,
                false,
                false,
                ConnectionMode.UNSELECTED);
    }

    public MainUiState(
            ConnectionState connectionState,
            TelemetryHealth telemetryHealth,
            String connectionErrorMessage,
            BatteryHealthResult batteryHealth,
            RtlResult rtlResult,
            SafetyEvaluation safetyEvaluation,
            boolean isSpraySafetyEnabled,
            boolean isSprayCutoffDispatched,
            long lastUpdateTimestampMillis,
            boolean hasEverConnected,
            boolean dashboardSessionActive) {
        this(
                connectionState,
                telemetryHealth,
                connectionErrorMessage,
                batteryHealth,
                rtlResult,
                safetyEvaluation,
                isSpraySafetyEnabled,
                isSprayCutoffDispatched,
                lastUpdateTimestampMillis,
                hasEverConnected,
                dashboardSessionActive,
                false,
                ConnectionMode.UNSELECTED);
    }

    public MainUiState(
            ConnectionState connectionState,
            TelemetryHealth telemetryHealth,
            String connectionErrorMessage,
            BatteryHealthResult batteryHealth,
            RtlResult rtlResult,
            SafetyEvaluation safetyEvaluation,
            boolean isSpraySafetyEnabled,
            boolean isSprayCutoffDispatched,
            long lastUpdateTimestampMillis,
            boolean hasEverConnected,
            boolean dashboardSessionActive,
            boolean criticalAlertMinimized) {
        this(
                connectionState,
                telemetryHealth,
                connectionErrorMessage,
                batteryHealth,
                rtlResult,
                safetyEvaluation,
                isSpraySafetyEnabled,
                isSprayCutoffDispatched,
                lastUpdateTimestampMillis,
                hasEverConnected,
                dashboardSessionActive,
                criticalAlertMinimized,
                ConnectionMode.UNSELECTED,
                null);
    }

    public MainUiState(
            ConnectionState connectionState,
            TelemetryHealth telemetryHealth,
            String connectionErrorMessage,
            BatteryHealthResult batteryHealth,
            RtlResult rtlResult,
            SafetyEvaluation safetyEvaluation,
            boolean isSpraySafetyEnabled,
            boolean isSprayCutoffDispatched,
            long lastUpdateTimestampMillis,
            boolean hasEverConnected,
            boolean dashboardSessionActive,
            boolean criticalAlertMinimized,
            ConnectionMode connectionMode) {
        this(
                connectionState,
                telemetryHealth,
                connectionErrorMessage,
                batteryHealth,
                rtlResult,
                safetyEvaluation,
                isSpraySafetyEnabled,
                isSprayCutoffDispatched,
                lastUpdateTimestampMillis,
                hasEverConnected,
                dashboardSessionActive,
                criticalAlertMinimized,
                connectionMode,
                null);
    }

    public MainUiState(
            ConnectionState connectionState,
            TelemetryHealth telemetryHealth,
            String connectionErrorMessage,
            BatteryHealthResult batteryHealth,
            RtlResult rtlResult,
            SafetyEvaluation safetyEvaluation,
            boolean isSpraySafetyEnabled,
            boolean isSprayCutoffDispatched,
            long lastUpdateTimestampMillis,
            boolean hasEverConnected,
            boolean dashboardSessionActive,
            boolean criticalAlertMinimized,
            ConnectionMode connectionMode,
            String connectionStatusMessage) {
        this.connectionState = connectionState != null ? connectionState : ConnectionState.DISCONNECTED;
        this.telemetryHealth = telemetryHealth != null ? telemetryHealth : TelemetryHealth.WAITING;
        this.connectionErrorMessage = connectionErrorMessage;
        this.batteryHealth = batteryHealth != null ? batteryHealth : BatteryHealthResult.empty();
        this.rtlResult = rtlResult != null ? rtlResult : RtlResult.unavailable(false);
        this.safetyEvaluation = safetyEvaluation != null ? safetyEvaluation : SafetyEvaluation.normal();
        this.isSpraySafetyEnabled = isSpraySafetyEnabled;
        this.isSprayCutoffDispatched = isSprayCutoffDispatched;
        this.lastUpdateTimestampMillis = lastUpdateTimestampMillis;
        this.hasEverConnected = hasEverConnected;
        this.dashboardSessionActive = dashboardSessionActive;
        this.criticalAlertMinimized = criticalAlertMinimized;
        this.connectionMode = connectionMode != null ? connectionMode : ConnectionMode.UNSELECTED;
        this.connectionStatusMessage = connectionStatusMessage;
    }

    public static MainUiState initial() {
        return new MainUiState(
                ConnectionState.DISCONNECTED,
                TelemetryHealth.WAITING,
                null,
                BatteryHealthResult.empty(),
                RtlResult.unavailable(false),
                SafetyEvaluation.normal(),
                true,
                false,
                0L
        );
    }

    public ConnectionState getConnectionState() {
        return connectionState;
    }

    public TelemetryHealth getTelemetryHealth() {
        return telemetryHealth;
    }

    public String getConnectionErrorMessage() {
        return connectionErrorMessage;
    }

    public BatteryHealthResult getBatteryHealth() {
        return batteryHealth;
    }

    public RtlResult getRtlResult() {
        return rtlResult;
    }

    public SafetyEvaluation getSafetyEvaluation() {
        return safetyEvaluation;
    }

    public boolean isSpraySafetyEnabled() {
        return isSpraySafetyEnabled;
    }

    public boolean isSprayCutoffDispatched() {
        return isSprayCutoffDispatched;
    }

    public long getLastUpdateTimestampMillis() {
        return lastUpdateTimestampMillis;
    }

    public boolean hasEverConnected() {
        return hasEverConnected;
    }

    public boolean isDashboardSessionActive() {
        return dashboardSessionActive;
    }

    public boolean isCriticalAlertMinimized() {
        return criticalAlertMinimized;
    }

    public ConnectionMode getConnectionMode() {
        return connectionMode;
    }

    public String getConnectionStatusMessage() {
        return connectionStatusMessage;
    }
}
