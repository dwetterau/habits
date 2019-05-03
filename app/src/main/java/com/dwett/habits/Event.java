package com.dwett.habits;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity(foreignKeys = @ForeignKey(
        entity = Habit.class,
        parentColumns = "id",
        childColumns = "habit_id",
        onDelete = ForeignKey.CASCADE
))
public class Event {
    @PrimaryKey(autoGenerate = true)
    long id;

    @ColumnInfo(name = "habit_id")
    long habitId;

    /**
     * An output of `System.currentTimeMillis()`
     */
    @ColumnInfo(name = "timestamp")
    long timestamp;

    // Returns true if the time was adjusted, false otherwise
    public boolean maybeAdjustTimestampToPreviousDay() {
        // If the time is between midnight and 3am, we want to record it for the
        // previous day at 11:59pm instead.
        LocalDateTime dt = Instant.ofEpochMilli(this.timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
        if (dt.getHour() < 3) {
            dt = dt.minusHours(dt.getHour() + 1);
            dt = dt.plusMinutes(59 - dt.getMinute());
            this.timestamp = dt.atZone(ZoneId.systemDefault()).toEpochSecond() * 1000L;
            return true;
        }
        return false;
    }

    @NonNull
    public static String csvHeader() {
        return "id,habitId,timestamp\n";
    }

    public String csv() {
        return id + "," + habitId + "," + timestamp;
    }

    public static Event fromCSV(String csv) {
        String[] parts = csv.split(",");
        if (parts.length != 3) {
            throw new RuntimeException("invalid event csv");
        }
        Event e = new Event();
        e.id = Integer.parseInt(parts[0]);
        e.habitId = Integer.parseInt(parts[1]);
        e.timestamp = Long.parseLong(parts[2]);
        return e;
    }
}
