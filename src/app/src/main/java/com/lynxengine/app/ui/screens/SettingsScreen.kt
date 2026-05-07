package com.lynxengine.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lynxengine.app.ui.theme.LynxGreen
import com.lynxengine.app.viewmodel.LynxUiState

@Composable
fun SettingsScreen(
    uiState: LynxUiState,
    onToggleAutoUpdate: (Boolean) -> Unit,
    onSetInterval: (Int) -> Unit,
    onBack: (() -> Unit)? = null
) {
    val isDark = isSystemInDarkTheme()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // ── Back button (when opened from top bar) ────────────────────────
        if (onBack != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onBack() }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.ArrowBack, null,
                    tint     = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "Settings",
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 16.sp,
                    color      = MaterialTheme.colorScheme.onSurface
                )
            }
        }

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
