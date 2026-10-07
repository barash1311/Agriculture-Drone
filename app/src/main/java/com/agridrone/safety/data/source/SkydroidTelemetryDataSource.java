package com.agridrone.safety.data.source;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.agridrone.safety.data.model.TelemetrySnapshot;
import com.agridrone.safety.protocol.mavlink.MavlinkParser;
import com.agridrone.safety.protocol.mavlink.MavlinkTelemetryDecoder;
import com.skydroid.rcsdk.PipelineManager;
import com.skydroid.rcsdk.RCSDKManager;
import com.skydroid.rcsdk.SDKManagerCallBack;
import com.skydroid.rcsdk.comm.CommListener;
import com.skydroid.rcsdk.common.DeviceType;
import com.skydroid.rcsdk.common.error.SkyException;
import com.skydroid.rcsdk.common.pipeline.Pipeline;

public final class SkydroidTelemetryDataSource implements TelemetryDataSource {

    private static final String TAG = "SkydroidTelemetrySource";
    private static final long CONTROLLER_RETRY_DELAY_MS = 3000L;

    private final Context applicationContext;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private volatile TelemetryListener listener;
    private volatile boolean running;
    private boolean sdkInitialized;
    private volatile boolean aircraftDetected;
    private Pipeline pipeline;

    private final MavlinkTelemetryDecoder decoder = new MavlinkTelemetryDecoder(
            new MavlinkTelemetryDecoder.Listener() {
                @Override
                public void onAircraftDetected() {
                    if (!running || aircraftDetected) {
                        return;
                    }
                    aircraftDetected = true;
                    mainHandler.removeCallbacks(controllerRetry);
                    publishStatus("Drone MAVLink heartbeat detected.");
                    TelemetryListener currentListener = listener;
                    if (currentListener != null) {
                        currentListener.onConnected();
                    }
                }

                @Override
                public void onTelemetrySnapshot(TelemetrySnapshot snapshot) {
                    if (running && aircraftDetected) {
                        TelemetryListener currentListener = listener;
                        if (currentListener != null) {
                            currentListener.onTelemetrySnapshot(snapshot);
                        }
                    }
                }
            });
    private final MavlinkParser mavlinkParser = new MavlinkParser(decoder);
    private final Runnable controllerRetry = () -> {
        if (running && !aircraftDetected) {
            connectToController();
        }
    };

    private final SDKManagerCallBack sdkCallback = new SDKManagerCallBack() {
        @Override
        public void onRcConnected() {
            if (!running) {
                return;
            }
            DeviceType deviceType = RCSDKManager.INSTANCE.getDeviceType();
            if (deviceType != DeviceType.G20) {
                String deviceName = deviceType == null ? "unknown controller" : deviceType.name();
                reportError("Connected controller is " + deviceName
                        + "; this integration requires a Skydroid G20.");
                return;
            }
            mainHandler.removeCallbacks(controllerRetry);
            publishStatus("Skydroid G20 connected; opening its flight telemetry link.");
            connectTelemetryPipeline();
        }

        @Override
        public void onRcConnectFail(@Nullable SkyException error) {
            if (!running) {
                return;
            }
            String errorMessage = error == null ? null : error.getMessage();
            boolean controllerNotDetected = RCSDKManager.INSTANCE.getDeviceType() == DeviceType.UNKNOWN
                    && "Not yet implemented".equals(errorMessage);
            if (!controllerNotDetected) {
                Log.w(TAG, "Skydroid controller search failed", error);
            }
            publishStatus(controllerNotDetected
                    ? "No Skydroid controller detected. Connect a G20 to continue."
                    : "Searching for a Skydroid G20 controller. Connect the controller to continue.");
            scheduleControllerRetry();
        }

        @Override
        public void onRcDisconnect() {
            if (!running) {
                return;
            }
            aircraftDetected = false;
            pipeline = null;
            TelemetryListener currentListener = listener;
            if (currentListener != null) {
                currentListener.onDisconnected();
            }
            publishStatus("Controller disconnected; searching for the G20 again.");
            scheduleControllerRetry();
        }
    };

    public SkydroidTelemetryDataSource(@NonNull Context context) {
        this.applicationContext = context.getApplicationContext();
    }

    @Override
    public synchronized void setListener(TelemetryListener listener) {
        this.listener = listener;
    }

    @Override
    public synchronized void start() {
        if (running) {
            return;
        }
        running = true;
        aircraftDetected = false;
        decoder.reset();
        mavlinkParser.reset();
        publishStatus("Initializing Skydroid SDK and searching for a G20.");
        try {
            if (!sdkInitialized) {
                RCSDKManager.INSTANCE.initSDK(applicationContext, sdkCallback);
                RCSDKManager.INSTANCE.setMainThreadCallBack(true);
                sdkInitialized = true;
            }
            connectToController();
        } catch (RuntimeException | LinkageError error) {
            Log.e(TAG, "Unable to initialize the Skydroid SDK", error);
            running = false;
            reportError("Skydroid SDK could not start: " + error.getClass().getSimpleName());
        }
    }

    @Override
    public synchronized void stop() {
        if (!running) {
            return;
        }
        running = false;
        aircraftDetected = false;
        mainHandler.removeCallbacks(controllerRetry);
        Log.i(TAG, "Stopping Skydroid G20 telemetry pipeline");
        Pipeline activePipeline = pipeline;
        pipeline = null;
        if (activePipeline != null) {
            PipelineManager.INSTANCE.disconnectPipeline(activePipeline);
        }
        if (sdkInitialized) {
            RCSDKManager.INSTANCE.disconnectRC();
        }
        decoder.reset();
        mavlinkParser.reset();
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    private void connectToController() {
        if (!running) {
            return;
        }
        try {
            RCSDKManager.INSTANCE.connectToRC();
        } catch (RuntimeException | LinkageError error) {
            Log.e(TAG, "Unable to connect to the Skydroid G20", error);
            publishStatus("Controller search failed; retrying.");
            scheduleControllerRetry();
        }
    }

    private synchronized void connectTelemetryPipeline() {
        if (!running || pipeline != null) {
            return;
        }
        try {
            Pipeline newPipeline = PipelineManager.INSTANCE.createG12G20Pipeline();
            if (newPipeline == null) {
                reportError("Skydroid SDK did not provide a G20 telemetry pipeline.");
                return;
            }
            newPipeline.setOnCommListener(new CommListener() {
                @Override
                public void onConnectSuccess() {
                    if (running) {
                        publishStatus("G20 telemetry link is open; waiting for aircraft MAVLink heartbeat.");
                    }
                }

                @Override
                public void onConnectFail(SkyException error) {
                    if (running) {
                        publishStatus("G20 telemetry link is unavailable; waiting for it to reconnect.");
                        Log.w(TAG, "G20 telemetry pipeline connection failed", error);
                    }
                }

                @Override
                public void onDisconnect() {
                    if (running) {
                        publishStatus("G20 telemetry link disconnected; waiting for it to reconnect.");
                    }
                }

                @Override
                public void onReadData(byte[] data) {
                    if (running && data != null && data.length > 0) {
                        mavlinkParser.parseBytes(data, data.length);
                    }
                }
            });
            pipeline = newPipeline;
            PipelineManager.INSTANCE.connectPipeline(newPipeline);
        } catch (RuntimeException | LinkageError error) {
            Log.e(TAG, "Unable to open the G20 telemetry pipeline", error);
            reportError("Could not open the G20 telemetry pipeline: "
                    + error.getClass().getSimpleName());
        }
    }

    private void scheduleControllerRetry() {
        mainHandler.removeCallbacks(controllerRetry);
        mainHandler.postDelayed(controllerRetry, CONTROLLER_RETRY_DELAY_MS);
    }

    private void publishStatus(String status) {
        TelemetryListener currentListener = listener;
        if (currentListener != null) {
            currentListener.onStatus(status);
        }
    }

    private void reportError(String message) {
        TelemetryListener currentListener = listener;
        if (currentListener != null) {
            currentListener.onError(message);
        }
    }
}
