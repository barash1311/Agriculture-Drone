package com.agridrone.safety.protocol.mavlink;

public final class MavlinkParser {

    private static final int MAX_FRAME_LENGTH = 280;

    public static final int MAVLINK_V1_STX = 0xFE;
    public static final int MAVLINK_V2_STX = 0xFD;

    public interface PacketListener {
        void onPacketReceived(int messageId, byte[] payload);
    }

    private final PacketListener listener;
    private final byte[] pending = new byte[MAX_FRAME_LENGTH];
    private int pendingLength;

    public MavlinkParser(PacketListener listener) {
        this.listener = listener;
    }

    /**
     * Ingests incoming raw byte array from telemetry pipeline.
     */
    public synchronized void parseBytes(byte[] buffer, int length) {
        if (buffer == null || length <= 0 || length > buffer.length) {
            return;
        }

        for (int i = 0; i < length; i++) {
            if (pendingLength == pending.length) {
                discardPrefix(1);
            }
            pending[pendingLength++] = buffer[i];
            extractFrames();
        }
    }

    public synchronized void reset() {
        pendingLength = 0;
    }

    private void extractFrames() {
        while (pendingLength > 0) {
            int start = findStartByte();
            if (start < 0) {
                pendingLength = 0;
                return;
            }
            if (start > 0) {
                discardPrefix(start);
            }

            int marker = pending[0] & 0xFF;
            int headerLength = marker == MAVLINK_V2_STX ? 10 : 6;
            if (pendingLength < headerLength) {
                return;
            }

            int payloadLength = pending[1] & 0xFF;
            int signatureLength = marker == MAVLINK_V2_STX
                    && ((pending[2] & 0x01) != 0) ? 13 : 0;
            int frameLength = headerLength + payloadLength + 2 + signatureLength;
            if (frameLength > pending.length) {
                discardPrefix(1);
                continue;
            }
            if (pendingLength < frameLength) {
                return;
            }

            int messageId = marker == MAVLINK_V2_STX
                    ? (pending[7] & 0xFF)
                            | ((pending[8] & 0xFF) << 8)
                            | ((pending[9] & 0xFF) << 16)
                    : pending[5] & 0xFF;
            int crcExtra = getCrcExtra(messageId);
            if (crcExtra >= 0 && !hasValidChecksum(headerLength, payloadLength, crcExtra)) {
                discardPrefix(1);
                continue;
            }

            byte[] payload = new byte[payloadLength];
            System.arraycopy(pending, headerLength, payload, 0, payloadLength);
            if (listener != null) {
                listener.onPacketReceived(messageId, payload);
            }
            discardPrefix(frameLength);
        }
    }

    private int findStartByte() {
        for (int i = 0; i < pendingLength; i++) {
            int value = pending[i] & 0xFF;
            if (value == MAVLINK_V1_STX || value == MAVLINK_V2_STX) {
                return i;
            }
        }
        return -1;
    }

    private boolean hasValidChecksum(int headerLength, int payloadLength, int crcExtra) {
        int crc = 0xFFFF;
        int contentEnd = headerLength + payloadLength;
        for (int i = 1; i < contentEnd; i++) {
            crc = accumulateCrc(pending[i] & 0xFF, crc);
        }
        crc = accumulateCrc(crcExtra, crc);
        int checksumOffset = contentEnd;
        int actualCrc = (pending[checksumOffset] & 0xFF)
                | ((pending[checksumOffset + 1] & 0xFF) << 8);
        return crc == actualCrc;
    }

    private static int accumulateCrc(int value, int crc) {
        int tmp = (value ^ (crc & 0xFF)) & 0xFF;
        tmp = (tmp ^ (tmp << 4)) & 0xFF;
        return ((crc >>> 8) ^ (tmp << 8) ^ (tmp << 3) ^ (tmp >>> 4)) & 0xFFFF;
    }

    private static int getCrcExtra(int messageId) {
        switch (messageId) {
            case 0:
                return 50;
            case 1:
                return 124;
            case 33:
            case 242:
                return 104;
            case 147:
                return 154;
            default:
                return -1;
        }
    }

    private void discardPrefix(int count) {
        int remaining = pendingLength - count;
        if (remaining > 0) {
            System.arraycopy(pending, count, pending, 0, remaining);
        }
        pendingLength = Math.max(0, remaining);
    }
}
