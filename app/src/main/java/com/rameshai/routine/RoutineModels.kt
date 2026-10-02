package com.rameshai.routine

/** A single routine entry, e.g. "07:00 Wake up". Time stored as "HH:mm" 24h. */
data class RoutineItem(val time: String, val activity: String, val dayCategory: DayCategory = DayCategory.EVERYDAY)

enum class DayCategory { EVERYDAY, WEEKDAY, WEEKEND }
