package com.agridrone.safety.protocol.mavlink;

import com.agridrone.safety.data.model.BatteryTelemetry;
import com.agridrone.safety.data.model.ConnectionState;
import com.agridrone.safety.data.model.HomePosition;
import com.agridrone.safety.data.model.PositionTelemetry;
import com.agridrone.safety.data.model.TelemetryHealth;
import com.agridrone.safety.data.model.TelemetrySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MavlinkTelemetryDecoder implements MavlinkParser.PacketListener {

    public interface Listener {
        void onAircraftDetected();

        void onTelemetrySnapshot(TelemetrySnapshot snapshot);
    }

    private final Listener listener;
    private BatteryTelemetry battery = BatteryTelemetry.empty();
    private PositionTelemetry position = PositionTelemetry.empty();
    private HomePosition home = HomePosition.unknown();
    private boolean aircraftDetected;

    public MavlinkTelemetryDecoder(Listener listener) {
        this.listener = listener;
    }

    public synchronized void reset() {
        battery = BatteryTelemetry.empty();
        position = PositionTelemetry.empty();
        home = HomePosition.unknown();
        aircraftDetected = false;
    }

    @Override
    public synchronized void onPacketReceived(int messageId, byte[] payload) {
        if (payload == null) {
            return;
        }
        long timestamp = System.currentTimeMillis();

        if (messageId == 0) {
            boolean autopilotHeartbeat = payload.length >= 9
                    && payload[5] != 8
                    && payload[4] != 6;
            if (autopilotHeartbeat && !aircraftDetected) {
                aircraftDetected = true;
                if (listener != null) {
                    listener.onAircraftDetected();
                }
            }
            return;
        }

        if (messageId == 147 && payload.length >= 36) {
            battery = mergeBattery(decodeBatteryStatus(payload, timestamp), battery);
        } else if (messageId == 1 && payload.length >= 31) {
            BatteryTelemetry statusBattery = MavlinkMessageMapper.mapSysStatus(
                    unsignedShort(payload, 14),
                    signedShort(payload, 16),
                    payload[30],
                    timestamp);
            battery = mergeBattery(statusBattery, battery);
        } else if (messageId == 33 && payload.length >= 28) {
            position = MavlinkMessageMapper.mapGlobalPositionInt(
                    signedInt(payload, 4),
                    signedInt(payload, 8),
                    signedInt(payload, 12),
                    signedShort(payload, 20),
                    signedShort(payload, 22),
                    timestamp);
        } else if (messageId == 242 && payload.length >= 12) {
            home = MavlinkMessageMapper.mapHomePosition(
                    signedInt(payload, 0),
                    signedInt(payload, 4),
                    signedInt(payload, 8));
        } else {
            return;
        }

        if (aircraftDetected && listener != null) {
            listener.onTelemetrySnapshot(new TelemetrySnapshot(
                    ConnectionState.CONNECTED,
                    TelemetryHealth.LIVE,
                    battery,
                    position,
                    home,
                    timestamp));
        }
    }

    private static BatteryTelemetry decodeBatteryStatus(byte[] payload, long timestamp) {
        int[] cellVoltages = new int[payload.length >= 44 ? 14 : 10];
        for (int i = 0; i < 10; i++) {
            cellVoltages[i] = unsignedShort(payload, 5 + i * 2);
        }
        if (payload.length >= 44) {
            for (int i = 0; i < 4; i++) {
                cellVoltages[10 + i] = unsignedShort(payload, 36 + i * 2);
            }
        }
        return MavlinkMessageMapper.mapBatteryStatus(
                cellVoltages,
                signedShort(payload, 25),
                signedInt(payload, 27),
                signedShort(payload, 3),
                payload[35],
                timestamp);
    }

    private static BatteryTelemetry mergeBattery(
            BatteryTelemetry incoming,
            BatteryTelemetry previous) {
        List<Double> cells = incoming.getCellVoltages().isEmpty()
                ? previous.getCellVoltages()
                : incoming.getCellVoltages();
        return new BatteryTelemetry(
                first(incoming.getBatteryPercent(), previous.getBatteryPercent()),
                first(incoming.getPackVoltageVolts(), previous.getPackVoltageVolts()),
                first(incoming.getCurrentAmps(), previous.getCurrentAmps()),
                first(incoming.getTemperatureCelsius(), previous.getTemperatureCelsius()),
                first(incoming.getConsumedMah(), previous.getConsumedMah()),
                first(incoming.getRemainingMah(), previous.getRemainingMah()),
                first(incoming.getNominalCapacityMah(), previous.getNominalCapacityMah()),
                incoming.getBatteryType() == null ? previous.getBatteryType() : incoming.getBatteryType(),
                new ArrayList<>(cells != null ? cells : Collections.emptyList()),
                incoming.getTimestampMillis());
    }

    private static Double first(Double preferred, Double fallback) {
        return preferred != null ? preferred : fallback;
    }

    private static int unsignedShort(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF) | ((bytes[offset + 1] & 0xFF) << 8);
    }

    private static short signedShort(byte[] bytes, int offset) {
        return (short) unsignedShort(bytes, offset);
    }

    private static int signedInt(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF)
                | ((bytes[offset + 1] & 0xFF) << 8)
                | ((bytes[offset + 2] & 0xFF) << 16)
                | (bytes[offset + 3] << 24);
    }
}
