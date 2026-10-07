package com.agridrone.safety.domain.safety;

import com.agridrone.safety.config.SafetyConfig;

import java.util.ArrayDeque;
import java.util.Deque;

public final class RapidSagDetector {

    private static final class VoltageSample {
        final double voltage;
        final long timestampMillis;

        VoltageSample(double voltage, long timestampMillis) {
            this.voltage = voltage;
            this.timestampMillis = timestampMillis;
        }
    }

    private final double thresholdVoltsPerSec;
    private final long windowMillis;
    private final Deque<VoltageSample> samples = new ArrayDeque<>();

    public RapidSagDetector() {
        this(SafetyConfig.RAPID_SAG_THRESHOLD_VOLTS_PER_SEC, SafetyConfig.RAPID_SAG_WINDOW_MS);
    }

    public RapidSagDetector(double thresholdVoltsPerSec, long windowMillis) {
        this.thresholdVoltsPerSec = thresholdVoltsPerSec;
        this.windowMillis = windowMillis;
    }

    /**
     * Evaluates current voltage reading.
     * Returns true if rapid voltage collapse is detected.
     */
    public synchronized boolean evaluate(Double voltageVolts, long timestampMillis) {
        if (voltageVolts == null || Double.isNaN(voltageVolts) || timestampMillis <= 0) {
            return false;
        }

        // Clean out samples older than the detection window
        while (!samples.isEmpty() && (timestampMillis - samples.peekFirst().timestampMillis) > windowMillis) {
            samples.pollFirst();
        }

        samples.addLast(new VoltageSample(voltageVolts, timestampMillis));

        if (samples.size() < 2) {
            return false;
        }

        VoltageSample oldest = samples.peekFirst();
        VoltageSample newest = samples.peekLast();
        if (oldest == null || newest == null) {
            return false;
        }

        long elapsedMs = newest.timestampMillis - oldest.timestampMillis;
        if (elapsedMs < 400L) { // Minimum sample baseline to avoid single-tick spikes
            return false;
        }

        double voltageDrop = oldest.voltage - newest.voltage;
        if (voltageDrop <= 0.0) {
            return false;
        }

        double dropRateVoltsPerSec = (voltageDrop / elapsedMs) * 1000.0;
        return dropRateVoltsPerSec >= thresholdVoltsPerSec;
    }

    public synchronized void reset() {
        samples.clear();
    }
}
