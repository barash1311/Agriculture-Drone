package com.agridrone.safety.data.source;

import android.os.SystemClock;
import android.util.Log;

import com.agridrone.safety.config.MockConfig;
import com.agridrone.safety.data.model.BatteryTelemetry;
import com.agridrone.safety.data.model.ConnectionState;
import com.agridrone.safety.data.model.HomePosition;
import com.agridrone.safety.data.model.PositionTelemetry;
import com.agridrone.safety.data.model.TelemetryHealth;
import com.agridrone.safety.data.model.TelemetrySnapshot;
import com.agridrone.safety.util.AppExecutors;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class MockTelemetryDataSource implements TelemetryDataSource {

    private static final String TAG = "MockTelemetryDataSource";

    public enum Scenario {
        NORMAL,
        NOTICE,
        WARNING_BATTERY,
        WARNING_CELL,
        CELL_IMBALANCE,
        CRITICAL_RTL,
        CRITICAL_CELL,
        EMERGENCY_CELL,
        RAPID_SAG,
        TELEMETRY_LOST
    }

    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private TelemetryListener listener;
    private ScheduledFuture<?> simulationTask;

    private volatile Scenario currentScenario = Scenario.NORMAL;

    private double currentBatteryPercent = 72.0;
    private double currentPackVoltage = 50.8;
    private double currentAmps = 42.6;
    private double currentTempCelsius = 37.0;
    private double consumedMah = 6160.0;
    private final int cellCount = MockConfig.DEFAULT_CELL_COUNT; // 12S

    // Coordinates: Home at baseline, aircraft moves outward
    private double aircraftLat = MockConfig.DEFAULT_HOME_LAT + 0.0065; // ~720m away initially
    private double aircraftLon = MockConfig.DEFAULT_HOME_LON + 0.0050;
    private double aircraftAltMeters = 55.0;
    private double groundSpeedMps = 10.0;

    private long lastTickElapsedRealtime = 0L;

    public synchronized void setScenario(Scenario scenario) {
        this.currentScenario = scenario;
        Log.i(TAG, "Switched mock scenario to: " + scenario);

        switch (scenario) {
            case NORMAL:
                currentBatteryPercent = 72.0;
                currentPackVoltage = 50.8;
                aircraftLat = MockConfig.DEFAULT_HOME_LAT + 0.0065;
                aircraftLon = MockConfig.DEFAULT_HOME_LON + 0.0050;
                break;
            case NOTICE:
                currentBatteryPercent = 29.5; // <= 30.0%
                currentPackVoltage = 45.6;
                break;
            case WARNING_BATTERY:
                currentBatteryPercent = 19.5; // <= 20.0%
                currentPackVoltage = 44.4;
                break;
            case WARNING_CELL:
                currentBatteryPercent = 45.0;
                currentPackVoltage = 45.0;
                break;
            case CELL_IMBALANCE:
                currentBatteryPercent = 55.0;
                currentPackVoltage = 46.5;
                break;
            case CRITICAL_RTL:
                currentBatteryPercent = 21.0;
                // Move aircraft far out so required RTL battery > 21% (e.g. ~2400m distance)
                aircraftLat = MockConfig.DEFAULT_HOME_LAT + 0.0210;
                aircraftLon = MockConfig.DEFAULT_HOME_LON + 0.0180;
                break;
            case CRITICAL_CELL:
                currentBatteryPercent = 35.0;
                currentPackVoltage = 43.5;
                break;
            case EMERGENCY_CELL:
                currentBatteryPercent = 25.0;
                currentPackVoltage = 41.5;
                break;
            case RAPID_SAG:
                currentBatteryPercent = 60.0;
                currentPackVoltage = 40.0;
                break;
            case TELEMETRY_LOST:
                break;
        }
    }

    public Scenario getCurrentScenario() {
        return currentScenario;
    }

    @Override
    public synchronized void setListener(TelemetryListener listener) {
        this.listener = listener;
    }

    @Override
    public synchronized void start() {
        if (isRunning.getAndSet(true)) {
            return;
        }

        Log.i(TAG, "Starting MockTelemetryDataSource");

        AppExecutors.getInstance().scheduled().schedule(() -> {
            if (!isRunning.get()) return;

            if (listener != null) {
                listener.onConnected();
            }

            lastTickElapsedRealtime = SystemClock.elapsedRealtime();

            simulationTask = AppExecutors.getInstance().scheduled().scheduleAtFixedRate(
                    this::simulationTick,
                    0,
                    MockConfig.MOCK_TICK_INTERVAL_MS,
                    TimeUnit.MILLISECONDS
            );
        }, 350, TimeUnit.MILLISECONDS);
    }

    @Override
    public synchronized void stop() {
        if (!isRunning.getAndSet(false)) {
            return;
        }

        Log.i(TAG, "Stopping MockTelemetryDataSource");

        if (simulationTask != null) {
            simulationTask.cancel(true);
            simulationTask = null;
        }

        if (listener != null) {
            listener.onDisconnected();
        }
    }

    @Override
    public boolean isRunning() {
        return isRunning.get();
    }

    private void simulationTick() {
        if (!isRunning.get() || listener == null) {
            return;
        }

        if (currentScenario == Scenario.TELEMETRY_LOST) {
            return;
        }

        long nowElapsed = SystemClock.elapsedRealtime();
        long deltaMs = lastTickElapsedRealtime > 0 ? (nowElapsed - lastTickElapsedRealtime) : MockConfig.MOCK_TICK_INTERVAL_MS;
        lastTickElapsedRealtime = nowElapsed;

        if (currentScenario == Scenario.NORMAL) {
            double drainPercent = (deltaMs / 1000.0) * 0.05; // 0.05% per sec
            currentBatteryPercent = Math.max(1.0, currentBatteryPercent - drainPercent);
            consumedMah += (currentAmps * (deltaMs / 3600000.0)) * 1000.0;
            currentPackVoltage = 44.0 + (currentBatteryPercent / 100.0) * (50.4 - 44.0);

            aircraftLat += 0.000008;
            aircraftLon += 0.000008;
        }

        List<Double> cellVoltages = generateCellVoltages();

        double minCell = Double.MAX_VALUE;
        for (double v : cellVoltages) {
            if (v < minCell) minCell = v;
        }

        long timestamp = System.currentTimeMillis();

        BatteryTelemetry batteryTelemetry = new BatteryTelemetry(
                currentBatteryPercent,
                currentPackVoltage,
                currentAmps,
                currentTempCelsius,
                consumedMah,
                Math.max(0.0, MockConfig.DEFAULT_NOMINAL_CAPACITY_MAH - consumedMah),
                MockConfig.DEFAULT_NOMINAL_CAPACITY_MAH,
                "LiPo 12S",
                cellVoltages,
                timestamp
        );

        PositionTelemetry positionTelemetry = new PositionTelemetry(
                aircraftLat,
                aircraftLon,
                aircraftAltMeters,
                groundSpeedMps,
                timestamp
        );

        HomePosition homePosition = new HomePosition(
                MockConfig.DEFAULT_HOME_LAT,
                MockConfig.DEFAULT_HOME_LON,
                MockConfig.DEFAULT_HOME_ALT_METERS,
                true
        );

        TelemetrySnapshot snapshot = new TelemetrySnapshot(
                ConnectionState.CONNECTED,
                TelemetryHealth.LIVE,
                batteryTelemetry,
                positionTelemetry,
                homePosition,
                timestamp
        );

        listener.onTelemetrySnapshot(snapshot);
    }

    private List<Double> generateCellVoltages() {
        List<Double> cells = new ArrayList<>(cellCount);
        double nominalCell = currentPackVoltage / cellCount;

        for (int i = 0; i < cellCount; i++) {
            // Introduce subtle baseline variation between cells (+-0.01V)
            double cellV = nominalCell + ((i % 3 == 0) ? 0.01 : ((i % 3 == 1) ? -0.01 : 0.0));

            if (currentScenario == Scenario.CELL_IMBALANCE) {
                if (i == 6) { // Weak Cell C7: delta = 3.82 - 3.70 = 0.12V > 0.08V
                    cellV = 3.70;
                } else {
                    cellV = 3.82;
                }
            } else if (currentScenario == Scenario.WARNING_CELL) {
                if (i == 2) {
                    cellV = 3.64; // <= 3.65V
                } else {
                    cellV = 3.78;
                }
            } else if (currentScenario == Scenario.CRITICAL_CELL) {
                if (i == 4) {
                    cellV = 3.49; // <= 3.50V
                } else {
                    cellV = 3.68;
                }
            } else if (currentScenario == Scenario.EMERGENCY_CELL) {
                if (i == 5) {
                    cellV = 3.38; // <= 3.40V
                } else {
                    cellV = 3.60;
                }
            }

            cells.add(Math.round(cellV * 100.0) / 100.0);
        }
        return cells;
    }
}
