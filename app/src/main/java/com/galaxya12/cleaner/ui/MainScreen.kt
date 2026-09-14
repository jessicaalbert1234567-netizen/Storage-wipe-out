package com.galaxya12.cleaner.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
    onNavigateToFiles: () -> Unit = {}
) {
    val context = LocalContext.current
    val storageInfo by viewModel.storageInfo.collectAsState()
    val scanState by viewModel.scanState.collectAsState()
    val scanProgress by viewModel.scanProgress.collectAsState()
    val cleanResult by viewModel.cleanResult.collectAsState()
    val cleaningMessage by viewModel.cleaningMessage.collectAsState()
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
                // Header Area with Title
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

            // Storage Gauge Card
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

            // Cleanable Junk Summary & Actions Card
            item {
                SamsungCard(
                    modifier = Modifier.widthIn(max = 600.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(R.string.junk_cleanable_title),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = StorageInfo.formatBytes(totalCleanableBytes),
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )

                        // Scan Progress indicator if scanning
                        if (scanState == ScanState.SCANNING) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Scanning: ${scanProgress.stageName}...",
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
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ActionButton(
                                text = if (scanState == ScanState.SCANNING) "SCANNING..." else stringResource(R.string.scan_now),
                                icon = Icons.Default.Search,
                                onClick = { viewModel.startScan() },
                                isPrimary = false,
                                enabled = scanState != ScanState.SCANNING && scanState != ScanState.CLEANING,
                                modifier = Modifier.weight(1f),
                                testTag = "scan_now_button"
                            )

                            ActionButton(
                                text = if (scanState == ScanState.CLEANING) "CLEANING..." else stringResource(R.string.clean_now),
                                icon = Icons.Default.CleaningServices,
                                onClick = { showCleanConfirmDialog = true },
                                isPrimary = true,
                                enabled = totalCleanableBytes > 0 && scanState != ScanState.SCANNING && scanState != ScanState.CLEANING,
                                modifier = Modifier.weight(1f),
                                testTag = "clean_now_button"
                            )
                        }
                    }
                }
            }

            // SECTION 1: CLEANABLE BY THIS APP
            item {
                Column(modifier = Modifier.widthIn(max = 600.dp)) {
                    SectionHeader(title = stringResource(R.string.section_cleanable))

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
                                        text = summary.category.displayName,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${summary.count} files • ${summary.formattedSize}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (summary.count > 0) {
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

            // SECTION 2: MANAGE APP CACHE (Security-compliant Android architecture)
            item {
                Column(modifier = Modifier.widthIn(max = 600.dp)) {
                    SectionHeader(title = stringResource(R.string.section_manage_cache))

                    SamsungCard {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = stringResource(R.string.app_cache_explanation),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 20.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ActionButton(
                                    text = stringResource(R.string.btn_manage_apps),
                                    icon = Icons.Default.Settings,
                                    onClick = { PermissionManager.openManageApplications(context) },
                                    isPrimary = false,
                                    modifier = Modifier.weight(1f),
                                    testTag = "btn_manage_apps"
                                )

                                ActionButton(
                                    text = "STORAGE",
                                    icon = Icons.Default.FolderOpen,
                                    onClick = { PermissionManager.openInternalStorageSettings(context) },
                                    isPrimary = false,
                                    modifier = Modifier.weight(1f),
                                    testTag = "btn_open_storage"
                                )
                            }
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
                    text = "This will permanently remove ${StorageInfo.formatBytes(totalCleanableBytes)} of safe junk and temporary cache files.",
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
