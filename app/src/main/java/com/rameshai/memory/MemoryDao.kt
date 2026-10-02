package com.rameshai.memory

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Insert
    suspend fun insert(entry: MemoryEntity): Long

    @Delete
    suspend fun delete(entry: MemoryEntity)

    @Query("DELETE FROM memory_entries")
    suspend fun clearAll()

    @Query("DELETE FROM memory_entries WHERE kind = :kind")
    suspend fun clearByKind(kind: MemoryKind)

    @Query("SELECT * FROM memory_entries ORDER BY createdAtMillis DESC")
    fun observeAll(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memory_entries WHERE kind = 'SHORT_TERM' ORDER BY createdAtMillis DESC LIMIT :limit")
    suspend fun recentShortTerm(limit: Int): List<MemoryEntity>

    @Query("SELECT * FROM memory_entries WHERE kind = 'LONG_TERM_PREFERENCE' ORDER BY createdAtMillis DESC")
    suspend fun allPreferences(): List<MemoryEntity>
}
