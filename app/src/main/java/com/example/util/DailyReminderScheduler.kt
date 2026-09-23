package com.example.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.pref.PreferencesManager
import java.util.Calendar
import java.util.Locale

object DailyReminderScheduler {
    const val CHANNEL_ID = "daily_transaction_reminders"
    const val NOTIFICATION_ID = 9910
    const val ALARM_REQUEST_CODE = 8820

    data class DayOfWeekInfo(
        val calendarDay: Int,
        val fullName: String,
        val shortName: String
    )

    val WEEK_DAYS = listOf(
        DayOfWeekInfo(Calendar.SATURDAY, "السبت", "سبت"),
        DayOfWeekInfo(Calendar.SUNDAY, "الأحد", "أحد"),
        DayOfWeekInfo(Calendar.MONDAY, "الإثنين", "إثنين"),
        DayOfWeekInfo(Calendar.TUESDAY, "الثلاثاء", "ثلاثاء"),
        DayOfWeekInfo(Calendar.WEDNESDAY, "الأربعاء", "أربعاء"),
        DayOfWeekInfo(Calendar.THURSDAY, "الخميس", "خميس"),
        DayOfWeekInfo(Calendar.FRIDAY, "الجمعة", "جمعة")
    )

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "تذكيرات تسجيل المعاملات"
            val descriptionText = "إشعارات تذكيرية يومية لتسجيل معاملات الديون والمدفوعات"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun format12Hour(hour: Int, minute: Int): String {
        val isPm = hour >= 12
        val hour12 = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        val periodStr = if (isPm) "م" else "ص"
        return String.format(Locale.US, "%02d:%02d %s", hour12, minute, periodStr)
    }

    fun schedule(context: Context) {
        val prefs = PreferencesManager(context)
        if (!prefs.isDailyReminderEnabled.value) {
            cancel(context)
            return
        }

        val targetHour = prefs.dailyReminderHour.value
        val targetMinute = prefs.dailyReminderMinute.value
        val enabledDays = prefs.dailyReminderDays.value

        if (enabledDays.isEmpty()) {
            cancel(context)
            return
        }

        val triggerTime = computeNextTriggerTime(targetHour, targetMinute, enabledDays)
        if (triggerTime == null) {
            cancel(context)
            return
        }

        createNotificationChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(context, DailyTransactionReminderReceiver::class.java).apply {
            action = "com.example.ACTION_DAILY_TRANSACTION_REMINDER"
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        } catch (e: SecurityException) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, DailyTransactionReminderReceiver::class.java).apply {
            action = "com.example.ACTION_DAILY_TRANSACTION_REMINDER"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun computeNextTriggerTime(hour: Int, minute: Int, enabledDays: Set<Int>): Long? {
        if (enabledDays.isEmpty()) return null
        val now = Calendar.getInstance()

        // Check the next 8 days starting from today
        for (i in 0..7) {
            val candidate = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, i)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val dayOfWeek = candidate.get(Calendar.DAY_OF_WEEK)
            if (enabledDays.contains(dayOfWeek)) {
                if (candidate.timeInMillis > now.timeInMillis) {
                    return candidate.timeInMillis
                }
            }
        }
        return null
    }

    fun showNotificationNow(context: Context) {
        createNotificationChannel(context)
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Tapping the notification opens MainActivity on the home screen
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("تذكير")
            .setContentText("هل سجّلت معاملاتك اليوم؟ افتح التطبيق لتسجيل ذالك .")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("هل سجّلت معاملاتك اليوم؟ افتح التطبيق لتسجيل ذالك .")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
