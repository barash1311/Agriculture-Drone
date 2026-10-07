package com.agridrone.safety.system.spray;

import android.util.Log;

public final class MockSprayControlGateway implements SprayControlGateway {

    private static final String TAG = "MockSprayControlGateway";
    private volatile boolean cutoffTriggered = false;

    @Override
    public synchronized boolean requestPumpCutoff(String reason) {
        cutoffTriggered = true;
        Log.i(TAG, "MOCK ACTION DISPATCHED: Agricultural spray pump cutoff executed. Reason: " + reason);
        return true;
    }

    @Override
    public synchronized boolean isCutoffTriggered() {
        return cutoffTriggered;
    }

    @Override
    public synchronized void resetCutoff() {
        cutoffTriggered = false;
        Log.i(TAG, "MOCK ACTION: Spray pump cutoff reset.");
    }
}
