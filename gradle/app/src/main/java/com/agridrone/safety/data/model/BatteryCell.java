package com.agridrone.safety.data.model;

import java.util.Objects;

public final class BatteryCell {

    private final int cellIndex; // 1-based (C1, C2, ...)
    private final Double voltageVolts;
    private final boolean isLowest;
    private final boolean isFault;

    public BatteryCell(int cellIndex, Double voltageVolts, boolean isLowest, boolean isFault) {
        this.cellIndex = cellIndex;
        this.voltageVolts = voltageVolts;
        this.isLowest = isLowest;
        this.isFault = isFault;
    }

    public int getCellIndex() {
        return cellIndex;
    }

    public String getDisplayLabel() {
        return "C" + cellIndex;
    }

    public Double getVoltageVolts() {
        return voltageVolts;
    }

    public boolean isLowest() {
        return isLowest;
    }

    public boolean isFault() {
        return isFault;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BatteryCell that = (BatteryCell) o;
        return cellIndex == that.cellIndex &&
                isLowest == that.isLowest &&
                isFault == that.isFault &&
                Objects.equals(voltageVolts, that.voltageVolts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cellIndex, voltageVolts, isLowest, isFault);
    }
}
