package me.weishu.kernelsu.ui.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.BuildConfig
import me.weishu.kernelsu.ksuApp
import okhttp3.Request
import org.json.JSONObject

data class ManagerUpdateInfo(
    val versionName: String,
    val versionCode: Int,
    val fileName: String,
    val downloadUrl: String,
    val changelog: String,
    val force: Boolean,
)

object ManagerUpdateChecker {
    private const val RELEASE_API_URL = "https://api.github.com/repos/fixz232/ApkeSU/releases/latest"
    private const val APK_MIME_TYPE = "application/vnd.android.package-archive"
    private const val PREF_CHECK_UPDATE = "check_update"
    private val apkNamePattern = Regex("""^ApkeSU_(.+)_(\d+)(?:-[^.]+)?\.apk$""", RegexOption.IGNORE_CASE)
    private val trailingVersionCodePattern = Regex("""_(\d+)(?:-[^.]+)?\.apk$""", RegexOption.IGNORE_CASE)
    private val releaseVersionCodePattern = Regex("""(?im)^\s*versionCode\s*[:=]\s*(\d+)\s*$""")
    private val forceUpdatePattern = Regex("""(?im)^\s*(forceUpdate|force_update|mandatoryUpdate|mandatory_update)\s*[:=]\s*(true|1|yes|on)\s*$""")
    private val forceUpdateBelowPattern = Regex(
        """(?im)^\s*(forceUpdateBelowVersionCode|forceUpdateBelow|force_update_below|minimumSupportedVersionCode|minVersionCode)\s*[:=]\s*(\d+)\s*$"""
    )
    private val updateDirectiveLinePattern = Regex(
        """(?im)^\s*(versionCode|forceUpdate|force_update|mandatoryUpdate|mandatory_update|forceUpdateBelowVersionCode|forceUpdateBelow|force_update_below|minimumSupportedVersionCode|minVersionCode)\s*[:=].*(?:\r?\n)?"""
    )

    suspend fun checkLatest(context: Context): ManagerUpdateInfo? = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        if (BuildConfig.IS_PR_BUILD) {
            return@withContext null
        }
        val allowOptionalUpdate = prefs.getBoolean(PREF_CHECK_UPDATE, true)

        runCatching {
            val request = Request.Builder()
                .url(RELEASE_API_URL)
                .header("Accept", "application/vnd.github+json")
                .build()

            ksuApp.okhttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@runCatching null
                parseRelease(JSONObject(response.body.string()))
                    ?.takeIf { allowOptionalUpdate || it.force }
            }
        }.getOrNull()
    }

    fun download(context: Context, updateInfo: ManagerUpdateInfo): Int {
        return DownloadManager.enqueue(
            context = context.applicationContext,
            url = updateInfo.downloadUrl,
            fileName = updateInfo.fileName,
            mimeType = APK_MIME_TYPE,
            completionAction = DownloadCompletionAction.OPEN_FILE,
        )
    }

    private fun parseRelease(release: JSONObject): ManagerUpdateInfo? {
        val releaseName = release.optString("name")
            .ifBlank { release.optString("tag_name") }
            .ifBlank { "latest" }
        val rawChangelog = release.optString("body")
        val changelog = sanitizeChangelog(rawChangelog)
        val releaseVersionCode = releaseVersionCodePattern.find(rawChangelog)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
        val forceUpdate = shouldForceUpdate(rawChangelog)
        val assets = release.optJSONArray("assets") ?: return null
        var best: ManagerUpdateInfo? = null

        for (index in 0 until assets.length()) {
            val asset = assets.optJSONObject(index) ?: continue
            val fileName = asset.optString("name")
            if (!fileName.endsWith(".apk", ignoreCase = true)) continue

            val downloadUrl = asset.optString("browser_download_url")
            if (downloadUrl.isBlank()) continue

            val parsed = parseApkAsset(fileName, releaseName, releaseVersionCode, downloadUrl, changelog, forceUpdate)
                ?: continue
            if (best == null || parsed.versionCode > best.versionCode) {
                best = parsed
            }
        }

        return best?.takeIf { it.versionCode > BuildConfig.VERSION_CODE }
    }

    private fun parseApkAsset(
        fileName: String,
        releaseName: String,
        releaseVersionCode: Int?,
        downloadUrl: String,
        changelog: String,
        force: Boolean,
    ): ManagerUpdateInfo? {
        val fullMatch = apkNamePattern.matchEntire(fileName)
        if (fullMatch != null) {
            val versionCode = fullMatch.groupValues[2].toIntOrNull() ?: return null
            return ManagerUpdateInfo(
                versionName = fullMatch.groupValues[1],
                versionCode = versionCode,
                fileName = fileName,
                downloadUrl = downloadUrl,
                changelog = changelog,
                force = force,
            )
        }

        val versionCode = trailingVersionCodePattern.find(fileName)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
            ?: releaseVersionCode
            ?: return null

        return ManagerUpdateInfo(
            versionName = releaseName,
            versionCode = versionCode,
            fileName = fileName,
            downloadUrl = downloadUrl,
            changelog = changelog,
            force = force,
        )
    }

    private fun shouldForceUpdate(changelog: String): Boolean {
        if (forceUpdatePattern.containsMatchIn(changelog)) return true

        val forceBelowVersionCode = forceUpdateBelowPattern.find(changelog)
            ?.groupValues
            ?.getOrNull(2)
            ?.toIntOrNull()
            ?: return false
        return BuildConfig.VERSION_CODE < forceBelowVersionCode
    }

    private fun sanitizeChangelog(changelog: String): String {
        return changelog.replace(updateDirectiveLinePattern, "").trim()
    }
}
