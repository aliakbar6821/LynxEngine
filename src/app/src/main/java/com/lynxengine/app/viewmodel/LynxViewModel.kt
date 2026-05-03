package com.lynxengine.app.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lynxengine.app.data.LynxRepository
import com.lynxengine.app.utils.ForceStopUtils
import com.lynxengine.app.utils.FrameworkVerifier
import com.lynxengine.app.utils.NetworkUtils
import com.lynxengine.app.worker.AutoUpdateScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class LynxUiState(
    val isPifLoaded: Boolean = false,
    val isKeyboxLoaded: Boolean = false,
    val toastMessage: String? = null,
    val isLoading: Boolean = false,
    
    // Auto Update
    val autoUpdateEnabled: Boolean = false,
    val autoUpdateIntervalDays: Int = 3,
    val lastAutoUpdateFormatted: String = "Never",
    
    // Framework Integration
    val isFrameworkIntegrated: Boolean = false,
    val integrationChecked: Boolean = false,   // true once verifyFrameworkIntegration() completes
    val showIntegrationWarning: Boolean = false,
    
    // Print PIF Dialog
    val showPrintPifDialog: Boolean = false,
    val currentPifDetails: String = "",

    // Hide Developer Status
    val hideDeveloperApps: Set<String> = emptySet()
)

class LynxViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = LynxRepository(application)

    private val _uiState = MutableStateFlow(
        LynxUiState(
            autoUpdateEnabled = repo.isAutoUpdateEnabled(),
            autoUpdateIntervalDays = repo.getAutoUpdateInterval(),
            lastAutoUpdateFormatted = formatLastUpdate(repo.getLastAutoUpdateTime())
        )
    )

    val uiState: StateFlow<LynxUiState> = _uiState.asStateFlow()

    init {
        verifyFrameworkIntegration()
        refreshStateOnly()
        loadHideDevApps()
        checkAndAutoUpdateOnLaunch()
    }

    private fun verifyFrameworkIntegration() {
        viewModelScope.launch {
            val isIntegrated = withContext(Dispatchers.IO) {
                FrameworkVerifier.isLynxIntegrated(getApplication())
            }
            _uiState.update {
                it.copy(
                    isFrameworkIntegrated = isIntegrated,
                    integrationChecked = true,
                    showIntegrationWarning = !isIntegrated
                )
            }
        }
    }

    fun dismissIntegrationWarning() {
        _uiState.update { it.copy(showIntegrationWarning = false) }
    }

    fun recheck() {
        verifyFrameworkIntegration()
        refreshStateOnly()
    }


    // ── Hide Developer Apps ───────────────────────────────────────────────
    fun loadHideDevApps() {
        viewModelScope.launch {
            val apps = withContext(Dispatchers.IO) {
                com.lynxengine.app.utils.SettingsUtils.getHideDevApps(getApplication())
            }
            _uiState.update { it.copy(hideDeveloperApps = apps) }
        }
    }

    fun addHideDevApps(pkgs: Set<String>) {
        if (pkgs.isEmpty()) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                pkgs.forEach { com.lynxengine.app.utils.SettingsUtils.addHideDevApp(getApplication(), it) }
            }
            loadHideDevApps()
            _uiState.update { it.copy(toastMessage = "Added ${pkgs.size} app(s)") }
        }
    }

    fun removeHideDevApp(pkg: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                com.lynxengine.app.utils.SettingsUtils.removeHideDevApp(getApplication(), pkg)
            }
            loadHideDevApps()
            _uiState.update { it.copy(toastMessage = "Removed: $pkg") }
        }
    }

    fun forceStopHideDevApps() {
        val apps = _uiState.value.hideDeveloperApps
        if (apps.isEmpty()) {
            _uiState.update { it.copy(toastMessage = "No apps configured") }
            return
        }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                apps.forEach { pkg ->
                    runCatching {
                        Runtime.getRuntime().exec(arrayOf("am", "force-stop", pkg))
                    }
                }
            }
            _uiState.update { it.copy(toastMessage = "Force stopped ${apps.size} app(s)") }
        }
    }
    private fun refreshStateOnly() {
        _uiState.update {
            it.copy(
                isPifLoaded = repo.isPifLoaded(),
                isKeyboxLoaded = repo.isKeyboxLoaded()
            )
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                refreshStateOnly()
                val stop = withContext(Dispatchers.IO) {
                    ForceStopUtils.restartGoogleServices(getApplication())
                }
                val msg = if (stop.hardStop) "✅ Refreshed & GMS restarted"
                else "⚠️ Refreshed (soft restart)"
                _uiState.update { it.copy(isLoading = false, toastMessage = msg) }
            } catch (e: Throwable) {
                _uiState.update {
                    it.copy(isLoading = false, toastMessage = "❌ ${e.message?.take(50)}")
                }
            }
        }
    }

    private fun checkAndAutoUpdateOnLaunch() {
        if (!repo.isAutoUpdateEnabled()) return
        if (!NetworkUtils.isOnline(getApplication())) return

        val last = repo.getLastAutoUpdateTime()
        val interval = TimeUnit.DAYS.toMillis(repo.getAutoUpdateInterval().toLong())
        val now = System.currentTimeMillis()

        if (last == 0L || now - last >= interval) {
            performAutoUpdate()
        }
    }

    // ── Auto Update ───────────────────────────────────────────────────────
    fun setAutoUpdateEnabled(enabled: Boolean) {
        repo.setAutoUpdateEnabled(enabled)
        _uiState.update { it.copy(autoUpdateEnabled = enabled) }
        if (enabled) {
            AutoUpdateScheduler.schedule(getApplication(), repo.getAutoUpdateInterval())
            performAutoUpdate()
        } else {
            AutoUpdateScheduler.cancel(getApplication())
        }
    }

    fun setAutoUpdateInterval(days: Int) {
        repo.setAutoUpdateInterval(days)
        _uiState.update { it.copy(autoUpdateIntervalDays = days) }
        if (repo.isAutoUpdateEnabled()) {
            AutoUpdateScheduler.schedule(getApplication(), days)
        }
    }

    fun performAutoUpdate() {
        viewModelScope.launch {
            if (!NetworkUtils.isOnline(getApplication())) {
                _uiState.update { it.copy(toastMessage = "❌ No internet") }
                return@launch
            }
            _uiState.update { it.copy(isLoading = true) }
            repo.downloadAndApplyFromGitHub()
                .onSuccess { msg ->
                    refreshStateOnly()
                    val stop = withContext(Dispatchers.IO) {
                        ForceStopUtils.restartGoogleServices(getApplication())
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            lastAutoUpdateFormatted = formatLastUpdate(repo.getLastAutoUpdateTime()),
                            toastMessage = "✅ $msg" + if (stop.hardStop) " & GMS restarted" else ""
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, toastMessage = "❌ ${e.message?.take(100)}")
                    }
                }
        }
    }

    // ── Load Files ────────────────────────────────────────────────────────
    fun loadPif(file: File) = viewModelScope.launch {
        if (_uiState.value.autoUpdateEnabled) {
            _uiState.update { it.copy(toastMessage = "⚠️ Disable Auto Update first") }
            return@launch
        }
        _uiState.update { it.copy(isLoading = true) }
        repo.loadPifFromFile(file)
            .onSuccess {
                refreshStateOnly()
                ForceStopUtils.restartGoogleServices(getApplication())
                _uiState.update { it.copy(isLoading = false, toastMessage = "✅ PIF loaded") }
            }
            .onFailure { e ->
                _uiState.update { it.copy(isLoading = false, toastMessage = "❌ ${e.message}") }
            }
    }

    fun loadKeybox(file: File) = viewModelScope.launch {
        if (_uiState.value.autoUpdateEnabled) {
            _uiState.update { it.copy(toastMessage = "⚠️ Disable Auto Update first") }
            return@launch
        }
        _uiState.update { it.copy(isLoading = true) }
        repo.loadKeyboxFromFile(file)
            .onSuccess {
                refreshStateOnly()
                ForceStopUtils.restartGoogleServices(getApplication())
                _uiState.update { it.copy(isLoading = false, toastMessage = "✅ Keybox loaded") }
            }
            .onFailure { e ->
                _uiState.update { it.copy(isLoading = false, toastMessage = "❌ ${e.message}") }
            }
    }

    // ── Print PIF ─────────────────────────────────────────────────────────
    fun showPrintPif() {
        val pif = repo.getPifData()
        val details = if (pif.isNullOrBlank()) {
            "No PIF loaded"
        } else {
            try {
                val json = com.google.gson.JsonParser.parseString(pif).asJsonObject
                buildString {
                    appendLine("=== Current PIF ===")
                    appendLine("Fingerprint: ${json.get("FINGERPRINT")?.asString ?: "N/A"}")
                    appendLine("Brand: ${json.get("BRAND")?.asString ?: "N/A"}")
                    appendLine("Device: ${json.get("DEVICE")?.asString ?: "N/A"}")
                    appendLine("Product: ${json.get("PRODUCT")?.asString ?: "N/A"}")
                    appendLine("Manufacturer: ${json.get("MANUFACTURER")?.asString ?: "N/A"}")
                    appendLine("Model: ${json.get("MODEL")?.asString ?: "N/A"}")
                    appendLine("Android: ${json.get("RELEASE")?.asString ?: "N/A"}")
                    appendLine("Security Patch: ${json.get("SECURITY_PATCH")?.asString ?: "N/A"}")
                    appendLine("Tags: ${json.get("TAGS")?.asString ?: "N/A"}")
                    appendLine("ID: ${json.get("ID")?.asString ?: "N/A"}")
                }
            } catch (e: Exception) {
                "Error parsing PIF: ${e.message}"
            }
        }
        _uiState.update { it.copy(showPrintPifDialog = true, currentPifDetails = details) }
    }

    fun dismissPrintPifDialog() {
        _uiState.update { it.copy(showPrintPifDialog = false) }
    }

    // ── Export ────────────────────────────────────────────────────────────
    fun exportPifToUri(uri: Uri) = viewModelScope.launch {
        val data = repo.getPifData()
        if (data.isNullOrBlank()) {
            _uiState.update { it.copy(toastMessage = "❌ No PIF") }
            return@launch
        }
        writeUri(uri, data, "PIF")
    }

    fun exportKeyboxToUri(uri: Uri) = viewModelScope.launch {
        val data = repo.getKeyboxData()
        if (data.isNullOrBlank()) {
            _uiState.update { it.copy(toastMessage = "❌ No Keybox") }
            return@launch
        }
        writeUri(uri, data, "Keybox")
    }

    private suspend fun writeUri(uri: Uri, text: String, name: String) {
        runCatching {
            getApplication<Application>().contentResolver
                .openOutputStream(uri)?.use { it.write(text.toByteArray(Charsets.UTF_8)) }
        }.onSuccess {
            _uiState.update { it.copy(toastMessage = "✅ $name exported") }
        }.onFailure {
            _uiState.update { it.copy(toastMessage = "❌ Export failed") }
        }
    }

    // ── Clear ─────────────────────────────────────────────────────────────
    fun clearAll() = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        repo.clearAll()
            .onSuccess {
                refreshStateOnly()
                ForceStopUtils.restartGoogleServices(getApplication())
                _uiState.update { it.copy(isLoading = false, toastMessage = "✅ Cleared") }
            }
            .onFailure { e ->
                _uiState.update { it.copy(isLoading = false, toastMessage = "❌ ${e.message}") }
            }
    }

    fun dismissToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    companion object {
        fun formatLastUpdate(millis: Long): String {
            if (millis == 0L) return "Never"
            return SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault()).format(Date(millis))
        }
    }
}