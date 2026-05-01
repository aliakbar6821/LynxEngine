package com.lynxengine.app.utils

import android.app.ActivityManager
import android.content.Context
import java.util.concurrent.TimeUnit

object ForceStopUtils {

    data class StopResult(val success: Boolean, val hardStop: Boolean, val message: String)

    fun restartGoogleServices(context: Context): StopResult {
        val pkgs = listOf("com.google.android.gms", "com.android.vending")
        var anyHard = false
        val messages = mutableListOf<String>()

        for (pkg in pkgs) {
            val result = forceStopSingle(context, pkg)
            messages += "${pkg.split('.').last()}: ${result.message}"
            if (result.hardStop) anyHard = true
        }

        return StopResult(true, anyHard, messages.joinToString(" | "))
    }

    private fun forceStopSingle(context: Context, pkg: String): StopResult {
        // Try hidden API
        try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val method = ActivityManager::class.java.getDeclaredMethod("forceStopPackage", String::class.java)
            method.isAccessible = true
            method.invoke(am, pkg)
            return StopResult(true, true, "OK")
        } catch (_: Throwable) {}

        // Try shell
        try {
            val proc = ProcessBuilder("/system/bin/am", "force-stop", pkg)
                .redirectErrorStream(true).start()
            if (proc.waitFor(5, TimeUnit.SECONDS) && proc.exitValue() == 0) {
                return StopResult(true, true, "OK")
            }
        } catch (_: Throwable) {}

        // Fallback
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            am.killBackgroundProcesses(pkg)
            StopResult(true, false, "soft kill")
        } catch (e: Throwable) {
            StopResult(false, false, e.message?.take(40) ?: "failed")
        }
    }
}