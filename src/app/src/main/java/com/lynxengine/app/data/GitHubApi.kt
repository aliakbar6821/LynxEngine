package com.lynxengine.app.data

import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

object GitHubApi {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    fun parseOwnerRepo(url: String): Pair<String, String>? {
        val prefix = "https://github.com/"
        if (!url.startsWith(prefix)) return null
        val path = url.removePrefix(prefix).split("/").filter { it.isNotBlank() }
        if (path.size < 2) return null
        return Pair(path[0], path[1])
    }

    fun parseBranch(url: String): String? {
        val prefix = "https://github.com/"
        if (!url.startsWith(prefix)) return null
        val parts = url.removePrefix(prefix).split("/").filter { it.isNotBlank() }
        if (parts.size >= 4 && (parts[2] == "tree" || parts[2] == "blob")) {
            return parts[3]
        }
        return null
    }

    suspend fun getDefaultBranch(owner: String, repo: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = "https://api.github.com/repos/$owner/$repo"
                val req = Request.Builder()
                    .url(url)
                    .header("User-Agent", "LynxEngine/2.0")
                    .build()

                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) throw IOException("GitHub API ${resp.code}")
                    val body = resp.body?.string() ?: throw IOException("Empty response")
                    val obj = gson.fromJson(body, JsonObject::class.java)
                    obj.get("default_branch")?.asString ?: throw IOException("No default_branch")
                }
            }
        }

    suspend fun getRawUrl(repoUrl: String, filePath: String): Result<String> {
        val ownerRepo = parseOwnerRepo(repoUrl)
            ?: return Result.failure(IllegalArgumentException("Invalid GitHub URL"))

        val (owner, repo) = ownerRepo
        val branch = parseBranch(repoUrl) ?: getDefaultBranch(owner, repo).getOrThrow()
        val rawPath = filePath.trim().removePrefix("/")

        return Result.success(
            "https://raw.githubusercontent.com/$owner/$repo/$branch/$rawPath"
        )
    }
}