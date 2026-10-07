package com.agridrone.safety.system.spray;

/**
 * Gateway interface for agricultural spray pump actuator commands (Spec Section 51).
 * <p>
 * HARD HARDWARE ISOLATION BOUNDARY:
 * Actuator electrical interface (PWM channel, relay, CAN bus, MAV_CMD_DO_SET_SERVO)
 * is unspecified for the target spray mechanism.
 * Production implementations must only be added when the actual hardware command contract is known.
 */
public interface SprayControlGateway {

    /**
     * Requests immediate shutdown/cutoff of the agricultural spraying pump.
     *
     * @param reason diagnostic explanation for shutdown request
     * @return true if shutdown command was successfully dispatched to controller/hardware
     */
    boolean requestPumpCutoff(String reason);

    /** Returns true if the pump shutdown was triggered */
    boolean isCutoffTriggered();

    /** Resets the cutoff interlock state */
    void resetCutoff();
}
