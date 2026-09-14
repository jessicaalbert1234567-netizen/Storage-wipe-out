package com.galaxya12.cleaner

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.galaxya12.cleaner.permissions.PermissionManager
import com.galaxya12.cleaner.ui.AboutScreen
import com.galaxya12.cleaner.ui.CleanerViewModel
import com.galaxya12.cleaner.ui.FilesScreen
import com.galaxya12.cleaner.ui.MainScreen
import com.galaxya12.cleaner.ui.PrivacyScreen
import com.galaxya12.cleaner.ui.SettingsScreen
import com.galaxya12.cleaner.ui.theme.GalaxyCleanerTheme

enum class CleanerNavDestination {
    DASHBOARD,
    FILES,
    SETTINGS,
    ABOUT,
    PRIVACY
}

class MainActivity : ComponentActivity() {

    private val viewModel: CleanerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntent(intent)

        setContent {
            GalaxyCleanerTheme {
                CleanerApp(
                    viewModel = viewModel,
                    onShortcutHandled = { handleIntent(intent) }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val action = intent?.getStringExtra("action")
        when (action) {
            "SCAN_STORAGE", "SCAN" -> {
                viewModel.startScan()
            }
            "CLEAN_JUNK", "CLEAN" -> {
                viewModel.startScan()
            }
        }
    }
}

@Composable
fun CleanerApp(
    viewModel: CleanerViewModel,
    onShortcutHandled: () -> Unit = {}
) {
    val context = LocalContext.current
    var currentDestination by remember { mutableStateOf(CleanerNavDestination.DASHBOARD) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        viewModel.refreshStorageInfo()
        viewModel.startScan()
    }

    LaunchedEffect(Unit) {
        if (!PermissionManager.hasStoragePermission(context)) {
            permissionLauncher.launch(PermissionManager.getRequiredStoragePermissions())
        } else {
            viewModel.startScan()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (currentDestination in listOf(
                    CleanerNavDestination.DASHBOARD,
                    CleanerNavDestination.FILES,
                    CleanerNavDestination.SETTINGS
                )
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    NavigationBarItem(
                        selected = currentDestination == CleanerNavDestination.DASHBOARD,
                        onClick = { currentDestination = CleanerNavDestination.DASHBOARD },
                        icon = { Icon(Icons.Default.CleaningServices, contentDescription = "Cleaner") },
                        label = { Text("Cleaner") },
                        modifier = Modifier.testTag("nav_cleaner")
                    )
                    NavigationBarItem(
                        selected = currentDestination == CleanerNavDestination.FILES,
                        onClick = { currentDestination = CleanerNavDestination.FILES },
                        icon = { Icon(Icons.Default.Folder, contentDescription = "Files") },
                        label = { Text("Files") },
                        modifier = Modifier.testTag("nav_files")
                    )
                    NavigationBarItem(
                        selected = currentDestination == CleanerNavDestination.SETTINGS,
                        onClick = { currentDestination = CleanerNavDestination.SETTINGS },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") },
                        modifier = Modifier.testTag("nav_settings")
                    )
                }
            }
        }
    ) { innerPadding ->
        when (currentDestination) {
            CleanerNavDestination.DASHBOARD -> MainScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding),
                onNavigateToFiles = { currentDestination = CleanerNavDestination.FILES }
            )
            CleanerNavDestination.FILES -> FilesScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
            CleanerNavDestination.SETTINGS -> SettingsScreen(
                onNavigateToAbout = { currentDestination = CleanerNavDestination.ABOUT },
                onNavigateToPrivacy = { currentDestination = CleanerNavDestination.PRIVACY },
                modifier = Modifier.padding(innerPadding)
            )
            CleanerNavDestination.ABOUT -> AboutScreen(
                onBack = { currentDestination = CleanerNavDestination.SETTINGS }
            )
            CleanerNavDestination.PRIVACY -> PrivacyScreen(
                onBack = { currentDestination = CleanerNavDestination.SETTINGS }
            )
        }
    }
}
