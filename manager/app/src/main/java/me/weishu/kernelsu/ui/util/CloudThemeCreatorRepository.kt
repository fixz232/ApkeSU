package me.weishu.kernelsu.ui.util

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import android.util.AtomicFile
import androidx.core.content.edit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.ksuApp
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.security.MessageDigest

private const val CREATOR_REGISTRY_CACHE_FRESH_MS = 15L * 60L * 1000L
private const val CREATOR_REGISTRY_ASSET = "theme-store/creators-v1.json"
private const val CREATOR_METADATA_PREFS = "cloud_theme_creator_metadata"
private const val CREATOR_REGISTRY_ETAG_KEY = "registry_etag"
private const val CREATOR_REGISTRY_LAST_MODIFIED_KEY = "registry_last_modified"
private const val CREATOR_REGISTRY_FETCHED_AT_KEY = "registry_fetched_at"
private const val CLOUD_THEME_GITHUB_API_HOST = "api.github.com"
private val cloudThemeCreatorFileLock = Any()

class CloudThemeCreatorRepository(
    context: Context,
    private val client: OkHttpClient = ksuApp.okhttpClient,
    private val registryUrl: String = CLOUD_THEME_DEFAULT_CREATOR_REGISTRY_URL,
) {
    private val appContext = context.applicationContext

    suspend fun loadRegistry(forceRefresh: Boolean = false): CloudThemeCreatorRegistrySnapshot =
        withContext(Dispatchers.IO) {
            val cachedRegistry = readCachedRegistry()
            val metadata = metadataPrefs()
            val fetchedAt = metadata.getLong(CREATOR_REGISTRY_FETCHED_AT_KEY, 0L)
                .coerceAtLeast(0L)
            val now = System.currentTimeMillis()
            if (!forceRefresh &&
                cachedRegistry != null &&
                now - fetchedAt < CREATOR_REGISTRY_CACHE_FRESH_MS
            ) {
                return@withContext CloudThemeCreatorRegistrySnapshot(
                    registry = cachedRegistry,
                    source = CloudThemeCreatorRegistrySource.Cache,
                    fetchedAt = fetchedAt,
                    offline = false,
                )
            }

            try {
                fetchRegistry(cachedRegistry)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                val fallback = cachedRegistry ?: readBundledRegistry()
                CloudThemeCreatorRegistrySnapshot(
                    registry = fallback,
                    source = if (cachedRegistry != null) {
                        CloudThemeCreatorRegistrySource.Cache
                    } else {
                        CloudThemeCreatorRegistrySource.Bundled
                    },
                    fetchedAt = fetchedAt,
                    offline = true,
                    errorMessage = error.safeCloudThemeMessage(),
                )
            }
        }

    fun readDraft(): CloudThemeSubmissionDraft = synchronized(cloudThemeCreatorFileLock) {
        val file = draftFile().baseFile
        if (!file.isFile) return@synchronized CloudThemeSubmissionDraft()
        runCatching {
            FileInputStream(file).bufferedReader(Charsets.UTF_8).use { reader ->
                decodeCloudThemeSubmissionDraft(reader.readText())
            }
        }.getOrDefault(CloudThemeSubmissionDraft())
    }

    fun saveDraft(draft: CloudThemeSubmissionDraft) = synchronized(cloudThemeCreatorFileLock) {
        val atomicFile = draftFile()
        val output = atomicFile.startWrite()
        try {
            output.write(encodeCloudThemeSubmissionDraft(draft).toByteArray(Charsets.UTF_8))
            output.flush()
            atomicFile.finishWrite(output)
        } catch (error: Throwable) {
            atomicFile.failWrite(output)
            throw error
        }
    }

    fun clearDraft() = synchronized(cloudThemeCreatorFileLock) {
        draftFile().delete()
    }

    suspend fun inspectPackage(uri: Uri): CloudThemeCreatorPackageInspection =
        withContext(Dispatchers.IO) {
            runCatching {
                appContext.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            val previewResult = previewThemeStorePackage(
                context = appContext,
                source = uri,
                requireCloudSafe = true,
            )
            if (!previewResult.success || previewResult.preview == null) {
                throw previewResult.error ?: IllegalArgumentException("Invalid theme package")
            }
            val digest = MessageDigest.getInstance("SHA-256")
            var copied = 0L
            appContext.contentResolver.openInputStream(uri)?.use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    copied += read
                    require(copied <= CLOUD_THEME_MAX_PACKAGE_BYTES) {
                        "Theme package exceeds the cloud store limit"
                    }
                    digest.update(buffer, 0, read)
                }
            } ?: error("Unable to read the selected theme package")
            require(copied > 0L) { "Theme package is empty" }
            CloudThemeCreatorPackageInspection(
                uriString = uri.toString(),
                displayName = queryDisplayName(uri)
                    ?.take(160)
                    ?.takeIf(String::isNotBlank)
                    ?: "theme.$THEME_STORE_FILE_EXTENSION",
                sha256 = digest.digest().joinToString("") {
                    "%02x".format(it.toInt() and 0xff)
                },
                sizeBytes = copied,
                packageVersion = previewResult.preview.version,
                configuredResourceCount = previewResult.preview.configuredResourceCount,
                authorDisplayName = previewResult.preview.author?.displayName
                    ?.take(64)
                    ?.takeIf(String::isNotBlank),
                warnings = previewResult.warnings,
            )
        }

    suspend fun verifyRemotePackage(
        packageUrl: String,
        expectedSha256: String,
        expectedSizeBytes: Long,
    ): CloudThemeRemotePackageVerification = withContext(Dispatchers.IO) {
        require(expectedSizeBytes in 1..CLOUD_THEME_MAX_PACKAGE_BYTES) {
            "Invalid expected package size"
        }
        require(Regex("[a-fA-F0-9]{64}").matches(expectedSha256)) {
            "Invalid expected package hash"
        }
        val validatedUrl = validateCloudThemeUrl(packageUrl.trim(), allowPackage = true)
        val request = Request.Builder()
            .url(validatedUrl)
            .header("Accept", "application/octet-stream")
            .build()
        val call = client.newCall(request)
        val cancellationWatcher = watchCloudThemeCallCancellation(call)
        try {
            call.execute().use { response ->
                require(
                    response.request.url.scheme == "https" &&
                        isAllowedCloudThemeHost(response.request.url.host)
                ) { "Remote package redirected to an unsupported host" }
                if (!response.isSuccessful) {
                    throw IOException("Remote package HTTP ${response.code}")
                }
                val declaredSize = response.body.contentLength()
                require(declaredSize < 0L || declaredSize == expectedSizeBytes) {
                    "Remote package size does not match the selected package"
                }
                val digest = MessageDigest.getInstance("SHA-256")
                var copied = 0L
                response.body.byteStream().use { input ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        copied += read
                        require(copied <= expectedSizeBytes) {
                            "Remote package exceeds the selected package size"
                        }
                        digest.update(buffer, 0, read)
                    }
                }
                require(copied == expectedSizeBytes) {
                    "Remote package size does not match the selected package"
                }
                val actualSha256 = digest.digest().joinToString("") {
                    "%02x".format(it.toInt() and 0xff)
                }
                require(actualSha256.equals(expectedSha256, ignoreCase = true)) {
                    "Remote package SHA-256 does not match the selected package"
                }
                CloudThemeRemotePackageVerification(
                    sha256 = actualSha256,
                    sizeBytes = copied,
                    verifiedAt = System.currentTimeMillis(),
                )
            }
        } finally {
            cancellationWatcher.cancel()
        }
    }

    suspend fun loadCreatorActivity(githubLogin: String): CloudThemeCreatorActivity =
        withContext(Dispatchers.IO) {
            val github = normalizeCloudThemeGithubLogin(githubLogin)
            val request = Request.Builder()
                .url(
                    "https://$CLOUD_THEME_GITHUB_API_HOST/repos/fixz232/ApkeSU/issues" +
                        "?state=all&creator=$github&per_page=100&sort=updated&direction=desc"
                )
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .build()
            val call = client.newCall(request)
            val cancellationWatcher = watchCloudThemeCallCancellation(call)
            try {
                call.execute().use { response ->
                    require(
                        response.request.url.scheme == "https" &&
                            response.request.url.host.equals(
                                CLOUD_THEME_GITHUB_API_HOST,
                                ignoreCase = true,
                            )
                    ) { "GitHub API redirected to an unsupported host" }
                    if (!response.isSuccessful) {
                        val rateLimited = response.code == 403 &&
                            response.header("X-RateLimit-Remaining") == "0"
                        throw IOException(
                            if (rateLimited) {
                                "GitHub API rate limit reached; try again later"
                            } else {
                                "GitHub issues HTTP ${response.code}"
                            }
                        )
                    }
                    val contentLength = response.body.contentLength()
                    require(
                        contentLength < 0L || contentLength <= CLOUD_THEME_MAX_GITHUB_ISSUES_BYTES
                    ) { "GitHub issue response is too large" }
                    val json = response.body.byteStream().use {
                        it.readCloudThemeBytes(CLOUD_THEME_MAX_GITHUB_ISSUES_BYTES)
                            .toString(Charsets.UTF_8)
                    }
                    parseCloudThemeCreatorActivity(json, github)
                }
            } finally {
                cancellationWatcher.cancel()
            }
        }

    private suspend fun fetchRegistry(
        cachedRegistry: CloudThemeCreatorRegistry?,
    ): CloudThemeCreatorRegistrySnapshot {
        val validatedUrl = validateCloudThemeUrl(registryUrl, allowPackage = false)
        val metadata = metadataPrefs()
        val request = Request.Builder()
            .url(validatedUrl)
            .header("Accept", "application/json")
            .apply {
                metadata.getString(CREATOR_REGISTRY_ETAG_KEY, null)
                    ?.takeIf(String::isNotBlank)
                    ?.let { header("If-None-Match", it) }
                metadata.getString(CREATOR_REGISTRY_LAST_MODIFIED_KEY, null)
                    ?.takeIf(String::isNotBlank)
                    ?.let { header("If-Modified-Since", it) }
            }
            .build()
        val call = client.newCall(request)
        val cancellationWatcher = watchCloudThemeCallCancellation(call)
        try {
            call.execute().use { response ->
                require(
                    response.request.url.scheme == "https" &&
                        isAllowedCloudThemeHost(response.request.url.host)
                ) { "Creator registry redirected to an unsupported host" }
                if (response.code == 304) {
                    val registry = cachedRegistry ?: error("Creator registry cache is missing")
                    val now = System.currentTimeMillis()
                    metadata.edit { putLong(CREATOR_REGISTRY_FETCHED_AT_KEY, now) }
                    return CloudThemeCreatorRegistrySnapshot(
                        registry = registry,
                        source = CloudThemeCreatorRegistrySource.Cache,
                        fetchedAt = now,
                        offline = false,
                    )
                }
                if (!response.isSuccessful) throw IOException("Creator registry HTTP ${response.code}")
                val contentLength = response.body.contentLength()
                require(
                    contentLength < 0L || contentLength <= CLOUD_THEME_MAX_CREATOR_REGISTRY_BYTES
                ) { "Creator registry response is too large" }
                val json = response.body.byteStream().use {
                    it.readCloudThemeBytes(CLOUD_THEME_MAX_CREATOR_REGISTRY_BYTES)
                        .toString(Charsets.UTF_8)
                }
                val registry = parseCloudThemeCreatorRegistry(json)
                writeRegistryCache(json)
                val now = System.currentTimeMillis()
                metadata.edit {
                    response.header("ETag")?.take(512)?.let {
                        putString(CREATOR_REGISTRY_ETAG_KEY, it)
                    } ?: remove(CREATOR_REGISTRY_ETAG_KEY)
                    response.header("Last-Modified")?.take(512)?.let {
                        putString(CREATOR_REGISTRY_LAST_MODIFIED_KEY, it)
                    } ?: remove(CREATOR_REGISTRY_LAST_MODIFIED_KEY)
                    putLong(CREATOR_REGISTRY_FETCHED_AT_KEY, now)
                }
                return CloudThemeCreatorRegistrySnapshot(
                    registry = registry,
                    source = CloudThemeCreatorRegistrySource.Network,
                    fetchedAt = now,
                    offline = false,
                )
            }
        } finally {
            cancellationWatcher.cancel()
        }
    }

    private fun readCachedRegistry(): CloudThemeCreatorRegistry? {
        val file = registryCacheFile().baseFile
        if (!file.isFile) return null
        return runCatching {
            FileInputStream(file).use { input ->
                parseCloudThemeCreatorRegistry(
                    input.readCloudThemeBytes(CLOUD_THEME_MAX_CREATOR_REGISTRY_BYTES)
                        .toString(Charsets.UTF_8)
                )
            }
        }.getOrNull()
    }

    private fun readBundledRegistry(): CloudThemeCreatorRegistry {
        return appContext.assets.open(CREATOR_REGISTRY_ASSET).use { input ->
            parseCloudThemeCreatorRegistry(
                input.readCloudThemeBytes(CLOUD_THEME_MAX_CREATOR_REGISTRY_BYTES)
                    .toString(Charsets.UTF_8)
            )
        }
    }

    private fun writeRegistryCache(json: String) {
        val atomicFile = registryCacheFile()
        val output = atomicFile.startWrite()
        try {
            output.write(json.toByteArray(Charsets.UTF_8))
            output.flush()
            atomicFile.finishWrite(output)
        } catch (error: Throwable) {
            atomicFile.failWrite(output)
            throw error
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        return runCatching {
            appContext.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null,
            )?.use { cursor: Cursor ->
                if (!cursor.moveToFirst()) return@use null
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index < 0) null else cursor.getString(index)
            }
        }.getOrNull()
    }

    private fun metadataPrefs() =
        appContext.getSharedPreferences(CREATOR_METADATA_PREFS, Context.MODE_PRIVATE)

    private fun creatorDirectory(): File =
        File(appContext.filesDir, "cloud-theme-creators").apply { mkdirs() }

    private fun registryCacheFile() = AtomicFile(File(creatorDirectory(), "creators-v1.json"))

    private fun draftFile() = AtomicFile(File(creatorDirectory(), "submission-draft-v1.json"))
}
