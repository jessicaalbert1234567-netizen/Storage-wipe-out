package com.galaxya12.cleaner.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.galaxya12.cleaner.cleaner.CleanerEngine
import com.galaxya12.cleaner.cleaner.DuplicateScanner
import com.galaxya12.cleaner.cleaner.JunkScanner
import com.galaxya12.cleaner.cleaner.LargeFileScanner
import com.galaxya12.cleaner.cleaner.LargeFileThreshold
import com.galaxya12.cleaner.cleaner.StorageAnalyzer
import com.galaxya12.cleaner.model.CleanResult
import com.galaxya12.cleaner.model.CleanableCategory
import com.galaxya12.cleaner.model.CleanableCategorySummary
import com.galaxya12.cleaner.model.CleanableItem
import com.galaxya12.cleaner.model.DuplicateGroup
import com.galaxya12.cleaner.model.ScanProgress
import com.galaxya12.cleaner.model.ScanResult
import com.galaxya12.cleaner.model.ScanState
import com.galaxya12.cleaner.model.StorageInfo
import com.galaxya12.cleaner.permissions.SafManager
import com.galaxya12.cleaner.widget.CleanerWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class CleanerViewModel(application: Application) : AndroidViewModel(application) {

    private val storageAnalyzer = StorageAnalyzer(application)
    private val junkScanner = JunkScanner(application)
    private val largeFileScanner = LargeFileScanner(application)
    private val duplicateScanner = DuplicateScanner(application)
    private val cleanerEngine = CleanerEngine(application)

    private val _storageInfo = MutableStateFlow(StorageInfo())
    val storageInfo: StateFlow<StorageInfo> = _storageInfo.asStateFlow()

    private val _scanState = MutableStateFlow(ScanState.IDLE)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val _scanProgress = MutableStateFlow(ScanProgress())
    val scanProgress: StateFlow<ScanProgress> = _scanProgress.asStateFlow()

    private val _scanResult = MutableStateFlow(ScanResult())
    val scanResult: StateFlow<ScanResult> = _scanResult.asStateFlow()

    private val _foundItems = MutableStateFlow<List<CleanableItem>>(emptyList())
    val foundItems: StateFlow<List<CleanableItem>> = _foundItems.asStateFlow()

    private val _largeFiles = MutableStateFlow<List<CleanableItem>>(emptyList())
    val largeFiles: StateFlow<List<CleanableItem>> = _largeFiles.asStateFlow()

    private val _largeFileThreshold = MutableStateFlow(LargeFileThreshold.MB_100)
    val largeFileThreshold: StateFlow<LargeFileThreshold> = _largeFileThreshold.asStateFlow()

    private val _duplicateGroups = MutableStateFlow<List<DuplicateGroup>>(emptyList())
    val duplicateGroups: StateFlow<List<DuplicateGroup>> = _duplicateGroups.asStateFlow()

    private val _cleanResult = MutableStateFlow<CleanResult?>(null)
    val cleanResult: StateFlow<CleanResult?> = _cleanResult.asStateFlow()

    private val _cleaningMessage = MutableStateFlow("")
    val cleaningMessage: StateFlow<String> = _cleaningMessage.asStateFlow()

    // SAF Status
    private val _hasSafAccess = MutableStateFlow(SafManager.hasSafAccess(application))
    val hasSafAccess: StateFlow<Boolean> = _hasSafAccess.asStateFlow()

    private val _safFolderName = MutableStateFlow(SafManager.getPersistedFolderName(application))
    val safFolderName: StateFlow<String?> = _safFolderName.asStateFlow()

    // Developer Test Status (DEBUG only)
    private val _devTestStatus = MutableStateFlow("")
    val devTestStatus: StateFlow<String> = _devTestStatus.asStateFlow()

    private var scanJob: Job? = null
    private var cleanJob: Job? = null

    init {
        refreshStorageInfo()
    }

    fun refreshStorageInfo() {
        val info = storageAnalyzer.queryStorageInfo()
        _storageInfo.value = info
    }

    fun refreshSafStatus() {
        _hasSafAccess.value = SafManager.hasSafAccess(getApplication())
        _safFolderName.value = SafManager.getPersistedFolderName(getApplication())
    }

    fun saveSafFolder(uri: Uri) {
        SafManager.savePersistedUri(getApplication(), uri)
        refreshSafStatus()
        startScan()
    }

    fun removeSafFolder() {
        SafManager.removeSafAccess(getApplication())
        refreshSafStatus()
        startScan()
    }

    fun startScan() {
        scanJob?.cancel()
        _cleanResult.value = null
        _scanState.value = ScanState.SCANNING

        scanJob = viewModelScope.launch {
            try {
                junkScanner.scanJunk().collect { step ->
                    _foundItems.value = step.foundItems
                    _scanProgress.value = step.progress
                    _scanResult.value = step.scanResult
                }

                // Update large files in background
                val large = largeFileScanner.scanLargeFiles(_largeFileThreshold.value)
                _largeFiles.value = large

                // Update duplicate groups
                val duplicates = duplicateScanner.scanDuplicates()
                _duplicateGroups.value = duplicates

                _scanState.value = ScanState.COMPLETE

                // Update widget cache with cleanable junk bytes
                val cleanableBytes = _foundItems.value.filter { it.isSelected }.sumOf { it.sizeBytes }
                CleanerWidget.setCachedJunkBytes(getApplication(), cleanableBytes, hasScanned = true)
                CleanerWidget.notifyDataChanged(getApplication())
            } catch (_: kotlinx.coroutines.CancellationException) {
                _scanState.value = ScanState.IDLE
            } catch (_: Exception) {
                _scanState.value = ScanState.IDLE
            }
        }
    }

    fun cancelScan() {
        scanJob?.cancel()
        _scanState.value = ScanState.IDLE
    }

    fun startClean() {
        if (_scanState.value == ScanState.CLEANING) return
        val itemsToClean = _foundItems.value.filter { it.isSelected }
        if (itemsToClean.isEmpty()) return

        cleanJob?.cancel()
        _scanState.value = ScanState.CLEANING

        cleanJob = viewModelScope.launch {
            try {
                cleanerEngine.cleanItems(itemsToClean).collect { progress ->
                    _cleaningMessage.value = progress.currentItemName
                    if (progress.isDone) {
                        val result = CleanResult(
                            deletedCount = progress.deletedCount,
                            failedCount = progress.failedCount,
                            freedBytes = progress.freedBytes
                        )
                        _cleanResult.value = result
                        _scanState.value = ScanState.CLEAN_COMPLETE

                        // Remove deleted items from list
                        _foundItems.value = _foundItems.value.filter { !it.isSelected }

                        // Recompute scan result
                        _scanResult.value = _scanResult.value.copy(
                            cleanableBytes = _foundItems.value.filter { it.isSelected }.sumOf { it.sizeBytes },
                            fileCount = _foundItems.value.count { it.isSelected }
                        )

                        // Refresh device storage
                        refreshStorageInfo()

                        // Update widget
                        CleanerWidget.setCachedJunkBytes(getApplication(), 0L, hasScanned = true)
                        CleanerWidget.notifyDataChanged(getApplication())
                    }
                }
            } catch (_: Exception) {
                _scanState.value = ScanState.COMPLETE
            }
        }
    }

    fun toggleItemSelection(itemId: String) {
        _foundItems.value = _foundItems.value.map {
            if (it.id == itemId) it.copy(isSelected = !it.isSelected) else it
        }
        recalculateCleanableBytes()
    }

    fun selectAllCategory(category: CleanableCategory, select: Boolean) {
        _foundItems.value = _foundItems.value.map {
            if (it.category == category) it.copy(isSelected = select) else it
        }
        recalculateCleanableBytes()
    }

    private fun recalculateCleanableBytes() {
        val cleanable = _foundItems.value.filter { it.isSelected }
        _scanResult.value = _scanResult.value.copy(
            cleanableBytes = cleanable.sumOf { it.sizeBytes },
            fileCount = cleanable.size
        )
    }

    fun setLargeFileThreshold(threshold: LargeFileThreshold) {
        _largeFileThreshold.value = threshold
        viewModelScope.launch {
            val items = largeFileScanner.scanLargeFiles(threshold)
            _largeFiles.value = items
        }
    }

    fun toggleDuplicateItem(itemId: String) {
        _duplicateGroups.value = _duplicateGroups.value.map { group ->
            group.copy(
                items = group.items.map { item ->
                    if (item.id == itemId) item.copy(isSelected = !item.isSelected) else item
                }
            )
        }
    }

    fun getCategorySummaries(): List<CleanableCategorySummary> {
        val items = _foundItems.value
        val distinctCats = listOf(
            CleanableCategory.APP_CACHE,
            CleanableCategory.TEMP_FILES,
            CleanableCategory.OLD_APKS,
            CleanableCategory.EMPTY_FILES
        )

        return distinctCats.map { cat ->
            val catItems = items.filter { it.category == cat }
            CleanableCategorySummary(
                name = cat.displayName,
                category = cat,
                bytes = catItems.sumOf { it.sizeBytes },
                fileCount = catItems.size,
                items = catItems,
                canDelete = true
            )
        }
    }

    val totalCleanableBytes: Long
        get() = _foundItems.value.filter { it.isSelected }.sumOf { it.sizeBytes }

    fun clearCleanResult() {
        _cleanResult.value = null
        _scanState.value = ScanState.IDLE
    }

    // -------------------------------------------------------------
    // REAL TEST MODE (Developer Test in DEBUG only)
    // -------------------------------------------------------------
    fun createDeveloperTestJunk() {
        if (!BuildConfig.DEBUG) return

        viewModelScope.launch(Dispatchers.IO) {
            val cacheDir = getApplication<Application>().cacheDir
            val testFiles = listOf(
                Pair("test_junk_1.tmp", 5 * 1024 * 1024), // 5 MB
                Pair("test_junk_2.tmp", 3 * 1024 * 1024), // 3 MB
                Pair("test_cache.tmp", 2 * 1024 * 1024)   // 2 MB
            )

            var createdTotal = 0L
            val dummyBuffer = ByteArray(64 * 1024) { 0x41.toByte() } // 64 KB chunks

            for ((fileName, size) in testFiles) {
                val file = File(cacheDir, fileName)
                try {
                    FileOutputStream(file).use { fos ->
                        var written = 0
                        while (written < size) {
                            val toWrite = minOf(dummyBuffer.size, size - written)
                            fos.write(dummyBuffer, 0, toWrite)
                            written += toWrite
                        }
                    }
                    createdTotal += file.length()
                } catch (_: Exception) {
                }
            }

            withContext(Dispatchers.Main) {
                _devTestStatus.value = "Created: 10 MB test junk (test_junk_1.tmp, test_junk_2.tmp, test_cache.tmp)"
                startScan()
            }
        }
    }
}
