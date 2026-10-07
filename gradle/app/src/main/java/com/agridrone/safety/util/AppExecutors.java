package com.agridrone.safety.util;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class AppExecutors {

    private static volatile AppExecutors sInstance;

    private final ExecutorService telemetryExecutor;
    private final ExecutorService databaseExecutor;
    private final ScheduledExecutorService scheduledExecutor;
    private final Executor mainThreadExecutor;

    private AppExecutors() {
        this.telemetryExecutor = Executors.newSingleThreadExecutor(new NamedThreadFactory("AEP-Telemetry"));
        this.databaseExecutor = Executors.newSingleThreadExecutor(new NamedThreadFactory("AEP-Database"));
        this.scheduledExecutor = Executors.newScheduledThreadPool(2, new NamedThreadFactory("AEP-Scheduled"));
        this.mainThreadExecutor = new MainThreadExecutor();
    }

    public static AppExecutors getInstance() {
        if (sInstance == null) {
            synchronized (AppExecutors.class) {
                if (sInstance == null) {
                    sInstance = new AppExecutors();
                }
            }
        }
        return sInstance;
    }

    public ExecutorService telemetry() {
        return telemetryExecutor;
    }

    public ExecutorService database() {
        return databaseExecutor;
    }

    public ScheduledExecutorService scheduled() {
        return scheduledExecutor;
    }

    public Executor mainThread() {
        return mainThreadExecutor;
    }

    private static final class MainThreadExecutor implements Executor {
        private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(@NonNull Runnable command) {
            mainThreadHandler.post(command);
        }
    }

    private static final class NamedThreadFactory implements ThreadFactory {
        private final String baseName;
        private final AtomicInteger threadNum = new AtomicInteger(1);

        NamedThreadFactory(String baseName) {
            this.baseName = baseName;
        }

        @Override
        public Thread newThread(@NonNull Runnable r) {
            Thread t = new Thread(r, baseName + "-" + threadNum.getAndIncrement());
            t.setDaemon(true);
            return t;
        }
    }
}
