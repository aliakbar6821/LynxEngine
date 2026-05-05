package com.lynxengine.app.utils

import android.content.Context
import android.provider.Settings
import java.io.File

object SettingsUtils {

    private const val KEY_PIF    = "lynx_pif_data"
    private const val KEY_KEYBOX = "lynx_keybox_data"

    // ── Root mode preference ──────────────────────────────────────────────
    private const val PREFS_NAME    = "lynx_prefs"
    private const val KEY_ROOT_MODE = "root_mode_enabled"

    fun isRootModeEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ROOT_MODE, false)

    fun setRootModeEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ROOT_MODE, enabled).apply()
    }

    // ── PIF / Keybox — always via Settings.Secure (unchanged) ─────────────
    fun getPifData(context: Context): String? =
        Settings.Secure.getString(context.contentResolver, KEY_PIF)

    fun getKeyboxData(context: Context): String? =
        Settings.Secure.getString(context.contentResolver, KEY_KEYBOX)

    fun isPifLoaded(context: Context): Boolean =
        !getPifData(context).isNullOrBlank()

    fun isKeyboxLoaded(context: Context): Boolean =
        !getKeyboxData(context).isNullOrBlank()

    fun setPifData(context: Context, json: String): Boolean = runCatching {
        Settings.Secure.putString(context.contentResolver, KEY_PIF, json)
    }.getOrDefault(false)

    fun setKeyboxData(context: Context, xml: String): Boolean = runCatching {
        Settings.Secure.putString(context.contentResolver, KEY_KEYBOX, xml)
    }.getOrDefault(false)

    fun clearPifData(context: Context): Boolean = runCatching {
        Settings.Secure.putString(context.contentResolver, KEY_PIF, null)
    }.getOrDefault(false)

    fun clearKeyboxData(context: Context): Boolean = runCatching {
        Settings.Secure.putString(context.contentResolver, KEY_KEYBOX, null)
    }.getOrDefault(false)

    fun clearAll(context: Context): Boolean =
        clearPifData(context) && clearKeyboxData(context)

    // ── Hide Developer Status ─────────────────────────────────────────────
    private const val KEY_HIDE_DEV = "lynx_hide_dev_status"

    fun getHideDevApps(context: Context): Set<String> = runCatching {
        val raw = Settings.Secure.getString(context.contentResolver, KEY_HIDE_DEV)
        if (raw.isNullOrBlank()) emptySet()
        else raw.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
    }.getOrDefault(emptySet())

    fun addHideDevApp(context: Context, pkg: String): Boolean = runCatching {
        val current = getHideDevApps(context).toMutableSet()
        current.add(pkg)
        Settings.Secure.putString(context.contentResolver, KEY_HIDE_DEV, current.joinToString(","))
    }.getOrDefault(false)

    fun removeHideDevApp(context: Context, pkg: String): Boolean = runCatching {
        val current = getHideDevApps(context).toMutableSet()
        current.remove(pkg)
        Settings.Secure.putString(context.contentResolver, KEY_HIDE_DEV, current.joinToString(","))
    }.getOrDefault(false)

    // ── Game Unlocker ─────────────────────────────────────────────────────
    // Root mode ON  → RootUtils shell ops  → /data/misc/lynx/game_data.json
    //                 Works even without ROM integration / SELinux policy.
    // Root mode OFF → direct File API      → /data/misc/lynx/game_data.json
    //                 Requires init.rc pre-creating the dir + SELinux policy
    //                 (ROM-baked install). Works with zero root at runtime.
    //
    // The smali hook always reads the same path regardless of how it was written.

    private val GAME_DATA_FILE = File("/data/misc/lynx/game_data.json")

    fun getGameData(context: Context): String? {
        return if (isRootModeEnabled(context)) {
            runCatching { RootUtils.readGameData() }.getOrNull()
        } else {
            runCatching {
                if (!GAME_DATA_FILE.exists()) null
                else GAME_DATA_FILE.readText(Charsets.UTF_8).takeIf { it.isNotBlank() }
            }.getOrNull()
        }
    }

    fun setGameData(context: Context, json: String): Boolean {
        return if (isRootModeEnabled(context)) {
            runCatching {
                RootUtils.writeGameData(json, context.cacheDir)
            }.getOrDefault(false)
        } else {
            runCatching {
                GAME_DATA_FILE.parentFile?.mkdirs()
                GAME_DATA_FILE.writeText(json, Charsets.UTF_8)
                true
            }.getOrDefault(false)
        }
    }

    fun clearGameData(context: Context): Boolean {
        return if (isRootModeEnabled(context)) {
            runCatching { RootUtils.deleteGameData() }.getOrDefault(false)
        } else {
            runCatching {
                if (GAME_DATA_FILE.exists()) GAME_DATA_FILE.delete() else true
            }.getOrDefault(false)
        }
    }
}
