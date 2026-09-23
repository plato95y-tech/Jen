package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.ui.components.FormatUtils
import java.net.URLEncoder

enum class ReminderTone(val label: String) {
    FRIENDLY("تذكير مهذب ولطيف"),
    STATEMENT("كشف حساب مختصر"),
    URGENT("إشعار استحقاق رسمي")
}

object ReminderMessageUtils {

    /**
     * Cleans phone number for international WhatsApp link format.
     * E.g. "0501234567" -> converts to standard or leaves international digits.
     */
    fun cleanPhoneNumber(rawPhone: String): String {
        val digits = rawPhone.filter { it.isDigit() }
        return digits
    }

    /**
     * Generates a customizable, professional Arabic reminder message for a store or customer.
     */
    fun generateReminderText(
        tone: ReminderTone,
        storeName: String,
        remainingBalance: Double,
        currency: String,
        totalDebt: Double = 0.0,
        totalPaid: Double = 0.0,
        dueDate: Long? = null,
        lastTransactionDate: Long? = null
    ): String {
        val formattedRemaining = FormatUtils.formatCurrency(remainingBalance, currency)
        val todayStr = FormatUtils.formatDate(System.currentTimeMillis())

        return when (tone) {
            ReminderTone.FRIENDLY -> {
                val dueNote = if (dueDate != null) "\nموعد الاستحقاق المتفق عليه: ${FormatUtils.formatDate(dueDate)}" else ""
                """
السلام عليكم ورحمة الله وبركاته،
الأخ/الأخت: $storeName

نود تذكيركم بلطف بأن الرصيد المتبقي المستحق على حسابكم حتى تاريخ $todayStr هو:
⭐ $formattedRemaining ⭐$dueNote

شاكرين لكم حسن تعاونكم وتواصلكم الدائم، ونسعد باستمرار التعامل معكم.
تحياتنا وتقديرنا.
                """.trimIndent()
            }
            ReminderTone.STATEMENT -> {
                val formattedDebt = FormatUtils.formatCurrency(totalDebt, currency)
                val formattedPaid = FormatUtils.formatCurrency(totalPaid, currency)
                val lastTxStr = if (lastTransactionDate != null) FormatUtils.formatDate(lastTransactionDate) else todayStr
                val dueStr = if (dueDate != null) "\n• تاريخ الاستحقاق: ${FormatUtils.formatDate(dueDate)}" else ""

                """
📊 كشف حساب مختصر
━━━━━━━━━━━━━━━
الاسم: $storeName
التاريخ: $todayStr

• إجمالي المشتريات/الدين: $formattedDebt
• إجمالي المبالغ المسددة: $formattedPaid
• الرصيد المتبقي المستحق: $formattedRemaining
• تاريخ آخر حركة: $lastTxStr$dueStr
━━━━━━━━━━━━━━━
نرجو التكرم بالاطلاع والمطابقة.
دمتم بألف خير.
                """.trimIndent()
            }
            ReminderTone.URGENT -> {
                val dueNote = if (dueDate != null) "المستحق بتاريخ ${FormatUtils.formatDate(dueDate)}" else "المستحق حالياً"
                """
إشعار مطالبة مالية
━━━━━━━━━━━━━━━
إلى: $storeName
التاريخ: $todayStr

نحيطكم علماً بضرورة المبادرة بسداد الرصيد المالي ($dueNote) والبالغ:
⚠️ $formattedRemaining ⚠️

يرجى التكرم بتسوية المبلغ في أقرب وقت ممكن شاكرين اهتمامكم وتعاونكم.
                """.trimIndent()
            }
        }
    }

    /**
     * Opens WhatsApp chat directly with a pre-filled reminder message.
     */
    fun openWhatsApp(context: Context, phone: String, message: String) {
        val cleaned = cleanPhoneNumber(phone)
        try {
            val encodedMessage = URLEncoder.encode(message, "UTF-8")
            val uriString = if (cleaned.isNotBlank()) {
                "https://api.whatsapp.com/send?phone=$cleaned&text=$encodedMessage"
            } else {
                "https://api.whatsapp.com/send?text=$encodedMessage"
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to generic share if WhatsApp is not installed
            Toast.makeText(context, "تعذر فتح واتساب، جاري فتح قائمة المشاركة", Toast.LENGTH_SHORT).show()
            shareTextMessage(context, message, "تذكير بالسداد - $phone")
        }
    }

    /**
     * Opens SMS app directly with the phone and message populated.
     */
    fun openSms(context: Context, phone: String, message: String) {
        try {
            val uri = if (phone.isNotBlank()) Uri.parse("smsto:$phone") else Uri.parse("smsto:")
            val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
                putExtra("sms_body", message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر فتح تطبيق الرسائل النصية", Toast.LENGTH_SHORT).show()
            shareTextMessage(context, message, "تذكير بالسداد")
        }
    }

    /**
     * Dials phone number directly in phone dialer.
     */
    fun dialPhone(context: Context, phone: String) {
        if (phone.isBlank()) {
            Toast.makeText(context, "لا يوجد رقم هاتف مسجل لهذا المحل", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر فتح لوحة الاتصال", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Generic share text action to any app or clipboard.
     */
    fun shareTextMessage(context: Context, message: String, title: String = "مشاركة تذكير بالسداد") {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
                putExtra(Intent.EXTRA_SUBJECT, title)
            }
            context.startActivity(Intent.createChooser(intent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            Toast.makeText(context, "فشل في مشاركة النص", Toast.LENGTH_SHORT).show()
        }
    }
}
