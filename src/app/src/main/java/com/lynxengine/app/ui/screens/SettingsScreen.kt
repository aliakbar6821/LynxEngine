package com.lynxengine.app.ui.screens

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lynxengine.app.ui.theme.LynxGreen
import com.lynxengine.app.ui.theme.LynxOrange
import com.lynxengine.app.ui.theme.LynxRed
import com.lynxengine.app.utils.RootUtils
import com.lynxengine.app.viewmodel.LynxUiState

@Composable
fun SettingsScreen(
    uiState: LynxUiState,
    onToggleAutoUpdate: (Boolean) -> Unit,
    onSetInterval: (Int) -> Unit,
    onEnableRootMode: () -> Unit,
    onDisableRootMode: () -> Unit
) {
    val isDark = isSystemInDarkTheme()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // ── App info card ─────────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Lynx Engine", fontWeight = FontWeight.Bold, fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface)
                    Text("Version 1.0.0", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Keybox & PIF spoofing engine", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // ── Root Access card ──────────────────────────────────────────────
        RootAccessCard(
            enabled     = uiState.rootModeEnabled,
            status      = uiState.rootStatus,
            rootLabel   = uiState.rootLabel,
            isChecking  = uiState.isLoading && uiState.rootStatus == RootUtils.RootStatus.CHECKING,
            onEnable    = onEnableRootMode,
            onDisable   = onDisableRootMode
        )

        // ── Auto Update card ──────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Autorenew, null,
                        tint = if (uiState.autoUpdateEnabled) LynxGreen else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto Update", fontWeight = FontWeight.Bold, fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            if (uiState.autoUpdateEnabled) "Enabled" else "Disabled",
                            fontSize = 12.sp,
                            color = if (uiState.autoUpdateEnabled) LynxGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.autoUpdateEnabled,
                        onCheckedChange = onToggleAutoUpdate,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor  = LynxGreen,
                            checkedTrackColor  = LynxGreen.copy(alpha = 0.3f)
                        )
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Automatically download latest keybox.xml and pif.json from GitHub repository.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }

        // ── Update Schedule card ──────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Timer, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Update Schedule", fontWeight = FontWeight.Bold, fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1 to "1 Day", 3 to "3 Days", 7 to "7 Days").forEach { (days, label) ->
                        val selected = uiState.autoUpdateIntervalDays == days
                        OutlinedCard(
                            onClick = { onSetInterval(days) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = if (selected) MaterialTheme.colorScheme.primary
                                                 else MaterialTheme.colorScheme.surface
                            ),
                            border = if (selected)
                                androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            else
                                androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = when (days) {
                                        1    -> Icons.Default.Today
                                        3    -> Icons.Default.DateRange
                                        else -> Icons.Default.CalendarMonth
                                    },
                                    contentDescription = null,
                                    tint = if (selected) MaterialTheme.colorScheme.onPrimary
                                           else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = label,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = if (selected) "Active" else "Tap",
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

// ── Root Access Card ──────────────────────────────────────────────────────────
@Composable
private fun RootAccessCard(
    enabled:    Boolean,
    status:     RootUtils.RootStatus,
    rootLabel:  String,
    isChecking: Boolean,
    onEnable:   () -> Unit,
    onDisable:  () -> Unit
) {
    val statusColor = when (status) {
        RootUtils.RootStatus.GRANTED  -> LynxGreen
        RootUtils.RootStatus.DENIED   -> LynxRed
        RootUtils.RootStatus.CHECKING -> LynxOrange
        RootUtils.RootStatus.UNKNOWN  -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val statusText = when (status) {
        RootUtils.RootStatus.GRANTED  -> "Root access granted • $rootLabel"
        RootUtils.RootStatus.DENIED   -> "Root access denied"
        RootUtils.RootStatus.CHECKING -> "Checking root access…"
        RootUtils.RootStatus.UNKNOWN  -> if (enabled) "Verifying…" else "Disabled"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AdminPanelSettings, null,
                    tint = if (enabled) LynxGreen else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Root Access",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isChecking) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(10.dp),
                                strokeWidth = 1.5.dp,
                                color = LynxOrange
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(statusText, fontSize = 12.sp, color = LynxOrange)
                        }
                    } else {
                        Text(statusText, fontSize = 12.sp, color = statusColor)
                    }
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = { isOn -> if (isOn) onEnable() else onDisable() },
                    enabled = !isChecking,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = LynxGreen,
                        checkedTrackColor = LynxGreen.copy(alpha = 0.3f)
                    )
                )
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(10.dp))

            // Mode description
            if (enabled) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.CheckCircle, null,
                        tint = LynxGreen, modifier = Modifier.size(16.dp).padding(top = 1.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Root mode — game data written via root shell. " +
                        "Works with module-based installs. No ROM patch required.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 17.sp
                    )
                }
            } else {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Info, null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp).padding(top = 1.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "ROM integration mode — game data written directly by the app. " +
                        "Requires framework patch, init.rc dir creation, and SELinux policy.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 17.sp
                    )
                }
            }
        }
    }
}
