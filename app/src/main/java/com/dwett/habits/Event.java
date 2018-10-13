package com.dwett.habits;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.PrimaryKey;

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

    public void maybeAdjustTimestampToPreviousDay() {
        // If the time is between midnight and 3am, we want to record it for the
        // previous day at 11:59pm instead.
        LocalDateTime dt = Instant.ofEpochMilli(this.timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
        if (dt.getHour() < 3) {
            dt = dt.minusHours(dt.getHour() + 1);
            dt = dt.plusMinutes(59 - dt.getMinute());
            this.timestamp = dt.atZone(ZoneId.systemDefault()).toEpochSecond() * 1000L;
        }
    }
}
