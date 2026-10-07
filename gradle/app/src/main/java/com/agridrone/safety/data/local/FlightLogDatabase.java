package com.agridrone.safety.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

@Database(entities = {FlightLogEntity.class}, version = 1, exportSchema = false)
@TypeConverters({CellVoltageConverter.class})
public abstract class FlightLogDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "agridrone_flight_log.db";
    private static volatile FlightLogDatabase sInstance;

    public abstract FlightLogDao flightLogDao();

    public static FlightLogDatabase getInstance(Context context) {
        if (sInstance == null) {
            synchronized (FlightLogDatabase.class) {
                if (sInstance == null) {
                    sInstance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            FlightLogDatabase.class,
                            DATABASE_NAME
                    ).fallbackToDestructiveMigration().build();
                }
            }
        }
        return sInstance;
    }
}
