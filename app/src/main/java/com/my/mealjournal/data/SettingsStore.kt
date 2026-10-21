package com.my.mealjournal.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<androidx.datastore.preferences.core.Preferences> by
    preferencesDataStore(name = "settings")

/** 用户设置：目标热量、提醒开关与时间。 */
class SettingsStore(private val context: Context) {

    private object Keys {
        val GOAL_KCAL = intPreferencesKey("goal_kcal")
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
    }

    val goalKcal: Flow<Int> = context.dataStore.data.map { it[Keys.GOAL_KCAL] ?: DEFAULT_GOAL }
    val reminderEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.REMINDER_ENABLED] ?: true }
    val reminderMinute: Flow<Int> = context.dataStore.data.map { it[Keys.REMINDER_MINUTE] ?: DEFAULT_REMINDER_MINUTE }

    suspend fun setGoalKcal(value: Int) = context.dataStore.edit { it[Keys.GOAL_KCAL] = value }

    suspend fun setReminderEnabled(value: Boolean) =
        context.dataStore.edit { it[Keys.REMINDER_ENABLED] = value }

    suspend fun setReminderMinute(value: Int) = context.dataStore.edit { it[Keys.REMINDER_MINUTE] = value }

    companion object {
        const val DEFAULT_GOAL = 1800
        const val DEFAULT_REMINDER_MINUTE = 20 * 60
        val GOAL_PRESETS = listOf(1200, 1500, 1800, 2200)
    }
}
