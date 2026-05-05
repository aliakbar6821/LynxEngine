package com.lynxengine.app.utils

import android.util.Log
import java.io.File
import java.io.InputStream

object RootUtils {

    private const val TAG = "RootUtils"
    private const val LYNX_DIR  = "/data/misc/lynx"
    private const val GAME_FILE = "/data/misc/lynx/game_data.json"

    enum class RootStatus { UNKNOWN, CHECKING, GRANTED, DENIED }

    // ── Detection ─────────────────────────────────────────────────────────

    /** Returns true if KernelSU or Magisk binary is reachable AND su grants uid 0. */
    fun detectRoot(): RootStatus {
        return try {
            val result = exec("id")
            if (result.contains("uid=0")) RootStatus.GRANTED else RootStatus.DENIED
        } catch (e: Exception) {
            Log.w(TAG, "Root detection failed: ${e.message}")
            RootStatus.DENIED
        }
    }

    fun isKsu(): Boolean  = File("/data/adb/ksu").exists()
    fun isMagisk(): Boolean = File("/data/adb/magisk").exists()

    /** User-facing label for which root implementation is detected. */
    fun rootLabel(): String = when {
        isKsu()    -> "KernelSU"
        isMagisk() -> "Magisk"
        else       -> "Unknown root"
    }

    // ── Game data file ops (root mode) ────────────────────────────────────

    /**
     * Write game_data.json as root.
     * Strategy: write JSON to app-private cache dir first (no permission needed),
     * then root-copy to /data/misc/lynx/ and set correct permissions + SELinux label.
     * This avoids any shell quoting/escaping issues with JSON content.
     */
    fun writeGameData(json: String, cacheDir: File): Boolean {
        return try {
            // Step 1 — write to app cache (always writable by app)
            val tmp = File(cacheDir, "game_data_tmp.json")
            tmp.writeText(json, Charsets.UTF_8)

            // Step 2 — root: create dir, copy, set perms, restore SELinux label
            val ok = execBool(
                "mkdir -p $LYNX_DIR",
                "cp ${tmp.absolutePath} $GAME_FILE",
                "chmod 644 $GAME_FILE",
                "chown system:system $GAME_FILE",
                "restorecon $GAME_FILE"
            )

            tmp.delete()
            ok
        } catch (e: Exception) {
            Log.e(TAG, "writeGameData failed: ${e.message}")
            false
        }
    }

    fun readGameData(): String? {
        return try {
            val out = exec("cat $GAME_FILE")
            out.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            Log.w(TAG, "readGameData failed: ${e.message}")
            null
        }
    }

    fun deleteGameData(): Boolean {
        return try {
            execBool("rm -f $GAME_FILE")
        } catch (e: Exception) {
            Log.w(TAG, "deleteGameData failed: ${e.message}")
            false
        }
    }

    /** Quick write test — verifies root can actually create the lynx dir and a test file. */
    fun testWrite(): Boolean {
        return try {
            execBool(
                "mkdir -p $LYNX_DIR",
                "touch $LYNX_DIR/.lynx_test",
                "rm -f $LYNX_DIR/.lynx_test"
            )
        } catch (e: Exception) {
            false
        }
    }

    // ── Shell helpers ─────────────────────────────────────────────────────

    /**
     * Run multiple commands sequentially via a single `su` session.
     * Returns true only if all commands exit 0.
     */
    private fun execBool(vararg cmds: String): Boolean {
        val script = cmds.joinToString(" && ")
        val proc = ProcessBuilder("su", "-c", script)
            .redirectErrorStream(true)
            .start()
        val exit = proc.waitFor()
        if (exit != 0) {
            val out = proc.inputStream.bufferedReader().readText()
            Log.w(TAG, "execBool failed (exit=$exit): $out")
        }
        return exit == 0
    }

    /** Run a command via su and return stdout as a String. */
    private fun exec(cmd: String): String {
        val proc = ProcessBuilder("su", "-c", cmd)
            .redirectErrorStream(true)
            .start()
        val out = proc.inputStream.bufferedReader().readText().trim()
        proc.waitFor()
        return out
    }
}
