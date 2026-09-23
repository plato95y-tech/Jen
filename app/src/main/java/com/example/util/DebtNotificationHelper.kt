package com.example.util

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
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.ui.components.FormatUtils

class DebtReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val storeId = intent.getLongExtra(DebtNotificationHelper.EXTRA_STORE_ID, 0L)
        val storeName = intent.getStringExtra(DebtNotificationHelper.EXTRA_STORE_NAME) ?: "محل/عميل"
        val balance = intent.getDoubleExtra(DebtNotificationHelper.EXTRA_BALANCE, 0.0)
        val currency = intent.getStringExtra(DebtNotificationHelper.EXTRA_CURRENCY) ?: "ر.س"

        DebtNotificationHelper.showNotification(
            context = context,
            notificationId = storeId.toInt().coerceAtLeast(1001),
            title = "⏰ موعد استحقاق سداد: $storeName",
            message = "يحل اليوم موعد سداد الحساب المستحق بمبلغ ${FormatUtils.formatCurrency(balance, currency)}"
        )
    }
}

object DebtNotificationHelper {
    const val CHANNEL_ID = "debt_due_reminders"
    const val EXTRA_STORE_ID = "extra_store_id"
    const val EXTRA_STORE_NAME = "extra_store_name"
    const val EXTRA_BALANCE = "extra_balance"
    const val EXTRA_CURRENCY = "extra_currency"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "تنبيهات مواعيد السداد"
            val descriptionText = "إشعارات تذكيرية بمواعيد استحقاق سداد الديون والمستحقات"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun scheduleDueReminder(
        context: Context,
        storeId: Long,
        storeName: String,
        balance: Double,
        currency: String,
        triggerTimeMillis: Long
    ) {
        createNotificationChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(context, DebtReminderReceiver::class.java).apply {
            putExtra(EXTRA_STORE_ID, storeId)
            putExtra(EXTRA_STORE_NAME, storeName)
            putExtra(EXTRA_BALANCE, balance)
            putExtra(EXTRA_CURRENCY, currency)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            storeId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // If trigger time is in the past, schedule for 1 minute in future or skip
        val actualTrigger = if (triggerTimeMillis <= System.currentTimeMillis()) {
            System.currentTimeMillis() + 60_000
        } else {
            triggerTimeMillis
        }

        try {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                actualTrigger,
                pendingIntent
            )
        } catch (e: SecurityException) {
            // Inexact fallback
            alarmManager.set(AlarmManager.RTC_WAKEUP, actualTrigger, pendingIntent)
        }
    }

    fun cancelReminder(context: Context, storeId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, DebtReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            storeId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun showNotification(
        context: Context,
        notificationId: Int,
        title: String,
        message: String
    ) {
        createNotificationChannel(context)
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    fun showBackupSuccessNotification(
        context: Context,
        fileName: String,
        storesCount: Int,
        txCount: Int
    ) {
        showNotification(
            context = context,
            notificationId = 9901,
            title = "💾 تم إنشاء نسخة احتياطية تلقائية",
            message = "تم حفظ نسخة احتياطية بنجاح تتضمن $storesCount محل/عميل و $txCount معاملة مالية."
        )
    }

    fun showBackupReminderNotification(
        context: Context,
        daysSince: Long
    ) {
        showNotification(
            context = context,
            notificationId = 9902,
            title = "⚠️ تذكير بالنسخ الاحتياطي",
            message = "لقد مر $daysSince يوماً منذ آخر نسخة احتياطية. احرص على تصدير نسخة لحماية حساباتك."
        )
    }

    fun showDebtLimitExceededNotification(
        context: Context,
        storeName: String,
        currentBalance: Double,
        debtLimit: Double,
        currency: String
    ) {
        val diff = currentBalance - debtLimit
        showNotification(
            context = context,
            notificationId = (20000 + (storeName.hashCode() % 5000)).coerceAtLeast(20001),
            title = "⚠️ تجاوز حد الدين: $storeName",
            message = "تجاوز رصيد الحساب سقف الدين المحدد (${FormatUtils.formatCurrency(debtLimit, currency)}) ليصل إلى ${FormatUtils.formatCurrency(currentBalance, currency)}، بزيادة ${FormatUtils.formatCurrency(diff, currency)}."
        )
    }

    fun showSmartSummaryNotification(
        context: Context,
        overdueCount: Int,
        totalDebt: Double,
        currency: String,
        isWeekly: Boolean = false
    ) {
        val periodText = if (isWeekly) "الأسبوعي" else "اليومي"
        val title = "📊 تقرير الديون $periodText"
        val message = if (overdueCount > 0) {
            "لديك $overdueCount عملاء بمديونيات متأخرة بإجمالي ${FormatUtils.formatCurrency(totalDebt, currency)}. افتح التطبيق للمتابعة وإرسال التذكيرات."
        } else {
            "إجمالي الديون المستحقة حالياً هو ${FormatUtils.formatCurrency(totalDebt, currency)}. لا توجد مديونيات متأخرة مسجلة اليوم."
        }

        showNotification(
            context = context,
            notificationId = 9903,
            title = title,
            message = message
        )
    }
}
