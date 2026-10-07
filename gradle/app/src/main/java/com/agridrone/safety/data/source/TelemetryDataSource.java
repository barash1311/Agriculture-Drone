package com.agridrone.safety.data.source;

public interface TelemetryDataSource {

    /** Starts receiving or simulating telemetry stream */
    void start();

    /** Stops receiving telemetry stream and releases background resources */
    void stop();

    /** Returns true if the data source pipeline is actively running */
    boolean isRunning();

    /** Sets the telemetry event callback listener */
    void setListener(TelemetryListener listener);
}
