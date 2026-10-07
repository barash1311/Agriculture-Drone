package com.agridrone.safety.data.local;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import java.util.List;

/**
 * Room Entity representing a blackbox flight telemetry record (Spec Section 53 & 55).
 */
@Entity(tableName = "flight_logs", indices = {
        @Index("timestamp_millis"),
        @Index("safety_state")
})
public final class FlightLogEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "timestamp_millis")
    public long timestampMillis;

    @ColumnInfo(name = "latitude")
    public Double latitude;

    @ColumnInfo(name = "longitude")
    public Double longitude;

    @ColumnInfo(name = "altitude_meters")
    public Double altitudeMeters;

    @ColumnInfo(name = "ground_speed_mps")
    public Double groundSpeedMps;

    @ColumnInfo(name = "battery_percent")
    public Double batteryPercent;

    @ColumnInfo(name = "pack_voltage_volts")
    public Double packVoltageVolts;

    @ColumnInfo(name = "resting_voltage_volts")
    public Double restingVoltageVolts;

    @ColumnInfo(name = "current_amps")
    public Double currentAmps;

    @ColumnInfo(name = "temperature_celsius")
    public Double temperatureCelsius;

    @ColumnInfo(name = "cell_count")
    public int cellCount;

    @ColumnInfo(name = "cell_min_volts")
    public Double cellMinVolts;

    @ColumnInfo(name = "cell_max_volts")
    public Double cellMaxVolts;

    @ColumnInfo(name = "cell_average_volts")
    public Double cellAverageVolts;

    @ColumnInfo(name = "cell_delta_volts")
    public Double cellDeltaVolts;

    @TypeConverters(CellVoltageConverter.class)
    @ColumnInfo(name = "cell_voltages")
    public List<Double> cellVoltages;

    @ColumnInfo(name = "consumption_rate_mah_per_minute")
    public Double consumptionRateMahPerMinute;

    @ColumnInfo(name = "discharge_rate_percent_per_second")
    public Double dischargeRatePercentPerSecond;

    @ColumnInfo(name = "remaining_flight_time_seconds")
    public Long remainingFlightTimeSeconds;

    @ColumnInfo(name = "distance_home_meters")
    public Double distanceHomeMeters;

    @ColumnInfo(name = "required_rtl_battery_percent")
    public Double requiredRtlBatteryPercent;

    @ColumnInfo(name = "safety_state")
    public String safetyState;

    @ColumnInfo(name = "connection_state")
    public String connectionState;

    @ColumnInfo(name = "spray_safety_enabled")
    public boolean spraySafetyEnabled;

    @ColumnInfo(name = "spray_shutdown_requested")
    public boolean sprayShutdownRequested;

    public FlightLogEntity() {
    }
}
