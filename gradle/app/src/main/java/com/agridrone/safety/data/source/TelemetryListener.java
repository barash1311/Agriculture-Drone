package com.agridrone.safety.data.source;

import com.agridrone.safety.data.model.TelemetrySnapshot;

public interface TelemetryListener {

    void onConnected();

    void onDisconnected();

    void onTelemetrySnapshot(TelemetrySnapshot snapshot);

    void onError(String errorMessage);

    default void onStatus(String statusMessage) {
    }
}
