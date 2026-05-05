package com.lynxengine.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.lynxengine.app.ui.screens.HomeScreen
import com.lynxengine.app.ui.screens.SettingsScreen
import com.lynxengine.app.ui.screens.ToolsScreen
import com.lynxengine.app.ui.theme.LynxEngineTheme
import com.lynxengine.app.viewmodel.LynxViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: LynxViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LynxEngineTheme {
                LynxApp(viewModel)
            }
        }
    }
}

private enum class Tab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    TOOLS("Tools", Icons.Default.Build),
    SETTINGS("Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LynxApp(viewModel: LynxViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.recheck()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var toastHandled by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.integrationChecked) {
        if (!uiState.integrationChecked) return@LaunchedEffect
        if (toastHandled) return@LaunchedEffect
        toastHandled = true
        if (uiState.isFrameworkIntegrated) {
            Toast.makeText(context, "LynxEngine Integrated", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.dismissToast()
        }
    }

    var selectedTab by remember { mutableStateOf(Tab.HOME) }

    LaunchedEffect(uiState.isFrameworkIntegrated) {
        if (!uiState.isFrameworkIntegrated) selectedTab = Tab.HOME
    }

    if (uiState.showIntegrationWarning) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissIntegrationWarning() },
            icon = {
                Icon(Icons.Default.Warning, null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp))
            },
            title = {
                Text("LynxEngine Not Integrated",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface)
            },
            text = {
                Text(
                    "No PIF or Keybox data found in Settings.Secure.\n\n" +
                    "Load both files via cmd, then come back to the app:\n" +
                    "  settings put secure lynx_pif_data <json>\n" +
                    "  settings put secure lynx_keybox_data <xml>\n\n" +
                    "Tools and Settings will unlock automatically.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissIntegrationWarning() }) { Text("OK") }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Lynx Engine") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                val availableTabs = if (uiState.isFrameworkIntegrated) Tab.entries else listOf(Tab.HOME)
                availableTabs.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick  = { selectedTab = tab },
                        icon     = { Icon(tab.icon, tab.label) },
                        label    = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (selectedTab) {
                Tab.HOME -> HomeScreen(
                    uiState      = uiState,
                    onRefresh    = viewModel::refresh,
                    isIntegrated = uiState.isFrameworkIntegrated
                )
                Tab.TOOLS -> {
                    if (uiState.isFrameworkIntegrated) {
                        ToolsScreen(
                            uiState              = uiState,
                            onLoadPif            = viewModel::loadPif,
                            onLoadKeybox         = viewModel::loadKeybox,
                            onRefresh            = viewModel::refresh,
                            onClearAll           = viewModel::clearAll,
                            onAutoUpdate         = viewModel::performAutoUpdate,
                            onExportPif          = viewModel::exportPifToUri,
                            onExportKeybox       = viewModel::exportKeyboxToUri,
                            onShowPrintPif       = viewModel::showPrintPif,
                            onDismissPrintPif    = viewModel::dismissPrintPifDialog,
                            onAddHideDevApps     = viewModel::addHideDevApps,
                            onRemoveHideDevApp   = viewModel::removeHideDevApp,
                            onRefreshHideDevApps = viewModel::forceStopHideDevApps
                        )
                    }
                }
                Tab.SETTINGS -> {
                    if (uiState.isFrameworkIntegrated) {
                        SettingsScreen(
                            uiState           = uiState,
                            onToggleAutoUpdate = viewModel::setAutoUpdateEnabled,
                            onSetInterval      = viewModel::setAutoUpdateInterval
                        )
                    }
                }
            }
        }
    }
}
