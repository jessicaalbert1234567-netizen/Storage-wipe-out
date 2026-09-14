package com.galaxya12.cleaner.cleaner

import android.content.Context
import com.galaxya12.cleaner.model.CleanResult
import com.galaxya12.cleaner.model.CleanableItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

class CleanerEngine(private val context: Context) {

    data class CleaningProgress(
        val currentItemName: String,
        val processedCount: Int,
        val totalCount: Int,
        val deletedCount: Int,
        val failedCount: Int,
        val freedBytes: Long,
        val isDone: Boolean = false
    )

    fun cleanItems(items: List<CleanableItem>): Flow<CleaningProgress> = flow {
        val selectedItems = items.filter { it.isSelected }
        val total = selectedItems.size
        var deletedCount = 0
        var failedCount = 0
        var freedBytes = 0L
        val details = mutableListOf<String>()

        emit(
            CleaningProgress(
                currentItemName = "Starting clean engine...",
                processedCount = 0,
                totalCount = total,
                deletedCount = 0,
                failedCount = 0,
                freedBytes = 0L,
                isDone = false
            )
        )

        for ((index, item) in selectedItems.withIndex()) {
            emit(
                CleaningProgress(
                    currentItemName = item.name,
                    processedCount = index + 1,
                    totalCount = total,
                    deletedCount = deletedCount,
                    failedCount = failedCount,
                    freedBytes = freedBytes,
                    isDone = false
                )
            )

            val success = deleteItemSafely(item)
            if (success) {
                deletedCount++
                freedBytes += item.sizeBytes
                details.add("Deleted: ${item.name} (${item.formattedSize})")
            } else {
                failedCount++
                details.add("Failed: ${item.name}")
            }
        }

        emit(
            CleaningProgress(
                currentItemName = "Cleaning complete",
                processedCount = total,
                totalCount = total,
                deletedCount = deletedCount,
                failedCount = failedCount,
                freedBytes = freedBytes,
                isDone = true
            )
        )
    }.flowOn(Dispatchers.IO)

    suspend fun executeClean(items: List<CleanableItem>): CleanResult {
        val selectedItems = items.filter { it.isSelected }
        var deleted = 0
        var failed = 0
        var freed = 0L
        val details = mutableListOf<String>()

        for (item in selectedItems) {
            val success = deleteItemSafely(item)
            if (success) {
                deleted++
                freed += item.sizeBytes
                details.add("Deleted: ${item.name}")
            } else {
                failed++
                details.add("Failed: ${item.name}")
            }
        }

        return CleanResult(
            deletedCount = deleted,
            failedCount = failed,
            freedBytes = freed,
            details = details
        )
    }

    private fun deleteItemSafely(item: CleanableItem): Boolean {
        // 1. If item has a content URI (e.g. MediaStore)
        if (item.uri != null) {
            return try {
                val rows = context.contentResolver.delete(item.uri, null, null)
                rows > 0
            } catch (_: SecurityException) {
                false
            } catch (_: IllegalArgumentException) {
                false
            } catch (_: Exception) {
                false
            }
        }

        // 2. Regular filesystem delete
        val file = File(item.path)
        if (!file.exists()) {
            // Already deleted or moved
            return false
        }

        // Safety verification: Never delete protected files
        if (FileSafetyPolicy.isProtectedDirectory(file)) {
            return false
        }

        return try {
            val deleted = file.delete()
            // Never falsely report deletion: verify file no longer exists!
            deleted && !file.exists()
        } catch (_: SecurityException) {
            false
        } catch (_: IOException) {
            false
        } catch (_: FileNotFoundException) {
            false
        } catch (_: Exception) {
            false
        }
    }
}
