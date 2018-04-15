package com.dwett.habits;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.PrimaryKey;

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
}
