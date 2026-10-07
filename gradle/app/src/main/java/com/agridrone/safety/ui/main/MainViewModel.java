package com.agridrone.safety.ui.main;

import android.app.Application;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.agridrone.safety.config.TelemetryConfig;
import com.agridrone.safety.data.local.FlightLogDatabase;
import com.agridrone.safety.data.model.ConnectionState;
import com.agridrone.safety.data.model.SafetyState;
import com.agridrone.safety.data.model.TelemetryHealth;
import com.agridrone.safety.data.model.TelemetrySnapshot;
import com.agridrone.safety.data.repository.TelemetryRepository;
import com.agridrone.safety.data.source.ModeTelemetryDataSource;
import com.agridrone.safety.data.source.MockTelemetryDataSource;
import com.agridrone.safety.data.source.SkydroidTelemetryDataSource;
import com.agridrone.safety.domain.battery.BatteryAnalyzer;
import com.agridrone.safety.domain.battery.BatteryHealthResult;
import com.agridrone.safety.domain.rtl.RtlCalculator;
import com.agridrone.safety.domain.rtl.RtlResult;
import com.agridrone.safety.domain.safety.SafetyEngine;
import com.agridrone.safety.domain.safety.SafetyEvaluation;
import com.agridrone.safety.logging.FlightLogger;
import com.agridrone.safety.system.alerts.DroneAlertController;
import com.agridrone.safety.system.spray.MockSprayControlGateway;
import com.agridrone.safety.system.spray.SprayControlGateway;
import com.agridrone.safety.system.spray.SprayInterlockController;
import com.agridrone.safety.util.AppExecutors;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class MainViewModel extends AndroidViewModel {

    private final TelemetryRepository repository;
    private final ModeTelemetryDataSource sourceRouter;
    private final BatteryAnalyzer batteryAnalyzer;
    private final RtlCalculator rtlCalculator;
    private final SafetyEngine safetyEngine;
    private final DroneAlertController alertController;
    private final SprayControlGateway sprayGateway;
    private final SprayInterlockController sprayInterlock;
    private final FlightLogger flightLogger;
    private final Observer<TelemetrySnapshot> telemetrySnapshotObserver = this::processSnapshot;
    private final Observer<ConnectionState> connectionStateObserver = this::processConnectionState;
    private final Observer<String> connectionErrorObserver = this::processConnectionError;
    private final Observer<String> connectionStatusObserver = this::processConnectionStatus;

    private final MutableLiveData<MainUiState> uiStateLiveData = new MutableLiveData<>(MainUiState.initial());

    private long lastUiPostElapsedRealtime = 0L;
    private volatile boolean hasEverConnected;
    private volatile boolean dashboardSessionActive;
    private volatile ConnectionMode connectionMode = ConnectionMode.UNSELECTED;
    private ScheduledFuture<?> telemetryAgeTicker;

    public MainViewModel(@NonNull Application application) {
        super(application);

        MockTelemetryDataSource mockSource = new MockTelemetryDataSource();
        SkydroidTelemetryDataSource realTimeSource = new SkydroidTelemetryDataSource(application);
        this.sourceRouter = new ModeTelemetryDataSource(mockSource, realTimeSource);
        this.repository = TelemetryRepository.getInstance(sourceRouter);

        this.batteryAnalyzer = new BatteryAnalyzer();
        this.rtlCalculator = new RtlCalculator();
        this.safetyEngine = new SafetyEngine();
        this.alertController = new DroneAlertController(application);
        this.sprayGateway = new MockSprayControlGateway();
        this.sprayInterlock = new SprayInterlockController(sprayGateway);

        FlightLogDatabase db = FlightLogDatabase.getInstance(application);
        this.flightLogger = new FlightLogger(db.flightLogDao());

        repository.getTelemetrySnapshotLiveData().observeForever(telemetrySnapshotObserver);
        repository.getConnectionStateLiveData().observeForever(connectionStateObserver);
        repository.getConnectionErrorLiveData().observeForever(connectionErrorObserver);
        repository.getConnectionStatusLiveData().observeForever(connectionStatusObserver);
    }

    public LiveData<MainUiState> getUiStateLiveData() {
        return uiStateLiveData;
    }

    public void startDemoMode() {
        if (repository.isConnected()) {
            repository.disconnect();
        }
        sourceRouter.select(sourceRouter.getDemoSource());
        connectionMode = ConnectionMode.DEMO;
        dashboardSessionActive = false;
        resetLandingStateForMode(connectionMode);
        resetSafetySession();
        connectToDrone();
    }

    public void startRealTimeMode() {
        MainUiState current = uiStateLiveData.getValue();
        if (connectionMode == ConnectionMode.REAL_TIME
                && current != null
                && current.getConnectionState() == ConnectionState.CONNECTING) {
            disconnectFromDrone();
            return;
        }
        if (repository.isConnected()) {
            repository.disconnect();
        }
        sourceRouter.select(sourceRouter.getRealTimeSource());
        connectionMode = ConnectionMode.REAL_TIME;
        dashboardSessionActive = false;
        resetLandingStateForMode(connectionMode);
        resetSafetySession();
        connectToDrone();
    }

    public void connectToDrone() {
        if (connectionMode == ConnectionMode.UNSELECTED) {
            return;
        }
        MainUiState current = uiStateLiveData.getValue();
        if (current != null && (current.getConnectionState() == ConnectionState.CONNECTING
                || current.getConnectionState() == ConnectionState.CONNECTED)) {
            return;
        }
        resetSafetySession();
        repository.connect();
    }

    private void resetSafetySession() {
        batteryAnalyzer.resetEstimator();
        safetyEngine.reset();
        sprayInterlock.reset();
    }

    private void resetLandingStateForMode(ConnectionMode mode) {
        stopTelemetryAgeTicker();
        MainUiState current = uiStateLiveData.getValue();
        if (current == null) {
            current = MainUiState.initial();
        }
        uiStateLiveData.setValue(new MainUiState(
                ConnectionState.DISCONNECTED,
                TelemetryHealth.WAITING,
                null,
                current.getBatteryHealth(),
                current.getRtlResult(),
                current.getSafetyEvaluation(),
                current.isSpraySafetyEnabled(),
                current.isSprayCutoffDispatched(),
                System.currentTimeMillis(),
                hasEverConnected,
                false,
                false,
                mode,
                null
        ));
    }

    public void disconnectFromDrone() {
        dashboardSessionActive = false;
        connectionMode = ConnectionMode.UNSELECTED;
        stopTelemetryAgeTicker();
        MainUiState current = uiStateLiveData.getValue();
        if (current != null) {
            uiStateLiveData.postValue(new MainUiState(
                    ConnectionState.DISCONNECTED,
                    com.agridrone.safety.data.model.TelemetryHealth.WAITING,
                    null,
                    current.getBatteryHealth(),
                    current.getRtlResult(),
                    current.getSafetyEvaluation(),
                    current.isSpraySafetyEnabled(),
                    current.isSprayCutoffDispatched(),
                    System.currentTimeMillis(),
                    hasEverConnected,
                    false,
                    false,
                    connectionMode,
                    null));
        }
        repository.disconnect();
    }

    public void minimizeCriticalAlert() {
        MainUiState current = uiStateLiveData.getValue();
        if (current == null
                || !current.getSafetyEvaluation().isHazardous()
                || current.getSafetyEvaluation().getPrimaryState() == SafetyState.EMERGENCY) {
            return;
        }

        uiStateLiveData.postValue(copyState(current, true));
    }

    public void setSpraySafetyEnabled(boolean enabled) {
        sprayInterlock.setSpraySafetyEnabled(enabled);
        MainUiState current = uiStateLiveData.getValue();
        if (current != null) {
            uiStateLiveData.postValue(new MainUiState(
                    current.getConnectionState(),
                    current.getTelemetryHealth(),
                    current.getConnectionErrorMessage(),
                    current.getBatteryHealth(),
                    current.getRtlResult(),
                    current.getSafetyEvaluation(),
                    enabled,
                    sprayInterlock.isCutoffDispatched(),
                    System.currentTimeMillis(),
                    hasEverConnected,
                    dashboardSessionActive,
                    current.isCriticalAlertMinimized(),
                    connectionMode,
                    current.getConnectionStatusMessage()
            ));
        }
    }

    public void setMockScenario(MockTelemetryDataSource.Scenario scenario) {
        if (connectionMode != ConnectionMode.DEMO || scenario == null) {
            return;
        }
        resetSafetySession();
        repository.setMockScenario(scenario);
    }

    private void processSnapshot(TelemetrySnapshot snapshot) {
        if (snapshot == null) return;

        AppExecutors.getInstance().telemetry().execute(() -> {
            BatteryHealthResult batteryHealth = batteryAnalyzer.analyze(snapshot.getBattery());

            RtlResult rtlResult = rtlCalculator.calculate(
                    snapshot.getPosition(),
                    snapshot.getHome(),
                    batteryHealth.getBatteryPercent(),
                    batteryHealth.getDischargeRatePercentPerSecond()
            );

            SafetyEvaluation safety = safetyEngine.evaluate(
                    batteryHealth,
                    rtlResult,
                    snapshot.getTimestampMillis()
            );

            sprayInterlock.evaluate(batteryHealth.getBatteryPercent());

            alertController.onSafetyEvaluation(
                    safety,
                    batteryHealth.getBatteryPercent(),
                    batteryHealth.getCellHealth().getAverageCell(),
                    snapshot.getTimestampMillis()
            );

            flightLogger.logSample(
                    snapshot.getConnectionState(),
                    snapshot.getPosition(),
                    batteryHealth,
                    rtlResult,
                    safety,
                    sprayInterlock.isSpraySafetyEnabled(),
                    sprayInterlock.isCutoffDispatched(),
                    snapshot.getTimestampMillis()
            );

            long now = SystemClock.elapsedRealtime();
            boolean isCriticalOrEmergency = safety.isHazardous();
            boolean throttleElapsed = (now - lastUiPostElapsedRealtime) >= TelemetryConfig.UI_THROTTLE_MS;

            if (throttleElapsed || isCriticalOrEmergency) {
                lastUiPostElapsedRealtime = now;
                if (snapshot.getConnectionState() == ConnectionState.CONNECTED && repository.isConnected()) {
                    hasEverConnected = true;
                    dashboardSessionActive = true;
                }
                MainUiState current = uiStateLiveData.getValue();
                boolean criticalAlertMinimized = current != null
                        && current.isCriticalAlertMinimized()
                        && current.getSafetyEvaluation().getPrimaryState() == safety.getPrimaryState()
                        && safety.getPrimaryState() != SafetyState.EMERGENCY;
                MainUiState newState = new MainUiState(
                        snapshot.getConnectionState(),
                        snapshot.getTelemetryHealth(),
                        null,
                        batteryHealth,
                        rtlResult,
                        safety,
                        sprayInterlock.isSpraySafetyEnabled(),
                        sprayInterlock.isCutoffDispatched(),
                        snapshot.getTimestampMillis(),
                        hasEverConnected,
                        dashboardSessionActive,
                        criticalAlertMinimized,
                        connectionMode,
                        current == null ? null : current.getConnectionStatusMessage()
                );
                uiStateLiveData.postValue(newState);
                updateTelemetryAgeTicker(snapshot.getTelemetryHealth());
            }
        });
    }

    private void processConnectionState(ConnectionState state) {
        MainUiState current = uiStateLiveData.getValue();
        ConnectionState effectiveState = state;
        if (state == ConnectionState.CONNECTED) {
            hasEverConnected = true;
            if (repository.isConnected()) {
                dashboardSessionActive = true;
            } else {
                effectiveState = ConnectionState.DISCONNECTED;
            }
        }
        if (current != null && current.getConnectionState() != effectiveState) {
            uiStateLiveData.postValue(new MainUiState(
                    effectiveState,
                    effectiveState == ConnectionState.CONNECTED
                            ? current.getTelemetryHealth()
                            : dashboardSessionActive
                                    ? com.agridrone.safety.data.model.TelemetryHealth.LOST
                                    : com.agridrone.safety.data.model.TelemetryHealth.WAITING,
                    current.getConnectionErrorMessage(),
                    current.getBatteryHealth(),
                    current.getRtlResult(),
                    current.getSafetyEvaluation(),
                    current.isSpraySafetyEnabled(),
                    current.isSprayCutoffDispatched(),
                    System.currentTimeMillis(),
                    hasEverConnected,
                    dashboardSessionActive,
                    current.isCriticalAlertMinimized(),
                    connectionMode,
                    current.getConnectionStatusMessage()
            ));
            updateTelemetryAgeTicker(effectiveState == ConnectionState.CONNECTED
                    ? current.getTelemetryHealth()
                    : dashboardSessionActive ? TelemetryHealth.LOST : TelemetryHealth.WAITING);
        }
    }

    private void processConnectionError(String error) {
        if (error != null) {
            MainUiState current = uiStateLiveData.getValue();
            if (current != null) {
                uiStateLiveData.postValue(new MainUiState(
                        ConnectionState.ERROR,
                        current.getTelemetryHealth(),
                        error,
                        current.getBatteryHealth(),
                        current.getRtlResult(),
                        current.getSafetyEvaluation(),
                        current.isSpraySafetyEnabled(),
                        current.isSprayCutoffDispatched(),
                        System.currentTimeMillis(),
                        hasEverConnected,
                        dashboardSessionActive,
                        current.isCriticalAlertMinimized(),
                        connectionMode,
                        current.getConnectionStatusMessage()
                ));
                updateTelemetryAgeTicker(current.getTelemetryHealth());
            }
        }
    }

    private MainUiState copyState(MainUiState state, boolean criticalAlertMinimized) {
        return new MainUiState(
                state.getConnectionState(),
                state.getTelemetryHealth(),
                state.getConnectionErrorMessage(),
                state.getBatteryHealth(),
                state.getRtlResult(),
                state.getSafetyEvaluation(),
                state.isSpraySafetyEnabled(),
                state.isSprayCutoffDispatched(),
                state.getLastUpdateTimestampMillis(),
                state.hasEverConnected(),
                state.isDashboardSessionActive(),
                criticalAlertMinimized,
                connectionMode,
                state.getConnectionStatusMessage());
    }

    private void processConnectionStatus(String status) {
        MainUiState current = uiStateLiveData.getValue();
        if (current == null) {
            return;
        }
        uiStateLiveData.postValue(new MainUiState(
                connectionMode == ConnectionMode.REAL_TIME
                        && current.getConnectionState() == ConnectionState.DISCONNECTED
                        && status != null
                        ? ConnectionState.CONNECTING
                        : current.getConnectionState(),
                current.getTelemetryHealth(),
                current.getConnectionErrorMessage(),
                current.getBatteryHealth(),
                current.getRtlResult(),
                current.getSafetyEvaluation(),
                current.isSpraySafetyEnabled(),
                current.isSprayCutoffDispatched(),
                current.getLastUpdateTimestampMillis(),
                hasEverConnected,
                dashboardSessionActive,
                current.isCriticalAlertMinimized(),
                connectionMode,
                status
        ));
    }

    private synchronized void updateTelemetryAgeTicker(TelemetryHealth health) {
        boolean needsTicker = health == TelemetryHealth.STALE || health == TelemetryHealth.LOST;
        if (needsTicker && telemetryAgeTicker == null) {
            telemetryAgeTicker = AppExecutors.getInstance().scheduled().scheduleAtFixedRate(
                    this::publishTelemetryAgeTick,
                    1,
                    1,
                    TimeUnit.SECONDS);
        } else if (!needsTicker) {
            stopTelemetryAgeTicker();
        }
    }

    private void publishTelemetryAgeTick() {
        MainUiState current = uiStateLiveData.getValue();
        if (current == null
                || (current.getTelemetryHealth() != TelemetryHealth.STALE
                && current.getTelemetryHealth() != TelemetryHealth.LOST)) {
            return;
        }
        uiStateLiveData.postValue(copyState(current, current.isCriticalAlertMinimized()));
    }

    private synchronized void stopTelemetryAgeTicker() {
        if (telemetryAgeTicker != null) {
            telemetryAgeTicker.cancel(true);
            telemetryAgeTicker = null;
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        stopTelemetryAgeTicker();
        repository.getTelemetrySnapshotLiveData().removeObserver(telemetrySnapshotObserver);
        repository.getConnectionStateLiveData().removeObserver(connectionStateObserver);
        repository.getConnectionErrorLiveData().removeObserver(connectionErrorObserver);
        repository.getConnectionStatusLiveData().removeObserver(connectionStatusObserver);
        repository.disconnect();
        alertController.release();
    }
}
