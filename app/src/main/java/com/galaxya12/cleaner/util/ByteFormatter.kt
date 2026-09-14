package com.galaxya12.cleaner.util

import java.util.Locale

object ByteFormatter {

    private val UNITS = arrayOf("B", "KB", "MB", "GB", "TB")

    /**
     * Formats bytes into a human-readable string: B, KB, MB, GB, TB.
     * Prevents integer overflow by working purely on Long and Double values.
     * Examples: 0 B, 512 B, 1.2 KB, 25.4 MB, 1.24 GB.
     */
    fun format(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        if (bytes < 1024L) return "$bytes B"

        var value = bytes.toDouble()
        var unitIndex = 0

        while (value >= 1024.0 && unitIndex < UNITS.size - 1) {
            value /= 1024.0
            unitIndex++
        }

        // For KB / MB / GB, show 1 or 2 decimals depending on magnitude
        return if (value >= 100.0) {
            String.format(Locale.US, "%.1f %s", value, UNITS[unitIndex])
        } else {
            val formatted = String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
            "$formatted ${UNITS[unitIndex]}"
        }
    }
}
