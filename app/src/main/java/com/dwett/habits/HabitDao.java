package com.dwett.habits;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

@Dao
public interface HabitDao {
    @Insert
    long insertNewHabit(Habit h);

    @Delete
    void deleteHabit(Habit h);

    @Update
    void updateHabit(Habit h);

    @Insert
    void insertNewEvent(Event e);

    @Delete
    void deleteEvent(Event e);

    @Update
    void updateEvent(Event e);

    @Query("SELECT * FROM habit")
    Habit[] loadAllHabits();

    @Query("SELECT * FROM habit WHERE id = :habitId")
    Habit loadHabit(long habitId);

    @Query("SELECT * FROM event where habit_id = :habitId")
    Event[] loadEventsForHabit(long habitId);
}
