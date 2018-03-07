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

    @ColumnInfo(name = "period")
    int period;

    @ColumnInfo(name = "frequency")
    int frequency;
}
