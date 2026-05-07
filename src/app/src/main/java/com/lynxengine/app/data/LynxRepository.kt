package com.lynxengine.app.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.lynxengine.app.utils.KeyboxCertParser
import com.lynxengine.app.utils.SettingsUtils
import java.io.File

class LynxRepository(private val context: Context) {

    private val prefs by lazy {
        context.getSharedPreferences("lynx_prefs", Context.MODE_PRIVATE)
    }

    private val gson = Gson()
    private val GITHUB_REPO_URL = "https://github.com/aliakbar6821/LynxEngine"

    // ── Status ────────────────────────────────────────────────────────────
    fun isPifLoaded(): Boolean = SettingsUtils.isPifLoaded(context)
    fun isKeyboxLoaded(): Boolean = SettingsUtils.isKeyboxLoaded(context)
    fun getPifData(): String? = SettingsUtils.getPifData(context)
    fun getKeyboxData(): String? = SettingsUtils.getKeyboxData(context)

    // ── Auto Update Config ────────────────────────────────────────────────
    fun setAutoUpdateEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("auto_update_enabled", enabled).apply()
    }

    fun isAutoUpdateEnabled(): Boolean =
        prefs.getBoolean("auto_update_enabled", false)

    fun setAutoUpdateInterval(days: Int) {
        prefs.edit().putInt("auto_update_interval_days", days).apply()
    }

    fun getAutoUpdateInterval(): Int =
        prefs.getInt("auto_update_interval_days", 3)

    fun setLastAutoUpdateTime(millis: Long) {
        prefs.edit().putLong("last_auto_update_time", millis).apply()
    }

    fun getLastAutoUpdateTime(): Long =
        prefs.getLong("last_auto_update_time", 0L)

    // ── Validation ────────────────────────────────────────────────────────
    private fun validatePifText(content: String) {
        val trimmed = content.trim()
        require(trimmed.startsWith("{")) { "Invalid JSON" }
        require(trimmed.length > 10) { "PIF too small" }
        JsonParser.parseString(trimmed)
    }

    private fun validateKeyboxText(content: String) {
        val trimmed = content.trim()
        require(trimmed.startsWith("<")) { "Invalid XML" }
        require(trimmed.contains("<AndroidAttestation")) { "Missing AndroidAttestation" }
        require(trimmed.contains("</AndroidAttestation>")) { "Unclosed AndroidAttestation" }
        require(trimmed.length > 500) { "Keybox too small" }
    }

    // ── Download & Apply from GitHub ──────────────────────────────────────
    suspend fun downloadAndApplyFromGitHub(): Result<String> = runCatching {
        val repoUrl = GITHUB_REPO_URL
        require(repoUrl.isNotEmpty()) { "GitHub repo URL not configured" }

        val keyboxRawUrl = GitHubApi.getRawUrl(repoUrl, "data/keybox.xml").getOrThrow()
        val pifRawUrl = GitHubApi.getRawUrl(repoUrl, "data/pif.json").getOrThrow()

        val keyboxBytes = NetworkFetcher.downloadToBytes(keyboxRawUrl).getOrThrow()
        val pifBytes = NetworkFetcher.downloadToBytes(pifRawUrl).getOrThrow()

        val keyboxText = String(keyboxBytes, Charsets.UTF_8)
            .replace("\r\n", "\n").replace("\r", "\n")
        val pifText = String(pifBytes, Charsets.UTF_8)
            .replace("\r\n", "\n").replace("\r", "\n")

        validateKeyboxText(keyboxText)
        validatePifText(pifText)

        loadKeyboxFromString(keyboxText).getOrThrow()
        loadPifFromString(pifText).getOrThrow()

        setLastAutoUpdateTime(System.currentTimeMillis())

        "Successfully updated from repository"
    }

    // ── Load from String ──────────────────────────────────────────────────
    fun loadPifFromString(content: String): Result<Unit> = runCatching {
        validatePifText(content)
        val ok = SettingsUtils.setPifData(context, content)
        require(ok) { "Failed to write Settings.Secure" }
    }

    fun loadKeyboxFromString(content: String): Result<Unit> = runCatching {
        validateKeyboxText(content)
        val ok = SettingsUtils.setKeyboxData(context, content)
        require(ok) { "Failed to write Settings.Secure" }

        // Extract verifiedBootHash from the leaf cert and store separately.
        // The smali hook reads this key to spoof ro.boot.vbmeta.digest so that
        // the attested hash and the live system property match.
        val hash = KeyboxCertParser.extractVerifiedBootHash(content)
        if (hash != null) {
            SettingsUtils.setVbmetaDigest(context, hash)
        } else {
            // Non-fatal — clear any stale value so the hook returns nothing
            SettingsUtils.clearVbmetaDigest(context)
        }
    }

    // ── Load from File ────────────────────────────────────────────────────
    fun loadPifFromFile(file: File): Result<Unit> = runCatching {
        val content = String(file.readBytes(), Charsets.UTF_8)
            .replace("\r\n", "\n").replace("\r", "\n")
        validatePifText(content)
        loadPifFromString(content).getOrThrow()
    }

    fun loadKeyboxFromFile(file: File): Result<Unit> = runCatching {
        val content = String(file.readBytes(), Charsets.UTF_8)
            .replace("\r\n", "\n").replace("\r", "\n")
        validateKeyboxText(content)
        loadKeyboxFromString(content).getOrThrow()
    }

    // ── Clear ─────────────────────────────────────────────────────────────
    fun clearAll(): Result<Unit> = runCatching {
        val ok = SettingsUtils.clearAll(context)
        require(ok) { "Failed to clear all data" }
    }
}