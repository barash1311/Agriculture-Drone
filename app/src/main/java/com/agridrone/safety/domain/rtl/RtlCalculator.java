package com.agridrone.safety.domain.rtl;

import com.agridrone.safety.config.SafetyConfig;
import com.agridrone.safety.data.model.HomePosition;
import com.agridrone.safety.data.model.PositionTelemetry;
import com.agridrone.safety.domain.navigation.DistanceCalculator;

public final class RtlCalculator {

    private final double nominalCruiseSpeedMps;
    private final double fallbackDischargeRatePercentPerSec;

    public RtlCalculator() {
        this(SafetyConfig.DEFAULT_CRUISE_SPEED_MPS, 0.05); // 0.05% per second baseline
    }

    public RtlCalculator(double nominalCruiseSpeedMps, double fallbackDischargeRatePercentPerSec) {
        this.nominalCruiseSpeedMps = nominalCruiseSpeedMps > 0.0 ? nominalCruiseSpeedMps : SafetyConfig.DEFAULT_CRUISE_SPEED_MPS;
        this.fallbackDischargeRatePercentPerSec = fallbackDischargeRatePercentPerSec;
    }

    public RtlResult calculate(
            PositionTelemetry aircraftPosition,
            HomePosition homePosition,
            Double currentBatteryPercent,
            Double measuredDischargeRatePercentPerSecond) {

        boolean isHomeKnown = homePosition != null && homePosition.isSet();
        if (!isHomeKnown) {
            return RtlResult.unavailable(false);
        }

        if (aircraftPosition == null || !aircraftPosition.hasCoordinates() || currentBatteryPercent == null) {
            return RtlResult.unavailable(true);
        }

        double distanceMeters = DistanceCalculator.calculateDistanceMeters(
                aircraftPosition.getLatitude(),
                aircraftPosition.getLongitude(),
                homePosition.getLatitude(),
                homePosition.getLongitude()
        );

        if (Double.isNaN(distanceMeters) || distanceMeters < 0.0) {
            return RtlResult.unavailable(true);
        }

        double cruiseSpeedMps = nominalCruiseSpeedMps;
        if (aircraftPosition.getGroundSpeedMps() != null && aircraftPosition.getGroundSpeedMps() > 3.0) {
            cruiseSpeedMps = aircraftPosition.getGroundSpeedMps();
        }

        long estimatedReturnSeconds = Math.round(distanceMeters / cruiseSpeedMps);

        double dischargeRate = fallbackDischargeRatePercentPerSec;
        if (measuredDischargeRatePercentPerSecond != null && measuredDischargeRatePercentPerSecond > 0.001) {
            dischargeRate = measuredDischargeRatePercentPerSecond;
        }

        double energyPercentNeeded = estimatedReturnSeconds * dischargeRate;
        double requiredBatteryPercent = energyPercentNeeded + SafetyConfig.RTL_SAFETY_MARGIN_PERCENT;

        RtlResult.Status status;
        if (currentBatteryPercent <= requiredBatteryPercent) {
            status = RtlResult.Status.CRITICAL_RTL;
        } else {
            status = RtlResult.Status.SAFE;
        }

        return new RtlResult(
                status,
                distanceMeters,
                cruiseSpeedMps,
                estimatedReturnSeconds,
                dischargeRate,
                energyPercentNeeded,
                SafetyConfig.RTL_SAFETY_MARGIN_PERCENT,
                requiredBatteryPercent,
                currentBatteryPercent,
                true
        );
    }
}
