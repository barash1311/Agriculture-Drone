package com.agridrone.safety.protocol.mavlink;

public enum MavlinkMessageType {
    HEARTBEAT(0),
    SYS_STATUS(1),
    GLOBAL_POSITION_INT(33),
    BATTERY_STATUS(147),
    HOME_POSITION(242);

    private final int messageId;

    MavlinkMessageType(int messageId) {
        this.messageId = messageId;
    }

    public int getMessageId() {
        return messageId;
    }

    public static MavlinkMessageType fromId(int id) {
        for (MavlinkMessageType type : values()) {
            if (type.messageId == id) {
                return type;
            }
        }
        return null;
    }
}
