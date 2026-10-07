package com.agridrone.safety;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.agridrone.safety.data.model.HomePosition;
import com.agridrone.safety.data.model.PositionTelemetry;
import com.agridrone.safety.domain.rtl.RtlCalculator;
import com.agridrone.safety.domain.rtl.RtlResult;

import org.junit.Before;
import org.junit.Test;

public class RtlCalculatorTest {

    private RtlCalculator rtlCalculator;

    @Before
    public void setUp() {
        rtlCalculator = new RtlCalculator(10.0, 0.08);
    }

    @Test
    public void testKnownSpecificationCalculation() {
        HomePosition home = new HomePosition(37.7749, -122.4194, 50.0, true);
        PositionTelemetry aircraft = new PositionTelemetry(37.7857, -122.4194, 60.0, 10.0, 1000L);

        RtlResult safeResult = rtlCalculator.calculate(aircraft, home, 28.0, 0.08);
        assertFalse("Battery 28% should be safe to continue", safeResult.isCritical());
        assertEquals(RtlResult.Status.SAFE, safeResult.getStatus());
        assertEquals(15.0, safeResult.getSafetyMarginPercent(), 0.01);

        RtlResult criticalResult = rtlCalculator.calculate(aircraft, home, 23.0, 0.08);
        assertTrue("Battery 23% should trigger CRITICAL RTL", criticalResult.isCritical());
        assertEquals(RtlResult.Status.CRITICAL_RTL, criticalResult.getStatus());
    }

    @Test
    public void testMissingHomePosition() {
        HomePosition unsetHome = HomePosition.unknown();
        PositionTelemetry aircraft = new PositionTelemetry(37.7857, -122.4194, 60.0, 10.0, 1000L);

        RtlResult result = rtlCalculator.calculate(aircraft, unsetHome, 50.0, 0.08);
        assertEquals(RtlResult.Status.UNAVAILABLE, result.getStatus());
        assertFalse(result.isHomeKnown());
    }

    @Test
    public void testMissingAircraftCoordinates() {
        HomePosition home = new HomePosition(37.7749, -122.4194, 50.0, true);
        PositionTelemetry aircraft = PositionTelemetry.empty();

        RtlResult result = rtlCalculator.calculate(aircraft, home, 50.0, 0.08);
        assertEquals(RtlResult.Status.UNAVAILABLE, result.getStatus());
    }
}
