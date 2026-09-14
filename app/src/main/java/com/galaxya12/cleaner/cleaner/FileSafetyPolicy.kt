package com.galaxya12.cleaner.cleaner

import java.io.File
import java.util.Locale

object FileSafetyPolicy {

    private val PROTECTED_DIRECTORY_SEGMENTS = setOf(
        "/android/data",
        "/android/obb",
        "/proc",
        "/sys",
        "/system",
        "/system_ext",
        "/vendor",
        "/product",
        "/apex",
        "/data/data",
        "/data/user",
        "/data/system",
        "/storage/emulated/0/android/data",
        "/storage/emulated/0/android/obb"
    )

    private val PROTECTED_USER_MEDIA_DIRS = setOf(
        "dcim",
        "pictures",
        "movies",
        "music",
        "documents",
        "whatsapp",
        "telegram"
    )

    private val USER_FILE_EXTENSIONS = setOf(
        "jpg", "jpeg", "png", "webp", "gif", "bmp", "heic",
        "mp4", "mkv", "avi", "mov", "3gp",
        "mp3", "m4a", "wav", "flac", "ogg", "aac",
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt",
        "zip", "rar", "7z", "tar", "gz"
    )

    val KNOWN_TEMP_EXTENSIONS = setOf(
        "tmp", "temp", "log", "cache", "bak", "dmp", "old", "chk", "crdownload", "part"
    )

    fun isProtectedDirectory(file: File): Boolean {
        val normalizedPath = file.absolutePath.lowercase(Locale.US).replace('\\', '/')
        for (protectedDir in PROTECTED_DIRECTORY_SEGMENTS) {
            if (normalizedPath == protectedDir || normalizedPath.startsWith("$protectedDir/")) {
                return true
            }
        }
        return false
    }

    fun isProtectedUserMediaDirectory(file: File): Boolean {
        val pathParts = file.absolutePath.lowercase(Locale.US).split('/', '\\')
        for (part in pathParts) {
            if (part in PROTECTED_USER_MEDIA_DIRS) {
                return true
            }
        }
        return false
    }

    fun isUserDocumentOrMediaFile(file: File): Boolean {
        val ext = file.extension.lowercase(Locale.US)
        return ext in USER_FILE_EXTENSIONS
    }

    fun isSafeJunkCandidate(file: File, appCacheRoot: String? = null): Boolean {
        if (!file.exists()) return false
        if (isProtectedDirectory(file)) return false

        val path = file.absolutePath.lowercase(Locale.US)

        // If file is inside the app's own cache, it's always safe
        if (appCacheRoot != null && path.startsWith(appCacheRoot.lowercase(Locale.US))) {
            return true
        }

        // Never classify normal user documents or media as junk
        if (isUserDocumentOrMediaFile(file)) {
            return false
        }

        // Never auto-clean anything inside protected user media directories (DCIM, Pictures, WhatsApp, etc.)
        if (isProtectedUserMediaDirectory(file)) {
            return false
        }

        // Check for recognized temporary extensions
        val ext = file.extension.lowercase(Locale.US)
        if (ext in KNOWN_TEMP_EXTENSIONS) {
            return true
        }

        // Check for thumbnail cache markers
        if (path.contains(".thumbnails") || path.contains(".thumbcache") || path.contains("thumbnail_cache")) {
            return true
        }

        // Safe empty file check: 0 bytes, not directory, not hidden config (.nomedia, .git, etc.)
        if (file.isFile && file.length() == 0L) {
            val name = file.name.lowercase(Locale.US)
            if (!name.startsWith(".") && ext in KNOWN_TEMP_EXTENSIONS) {
                return true
            }
        }

        return false
    }

    fun isApkFile(file: File): Boolean {
        if (isProtectedDirectory(file)) return false
        return file.isFile && file.extension.equals("apk", ignoreCase = true)
    }

    fun isThumbnailCache(file: File): Boolean {
        if (isProtectedDirectory(file)) return false
        val path = file.absolutePath.lowercase(Locale.US)
        return (path.contains(".thumbnails") || path.contains(".thumbcache")) && file.isFile
    }

    fun isSafeEmptyFile(file: File): Boolean {
        if (!file.exists() || !file.isFile || file.length() != 0L) return false
        if (isProtectedDirectory(file)) return false
        if (isProtectedUserMediaDirectory(file)) return false
        val name = file.name
        // Preserve critical system files like .nomedia
        if (name.startsWith(".")) return false
        return true
    }

    fun canSafelyDelete(file: File, appCacheRoot: String? = null): Boolean {
        if (!file.exists()) return false
        if (isProtectedDirectory(file)) return false
        if (file.isDirectory) return false

        val path = file.absolutePath.lowercase(Locale.US)
        if (appCacheRoot != null && path.startsWith(appCacheRoot.lowercase(Locale.US))) {
            return true
        }

        return file.canWrite()
    }
}
