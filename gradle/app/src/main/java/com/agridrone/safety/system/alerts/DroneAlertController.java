package com.agridrone.safety.system.alerts;

import android.content.Context;
import android.os.SystemClock;

import androidx.annotation.NonNull;

import com.agridrone.safety.data.model.SafetyState;
import com.agridrone.safety.domain.safety.SafetyEvaluation;

public final class DroneAlertController {

    private final TtsController ttsController;
    private final SoundController soundController;
    private final VibrationController vibrationController;

    private SafetyState previousSafetyState = SafetyState.NORMAL;
    private long lastAlertDispatchedMillis = 0L;

    public DroneAlertController(@NonNull Context context) {
        this.ttsController = new TtsController(context);
        this.soundController = new SoundController();
        this.vibrationController = new VibrationController(context);
    }

    public TtsController getTtsController() {
        return ttsController;
    }

    public synchronized void onSafetyEvaluation(
            SafetyEvaluation evaluation,
            Double batteryPercent,
            Double cellAverageVolts,
            long currentTimestampMillis) {

        if (evaluation == null) {
            return;
        }

        SafetyState currentState = evaluation.getPrimaryState();
        long now = SystemClock.elapsedRealtime();

        boolean stateChanged = (currentState != previousSafetyState);

        long repeatIntervalMs;
        switch (currentState) {
            case EMERGENCY:
                repeatIntervalMs = 5000L;
                break;
            case CRITICAL:
                repeatIntervalMs = 8000L;
                break;
            case CELL_FAULT:
            case WARNING:
                repeatIntervalMs = 18000L;
                break;
            case NOTICE:
                repeatIntervalMs = 45000L;
                break;
            case NORMAL:
            default:
                repeatIntervalMs = Long.MAX_VALUE;
                break;
        }

        boolean intervalElapsed = (now - lastAlertDispatchedMillis) >= repeatIntervalMs;

        if (stateChanged || intervalElapsed) {
            if (currentState != SafetyState.NORMAL) {
                vibrationController.vibrateForState(currentState);

                soundController.playSoundForState(currentState);

                ttsController.speakAlertForState(currentState);

                lastAlertDispatchedMillis = now;
            } else {
                soundController.stop();
                vibrationController.cancel();
            }
            previousSafetyState = currentState;
        }

        if (currentState == SafetyState.NORMAL) {
            ttsController.maybeSpeakPeriodicTelemetry(batteryPercent, cellAverageVolts, currentTimestampMillis);
        }
    }

    public synchronized void release() {
        soundController.release();
        vibrationController.cancel();
        ttsController.shutdown();
    }
}
