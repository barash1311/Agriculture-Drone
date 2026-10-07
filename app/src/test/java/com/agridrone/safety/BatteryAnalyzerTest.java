package com.agridrone.safety;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.agridrone.safety.data.model.BatteryTelemetry;
import com.agridrone.safety.domain.battery.BatteryAnalyzer;
import com.agridrone.safety.domain.battery.BatteryHealthResult;
import com.agridrone.safety.domain.battery.CellHealthResult;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BatteryAnalyzerTest {

    private BatteryAnalyzer analyzer;

    @Before
    public void setUp() {
        analyzer = new BatteryAnalyzer(0.015);
    }

    @Test
    public void testNominal12SCellAnalysis() {
        List<Double> voltages = Arrays.asList(
                3.91, 3.90, 3.92, 3.91, 3.90, 3.86, 3.93, 3.91, 3.90, 3.92, 3.91, 3.94
        );

        CellHealthResult result = analyzer.analyzeCells(voltages);

        assertEquals(12, result.getCellCount());
        assertEquals(3.86, result.getMinCell(), 0.001);
        assertEquals(3.94, result.getMaxCell(), 0.001);
        assertEquals(0.08, result.getCellDelta(), 0.001);
        assertEquals(6, result.getLowestCellIndex()); // C6 is 3.86
        assertEquals("C6", result.getLowestCellLabel());
        assertFalse("Delta 0.08 is not strictly > 0.08", result.isCellDeltaFault());
    }

    @Test
    public void testCellDeltaFaultCondition() {
        List<Double> voltages = Arrays.asList(
                3.90, 3.92, 3.90, 3.82, 3.91, 3.90
        );

        CellHealthResult result = analyzer.analyzeCells(voltages);

        assertEquals(6, result.getCellCount());
        assertEquals(3.82, result.getMinCell(), 0.001);
        assertEquals(3.92, result.getMaxCell(), 0.001);
        assertEquals(0.10, result.getCellDelta(), 0.001);
        assertEquals(4, result.getLowestCellIndex());
        assertTrue("Delta 0.10 should trigger cell delta fault", result.isCellDeltaFault());
    }

    @Test
    public void testEmptyOrNullVoltages() {
        CellHealthResult emptyResult = analyzer.analyzeCells(Collections.emptyList());
        assertEquals(0, emptyResult.getCellCount());
        assertFalse(emptyResult.hasData());

        CellHealthResult nullResult = analyzer.analyzeCells(null);
        assertEquals(0, nullResult.getCellCount());
        assertFalse(nullResult.hasData());
    }

    @Test
    public void testFullBatteryHealthAnalysis() {
        List<Double> voltages = Arrays.asList(3.90, 3.90, 3.90, 3.90, 3.90, 3.90);
        BatteryTelemetry telemetry = new BatteryTelemetry(
                75.0, 23.4, 10.0, 35.0, 5000.0, 15000.0, 20000.0,
                "LiPo", voltages, System.currentTimeMillis()
        );

        BatteryHealthResult result = analyzer.analyze(telemetry);

        assertEquals(75.0, result.getBatteryPercent(), 0.001);
        assertEquals(23.4, result.getPackVoltage(), 0.001);
        assertEquals(23.55, result.getRestingVoltage(), 0.001);
        assertEquals(6, result.getCellHealth().getCellCount());
        assertEquals("6S", result.getConfiguration().getDisplayConfiguration());
    }
}
