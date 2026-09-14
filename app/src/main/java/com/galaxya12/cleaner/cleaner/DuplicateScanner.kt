package com.galaxya12.cleaner.cleaner

import android.content.Context
import android.os.Environment
import com.galaxya12.cleaner.model.CleanableCategory
import com.galaxya12.cleaner.model.CleanableItem
import com.galaxya12.cleaner.model.DuplicateGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.UUID

class DuplicateScanner(private val context: Context) {

    suspend fun scanDuplicates(dirs: List<File>? = null): List<DuplicateGroup> {
        return withContext(Dispatchers.IO) {
            val targetDirs = dirs ?: listOfNotNull(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                context.getExternalFilesDir(null)
            )

            // Step 1: Collect accessible candidate files (size > 1KB, isFile, readable)
            val candidateFiles = mutableListOf<File>()
            for (dir in targetDirs) {
                if (dir.exists() && dir.canRead() && !FileSafetyPolicy.isProtectedDirectory(dir)) {
                    collectFiles(dir, candidateFiles, maxDepth = 2)
                }
            }

            // STAGE 1: Compare file size.
            // Group files by exact byte size. Discard sizes with only 1 file.
            val sizeBuckets = candidateFiles
                .groupBy { it.length() }
                .filter { (size, files) -> size > 1024L && files.size > 1 }

            if (sizeBuckets.isEmpty()) return@withContext emptyList()

            // STAGE 2: Compare small sample / partial hash (first 4KB)
            val partialHashBuckets = mutableMapOf<String, MutableList<File>>()
            val buffer = ByteArray(4096)

            for ((size, files) in sizeBuckets) {
                for (file in files) {
                    val partialHash = computeSampleHash(file, buffer)
                    if (partialHash != null) {
                        val key = "$size-$partialHash"
                        partialHashBuckets.getOrPut(key) { mutableListOf() }.add(file)
                    }
                }
            }

            val filteredPartial = partialHashBuckets.filter { it.value.size > 1 }
            if (filteredPartial.isEmpty()) return@withContext emptyList()

            // STAGE 3: Calculate full SHA-256 ONLY for candidates with matching size and partial hash
            val fullHashBuckets = mutableMapOf<String, MutableList<File>>()
            for ((_, files) in filteredPartial) {
                for (file in files) {
                    val fullHash = computeFullSha256(file, buffer)
                    if (fullHash != null) {
                        val key = "${file.length()}-$fullHash"
                        fullHashBuckets.getOrPut(key) { mutableListOf() }.add(file)
                    }
                }
            }

            // Group duplicates into DuplicateGroup objects
            var groupIndex = 1
            val duplicateGroups = mutableListOf<DuplicateGroup>()

            for ((_, duplicateFileList) in fullHashBuckets) {
                if (duplicateFileList.size > 1) {
                    val groupId = "Group #$groupIndex"
                    groupIndex++

                    // Sort by lastModified: oldest is considered original, others are candidate duplicates
                    val sortedFiles = duplicateFileList.sortedBy { it.lastModified() }
                    val items = sortedFiles.mapIndexed { index, file ->
                        CleanableItem(
                            id = UUID.randomUUID().toString(),
                            name = file.name,
                            path = file.absolutePath,
                            sizeBytes = file.length(),
                            lastModified = file.lastModified(),
                            category = CleanableCategory.DUPLICATE_FILES,
                            canDeleteDirectly = file.canWrite(),
                            isSelected = index > 0, // Never select the original copy by default!
                            duplicateGroupId = groupId,
                            isOriginal = (index == 0)
                        )
                    }

                    duplicateGroups.add(
                        DuplicateGroup(
                            groupId = groupId,
                            sizePerFile = duplicateFileList.first().length(),
                            items = items
                        )
                    )
                }
            }

            duplicateGroups
        }
    }

    private fun collectFiles(dir: File, result: MutableList<File>, maxDepth: Int) {
        if (maxDepth < 0 || !dir.exists() || !dir.canRead()) return
        try {
            val files = dir.listFiles() ?: return
            for (file in files) {
                if (file.isFile && !FileSafetyPolicy.isProtectedDirectory(file)) {
                    result.add(file)
                } else if (file.isDirectory && !FileSafetyPolicy.isProtectedDirectory(file)) {
                    collectFiles(file, result, maxDepth - 1)
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun computeSampleHash(file: File, buffer: ByteArray): String? {
        return try {
            FileInputStream(file).use { fis ->
                val bytesRead = fis.read(buffer, 0, buffer.size)
                if (bytesRead <= 0) return null
                val md = MessageDigest.getInstance("MD5")
                md.update(buffer, 0, bytesRead)
                bytesToHex(md.digest())
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun computeFullSha256(file: File, buffer: ByteArray): String? {
        return try {
            FileInputStream(file).use { fis ->
                val md = MessageDigest.getInstance("SHA-256")
                var read: Int
                while (fis.read(buffer).also { read = it } != -1) {
                    md.update(buffer, 0, read)
                }
                bytesToHex(md.digest())
            }
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        fun bytesToHex(bytes: ByteArray): String {
            val sb = StringBuilder(bytes.size * 2)
            for (b in bytes) {
                sb.append(String.format("%02x", b))
            }
            return sb.toString()
        }

        fun calculateStreamSha256(input: InputStream): String {
            val md = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(4096)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                md.update(buffer, 0, read)
            }
            return bytesToHex(md.digest())
        }
    }
}
