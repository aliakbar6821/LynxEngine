package com.lynxengine.app.utils

import android.os.Build

object DeviceUtils {

    fun getDeviceName(): String {
        val mfr = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        val model = Build.MODEL
        return if (model.startsWith(mfr, ignoreCase = true)) model else "$mfr $model"
    }

    fun getAndroidVersion(): String {
        val name = when (Build.VERSION.SDK_INT) {
            36 -> "Android 16"
            35 -> "Android 15"
            34 -> "Android 14"
            33 -> "Android 13"
            32, 31 -> "Android 12"
            else -> "Android ${Build.VERSION.RELEASE}"
        }
        return "$name (SDK ${Build.VERSION.SDK_INT})"
    }

    fun getSecurityPatch(): String = Build.VERSION.SECURITY_PATCH
    fun getBrand(): String = Build.BRAND.replaceFirstChar { it.uppercase() }
    fun getManufacturer(): String = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
    fun getProduct(): String = Build.PRODUCT
    fun getCodename(): String = Build.DEVICE
    fun getModel(): String = Build.MODEL
    fun getKernelVersion(): String = System.getProperty("os.version") ?: "Unknown"
    fun getFingerprint(): String = Build.FINGERPRINT
}