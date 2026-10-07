package com.agridrone.safety.domain.safety;

import com.agridrone.safety.data.model.AlertPriority;
import com.agridrone.safety.data.model.SafetyState;

import java.util.Objects;

public final class SafetyEvaluation {

    private final SafetyState primaryState;
    private final String reason;
    private final String displayMessage;
    private final String instruction;
    private final int priority;

    public SafetyEvaluation(SafetyState primaryState, String reason, String displayMessage, String instruction) {
        this.primaryState = primaryState != null ? primaryState : SafetyState.NORMAL;
        this.reason = reason != null ? reason : "";
        this.displayMessage = displayMessage != null ? displayMessage : "";
        this.instruction = instruction != null ? instruction : "";
        this.priority = AlertPriority.getPriorityLevel(this.primaryState);
    }

    public static SafetyEvaluation normal() {
        return new SafetyEvaluation(SafetyState.NORMAL, "System nominal", "SAFE", "");
    }

    public SafetyState getPrimaryState() {
        return primaryState;
    }

    public String getReason() {
        return reason;
    }

    public String getDisplayMessage() {
        return displayMessage;
    }

    public String getInstruction() {
        return instruction;
    }

    public int getPriority() {
        return priority;
    }

    public boolean isHazardous() {
        return primaryState == SafetyState.CRITICAL || primaryState == SafetyState.EMERGENCY;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SafetyEvaluation that = (SafetyEvaluation) o;
        return priority == that.priority &&
                primaryState == that.primaryState &&
                Objects.equals(reason, that.reason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(primaryState, reason, priority);
    }
}
