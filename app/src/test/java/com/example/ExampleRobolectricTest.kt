package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.entity.StoreEntity
import com.example.data.entity.TransactionEntity
import com.example.data.entity.TransactionType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {
  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("سجل الديون", appName)
  }

  @Test
  fun `database store and transaction creation`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = AppDatabase.getInstance(context)
    val storeId = db.storeDao().insertStore(
      StoreEntity(
        name = "بقالة الأمل",
        phone = "777123456",
        notes = "حساب مستلزمات شهرية",
        debtLimit = 100000.0,
        dueDate = null
      )
    )
    assertNotNull(storeId)

    db.transactionDao().insertTransaction(
      TransactionEntity(
        storeId = storeId,
        storeName = "بقالة الأمل",
        type = TransactionType.DEBT,
        amount = 15000.0,
        note = "شراء مواد غذائية",
        timestamp = System.currentTimeMillis()
      )
    )

    val summary = db.transactionDao().getStoreBalanceSummaryDirect(storeId)
    assertEquals(15000.0, summary.totalDebt, 0.001)
    assertEquals(0.0, summary.totalPaid, 0.001)
    assertEquals(15000.0, summary.remainingBalance, 0.001)
  }

  @Test
  fun `security and privacy preferences test`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = com.example.data.pref.PreferencesManager(context)

    // Initially app lock is disabled
    prefs.setAppLock(true, "1234")
    assertEquals(true, prefs.isAppLockEnabled.value)
    assertEquals("1234", prefs.pinCode.value)

    // Verify PIN
    val wrongPin = prefs.verifyPin("9999")
    assertEquals(false, wrongPin)

    val correctPin = prefs.verifyPin("1234")
    assertEquals(true, correctPin)
    assertEquals(true, prefs.isUnlocked.value)

    // Privacy mode
    prefs.setPrivacyMode(true)
    assertEquals(true, prefs.isPrivacyMode.value)

    val maskedText = com.example.ui.components.FormatUtils.formatCurrency(5000.0, "ر.س", isPrivacyMode = true)
    assertEquals("•••••• ر.س", maskedText)

    val unmaskedText = com.example.ui.components.FormatUtils.formatCurrency(5000.0, "ر.س", isPrivacyMode = false)
    assertEquals("5,000 ر.س", unmaskedText)
  }
}
