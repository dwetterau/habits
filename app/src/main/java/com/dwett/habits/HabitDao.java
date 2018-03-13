package com.dwett.habits;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

@Dao
public interface HabitDao {
    @Insert
    long insertNewHabit(Habit h);

    @Insert
    void insertNewEvent(Event e);

    @Delete
    void deleteHabit(Habit h);

    @Delete
    void deleteEvent(Event e);

    @Query("SELECT * FROM habit")
    Habit[] loadAllHabits();

    @Query("SELECT * FROM event where habit_id = :habitId")
    Event[] loadEventsForHabit(long habitId);
}
