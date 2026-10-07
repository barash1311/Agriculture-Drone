package com.agridrone.safety.system.alerts;

import android.content.Context;
import android.os.Build;
import android.os.CombinedVibration;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.Log;

import androidx.annotation.NonNull;

import com.agridrone.safety.data.model.SafetyState;

public final class VibrationController {

    private static final String TAG = "VibrationController";

    private final Vibrator vibrator;
    private final VibratorManager vibratorManager;

    public VibrationController(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            this.vibratorManager = (VibratorManager) appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            this.vibrator = vibratorManager != null ? vibratorManager.getDefaultVibrator() : null;
        } else {
            this.vibratorManager = null;
            this.vibrator = (Vibrator) appContext.getSystemService(Context.VIBRATOR_SERVICE);
        }
    }

    public void vibrateForState(SafetyState state) {
        if (vibrator == null || !vibrator.hasVibrator() || state == null) {
            return;
        }

        try {
            switch (state) {
                case NOTICE:
                    vibrateEffect(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE));
                    break;
                case WARNING:
                    vibrateEffect(VibrationEffect.createWaveform(new long[]{0, 200, 150, 200}, -1));
                    break;
                case CELL_FAULT:
                    vibrateEffect(VibrationEffect.createWaveform(new long[]{0, 250, 100, 250, 100, 250}, -1));
                    break;
                case CRITICAL:
                    vibrateEffect(VibrationEffect.createWaveform(new long[]{0, 400, 150, 400, 150, 400}, -1));
                    break;
                case EMERGENCY:
                    vibrateEffect(VibrationEffect.createWaveform(new long[]{0, 500, 100, 500, 100, 500, 100, 500}, -1));
                    break;
                case NORMAL:
                default:
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error triggering vibration", e);
        }
    }

    private void vibrateEffect(VibrationEffect effect) {
        if (vibrator == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && vibratorManager != null) {
            CombinedVibration combined = CombinedVibration.createParallel(effect);
            vibratorManager.vibrate(combined);
        } else {
            vibrator.vibrate(effect);
        }
    }

    public void cancel() {
        if (vibrator != null) {
            try {
                vibrator.cancel();
            } catch (Exception ignored) {
            }
        }
    }
}
