package com.lynxengine.app.utils

import android.content.Context
import android.provider.Settings
import android.util.Log

object FrameworkVerifier {

    private const val TAG = "FrameworkVerifier"

    /**
     * Integration is considered active when lynx_pif_data already exists
     * in Settings.Secure (written there by the user via cmd / adb).
     *
     * Logic:
     *   - lynx_pif_data present & non-blank  → integrated, show 5s banner
     *   - missing / blank                     → show warning dialog
     *
     * We NEVER write or delete any key here.
     */
    fun isLynxIntegrated(context: Context): Boolean {
        return try {
            val pif = Settings.Secure.getString(
                context.contentResolver,
                "lynx_pif_data"
            )
            val integrated = !pif.isNullOrBlank()
            if (integrated) {
                Log.d(TAG, "lynx_pif_data found — integration confirmed")
            } else {
                Log.w(TAG, "lynx_pif_data missing — not integrated")
            }
            integrated
        } catch (e: SecurityException) {
            // Settings.Secure read blocked — framework hook not granting access
            Log.e(TAG, "SecurityException reading lynx_pif_data — hook missing?", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during integration check", e)
            false
        }
    }
}
