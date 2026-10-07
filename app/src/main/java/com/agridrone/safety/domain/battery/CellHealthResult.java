package com.agridrone.safety.domain.battery;

import com.agridrone.safety.data.model.BatteryCell;

import java.util.Collections;
import java.util.List;

public final class CellHealthResult {

    private final int cellCount;
    private final Double minCell;
    private final Double maxCell;
    private final Double averageCell;
    private final Double cellDelta;
    private final int lowestCellIndex; // 1-based index (e.g. 7 for C7)
    private final boolean isCellDeltaFault; // delta > 0.08 V
    private final List<BatteryCell> cells;

    public CellHealthResult(
            int cellCount,
            Double minCell,
            Double maxCell,
            Double averageCell,
            Double cellDelta,
            int lowestCellIndex,
            boolean isCellDeltaFault,
            List<BatteryCell> cells) {
        this.cellCount = cellCount;
        this.minCell = minCell;
        this.maxCell = maxCell;
        this.averageCell = averageCell;
        this.cellDelta = cellDelta;
        this.lowestCellIndex = lowestCellIndex;
        this.isCellDeltaFault = isCellDeltaFault;
        this.cells = cells != null ? Collections.unmodifiableList(cells) : Collections.emptyList();
    }

    public static CellHealthResult empty() {
        return new CellHealthResult(0, null, null, null, null, -1, false, Collections.emptyList());
    }

    public boolean hasData() {
        return cellCount > 0 && minCell != null;
    }

    public int getCellCount() {
        return cellCount;
    }

    public Double getMinCell() {
        return minCell;
    }

    public Double getMaxCell() {
        return maxCell;
    }

    public Double getAverageCell() {
        return averageCell;
    }

    public Double getCellDelta() {
        return cellDelta;
    }

    public int getLowestCellIndex() {
        return lowestCellIndex;
    }

    public String getLowestCellLabel() {
        if (lowestCellIndex <= 0) {
            return "\u2014\u2014";
        }
        return "C" + lowestCellIndex;
    }

    public boolean isCellDeltaFault() {
        return isCellDeltaFault;
    }

    public List<BatteryCell> getCells() {
        return cells;
    }
}
