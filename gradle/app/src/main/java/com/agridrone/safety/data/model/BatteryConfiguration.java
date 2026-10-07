package com.agridrone.safety.data.model;

public final class BatteryConfiguration {

    private final Integer cellCount;
    private final String chemistry;
    private final Double nominalCapacityMah;

    public BatteryConfiguration(Integer cellCount, String chemistry, Double nominalCapacityMah) {
        this.cellCount = cellCount;
        this.chemistry = chemistry != null ? chemistry : "Unknown";
        this.nominalCapacityMah = nominalCapacityMah;
    }

    public static BatteryConfiguration unknown() {
        return new BatteryConfiguration(null, "Unknown", null);
    }

    public Integer getCellCount() {
        return cellCount;
    }

    public String getDisplayConfiguration() {
        if (cellCount == null || cellCount <= 0) {
            return "Detecting…";
        }
        return cellCount + "S";
    }

    public String getChemistry() {
        return chemistry;
    }

    public Double getNominalCapacityMah() {
        return nominalCapacityMah;
    }
}
