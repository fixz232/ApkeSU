package me.weishu.kernelsu.ui.util

import android.content.Context
import android.util.AtomicFile
import androidx.core.content.edit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.ksuApp
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.IOException

private const val CLOUD_THEME_CACHE_FRESH_MS = 15L * 60L * 1000L
private const val CLOUD_THEME_CATALOG_ASSET = "theme-store/catalog-v1.json"
private const val CLOUD_THEME_METADATA_PREFS = "cloud_theme_store_metadata"
private const val CLOUD_THEME_ETAG_KEY = "catalog_etag"
private const val CLOUD_THEME_LAST_MODIFIED_KEY = "catalog_last_modified"
private const val CLOUD_THEME_FETCHED_AT_KEY = "catalog_fetched_at"
private val cloudThemeStateLock = Any()

class CloudThemeRepository(
    context: Context,
    private val client: OkHttpClient = ksuApp.okhttpClient,
    private val catalogUrl: String = CLOUD_THEME_DEFAULT_CATALOG_URL,
) {
    private val appContext = context.applicationContext

    suspend fun loadCatalog(forceRefresh: Boolean = false): CloudThemeCatalogSnapshot =
        withContext(Dispatchers.IO) {
            val cachedCatalog = readCachedCatalog()
            val metadata = metadataPrefs()
            val fetchedAt = metadata.getLong(CLOUD_THEME_FETCHED_AT_KEY, 0L).coerceAtLeast(0L)
            val now = System.currentTimeMillis()
            if (!forceRefresh && cachedCatalog != null && now - fetchedAt < CLOUD_THEME_CACHE_FRESH_MS) {
                return@withContext CloudThemeCatalogSnapshot(
                    catalog = cachedCatalog,
                    source = CloudThemeCatalogSource.Cache,
                    fetchedAt = fetchedAt,
                    offline = false,
                )
            }

            try {
                fetchCatalog(cachedCatalog)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                val fallback = cachedCatalog ?: readBundledCatalog()
                CloudThemeCatalogSnapshot(
                    catalog = fallback,
                    source = if (cachedCatalog != null) {
                        CloudThemeCatalogSource.Cache
                    } else {
                        CloudThemeCatalogSource.Bundled
                    },
                    fetchedAt = fetchedAt,
                    offline = true,
                    errorMessage = error.safeCloudThemeMessage(),
                )
            }
        }

    fun readLocalState(): CloudThemeLocalState = synchronized(cloudThemeStateLock) {
        readLocalStateLocked()
    }

    fun setFavorite(themeId: String, favorite: Boolean): CloudThemeLocalState =
        synchronized(cloudThemeStateLock) {
            require(Regex("[a-z0-9][a-z0-9._-]{1,79}").matches(themeId)) {
                "Invalid cloud theme id"
            }
            val current = readLocalStateLocked()
            val favorites = current.favorites.toMutableSet().apply {
                if (favorite) add(themeId) else remove(themeId)
            }
            val updated = current.copy(favorites = favorites)
            writeLocalStateLocked(updated)
            updated
        }

    fun recordDownload(
        theme: CloudTheme,
        libraryEntryId: String,
        downloadedAt: Long = System.currentTimeMillis(),
    ): CloudThemeLocalState = synchronized(cloudThemeStateLock) {
        val current = readLocalStateLocked()
        val previous = current.records[theme.id]
        val record = CloudThemeLocalRecord(
            themeId = theme.id,
            versionCode = theme.versionCode,
            versionName = theme.versionName,
            sha256 = theme.sha256,
            libraryEntryId = libraryEntryId,
            downloadedAt = downloadedAt,
            appliedVersionCode = previous?.appliedVersionCode,
            appliedSha256 = previous?.appliedSha256,
            appliedLibraryEntryId = previous?.appliedLibraryEntryId,
            appliedAt = previous?.appliedAt,
            rollbackEntryId = previous?.rollbackEntryId,
            rolledBackAt = previous?.rolledBackAt,
        )
        val updated = current.copy(records = current.records + (theme.id to record))
        writeLocalStateLocked(updated)
        updated
    }

    fun recordApplied(
        theme: CloudTheme,
        libraryEntryId: String,
        rollbackEntryId: String,
        appliedAt: Long = System.currentTimeMillis(),
    ): CloudThemeLocalState = synchronized(cloudThemeStateLock) {
        val current = readLocalStateLocked()
        val previous = current.records[theme.id]
        val record = CloudThemeLocalRecord(
            themeId = theme.id,
            versionCode = theme.versionCode,
            versionName = theme.versionName,
            sha256 = theme.sha256,
            libraryEntryId = libraryEntryId,
            downloadedAt = previous?.downloadedAt ?: appliedAt,
            appliedVersionCode = theme.versionCode,
            appliedSha256 = theme.sha256,
            appliedLibraryEntryId = libraryEntryId,
            appliedAt = appliedAt,
            rollbackEntryId = rollbackEntryId,
            rolledBackAt = previous?.rolledBackAt,
        )
        val clearedRecords = current.records.mapValues { (_, item) ->
            item.copy(
                appliedVersionCode = null,
                appliedSha256 = null,
                appliedLibraryEntryId = null,
                appliedAt = null,
                rollbackEntryId = null,
            )
        }
        val updated = current.copy(
            records = clearedRecords + (theme.id to record),
            activeThemeId = theme.id,
            lastRollbackThemeId = theme.id,
        )
        writeLocalStateLocked(updated)
        updated
    }

    fun recordRollback(
        themeId: String,
        rolledBackAt: Long = System.currentTimeMillis(),
    ): CloudThemeLocalState = synchronized(cloudThemeStateLock) {
        val current = readLocalStateLocked()
        if (!current.canRollback(themeId)) return@synchronized current
        val record = current.records[themeId] ?: return@synchronized current
        val updatedRecord = record.copy(
            appliedVersionCode = null,
            appliedSha256 = null,
            appliedLibraryEntryId = null,
            appliedAt = null,
            rollbackEntryId = null,
            rolledBackAt = rolledBackAt,
        )
        val updated = current.copy(
            records = current.records + (themeId to updatedRecord),
            activeThemeId = null,
            lastRollbackThemeId = null,
        )
        writeLocalStateLocked(updated)
        updated
    }

    fun recordExternalThemeApplied(): CloudThemeLocalState = synchronized(cloudThemeStateLock) {
        val current = readLocalStateLocked()
        val updated = clearCloudThemeAppliedState(current)
        if (updated != current) {
            writeLocalStateLocked(updated)
        }
        updated
    }

    private suspend fun fetchCatalog(
        cachedCatalog: CloudThemeCatalog?,
    ): CloudThemeCatalogSnapshot {
        val validatedUrl = validateCloudThemeUrl(catalogUrl, allowPackage = false)
        val metadata = metadataPrefs()
        val request = Request.Builder()
            .url(validatedUrl)
            .header("Accept", "application/json")
            .apply {
                metadata.getString(CLOUD_THEME_ETAG_KEY, null)
                    ?.takeIf { it.isNotBlank() }
                    ?.let { header("If-None-Match", it) }
                metadata.getString(CLOUD_THEME_LAST_MODIFIED_KEY, null)
                    ?.takeIf { it.isNotBlank() }
                    ?.let { header("If-Modified-Since", it) }
            }
            .build()

        val call = client.newCall(request)
        val cancellationWatcher = watchCloudThemeCallCancellation(call)
        try {
            call.execute().use { response ->
                val finalHost = response.request.url.host
                require(response.request.url.scheme == "https" && isAllowedCloudThemeHost(finalHost)) {
                    "Cloud theme catalog redirected to an unsupported host"
                }
                if (response.code == 304) {
                    val catalog = cachedCatalog ?: error("Cloud theme catalog cache is missing")
                    val now = System.currentTimeMillis()
                    metadata.edit { putLong(CLOUD_THEME_FETCHED_AT_KEY, now) }
                    return CloudThemeCatalogSnapshot(
                        catalog = catalog,
                        source = CloudThemeCatalogSource.Cache,
                        fetchedAt = now,
                        offline = false,
                    )
                }
                if (!response.isSuccessful) throw IOException("Cloud theme catalog HTTP ${response.code}")
                val contentLength = response.body.contentLength()
                require(contentLength < 0L || contentLength <= CLOUD_THEME_MAX_CATALOG_BYTES) {
                    "Cloud theme catalog response is too large"
                }
                val bytes = response.body.byteStream().use {
                    it.readCloudThemeBytes(CLOUD_THEME_MAX_CATALOG_BYTES)
                }
                val catalogText = bytes.toString(Charsets.UTF_8)
                val catalog = parseCloudThemeCatalog(catalogText)
                writeCatalogCache(catalogText)
                val now = System.currentTimeMillis()
                metadata.edit {
                    response.header("ETag")?.take(512)?.let {
                        putString(CLOUD_THEME_ETAG_KEY, it)
                    } ?: remove(CLOUD_THEME_ETAG_KEY)
                    response.header("Last-Modified")?.take(512)?.let {
                        putString(CLOUD_THEME_LAST_MODIFIED_KEY, it)
                    } ?: remove(CLOUD_THEME_LAST_MODIFIED_KEY)
                    putLong(CLOUD_THEME_FETCHED_AT_KEY, now)
                }
                return CloudThemeCatalogSnapshot(
                    catalog = catalog,
                    source = CloudThemeCatalogSource.Network,
                    fetchedAt = now,
                    offline = false,
                )
            }
        } finally {
            cancellationWatcher.cancel()
        }
    }

    private fun readCachedCatalog(): CloudThemeCatalog? {
        val file = catalogCacheFile().baseFile
        if (!file.isFile) return null
        return runCatching {
            FileInputStream(file).use { input ->
                parseCloudThemeCatalog(
                    input.readCloudThemeBytes(CLOUD_THEME_MAX_CATALOG_BYTES).toString(Charsets.UTF_8)
                )
            }
        }.getOrNull()
    }

    private fun readBundledCatalog(): CloudThemeCatalog {
        return appContext.assets.open(CLOUD_THEME_CATALOG_ASSET).use { input ->
            parseCloudThemeCatalog(
                input.readCloudThemeBytes(CLOUD_THEME_MAX_CATALOG_BYTES).toString(Charsets.UTF_8)
            )
        }
    }

    private fun writeCatalogCache(json: String) {
        val atomicFile = catalogCacheFile()
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

    private fun readLocalStateLocked(): CloudThemeLocalState {
        val file = localStateFile().baseFile
        if (!file.isFile) return CloudThemeLocalState()
        return runCatching {
            FileInputStream(file).bufferedReader(Charsets.UTF_8).use { reader ->
                decodeCloudThemeLocalState(reader.readText())
            }
        }.getOrDefault(CloudThemeLocalState())
    }

    private fun writeLocalStateLocked(state: CloudThemeLocalState) {
        val atomicFile = localStateFile()
        val output = atomicFile.startWrite()
        try {
            output.write(encodeCloudThemeLocalState(state).toByteArray(Charsets.UTF_8))
            output.flush()
            atomicFile.finishWrite(output)
        } catch (error: Throwable) {
            atomicFile.failWrite(output)
            throw error
        }
    }

    private fun metadataPrefs() =
        appContext.getSharedPreferences(CLOUD_THEME_METADATA_PREFS, Context.MODE_PRIVATE)

    private fun cloudThemeDirectory(): File =
        File(appContext.filesDir, "cloud-theme-store").apply { mkdirs() }

    private fun catalogCacheFile() = AtomicFile(File(cloudThemeDirectory(), "catalog-v1.json"))

    private fun localStateFile() = AtomicFile(File(cloudThemeDirectory(), "state-v1.json"))
}

internal suspend fun watchCloudThemeCallCancellation(call: Call): Job {
    return CoroutineScope(kotlin.coroutines.coroutineContext).launch(
        start = CoroutineStart.UNDISPATCHED,
    ) {
        try {
            awaitCancellation()
        } finally {
            call.cancel()
        }
    }
}

internal fun InputStream.readCloudThemeBytes(maxBytes: Long): ByteArray {
    val output = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0L
    while (true) {
        val read = read(buffer)
        if (read < 0) break
        total += read
        require(total <= maxBytes) { "Cloud theme response is too large" }
        output.write(buffer, 0, read)
    }
    return output.toByteArray()
}

internal fun Throwable.safeCloudThemeMessage(): String {
    return localizedMessage
        ?.trim()
        ?.lineSequence()
        ?.firstOrNull()
        ?.take(240)
        ?.takeIf { it.isNotBlank() }
        ?: javaClass.simpleName
}
