package com.agridrone.safety.logging;

import android.os.SystemClock;
import android.util.Log;

import com.agridrone.safety.config.TelemetryConfig;
import com.agridrone.safety.data.local.FlightLogDao;
import com.agridrone.safety.data.local.FlightLogEntity;
import com.agridrone.safety.data.model.ConnectionState;
import com.agridrone.safety.data.model.PositionTelemetry;
import com.agridrone.safety.data.model.SafetyState;
import com.agridrone.safety.domain.battery.BatteryHealthResult;
import com.agridrone.safety.domain.rtl.RtlResult;
import com.agridrone.safety.domain.safety.SafetyEvaluation;
import com.agridrone.safety.util.AppExecutors;

public final class FlightLogger {

    private static final String TAG = "FlightLogger";

    private final FlightLogDao dao;
    private final AppExecutors executors;

    private long lastLogElapsedRealtime = 0L;
    private SafetyState lastLoggedSafetyState = null;

    public FlightLogger(FlightLogDao dao) {
        this(dao, AppExecutors.getInstance());
    }

    public FlightLogger(FlightLogDao dao, AppExecutors executors) {
        this.dao = dao;
        this.executors = executors;
    }

    /**
     * Evaluates whether to persist a telemetry snapshot.
     * Persists if periodic interval (1000ms) has elapsed OR if safety state has transitioned.
     */
    public void logSample(
            ConnectionState connectionState,
            PositionTelemetry position,
            BatteryHealthResult batteryHealth,
            RtlResult rtlResult,
            SafetyEvaluation safetyEvaluation,
            boolean spraySafetyEnabled,
            boolean sprayShutdownRequested,
            long currentTimestampMillis) {

        if (dao == null || batteryHealth == null || safetyEvaluation == null) {
            return;
        }

        long now = SystemClock.elapsedRealtime();
        boolean stateChanged = (safetyEvaluation.getPrimaryState() != lastLoggedSafetyState);
        boolean intervalElapsed = (now - lastLogElapsedRealtime) >= TelemetryConfig.DB_LOG_INTERVAL_MS;

        if (!stateChanged && !intervalElapsed) {
            return;
        }

        lastLogElapsedRealtime = now;
        lastLoggedSafetyState = safetyEvaluation.getPrimaryState();

        FlightLogEntity entity = new FlightLogEntity();
        entity.timestampMillis = currentTimestampMillis;
        if (position != null) {
            entity.latitude = position.getLatitude();
            entity.longitude = position.getLongitude();
            entity.altitudeMeters = position.getAltitudeMeters();
            entity.groundSpeedMps = position.getGroundSpeedMps();
        }
        entity.batteryPercent = batteryHealth.getBatteryPercent();
        entity.packVoltageVolts = batteryHealth.getPackVoltage();
        entity.restingVoltageVolts = batteryHealth.getRestingVoltage();
        entity.currentAmps = batteryHealth.getCurrentAmps();
        entity.temperatureCelsius = batteryHealth.getTemperatureCelsius();
        entity.cellCount = batteryHealth.getCellHealth().getCellCount();
        entity.cellMinVolts = batteryHealth.getCellHealth().getMinCell();
        entity.cellMaxVolts = batteryHealth.getCellHealth().getMaxCell();
        entity.cellAverageVolts = batteryHealth.getCellHealth().getAverageCell();
        entity.cellDeltaVolts = batteryHealth.getCellHealth().getCellDelta();

        if (batteryHealth.getCellHealth().getCells() != null) {
            java.util.List<Double> vList = new java.util.ArrayList<>();
            for (com.agridrone.safety.data.model.BatteryCell c : batteryHealth.getCellHealth().getCells()) {
                vList.add(c.getVoltageVolts());
            }
            entity.cellVoltages = vList;
        }

        entity.consumptionRateMahPerMinute = batteryHealth.getConsumptionMahPerMinute();
        entity.dischargeRatePercentPerSecond = batteryHealth.getDischargeRatePercentPerSecond();
        entity.remainingFlightTimeSeconds = batteryHealth.getRemainingFlightTimeSeconds();

        if (rtlResult != null) {
            entity.distanceHomeMeters = rtlResult.getDistanceHomeMeters();
            entity.requiredRtlBatteryPercent = rtlResult.getRequiredBatteryPercent();
        }

        entity.safetyState = safetyEvaluation.getPrimaryState().name();
        entity.connectionState = connectionState != null ? connectionState.name() : ConnectionState.DISCONNECTED.name();
        entity.spraySafetyEnabled = spraySafetyEnabled;
        entity.sprayShutdownRequested = sprayShutdownRequested;

        executors.database().execute(() -> {
            try {
                dao.insert(entity);
            } catch (Exception e) {
                Log.e(TAG, "Failed to persist blackbox flight record", e);
            }
        });
    }
}
