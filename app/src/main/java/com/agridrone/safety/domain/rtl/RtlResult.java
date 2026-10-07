package com.agridrone.safety.domain.rtl;

public final class RtlResult {

    public enum Status {
        SAFE,
        CRITICAL_RTL,
        UNAVAILABLE
    }

    private final Status status;
    private final Double distanceHomeMeters;
    private final Double cruiseSpeedMps;
    private final Long estimatedReturnSeconds;
    private final Double dischargeRatePercentPerSecond;
    private final Double energyPercentNeeded;
    private final double safetyMarginPercent; // 15%
    private final Double requiredBatteryPercent;
    private final Double currentBatteryPercent;
    private final boolean isHomeKnown;

    public RtlResult(
            Status status,
            Double distanceHomeMeters,
            Double cruiseSpeedMps,
            Long estimatedReturnSeconds,
            Double dischargeRatePercentPerSecond,
            Double energyPercentNeeded,
            double safetyMarginPercent,
            Double requiredBatteryPercent,
            Double currentBatteryPercent,
            boolean isHomeKnown) {
        this.status = status;
        this.distanceHomeMeters = distanceHomeMeters;
        this.cruiseSpeedMps = cruiseSpeedMps;
        this.estimatedReturnSeconds = estimatedReturnSeconds;
        this.dischargeRatePercentPerSecond = dischargeRatePercentPerSecond;
        this.energyPercentNeeded = energyPercentNeeded;
        this.safetyMarginPercent = safetyMarginPercent;
        this.requiredBatteryPercent = requiredBatteryPercent;
        this.currentBatteryPercent = currentBatteryPercent;
        this.isHomeKnown = isHomeKnown;
    }

    public static RtlResult unavailable(boolean isHomeKnown) {
        return new RtlResult(
                Status.UNAVAILABLE,
                null, null, null, null, null,
                15.0, null, null,
                isHomeKnown
        );
    }

    public boolean isCritical() {
        return status == Status.CRITICAL_RTL;
    }

    public Status getStatus() {
        return status;
    }

    public Double getDistanceHomeMeters() {
        return distanceHomeMeters;
    }

    public Double getCruiseSpeedMps() {
        return cruiseSpeedMps;
    }

    public Long getEstimatedReturnSeconds() {
        return estimatedReturnSeconds;
    }

    public Double getDischargeRatePercentPerSecond() {
        return dischargeRatePercentPerSecond;
    }

    public Double getEnergyPercentNeeded() {
        return energyPercentNeeded;
    }

    public double getSafetyMarginPercent() {
        return safetyMarginPercent;
    }

    public Double getRequiredBatteryPercent() {
        return requiredBatteryPercent;
    }

    public Double getCurrentBatteryPercent() {
        return currentBatteryPercent;
    }

    public boolean isHomeKnown() {
        return isHomeKnown;
    }
}
