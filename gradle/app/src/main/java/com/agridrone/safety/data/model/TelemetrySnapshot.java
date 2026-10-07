package com.agridrone.safety.data.model;

public final class TelemetrySnapshot {

    private final ConnectionState connectionState;
    private final TelemetryHealth telemetryHealth;
    private final BatteryTelemetry battery;
    private final PositionTelemetry position;
    private final HomePosition home;
    private final long timestampMillis;

    public TelemetrySnapshot(
            ConnectionState connectionState,
            TelemetryHealth telemetryHealth,
            BatteryTelemetry battery,
            PositionTelemetry position,
            HomePosition home,
            long timestampMillis) {
        this.connectionState = connectionState != null ? connectionState : ConnectionState.DISCONNECTED;
        this.telemetryHealth = telemetryHealth != null ? telemetryHealth : TelemetryHealth.WAITING;
        this.battery = battery != null ? battery : BatteryTelemetry.empty();
        this.position = position != null ? position : PositionTelemetry.empty();
        this.home = home != null ? home : HomePosition.unknown();
        this.timestampMillis = timestampMillis;
    }

    public static TelemetrySnapshot initial() {
        return new TelemetrySnapshot(
                ConnectionState.DISCONNECTED,
                TelemetryHealth.WAITING,
                BatteryTelemetry.empty(),
                PositionTelemetry.empty(),
                HomePosition.unknown(),
                System.currentTimeMillis()
        );
    }

    public ConnectionState getConnectionState() {
        return connectionState;
    }

    public TelemetryHealth getTelemetryHealth() {
        return telemetryHealth;
    }

    public BatteryTelemetry getBattery() {
        return battery;
    }

    public PositionTelemetry getPosition() {
        return position;
    }

    public HomePosition getHome() {
        return home;
    }

    public long getTimestampMillis() {
        return timestampMillis;
    }
}
