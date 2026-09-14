package com.galaxya12.cleaner.cleaner

import android.content.Context
import android.os.Environment
import com.galaxya12.cleaner.model.CleanableCategory
import com.galaxya12.cleaner.model.CleanableItem
import com.galaxya12.cleaner.model.ScanProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import java.io.File
import java.util.UUID
import kotlin.coroutines.coroutineContext

class JunkScanner(private val context: Context) {

    data class ScanStep(
        val stage: String,
        val foundItems: List<CleanableItem>,
        val progress: ScanProgress
    )

    fun scanJunk(): Flow<ScanStep> = flow {
        val appCacheRoot = context.cacheDir.absolutePath
        val allFoundItems = mutableListOf<CleanableItem>()
        var totalBytes = 0L

        // Stage 1: Application's own cache & temp files
        emit(
            ScanStep(
                stage = "Application cache & temporary files",
                foundItems = emptyList(),
                progress = ScanProgress("Application Cache", 0, 0L, false)
            )
        )

        val appCacheItems = scanDirectoryForJunk(context.cacheDir, CleanableCategory.JUNK_FILES, appCacheRoot)
        val codeCacheItems = context.codeCacheDir?.let {
            scanDirectoryForJunk(it, CleanableCategory.TEMP_FILES, appCacheRoot)
        } ?: emptyList()

        val externalCacheDirs = context.externalCacheDirs.filterNotNull()
        val extCacheItems = mutableListOf<CleanableItem>()
        for (dir in externalCacheDirs) {
            extCacheItems.addAll(scanDirectoryForJunk(dir, CleanableCategory.JUNK_FILES, appCacheRoot))
        }

        val stage1Items = appCacheItems + codeCacheItems + extCacheItems
        allFoundItems.addAll(stage1Items)
        totalBytes += stage1Items.sumOf { it.sizeBytes }

        emit(
            ScanStep(
                stage = "Public temporary files",
                foundItems = allFoundItems.toList(),
                progress = ScanProgress("Temporary Files", allFoundItems.size, totalBytes, false)
            )
        )

        // Stage 2: Accessible Public Directories for Safe Junk
        val publicDirsToInspect = listOfNotNull(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            context.getExternalFilesDir(null)
        )

        for (dir in publicDirsToInspect) {
            if (!coroutineContext.isActive) break
            if (dir.exists() && dir.canRead() && !FileSafetyPolicy.isProtectedDirectory(dir)) {
                val scanned = scanAccessibleSafeJunk(dir, appCacheRoot)
                allFoundItems.addAll(scanned)
                totalBytes += scanned.sumOf { it.sizeBytes }
                emit(
                    ScanStep(
                        stage = "Checking ${dir.name}...",
                        foundItems = allFoundItems.toList(),
                        progress = ScanProgress("Scanning ${dir.name}", allFoundItems.size, totalBytes, false)
                    )
                )
            }
        }

        // Stage 3: Old APK files in accessible locations
        emit(
            ScanStep(
                stage = "Old APK files",
                foundItems = allFoundItems.toList(),
                progress = ScanProgress("Old APKs", allFoundItems.size, totalBytes, false)
            )
        )

        val apkItems = scanForApks(publicDirsToInspect)
        allFoundItems.addAll(apkItems)
        totalBytes += apkItems.sumOf { it.sizeBytes }

        // Stage 4: Thumbnails
        emit(
            ScanStep(
                stage = "Thumbnail caches",
                foundItems = allFoundItems.toList(),
                progress = ScanProgress("Thumbnails", allFoundItems.size, totalBytes, false)
            )
        )

        val thumbItems = scanForThumbnails(publicDirsToInspect)
        allFoundItems.addAll(thumbItems)
        totalBytes += thumbItems.sumOf { it.sizeBytes }

        // Stage 5: Empty files
        emit(
            ScanStep(
                stage = "Empty files",
                foundItems = allFoundItems.toList(),
                progress = ScanProgress("Empty Files", allFoundItems.size, totalBytes, false)
            )
        )

        val emptyItems = scanForEmptyFiles(publicDirsToInspect)
        allFoundItems.addAll(emptyItems)
        totalBytes += emptyItems.sumOf { it.sizeBytes }

        // Final emission
        emit(
            ScanStep(
                stage = "Scan Complete",
                foundItems = allFoundItems.toList(),
                progress = ScanProgress("Complete", allFoundItems.size, totalBytes, true)
            )
        )
    }.flowOn(Dispatchers.IO)

    private fun scanDirectoryForJunk(
        dir: File,
        category: CleanableCategory,
        appCacheRoot: String
    ): List<CleanableItem> {
        val result = mutableListOf<CleanableItem>()
        if (!dir.exists() || !dir.canRead()) return result

        try {
            val files = dir.listFiles() ?: return result
            for (file in files) {
                if (file.isDirectory) {
                    result.addAll(scanDirectoryForJunk(file, category, appCacheRoot))
                } else if (file.isFile) {
                    result.add(
                        CleanableItem(
                            id = UUID.randomUUID().toString(),
                            name = file.name,
                            path = file.absolutePath,
                            sizeBytes = file.length(),
                            lastModified = file.lastModified(),
                            category = category,
                            canDeleteDirectly = true,
                            isSelected = true
                        )
                    )
                }
            }
        } catch (_: SecurityException) {
        } catch (_: Exception) {
        }
        return result
    }

    private fun scanAccessibleSafeJunk(dir: File, appCacheRoot: String): List<CleanableItem> {
        val result = mutableListOf<CleanableItem>()
        if (!dir.exists() || !dir.canRead()) return result

        try {
            val files = dir.listFiles() ?: return result
            for (file in files) {
                if (FileSafetyPolicy.isProtectedDirectory(file)) continue
                if (file.isDirectory) {
                    // Do not deep-recurse more than 2 levels into arbitrary folders for Galaxy A12 performance
                    continue
                }
                if (file.isFile && FileSafetyPolicy.isSafeJunkCandidate(file, appCacheRoot)) {
                    val category = if (file.extension.equals("tmp", ignoreCase = true) ||
                        file.extension.equals("temp", ignoreCase = true)
                    ) {
                        CleanableCategory.TEMP_FILES
                    } else {
                        CleanableCategory.JUNK_FILES
                    }
                    result.add(
                        CleanableItem(
                            id = UUID.randomUUID().toString(),
                            name = file.name,
                            path = file.absolutePath,
                            sizeBytes = file.length(),
                            lastModified = file.lastModified(),
                            category = category,
                            canDeleteDirectly = file.canWrite(),
                            isSelected = true
                        )
                    )
                }
            }
        } catch (_: Exception) {
        }
        return result
    }

    private fun scanForApks(dirs: List<File>): List<CleanableItem> {
        val result = mutableListOf<CleanableItem>()
        for (dir in dirs) {
            try {
                val files = dir.listFiles() ?: continue
                for (file in files) {
                    if (file.isFile && FileSafetyPolicy.isApkFile(file)) {
                        result.add(
                            CleanableItem(
                                id = UUID.randomUUID().toString(),
                                name = file.name,
                                path = file.absolutePath,
                                sizeBytes = file.length(),
                                lastModified = file.lastModified(),
                                category = CleanableCategory.OLD_APKS,
                                canDeleteDirectly = file.canWrite(),
                                isSelected = false // Require explicit user confirmation
                            )
                        )
                    }
                }
            } catch (_: Exception) {
            }
        }
        return result
    }

    private fun scanForThumbnails(dirs: List<File>): List<CleanableItem> {
        val result = mutableListOf<CleanableItem>()
        for (dir in dirs) {
            try {
                val files = dir.listFiles() ?: continue
                for (file in files) {
                    if (file.isFile && FileSafetyPolicy.isThumbnailCache(file)) {
                        result.add(
                            CleanableItem(
                                id = UUID.randomUUID().toString(),
                                name = file.name,
                                path = file.absolutePath,
                                sizeBytes = file.length(),
                                lastModified = file.lastModified(),
                                category = CleanableCategory.THUMBNAILS,
                                canDeleteDirectly = file.canWrite(),
                                isSelected = true
                            )
                        )
                    }
                }
            } catch (_: Exception) {
            }
        }
        return result
    }

    private fun scanForEmptyFiles(dirs: List<File>): List<CleanableItem> {
        val result = mutableListOf<CleanableItem>()
        for (dir in dirs) {
            try {
                val files = dir.listFiles() ?: continue
                for (file in files) {
                    if (file.isFile && FileSafetyPolicy.isSafeEmptyFile(file)) {
                        result.add(
                            CleanableItem(
                                id = UUID.randomUUID().toString(),
                                name = file.name,
                                path = file.absolutePath,
                                sizeBytes = 0L,
                                lastModified = file.lastModified(),
                                category = CleanableCategory.EMPTY_FILES,
                                canDeleteDirectly = file.canWrite(),
                                isSelected = true
                            )
                        )
                    }
                }
            } catch (_: Exception) {
            }
        }
        return result
    }
}
