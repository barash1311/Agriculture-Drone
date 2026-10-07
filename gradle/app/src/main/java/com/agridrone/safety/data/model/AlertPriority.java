package com.agridrone.safety.data.model;

public final class AlertPriority {

    private AlertPriority() {
    }

    public static int getPriorityLevel(SafetyState state) {
        if (state == null) {
            return 0;
        }
        switch (state) {
            case EMERGENCY:
                return 6;
            case CRITICAL:
                return 5;
            case CELL_FAULT:
                return 4;
            case WARNING:
                return 3;
            case NOTICE:
                return 2;
            case NORMAL:
            default:
                return 1;
        }
    }

    public static boolean isHigherPriority(SafetyState candidate, SafetyState current) {
        return getPriorityLevel(candidate) > getPriorityLevel(current);
    }
}
