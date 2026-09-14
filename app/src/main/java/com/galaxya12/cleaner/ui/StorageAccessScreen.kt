package com.galaxya12.cleaner.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.galaxya12.cleaner.permissions.PermissionManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.galaxya12.cleaner.ui.components.ActionButton
import com.galaxya12.cleaner.ui.components.SamsungCard
import com.galaxya12.cleaner.ui.components.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageAccessScreen(
    viewModel: CleanerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasSafAccess by viewModel.hasSafAccess.collectAsState()
    val safFolderName by viewModel.safFolderName.collectAsState()

    val openDocumentTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            viewModel.saveSafFolder(uri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Storage Access",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("storage_access_btn_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Column(modifier = Modifier.widthIn(max = 600.dp)) {
                    SamsungCard {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Android Scoped Storage Policy",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Android limits access to some shared folders. You can optionally select a folder to allow Galaxy A12 Cleaner to scan it.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }

            item {
                val context = LocalContext.current
                val hasAllFiles = PermissionManager.hasAllFilesAccess()

                Column(modifier = Modifier.widthIn(max = 600.dp)) {
                    SectionHeader(title = "DEVICE ALL FILES ACCESS (ANDROID 11/12+)")

                    SamsungCard {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (hasAllFiles) Icons.Default.CheckCircle else Icons.Default.Security,
                                    contentDescription = null,
                                    tint = if (hasAllFiles) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = if (hasAllFiles) "All Files Access Active ✓" else "All Files Access Required",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (hasAllFiles) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Allows Galaxy A12 Cleaner to scan Downloads, thumbnail caches, temporary junk, and leftover APKs across your device storage.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (!hasAllFiles) {
                                Spacer(modifier = Modifier.height(14.dp))
                                ActionButton(
                                    text = "GRANT ALL FILES ACCESS",
                                    icon = Icons.Default.Security,
                                    onClick = { PermissionManager.openAllFilesAccessSettings(context) },
                                    isPrimary = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    testTag = "saf_btn_grant_all_files"
                                )
                            }
                        }
                    }
                }
            }

            item {
                Column(modifier = Modifier.widthIn(max = 600.dp)) {
                    SectionHeader(title = "STORAGE ACCESS FRAMEWORK (SAF)")

                    SamsungCard {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            if (hasSafAccess) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Storage access granted ✓",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Selected folder:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = safFolderName ?: "Custom storage folder",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    ActionButton(
                                        text = "SCAN AGAIN",
                                        icon = Icons.Default.Refresh,
                                        onClick = {
                                            viewModel.startScan()
                                            onBack()
                                        },
                                        isPrimary = true,
                                        modifier = Modifier.weight(1f),
                                        testTag = "saf_btn_scan_again"
                                    )

                                    ActionButton(
                                        text = "REMOVE ACCESS",
                                        icon = Icons.Default.DeleteSweep,
                                        onClick = { viewModel.removeSafFolder() },
                                        isPrimary = false,
                                        modifier = Modifier.weight(1f),
                                        testTag = "saf_btn_remove_access"
                                    )
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "No custom folder selected",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Granting access to a folder (such as Downloads or Temp) allows safe inspection of temporary files and old APK packages.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                ActionButton(
                                    text = "SELECT FOLDER",
                                    icon = Icons.Default.FolderOpen,
                                    onClick = { openDocumentTreeLauncher.launch(null) },
                                    isPrimary = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    testTag = "saf_btn_select_folder"
                                )
                            }
                        }
                    }
                }
            }

            item {
                Column(modifier = Modifier.widthIn(max = 600.dp)) {
                    SectionHeader(title = "PROTECTED BY ANDROID")

                    SamsungCard {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "System and Private Data",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Android protects application data folders (Android/data, Android/obb, and private app caches). Even with SAF, third-party apps cannot access or silently purge these folders.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
