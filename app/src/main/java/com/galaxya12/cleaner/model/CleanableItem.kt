package com.galaxya12.cleaner.model

import android.net.Uri
import com.galaxya12.cleaner.util.ByteFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class CleanableCategory(val displayName: String) {
    APP_CACHE("App Cache"),
    TEMP_FILES("Temporary Files"),
    OLD_APKS("Old APKs"),
    EMPTY_FILES("Empty Files"),
    JUNK_FILES("Junk Files"),
    THUMBNAILS("Thumbnails"),
    LARGE_FILES("Large Files"),
    DUPLICATE_FILES("Duplicate Files")
}

data class CleanableItem(
    val id: String,
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val category: CleanableCategory,
    val canDeleteDirectly: Boolean = true,
    val isSelected: Boolean = true,
    val uri: Uri? = null,
    val isSafDocument: Boolean = false,
    val duplicateGroupId: String? = null,
    val isOriginal: Boolean = false
) {
    val formattedSize: String get() = ByteFormatter.format(sizeBytes)
    val formattedDate: String get() {
        if (lastModified <= 0L) return "Unknown date"
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return sdf.format(Date(lastModified))
    }
}

data class DuplicateGroup(
    val groupId: String,
    val sizePerFile: Long,
    val items: List<CleanableItem>
) {
    val formattedSize: String get() = ByteFormatter.format(sizePerFile)
    val totalWasteBytes: Long get() = if (items.size > 1) (items.size - 1) * sizePerFile else 0L
}
