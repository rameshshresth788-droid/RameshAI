package com.rameshai.reminders

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter
    fun toRecurrence(value: String) = Recurrence.valueOf(value)
    @TypeConverter
    fun fromRecurrence(value: Recurrence) = value.name
}

@Database(entities = [ReminderEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class ReminderDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao

    companion object {
        fun build(context: Context): ReminderDatabase = Room.databaseBuilder(
            context.applicationContext, ReminderDatabase::class.java, "reminders.db"
        ).build()
    }
}
