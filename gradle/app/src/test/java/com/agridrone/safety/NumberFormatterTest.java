package com.agridrone.safety;

import static org.junit.Assert.assertEquals;

import com.agridrone.safety.util.NumberFormatter;

import org.junit.Test;

public final class NumberFormatterTest {

    @Test
    public void voltageAndUnitStayTogether() {
        assertEquals("4.95\u00a0V", NumberFormatter.formatVoltage(4.95));
        assertEquals("4.95\u00a0V", NumberFormatter.formatCellVoltage(4.95));
        assertEquals("0.08\u00a0V", NumberFormatter.formatCellDelta(0.08));
    }
}
