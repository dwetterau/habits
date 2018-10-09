package com.dwett.habits;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

@Database(entities = {Habit.class, Event.class}, version = 2)
abstract class HabitDatabase extends RoomDatabase {
    abstract HabitDao habitDao();
}
