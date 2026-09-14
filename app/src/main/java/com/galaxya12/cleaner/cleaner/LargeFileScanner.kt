package com.galaxya12.cleaner.cleaner

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.galaxya12.cleaner.model.CleanableCategory
import com.galaxya12.cleaner.model.CleanableItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

enum class LargeFileThreshold(val minBytes: Long, val label: String) {
    MB_100(100L * 1024L * 1024L, "100 MB+"),
    MB_500(500L * 1024L * 1024L, "500 MB+"),
    GB_1(1024L * 1024L * 1024L, "1 GB+")
}

class LargeFileScanner(private val context: Context) {

    suspend fun scanLargeFiles(threshold: LargeFileThreshold = LargeFileThreshold.MB_100): List<CleanableItem> {
        return withContext(Dispatchers.IO) {
            val items = mutableListOf<CleanableItem>()

            // 1. Query MediaStore Files where size >= threshold
            try {
                val collection: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
                } else {
                    MediaStore.Files.getContentUri("external")
                }

                val projection = arrayOf(
                    MediaStore.Files.FileColumns._ID,
                    MediaStore.Files.FileColumns.DISPLAY_NAME,
                    MediaStore.Files.FileColumns.SIZE,
                    MediaStore.Files.FileColumns.DATE_MODIFIED,
                    MediaStore.Files.FileColumns.DATA
                )

                val selection = "${MediaStore.Files.FileColumns.SIZE} >= ?"
                val selectionArgs = arrayOf(threshold.minBytes.toString())
                val sortOrder = "${MediaStore.Files.FileColumns.SIZE} DESC LIMIT 100"

                context.contentResolver.query(
                    collection,
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                    val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                    val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
                    val dataCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val name = cursor.getString(nameCol) ?: "Unnamed File"
                        val size = cursor.getLong(sizeCol)
                        val dateModified = cursor.getLong(dateCol) * 1000L
                        val path = if (dataCol != -1) cursor.getString(dataCol) ?: "" else ""
                        val contentUri = ContentUris.withAppendedId(collection, id)

                        items.add(
                            CleanableItem(
                                id = UUID.randomUUID().toString(),
                                name = name,
                                path = path,
                                sizeBytes = size,
                                lastModified = dateModified,
                                category = CleanableCategory.LARGE_FILES,
                                canDeleteDirectly = false, // Never auto-delete, requires explicit user action
                                isSelected = false,
                                uri = contentUri
                            )
                        )
                    }
                }
            } catch (_: Exception) {
                // MediaStore query fallback
            }

            // 2. Direct scan of accessible Downloads folder as fallback / supplement
            if (items.isEmpty()) {
                val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (downloads != null && downloads.exists() && downloads.canRead()) {
                    try {
                        downloads.listFiles()?.forEach { file ->
                            if (file.isFile && file.length() >= threshold.minBytes) {
                                items.add(
                                    CleanableItem(
                                        id = UUID.randomUUID().toString(),
                                        name = file.name,
                                        path = file.absolutePath,
                                        sizeBytes = file.length(),
                                        lastModified = file.lastModified(),
                                        category = CleanableCategory.LARGE_FILES,
                                        canDeleteDirectly = file.canWrite(),
                                        isSelected = false
                                    )
                                )
                            }
                        }
                    } catch (_: Exception) {
                    }
                }
            }

            items.sortedByDescending { it.sizeBytes }
        }
    }
}
