package com.agridrone.safety;

import static org.junit.Assert.assertEquals;

import com.agridrone.safety.protocol.mavlink.MavlinkParser;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class MavlinkParserTest {

    @Test
    public void parsesFragmentedV1HeartbeatWithValidChecksum() {
        List<Integer> received = new ArrayList<>();
        MavlinkParser parser = new MavlinkParser((messageId, payload) -> received.add(messageId));
        byte[] frame = frameV1(0, new byte[9], 50);

        parser.parseBytes(frame, 4);
        assertEquals(0, received.size());

        byte[] remainder = Arrays.copyOfRange(frame, 4, frame.length);
        parser.parseBytes(remainder, remainder.length);
        assertEquals(1, received.size());
        assertEquals(Integer.valueOf(0), received.get(0));
    }

    @Test
    public void parsesSignedV2HeartbeatAndFollowingV1Frame() {
        List<Integer> received = new ArrayList<>();
        MavlinkParser parser = new MavlinkParser((messageId, payload) -> received.add(messageId));
        byte[] v2 = frameV2(0, new byte[9], 50, true);
        byte[] v1 = frameV1(0, new byte[9], 50);
        byte[] stream = concatenate(v2, v1);

        parser.parseBytes(stream, stream.length);

        assertEquals(2, received.size());
        assertEquals(Integer.valueOf(0), received.get(0));
        assertEquals(Integer.valueOf(0), received.get(1));
    }

    @Test
    public void ignoresHeartbeatWithInvalidChecksumAndResynchronizes() {
        List<Integer> received = new ArrayList<>();
        MavlinkParser parser = new MavlinkParser((messageId, payload) -> received.add(messageId));
        byte[] invalid = frameV1(0, new byte[9], 50);
        invalid[invalid.length - 1] ^= 1;
        byte[] valid = frameV1(0, new byte[9], 50);
        byte[] stream = concatenate(invalid, valid);

        parser.parseBytes(stream, stream.length);

        assertEquals(1, received.size());
        assertEquals(Integer.valueOf(0), received.get(0));
    }

    private static byte[] frameV1(int messageId, byte[] payload, int crcExtra) {
        byte[] frame = new byte[6 + payload.length + 2];
        frame[0] = (byte) MavlinkParser.MAVLINK_V1_STX;
        frame[1] = (byte) payload.length;
        frame[2] = 1;
        frame[3] = 1;
        frame[4] = 1;
        frame[5] = (byte) messageId;
        System.arraycopy(payload, 0, frame, 6, payload.length);
        setChecksum(frame, 6 + payload.length, crcExtra);
        return frame;
    }

    private static byte[] frameV2(int messageId, byte[] payload, int crcExtra, boolean signed) {
        int signatureLength = signed ? 13 : 0;
        byte[] frame = new byte[10 + payload.length + 2 + signatureLength];
        frame[0] = (byte) MavlinkParser.MAVLINK_V2_STX;
        frame[1] = (byte) payload.length;
        frame[2] = signed ? (byte) 1 : 0;
        frame[3] = 0;
        frame[4] = 1;
        frame[5] = 1;
        frame[6] = 1;
        frame[7] = (byte) messageId;
        System.arraycopy(payload, 0, frame, 10, payload.length);
        setChecksum(frame, 10 + payload.length, crcExtra);
        return frame;
    }

    private static void setChecksum(byte[] frame, int checksumOffset, int crcExtra) {
        int crc = 0xFFFF;
        int contentEnd = checksumOffset;
        for (int i = 1; i < contentEnd; i++) {
            crc = accumulateCrc(frame[i] & 0xFF, crc);
        }
        crc = accumulateCrc(crcExtra, crc);
        frame[checksumOffset] = (byte) crc;
        frame[checksumOffset + 1] = (byte) (crc >>> 8);
    }

    private static int accumulateCrc(int value, int crc) {
        int tmp = (value ^ (crc & 0xFF)) & 0xFF;
        tmp = (tmp ^ (tmp << 4)) & 0xFF;
        return ((crc >>> 8) ^ (tmp << 8) ^ (tmp << 3) ^ (tmp >>> 4)) & 0xFFFF;
    }

    private static byte[] concatenate(byte[] first, byte[] second) {
        ByteArrayOutputStream output = new ByteArrayOutputStream(first.length + second.length);
        output.write(first, 0, first.length);
        output.write(second, 0, second.length);
        return output.toByteArray();
    }
}
