package com.agridrone.safety.system.alerts;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import androidx.annotation.NonNull;

import com.agridrone.safety.data.model.SafetyState;

import java.util.Locale;

public final class TtsController implements TextToSpeech.OnInitListener {

    private static final String TAG = "TtsController";

    private TextToSpeech textToSpeech;
    private volatile boolean isInitialized = false;
    private volatile boolean isEnabled = true;

    private long lastPeriodicSpokenMillis = 0L;

    public TtsController(@NonNull Context context) {
        try {
            textToSpeech = new TextToSpeech(context.getApplicationContext(), this);
        } catch (Exception e) {
            Log.e(TAG, "Failed to instantiate TextToSpeech", e);
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS && textToSpeech != null) {
            int result = textToSpeech.setLanguage(Locale.US);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w(TAG, "TTS US English not supported, falling back to default locale");
                textToSpeech.setLanguage(Locale.getDefault());
            }
            textToSpeech.setSpeechRate(1.05f);
            isInitialized = true;
            Log.i(TAG, "TextToSpeech successfully initialized");
        } else {
            Log.e(TAG, "TextToSpeech initialization failed with status: " + status);
            isInitialized = false;
        }
    }

    public boolean isReady() {
        return isInitialized && isEnabled;
    }

    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
        if (!enabled && textToSpeech != null) {
            textToSpeech.stop();
        }
    }

    /**
     * Speaks safety alerts according to strict priority order.
     * High priority alerts (CRITICAL, EMERGENCY) immediately flush any ongoing speech.
     */
    public synchronized void speakAlertForState(SafetyState state) {
        if (!isReady() || state == null) {
            return;
        }

        switch (state) {
            case EMERGENCY:
                speak("Land immediately. Emergency battery condition.", TextToSpeech.QUEUE_FLUSH);
                break;
            case CRITICAL:
                speak("Return to launch. Battery critical.", TextToSpeech.QUEUE_FLUSH);
                break;
            case CELL_FAULT:
                speak("Cell voltage imbalance. Land and inspect battery.", TextToSpeech.QUEUE_FLUSH);
                break;
            case WARNING:
                speak("Warning. Low battery.", TextToSpeech.QUEUE_ADD);
                break;
            case NOTICE:
                speak("Plan to return soon.", TextToSpeech.QUEUE_ADD);
                break;
            case NORMAL:
            default:
                break;
        }
    }

    /**
     * Periodic status telemetry speech (approximately every 60 seconds).
     * Example: "Battery 48 percent. Cell average 3.82 volts."
     */
    public synchronized void maybeSpeakPeriodicTelemetry(Double batteryPercent, Double cellAverageVolts, long currentMillis) {
        if (!isReady() || batteryPercent == null || cellAverageVolts == null) {
            return;
        }

        if (currentMillis - lastPeriodicSpokenMillis >= 60000L) {
            lastPeriodicSpokenMillis = currentMillis;
            String text = String.format(Locale.US, "Battery %.0f percent. Cell average %.2f volts.",
                    batteryPercent, cellAverageVolts);
            speak(text, TextToSpeech.QUEUE_ADD);
        }
    }

    private void speak(String text, int queueMode) {
        if (textToSpeech != null && isInitialized && isEnabled) {
            textToSpeech.speak(text, queueMode, null, "agri_alert_" + System.currentTimeMillis());
        }
    }

    public synchronized void stop() {
        if (textToSpeech != null) {
            textToSpeech.stop();
        }
    }

    public synchronized void shutdown() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
        }
        isInitialized = false;
    }
}
