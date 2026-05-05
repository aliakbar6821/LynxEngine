package com.lynxengine.app.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lynxengine.app.data.GameEntry
import com.lynxengine.app.data.LynxRepository
import com.lynxengine.app.utils.ForceStopUtils
import com.lynxengine.app.utils.FrameworkVerifier
import com.lynxengine.app.utils.NetworkUtils
import com.lynxengine.app.utils.RootUtils
import com.lynxengine.app.utils.SettingsUtils
import com.lynxengine.app.worker.AutoUpdateScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
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
    val integrationChecked: Boolean = false,
    val showIntegrationWarning: Boolean = false,

    // Print PIF Dialog
    val showPrintPifDialog: Boolean = false,
    val currentPifDetails: String = "",

    // Hide Developer Status
    val hideDeveloperApps: Set<String> = emptySet(),

    // Game Unlocker
    val gameUnlockerEnabled: Boolean = false,
    val gameUnlockerGames: List<GameEntry> = emptyList(),

    // Root Mode
    val rootModeEnabled: Boolean = false,
    val rootStatus: RootUtils.RootStatus = RootUtils.RootStatus.UNKNOWN,
    val rootLabel: String = ""           // "KernelSU" / "Magisk" / ""
)

class LynxViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = LynxRepository(application)

    private val _uiState = MutableStateFlow(
        LynxUiState(
            autoUpdateEnabled       = repo.isAutoUpdateEnabled(),
            autoUpdateIntervalDays  = repo.getAutoUpdateInterval(),
            lastAutoUpdateFormatted = formatLastUpdate(repo.getLastAutoUpdateTime()),
            rootModeEnabled         = SettingsUtils.isRootModeEnabled(application)
        )
    )

    val uiState: StateFlow<LynxUiState> = _uiState.asStateFlow()

    init {
        verifyFrameworkIntegration()
        refreshStateOnly()
        loadHideDevApps()
        loadGameUnlocker()
        checkAndAutoUpdateOnLaunch()
        // If root mode was previously saved as enabled, verify it's still valid
        if (SettingsUtils.isRootModeEnabled(application)) {
            verifyExistingRootAccess()
        }
    }

    // ── Root Mode ─────────────────────────────────────────────────────────

    /**
     * Called when user flips the root switch ON.
     * Tests actual root access before committing.
     */
    fun enableRootMode() {
        viewModelScope.launch {
            _uiState.update { it.copy(rootStatus = RootUtils.RootStatus.CHECKING, isLoading = true) }

            val (status, label) = withContext(Dispatchers.IO) {
                val detected = RootUtils.detectRoot()
                if (detected == RootUtils.RootStatus.GRANTED) {
                    // Double-check: can we actually write to the target path?
                    val writeOk = RootUtils.testWrite()
                    if (writeOk) {
                        Pair(RootUtils.RootStatus.GRANTED, RootUtils.rootLabel())
                    } else {
                        Pair(RootUtils.RootStatus.DENIED, "")
                    }
                } else {
                    Pair(RootUtils.RootStatus.DENIED, "")
                }
            }

            if (status == RootUtils.RootStatus.GRANTED) {
                SettingsUtils.setRootModeEnabled(getApplication(), true)
                _uiState.update {
                    it.copy(
                        isLoading      = false,
                        rootModeEnabled = true,
                        rootStatus     = RootUtils.RootStatus.GRANTED,
                        rootLabel      = label,
                        toastMessage   = "✅ Root access granted ($label)"
                    )
                }
            } else {
                // Don't save — keep root mode off
                SettingsUtils.setRootModeEnabled(getApplication(), false)
                _uiState.update {
                    it.copy(
                        isLoading       = false,
                        rootModeEnabled = false,
                        rootStatus      = RootUtils.RootStatus.DENIED,
                        rootLabel       = "",
                        toastMessage    = "❌ Root access denied — KSU or Magisk not found"
                    )
                }
            }
        }
    }

    /** Called when user flips the root switch OFF. */
    fun disableRootMode() {
        SettingsUtils.setRootModeEnabled(getApplication(), false)
        _uiState.update {
            it.copy(
                rootModeEnabled = false,
                rootStatus      = RootUtils.RootStatus.UNKNOWN,
                rootLabel       = "",
                toastMessage    = "Root mode disabled — ROM integration mode active"
            )
        }
    }

    /** On init, if root mode preference is true, silently verify it's still valid. */
    private fun verifyExistingRootAccess() {
        viewModelScope.launch {
            val status = withContext(Dispatchers.IO) { RootUtils.detectRoot() }
            if (status != RootUtils.RootStatus.GRANTED) {
                // Root lost (module removed, etc.) — disable silently
                SettingsUtils.setRootModeEnabled(getApplication(), false)
                _uiState.update {
                    it.copy(
                        rootModeEnabled = false,
                        rootStatus      = RootUtils.RootStatus.DENIED,
                        rootLabel       = "",
                        toastMessage    = "⚠️ Root access lost — root mode disabled"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        rootStatus = RootUtils.RootStatus.GRANTED,
                        rootLabel  = RootUtils.rootLabel()
                    )
                }
            }
        }
    }

    private fun verifyFrameworkIntegration() {
        viewModelScope.launch {
            val isIntegrated = withContext(Dispatchers.IO) {
                FrameworkVerifier.isLynxIntegrated(getApplication())
            }
            _uiState.update {
                it.copy(
                    isFrameworkIntegrated  = isIntegrated,
                    integrationChecked     = true,
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
                SettingsUtils.getHideDevApps(getApplication())
            }
            _uiState.update { it.copy(hideDeveloperApps = apps) }
        }
    }

    fun addHideDevApps(pkgs: Set<String>) {
        if (pkgs.isEmpty()) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                pkgs.forEach { SettingsUtils.addHideDevApp(getApplication(), it) }
            }
            loadHideDevApps()
            _uiState.update { it.copy(toastMessage = "Added ${pkgs.size} app(s)") }
        }
    }

    fun removeHideDevApp(pkg: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                SettingsUtils.removeHideDevApp(getApplication(), pkg)
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
                    runCatching { Runtime.getRuntime().exec(arrayOf("am", "force-stop", pkg)) }
                }
            }
            _uiState.update { it.copy(toastMessage = "Force stopped ${apps.size} app(s)") }
        }
    }

    // ── Game Unlocker ─────────────────────────────────────────────────────
    fun loadGameUnlocker() {
        viewModelScope.launch {
            val (enabled, games) = withContext(Dispatchers.IO) {
                parseGameData(SettingsUtils.getGameData(getApplication()))
            }
            _uiState.update { it.copy(gameUnlockerEnabled = enabled, gameUnlockerGames = games) }
        }
    }

    fun toggleGameUnlocker(enabled: Boolean) {
        val games = _uiState.value.gameUnlockerGames
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                SettingsUtils.setGameData(getApplication(), serializeGameData(enabled, games))
            }
            _uiState.update { it.copy(gameUnlockerEnabled = enabled) }
            _uiState.update {
                it.copy(toastMessage = if (enabled) "Game Unlocker enabled" else "Game Unlocker disabled")
            }
        }
    }

    fun addGameEntry(entry: GameEntry) {
        val current = _uiState.value.gameUnlockerGames.toMutableList()
        current.removeAll { it.packageName == entry.packageName }
        current.add(entry)
        saveGameList(current)
        _uiState.update { it.copy(toastMessage = "✅ ${entry.appName} added") }
    }

    fun removeGameEntry(packageName: String) {
        val current = _uiState.value.gameUnlockerGames.filter { it.packageName != packageName }
        saveGameList(current)
        _uiState.update { it.copy(toastMessage = "Removed game profile") }
    }

    private fun saveGameList(games: List<GameEntry>) {
        val enabled = _uiState.value.gameUnlockerEnabled
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                SettingsUtils.setGameData(getApplication(), serializeGameData(enabled, games))
            }
            _uiState.update { it.copy(gameUnlockerGames = games) }
        }
    }

    // ── JSON helpers ──────────────────────────────────────────────────────
    private fun parseGameData(raw: String?): Pair<Boolean, List<GameEntry>> {
        if (raw.isNullOrBlank()) return Pair(false, emptyList())
        return runCatching {
            val root    = JSONObject(raw)
            val enabled = root.optBoolean("enabled", false)
            val games   = mutableListOf<GameEntry>()
            if (root.has("games")) {
                val obj = root.getJSONObject("games")
                obj.keys().forEach { pkg ->
                    val g = obj.getJSONObject(pkg)
                    games.add(
                        GameEntry(
                            packageName  = pkg,
                            appName      = g.optString("appName", pkg),
                            mode         = g.optString("mode", "auto"),
                            profileName  = g.optString("profileName", ""),
                            brand        = g.optString("BRAND", ""),
                            device       = g.optString("DEVICE", ""),
                            manufacturer = g.optString("MANUFACTURER", ""),
                            model        = g.optString("MODEL", ""),
                            fingerprint  = g.optString("FINGERPRINT", ""),
                            product      = g.optString("PRODUCT", "")
                        )
                    )
                }
            }
            Pair(enabled, games)
        }.getOrDefault(Pair(false, emptyList()))
    }

    private fun serializeGameData(enabled: Boolean, games: List<GameEntry>): String {
        val root     = JSONObject()
        val gamesObj = JSONObject()
        games.forEach { g ->
            val entry = JSONObject()
            entry.put("appName",      g.appName)
            entry.put("mode",         g.mode)
            entry.put("profileName",  g.profileName)
            entry.put("BRAND",        g.brand)
            entry.put("DEVICE",       g.device)
            entry.put("MANUFACTURER", g.manufacturer)
            entry.put("MODEL",        g.model)
            entry.put("FINGERPRINT",  g.fingerprint)
            entry.put("PRODUCT",      g.product)
            gamesObj.put(g.packageName, entry)
        }
        root.put("enabled", enabled)
        root.put("games",   gamesObj)
        return root.toString()
    }

    // ── Refresh / PIF / Keybox / etc. (unchanged) ─────────────────────────
    private fun refreshStateOnly() {
        _uiState.update {
            it.copy(
                isPifLoaded    = repo.isPifLoaded(),
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
                val msg = if (stop.hardStop) "✅ Refreshed & GMS restarted" else "⚠️ Refreshed (soft restart)"
                _uiState.update { it.copy(isLoading = false, toastMessage = msg) }
            } catch (e: Throwable) {
                _uiState.update { it.copy(isLoading = false, toastMessage = "❌ ${e.message?.take(50)}") }
            }
        }
    }

    private fun checkAndAutoUpdateOnLaunch() {
        if (!repo.isAutoUpdateEnabled()) return
        if (!NetworkUtils.isOnline(getApplication())) return
        val last     = repo.getLastAutoUpdateTime()
        val interval = TimeUnit.DAYS.toMillis(repo.getAutoUpdateInterval().toLong())
        if (last == 0L || System.currentTimeMillis() - last >= interval) performAutoUpdate()
    }

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
        if (repo.isAutoUpdateEnabled()) AutoUpdateScheduler.schedule(getApplication(), days)
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
                    val stop = withContext(Dispatchers.IO) { ForceStopUtils.restartGoogleServices(getApplication()) }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            lastAutoUpdateFormatted = formatLastUpdate(repo.getLastAutoUpdateTime()),
                            toastMessage = "✅ $msg" + if (stop.hardStop) " & GMS restarted" else ""
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, toastMessage = "❌ ${e.message?.take(100)}") }
                }
        }
    }

    fun loadPif(file: File) = viewModelScope.launch {
        if (_uiState.value.autoUpdateEnabled) {
            _uiState.update { it.copy(toastMessage = "⚠️ Disable Auto Update first") }; return@launch
        }
        _uiState.update { it.copy(isLoading = true) }
        repo.loadPifFromFile(file)
            .onSuccess { refreshStateOnly(); ForceStopUtils.restartGoogleServices(getApplication()); _uiState.update { it.copy(isLoading = false, toastMessage = "✅ PIF loaded") } }
            .onFailure { e -> _uiState.update { it.copy(isLoading = false, toastMessage = "❌ ${e.message}") } }
    }

    fun loadKeybox(file: File) = viewModelScope.launch {
        if (_uiState.value.autoUpdateEnabled) {
            _uiState.update { it.copy(toastMessage = "⚠️ Disable Auto Update first") }; return@launch
        }
        _uiState.update { it.copy(isLoading = true) }
        repo.loadKeyboxFromFile(file)
            .onSuccess { refreshStateOnly(); ForceStopUtils.restartGoogleServices(getApplication()); _uiState.update { it.copy(isLoading = false, toastMessage = "✅ Keybox loaded") } }
            .onFailure { e -> _uiState.update { it.copy(isLoading = false, toastMessage = "❌ ${e.message}") } }
    }

    fun showPrintPif() {
        val pif = repo.getPifData()
        val details = if (pif.isNullOrBlank()) "No PIF loaded"
        else runCatching {
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
            }
        }.getOrDefault("Error parsing PIF")
        _uiState.update { it.copy(showPrintPifDialog = true, currentPifDetails = details) }
    }

    fun dismissPrintPifDialog() { _uiState.update { it.copy(showPrintPifDialog = false) } }

    fun exportPifToUri(uri: Uri) = viewModelScope.launch {
        val data = repo.getPifData()
        if (data.isNullOrBlank()) { _uiState.update { it.copy(toastMessage = "❌ No PIF") }; return@launch }
        writeUri(uri, data, "PIF")
    }

    fun exportKeyboxToUri(uri: Uri) = viewModelScope.launch {
        val data = repo.getKeyboxData()
        if (data.isNullOrBlank()) { _uiState.update { it.copy(toastMessage = "❌ No Keybox") }; return@launch }
        writeUri(uri, data, "Keybox")
    }

    private suspend fun writeUri(uri: Uri, text: String, name: String) {
        runCatching {
            getApplication<Application>().contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray(Charsets.UTF_8)) }
        }.onSuccess { _uiState.update { it.copy(toastMessage = "✅ $name exported") } }
         .onFailure { _uiState.update { it.copy(toastMessage = "❌ Export failed") } }
    }

    fun clearAll() = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        repo.clearAll()
            .onSuccess { refreshStateOnly(); ForceStopUtils.restartGoogleServices(getApplication()); _uiState.update { it.copy(isLoading = false, toastMessage = "✅ Cleared") } }
            .onFailure { e -> _uiState.update { it.copy(isLoading = false, toastMessage = "❌ ${e.message}") } }
    }

    fun dismissToast() { _uiState.update { it.copy(toastMessage = null) } }

    companion object {
        fun formatLastUpdate(millis: Long): String {
            if (millis == 0L) return "Never"
            return SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault()).format(Date(millis))
        }
    }
}
