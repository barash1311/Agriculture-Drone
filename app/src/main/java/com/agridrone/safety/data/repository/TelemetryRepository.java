package com.agridrone.safety.data.repository;

import android.os.SystemClock;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.agridrone.safety.config.TelemetryConfig;
import com.agridrone.safety.data.model.ConnectionState;
import com.agridrone.safety.data.model.TelemetryHealth;
import com.agridrone.safety.data.model.TelemetrySnapshot;
import com.agridrone.safety.data.source.MockTelemetryDataSource;
import com.agridrone.safety.data.source.ModeTelemetryDataSource;
import com.agridrone.safety.data.source.TelemetryDataSource;
import com.agridrone.safety.data.source.TelemetryListener;
import com.agridrone.safety.util.AppExecutors;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class TelemetryRepository implements TelemetryListener {

    private static volatile TelemetryRepository sInstance;

    private final TelemetryDataSource dataSource;
    private final MutableLiveData<TelemetrySnapshot> telemetrySnapshotLiveData = new MutableLiveData<>(TelemetrySnapshot.initial());
    private final MutableLiveData<ConnectionState> connectionStateLiveData = new MutableLiveData<>(ConnectionState.DISCONNECTED);
    private final MutableLiveData<String> connectionErrorLiveData = new MutableLiveData<>(null);
    private final MutableLiveData<String> connectionStatusLiveData = new MutableLiveData<>(null);

    private volatile TelemetrySnapshot latestSnapshot = TelemetrySnapshot.initial();
    private volatile long lastTelemetryTimestamp = 0L;
    private ScheduledFuture<?> stalenessWatchdog;

    public TelemetryRepository(TelemetryDataSource dataSource) {
        this.dataSource = dataSource;
        this.dataSource.setListener(this);
    }

    public static TelemetryRepository getInstance(TelemetryDataSource dataSource) {
        if (sInstance == null) {
            synchronized (TelemetryRepository.class) {
                if (sInstance == null) {
                    sInstance = new TelemetryRepository(dataSource);
                }
            }
        }
        return sInstance;
    }

    public LiveData<TelemetrySnapshot> getTelemetrySnapshotLiveData() {
        return telemetrySnapshotLiveData;
    }

    public LiveData<ConnectionState> getConnectionStateLiveData() {
        return connectionStateLiveData;
    }

    public LiveData<String> getConnectionErrorLiveData() {
        return connectionErrorLiveData;
    }

    public LiveData<String> getConnectionStatusLiveData() {
        return connectionStatusLiveData;
    }

    public void connect() {
        lastTelemetryTimestamp = 0L;
        connectionStateLiveData.postValue(ConnectionState.CONNECTING);
        connectionErrorLiveData.postValue(null);
        connectionStatusLiveData.postValue(null);
        startStalenessWatchdog();
        dataSource.start();
    }

    public void disconnect() {
        dataSource.stop();
        stopStalenessWatchdog();
        connectionStateLiveData.postValue(ConnectionState.DISCONNECTED);
        connectionStatusLiveData.postValue(null);
        lastTelemetryTimestamp = 0L;

        latestSnapshot = new TelemetrySnapshot(
                ConnectionState.DISCONNECTED,
                TelemetryHealth.WAITING,
                latestSnapshot.getBattery(),
                latestSnapshot.getPosition(),
                latestSnapshot.getHome(),
                System.currentTimeMillis()
        );
        telemetrySnapshotLiveData.postValue(latestSnapshot);
    }

    public boolean isConnected() {
        return dataSource.isRunning();
    }

    public TelemetryDataSource getDataSource() {
        return dataSource;
    }

    public void setMockScenario(MockTelemetryDataSource.Scenario scenario) {
        if (dataSource instanceof ModeTelemetryDataSource) {
            ((ModeTelemetryDataSource) dataSource).setDemoScenario(scenario);
        } else if (dataSource instanceof MockTelemetryDataSource) {
            ((MockTelemetryDataSource) dataSource).setScenario(scenario);
        }
    }

    @Override
    public void onConnected() {
        connectionStateLiveData.postValue(ConnectionState.CONNECTED);
    }

    @Override
    public void onDisconnected() {
        connectionStateLiveData.postValue(ConnectionState.DISCONNECTED);
    }

    @Override
    public void onTelemetrySnapshot(TelemetrySnapshot snapshot) {
        lastTelemetryTimestamp = SystemClock.elapsedRealtime();

        latestSnapshot = new TelemetrySnapshot(
                ConnectionState.CONNECTED,
                TelemetryHealth.LIVE,
                snapshot.getBattery(),
                snapshot.getPosition(),
                snapshot.getHome(),
                snapshot.getTimestampMillis()
        );
        telemetrySnapshotLiveData.postValue(latestSnapshot);
    }

    @Override
    public void onError(String errorMessage) {
        connectionStateLiveData.postValue(ConnectionState.ERROR);
        connectionErrorLiveData.postValue(errorMessage);
    }

    @Override
    public void onStatus(String statusMessage) {
        connectionStatusLiveData.postValue(statusMessage);
    }

    private synchronized void startStalenessWatchdog() {
        stopStalenessWatchdog();
        stalenessWatchdog = AppExecutors.getInstance().scheduled().scheduleAtFixedRate(
                this::evaluateStaleness,
                1000,
                500,
                TimeUnit.MILLISECONDS
        );
    }

    private synchronized void stopStalenessWatchdog() {
        if (stalenessWatchdog != null) {
            stalenessWatchdog.cancel(true);
            stalenessWatchdog = null;
        }
    }

    private void evaluateStaleness() {
        if (!dataSource.isRunning() || lastTelemetryTimestamp == 0L) {
            return;
        }

        long elapsedSinceLast = SystemClock.elapsedRealtime() - lastTelemetryTimestamp;
        TelemetryHealth newHealth = latestSnapshot.getTelemetryHealth();

        if (elapsedSinceLast >= TelemetryConfig.TELEMETRY_LOST_TIMEOUT_MS) {
            newHealth = TelemetryHealth.LOST;
        } else if (elapsedSinceLast >= TelemetryConfig.TELEMETRY_STALE_TIMEOUT_MS) {
            newHealth = TelemetryHealth.STALE;
        }

        if (newHealth != latestSnapshot.getTelemetryHealth()) {
            latestSnapshot = new TelemetrySnapshot(
                    latestSnapshot.getConnectionState(),
                    newHealth,
                    latestSnapshot.getBattery(),
                    latestSnapshot.getPosition(),
                    latestSnapshot.getHome(),
                    latestSnapshot.getTimestampMillis()
            );
            telemetrySnapshotLiveData.postValue(latestSnapshot);
        }
    }
}
