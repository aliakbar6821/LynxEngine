package com.lynxengine.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

private enum class Tab { HOME, TOOLS }

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

    var showSettings by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar    = {
            LynxTopBar(
                onRefresh       = viewModel::refresh,
                onOpenSettings  = { showSettings = true }
            )
        },
        bottomBar = {
            if (!showSettings) {
                LynxBottomNav(
                    selectedTab  = selectedTab,
                    integrated   = uiState.isFrameworkIntegrated,
                    onSelectTab  = { selectedTab = it }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        ) {
            when {
                showSettings -> SettingsScreen(
                    uiState            = uiState,
                    onToggleAutoUpdate = viewModel::setAutoUpdateEnabled,
                    onSetInterval      = viewModel::setAutoUpdateInterval,
                    onBack             = { showSettings = false }
                )
                selectedTab == Tab.HOME -> HomeScreen(
                    uiState      = uiState,
                    onRefresh    = viewModel::refresh,
                    isIntegrated = uiState.isFrameworkIntegrated
                )
                selectedTab == Tab.TOOLS && uiState.isFrameworkIntegrated -> ToolsScreen(
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
    }
}

// ── Custom Top Bar ────────────────────────────────────────────────────────────
@Composable
private fun LynxTopBar(
    onRefresh:      () -> Unit,
    onOpenSettings: () -> Unit
) {
    val surface  = MaterialTheme.colorScheme.surface
    val outline  = MaterialTheme.colorScheme.outlineVariant
    val primary  = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color    = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .statusBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Logo tile
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(surface)
                    .border(1.dp, outline, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Adb,
                    contentDescription = "LynxEngine Logo",
                    tint    = primary,
                    modifier = Modifier.size(26.dp)
                )
            }

            // App name pill — fills remaining space
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(surface)
                    .border(1.dp, outline, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = "Lynx Engine",
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 16.sp,
                    color      = onSurface
                )
            }

            // Refresh button tile
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(surface)
                    .border(1.dp, outline, RoundedCornerShape(14.dp))
                    .clickable { onRefresh() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint               = onSurface,
                    modifier           = Modifier.size(22.dp)
                )
            }

            // Settings button tile
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(surface)
                    .border(1.dp, outline, RoundedCornerShape(14.dp))
                    .clickable { onOpenSettings() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint               = onSurface,
                    modifier           = Modifier.size(22.dp)
                )
            }
        }
    }
}

// ── Custom Bottom Nav ─────────────────────────────────────────────────────────
@Composable
private fun LynxBottomNav(
    selectedTab: Tab,
    integrated:  Boolean,
    onSelectTab: (Tab) -> Unit
) {
    val surface  = MaterialTheme.colorScheme.surface
    val outline  = MaterialTheme.colorScheme.outlineVariant
    val primary  = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val disabled  = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color    = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val tabs = listOf(Tab.HOME to "Home", Tab.TOOLS to "Tools")

            tabs.forEach { (tab, label) ->
                val isSelected  = selectedTab == tab
                val isEnabled   = tab == Tab.HOME || integrated
                val borderColor = if (isSelected) primary else outline
                val textColor   = when {
                    isSelected -> primary
                    !isEnabled -> disabled
                    else       -> onSurface
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) primary.copy(alpha = 0.08f) else surface)
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = borderColor,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .then(
                            if (isEnabled) Modifier.clickable { onSelectTab(tab) }
                            else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text       = label,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize   = 14.sp,
                            color      = textColor
                        )
                        // Active indicator line
                        if (isSelected) {
                            Spacer(Modifier.height(3.dp))
                            Box(
                                modifier = Modifier
                                    .width(24.dp)
                                    .height(2.dp)
                                    .clip(RoundedCornerShape(1.dp))
                                    .background(primary)
                            )
                        }
                    }
                }
            }
        }
    }
}
