package com.agridrone.safety.app;

import android.app.Application;

import com.agridrone.safety.data.local.FlightLogDatabase;
import com.agridrone.safety.util.AppExecutors;

public final class AgriDroneApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        AppExecutors.getInstance();
        FlightLogDatabase.getInstance(this);
    }
}
