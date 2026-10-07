package com.agridrone.safety.domain.battery;

import com.agridrone.safety.config.SafetyConfig;
import com.agridrone.safety.config.TelemetryConfig;
import com.agridrone.safety.data.model.BatteryCell;
import com.agridrone.safety.data.model.BatteryConfiguration;
import com.agridrone.safety.data.model.BatteryTelemetry;

import java.util.ArrayList;
import java.util.List;

public final class BatteryAnalyzer {

    private final DischargeRateEstimator dischargeRateEstimator;
    private final double internalResistanceOhms;

    public BatteryAnalyzer() {
        this(SafetyConfig.DEFAULT_INTERNAL_RESISTANCE_OHMS);
    }

    public BatteryAnalyzer(double internalResistanceOhms) {
        this.dischargeRateEstimator = new DischargeRateEstimator(TelemetryConfig.DISCHARGE_RATE_WINDOW_SAMPLES);
        this.internalResistanceOhms = internalResistanceOhms;
    }

    public BatteryHealthResult analyze(BatteryTelemetry telemetry) {
        if (telemetry == null) {
            return BatteryHealthResult.empty();
        }

        CellHealthResult cellHealth = analyzeCells(telemetry.getCellVoltages());

        Double restingVoltage = null;
        if (telemetry.getPackVoltageVolts() != null) {
            double calculatedRest = VoltageSagCalculator.calculateRestingVoltage(
                    telemetry.getPackVoltageVolts(),
                    telemetry.getCurrentAmps(),
                    internalResistanceOhms
            );
            if (!Double.isNaN(calculatedRest)) {
                restingVoltage = calculatedRest;
            }
        }

        Double consumptionMahPerMin = null;
        Double dischargePercentPerSec = null;
        if (telemetry.getConsumedMah() != null && telemetry.getBatteryPercent() != null && telemetry.getTimestampMillis() > 0) {
            dischargeRateEstimator.addSample(
                    telemetry.getConsumedMah(),
                    telemetry.getBatteryPercent(),
                    telemetry.getTimestampMillis()
            );

            double rateMahMin = dischargeRateEstimator.getConsumptionMahPerMinute();
            if (!Double.isNaN(rateMahMin) && rateMahMin > 0.0) {
                consumptionMahPerMin = rateMahMin;
            }

            double ratePercentSec = dischargeRateEstimator.getDischargeRatePercentPerSecond();
            if (!Double.isNaN(ratePercentSec) && ratePercentSec > 0.0) {
                dischargePercentPerSec = ratePercentSec;
            }
        }

        Long remainingTimeSeconds = null;
        if (telemetry.getRemainingMah() != null && consumptionMahPerMin != null && consumptionMahPerMin > 0.0) {
            double minutes = telemetry.getRemainingMah() / consumptionMahPerMin;
            if (minutes > 0.0 && minutes < 300.0) { // Capped at 5 hours
                remainingTimeSeconds = (long) (minutes * 60.0);
            }
        } else if (telemetry.getBatteryPercent() != null && dischargePercentPerSec != null && dischargePercentPerSec > 0.0) {
            double seconds = telemetry.getBatteryPercent() / dischargePercentPerSec;
            if (seconds > 0.0 && seconds < 18000.0) {
                remainingTimeSeconds = (long) seconds;
            }
        }

        int count = cellHealth.getCellCount();
        BatteryConfiguration config = new BatteryConfiguration(
                count > 0 ? count : null,
                telemetry.getBatteryType(),
                telemetry.getNominalCapacityMah()
        );

        return new BatteryHealthResult(
                cellHealth,
                telemetry.getBatteryPercent(),
                telemetry.getPackVoltageVolts(),
                restingVoltage,
                telemetry.getCurrentAmps(),
                telemetry.getTemperatureCelsius(),
                consumptionMahPerMin,
                dischargePercentPerSec,
                remainingTimeSeconds,
                config
        );
    }

    public CellHealthResult analyzeCells(List<Double> rawVoltages) {
        if (rawVoltages == null || rawVoltages.isEmpty()) {
            return CellHealthResult.empty();
        }

        List<Double> validVoltages = new ArrayList<>();
        double sum = 0.0;
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        int lowestIndex = -1;

        for (int i = 0; i < rawVoltages.size(); i++) {
            Double v = rawVoltages.get(i);
            if (v != null && !Double.isNaN(v) && v > 0.5 && v < 5.5) { // Valid LiPo/LiHV bounds
                validVoltages.add(v);
                sum += v;
                if (v < min) {
                    min = v;
                    lowestIndex = i + 1; // 1-based index (C1, C2...)
                }
                if (v > max) {
                    max = v;
                }
            }
        }

        if (validVoltages.isEmpty()) {
            return CellHealthResult.empty();
        }

        int count = validVoltages.size();
        double avg = sum / count;
        double delta = Math.round((max - min) * 10000.0) / 10000.0;
        // Strictly greater than 0.08 per Spec Section 27
        boolean isDeltaFault = delta > SafetyConfig.CELL_DELTA_FAULT_VOLTS;

        List<BatteryCell> cells = new ArrayList<>(count);
        for (int i = 0; i < rawVoltages.size(); i++) {
            Double v = rawVoltages.get(i);
            if (v != null && !Double.isNaN(v) && v > 0.5 && v < 5.5) {
                boolean isLowest = (i + 1 == lowestIndex);
                double cellDiff = Math.round((max - v) * 10000.0) / 10000.0;
                boolean isFault = isDeltaFault && (isLowest || (cellDiff > SafetyConfig.CELL_DELTA_FAULT_VOLTS));
                cells.add(new BatteryCell(i + 1, v, isLowest, isFault));
            }
        }

        return new CellHealthResult(
                count,
                min,
                max,
                avg,
                delta,
                lowestIndex,
                isDeltaFault,
                cells
        );
    }

    public void resetEstimator() {
        dischargeRateEstimator.reset();
    }
}
