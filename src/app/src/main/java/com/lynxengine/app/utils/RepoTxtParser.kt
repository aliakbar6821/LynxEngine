package com.lynxengine.app.utils

import com.lynxengine.app.data.RepoSource

object RepoTxtParser {

    private val lineRegex = Regex(
        """^(Source|Keybox|Pif|Repo)(\d{1,2})\s*=\s*(.+)$""",
        RegexOption.IGNORE_CASE
    )

    fun parse(content: String, maxSources: Int = 10): List<RepoSource> {
        val buckets = mutableMapOf<Int, MutableMap<String, String>>()

        content.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") && !it.startsWith("//") }
            .forEach { line ->
                val m = lineRegex.find(line) ?: return@forEach
                val key = m.groupValues[1].lowercase()
                val idx = m.groupValues[2].toIntOrNull() ?: return@forEach
                val value = m.groupValues[3].trim()

                if (idx !in 1..maxSources) return@forEach
                buckets.getOrPut(idx) { mutableMapOf() }[key] = value
            }

        return buckets.toSortedMap().mapNotNull { (id, map) ->
            val name = map["source"]?.trim().orEmpty()
            val keybox = map["keybox"]?.trim().orEmpty()
            val pif = map["pif"]?.trim().orEmpty()
            val repo = map["repo"]?.trim().orEmpty()

            if (name.isBlank() || keybox.isBlank() || pif.isBlank() || repo.isBlank()) return@mapNotNull null
            if (!keybox.endsWith(".xml", ignoreCase = true)) return@mapNotNull null
            if (!pif.endsWith(".json", ignoreCase = true)) return@mapNotNull null

            RepoSource(id, name, keybox, pif, repo)
        }
    }
}