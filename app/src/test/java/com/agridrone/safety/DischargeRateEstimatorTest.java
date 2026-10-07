package com.agridrone.safety;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.agridrone.safety.domain.battery.DischargeRateEstimator;
import com.agridrone.safety.util.UnitConverter;

import org.junit.Test;

public class DischargeRateEstimatorTest {

    @Test
    public void testUnitConverterCurrentIntegration() {
        double mah1 = UnitConverter.currentToMah(1.0, 3600000L);
        assertEquals(1000.0, mah1, 0.001);

        double mah2 = UnitConverter.currentToMah(10.0, 360000L);
        assertEquals(1000.0, mah2, 0.001);
    }

    @Test
    public void testInsufficientHistory() {
        DischargeRateEstimator estimator = new DischargeRateEstimator(10);
        estimator.addSample(500.0, 95.0, 1000L);
        assertTrue(Double.isNaN(estimator.getConsumptionMahPerMinute()));
        assertTrue(Double.isNaN(estimator.getDischargeRatePercentPerSecond()));
    }

    @Test
    public void testCapacityConsumptionOverTime() {
        DischargeRateEstimator estimator = new DischargeRateEstimator(10);

        estimator.addSample(1000.0, 90.0, 0L);
        estimator.addSample(1600.0, 87.0, 60000L);

        double mahPerMin = estimator.getConsumptionMahPerMinute();
        assertEquals(600.0, mahPerMin, 1.0);

        double percentPerSec = estimator.getDischargeRatePercentPerSecond();
        assertEquals(0.05, percentPerSec, 0.001);
    }
}
