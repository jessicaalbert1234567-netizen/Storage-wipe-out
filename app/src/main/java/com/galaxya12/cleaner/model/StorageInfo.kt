package com.galaxya12.cleaner.model

import java.util.Locale

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
        fun formatBytes(bytes: Long): String {
            if (bytes <= 0L) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            var value = bytes.toDouble()
            var index = 0
            while (value >= 1024.0 && index < units.size - 1) {
                value /= 1024.0
                index++
            }
            return if (index == 0) {
                String.format(Locale.US, "%.0f %s", value, units[index])
            } else {
                String.format(Locale.US, "%.2f %s", value, units[index])
            }
        }
    }
}
