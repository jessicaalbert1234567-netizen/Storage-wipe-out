package com.galaxya12.cleaner.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.galaxya12.cleaner.cleaner.CleanerEngine
import com.galaxya12.cleaner.cleaner.DuplicateScanner
import com.galaxya12.cleaner.cleaner.JunkScanner
import com.galaxya12.cleaner.cleaner.LargeFileScanner
import com.galaxya12.cleaner.cleaner.LargeFileThreshold
import com.galaxya12.cleaner.cleaner.StorageAnalyzer
import com.galaxya12.cleaner.model.CategorySummary
import com.galaxya12.cleaner.model.CleanResult
import com.galaxya12.cleaner.model.CleanableCategory
import com.galaxya12.cleaner.model.CleanableItem
import com.galaxya12.cleaner.model.DuplicateGroup
import com.galaxya12.cleaner.model.ScanProgress
import com.galaxya12.cleaner.model.ScanState
import com.galaxya12.cleaner.model.StorageInfo
import com.galaxya12.cleaner.widget.CleanerWidget
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

    private var scanJob: Job? = null
    private var cleanJob: Job? = null

    init {
        refreshStorageInfo()
    }

    fun refreshStorageInfo() {
        val info = storageAnalyzer.queryStorageInfo()
        _storageInfo.value = info
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
                CleanerWidget.setCachedJunkBytes(getApplication(), cleanableBytes)
                CleanerWidget.notifyDataChanged(getApplication())
            } catch (e: Exception) {
                _scanState.value = ScanState.IDLE
            }
        }
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

                        // Refresh device storage
                        refreshStorageInfo()

                        // Update widget
                        CleanerWidget.setCachedJunkBytes(getApplication(), 0L)
                        CleanerWidget.notifyDataChanged(getApplication())
                    }
                }
            } catch (e: Exception) {
                _scanState.value = ScanState.COMPLETE
            }
        }
    }

    fun toggleItemSelection(itemId: String) {
        _foundItems.value = _foundItems.value.map {
            if (it.id == itemId) it.copy(isSelected = !it.isSelected) else it
        }
    }

    fun selectAllCategory(category: CleanableCategory, select: Boolean) {
        _foundItems.value = _foundItems.value.map {
            if (it.category == category) it.copy(isSelected = select) else it
        }
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

    fun getCategorySummaries(): List<CategorySummary> {
        val items = _foundItems.value
        return CleanableCategory.values().map { cat ->
            val catItems = items.filter { it.category == cat }
            CategorySummary(
                category = cat,
                count = catItems.size,
                totalBytes = catItems.sumOf { it.sizeBytes },
                items = catItems
            )
        }
    }

    val totalCleanableBytes: Long
        get() = _foundItems.value.filter { it.isSelected }.sumOf { it.sizeBytes }

    fun clearCleanResult() {
        _cleanResult.value = null
        _scanState.value = ScanState.IDLE
    }
}
