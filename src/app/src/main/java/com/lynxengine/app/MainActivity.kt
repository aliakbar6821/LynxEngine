package com.lynxengine.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lynxengine.app.ui.screens.HomeScreen
import com.lynxengine.app.ui.screens.SettingsScreen
import com.lynxengine.app.ui.screens.ToolsScreen
import com.lynxengine.app.ui.theme.LynxEngineTheme
import com.lynxengine.app.viewmodel.LynxViewModel
import kotlinx.coroutines.delay

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

// ── 5-second integration banner ───────────────────────────────────────────────
@Composable
fun IntegrationBanner(visible: Boolean) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1B5E20),
                tonalElevation = 8.dp,
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF69F0AE),
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        "LynxEngine Integrated",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LynxApp(viewModel: LynxViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Show banner for exactly 5 seconds once integration check completes
    var showBanner by remember { mutableStateOf(false) }
    var bannerHandled by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.integrationChecked) {
        if (!uiState.integrationChecked) return@LaunchedEffect
        if (bannerHandled) return@LaunchedEffect
        bannerHandled = true

        if (uiState.isFrameworkIntegrated) {
            showBanner = true
            delay(5000L)
            showBanner = false
        }
        // Not integrated -> warning dialog shown below
    }

    // Generic toasts for load/refresh/etc actions
    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.dismissToast()
        }
    }

    var selectedTab by remember { mutableStateOf(Tab.HOME) }

    // Not-integrated warning dialog
    if (uiState.showIntegrationWarning) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissIntegrationWarning() },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    "LynxEngine Not Integrated",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    "No PIF data found in Settings.Secure.\n\n" +
                    "Load your pif.json via cmd first, then reopen the app:\n" +
                    "  settings put secure lynx_pif_data <json>\n\n" +
                    "Tools and Settings will unlock once PIF data is detected.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissIntegrationWarning() }) {
                    Text("OK")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Lynx Engine") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                // Banner slides in below top bar, auto-hides after 5s
                IntegrationBanner(visible = showBanner)
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                // Tools + Settings only visible when hook is confirmed active
                val availableTabs = if (uiState.isFrameworkIntegrated) {
                    Tab.entries
                } else {
                    listOf(Tab.HOME)
                }

                availableTabs.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                Tab.HOME -> HomeScreen(
                    uiState = uiState,
                    onRefresh = viewModel::refresh,
                    isIntegrated = uiState.isFrameworkIntegrated
                )
                Tab.TOOLS -> {
                    if (uiState.isFrameworkIntegrated) {
                        ToolsScreen(
                            uiState = uiState,
                            onLoadPif = viewModel::loadPif,
                            onLoadKeybox = viewModel::loadKeybox,
                            onRefresh = viewModel::refresh,
                            onClearAll = viewModel::clearAll,
                            onAutoUpdate = viewModel::performAutoUpdate,
                            onExportPif = viewModel::exportPifToUri,
                            onExportKeybox = viewModel::exportKeyboxToUri,
                            onShowPrintPif = viewModel::showPrintPif,
                            onDismissPrintPif = viewModel::dismissPrintPifDialog,
                            onAddHideDevApp = viewModel::addHideDevApp,
                            onRemoveHideDevApp = viewModel::removeHideDevApp
                        )
                    }
                }
                Tab.SETTINGS -> {
                    if (uiState.isFrameworkIntegrated) {
                        SettingsScreen(
                            uiState = uiState,
                            onToggleAutoUpdate = viewModel::setAutoUpdateEnabled,
                            onSetInterval = viewModel::setAutoUpdateInterval
                        )
                    }
                }
            }
        }
    }
}
