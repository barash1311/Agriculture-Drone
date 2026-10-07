package com.agridrone.safety.data.source;

import com.agridrone.safety.data.model.TelemetrySnapshot;

public final class ModeTelemetryDataSource implements TelemetryDataSource {

    private final TelemetryDataSource demoSource;
    private final TelemetryDataSource realTimeSource;
    private volatile TelemetryDataSource activeSource;
    private volatile TelemetryListener listener;

    public ModeTelemetryDataSource(
            TelemetryDataSource demoSource,
            TelemetryDataSource realTimeSource) {
        if (demoSource == null || realTimeSource == null) {
            throw new IllegalArgumentException("Both telemetry sources are required");
        }
        this.demoSource = demoSource;
        this.realTimeSource = realTimeSource;
        this.activeSource = demoSource;
        installListener(demoSource);
    }

    public synchronized void select(TelemetryDataSource source) {
        if (source != demoSource && source != realTimeSource) {
            throw new IllegalArgumentException("Unknown telemetry source");
        }
        if (activeSource.isRunning()) {
            throw new IllegalStateException("Stop telemetry before changing its source");
        }
        activeSource = source;
        installListener(source);
    }

    public TelemetryDataSource getDemoSource() {
        return demoSource;
    }

    public TelemetryDataSource getRealTimeSource() {
        return realTimeSource;
    }

    public void setDemoScenario(MockTelemetryDataSource.Scenario scenario) {
        if (activeSource == demoSource && demoSource instanceof MockTelemetryDataSource) {
            ((MockTelemetryDataSource) demoSource).setScenario(scenario);
        }
    }

    @Override
    public void start() {
        activeSource.start();
    }

    @Override
    public void stop() {
        activeSource.stop();
    }

    @Override
    public boolean isRunning() {
        return activeSource.isRunning();
    }

    @Override
    public void setListener(TelemetryListener listener) {
        this.listener = listener;
        installListener(activeSource);
    }

    private void installListener(TelemetryDataSource source) {
        source.setListener(new TelemetryListener() {
            @Override
            public void onConnected() {
                if (activeSource == source && listener != null) {
                    listener.onConnected();
                }
            }

            @Override
            public void onDisconnected() {
                if (activeSource == source && listener != null) {
                    listener.onDisconnected();
                }
            }

            @Override
            public void onTelemetrySnapshot(TelemetrySnapshot snapshot) {
                if (activeSource == source && listener != null) {
                    listener.onTelemetrySnapshot(snapshot);
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (activeSource == source && listener != null) {
                    listener.onError(errorMessage);
                }
            }

            @Override
            public void onStatus(String statusMessage) {
                if (activeSource == source && listener != null) {
                    listener.onStatus(statusMessage);
                }
            }
        });
    }
}
