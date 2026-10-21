package com.my.mealjournal

import android.app.Application
import com.my.mealjournal.data.MealDatabase
import com.my.mealjournal.data.MealRepository
import com.my.mealjournal.data.SettingsStore
import com.my.mealjournal.notify.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MealApp : Application() {

    val database: MealDatabase by lazy { MealDatabase.get(this) }
    val repository: MealRepository by lazy { MealRepository(database) }
    val settings: SettingsStore by lazy { SettingsStore(this) }
    val reminderScheduler: ReminderScheduler by lazy { ReminderScheduler(this) }

    override fun onCreate() {
        super.onCreate()
        // 冷启动时把闹钟按已保存的设置重新排一次，避免首次安装后提醒不生效。
        CoroutineScope(Dispatchers.IO).launch {
            reminderScheduler.apply(settings.reminderEnabled.first(), settings.reminderMinute.first())
        }
    }
}
