package com.agridrone.safety.util;

import java.util.ArrayDeque;
import java.util.Deque;

public final class RollingAverage {

    private final int maxSamples;
    private final Deque<Double> window;
    private double sum;

    public RollingAverage(int maxSamples) {
        if (maxSamples <= 0) {
            throw new IllegalArgumentException("maxSamples must be > 0");
        }
        this.maxSamples = maxSamples;
        this.window = new ArrayDeque<>(maxSamples);
        this.sum = 0.0;
    }

    public synchronized void addSample(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return;
        }
        if (window.size() >= maxSamples) {
            Double oldest = window.pollFirst();
            if (oldest != null) {
                sum -= oldest;
            }
        }
        window.addLast(value);
        sum += value;
    }

    public synchronized double getAverage() {
        if (window.isEmpty()) {
            return Double.NaN;
        }
        return sum / window.size();
    }

    public synchronized int getSampleCount() {
        return window.size();
    }

    public synchronized void clear() {
        window.clear();
        sum = 0.0;
    }
}
