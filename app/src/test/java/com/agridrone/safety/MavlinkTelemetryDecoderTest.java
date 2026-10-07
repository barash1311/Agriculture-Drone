package com.agridrone.safety;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.agridrone.safety.data.model.TelemetrySnapshot;
import com.agridrone.safety.protocol.mavlink.MavlinkTelemetryDecoder;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public final class MavlinkTelemetryDecoderTest {

    @Test
    public void requiresHeartbeatBeforePublishingAircraftTelemetry() {
        List<TelemetrySnapshot> snapshots = new ArrayList<>();
        int[] detections = {0};
        MavlinkTelemetryDecoder decoder = new MavlinkTelemetryDecoder(
                new MavlinkTelemetryDecoder.Listener() {
                    @Override
                    public void onAircraftDetected() {
                        detections[0]++;
                    }

                    @Override
                    public void onTelemetrySnapshot(TelemetrySnapshot snapshot) {
                        snapshots.add(snapshot);
                    }
                });

        decoder.onPacketReceived(147, batteryStatusPayload(73, 3900));
        assertTrue(snapshots.isEmpty());
        assertEquals(0, detections[0]);

        decoder.onPacketReceived(0, autopilotHeartbeat());
        decoder.onPacketReceived(0, autopilotHeartbeat());
        assertEquals(1, detections[0]);
        assertTrue(snapshots.isEmpty());

        decoder.onPacketReceived(147, batteryStatusPayload(73, 3900));

        assertEquals(1, snapshots.size());
        assertEquals(73.0, snapshots.get(0).getBattery().getBatteryPercent(), 0.0);
        assertEquals(3.9, snapshots.get(0).getBattery().getCellVoltages().get(0), 0.0001);
    }

    @Test
    public void resetRequiresFreshHeartbeat() {
        List<TelemetrySnapshot> snapshots = new ArrayList<>();
        MavlinkTelemetryDecoder decoder = new MavlinkTelemetryDecoder(
                new MavlinkTelemetryDecoder.Listener() {
                    @Override
                    public void onAircraftDetected() {
                    }

                    @Override
                    public void onTelemetrySnapshot(TelemetrySnapshot snapshot) {
                        snapshots.add(snapshot);
                    }
                });

        decoder.onPacketReceived(0, autopilotHeartbeat());
        decoder.onPacketReceived(147, batteryStatusPayload(70, 3800));
        assertEquals(1, snapshots.size());
        assertEquals(70.0, snapshots.get(0).getBattery().getBatteryPercent(), 0.0);
        decoder.reset();
        int snapshotCount = snapshots.size();
        decoder.onPacketReceived(147, batteryStatusPayload(68, 3700));
        assertEquals(snapshotCount, snapshots.size());
    }

    @Test
    public void ignoresGroundControlAndInvalidAutopilotHeartbeats() {
        int[] detections = {0};
        MavlinkTelemetryDecoder decoder = new MavlinkTelemetryDecoder(
                new MavlinkTelemetryDecoder.Listener() {
                    @Override
                    public void onAircraftDetected() {
                        detections[0]++;
                    }

                    @Override
                    public void onTelemetrySnapshot(TelemetrySnapshot snapshot) {
                    }
                });
        byte[] groundStation = autopilotHeartbeat();
        groundStation[4] = 6;
        byte[] invalidAutopilot = autopilotHeartbeat();
        invalidAutopilot[5] = 8;

        decoder.onPacketReceived(0, groundStation);
        decoder.onPacketReceived(0, invalidAutopilot);

        assertEquals(0, detections[0]);
    }

    private static byte[] batteryStatusPayload(int batteryPercent, int cellMillivolts) {
        byte[] payload = new byte[36];
        setShort(payload, 3, 2500);
        for (int i = 0; i < 10; i++) {
            setShort(payload, 5 + i * 2, cellMillivolts);
        }
        setShort(payload, 25, 1000);
        setInt(payload, 27, 5000);
        payload[35] = (byte) batteryPercent;
        return payload;
    }

    private static byte[] autopilotHeartbeat() {
        byte[] payload = new byte[9];
        payload[4] = 2;
        payload[5] = 3;
        return payload;
    }

    private static void setShort(byte[] bytes, int offset, int value) {
        bytes[offset] = (byte) value;
        bytes[offset + 1] = (byte) (value >>> 8);
    }

    private static void setInt(byte[] bytes, int offset, int value) {
        bytes[offset] = (byte) value;
        bytes[offset + 1] = (byte) (value >>> 8);
        bytes[offset + 2] = (byte) (value >>> 16);
        bytes[offset + 3] = (byte) (value >>> 24);
    }
}
