package com.agridrone.safety.domain.battery;

import java.util.ArrayDeque;
import java.util.Deque;

public final class DischargeRateEstimator {

    private static final class Sample {
        final double consumedMah;
        final double batteryPercent;
        final long timestampMillis;

        Sample(double consumedMah, double batteryPercent, long timestampMillis) {
            this.consumedMah = consumedMah;
            this.batteryPercent = batteryPercent;
            this.timestampMillis = timestampMillis;
        }
    }

    private final int maxSamples;
    private final Deque<Sample> window;

    public DischargeRateEstimator(int maxSamples) {
        this.maxSamples = Math.max(2, maxSamples);
        this.window = new ArrayDeque<>(this.maxSamples);
    }

    public synchronized void addSample(Double consumedMah, Double batteryPercent, long timestampMillis) {
        if (consumedMah == null || batteryPercent == null || timestampMillis < 0) {
            return;
        }
        if (window.size() >= maxSamples) {
            window.pollFirst();
        }
        window.addLast(new Sample(consumedMah, batteryPercent, timestampMillis));
    }

    /**
     * Computes consumption rate in mAh/minute.
     * Returns Double.NaN if insufficient history exists (< 2 samples or < 500 ms elapsed).
     */
    public synchronized double getConsumptionMahPerMinute() {
        if (window.size() < 2) {
            return Double.NaN;
        }
        Sample oldest = window.peekFirst();
        Sample newest = window.peekLast();
        if (oldest == null || newest == null) {
            return Double.NaN;
        }

        long elapsedMillis = newest.timestampMillis - oldest.timestampMillis;
        if (elapsedMillis < 500L) {
            return Double.NaN;
        }

        double deltaMah = newest.consumedMah - oldest.consumedMah;
        if (deltaMah <= 0.0) {
            return 0.0;
        }

        double elapsedMinutes = (double) elapsedMillis / 60000.0;
        return deltaMah / elapsedMinutes;
    }

    /**
     * Computes discharge rate in percent per second (%/sec) for dynamic RTL calculation.
     * Returns Double.NaN if insufficient history exists.
     */
    public synchronized double getDischargeRatePercentPerSecond() {
        if (window.size() < 2) {
            return Double.NaN;
        }
        Sample oldest = window.peekFirst();
        Sample newest = window.peekLast();
        if (oldest == null || newest == null) {
            return Double.NaN;
        }

        long elapsedMillis = newest.timestampMillis - oldest.timestampMillis;
        if (elapsedMillis < 500L) {
            return Double.NaN;
        }

        double deltaPercent = oldest.batteryPercent - newest.batteryPercent;
        if (deltaPercent <= 0.0) {
            return 0.0;
        }

        double elapsedSeconds = (double) elapsedMillis / 1000.0;
        return deltaPercent / elapsedSeconds;
    }

    public synchronized void reset() {
        window.clear();
    }
}
