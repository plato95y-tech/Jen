package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PaymentGreen
import com.example.util.ReminderMessageUtils
import com.example.util.ReminderTone

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReminderCustomizationDialog(
    storeName: String,
    phone: String,
    remainingBalance: Double,
    currency: String,
    totalDebt: Double = 0.0,
    totalPaid: Double = 0.0,
    dueDate: Long? = null,
    lastTransactionDate: Long? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTone by remember { mutableStateOf(ReminderTone.FRIENDLY) }
    var customMessage by remember {
        mutableStateOf(
            ReminderMessageUtils.generateReminderText(
                tone = ReminderTone.FRIENDLY,
                storeName = storeName,
                remainingBalance = remainingBalance,
                currency = currency,
                totalDebt = totalDebt,
                totalPaid = totalPaid,
                dueDate = dueDate,
                lastTransactionDate = lastTransactionDate
            )
        )
    }

    // Function to regenerate template
    fun updateTone(tone: ReminderTone) {
        selectedTone = tone
        customMessage = ReminderMessageUtils.generateReminderText(
            tone = tone,
            storeName = storeName,
            remainingBalance = remainingBalance,
            currency = currency,
            totalDebt = totalDebt,
            totalPaid = totalPaid,
            dueDate = dueDate,
            lastTransactionDate = lastTransactionDate
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "إرسال تذكير بالسداد / كشف حساب",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Store Info Header
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = storeName,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            if (phone.isNotBlank()) {
                                Text(
                                    text = phone,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "المبلغ المستحق",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = FormatUtils.formatCurrency(remainingBalance, currency),
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }

                // Tone selection chips
                Text(
                    text = "نمط الرسالة:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ReminderTone.values().forEach { tone ->
                        FilterChip(
                            selected = selectedTone == tone,
                            onClick = { updateTone(tone) },
                            label = { Text(tone.label, fontSize = 12.sp) }
                        )
                    }
                }

                // Editable message text
                OutlinedTextField(
                    value = customMessage,
                    onValueChange = { customMessage = it },
                    label = { Text("نص الرسالة (يمكنك تعديله قبل الإرسال)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .testTag("reminder_message_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Actions: WhatsApp, SMS, Share, Copy
                Text(
                    text = "خيارات الإرسال المباشر:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                // WhatsApp Button (Prominent)
                Button(
                    onClick = {
                        ReminderMessageUtils.openWhatsApp(context, phone, customMessage)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("send_whatsapp_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF25D366) // WhatsApp brand green
                    )
                ) {
                    Icon(
                        Icons.Default.Message,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (phone.isNotBlank()) "إرسال عبر واتساب للمستلم" else "مشاركة عبر واتساب",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // SMS Button
                    OutlinedButton(
                        onClick = {
                            ReminderMessageUtils.openSms(context, phone, customMessage)
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("send_sms_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Message, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("رسالة SMS", fontSize = 12.sp)
                    }

                    // Share button
                    OutlinedButton(
                        onClick = {
                            ReminderMessageUtils.shareTextMessage(context, customMessage, "تذكير بالسداد - $storeName")
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_reminder_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("مشاركة", fontSize = 12.sp)
                    }

                    // Copy text button
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Debt Reminder", customMessage)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "تم نسخ نص التذكير إلى الحافظة", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("copy_reminder_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "نسخ", modifier = Modifier.size(16.dp))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}
