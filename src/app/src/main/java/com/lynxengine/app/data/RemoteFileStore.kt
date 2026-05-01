package com.lynxengine.app.data

import android.content.Context
import java.io.File

class RemoteFileStore(context: Context) {

    private val rootDir = File(context.filesDir, "lynx_remote")
    private val workingDir = File(rootDir, "working")
    private val appliedDir = File(rootDir, "applied")

    init {
        ensureDirs()
    }

    fun rootPath(): String = rootDir.absolutePath

    fun ensureDirs() {
        if (!workingDir.exists()) workingDir.mkdirs()
        if (!appliedDir.exists()) appliedDir.mkdirs()
    }

    fun clearWorking() {
        workingDir.deleteRecursively()
        workingDir.mkdirs()
    }

    fun clearApplied() {
        appliedDir.deleteRecursively()
        appliedDir.mkdirs()
    }

    fun clearAll() {
        rootDir.deleteRecursively()
        ensureDirs()
    }

    fun workingKeyboxFile(): File = File(workingDir, "keybox.xml")
    fun workingPifFile(): File = File(workingDir, "pif.json")

    fun appliedKeyboxFile(): File = File(appliedDir, "keybox.xml.applied")
    fun appliedPifFile(): File = File(appliedDir, "pif.json.applied")

    fun getAppliedKeyboxText(): String? =
        appliedKeyboxFile().takeIf { it.exists() }?.readText(Charsets.UTF_8)

    fun getAppliedPifText(): String? =
        appliedPifFile().takeIf { it.exists() }?.readText(Charsets.UTF_8)

    fun saveCleanVersion(content: String, targetFile: File) {
        targetFile.parentFile?.mkdirs()
        targetFile.writeText(content, charset = Charsets.UTF_8)
    }
}