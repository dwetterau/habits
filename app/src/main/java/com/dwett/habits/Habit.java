package com.dwett.habits;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

@Entity
class Habit {
    @PrimaryKey(autoGenerate = true)
    long id;

    @ColumnInfo(name = "title")
    String title;

    /**
     * Number of hours
     * e.g. For a X times / week event. This should be 7 * 24.
     */
    @ColumnInfo(name = "period")
    int period;

    /**
     * Number of times / period
     * e.g. For a X times / week event. This should be X.
     */
    @ColumnInfo(name = "frequency")
    int frequency;

    /**
     * If true, the habit is hidden from the UI, but still exists.
     */
    @ColumnInfo(name = "archived_2")
    boolean archived;

    @NonNull
    public static String csvHeader() {
        return "id,period,frequency,archived,title\n";
    }

    public String csv() {
        return id + "," + period + "," + frequency + "," + archived + "," + title;
    }
}
