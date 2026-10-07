package com.agridrone.safety.domain.battery;

public final class VoltageSagCalculator {

    private VoltageSagCalculator() {
    }

    /**
     * Calculates estimated resting voltage.
     *
     * @param measuredVolts measured pack or cell voltage under load (V)
     * @param currentAmps   measured instantaneous current draw (A)
     * @param resistanceOhms internal resistance Ri (Ohms)
     * @return calculated resting voltage in Volts, or measuredVolts if Ri is unavailable/0
     */
    public static double calculateRestingVoltage(Double measuredVolts, Double currentAmps, Double resistanceOhms) {
        if (measuredVolts == null || Double.isNaN(measuredVolts) || measuredVolts <= 0.0) {
            return Double.NaN;
        }
        if (currentAmps == null || Double.isNaN(currentAmps) || currentAmps <= 0.0) {
            return measuredVolts;
        }
        if (resistanceOhms == null || Double.isNaN(resistanceOhms) || resistanceOhms <= 0.0) {
            return measuredVolts;
        }

        return measuredVolts + (currentAmps * resistanceOhms);
    }
}
