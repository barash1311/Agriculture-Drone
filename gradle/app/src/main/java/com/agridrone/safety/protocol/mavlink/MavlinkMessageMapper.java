package com.agridrone.safety.protocol.mavlink;

import com.agridrone.safety.data.model.BatteryTelemetry;
import com.agridrone.safety.data.model.HomePosition;
import com.agridrone.safety.data.model.PositionTelemetry;

import java.util.ArrayList;
import java.util.List;

public final class MavlinkMessageMapper {

    private MavlinkMessageMapper() {
    }

    /**
     * Maps BATTERY_STATUS message payload (MAVLink msg 147).
     *
     * @param cellVoltagesMv   raw cell voltages in millivolts (sentinel 65535 = unpopulated)
     * @param currentCentiAmps raw current in centi-Amps (10 mA, sentinel -1 = unknown)
     * @param consumedMah      raw consumed capacity in mAh (sentinel -1 = unknown)
     * @param temperatureCdegC raw temperature in centi-degrees Celsius (sentinel 32767 = unknown)
     * @param batteryRemaining raw remaining percentage 0-100 (sentinel -1 = unknown)
     */
    public static BatteryTelemetry mapBatteryStatus(
            int[] cellVoltagesMv,
            int currentCentiAmps,
            int consumedMah,
            short temperatureCdegC,
            byte batteryRemaining,
            long timestampMillis) {

        Double batteryPercent = (batteryRemaining >= 0 && batteryRemaining <= 100)
                ? (double) batteryRemaining : null;

        Double currentAmps = (currentCentiAmps != -1 && currentCentiAmps >= 0)
                ? (double) currentCentiAmps / 100.0 : null;

        Double consumed = (consumedMah != -1 && consumedMah >= 0)
                ? (double) consumedMah : null;

        Double temperature = (temperatureCdegC != 32767 && temperatureCdegC > -5000)
                ? (double) temperatureCdegC / 100.0 : null;

        List<Double> cellList = new ArrayList<>();
        double packSum = 0.0;
        int validCellCount = 0;

        if (cellVoltagesMv != null) {
            for (int mv : cellVoltagesMv) {
                if (mv > 500 && mv != 65535) { // 65535 is MAVLink sentinel for not connected
                    double v = (double) mv / 1000.0;
                    cellList.add(v);
                    packSum += v;
                    validCellCount++;
                }
            }
        }

        Double packVoltage = validCellCount > 0 ? packSum : null;

        return new BatteryTelemetry(
                batteryPercent,
                packVoltage,
                currentAmps,
                temperature,
                consumed,
                null,
                null,
                "MAVLink Battery",
                cellList,
                timestampMillis
        );
    }

    /**
     * Maps SYS_STATUS message payload (MAVLink msg 1).
     */
    public static BatteryTelemetry mapSysStatus(
            int voltageBatteryMv,
            int currentBatteryCa,
            byte batteryRemaining,
            long timestampMillis) {

        Double packVoltage = (voltageBatteryMv > 0 && voltageBatteryMv != 65535)
                ? (double) voltageBatteryMv / 1000.0 : null;

        Double currentAmps = (currentBatteryCa != -1 && currentBatteryCa >= 0)
                ? (double) currentBatteryCa / 100.0 : null;

        Double batteryPercent = (batteryRemaining >= 0 && batteryRemaining <= 100)
                ? (double) batteryRemaining : null;

        return new BatteryTelemetry(
                batteryPercent,
                packVoltage,
                currentAmps,
                null,
                null,
                null,
                null,
                "MAVLink SYS_STATUS",
                new ArrayList<>(),
                timestampMillis
        );
    }

    /**
     * Maps GLOBAL_POSITION_INT message payload (MAVLink msg 33).
     */
    public static PositionTelemetry mapGlobalPositionInt(
            int latE7,
            int lonE7,
            int altMm,
            short vxCmS,
            short vyCmS,
            long timestampMillis) {

        Double latitude = (latE7 != 0) ? (double) latE7 / 1e7 : null;
        Double longitude = (lonE7 != 0) ? (double) lonE7 / 1e7 : null;
        Double altitudeMeters = (altMm != 0) ? (double) altMm / 1000.0 : null;

        double vxMps = (double) vxCmS / 100.0;
        double vyMps = (double) vyCmS / 100.0;
        double groundSpeedMps = Math.sqrt((vxMps * vxMps) + (vyMps * vyMps));

        return new PositionTelemetry(
                latitude,
                longitude,
                altitudeMeters,
                groundSpeedMps,
                timestampMillis
        );
    }

    /**
     * Maps HOME_POSITION message payload (MAVLink msg 242).
     */
    public static HomePosition mapHomePosition(int latE7, int lonE7, int altMm) {
        if (latE7 == 0 && lonE7 == 0) {
            return HomePosition.unknown();
        }
        return new HomePosition(
                (double) latE7 / 1e7,
                (double) lonE7 / 1e7,
                (double) altMm / 1000.0,
                true
        );
    }
}
