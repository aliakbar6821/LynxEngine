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
}