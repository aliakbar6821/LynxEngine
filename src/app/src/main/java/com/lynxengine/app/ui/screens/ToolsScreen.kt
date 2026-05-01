package com.lynxengine.app.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lynxengine.app.ui.components.StatusCard
import com.lynxengine.app.ui.components.ToolItem
import com.lynxengine.app.ui.theme.LynxGreen
import com.lynxengine.app.ui.theme.LynxRed
import com.lynxengine.app.viewmodel.LynxUiState
import java.io.File
import java.io.FileOutputStream

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
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(uiState.currentPifDetails, fontSize = 12.sp)
                }
            },
            confirmButton = { TextButton(onClick = onDismissPrintPif) { Text("OK") } }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            if (uiState.autoUpdateEnabled) {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Auto Update", fontWeight = FontWeight.Bold, fontSize = 15.sp, 
                            color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
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
                                type = "*/*"; addCategory(Intent.CATEGORY_OPENABLE) 
                            }) 
                        })
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), 
                            modifier = Modifier.padding(start = 58.dp))
                        ToolItem(Icons.Default.UploadFile, "Select PIF",
                            if (uiState.isPifLoaded) "PIF loaded ✓" else "Load pif.json",
                            iconTint = if (uiState.isPifLoaded) LynxGreen else MaterialTheme.colorScheme.primary,
                            onClick = { pifLauncher.launch(Intent(Intent.ACTION_GET_CONTENT).apply { 
                                type = "*/*"; addCategory(Intent.CATEGORY_OPENABLE) 
                            }) 
                        })
                    }
                }
            }

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

            StatusCard(uiState.isPifLoaded, uiState.isKeyboxLoaded, onRefresh)
        }

        if (uiState.isLoading) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background.copy(alpha = 0.8f)) {
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

private fun copyUriToTemp(context: android.content.Context, uri: Uri, name: String): File? = runCatching {
    val tmp = File(context.cacheDir, name)
    context.contentResolver.openInputStream(uri)!!.use { input ->
        FileOutputStream(tmp).use { output -> input.copyTo(output) }
    }
    tmp
}.getOrNull()