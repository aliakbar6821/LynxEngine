package com.lynxengine.app.ui.screens

import android.app.Activity
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lynxengine.app.ui.components.ToolItem
import com.lynxengine.app.ui.theme.LynxGreen
import com.lynxengine.app.ui.theme.LynxRed
import com.lynxengine.app.viewmodel.LynxUiState
import java.io.File
import java.io.FileOutputStream

// ── Navigation within Tools ───────────────────────────────────────────────────
private enum class ToolsPage { MAIN, PLAY_INTEGRITY, HIDE_DEV }

@Composable
fun ToolsScreen(
    uiState: LynxUiState,
    onLoadPif: (File) -> Unit,
    onLoadKeybox: (File) -> Unit,
    onRefresh: () -> Unit,
    onClearAll: () -> Unit,
    onAutoUpdate: () -> Unit,
    onExportPif: (Uri) -> Unit,
    onExportKeybox: (Uri) -> Unit,
    onShowPrintPif: () -> Unit,
    onDismissPrintPif: () -> Unit,
    onAddHideDevApps: (Set<String>) -> Unit,
    onRemoveHideDevApp: (String) -> Unit,
    onRefreshHideDevApps: () -> Unit
) {
    var page by remember { mutableStateOf(ToolsPage.MAIN) }

    when (page) {
        ToolsPage.MAIN -> ToolsMainPage(
            uiState = uiState,
            onOpenPlayIntegrity = { page = ToolsPage.PLAY_INTEGRITY },
            onOpenHideDev = { page = ToolsPage.HIDE_DEV }
        )
        ToolsPage.PLAY_INTEGRITY -> PlayIntegrityPage(
            uiState = uiState,
            onBack = { page = ToolsPage.MAIN },
            onLoadPif = onLoadPif,
            onLoadKeybox = onLoadKeybox,
            onRefresh = onRefresh,
            onClearAll = onClearAll,
            onAutoUpdate = onAutoUpdate,
            onExportPif = onExportPif,
            onExportKeybox = onExportKeybox,
            onShowPrintPif = onShowPrintPif,
            onDismissPrintPif = onDismissPrintPif
        )
        ToolsPage.HIDE_DEV -> HideDevPage(
            uiState = uiState,
            onBack = { page = ToolsPage.MAIN },
            onAddHideDevApps = onAddHideDevApps,
            onRemoveHideDevApp = onRemoveHideDevApp,
            onRefreshHideDevApps = onRefreshHideDevApps
        )
    }
}

// ── Main page — 2 option cards ────────────────────────────────────────────────
@Composable
private fun ToolsMainPage(
    uiState: LynxUiState,
    onOpenPlayIntegrity: () -> Unit,
    onOpenHideDev: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Card 1 — Play Integrity Fix
        Card(
            modifier = Modifier.fillMaxWidth().clickable { onOpenPlayIntegrity() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(Icons.Default.Security, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Play Integrity Fix", fontWeight = FontWeight.Bold, fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(2.dp))
                    val pifStatus = if (uiState.isPifLoaded && uiState.isKeyboxLoaded) "PIF & Keybox loaded"
                        else if (uiState.isPifLoaded) "PIF loaded, Keybox missing"
                        else if (uiState.isKeyboxLoaded) "Keybox loaded, PIF missing"
                        else "Not configured"
                    Text(pifStatus, fontSize = 12.sp,
                        color = if (uiState.isPifLoaded && uiState.isKeyboxLoaded) LynxGreen
                                else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Default.ChevronRight, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // Card 2 — Hide Developer Options
        Card(
            modifier = Modifier.fillMaxWidth().clickable { onOpenHideDev() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(Icons.Default.VisibilityOff, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Hide Developer Options", fontWeight = FontWeight.Bold, fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (uiState.hideDeveloperApps.isEmpty()) "No apps configured"
                        else "${uiState.hideDeveloperApps.size} app(s) configured",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(Icons.Default.ChevronRight, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ── Play Integrity sub-screen ─────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayIntegrityPage(
    uiState: LynxUiState,
    onBack: () -> Unit,
    onLoadPif: (File) -> Unit,
    onLoadKeybox: (File) -> Unit,
    onRefresh: () -> Unit,
    onClearAll: () -> Unit,
    onAutoUpdate: () -> Unit,
    onExportPif: (Uri) -> Unit,
    onExportKeybox: (Uri) -> Unit,
    onShowPrintPif: () -> Unit,
    onDismissPrintPif: () -> Unit
) {
    val context = LocalContext.current
    var showClearDialog by remember { mutableStateOf(false) }

    val pifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val tmp = copyUriToTemp(context, uri, "pif_tmp.json")
                if (tmp != null) onLoadPif(tmp)
            }
        }
    }

    val keyboxLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val tmp = copyUriToTemp(context, uri, "keybox_tmp.xml")
                if (tmp != null) onLoadKeybox(tmp)
            }
        }
    }

    val exportPifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> uri?.let(onExportPif) }
    val exportKeyboxLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/xml")) { uri -> uri?.let(onExportKeybox) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Lynx Engine", fontWeight = FontWeight.Bold) },
            text = { Text("Remove all PIF & Keybox data?") },
            confirmButton = { TextButton(onClick = { showClearDialog = false; onClearAll() }) { Text("Clear", color = LynxRed) } },
            dismissButton = { TextButton(onClick = { showClearDialog = false }) { Text("Cancel") } }
        )
    }

    if (uiState.showPrintPifDialog) {
        AlertDialog(
            onDismissRequest = onDismissPrintPif,
            title = { Text("Current PIF", fontWeight = FontWeight.Bold) },
            text = { Column(Modifier.verticalScroll(rememberScrollState())) { Text(uiState.currentPifDetails, fontSize = 12.sp) } },
            confirmButton = { TextButton(onClick = onDismissPrintPif) { Text("OK") } }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Back button row
            TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) {
                Icon(Icons.Default.ArrowBack, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Back")
            }

            // Auto Update card (when enabled)
            if (uiState.autoUpdateEnabled) {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Auto Update", fontWeight = FontWeight.Bold, fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(4.dp))
                        Text("Last: ${uiState.lastAutoUpdateFormatted}", fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(10.dp))
                        Button(onClick = onAutoUpdate, modifier = Modifier.fillMaxWidth(), enabled = !uiState.isLoading) {
                            Icon(Icons.Default.Update, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(if (uiState.isLoading) "Updating..." else "Update Now")
                        }
                        Spacer(Modifier.height(8.dp))
                        ToolItem(Icons.Default.FileDownload, "Export PIF", "Save applied pif.json",
                            enabled = uiState.isPifLoaded, onClick = { exportPifLauncher.launch("pif.json") })
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            modifier = Modifier.padding(start = 58.dp))
                        ToolItem(Icons.Default.FileDownload, "Export Keybox", "Save applied keybox.xml",
                            enabled = uiState.isKeyboxLoaded, onClick = { exportKeyboxLauncher.launch("keybox.xml") })
                    }
                }
            }

            // Custom Spoofing card (when auto update disabled)
            if (!uiState.autoUpdateEnabled) {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Custom Spoofing", fontWeight = FontWeight.Bold, fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                        ToolItem(Icons.Default.Key, "Select Keybox",
                            if (uiState.isKeyboxLoaded) "Keybox loaded ✓" else "Load keybox.xml",
                            iconTint = if (uiState.isKeyboxLoaded) LynxGreen else MaterialTheme.colorScheme.primary,
                            onClick = { keyboxLauncher.launch(Intent(Intent.ACTION_GET_CONTENT).apply {
                                type = "*/*"; addCategory(Intent.CATEGORY_OPENABLE) }) })
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            modifier = Modifier.padding(start = 58.dp))
                        ToolItem(Icons.Default.UploadFile, "Select PIF",
                            if (uiState.isPifLoaded) "PIF loaded ✓" else "Load pif.json",
                            iconTint = if (uiState.isPifLoaded) LynxGreen else MaterialTheme.colorScheme.primary,
                            onClick = { pifLauncher.launch(Intent(Intent.ACTION_GET_CONTENT).apply {
                                type = "*/*"; addCategory(Intent.CATEGORY_OPENABLE) }) })
                    }
                }
            }

            // Actions card
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Actions", fontWeight = FontWeight.Bold, fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    ToolItem(Icons.Default.Refresh, "Refresh", "Re-read status & restart GMS", onClick = onRefresh)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        modifier = Modifier.padding(start = 58.dp))
                    ToolItem(Icons.Default.Terminal, "Print PIF", "Show current PIF details", onClick = onShowPrintPif)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        modifier = Modifier.padding(start = 58.dp))
                    ToolItem(Icons.Default.DeleteForever, "Clear Engine", "Remove all data",
                        iconTint = LynxRed, onClick = { showClearDialog = true })
                }
            }
        }

        if (uiState.isLoading) {
            Surface(modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.8f)) {
                Box(contentAlignment = Alignment.Center) {
                    Card(shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(16.dp))
                            Text("Processing...", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

// ── Hide Dev sub-screen ───────────────────────────────────────────────────────
@Composable
private fun HideDevPage(
    uiState: LynxUiState,
    onBack: () -> Unit,
    onAddHideDevApps: (Set<String>) -> Unit,
    onRemoveHideDevApp: (String) -> Unit,
    onRefreshHideDevApps: () -> Unit
) {
    var showAppPicker by remember { mutableStateOf(false) }

    if (showAppPicker) {
        InstalledAppPicker(
            alreadyAdded = uiState.hideDeveloperApps,
            onConfirm = { selected ->
                showAppPicker = false
                if (selected.isNotEmpty()) onAddHideDevApps(selected.toSet())
            },
            onDismiss = { showAppPicker = false }
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            // Back
            TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) {
                Icon(Icons.Default.ArrowBack, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Back")
            }

            Spacer(Modifier.height(8.dp))

            Text("Hide Developer Options", fontWeight = FontWeight.Bold, fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface)

            Spacer(Modifier.height(8.dp))

            // Description
            Surface(shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)) {
                Text(
                    "Apps added here will see ADB debugging and Developer Options as disabled, " +
                    "even when they are enabled on the device. Useful for banking and integrity-sensitive apps.",
                    modifier = Modifier.padding(12.dp),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 18.sp
                )
            }

            Spacer(Modifier.height(16.dp))

            // Add App button
            Button(
                onClick = { showAppPicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Add App")
            }

            Spacer(Modifier.height(8.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            Spacer(Modifier.height(8.dp))

            // App list — fills remaining space
            if (uiState.hideDeveloperApps.isEmpty()) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text("No apps added yet", fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(uiState.hideDeveloperApps.sorted()) { pkg ->
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Android, null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(pkg, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f), maxLines = 1,
                                overflow = TextOverflow.Ellipsis)
                            IconButton(
                                onClick = { onRemoveHideDevApp(pkg) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Delete, "Remove",
                                    tint = LynxRed, modifier = Modifier.size(20.dp))
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Bottom Refresh button — force-stops all apps in list
            Button(
                onClick = onRefreshHideDevApps,
                modifier = Modifier.align(Alignment.CenterHorizontally).fillMaxWidth(0.6f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Refresh")
            }
        }
    }
}

// ── Installed App Picker — full-screen with multi-select ─────────────────────
@Composable

// ── Installed App Picker — full-screen with multi-select ─────────────────────
@Composable
private fun InstalledAppPicker(
    alreadyAdded: Set<String>,
    onConfirm: (Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // BUG FIX: must be inside remember{} — without it the list is recreated
    // on every recomposition, wiping selections the instant a checkbox is tapped.
    val selected = remember { androidx.compose.runtime.mutableStateListOf<String>() }

    var searchQuery by remember { mutableStateOf("") }
    var showSystemApps by remember { mutableStateOf(false) }

    // All user apps (non-system) — loaded once
    val userApps = remember {
        context.packageManager
            .getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.packageName != context.packageName }
            .filter { (it.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0 }
            .sortedBy { context.packageManager.getApplicationLabel(it).toString().lowercase() }
    }

    // All apps including system — loaded lazily when toggle is first switched on
    val allApps = remember {
        context.packageManager
            .getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.packageName != context.packageName }
            .sortedBy { context.packageManager.getApplicationLabel(it).toString().lowercase() }
    }

    // Active list switches based on toggle
    val baseList = if (showSystemApps) allApps else userApps

    // Filter by search query
    val filtered = remember(searchQuery, showSystemApps) {
        if (searchQuery.isBlank()) baseList
        else baseList.filter {
            val label = context.packageManager.getApplicationLabel(it).toString()
            label.contains(searchQuery, ignoreCase = true) ||
            it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

            // ── Top bar ───────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.ArrowBack, null)
                }
                Text("Select Apps", fontWeight = FontWeight.Bold, fontSize = 17.sp,
                    modifier = Modifier.weight(1f))
                TextButton(
                    onClick = { onConfirm(selected.toSet()) },
                    enabled = selected.isNotEmpty()
                ) {
                    Text("Add (${selected.size})")
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Search bar ────────────────────────────────────────────────
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search apps...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, null)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(8.dp))

            // ── Stats row + system apps toggle ────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "${filtered.size} apps  •  ${selected.size} selected",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("System apps", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Switch(
                        checked = showSystemApps,
                        onCheckedChange = { showSystemApps = it },
                        modifier = Modifier.height(24.dp)
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // ── App list ──────────────────────────────────────────────────
            LazyColumn {
                items(filtered, key = { it.packageName }) { appInfo ->
                    val pkg = appInfo.packageName
                    val label = context.packageManager.getApplicationLabel(appInfo).toString()
                    val isChecked = pkg in selected
                    val alreadyIn = pkg in alreadyAdded

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !alreadyIn) {
                                // Directly mutate the SnapshotStateList — triggers recomposition correctly
                                if (isChecked) selected.remove(pkg) else selected.add(pkg)
                            }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Checkbox(
                            checked = isChecked || alreadyIn,
                            onCheckedChange = { checked ->
                                if (!alreadyIn) {
                                    if (checked) selected.add(pkg) else selected.remove(pkg)
                                }
                            },
                            enabled = !alreadyIn
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                                color = if (alreadyIn) MaterialTheme.colorScheme.onSurfaceVariant
                                        else MaterialTheme.colorScheme.onSurface)
                            Text(pkg, fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (alreadyIn) {
                            Text("Added", fontSize = 11.sp, color = LynxGreen)
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                }
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────
private fun copyUriToTemp(context: android.content.Context, uri: Uri, name: String): File? = runCatching {
    val tmp = File(context.cacheDir, name)
    context.contentResolver.openInputStream(uri)!!.use { input ->
        FileOutputStream(tmp).use { output -> input.copyTo(output) }
    }
    tmp
}.getOrNull()

