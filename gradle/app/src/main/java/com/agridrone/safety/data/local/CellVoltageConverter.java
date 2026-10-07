package com.agridrone.safety.data.local;

import androidx.room.TypeConverter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CellVoltageConverter {

    @TypeConverter
    public static String fromCellVoltages(List<Double> voltages) {
        if (voltages == null || voltages.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < voltages.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(voltages.get(i));
        }
        return sb.toString();
    }

    @TypeConverter
    public static List<Double> toCellVoltages(String data) {
        if (data == null || data.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String[] parts = data.split(",");
        List<Double> result = new ArrayList<>(parts.length);
        for (String part : parts) {
            try {
                result.add(Double.parseDouble(part.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        return result;
    }
}
