package com.galaxya12.cleaner.model

import com.galaxya12.cleaner.util.ByteFormatter

enum class ScanState {
    IDLE,
    SCANNING,
    COMPLETE,
    CLEANING,
    CLEAN_COMPLETE
}

data class ScanProgress(
    val stageName: String = "",
    val itemsScanned: Int = 0,
    val bytesFound: Long = 0L,
    val isFinished: Boolean = false,
    val currentDetail: String = ""
) {
    val formattedBytesFound: String get() = ByteFormatter.format(bytesFound)
}

data class CleanableCategorySummary(
    val name: String,
    val category: CleanableCategory,
    val bytes: Long,
    val fileCount: Int,
    val items: List<CleanableItem>,
    val canDelete: Boolean = true
) {
    val formattedSize: String get() = ByteFormatter.format(bytes)
}

// Backward compatibility alias for CategorySummary if used elsewhere
typealias CategorySummary = CleanableCategorySummary

data class ScanResult(
    val cleanableBytes: Long = 0L,
    val fileCount: Int = 0,
    val scannedBytes: Long = 0L,
    val scannedFiles: Int = 0,
    val inaccessibleLocations: Int = 0,
    val requiresPermission: Boolean = false,
    val categories: List<CleanableCategorySummary> = emptyList(),
    val statusDescription: String = ""
) {
    val formattedCleanable: String get() = ByteFormatter.format(cleanableBytes)
    val formattedScanned: String get() = ByteFormatter.format(scannedBytes)

    val isPartiallyScanned: Boolean get() = inaccessibleLocations > 0
    val isNothingFound: Boolean get() = cleanableBytes == 0L && !requiresPermission
}

data class CleanResult(
    val deletedCount: Int = 0,
    val failedCount: Int = 0,
    val freedBytes: Long = 0L,
    val details: List<String> = emptyList()
) {
    val formattedFreed: String get() = ByteFormatter.format(freedBytes)
}
