package com.agridrone.safety.data.model;

public final class HomePosition {

    private final Double latitude;
    private final Double longitude;
    private final Double altitudeMeters;
    private final boolean isSet;

    public HomePosition(Double latitude, Double longitude, Double altitudeMeters, boolean isSet) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitudeMeters = altitudeMeters;
        this.isSet = isSet;
    }

    public static HomePosition unknown() {
        return new HomePosition(null, null, null, false);
    }

    public boolean isSet() {
        return isSet && latitude != null && longitude != null;
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
}
