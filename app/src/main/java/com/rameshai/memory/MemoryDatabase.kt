package com.rameshai.memory

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class MemoryConverters {
    @TypeConverter fun toKind(value: String) = MemoryKind.valueOf(value)
    @TypeConverter fun fromKind(value: MemoryKind) = value.name
}

@Database(entities = [MemoryEntity::class], version = 1, exportSchema = false)
@TypeConverters(MemoryConverters::class)
abstract class MemoryDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao

    companion object {
        fun build(context: Context): MemoryDatabase = Room.databaseBuilder(
            context.applicationContext, MemoryDatabase::class.java, "memory.db"
        ).build()
    }
}
