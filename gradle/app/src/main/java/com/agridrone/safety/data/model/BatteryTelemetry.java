package com.agridrone.safety.data.model;

import java.util.Collections;
import java.util.List;

public final class BatteryTelemetry {

    private final Double batteryPercent;
    private final Double packVoltageVolts;
    private final Double currentAmps;
    private final Double temperatureCelsius;
    private final Double consumedMah;
    private final Double remainingMah;
    private final Double nominalCapacityMah;
    private final String batteryType;
    private final List<Double> cellVoltages;
    private final long timestampMillis;

    public BatteryTelemetry(
            Double batteryPercent,
            Double packVoltageVolts,
            Double currentAmps,
            Double temperatureCelsius,
            Double consumedMah,
            Double remainingMah,
            Double nominalCapacityMah,
            String batteryType,
            List<Double> cellVoltages,
            long timestampMillis) {
        this.batteryPercent = batteryPercent;
        this.packVoltageVolts = packVoltageVolts;
        this.currentAmps = currentAmps;
        this.temperatureCelsius = temperatureCelsius;
        this.consumedMah = consumedMah;
        this.remainingMah = remainingMah;
        this.nominalCapacityMah = nominalCapacityMah;
        this.batteryType = batteryType;
        this.cellVoltages = cellVoltages != null ? Collections.unmodifiableList(cellVoltages) : Collections.emptyList();
        this.timestampMillis = timestampMillis;
    }

    public static BatteryTelemetry empty() {
        return new BatteryTelemetry(null, null, null, null, null, null, null, "Unknown", Collections.emptyList(), 0L);
    }

    public Double getBatteryPercent() {
        return batteryPercent;
    }

    public Double getPackVoltageVolts() {
        return packVoltageVolts;
    }

    public Double getCurrentAmps() {
        return currentAmps;
    }

    public Double getTemperatureCelsius() {
        return temperatureCelsius;
    }

    public Double getConsumedMah() {
        return consumedMah;
    }

    public Double getRemainingMah() {
        return remainingMah;
    }

    public Double getNominalCapacityMah() {
        return nominalCapacityMah;
    }

    public String getBatteryType() {
        return batteryType;
    }

    public List<Double> getCellVoltages() {
        return cellVoltages;
    }

    public long getTimestampMillis() {
        return timestampMillis;
    }
}
