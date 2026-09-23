package com.example.data.export

import android.content.Context
import com.example.data.entity.TransactionEntity
import com.example.data.entity.TransactionType
import com.example.util.CurrencyUtils
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {
    fun exportTransactionsToCsv(
        context: Context,
        transactions: List<TransactionEntity>,
        storeName: String?,
        defaultCurrencySymbol: String,
        defaultCurrencyCode: String = "",
        useLatestRateForAll: Boolean = false,
        latestRateMap: Map<String, Double> = emptyMap()
    ): File {
        val timeFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault())
        val displayDateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        val displayTimeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val timestampStr = timeFormat.format(Date())

        val prefix = if (storeName != null) "ديون_${storeName.replace(" ", "_")}" else "تقرير_الديون"
        val fileName = "${prefix}_$timestampStr.csv"
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportDir, fileName)

        var totalDebtConverted = 0.0
        var totalPaidConverted = 0.0

        FileOutputStream(file).use { fos ->
            // Write UTF-8 Byte Order Mark (BOM) so Excel detects UTF-8 properly for Arabic
            fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
            val writer = fos.bufferedWriter(Charsets.UTF_8)

            // CSV Header with multi-currency fields
            writer.write("التاريخ,الوقت,المحل/العميل,النوع,المبلغ بالعملة الأصلية,العملة الأصلية,سعر الصرف المعتمد,المعادل بالعملة الافتراضية ($defaultCurrencySymbol),ملاحظات\n")

            for (tx in transactions) {
                val date = displayDateFormat.format(Date(tx.timestamp))
                val time = displayTimeFormat.format(Date(tx.timestamp))
                val typeStr = if (tx.type == TransactionType.DEBT) "دين" else "سداد"
                val escapedNote = "\"${tx.note.replace("\"", "\"\"")}\""
                val escapedStore = "\"${tx.storeName.replace("\"", "\"\"")}\""

                val txCurr = tx.currencyCode.ifBlank { defaultCurrencyCode }
                val effectiveRate = if (useLatestRateForAll) {
                    latestRateMap[tx.currencyCode] ?: tx.exchangeRate
                } else {
                    tx.exchangeRate
                }

                val convertedAmount = CurrencyUtils.convertToDefaultCurrency(
                    amount = tx.amount,
                    txCurrencyCode = tx.currencyCode,
                    txExchangeRate = tx.exchangeRate,
                    defaultCurrencyCode = defaultCurrencyCode,
                    useLatestRateForAll = useLatestRateForAll,
                    latestRateMap = latestRateMap
                )

                if (tx.type == TransactionType.DEBT) {
                    totalDebtConverted += convertedAmount
                } else {
                    totalPaidConverted += convertedAmount
                }

                writer.write("$date,$time,$escapedStore,$typeStr,${tx.amount},$txCurr,$effectiveRate,$convertedAmount,$escapedNote\n")
            }

            val remainingConverted = totalDebtConverted - totalPaidConverted
            val csvStatus = when {
                remainingConverted > 0 -> "متبقي عليه"
                remainingConverted < 0 -> "متبقي له"
                else -> "تم السداد بالكامل"
            }
            writer.write("\n")
            val rateModeLabel = if (useLatestRateForAll) "اعتماد آخر سعر صرف" else "أسعار الصرف التاريخية وقت كل عملية"
            writer.write("الملخص العام (محول للعملة الافتراضية: $defaultCurrencySymbol - $rateModeLabel),,,,,,,,\n")
            writer.write("إجمالي الديون (بالعملة الافتراضية),,,,$totalDebtConverted,$defaultCurrencySymbol,,,\n")
            writer.write("إجمالي المسدد (بالعملة الافتراضية),,,,$totalPaidConverted,$defaultCurrencySymbol,,,\n")
            writer.write("صافي الرصيد المتبقي (بالعملة الافتراضية),,,,$remainingConverted,$defaultCurrencySymbol,,,\n")
            writer.write("حالة الحساب,,,,$csvStatus,,,,,\n")
            writer.flush()
        }

        return file
    }
}
