package com.lynxengine.app.utils

object FileSanitizer {

    private const val BOM_UTF8 = "\ufeff"

    fun sanitize(text: String): String {
        if (text.isBlank()) return ""

        var result = text.removePrefix(BOM_UTF8)   // String overload — removePrefix not trimStart
        result = result.replace("\r\n", "\n").replace("\r", "\n")
        result = result.replace("\u0000", "")

        return result.trim()
    }
}
