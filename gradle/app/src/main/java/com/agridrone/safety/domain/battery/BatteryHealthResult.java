package com.agridrone.safety.domain.battery;

import com.agridrone.safety.data.model.BatteryConfiguration;

public final class BatteryHealthResult {

    private final CellHealthResult cellHealth;
    private final Double batteryPercent;
    private final Double packVoltage;
    private final Double restingVoltage;
    private final Double currentAmps;
    private final Double temperatureCelsius;
    private final Double consumptionMahPerMinute;
    private final Double dischargeRatePercentPerSecond;
    private final Long remainingFlightTimeSeconds;
    private final BatteryConfiguration configuration;

    public BatteryHealthResult(
            CellHealthResult cellHealth,
            Double batteryPercent,
            Double packVoltage,
            Double restingVoltage,
            Double currentAmps,
            Double temperatureCelsius,
            Double consumptionMahPerMinute,
            Double dischargeRatePercentPerSecond,
            Long remainingFlightTimeSeconds,
            BatteryConfiguration configuration) {
        this.cellHealth = cellHealth != null ? cellHealth : CellHealthResult.empty();
        this.batteryPercent = batteryPercent;
        this.packVoltage = packVoltage;
        this.restingVoltage = restingVoltage;
        this.currentAmps = currentAmps;
        this.temperatureCelsius = temperatureCelsius;
        this.consumptionMahPerMinute = consumptionMahPerMinute;
        this.dischargeRatePercentPerSecond = dischargeRatePercentPerSecond;
        this.remainingFlightTimeSeconds = remainingFlightTimeSeconds;
        this.configuration = configuration != null ? configuration : BatteryConfiguration.unknown();
    }

    public static BatteryHealthResult empty() {
        return new BatteryHealthResult(
                CellHealthResult.empty(),
                null, null, null, null, null, null, null, null,
                BatteryConfiguration.unknown()
        );
    }

    public CellHealthResult getCellHealth() {
        return cellHealth;
    }

    public Double getBatteryPercent() {
        return batteryPercent;
    }

    public Double getPackVoltage() {
        return packVoltage;
    }

    public Double getRestingVoltage() {
        return restingVoltage;
    }

    public Double getCurrentAmps() {
        return currentAmps;
    }

    public Double getTemperatureCelsius() {
        return temperatureCelsius;
    }

    public Double getConsumptionMahPerMinute() {
        return consumptionMahPerMinute;
    }

    public Double getDischargeRatePercentPerSecond() {
        return dischargeRatePercentPerSecond;
    }

    public Long getRemainingFlightTimeSeconds() {
        return remainingFlightTimeSeconds;
    }

    public BatteryConfiguration getConfiguration() {
        return configuration;
    }
}
