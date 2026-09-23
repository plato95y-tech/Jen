package com.example.data.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.entity.TransactionEntity
import com.example.data.entity.TransactionType
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExporter {
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 36f

    fun exportTransactionsToPdf(
        context: Context,
        transactions: List<TransactionEntity>,
        storeName: String?,
        periodLabel: String,
        defaultCurrencySymbol: String,
        defaultCurrencyCode: String = "",
        useLatestRateForAll: Boolean = false,
        latestRateMap: Map<String, Double> = emptyMap()
    ): File {
        val timeFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault())
        val displayDateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        val displayTimeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val timestampStr = timeFormat.format(Date())

        val prefix = if (storeName != null) "تقرير_${storeName.replace(" ", "_")}" else "تقرير_الديون_الشامل"
        val fileName = "${prefix}_$timestampStr.pdf"
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportDir, fileName)

        // Calculate totals converted to Default Currency
        var totalDebtConverted = 0.0
        var totalPaidConverted = 0.0
        for (tx in transactions) {
            val converted = com.example.util.CurrencyUtils.convertToDefaultCurrency(
                amount = tx.amount,
                txCurrencyCode = tx.currencyCode,
                txExchangeRate = tx.exchangeRate,
                defaultCurrencyCode = defaultCurrencyCode,
                useLatestRateForAll = useLatestRateForAll,
                latestRateMap = latestRateMap
            )
            if (tx.type == TransactionType.DEBT) {
                totalDebtConverted += converted
            } else {
                totalPaidConverted += converted
            }
        }
        val remainingConverted = totalDebtConverted - totalPaidConverted

        val document = PdfDocument()

        val paintTitle = Paint().apply {
            color = Color.parseColor("#004D40")
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val paintSub = Paint().apply {
            color = Color.parseColor("#455A64")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val paintTextCenter = Paint().apply {
            color = Color.parseColor("#37474F")
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val paintHeaderTh = Paint().apply {
            color = Color.WHITE
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val paintBg = Paint().apply { isAntiAlias = true }

        val rowsPerPage = 20
        val totalPages = if (transactions.isEmpty()) 1 else ((transactions.size - 1) / rowsPerPage) + 1

        for (pageIndex in 0 until totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex + 1).create()
            val page = document.startPage(pageInfo)
            val canvas: Canvas = page.canvas
            var currentY = MARGIN

            // Top decorative bar
            paintBg.color = Color.parseColor("#00695C")
            canvas.drawRect(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + 6f, paintBg)
            currentY += 24f

            // Title & Subtitle
            val headerTitle = if (storeName != null) "كشف حساب ديون - $storeName" else "تقرير الديون والحسابات الشامل"
            canvas.drawText(headerTitle, PAGE_WIDTH / 2f, currentY, paintTitle)
            currentY += 16f

            val rateModeLabel = if (useLatestRateForAll) "اعتماد آخر سعر صرف" else "أسعار الصرف التاريخية وقت كل عملية"
            val reportDateStr = "الفترة: $periodLabel | العملة الافتراضية: $defaultCurrencySymbol ($rateModeLabel)"
            canvas.drawText(reportDateStr, PAGE_WIDTH / 2f, currentY, paintSub)
            currentY += 20f

            // If first page, draw Summary Card (Totals converted to Default Currency)
            if (pageIndex == 0) {
                val cardRect = RectF(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + 54f)
                paintBg.color = Color.parseColor("#E0F2F1")
                canvas.drawRoundRect(cardRect, 8f, 8f, paintBg)

                val colWidth = (PAGE_WIDTH - 2 * MARGIN) / 3f

                // Card 1: Total Debt in Default Currency (on right for RTL)
                paintTextCenter.color = Color.parseColor("#C62828")
                paintTextCenter.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paintTextCenter.textSize = 11f
                canvas.drawText("الديون: ${formatAmount(totalDebtConverted)} $defaultCurrencySymbol", MARGIN + colWidth * 2.5f, currentY + 24f, paintTextCenter)
                paintTextCenter.textSize = 8.5f
                paintTextCenter.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText("إجمالي الديون (بالعملة الافتراضية)", MARGIN + colWidth * 2.5f, currentY + 40f, paintTextCenter)

                // Card 2: Total Paid in Default Currency (middle)
                paintTextCenter.color = Color.parseColor("#2E7D32")
                paintTextCenter.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paintTextCenter.textSize = 11f
                canvas.drawText("المسدد: ${formatAmount(totalPaidConverted)} $defaultCurrencySymbol", MARGIN + colWidth * 1.5f, currentY + 24f, paintTextCenter)
                paintTextCenter.textSize = 8.5f
                paintTextCenter.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText("إجمالي السداد (بالعملة الافتراضية)", MARGIN + colWidth * 1.5f, currentY + 40f, paintTextCenter)

                // Card 3: Remaining Balance in Default Currency (left)
                val isCreditPdf = remainingConverted < 0
                val hasDebtPdf = remainingConverted > 0
                val statusLabelPdf = when {
                    hasDebtPdf -> "متبقي عليه"
                    isCreditPdf -> "متبقي له"
                    else -> "تم السداد بالكامل"
                }
                val statusColorPdf = when {
                    hasDebtPdf -> Color.parseColor("#C62828")
                    isCreditPdf -> Color.parseColor("#0284C7")
                    else -> Color.parseColor("#2E7D32")
                }
                paintTextCenter.color = statusColorPdf
                paintTextCenter.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paintTextCenter.textSize = 11f
                canvas.drawText("$statusLabelPdf: ${formatAmount(remainingConverted)} $defaultCurrencySymbol", MARGIN + colWidth * 0.5f, currentY + 24f, paintTextCenter)
                paintTextCenter.textSize = 8.5f
                paintTextCenter.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paintTextCenter.color = Color.parseColor("#37474F")
                canvas.drawText("الحالة: $statusLabelPdf (بالعملة الافتراضية)", MARGIN + colWidth * 0.5f, currentY + 40f, paintTextCenter)

                currentY += 66f
            }

            // Table Columns (RTL from right to left)
            val tableRight = PAGE_WIDTH - MARGIN
            val tableLeft = MARGIN
            val rowHeight = 22f

            // Table Header Bar
            paintBg.color = Color.parseColor("#00796B")
            canvas.drawRoundRect(RectF(tableLeft, currentY, tableRight, currentY + rowHeight), 4f, 4f, paintBg)

            val yTh = currentY + 15f
            canvas.drawText("التاريخ", tableRight - 45f, yTh, paintHeaderTh)
            canvas.drawText("المحل / العميل", tableRight - 130f, yTh, paintHeaderTh)
            canvas.drawText("النوع", tableRight - 195f, yTh, paintHeaderTh)
            canvas.drawText("المبلغ بالعملة الأصلية", tableRight - 265f, yTh, paintHeaderTh)
            canvas.drawText("سعر الصرف", tableRight - 335f, yTh, paintHeaderTh)
            canvas.drawText("البيان والملاحظات", tableLeft + 80f, yTh, paintHeaderTh)

            currentY += rowHeight + 3f

            val startIndex = pageIndex * rowsPerPage
            val endIndex = minOf(startIndex + rowsPerPage, transactions.size)

            if (transactions.isEmpty()) {
                paintTextCenter.color = Color.parseColor("#757575")
                paintTextCenter.textSize = 10f
                canvas.drawText("لا توجد معاملات مسجلة في هذه الفترة", PAGE_WIDTH / 2f, currentY + 30f, paintTextCenter)
            } else {
                for (i in startIndex until endIndex) {
                    val tx = transactions[i]
                    val isEven = (i % 2 == 0)
                    paintBg.color = if (isEven) Color.parseColor("#F5F7F8") else Color.WHITE
                    canvas.drawRect(tableLeft, currentY, tableRight, currentY + rowHeight, paintBg)

                    val yTd = currentY + 14.5f
                    val dateText = "${displayDateFormat.format(Date(tx.timestamp))} ${displayTimeFormat.format(Date(tx.timestamp))}"
                    paintTextCenter.textSize = 7.5f
                    paintTextCenter.color = Color.parseColor("#424242")
                    canvas.drawText(dateText, tableRight - 45f, yTd, paintTextCenter)

                    val storeDisplay = truncate(tx.storeName, 12)
                    paintTextCenter.textSize = 8.5f
                    paintTextCenter.color = Color.parseColor("#212121")
                    canvas.drawText(storeDisplay, tableRight - 130f, yTd, paintTextCenter)

                    val isDebt = tx.type == TransactionType.DEBT
                    paintTextCenter.textSize = 8.5f
                    paintTextCenter.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    paintTextCenter.color = if (isDebt) Color.parseColor("#C62828") else Color.parseColor("#2E7D32")
                    canvas.drawText(if (isDebt) "دين" else "سداد", tableRight - 195f, yTd, paintTextCenter)

                    // Original amount with its currency
                    val txCurr = tx.currencyCode.ifBlank { defaultCurrencyCode }
                    paintTextCenter.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    paintTextCenter.color = Color.parseColor("#212121")
                    canvas.drawText("${formatAmount(tx.amount)} $txCurr", tableRight - 265f, yTd, paintTextCenter)

                    // Exchange Rate
                    val effectiveRate = if (useLatestRateForAll) {
                        latestRateMap[tx.currencyCode] ?: tx.exchangeRate
                    } else {
                        tx.exchangeRate
                    }
                    paintTextCenter.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    paintTextCenter.textSize = 8f
                    paintTextCenter.color = Color.parseColor("#546E7A")
                    canvas.drawText(if (txCurr.equals(defaultCurrencyCode, ignoreCase = true)) "1.0" else formatAmount(effectiveRate), tableRight - 335f, yTd, paintTextCenter)

                    val noteDisplay = truncate(tx.note.ifBlank { "-" }, 18)
                    paintTextCenter.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    paintTextCenter.color = Color.parseColor("#616161")
                    canvas.drawText(noteDisplay, tableLeft + 80f, yTd, paintTextCenter)

                    // Subtle bottom border
                    paintBg.color = Color.parseColor("#ECEFF1")
                    canvas.drawLine(tableLeft, currentY + rowHeight, tableRight, currentY + rowHeight, paintBg)

                    currentY += rowHeight
                }
            }

            // Footer
            val footerY = PAGE_HEIGHT - MARGIN + 10f
            paintBg.color = Color.parseColor("#CFD8DC")
            canvas.drawLine(MARGIN, footerY - 14f, PAGE_WIDTH - MARGIN, footerY - 14f, paintBg)

            paintSub.textSize = 8f
            canvas.drawText("تطبيق سجل الديون - حفظ محلي 100%", tableRight - 80f, footerY, paintSub)
            canvas.drawText("صفحة ${pageIndex + 1} من $totalPages", MARGIN + 40f, footerY, paintSub)

            document.finishPage(page)
        }

        FileOutputStream(file).use { fos ->
            document.writeTo(fos)
        }
        document.close()
        return file
    }

    private fun formatAmount(value: Double): String {
        return if (value % 1.0 == 0.0) {
            String.format(Locale.US, "%,.0f", value)
        } else {
            String.format(Locale.US, "%,.2f", value)
        }
    }

    private fun truncate(str: String, maxLength: Int): String {
        return if (str.length > maxLength) str.substring(0, maxLength - 1) + "…" else str
    }
}
