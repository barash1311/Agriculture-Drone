package com.agridrone.safety.data.model;

public final class PositionTelemetry {

    private final Double latitude;
    private final Double longitude;
    private final Double altitudeMeters;
    private final Double groundSpeedMps;
    private final long timestampMillis;

    public PositionTelemetry(Double latitude, Double longitude, Double altitudeMeters, Double groundSpeedMps, long timestampMillis) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitudeMeters = altitudeMeters;
        this.groundSpeedMps = groundSpeedMps;
        this.timestampMillis = timestampMillis;
    }

    public static PositionTelemetry empty() {
        return new PositionTelemetry(null, null, null, null, 0L);
    }

    public boolean hasCoordinates() {
        return latitude != null && longitude != null && !Double.isNaN(latitude) && !Double.isNaN(longitude);
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public Double getAltitudeMeters() {
        return altitudeMeters;
    }

    public Double getGroundSpeedMps() {
        return groundSpeedMps;
    }

    public long getTimestampMillis() {
        return timestampMillis;
    }
}
