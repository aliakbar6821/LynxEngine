package com.lynxengine.app.ui.screens

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lynxengine.app.data.DeviceProfiles
import com.lynxengine.app.data.GameDeviceProfile
import com.lynxengine.app.data.GameEntry
import com.lynxengine.app.ui.theme.LynxGreen
import com.lynxengine.app.ui.theme.LynxRed
import com.lynxengine.app.viewmodel.LynxUiState

// ── Steps inside the Game Unlocker flow ──────────────────────────────────────
private sealed class GamePickerStep {
    object Hidden          : GamePickerStep()
    object PickApp         : GamePickerStep()
    data class PickProfile(val app: ApplicationInfo, val label: String) : GamePickerStep()
    data class ManualEntry(val app: ApplicationInfo, val label: String) : GamePickerStep()
}

// ── Main composable — called from ToolsScreen ─────────────────────────────────
@Composable
fun GameUnlockerPage(
    uiState: LynxUiState,
    onBack: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onAddGame: (GameEntry) -> Unit,
    onRemoveGame: (String) -> Unit
) {
    var step by remember { mutableStateOf<GamePickerStep>(GamePickerStep.Hidden) }

    // Route to full-screen pickers
    when (val s = step) {
        is GamePickerStep.PickApp -> {
            GameAppPicker(
                alreadyAdded = uiState.gameUnlockerGames.map { it.packageName }.toSet(),
                onAppSelected = { appInfo, label ->
                    step = GamePickerStep.PickProfile(appInfo, label)
                },
                onDismiss = { step = GamePickerStep.Hidden }
            )
            return
        }
        is GamePickerStep.ManualEntry -> {
            ManualProfileForm(
                appLabel = s.label,
                onConfirm = { entry ->
                    onAddGame(entry.copy(packageName = s.app.packageName, appName = s.label))
                    step = GamePickerStep.Hidden
                },
                onBack = { step = GamePickerStep.PickProfile(s.app, s.label) }
            )
            return
        }
        else -> { /* continue to main page below */ }
    }

    // Profile picker is a Dialog (not full-screen) — shown on top of main page
    if (step is GamePickerStep.PickProfile) {
        val s = step as GamePickerStep.PickProfile
        ProfileSelectorDialog(
            onProfileSelected = { profile ->
                onAddGame(
                    GameEntry(
                        packageName  = s.app.packageName,
                        appName      = s.label,
                        mode         = "auto",
                        profileName  = profile.name,
                        brand        = profile.brand,
                        device       = profile.device,
                        manufacturer = profile.manufacturer,
                        model        = profile.model,
                        fingerprint  = profile.fingerprint,
                        product      = profile.product
                    )
                )
                step = GamePickerStep.Hidden
            },
            onManual = { step = GamePickerStep.ManualEntry(s.app, s.label) },
            onDismiss = { step = GamePickerStep.Hidden }
        )
    }

    // ── Main Game Unlocker page ───────────────────────────────────────────
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 16.dp)) {

        // Back row
        TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) {
            Icon(Icons.Default.ArrowBack, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Back")
        }

        Spacer(Modifier.height(4.dp))

        // Header + enable toggle
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.SportsEsports, null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(26.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Game Unlocker", fontWeight = FontWeight.Bold, fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        if (uiState.gameUnlockerEnabled)
                            "${uiState.gameUnlockerGames.size} game(s) active"
                        else "Disabled",
                        fontSize = 12.sp,
                        color = if (uiState.gameUnlockerEnabled) LynxGreen
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = uiState.gameUnlockerEnabled,
                    onCheckedChange = onToggleEnabled
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Description banner
        Surface(shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)) {
            Text(
                "Games added here will see a spoofed device identity at startup. " +
                "This unlocks higher graphics tiers and FPS caps for supported titles. " +
                "Takes effect on the next game launch.",
                modifier = Modifier.padding(12.dp),
                fontSize = 12.sp, lineHeight = 17.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(Modifier.height(12.dp))

        // Add Game button
        Button(
            onClick = { step = GamePickerStep.PickApp },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Add Game")
        }

        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        Spacer(Modifier.height(8.dp))

        // Game list
        if (uiState.gameUnlockerGames.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.SportsEsports, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                        modifier = Modifier.size(48.dp))
                    Text("No games configured", fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Tap Add Game to get started", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(uiState.gameUnlockerGames, key = { it.packageName }) { entry ->
                    GameEntryCard(entry = entry, onRemove = { onRemoveGame(entry.packageName) })
                }
            }
        }
    }
}

// ── Single game card in the list ──────────────────────────────────────────────
@Composable
private fun GameEntryCard(entry: GameEntry, onRemove: () -> Unit) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Remove Game?", fontWeight = FontWeight.Bold) },
            text = { Text("Remove spoofing config for ${entry.appName}?") },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; onRemove() }) {
                    Text("Remove", color = LynxRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Game icon placeholder
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Gamepad, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(entry.appName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface)
                    Text(entry.packageName, fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                // Expand/collapse chevron
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null, tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )

                // Delete button
                IconButton(onClick = { showDeleteDialog = true },
                    modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, "Remove",
                        tint = LynxRed, modifier = Modifier.size(20.dp))
                }
            }

            // Profile subtitle — always visible
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (entry.mode == "auto")
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                ) {
                    Text(
                        entry.mode.uppercase(),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 9.sp, fontWeight = FontWeight.Bold,
                        color = if (entry.mode == "auto") MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.secondary
                    )
                }
                Text("• ${entry.profileName}", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            // Expanded: full profile details
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit  = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(Modifier.height(8.dp))
                    Surface(shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.background.copy(alpha = 0.5f)) {
                        Column(modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            ProfileField("BRAND",        entry.brand)
                            ProfileField("DEVICE",       entry.device)
                            ProfileField("MANUFACTURER", entry.manufacturer)
                            ProfileField("MODEL",        entry.model)
                            ProfileField("PRODUCT",      entry.product)
                            ProfileField("FINGERPRINT",  entry.fingerprint)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileField(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(90.dp))
        Text(value, fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f))
    }
}

// ── Game App Picker — single select, full screen ──────────────────────────────
@Composable
private fun GameAppPicker(
    alreadyAdded: Set<String>,
    onAppSelected: (ApplicationInfo, String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var showSystemApps by remember { mutableStateOf(false) }

    val allApps = remember {
        context.packageManager
            .getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.packageName != context.packageName }
            .sortedBy { context.packageManager.getApplicationLabel(it).toString().lowercase() }
    }

    val userApps = remember(allApps) {
        allApps.filter { app ->
            val isSystem  = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val isUpdated = (app.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
            !isSystem || isUpdated
        }
    }

    val baseList = if (showSystemApps) allApps else userApps

    val filtered by remember(searchQuery, showSystemApps) {
        derivedStateOf {
            if (searchQuery.isBlank()) baseList
            else baseList.filter {
                val label = context.packageManager.getApplicationLabel(it).toString()
                label.contains(searchQuery, ignoreCase = true) ||
                it.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Top bar
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.ArrowBack, null)
                }
                Text("Select Game", fontWeight = FontWeight.Bold, fontSize = 17.sp,
                    modifier = Modifier.weight(1f))
            }

            Spacer(Modifier.height(8.dp))

            // Search
            OutlinedTextField(
                value = searchQuery, onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search apps...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank())
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, null)
                        }
                },
                singleLine = true, shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(8.dp))

            // Stats + system toggle
            Row(modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${filtered.size} apps", fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("System apps", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Switch(checked = showSystemApps,
                        onCheckedChange = { showSystemApps = it },
                        modifier = Modifier.height(24.dp))
                }
            }

            Spacer(Modifier.height(4.dp))

            // App list — single tap selects immediately
            LazyColumn {
                items(filtered, key = { it.packageName }) { appInfo ->
                    val pkg   = appInfo.packageName
                    val label = context.packageManager.getApplicationLabel(appInfo).toString()
                    val added = pkg in alreadyAdded

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !added) { onAppSelected(appInfo, label) }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Android, null,
                            tint = if (added) MaterialTheme.colorScheme.onSurfaceVariant
                                   else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                                color = if (added) MaterialTheme.colorScheme.onSurfaceVariant
                                        else MaterialTheme.colorScheme.onSurface)
                            Text(pkg, fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (added) {
                            Text("Added", fontSize = 11.sp, color = LynxGreen)
                        } else {
                            Icon(Icons.Default.ChevronRight, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp))
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                }
            }
        }
    }
}

// ── Profile Selector Dialog ───────────────────────────────────────────────────
@Composable
private fun ProfileSelectorDialog(
    onProfileSelected: (GameDeviceProfile) -> Unit,
    onManual: () -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember { mutableStateOf<GameDeviceProfile?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Dialog title
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.PhoneAndroid, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp))
                    Text("Select Device Profile", fontWeight = FontWeight.Bold, fontSize = 17.sp,
                        modifier = Modifier.weight(1f))
                }

                Spacer(Modifier.height(4.dp))
                Text("Choose a gaming device to spoof. The game will see this device's identity.",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp)

                Spacer(Modifier.height(12.dp))

                // Profiles list
                LazyColumn(modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(DeviceProfiles.ALL, key = { it.name }) { profile ->
                        ProfileOption(
                            profile    = profile,
                            isSelected = selected == profile,
                            onClick    = { selected = profile }
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // Buttons row
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onManual, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Manual", fontSize = 13.sp)
                    }
                    Button(
                        onClick = { selected?.let(onProfileSelected) },
                        enabled = selected != null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Select", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

// ── One profile row in the selector ──────────────────────────────────────────
@Composable
private fun ProfileOption(
    profile: GameDeviceProfile,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
             else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    Surface(
        modifier  = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape     = RoundedCornerShape(12.dp),
        color     = bg
    ) {
        Column(modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Selection indicator
                Box(
                    modifier = Modifier.size(16.dp).clip(CircleShape).background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected)
                        Icon(Icons.Default.Check, null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(10.dp))
                }
                Text(profile.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface)
            }

            // Manufacturer + Model
            Text("${profile.manufacturer}  ·  ${profile.model}",
                fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 24.dp))

            // Show extra detail when selected
            AnimatedVisibility(visible = isSelected) {
                Column(modifier = Modifier.padding(start = 24.dp, top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    MiniProfileRow("Brand",   profile.brand)
                    MiniProfileRow("Device",  profile.device)
                    MiniProfileRow("Product", profile.product)
                    MiniProfileRow("FP", profile.fingerprint)
                }
            }
        }
    }
}

@Composable
private fun MiniProfileRow(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(40.dp))
        Text(value, fontSize = 9.sp, fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

// ── Manual Profile Entry — full-screen form ───────────────────────────────────
@Composable
private fun ManualProfileForm(
    appLabel: String,
    onConfirm: (GameEntry) -> Unit,
    onBack: () -> Unit
) {
    var name         by remember { mutableStateOf("") }
    var brand        by remember { mutableStateOf("") }
    var device       by remember { mutableStateOf("") }
    var manufacturer by remember { mutableStateOf("") }
    var model        by remember { mutableStateOf("") }
    var fingerprint  by remember { mutableStateOf("") }
    var product      by remember { mutableStateOf("") }

    val isValid = model.isNotBlank() && manufacturer.isNotBlank()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Back
            TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) {
                Icon(Icons.Default.ArrowBack, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Back to profiles")
            }

            Text("Manual Profile for $appLabel",
                fontWeight = FontWeight.Bold, fontSize = 17.sp)

            Surface(shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)) {
                Text(
                    "MODEL and MANUFACTURER are required. All other fields are optional " +
                    "but recommended for full spoofing accuracy.",
                    modifier = Modifier.padding(10.dp),
                    fontSize = 12.sp, lineHeight = 16.sp
                )
            }

            // Profile display name (optional label, not a Build field)
            ManualField("Profile Name (label)", name, required = false) { name = it }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Text("Build Fields", fontWeight = FontWeight.Bold, fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary)

            ManualField("BRAND",         brand,        required = false)  { brand = it }
            ManualField("DEVICE",        device,       required = false)  { device = it }
            ManualField("MANUFACTURER",  manufacturer, required = true)   { manufacturer = it }
            ManualField("MODEL",         model,        required = true)   { model = it }
            ManualField("FINGERPRINT",   fingerprint,  required = false,
                hint = "e.g. asus/WW_AI2401/ASUS_AI2401:14/...") { fingerprint = it }
            ManualField("PRODUCT",       product,      required = false)  { product = it }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    onConfirm(
                        GameEntry(
                            packageName  = "",   // filled by caller
                            appName      = "",   // filled by caller
                            mode         = "manual",
                            profileName  = name.ifBlank { "$manufacturer $model" },
                            brand        = brand,
                            device       = device,
                            manufacturer = manufacturer,
                            model        = model,
                            fingerprint  = fingerprint,
                            product      = product
                        )
                    )
                },
                enabled  = isValid,
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Add Game with This Profile")
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ManualField(
    label: String,
    value: String,
    required: Boolean,
    hint: String = "",
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        modifier      = Modifier.fillMaxWidth(),
        label         = {
            Text(if (required) "$label *" else label)
        },
        placeholder   = if (hint.isNotBlank()) {{ Text(hint, fontSize = 11.sp) }} else null,
        singleLine    = label != "FINGERPRINT",
        maxLines      = if (label == "FINGERPRINT") 2 else 1,
        shape         = RoundedCornerShape(10.dp),
        isError       = required && value.isBlank()
    )
}
