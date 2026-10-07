package com.agridrone.safety;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.agridrone.safety.domain.battery.VoltageSagCalculator;

import org.junit.Test;

public class VoltageSagCalculatorTest {

    @Test
    public void testKnownValuesFormula() {
        double resting = VoltageSagCalculator.calculateRestingVoltage(48.0, 40.0, 0.020);
        assertEquals(48.8, resting, 0.001);
    }

    @Test
    public void testUnavailableOrZeroResistance() {
        double restingZero = VoltageSagCalculator.calculateRestingVoltage(48.0, 40.0, 0.0);
        assertEquals(48.0, restingZero, 0.001);

        double restingNull = VoltageSagCalculator.calculateRestingVoltage(48.0, 40.0, null);
        assertEquals(48.0, restingNull, 0.001);
    }

    @Test
    public void testZeroOrNegativeCurrent() {
        double resting = VoltageSagCalculator.calculateRestingVoltage(48.0, 0.0, 0.020);
        assertEquals(48.0, resting, 0.001);
    }

    @Test
    public void testInvalidVoltage() {
        double resting = VoltageSagCalculator.calculateRestingVoltage(null, 40.0, 0.020);
        assertTrue(Double.isNaN(resting));
    }
}
