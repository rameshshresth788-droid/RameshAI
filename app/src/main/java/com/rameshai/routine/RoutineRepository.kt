package com.rameshai.routine

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate

private val Context.routineDataStore by preferencesDataStore(name = "routine_prefs")

/**
 * User-editable daily routine (morning/work/evening/night blocks), stored as a
 * simple JSON array in DataStore. No medical claims are made anywhere in this
 * module — it only reflects times and activities the user themselves entered.
 */
class RoutineRepository(private val context: Context) {

    private val key = stringPreferencesKey("routine_items")

    suspend fun getAll(): List<RoutineItem> {
        val json = context.routineDataStore.data.first()[key] ?: return defaultRoutine()
        return parse(json)
    }

    suspend fun getTodayRoutine(): List<RoutineItem> {
        val all = getAll()
        val today = LocalDate.now().dayOfWeek
        val isWeekend = today == DayOfWeek.SATURDAY || today == DayOfWeek.SUNDAY
        return all.filter {
            it.dayCategory == DayCategory.EVERYDAY ||
                (it.dayCategory == DayCategory.WEEKEND && isWeekend) ||
                (it.dayCategory == DayCategory.WEEKDAY && !isWeekend)
        }.sortedBy { it.time }
    }

    suspend fun saveAll(items: List<RoutineItem>) {
        context.routineDataStore.edit { prefs ->
            val array = JSONArray()
            items.forEach {
                array.put(JSONObject().apply {
                    put("time", it.time)
                    put("activity", it.activity)
                    put("category", it.dayCategory.name)
                })
            }
            prefs[key] = array.toString()
        }
    }

    private fun parse(json: String): List<RoutineItem> {
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map {
                val obj = array.getJSONObject(it)
                RoutineItem(
                    time = obj.getString("time"),
                    activity = obj.getString("activity"),
                    dayCategory = DayCategory.valueOf(obj.optString("category", "EVERYDAY"))
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun defaultRoutine(): List<RoutineItem> = emptyList() // user must configure their own; no assumptions made
}
