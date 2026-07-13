package me.weishu.kernelsu.data.repository

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.data.model.Module
import me.weishu.kernelsu.data.model.ModuleUpdateInfo
import me.weishu.kernelsu.ksuApp
import me.weishu.kernelsu.ui.util.isNetworkAvailable
import me.weishu.kernelsu.ui.util.listModulesWithTimeout
import me.weishu.kernelsu.ui.util.module.sanitizeVersionString
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

class ModuleRepositoryImpl : ModuleRepository {

    companion object {
        private const val TAG = "ModuleRepository"
    }

    override suspend fun getModules(): Result<List<Module>> = withContext(Dispatchers.IO) {
        runCatching {
            val result = listModulesWithTimeout()
            val array = JSONArray(result)
            (0 until array.length())
                .asSequence()
                .mapNotNull { index ->
                    val obj = array.optJSONObject(index)
                    if (obj == null) {
                        Log.w(TAG, "skip malformed module entry at index $index")
                        null
                    } else {
                        parseModule(obj, index)
                    }
                }.toList()
        }
    }

    private fun parseModule(obj: JSONObject, index: Int): Module? {
        val id = obj.optString("id").trim()
        if (id.isEmpty()) {
            Log.w(TAG, "skip module without id at index $index")
            return null
        }

        return Module(
            id = id,
            name = obj.optString("name").takeIf { it.isNotBlank() } ?: id,
            author = obj.optString("author", "Unknown").takeIf { it.isNotBlank() } ?: "Unknown",
            version = obj.optString("version", "Unknown").takeIf { it.isNotBlank() } ?: "Unknown",
            versionCode = obj.optInt("versionCode", 0),
            description = obj.optString("description"),
            enabled = obj.optBoolean("enabled", true),
            update = obj.optBoolean("update", false),
            remove = obj.optBoolean("remove", false),
            updateJson = obj.optString("updateJson"),
            hasWebUi = obj.optBoolean("web", false),
            hasActionScript = obj.optBoolean("action", false),
            metamodule = (obj.optInt("metamodule") != 0) || obj.optBoolean("metamodule", false),
            actionIconPath = obj.optString("actionIcon").takeIf { it.isNotBlank() },
            webUiIconPath = obj.optString("webuiIcon").takeIf { it.isNotBlank() }
        )
    }

    override suspend fun checkUpdate(module: Module): Result<ModuleUpdateInfo> = withContext(Dispatchers.IO) {
        runCatching {
            if (!isNetworkAvailable(ksuApp)) {
                return@runCatching ModuleUpdateInfo.Empty
            }
            if (module.updateJson.isEmpty() || module.remove || module.update || !module.enabled) {
                return@runCatching ModuleUpdateInfo.Empty
            }

            val url = module.updateJson
            val result = ksuApp.okhttpClient.newCall(
                Request.Builder().url(url).build()
            ).execute().use { response ->
                if (response.isSuccessful) response.body.string() else ""
            }

            if (result.isEmpty()) {
                return@runCatching ModuleUpdateInfo.Empty
            }

            val updateJson = JSONObject(result)
            var version = updateJson.optString("version", "")
            version = sanitizeVersionString(version)
            val versionCode = updateJson.optInt("versionCode", 0)
            val zipUrl = updateJson.optString("zipUrl", "")
            val changelog = updateJson.optString("changelog", "")

            if (versionCode <= module.versionCode || zipUrl.isEmpty()) {
                ModuleUpdateInfo.Empty
            } else {
                ModuleUpdateInfo(zipUrl, version, changelog)
            }
        }
    }
}
