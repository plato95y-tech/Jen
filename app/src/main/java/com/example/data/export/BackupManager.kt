package com.example.data.export

import android.content.Context
import android.net.Uri
import com.example.data.entity.StoreEntity
import com.example.data.entity.TransactionEntity
import com.example.data.entity.TransactionType
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ParsedBackupData(
    val schemaVersion: Int,
    val exportDate: String,
    val currency: String,
    val stores: List<StoreEntity>,
    val transactions: List<TransactionEntity>
)

data class BackupFileInfo(
    val file: File,
    val name: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val isAuto: Boolean,
    val storesCount: Int,
    val transactionsCount: Int,
    val exportDate: String,
    val currency: String
)

object BackupManager {
    private const val CURRENT_SCHEMA_VERSION = 1
    private const val MAX_AUTO_BACKUPS_RETAINED = 7

    fun getSavedBackupsDir(context: Context): File {
        return File(context.filesDir, "saved_backups").apply { mkdirs() }
    }

    fun buildBackupJson(
        stores: List<StoreEntity>,
        transactions: List<TransactionEntity>,
        currency: String
    ): JSONObject {
        val displayFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
        val now = Date()

        val rootJson = JSONObject()
        rootJson.put("schemaVersion", CURRENT_SCHEMA_VERSION)
        rootJson.put("appName", "سجل الديون")
        rootJson.put("exportTimestamp", now.time)
        rootJson.put("exportDate", displayFormat.format(now))
        rootJson.put("currency", currency)

        val storesArray = JSONArray()
        for (store in stores) {
            val sObj = JSONObject()
            sObj.put("id", store.id)
            sObj.put("name", store.name)
            sObj.put("phone", store.phone)
            sObj.put("notes", store.notes)
            sObj.put("debtLimit", store.debtLimit)
            if (store.dueDate != null) {
                sObj.put("dueDate", store.dueDate)
            } else {
                sObj.put("dueDate", JSONObject.NULL)
            }
            sObj.put("createdAt", store.createdAt)
            storesArray.put(sObj)
        }
        rootJson.put("stores", storesArray)

        val txArray = JSONArray()
        for (tx in transactions) {
            val tObj = JSONObject()
            tObj.put("id", tx.id)
            tObj.put("storeId", tx.storeId)
            tObj.put("storeName", tx.storeName)
            tObj.put("type", tx.type.name)
            tObj.put("amount", tx.amount)
            tObj.put("note", tx.note)
            if (tx.imageUri != null) {
                tObj.put("imageUri", tx.imageUri)
            } else {
                tObj.put("imageUri", JSONObject.NULL)
            }
            tObj.put("timestamp", tx.timestamp)
            tObj.put("createdAt", tx.createdAt)
            txArray.put(tObj)
        }
        rootJson.put("transactions", txArray)

        return rootJson
    }

    fun exportBackup(
        context: Context,
        stores: List<StoreEntity>,
        transactions: List<TransactionEntity>,
        currency: String
    ): File {
        val timeFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault())
        val timestampStr = timeFormat.format(Date())
        val rootJson = buildBackupJson(stores, transactions, currency)

        // Also save a copy to permanent saved_backups
        saveBackupToPermanentDir(context, rootJson, "نسخة_احتياطية_ديون_$timestampStr.json", false)

        val fileName = "نسخة_احتياطية_ديون_$timestampStr.json"
        val backupDir = File(context.cacheDir, "backups").apply { mkdirs() }
        val file = File(backupDir, fileName)
        FileOutputStream(file).use { fos ->
            fos.write(rootJson.toString(2).toByteArray(Charsets.UTF_8))
        }
        return file
    }

    fun exportBackupToUri(
        context: Context,
        uri: Uri,
        stores: List<StoreEntity>,
        transactions: List<TransactionEntity>,
        currency: String
    ): Boolean {
        val rootJson = buildBackupJson(stores, transactions, currency)
        context.contentResolver.openOutputStream(uri)?.use { os ->
            os.write(rootJson.toString(2).toByteArray(Charsets.UTF_8))
            os.flush()
        }
        val timeFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault())
        val timestampStr = timeFormat.format(Date())
        saveBackupToPermanentDir(context, rootJson, "نسخة_احتياطية_ديون_$timestampStr.json", false)
        return true
    }

    fun createAutoBackup(
        context: Context,
        stores: List<StoreEntity>,
        transactions: List<TransactionEntity>,
        currency: String
    ): File {
        val timeFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault())
        val timestampStr = timeFormat.format(Date())
        val fileName = "نسخة_تلقائية_$timestampStr.json"
        val rootJson = buildBackupJson(stores, transactions, currency)

        val file = saveBackupToPermanentDir(context, rootJson, fileName, true)
        pruneOldAutoBackups(context)
        return file
    }

    private fun saveBackupToPermanentDir(
        context: Context,
        rootJson: JSONObject,
        fileName: String,
        isAuto: Boolean
    ): File {
        val dir = getSavedBackupsDir(context)
        val file = File(dir, fileName)
        FileOutputStream(file).use { fos ->
            fos.write(rootJson.toString(2).toByteArray(Charsets.UTF_8))
        }
        return file
    }

    private fun pruneOldAutoBackups(context: Context) {
        try {
            val dir = getSavedBackupsDir(context)
            val autoFiles = dir.listFiles { f -> f.name.startsWith("نسخة_تلقائية_") && f.name.endsWith(".json") }
                ?: return
            if (autoFiles.size > MAX_AUTO_BACKUPS_RETAINED) {
                autoFiles.sortBy { it.lastModified() }
                val toDelete = autoFiles.take(autoFiles.size - MAX_AUTO_BACKUPS_RETAINED)
                for (oldFile in toDelete) {
                    oldFile.delete()
                }
            }
        } catch (_: Exception) {}
    }

    fun listSavedBackups(context: Context): List<BackupFileInfo> {
        val dir = getSavedBackupsDir(context)
        val files = dir.listFiles { f -> f.name.endsWith(".json") } ?: return emptyList()
        val result = mutableListOf<BackupFileInfo>()

        val displayFormat = SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale.getDefault())

        for (file in files) {
            try {
                val content = file.readText(Charsets.UTF_8)
                val root = JSONObject(content)
                val exportDate = root.optString("exportDate", displayFormat.format(Date(file.lastModified())))
                val currency = root.optString("currency", "ر.س")
                val storesCount = root.optJSONArray("stores")?.length() ?: 0
                val txCount = root.optJSONArray("transactions")?.length() ?: 0
                val isAuto = file.name.startsWith("نسخة_تلقائية_")

                result.add(
                    BackupFileInfo(
                        file = file,
                        name = file.name,
                        sizeBytes = file.length(),
                        lastModified = file.lastModified(),
                        isAuto = isAuto,
                        storesCount = storesCount,
                        transactionsCount = txCount,
                        exportDate = exportDate,
                        currency = currency
                    )
                )
            } catch (e: Exception) {
                // If corrupted file, still show it as raw file
                result.add(
                    BackupFileInfo(
                        file = file,
                        name = file.name,
                        sizeBytes = file.length(),
                        lastModified = file.lastModified(),
                        isAuto = file.name.startsWith("نسخة_تلقائية_"),
                        storesCount = 0,
                        transactionsCount = 0,
                        exportDate = displayFormat.format(Date(file.lastModified())),
                        currency = ""
                    )
                )
            }
        }

        return result.sortedByDescending { it.lastModified }
    }

    fun deleteBackupFile(file: File): Boolean {
        return try {
            file.delete()
        } catch (_: Exception) {
            false
        }
    }

    fun parseBackupFromFile(file: File): ParsedBackupData {
        val content = file.readText(Charsets.UTF_8)
        return parseBackupContent(content)
    }

    fun parseBackupFromUri(context: Context, uri: Uri): ParsedBackupData {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("تعذر فتح ملف النسخة الاحتياطية")
        val stringBuilder = StringBuilder()
        BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                stringBuilder.append(line).append('\n')
            }
        }
        return parseBackupContent(stringBuilder.toString())
    }

    fun parseBackupContent(jsonContent: String): ParsedBackupData {
        val root = JSONObject(jsonContent)
        val schemaVersion = root.optInt("schemaVersion", 1)
        val exportDate = root.optString("exportDate", "")
        val currency = root.optString("currency", "ر.س")

        val storesList = mutableListOf<StoreEntity>()
        val storesArray = root.optJSONArray("stores") ?: JSONArray()
        for (i in 0 until storesArray.length()) {
            val obj = storesArray.getJSONObject(i)
            val store = StoreEntity(
                id = obj.optLong("id", 0L),
                name = obj.optString("name", ""),
                phone = obj.optString("phone", ""),
                notes = obj.optString("notes", ""),
                debtLimit = obj.optDouble("debtLimit", 0.0),
                dueDate = if (obj.isNull("dueDate")) null else obj.optLong("dueDate"),
                createdAt = obj.optLong("createdAt", System.currentTimeMillis())
            )
            storesList.add(store)
        }

        val txList = mutableListOf<TransactionEntity>()
        val txArray = root.optJSONArray("transactions") ?: JSONArray()
        for (i in 0 until txArray.length()) {
            val obj = txArray.getJSONObject(i)
            val typeStr = obj.optString("type", "DEBT")
            val type = try {
                TransactionType.valueOf(typeStr)
            } catch (e: Exception) {
                TransactionType.DEBT
            }
            val tx = TransactionEntity(
                id = obj.optLong("id", 0L),
                storeId = obj.optLong("storeId", 0L),
                storeName = obj.optString("storeName", ""),
                type = type,
                amount = obj.optDouble("amount", 0.0),
                note = obj.optString("note", ""),
                imageUri = if (obj.isNull("imageUri")) null else obj.optString("imageUri").takeIf { it.isNotBlank() },
                timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                createdAt = obj.optLong("createdAt", System.currentTimeMillis())
            )
            txList.add(tx)
        }

        return ParsedBackupData(
            schemaVersion = schemaVersion,
            exportDate = exportDate,
            currency = currency,
            stores = storesList,
            transactions = txList
        )
    }
}
