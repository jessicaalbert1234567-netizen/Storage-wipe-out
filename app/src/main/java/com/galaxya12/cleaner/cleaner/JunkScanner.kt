package com.galaxya12.cleaner.cleaner

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.example.BuildConfig
import com.galaxya12.cleaner.model.CleanableCategory
import com.galaxya12.cleaner.model.CleanableCategorySummary
import com.galaxya12.cleaner.model.CleanableItem
import com.galaxya12.cleaner.model.ScanProgress
import com.galaxya12.cleaner.model.ScanResult
import com.galaxya12.cleaner.permissions.PermissionManager
import com.galaxya12.cleaner.permissions.SafManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import java.io.File
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.coroutineContext

class JunkScanner(private val context: Context) {

    data class ScanStep(
        val stage: String,
        val foundItems: List<CleanableItem>,
        val progress: ScanProgress,
        val scanResult: ScanResult
    )

    companion object {
        private const val TAG = "GalaxyCleaner/Scanner"

        private fun logDebug(message: String) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, message)
            }
        }
    }

    fun scanJunk(): Flow<ScanStep> = flow {
        val allFoundItems = mutableListOf<CleanableItem>()
        var totalScannedBytes = 0L
        var totalScannedFiles = 0
        var inaccessibleLocations = 0
        var requiresPermission = false

        val hasAllFiles = PermissionManager.hasAllFilesAccess()
        val hasStoragePerm = PermissionManager.hasStoragePermission(context)
        if (!hasAllFiles && !hasStoragePerm) {
            requiresPermission = true
            inaccessibleLocations++
        }

        fun buildCurrentResult(message: String = ""): ScanResult {
            val cleanable = allFoundItems.filter { it.isSelected }
            val cleanableBytes = cleanable.sumOf { it.sizeBytes }

            val categories = CleanableCategory.values().map { cat ->
                val catItems = allFoundItems.filter { it.category == cat }
                CleanableCategorySummary(
                    name = cat.displayName,
                    category = cat,
                    bytes = catItems.sumOf { it.sizeBytes },
                    fileCount = catItems.size,
                    items = catItems,
                    canDelete = true
                )
            }.filter { it.fileCount > 0 }

            return ScanResult(
                cleanableBytes = cleanableBytes,
                fileCount = cleanable.size,
                scannedBytes = totalScannedBytes,
                scannedFiles = totalScannedFiles,
                inaccessibleLocations = inaccessibleLocations,
                requiresPermission = requiresPermission,
                categories = categories,
                statusDescription = message
            )
        }

        // -------------------------------------------------------------
        // Stage 1: Application's own cache & temporary files
        // -------------------------------------------------------------
        emit(
            ScanStep(
                stage = "Scanning app cache...",
                foundItems = emptyList(),
                progress = ScanProgress("App cache", totalScannedFiles, 0L, false, "Checking application cache"),
                scanResult = buildCurrentResult("Scanning application cache...")
            )
        )
        delay(60)

        val appDirsToScan = listOfNotNull(
            context.cacheDir,
            context.codeCacheDir,
            context.externalCacheDir,
            context.filesDir?.resolve("temp"),
            context.filesDir?.resolve("cache"),
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.resolve(".thumbnails")
        ) + (context.externalCacheDirs?.filterNotNull() ?: emptyList())

        for (dir in appDirsToScan.distinctBy { it.absolutePath }) {
            if (!coroutineContext.isActive) break
            logDebug("Scanning cache: ${dir.absolutePath}")
            val (items, bytes, count) = scanInternalAppDir(dir)
            allFoundItems.addAll(items)
            totalScannedBytes += bytes
            totalScannedFiles += count
        }

        // -------------------------------------------------------------
        // Stage 2: Scanning Shared Storage, Downloads & Thumbnails
        // -------------------------------------------------------------
        emit(
            ScanStep(
                stage = "Scanning storage & Downloads...",
                foundItems = allFoundItems.toList(),
                progress = ScanProgress("Downloads", totalScannedFiles, allFoundItems.sumOf { it.sizeBytes }, false, "Inspecting shared storage"),
                scanResult = buildCurrentResult("Scanning shared storage...")
            )
        )
        delay(60)

        if (hasAllFiles || hasStoragePerm) {
            try {
                val (extItems, extBytes, extCount) = scanExternalStorageTree()
                val existingPaths = allFoundItems.map { it.path }.toSet()
                for (item in extItems) {
                    if (item.path !in existingPaths) {
                        allFoundItems.add(item)
                    }
                }
                totalScannedBytes += extBytes
                totalScannedFiles += extCount
            } catch (e: SecurityException) {
                inaccessibleLocations++
                requiresPermission = true
                logDebug("Permission denied on storage: ${e.message}")
            } catch (_: Exception) {
                inaccessibleLocations++
            }
        } else {
            // Check downloads if readable directly
            try {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (downloadsDir != null && downloadsDir.exists() && downloadsDir.canRead()) {
                    val (dlItems, dlBytes, dlCount) = scanDirectoryRecursively(downloadsDir, maxDepth = 2)
                    allFoundItems.addAll(dlItems)
                    totalScannedBytes += dlBytes
                    totalScannedFiles += dlCount
                } else {
                    inaccessibleLocations++
                    requiresPermission = true
                }
            } catch (_: Exception) {
                inaccessibleLocations++
                requiresPermission = true
            }
        }

        // -------------------------------------------------------------
        // Stage 3: Scanning temporary files & MediaStore
        // -------------------------------------------------------------
        emit(
            ScanStep(
                stage = "Scanning temporary files...",
                foundItems = allFoundItems.toList(),
                progress = ScanProgress("Temporary files", totalScannedFiles, allFoundItems.sumOf { it.sizeBytes }, false, "Querying MediaStore"),
                scanResult = buildCurrentResult("Scanning temporary files via MediaStore...")
            )
        )
        delay(60)

        if (hasStoragePerm || hasAllFiles) {
            try {
                val mediaStoreItems = scanMediaStoreForTempAndJunk()
                val existingPaths = allFoundItems.map { it.path }.toSet()
                for (item in mediaStoreItems.items) {
                    if (item.path !in existingPaths) {
                        allFoundItems.add(item)
                    }
                }
                totalScannedBytes += mediaStoreItems.scannedBytes
                totalScannedFiles += mediaStoreItems.scannedFiles
            } catch (e: SecurityException) {
                inaccessibleLocations++
                requiresPermission = true
                logDebug("Permission denied during MediaStore query: ${e.message}")
            } catch (e: Exception) {
                logDebug("MediaStore query exception: ${e.message}")
            }
        }

        // -------------------------------------------------------------
        // Stage 4: Checking APK files
        // -------------------------------------------------------------
        emit(
            ScanStep(
                stage = "Checking APK files...",
                foundItems = allFoundItems.toList(),
                progress = ScanProgress("APK files", totalScannedFiles, allFoundItems.sumOf { it.sizeBytes }, false, "Scanning for APK packages"),
                scanResult = buildCurrentResult("Checking accessible APK files...")
            )
        )
        delay(60)

        if (hasStoragePerm || hasAllFiles) {
            try {
                val apkItems = scanMediaStoreForApks()
                val existingPaths = allFoundItems.map { it.path }.toSet()
                for (apk in apkItems.items) {
                    if (apk.path !in existingPaths) {
                        allFoundItems.add(apk)
                    }
                }
                totalScannedBytes += apkItems.scannedBytes
                totalScannedFiles += apkItems.scannedFiles
            } catch (_: Exception) {
            }
        }

        // -------------------------------------------------------------
        // Stage 5: Checking user-selected SAF folders (if granted)
        // -------------------------------------------------------------
        emit(
            ScanStep(
                stage = "Checking accessible folders...",
                foundItems = allFoundItems.toList(),
                progress = ScanProgress("Shared storage", totalScannedFiles, allFoundItems.sumOf { it.sizeBytes }, false, "Checking storage access"),
                scanResult = buildCurrentResult("Checking user-selected folders...")
            )
        )
        delay(60)

        val safUri = SafManager.getPersistedUri(context)
        if (safUri != null) {
            logDebug("SAF scan: $safUri")
            try {
                val safResult = scanSafDocumentTree(safUri)
                val existingPaths = allFoundItems.map { it.path }.toSet()
                for (item in safResult.items) {
                    if (item.path !in existingPaths) {
                        allFoundItems.add(item)
                    }
                }
                totalScannedBytes += safResult.scannedBytes
                totalScannedFiles += safResult.scannedFiles
            } catch (e: Exception) {
                logDebug("SAF scan failed: ${e.message}")
                inaccessibleLocations++
            }
        }

        // -------------------------------------------------------------
        // Final Completion
        // -------------------------------------------------------------
        val finalCleanableBytes = allFoundItems.filter { it.isSelected }.sumOf { it.sizeBytes }
        logDebug("Total: $finalCleanableBytes cleanable bytes across ${allFoundItems.size} items.")

        val finalStatus = when {
            allFoundItems.isEmpty() && requiresPermission -> "Storage access required to scan additional folders."
            allFoundItems.isEmpty() -> "0 B — No safe junk found"
            inaccessibleLocations > 0 -> "Some locations could not be scanned."
            else -> "Scan completed successfully."
        }

        val finalResult = buildCurrentResult(finalStatus)

        emit(
            ScanStep(
                stage = "Scan Complete",
                foundItems = allFoundItems.toList(),
                progress = ScanProgress("Complete", totalScannedFiles, finalCleanableBytes, true, finalStatus),
                scanResult = finalResult
            )
        )
    }.flowOn(Dispatchers.IO)

    private fun scanExternalStorageTree(): Triple<List<CleanableItem>, Long, Int> {
        val items = mutableListOf<CleanableItem>()
        var bytes = 0L
        var count = 0

        val externalRoot = Environment.getExternalStorageDirectory() ?: return Triple(items, bytes, count)
        if (!externalRoot.exists() || !externalRoot.canRead()) return Triple(items, bytes, count)

        val candidateDirs = listOfNotNull(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            File(externalRoot, "DCIM/.thumbnails"),
            File(externalRoot, "Pictures/.thumbnails"),
            File(externalRoot, "Movies/.thumbnails"),
            File(externalRoot, "LOST.DIR"),
            File(externalRoot, ".cache"),
            File(externalRoot, ".temp"),
            File(externalRoot, "temp")
        )

        val scannedPaths = mutableSetOf<String>()

        for (dir in candidateDirs) {
            if (dir.exists() && dir.canRead() && scannedPaths.add(dir.absolutePath)) {
                val (dirItems, dirBytes, dirCount) = scanDirectoryRecursively(dir, maxDepth = 3)
                items.addAll(dirItems)
                bytes += dirBytes
                count += dirCount
            }
        }

        try {
            val rootFiles = externalRoot.listFiles()
            if (rootFiles != null) {
                for (file in rootFiles) {
                    if (file.isFile) {
                        count++
                        val len = file.length()
                        bytes += len
                        if (FileSafetyPolicy.isThumbnailCache(file)) {
                            items.add(fileToCleanableItem(file, CleanableCategory.THUMBNAILS))
                        } else if (FileSafetyPolicy.isSafeJunkCandidate(file)) {
                            items.add(fileToCleanableItem(file, CleanableCategory.TEMP_FILES))
                        } else if (FileSafetyPolicy.isApkFile(file)) {
                            items.add(fileToCleanableItem(file, CleanableCategory.OLD_APKS, isSelected = false))
                        } else if (FileSafetyPolicy.isSafeEmptyFile(file)) {
                            items.add(fileToCleanableItem(file, CleanableCategory.EMPTY_FILES))
                        }
                    } else if (file.isDirectory && !FileSafetyPolicy.isProtectedDirectory(file)) {
                        val lower = file.name.lowercase(Locale.US)
                        if (lower == "cache" || lower == "temp" || lower == "logs" || lower == ".cache" || lower == ".temp") {
                            if (scannedPaths.add(file.absolutePath)) {
                                val (dirItems, dirBytes, dirCount) = scanDirectoryRecursively(file, maxDepth = 2)
                                items.addAll(dirItems)
                                bytes += dirBytes
                                count += dirCount
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }

        return Triple(items, bytes, count)
    }

    private fun scanDirectoryRecursively(
        dir: File,
        currentDepth: Int = 0,
        maxDepth: Int = 3
    ): Triple<List<CleanableItem>, Long, Int> {
        val items = mutableListOf<CleanableItem>()
        var bytes = 0L
        var count = 0

        if (currentDepth > maxDepth || !dir.exists() || !dir.canRead() || FileSafetyPolicy.isProtectedDirectory(dir)) {
            return Triple(items, bytes, count)
        }

        try {
            val files = dir.listFiles() ?: return Triple(items, bytes, count)
            for (file in files) {
                if (FileSafetyPolicy.isProtectedDirectory(file)) continue

                if (file.isDirectory) {
                    val (subItems, subBytes, subCount) = scanDirectoryRecursively(file, currentDepth + 1, maxDepth)
                    items.addAll(subItems)
                    bytes += subBytes
                    count += subCount
                } else if (file.isFile) {
                    count++
                    val len = file.length()
                    bytes += len

                    if (FileSafetyPolicy.isThumbnailCache(file)) {
                        items.add(fileToCleanableItem(file, CleanableCategory.THUMBNAILS))
                    } else if (FileSafetyPolicy.isSafeJunkCandidate(file)) {
                        items.add(fileToCleanableItem(file, CleanableCategory.TEMP_FILES))
                    } else if (FileSafetyPolicy.isApkFile(file)) {
                        items.add(fileToCleanableItem(file, CleanableCategory.OLD_APKS, isSelected = false))
                    } else if (FileSafetyPolicy.isSafeEmptyFile(file)) {
                        items.add(fileToCleanableItem(file, CleanableCategory.EMPTY_FILES))
                    }
                }
            }
        } catch (_: Exception) {
        }

        return Triple(items, bytes, count)
    }

    private fun scanInternalAppDir(dir: File): Triple<List<CleanableItem>, Long, Int> {
        val items = mutableListOf<CleanableItem>()
        var bytes = 0L
        var count = 0

        if (!dir.exists() || !dir.canRead()) return Triple(items, bytes, count)

        try {
            val files = dir.listFiles() ?: return Triple(items, bytes, count)
            for (file in files) {
                if (file.isDirectory) {
                    val (subItems, subBytes, subCount) = scanInternalAppDir(file)
                    items.addAll(subItems)
                    bytes += subBytes
                    count += subCount
                } else if (file.isFile) {
                    count++
                    val len = file.length()
                    bytes += len

                    val category = if (file.name.endsWith(".tmp", ignoreCase = true) ||
                        file.name.endsWith(".temp", ignoreCase = true)
                    ) {
                        CleanableCategory.TEMP_FILES
                    } else {
                        CleanableCategory.APP_CACHE
                    }

                    val item = CleanableItem(
                        id = UUID.randomUUID().toString(),
                        name = file.name,
                        path = file.absolutePath,
                        sizeBytes = len,
                        lastModified = file.lastModified(),
                        category = category,
                        canDeleteDirectly = true,
                        isSelected = true
                    )
                    items.add(item)
                    logDebug("Found app cache file: ${file.name} ($len bytes)")
                }
            }
        } catch (_: Exception) {
        }

        return Triple(items, bytes, count)
    }

    private data class MediaScanResult(
        val items: List<CleanableItem>,
        val scannedBytes: Long,
        val scannedFiles: Int
    )

    private fun scanMediaStoreForTempAndJunk(): MediaScanResult {
        val items = mutableListOf<CleanableItem>()
        var scannedBytes = 0L
        var scannedFiles = 0

        val collection = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATA
        )

        val selection = "(" +
                "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.tmp' OR " +
                "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.temp' OR " +
                "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.log' OR " +
                "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.bak' OR " +
                "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.crdownload' OR " +
                "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.part' OR " +
                "${MediaStore.Files.FileColumns.SIZE} = 0" +
                ")"

        try {
            context.contentResolver.query(collection, projection, selection, null, null)?.use { cursor ->
                val idCol = cursor.getColumnIndex(MediaStore.Files.FileColumns._ID)
                val nameCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE)
                val modCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_MODIFIED)
                val dataCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)

                while (cursor.moveToNext()) {
                    val id = if (idCol != -1) cursor.getLong(idCol) else continue
                    val name = if (nameCol != -1) cursor.getString(nameCol) ?: "unknown" else "unknown"
                    val size = if (sizeCol != -1) cursor.getLong(sizeCol) else 0L
                    val mod = if (modCol != -1) cursor.getLong(modCol) * 1000L else System.currentTimeMillis()
                    val path = if (dataCol != -1) cursor.getString(dataCol) ?: "" else ""

                    scannedFiles++
                    scannedBytes += size

                    val fileRef = if (path.isNotEmpty()) File(path) else null
                    if (fileRef != null && FileSafetyPolicy.isProtectedDirectory(fileRef)) {
                        continue
                    }
                    if (fileRef != null && FileSafetyPolicy.isProtectedUserMediaDirectory(fileRef) && !FileSafetyPolicy.isThumbnailCache(fileRef)) {
                        continue
                    }

                    val contentUri = ContentUris.withAppendedId(collection, id)

                    val category = when {
                        FileSafetyPolicy.isThumbnailCache(File(path.ifEmpty { name })) -> CleanableCategory.THUMBNAILS
                        size == 0L -> CleanableCategory.EMPTY_FILES
                        else -> CleanableCategory.TEMP_FILES
                    }

                    items.add(
                        CleanableItem(
                            id = UUID.randomUUID().toString(),
                            name = name,
                            path = path.ifEmpty { contentUri.toString() },
                            sizeBytes = size,
                            lastModified = mod,
                            category = category,
                            canDeleteDirectly = true,
                            isSelected = true,
                            uri = contentUri
                        )
                    )
                }
            }
        } catch (e: Exception) {
            logDebug("Error in scanMediaStoreForTempAndJunk: ${e.message}")
        }

        return MediaScanResult(items, scannedBytes, scannedFiles)
    }

    private fun scanMediaStoreForApks(): MediaScanResult {
        val items = mutableListOf<CleanableItem>()
        var scannedBytes = 0L
        var scannedFiles = 0

        val collection = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.DATA
        )

        val selection = "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.apk'"

        try {
            context.contentResolver.query(collection, projection, selection, null, null)?.use { cursor ->
                val idCol = cursor.getColumnIndex(MediaStore.Files.FileColumns._ID)
                val nameCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE)
                val modCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_MODIFIED)
                val dataCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)

                while (cursor.moveToNext()) {
                    val id = if (idCol != -1) cursor.getLong(idCol) else continue
                    val name = if (nameCol != -1) cursor.getString(nameCol) ?: "unknown.apk" else "unknown.apk"
                    val size = if (sizeCol != -1) cursor.getLong(sizeCol) else 0L
                    val mod = if (modCol != -1) cursor.getLong(modCol) * 1000L else System.currentTimeMillis()
                    val path = if (dataCol != -1) cursor.getString(dataCol) ?: "" else ""

                    scannedFiles++
                    scannedBytes += size

                    val contentUri = ContentUris.withAppendedId(collection, id)

                    items.add(
                        CleanableItem(
                            id = UUID.randomUUID().toString(),
                            name = name,
                            path = path.ifEmpty { contentUri.toString() },
                            sizeBytes = size,
                            lastModified = mod,
                            category = CleanableCategory.OLD_APKS,
                            canDeleteDirectly = true,
                            isSelected = false,
                            uri = contentUri
                        )
                    )
                }
            }
        } catch (e: Exception) {
            logDebug("Error in scanMediaStoreForApks: ${e.message}")
        }

        return MediaScanResult(items, scannedBytes, scannedFiles)
    }

    private fun scanSafDocumentTree(treeUri: Uri): MediaScanResult {
        val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return MediaScanResult(emptyList(), 0L, 0)
        return scanSafDocumentDir(rootDoc, depth = 0, maxDepth = 4)
    }

    private fun scanSafDocumentDir(docDir: DocumentFile, depth: Int, maxDepth: Int): MediaScanResult {
        val items = mutableListOf<CleanableItem>()
        var scannedBytes = 0L
        var scannedFiles = 0

        if (depth > maxDepth || !docDir.canRead()) return MediaScanResult(items, scannedBytes, scannedFiles)

        val children = docDir.listFiles()
        for (doc in children) {
            val name = doc.name ?: continue
            val lowerName = name.lowercase(Locale.US)

            if (lowerName == "data" || lowerName == "obb" || lowerName == "android") {
                continue
            }

            if (doc.isDirectory) {
                val subResult = scanSafDocumentDir(doc, depth + 1, maxDepth)
                items.addAll(subResult.items)
                scannedBytes += subResult.scannedBytes
                scannedFiles += subResult.scannedFiles
            } else if (doc.isFile) {
                scannedFiles++
                val size = doc.length()
                scannedBytes += size
                val lastMod = doc.lastModified()

                if (lowerName.endsWith(".tmp") || lowerName.endsWith(".temp") ||
                    (lowerName.endsWith(".log") && !lowerName.contains("system")) ||
                    lowerName.endsWith(".crdownload") || lowerName.endsWith(".part")
                ) {
                    items.add(
                        CleanableItem(
                            id = UUID.randomUUID().toString(),
                            name = name,
                            path = doc.uri.toString(),
                            sizeBytes = size,
                            lastModified = lastMod,
                            category = CleanableCategory.TEMP_FILES,
                            canDeleteDirectly = doc.canWrite(),
                            isSelected = true,
                            uri = doc.uri,
                            isSafDocument = true
                        )
                    )
                    logDebug("Found SAF temp file: $name ($size bytes)")
                } else if (lowerName.endsWith(".apk")) {
                    items.add(
                        CleanableItem(
                            id = UUID.randomUUID().toString(),
                            name = name,
                            path = doc.uri.toString(),
                            sizeBytes = size,
                            lastModified = lastMod,
                            category = CleanableCategory.OLD_APKS,
                            canDeleteDirectly = doc.canWrite(),
                            isSelected = false,
                            uri = doc.uri,
                            isSafDocument = true
                        )
                    )
                    logDebug("Found SAF APK file: $name")
                } else if (size == 0L && !name.startsWith(".")) {
                    items.add(
                        CleanableItem(
                            id = UUID.randomUUID().toString(),
                            name = name,
                            path = doc.uri.toString(),
                            sizeBytes = 0L,
                            lastModified = lastMod,
                            category = CleanableCategory.EMPTY_FILES,
                            canDeleteDirectly = doc.canWrite(),
                            isSelected = true,
                            uri = doc.uri,
                            isSafDocument = true
                        )
                    )
                }
            }
        }

        return MediaScanResult(items, scannedBytes, scannedFiles)
    }

    private fun fileToCleanableItem(
        file: File,
        category: CleanableCategory,
        isSelected: Boolean = true
    ): CleanableItem {
        return CleanableItem(
            id = UUID.randomUUID().toString(),
            name = file.name,
            path = file.absolutePath,
            sizeBytes = file.length(),
            lastModified = file.lastModified(),
            category = category,
            canDeleteDirectly = file.canWrite(),
            isSelected = isSelected
        )
    }
}
