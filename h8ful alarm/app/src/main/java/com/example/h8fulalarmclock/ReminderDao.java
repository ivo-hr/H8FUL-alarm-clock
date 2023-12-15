package com.example.h8fulalarmclock;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ReminderDao {
    @Query("SELECT * FROM ReminderEntity")
    List<ReminderEntity> getAll();

    @Query("SELECT * FROM ReminderEntity WHERE id = :id")
    ReminderEntity getById(int id);

    @Insert
    void insert(ReminderEntity reminder);

    @Delete
    void delete(ReminderEntity reminder);

    @Update
    void update(ReminderEntity reminder);

    @Query("SELECT * FROM ReminderEntity WHERE isEnabled = 1")
    List<ReminderEntity> getActiveReminders();
}