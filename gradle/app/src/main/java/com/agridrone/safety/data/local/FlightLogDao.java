package com.agridrone.safety.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface FlightLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(FlightLogEntity entity);

    @Query("SELECT * FROM flight_logs ORDER BY timestamp_millis DESC LIMIT :limit")
    List<FlightLogEntity> getRecentLogs(int limit);

    @Query("SELECT * FROM flight_logs WHERE timestamp_millis BETWEEN :startMillis AND :endMillis ORDER BY timestamp_millis ASC")
    List<FlightLogEntity> getLogsBetween(long startMillis, long endMillis);

    @Query("SELECT COUNT(*) FROM flight_logs")
    long getLogCount();

    @Query("DELETE FROM flight_logs")
    void clearAll();
}
