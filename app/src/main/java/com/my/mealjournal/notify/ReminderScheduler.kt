package com.my.mealjournal.notify

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** 每天固定时刻提醒"该记三餐了"，用 AlarmManager 精确到分钟。 */
class ReminderScheduler(private val context: Context) {

    private val alarmManager: AlarmManager
        get() = context.getSystemService(AlarmManager::class.java)

    fun apply(enabled: Boolean, minuteOfDay: Int) {
        if (!enabled) {
            cancel()
            return
        }
        scheduleAt(nextTrigger(minuteOfDay, LocalDateTime.now()), minuteOfDay)
    }

    fun cancel() {
        alarmManager.cancel(pendingIntent(minuteOfDay = 0))
    }

    private fun scheduleAt(trigger: Long, minuteOfDay: Int) {
        val pi = pendingIntent(minuteOfDay)
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        try {
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
        }
    }

    private fun pendingIntent(minuteOfDay: Int): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(ReminderReceiver.ACTION_REMIND)
            .putExtra(ReminderReceiver.EXTRA_MINUTE, minuteOfDay)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val REQUEST_CODE = 8801
        const val CHANNEL_ID = "daily_reminder"

        fun nextTrigger(minuteOfDay: Int, now: LocalDateTime): Long {
            var target = now.toLocalDate().atTime(LocalTime.ofSecondOfDay(minuteOfDay.toLong()))
            if (!target.isAfter(now)) target = target.plusDays(1)
            return target.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }
    }
}

/** 提醒到点：发通知并把下一次闹钟排好。 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_REMIND) return
        val minute = intent.getIntExtra(EXTRA_MINUTE, 20 * 60)
        notifyToday(context)
        ReminderScheduler(context).apply(enabled = true, minuteOfDay = minute)
    }

    private fun notifyToday(context: Context) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        ensureChannel(context)

        val open = Intent(context, Class.forName("com.my.mealjournal.MainActivity"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val content = PendingIntent.getActivity(
            context,
            0,
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("今天的三餐记了吗？")
            .setContentText("花 10 秒补一条，别让今天的打卡断掉。")
            .setAutoCancel(true)
            .setContentIntent(content)
            .build()
        try {
            manager.notify(NOTIFY_ID, notification)
        } catch (_: SecurityException) {
            // 没给通知权限就静默跳过，不崩 App。
        }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(ReminderScheduler.CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                ReminderScheduler.CHANNEL_ID,
                "每日打卡提醒",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "每天固定时间提醒记录三餐" },
        )
    }

    companion object {
        const val ACTION_REMIND = "com.my.mealjournal.action.REMIND"
        const val EXTRA_MINUTE = "minute"
        const val NOTIFY_ID = 4101
    }
}

/** 重启后闹钟会丢，这里按保存的设置重新排一次。 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        val app = context.applicationContext as com.my.mealjournal.MealApp
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val enabled = app.settings.reminderEnabled.first()
                val minute = app.settings.reminderMinute.first()
                app.reminderScheduler.apply(enabled, minute)
            } finally {
                pending.finish()
            }
        }
    }
}
