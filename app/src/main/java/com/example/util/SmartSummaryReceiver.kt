package com.example.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.db.AppDatabase
import com.example.data.pref.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmartSummaryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val isWeekly = intent.getBooleanExtra("is_weekly", false)
        val prefs = PreferencesManager(context)
        if (!prefs.isSmartSummaryNotificationEnabled.value) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                val summary = db.transactionDao().getOverallBalanceSummaryDirect()
                val totalDebt = (summary.totalDebt - summary.totalPaid).coerceAtLeast(0.0)
                val allStores = db.storeDao().getAllStoresDirect()
                val now = System.currentTimeMillis()
                var overdueCount = 0
                for (store in allStores) {
                    if (store.dueDate != null && store.dueDate < now) {
                        val stSummary = db.transactionDao().getStoreBalanceSummaryDirect(store.id)
                        if (stSummary.remainingBalance > 0) {
                            overdueCount++
                        }
                    }
                }

                DebtNotificationHelper.showSmartSummaryNotification(
                    context = context,
                    overdueCount = overdueCount,
                    totalDebt = totalDebt,
                    currency = prefs.currency.value,
                    isWeekly = isWeekly
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
