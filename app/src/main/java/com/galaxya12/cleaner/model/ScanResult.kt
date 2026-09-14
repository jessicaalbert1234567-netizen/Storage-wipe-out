package com.galaxya12.cleaner.model

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
    val isFinished: Boolean = false
) {
    val formattedBytesFound: String get() = StorageInfo.formatBytes(bytesFound)
}

data class CleanResult(
    val deletedCount: Int = 0,
    val failedCount: Int = 0,
    val freedBytes: Long = 0L,
    val details: List<String> = emptyList()
) {
    val formattedFreed: String get() = StorageInfo.formatBytes(freedBytes)
}

data class CategorySummary(
    val category: CleanableCategory,
    val count: Int,
    val totalBytes: Long,
    val items: List<CleanableItem>
) {
    val formattedSize: String get() = StorageInfo.formatBytes(totalBytes)
}
