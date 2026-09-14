package com.galaxya12.cleaner.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.R
import com.galaxya12.cleaner.model.CleanableCategory
import com.galaxya12.cleaner.model.ScanState
import com.galaxya12.cleaner.model.StorageInfo
import com.galaxya12.cleaner.permissions.PermissionManager
import com.galaxya12.cleaner.ui.components.ActionButton
import com.galaxya12.cleaner.ui.components.SamsungCard
import com.galaxya12.cleaner.ui.components.SectionHeader
import com.galaxya12.cleaner.ui.components.StorageCircularIndicator

@Composable
fun MainScreen(
    viewModel: CleanerViewModel,
    modifier: Modifier = Modifier,
    onNavigateToFiles: () -> Unit = {},
    onNavigateToStorageAccess: () -> Unit = {},
    onGrantStoragePermission: () -> Unit = {}
) {
    val context = LocalContext.current
    val storageInfo by viewModel.storageInfo.collectAsState()
    val scanState by viewModel.scanState.collectAsState()
    val scanProgress by viewModel.scanProgress.collectAsState()
    val scanResult by viewModel.scanResult.collectAsState()
    val cleanResult by viewModel.cleanResult.collectAsState()
    val cleaningMessage by viewModel.cleaningMessage.collectAsState()
    val hasSafAccess by viewModel.hasSafAccess.collectAsState()
    val safFolderName by viewModel.safFolderName.collectAsState()
    val totalCleanableBytes = viewModel.totalCleanableBytes
    val summaries = viewModel.getCategorySummaries()

    var showCleanConfirmDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Samsung Galaxy A12 Storage Optimizer",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Storage Gauge Card (StatFs based, always independent of junk scan)
            item {
                SamsungCard(
                    modifier = Modifier.widthIn(max = 600.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        StorageCircularIndicator(storageInfo = storageInfo)

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Available",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = storageInfo.formattedFree,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Used",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = storageInfo.formattedUsed,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Total",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = storageInfo.formattedTotal,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Scan / Clean Status Card
            item {
                SamsungCard(
                    modifier = Modifier.widthIn(max = 600.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = if (scanState == ScanState.SCANNING) "SCANNING STORAGE" else "JUNK THAT CAN BE CLEANED",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        val displayMainSize = if (scanState == ScanState.SCANNING) {
                            scanProgress.formattedBytesFound
                        } else {
                            StorageInfo.formatBytes(totalCleanableBytes)
                        }

                        Text(
                            text = displayMainSize,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )

                        // Status subtitle message
                        if (scanState == ScanState.COMPLETE) {
                            Spacer(modifier = Modifier.height(4.dp))
                            if (totalCleanableBytes == 0L) {
                                if (scanResult.requiresPermission) {
                                    Text(
                                        text = "Additional storage locations need permission",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                } else {
                                    Text(
                                        text = "0 B — No safe junk found",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                Text(
                                    text = "${scanResult.fileCount} safe items ready to clean",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Scanning Progress Bar & Stages
                        if (scanState == ScanState.SCANNING) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = scanProgress.stageName,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            ActionButton(
                                text = "CANCEL SCAN",
                                icon = Icons.Default.Close,
                                onClick = { viewModel.cancelScan() },
                                isPrimary = false,
                                modifier = Modifier.fillMaxWidth(),
                                testTag = "btn_cancel_scan"
                            )
                        } else if (scanState == ScanState.CLEANING) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Cleaning: $cleaningMessage",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            )
                        } else {
                            // Primary Action Buttons
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ActionButton(
                                    text = stringResource(R.string.scan_now),
                                    icon = Icons.Default.Search,
                                    onClick = { viewModel.startScan() },
                                    isPrimary = totalCleanableBytes == 0L,
                                    enabled = true,
                                    modifier = Modifier.weight(1f),
                                    testTag = "scan_now_button"
                                )

                                ActionButton(
                                    text = stringResource(R.string.clean_now),
                                    icon = Icons.Default.CleaningServices,
                                    onClick = { showCleanConfirmDialog = true },
                                    isPrimary = totalCleanableBytes > 0L,
                                    enabled = totalCleanableBytes > 0L,
                                    modifier = Modifier.weight(1f),
                                    testTag = "clean_now_button"
                                )
                            }
                        }

                        // Inspection stats footer when scan complete
                        if (scanState == ScanState.COMPLETE && scanResult.scannedFiles > 0) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Scanned: ${scanResult.formattedScanned} (${scanResult.scannedFiles} files)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (scanResult.inaccessibleLocations > 0) {
                                    Text(
                                        text = "${scanResult.inaccessibleLocations} restricted",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Permission Request Banner if All Files Access is needed
            if (!PermissionManager.hasAllFilesAccess()) {
                item {
                    SamsungCard(
                        modifier = Modifier.widthIn(max = 600.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "All Files Access Required",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "To find junk in Downloads, thumbnail caches, and leftover APKs on Android 11/12, grant storage access.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { PermissionManager.openAllFilesAccessSettings(context) },
                                modifier = Modifier.testTag("btn_grant_all_files_access")
                            ) {
                                Text("ALLOW")
                            }
                        }
                    }
                }
            } else if (scanResult.requiresPermission || !hasSafAccess) {
                item {
                    SamsungCard(
                        modifier = Modifier.widthIn(max = 600.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Additional Storage Folders",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Select custom folders or SD card for deep scanning.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = onNavigateToStorageAccess,
                                modifier = Modifier.testTag("btn_grant_storage_access")
                            ) {
                                Text("CUSTOM")
                            }
                        }
                    }
                }
            }

            // Zero Junk Verification Banner (DEBUG & Testing)
            if (scanState == ScanState.COMPLETE && totalCleanableBytes == 0L && BuildConfig.DEBUG) {
                item {
                    SamsungCard(
                        modifier = Modifier.widthIn(max = 600.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Storage Clean (0 B Found)",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "All accessible locations were scanned. To verify real scanning, detection, and deletion in this test environment, create 10 MB test cache files below:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            ActionButton(
                                text = "CREATE 10 MB TEST FILES (VERIFY SCANNER)",
                                icon = Icons.Default.Refresh,
                                onClick = { viewModel.createDeveloperTestJunk() },
                                isPrimary = false,
                                modifier = Modifier.fillMaxWidth(),
                                testTag = "btn_create_test_junk_main"
                            )
                        }
                    }
                }
            }

            // SECTION 1: SAFE TO CLEAN
            item {
                Column(modifier = Modifier.widthIn(max = 600.dp)) {
                    SectionHeader(title = "SAFE TO CLEAN")

                    summaries.forEach { summary ->
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(18.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = summary.name,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${summary.fileCount} files • ${summary.formattedSize}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (summary.fileCount > 0) {
                                    val allSelected = summary.items.all { it.isSelected }
                                    Checkbox(
                                        checked = allSelected,
                                        onCheckedChange = { isChecked ->
                                            viewModel.selectAllCategory(summary.category, isChecked)
                                        },
                                        modifier = Modifier.testTag("checkbox_${summary.category.name}")
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 2: NEEDS PERMISSION
            item {
                Column(modifier = Modifier.widthIn(max = 600.dp)) {
                    SectionHeader(title = "NEEDS PERMISSION")

                    SamsungCard {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Downloads item
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Downloads & MediaStore",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (PermissionManager.hasStoragePermission(context)) "Permission granted ✓" else "Read permission required",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (PermissionManager.hasStoragePermission(context)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Selected SAF Folders item
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigateToStorageAccess() },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Selected folders (Storage Access Framework)",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (hasSafAccess) "Access granted: ${safFolderName ?: "Folder"} ✓" else "Tap to choose folder",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (hasSafAccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 3: PROTECTED BY ANDROID
            item {
                Column(modifier = Modifier.widthIn(max = 600.dp)) {
                    SectionHeader(title = "PROTECTED BY ANDROID")

                    SamsungCard {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Android protects application data folders",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "• Other app private caches (/data/user/0/*)\n• Android/data and Android/obb\n\nAndroid Scoped Storage prohibits third-party cleaners from accessing or silently clearing other apps' private cache files.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            ActionButton(
                                text = stringResource(R.string.btn_manage_apps),
                                icon = Icons.Default.Settings,
                                onClick = { PermissionManager.openManageApplications(context) },
                                isPrimary = false,
                                modifier = Modifier.fillMaxWidth(),
                                testTag = "main_btn_manage_apps"
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Clean Confirmation Dialog
    if (showCleanConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showCleanConfirmDialog = false },
            title = {
                Text(
                    text = "Clean Selected Files?",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "This will permanently remove ${StorageInfo.formatBytes(totalCleanableBytes)} of verified safe junk and temporary cache files.",
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCleanConfirmDialog = false
                        viewModel.startClean()
                    },
                    modifier = Modifier.testTag("confirm_clean_button")
                ) {
                    Text("Clean")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCleanConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clean Completion Dialog
    cleanResult?.let { result ->
        AlertDialog(
            onDismissRequest = { viewModel.clearCleanResult() },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.clean_complete_title),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.clean_freed_fmt, result.formattedFreed),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(
                            R.string.clean_details_fmt,
                            result.deletedCount,
                            result.failedCount,
                            result.formattedFreed
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.clearCleanResult() },
                    modifier = Modifier.testTag("clean_done_button")
                ) {
                    Text("Done")
                }
            }
        )
    }
}
