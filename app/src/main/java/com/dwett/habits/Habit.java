package com.dwett.habits;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

@Entity
class Habit {
    @PrimaryKey(autoGenerate = true)
    int id;

    @ColumnInfo(name = "title")
    String title;

    /**
     * Number of hours
     */
    @ColumnInfo(name = "period")
    int period;

    /**
     * Number of times / period
     */
    @ColumnInfo(name = "frequency")
    int frequency;
}
