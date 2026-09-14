package com.galaxya12.cleaner.model

import com.galaxya12.cleaner.util.ByteFormatter

data class StorageInfo(
    val totalBytes: Long = 0L,
    val usedBytes: Long = 0L,
    val freeBytes: Long = 0L,
    val usedPercentage: Int = 0
) {
    val formattedTotal: String get() = formatBytes(totalBytes)
    val formattedUsed: String get() = formatBytes(usedBytes)
    val formattedFree: String get() = formatBytes(freeBytes)

    companion object {
        fun formatBytes(bytes: Long): String = ByteFormatter.format(bytes)
    }
}
