package com.agridrone.safety;

import static org.junit.Assert.assertEquals;

import com.agridrone.safety.data.model.BatteryCell;
import com.agridrone.safety.data.model.BatteryConfiguration;
import com.agridrone.safety.data.model.SafetyState;
import com.agridrone.safety.domain.battery.BatteryHealthResult;
import com.agridrone.safety.domain.battery.CellHealthResult;
import com.agridrone.safety.domain.rtl.RtlResult;
import com.agridrone.safety.domain.safety.RapidSagDetector;
import com.agridrone.safety.domain.safety.SafetyEngine;
import com.agridrone.safety.domain.safety.SafetyEvaluation;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

public class SafetyEngineTest {

    private SafetyEngine safetyEngine;

    @Before
    public void setUp() {
        safetyEngine = new SafetyEngine(new RapidSagDetector());
    }

    private BatteryHealthResult createBatteryHealth(Double batteryPercent, Double minCell, Double cellDelta) {
        CellHealthResult cellHealth = new CellHealthResult(
                6,
                minCell,
                (minCell != null && cellDelta != null) ? (minCell + cellDelta) : minCell,
                minCell,
                cellDelta,
                1,
                cellDelta != null && cellDelta > 0.08,
                Collections.emptyList()
        );

        return new BatteryHealthResult(
                cellHealth,
                batteryPercent,
                24.0,
                24.0,
                10.0,
                30.0,
                500.0,
                0.05,
                1200L,
                new BatteryConfiguration(6, "LiPo", 16000.0)
        );
    }

    @Test
    public void testBatteryPercentBoundaries() {
        SafetyEvaluation e301 = safetyEngine.evaluate(createBatteryHealth(30.1, 3.85, 0.02), null, 1000L);
        assertEquals(SafetyState.NORMAL, e301.getPrimaryState());

        SafetyEvaluation e300 = safetyEngine.evaluate(createBatteryHealth(30.0, 3.85, 0.02), null, 1000L);
        assertEquals(SafetyState.NOTICE, e300.getPrimaryState());

        SafetyEvaluation e299 = safetyEngine.evaluate(createBatteryHealth(29.9, 3.85, 0.02), null, 1000L);
        assertEquals(SafetyState.NOTICE, e299.getPrimaryState());

        SafetyEvaluation e201 = safetyEngine.evaluate(createBatteryHealth(20.1, 3.85, 0.02), null, 1000L);
        assertEquals(SafetyState.NOTICE, e201.getPrimaryState());

        SafetyEvaluation e200 = safetyEngine.evaluate(createBatteryHealth(20.0, 3.85, 0.02), null, 1000L);
        assertEquals(SafetyState.WARNING, e200.getPrimaryState());

        SafetyEvaluation e199 = safetyEngine.evaluate(createBatteryHealth(19.9, 3.85, 0.02), null, 1000L);
        assertEquals(SafetyState.WARNING, e199.getPrimaryState());
    }

    @Test
    public void testCellVoltageWarningBoundaries() {
        SafetyEvaluation e3651 = safetyEngine.evaluate(createBatteryHealth(50.0, 3.651, 0.02), null, 1000L);
        assertEquals(SafetyState.NORMAL, e3651.getPrimaryState());

        SafetyEvaluation e3650 = safetyEngine.evaluate(createBatteryHealth(50.0, 3.650, 0.02), null, 1000L);
        assertEquals(SafetyState.WARNING, e3650.getPrimaryState());

        SafetyEvaluation e3649 = safetyEngine.evaluate(createBatteryHealth(50.0, 3.649, 0.02), null, 1000L);
        assertEquals(SafetyState.WARNING, e3649.getPrimaryState());
    }

    @Test
    public void testCellVoltageCriticalBoundaries() {
        SafetyEvaluation e3501 = safetyEngine.evaluate(createBatteryHealth(50.0, 3.501, 0.02), null, 1000L);
        assertEquals(SafetyState.WARNING, e3501.getPrimaryState());

        SafetyEvaluation e3500 = safetyEngine.evaluate(createBatteryHealth(50.0, 3.500, 0.02), null, 1000L);
        assertEquals(SafetyState.CRITICAL, e3500.getPrimaryState());

        SafetyEvaluation e3499 = safetyEngine.evaluate(createBatteryHealth(50.0, 3.499, 0.02), null, 1000L);
        assertEquals(SafetyState.CRITICAL, e3499.getPrimaryState());
    }

    @Test
    public void testCellVoltageEmergencyBoundaries() {
        SafetyEvaluation e3401 = safetyEngine.evaluate(createBatteryHealth(50.0, 3.401, 0.02), null, 1000L);
        assertEquals(SafetyState.CRITICAL, e3401.getPrimaryState());

        SafetyEvaluation e3400 = safetyEngine.evaluate(createBatteryHealth(50.0, 3.400, 0.02), null, 1000L);
        assertEquals(SafetyState.EMERGENCY, e3400.getPrimaryState());

        SafetyEvaluation e3399 = safetyEngine.evaluate(createBatteryHealth(50.0, 3.399, 0.02), null, 1000L);
        assertEquals(SafetyState.EMERGENCY, e3399.getPrimaryState());
    }

    @Test
    public void testCellDeltaBoundaries() {
        SafetyEvaluation e079 = safetyEngine.evaluate(createBatteryHealth(50.0, 3.80, 0.079), null, 1000L);
        assertEquals(SafetyState.NORMAL, e079.getPrimaryState());

        SafetyEvaluation e080 = safetyEngine.evaluate(createBatteryHealth(50.0, 3.80, 0.080), null, 1000L);
        assertEquals(SafetyState.NORMAL, e080.getPrimaryState());

        SafetyEvaluation e081 = safetyEngine.evaluate(createBatteryHealth(50.0, 3.80, 0.081), null, 1000L);
        assertEquals(SafetyState.CELL_FAULT, e081.getPrimaryState());
    }

    @Test
    public void testPriorityHierarchyResolution() {
        RtlResult criticalRtl = new RtlResult(
                RtlResult.Status.CRITICAL_RTL, 1200.0, 10.0, 120L, 0.08, 9.6, 15.0, 24.6, 21.0, true
        );
        SafetyEvaluation evalRtl = safetyEngine.evaluate(createBatteryHealth(21.0, 3.75, 0.12), criticalRtl, 1000L);
        assertEquals("CRITICAL must override CELL_FAULT", SafetyState.CRITICAL, evalRtl.getPrimaryState());

        SafetyEvaluation evalEmerg = safetyEngine.evaluate(createBatteryHealth(21.0, 3.38, 0.12), criticalRtl, 1000L);
        assertEquals("EMERGENCY must override CRITICAL", SafetyState.EMERGENCY, evalEmerg.getPrimaryState());
    }
}
