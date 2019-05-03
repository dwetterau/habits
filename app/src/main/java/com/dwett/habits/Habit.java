package com.dwett.habits;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

import java.util.Arrays;

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

    public static Habit fromCSV(String csv) {
        String[] parts = csv.split(",");
        if (parts.length < 5) {
            throw new RuntimeException("invalid habit csv");
        }
        Habit h = new Habit();
        h.id = Integer.parseInt(parts[0]);
        h.period = Integer.parseInt(parts[1]);
        h.frequency = Integer.parseInt(parts[2]);
        h.archived = Boolean.parseBoolean(parts[3]);
        h.title = String.join(",", Arrays.copyOfRange(parts, 4, parts.length));
        return h;
    }
}
