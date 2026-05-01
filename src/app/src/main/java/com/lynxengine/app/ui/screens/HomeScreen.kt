package com.lynxengine.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lynxengine.app.ui.components.DeviceInfoCard
import com.lynxengine.app.ui.components.InfoRow
import com.lynxengine.app.ui.components.StatusCard
import com.lynxengine.app.utils.DeviceUtils
import com.lynxengine.app.viewmodel.LynxUiState

@Composable
fun HomeScreen(uiState: LynxUiState, onRefresh: () -> Unit, isIntegrated: Boolean) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item { DeviceInfoCard(DeviceUtils.getDeviceName(), DeviceUtils.getAndroidVersion(), DeviceUtils.getSecurityPatch()) }
        item { InfoRow(Icons.Default.Business, "Brand", DeviceUtils.getBrand()) }
        item { InfoRow(Icons.Default.Factory, "Manufacturer", DeviceUtils.getManufacturer()) }
        item { InfoRow(Icons.Default.Inventory2, "Product", DeviceUtils.getProduct()) }
        item { InfoRow(Icons.Default.Code, "Codename", DeviceUtils.getCodename()) }
        item { InfoRow(Icons.Default.Laptop, "Model", DeviceUtils.getModel()) }
        item { InfoRow(Icons.Default.Terminal, "Kernel", DeviceUtils.getKernelVersion()) }
        item { InfoRow(Icons.Default.Fingerprint, "Fingerprint", DeviceUtils.getFingerprint()) }
        item { StatusCard(uiState.isPifLoaded, uiState.isKeyboxLoaded, onRefresh) }
        item { Spacer(Modifier.height(8.dp)) }
    }
}