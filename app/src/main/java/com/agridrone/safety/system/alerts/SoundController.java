package com.agridrone.safety.system.alerts;

import android.media.AudioManager;
import android.media.ToneGenerator;
import android.util.Log;

import com.agridrone.safety.data.model.SafetyState;

public final class SoundController {

    private static final String TAG = "SoundController";

    private ToneGenerator toneGenerator;

    public SoundController() {
        try {
            toneGenerator = new ToneGenerator(AudioManager.STREAM_ALARM, 85);
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize ToneGenerator", e);
        }
    }

    public synchronized void playSoundForState(SafetyState state) {
        if (toneGenerator == null || state == null) {
            return;
        }

        try {
            switch (state) {
                case NOTICE:
                    toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 200);
                    break;
                case WARNING:
                    toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 350);
                    break;
                case CELL_FAULT:
                    toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP2, 400);
                    break;
                case CRITICAL:
                    toneGenerator.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 600);
                    break;
                case EMERGENCY:
                    toneGenerator.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 800);
                    break;
                case NORMAL:
                default:
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to play safety sound", e);
        }
    }

    public synchronized void stop() {
        if (toneGenerator != null) {
            try {
                toneGenerator.stopTone();
            } catch (Exception ignored) {
            }
        }
    }

    public synchronized void release() {
        if (toneGenerator != null) {
            try {
                toneGenerator.release();
            } catch (Exception ignored) {
            }
            toneGenerator = null;
        }
    }
}
