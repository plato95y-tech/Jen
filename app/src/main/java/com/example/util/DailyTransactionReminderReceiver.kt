package com.example.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.db.AppDatabase
import com.example.data.pref.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class DailyTransactionReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = PreferencesManager(context)

        // Reschedule on reboot if enabled
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            if (prefs.isDailyReminderEnabled.value) {
                DailyReminderScheduler.schedule(context)
            }
            return
        }

        if (!prefs.isDailyReminderEnabled.value) {
            DailyReminderScheduler.cancel(context)
            return
        }

        val todayCal = Calendar.getInstance()
        val currentDayOfWeek = todayCal.get(Calendar.DAY_OF_WEEK)
        val enabledDays = prefs.dailyReminderDays.value

        // Check if today is one of the chosen days
        if (!enabledDays.contains(currentDayOfWeek)) {
            // Not scheduled for today; schedule for next enabled day
            DailyReminderScheduler.schedule(context)
            return
        }

        val skipIfRecorded = prefs.skipReminderIfRecordedToday.value

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (skipIfRecorded) {
                    val cal = Calendar.getInstance()
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    val startOfDay = cal.timeInMillis

                    cal.set(Calendar.HOUR_OF_DAY, 23)
                    cal.set(Calendar.MINUTE, 59)
                    cal.set(Calendar.SECOND, 59)
                    cal.set(Calendar.MILLISECOND, 999)
                    val endOfDay = cal.timeInMillis

                    val db = AppDatabase.getInstance(context)
                    val todayTxCount = db.transactionDao().getTransactionCountBetween(startOfDay, endOfDay)

                    if (todayTxCount > 0) {
                        // User has already recorded a transaction today, skip notification!
                        DailyReminderScheduler.schedule(context)
                        return@launch
                    }
                }

                // If not skipped or no transaction recorded today, show reminder notification
                DailyReminderScheduler.showNotificationNow(context)

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                // Schedule next occurrence
                DailyReminderScheduler.schedule(context)
            }
        }
    }
}
