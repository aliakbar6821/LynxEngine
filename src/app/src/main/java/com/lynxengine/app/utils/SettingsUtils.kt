package com.lynxengine.app.utils

import android.content.Context
import android.provider.Settings

object SettingsUtils {

    private const val KEY_PIF = "lynx_pif_data"
    private const val KEY_KEYBOX = "lynx_keybox_data"

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
    // This key is read by LynxHideDevUtils.smali in framework.jar
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
}
