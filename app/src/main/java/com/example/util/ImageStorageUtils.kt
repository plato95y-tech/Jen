package com.example.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object ImageStorageUtils {
    private const val RECEIPTS_DIR = "receipts"

    /**
     * Copies an image from a selected content Uri into the app's internal persistent directory.
     * This ensures the image stays accessible even after restarts and device reboots.
     * Returns the absolute path of the saved file, or null on error.
     */
    fun saveImageToInternalStorage(context: Context, sourceUri: Uri): String? {
        return try {
            val receiptsDir = File(context.filesDir, RECEIPTS_DIR).apply {
                if (!exists()) mkdirs()
            }
            val destinationFile = File(receiptsDir, "receipt_${System.currentTimeMillis()}.jpg")

            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                FileOutputStream(destinationFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            destinationFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Safely deletes an image file given its path.
     */
    fun deleteImage(filePath: String?) {
        if (filePath.isNullOrBlank()) return
        try {
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Returns a File instance if the given path exists.
     */
    fun getImageFile(filePath: String?): File? {
        if (filePath.isNullOrBlank()) return null
        val file = File(filePath)
        return if (file.exists()) file else null
    }
}
