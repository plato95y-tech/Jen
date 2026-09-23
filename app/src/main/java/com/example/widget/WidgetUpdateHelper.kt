package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.db.AppDatabase
import com.example.data.pref.PreferencesManager
import com.example.ui.components.FormatUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object WidgetUpdateHelper {

    fun updateAllWidgets(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                updateDebtSummaryWidgets(context)
                updateQuickAddWidgets(context)
                updateDebtBalanceWidgets(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun updateDebtSummaryWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, DebtSummaryWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
        if (appWidgetIds.isEmpty()) return

        val db = AppDatabase.getInstance(context)
        val prefs = PreferencesManager(context)
        val currency = prefs.currency.value

        val summary = db.transactionDao().getOverallBalanceSummaryDirect()
        val allStores = db.storeDao().getAllStoresDirect()
        val now = System.currentTimeMillis()
        var overdueCount = 0
        for (store in allStores) {
            if (store.dueDate != null && store.dueDate < now) {
                val storeSummary = db.transactionDao().getStoreBalanceSummaryDirect(store.id)
                if (storeSummary.remainingBalance > 0) {
                    overdueCount++
                }
            }
        }

        val remaining = summary.totalDebt - summary.totalPaid

        for (widgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_debt_summary)

            // Total debt formatted
            views.setTextViewText(
                R.id.tv_total_debt,
                FormatUtils.formatCurrency(remaining, currency, false)
            )

            // Overdue indicator
            if (overdueCount > 0) {
                views.setTextViewText(R.id.tv_overdue_count, "$overdueCount عملاء بمديونيات متأخرة")
                views.setInt(R.id.tv_overdue_count, "setBackgroundResource", R.drawable.widget_chip_alert)
                views.setTextColor(R.id.tv_overdue_count, 0xFFFCA5A5.toInt())
            } else {
                views.setTextViewText(R.id.tv_overdue_count, "جميع الديون ضمن فترة السداد")
                views.setInt(R.id.tv_overdue_count, "setBackgroundResource", R.drawable.widget_chip_success)
                views.setTextColor(R.id.tv_overdue_count, 0xFFA7F3D0.toInt())
            }

            // Open app click
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                101,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_open_app, openAppPendingIntent)

            // Refresh button
            val refreshIntent = Intent(context, DebtSummaryWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(widgetId))
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                102,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_refresh, refreshPendingIntent)

            // Quick add debt button
            val addDebtIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("shortcut_action", "add_debt")
            }
            val addDebtPendingIntent = PendingIntent.getActivity(
                context,
                103,
                addDebtIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_quick_add, addDebtPendingIntent)

            // Quick add payment button
            val addPaymentIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("shortcut_action", "add_payment")
            }
            val addPaymentPendingIntent = PendingIntent.getActivity(
                context,
                104,
                addPaymentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_quick_payment, addPaymentPendingIntent)

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }

    suspend fun updateQuickAddWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, QuickAddDebtWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
        if (appWidgetIds.isEmpty()) return

        val db = AppDatabase.getInstance(context)
        val prefs = PreferencesManager(context)
        val currency = prefs.currency.value

        // CRITICAL REQUIREMENT:
        // The last customer shown in the widget is the customer for whom a transaction was added
        // (whether payment or debt). We query the most recent transaction in the database directly!
        val latestTx = db.transactionDao().getLatestTransactionDirect()
        val lastStore = if (latestTx != null && latestTx.storeId > 0L) {
            db.storeDao().getStoreByIdDirect(latestTx.storeId)
        } else {
            val lastUsedStoreId = prefs.lastUsedStoreId.value
            if (lastUsedStoreId > 0L) {
                db.storeDao().getStoreByIdDirect(lastUsedStoreId)
            } else {
                db.storeDao().getAllStoresDirect().firstOrNull()
            }
        }

        for (widgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_quick_add_debt)

            if (lastStore != null) {
                val storeSummary = db.transactionDao().getStoreBalanceSummaryDirect(lastStore.id)
                val formattedBalance = FormatUtils.formatCurrency(storeSummary.remainingBalance, currency, false)

                views.setTextViewText(R.id.tv_last_store_name, lastStore.name)
                views.setTextViewText(R.id.tv_last_store_hint, "الرصيد القائم: $formattedBalance")
                views.setTextViewText(R.id.tv_last_customer_badge, "آخر تعامل")
                views.setViewVisibility(R.id.btn_quick_pay_last_store, View.VISIBLE)

                // 1. Quick Add Debt to this specific customer
                views.setTextViewText(R.id.btn_quick_add_last_store, "دين لـ ${lastStore.name}")
                val lastStoreDebtIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("shortcut_action", "add_debt")
                    putExtra("extra_store_id", lastStore.id)
                    putExtra("extra_store_locked", true)
                }
                val lastStoreDebtPending = PendingIntent.getActivity(
                    context,
                    201,
                    lastStoreDebtIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.btn_quick_add_last_store, lastStoreDebtPending)

                // 2. Quick Add Payment for this specific customer
                views.setTextViewText(R.id.btn_quick_pay_last_store, "سداد دفعة")
                val lastStorePaymentIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("shortcut_action", "add_payment")
                    putExtra("extra_store_id", lastStore.id)
                    putExtra("extra_store_locked", true)
                }
                val lastStorePaymentPending = PendingIntent.getActivity(
                    context,
                    202,
                    lastStorePaymentIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.btn_quick_pay_last_store, lastStorePaymentPending)

                // 3. Card click opens the app
                val cardIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val cardPending = PendingIntent.getActivity(
                    context,
                    203,
                    cardIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.card_last_customer, cardPending)

            } else {
                // Empty state when no stores exist yet
                views.setTextViewText(R.id.tv_last_store_name, "لا يوجد عملاء مسجلين")
                views.setTextViewText(R.id.tv_last_store_hint, "اضغط لتسجيل أول دين وبدء الحسابات")
                views.setTextViewText(R.id.tv_last_customer_badge, "بدء سريع")
                views.setTextViewText(R.id.btn_quick_add_last_store, "تسجيل دين جديد")
                views.setViewVisibility(R.id.btn_quick_pay_last_store, View.GONE)

                val generalAddIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("shortcut_action", "add_debt")
                }
                val generalPending = PendingIntent.getActivity(
                    context,
                    204,
                    generalAddIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.btn_quick_add_last_store, generalPending)
            }

            // General add debt (for any customer)
            val generalDebtIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("shortcut_action", "add_debt")
            }
            val generalDebtPending = PendingIntent.getActivity(
                context,
                205,
                generalDebtIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_quick_add_general, generalDebtPending)

            // Open app main screen
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPending = PendingIntent.getActivity(
                context,
                206,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_quick_add_payment, openAppPending)

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }

    suspend fun updateDebtBalanceWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, DebtBalanceWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
        if (appWidgetIds.isEmpty()) return

        val db = AppDatabase.getInstance(context)
        val prefs = PreferencesManager(context)
        val currency = prefs.currency.value
        val summary = db.transactionDao().getOverallBalanceSummaryDirect()
        val remaining = summary.totalDebt - summary.totalPaid

        for (widgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_debt_balance)

            // Balance amount
            views.setTextViewText(R.id.tv_balance_amount, FormatUtils.formatCurrency(remaining, currency, false))

            // Subtitle stats
            val debtFormatted = FormatUtils.formatDecimal(summary.totalDebt)
            val paidFormatted = FormatUtils.formatDecimal(summary.totalPaid)
            views.setTextViewText(R.id.tv_balance_subtitle, "الديون: $debtFormatted | المسدد: $paidFormatted $currency")

            // Refresh button
            val refreshIntent = Intent(context, DebtBalanceWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(widgetId))
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                300,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_balance_refresh, refreshPendingIntent)

            // Open reports
            val reportIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("shortcut_action", "reports")
            }
            val reportPending = PendingIntent.getActivity(
                context,
                301,
                reportIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_open_balance_report, reportPending)
            views.setOnClickPendingIntent(R.id.btn_balance_reports, reportPending)

            // Add debt
            val addDebtIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("shortcut_action", "add_debt")
            }
            val addDebtPending = PendingIntent.getActivity(
                context,
                302,
                addDebtIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_balance_add_debt, addDebtPending)

            // Add payment
            val addPaymentIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("shortcut_action", "add_payment")
            }
            val addPaymentPending = PendingIntent.getActivity(
                context,
                303,
                addPaymentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_balance_add_payment, addPaymentPending)

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}
