package com.rameshai.memory

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MemoryKind { SHORT_TERM, LONG_TERM_PREFERENCE }

@Entity(tableName = "memory_entries")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: MemoryKind,
    val content: String,
    val createdAtMillis: Long = System.currentTimeMillis()
)
