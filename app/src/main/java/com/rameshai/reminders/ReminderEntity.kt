package com.rameshai.reminders

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Recurrence pattern for a reminder. CUSTOM_DAYS uses [ReminderEntity.daysOfWeekMask]. */
enum class Recurrence { NONE, DAILY, WEEKLY, CUSTOM_DAYS }

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val message: String,
    val triggerAtMillis: Long,
    val recurrence: Recurrence,
    /** Bitmask Sun=1,Mon=2,Tue=4,Wed=8,Thu=16,Fri=32,Sat=64 — only used for CUSTOM_DAYS/WEEKLY. */
    val daysOfWeekMask: Int = 0,
    val enabled: Boolean = true
)
