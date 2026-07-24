package me.weishu.kernelsu.ui.util

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.BuildConfig
import me.weishu.kernelsu.ksuApp
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext

enum class CloudThemeOperationStage {
    Downloading,
    Verifying,
    Importing,
    BackingUp,
    Applying,
    RollingBack,
}

data class CloudThemeOperationProgress(
    val stage: CloudThemeOperationStage,
    val bytesComplete: Long = 0L,
    val totalBytes: Long = 0L,
) {
    val fraction: Float?
        get() = totalBytes.takeIf { it > 0L }
            ?.let { (bytesComplete.toDouble() / it.toDouble()).toFloat().coerceIn(0f, 1f) }
}

data class CloudThemeOperationResult(
    val success: Boolean,
    val libraryEntryId: String? = null,
    val rollbackEntryId: String? = null,
    val rollbackPerformed: Boolean = false,
    val warnings: List<ThemeStorePackageWarning> = emptyList(),
    val error: Throwable? = null,
)

class CloudThemeInstaller(
    context: Context,
    private val repository: CloudThemeRepository,
    private val client: OkHttpClient = ksuApp.okhttpClient,
) {
    private val appContext = context.applicationContext

    suspend fun saveToLibrary(
        theme: CloudTheme,
        onProgress: (CloudThemeOperationProgress) -> Unit = {},
    ): CloudThemeOperationResult {
        return try {
            ensureCompatible(theme)
            val prepared = prepareTheme(theme, onProgress)
            CloudThemeOperationResult(
                success = true,
                libraryEntryId = prepared.libraryEntryId,
                warnings = prepared.warnings,
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            CloudThemeOperationResult(success = false, error = error)
        }
    }

    suspend fun applyTheme(
        theme: CloudTheme,
        onProgress: (CloudThemeOperationProgress) -> Unit = {},
    ): CloudThemeOperationResult {
        return try {
            ensureCompatible(theme)
            val prepared = prepareTheme(theme, onProgress)
            emitProgress(onProgress, CloudThemeOperationStage.BackingUp)
            withContext(NonCancellable) {
                val backup = withContext(Dispatchers.IO) {
                    saveCurrentThemeToLibrary(
                        appContext,
                        "Cloud rollback - ${theme.name}",
                    )
                }
                if (!backup.success || backup.entry == null) {
                    throw backup.error ?: IllegalStateException("Unable to create a rollback backup")
                }

                emitProgress(onProgress, CloudThemeOperationStage.Applying)
                val applied = withContext(Dispatchers.IO) {
                    applyThemeFromLibrary(
                        appContext,
                        prepared.libraryEntryId,
                        clearCloudThemeState = false,
                    )
                }
                val operationError = if (!applied.success) {
                    applied.error ?: IllegalStateException("Unable to apply cloud theme")
                } else {
                    try {
                        withContext(Dispatchers.IO) {
                            repository.recordApplied(
                                theme = theme,
                                libraryEntryId = prepared.libraryEntryId,
                                rollbackEntryId = backup.entry.id,
                            )
                        }
                        null
                    } catch (error: Throwable) {
                        error
                    }
                }
                if (operationError != null) {
                    emitProgress(onProgress, CloudThemeOperationStage.RollingBack)
                    val rollback = withContext(Dispatchers.IO) {
                        applyThemeFromLibrary(
                            appContext,
                            backup.entry.id,
                            clearCloudThemeState = false,
                        )
                    }
                    if (!rollback.success) {
                        rollback.error?.let(operationError::addSuppressed)
                    }
                    return@withContext CloudThemeOperationResult(
                        success = false,
                        libraryEntryId = prepared.libraryEntryId,
                        rollbackEntryId = backup.entry.id,
                        rollbackPerformed = rollback.success,
                        warnings = prepared.warnings + applied.packageResult.orEmptyWarnings(),
                        error = operationError,
                    )
                }
                CloudThemeOperationResult(
                    success = true,
                    libraryEntryId = prepared.libraryEntryId,
                    rollbackEntryId = backup.entry.id,
                    warnings = prepared.warnings + applied.packageResult.orEmptyWarnings(),
                )
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            CloudThemeOperationResult(success = false, error = error)
        }
    }

    suspend fun rollbackTheme(
        themeId: String,
        onProgress: (CloudThemeOperationProgress) -> Unit = {},
    ): CloudThemeOperationResult {
        return try {
            val state = repository.readLocalState()
            require(state.canRollback(themeId)) { "No current rollback backup is available" }
            val record = state.record(themeId)
                ?: error("Cloud theme install record was not found")
            val rollbackEntryId = record.rollbackEntryId
                ?: error("No rollback backup is available")
            val backupExists = withContext(Dispatchers.IO) {
                readThemeLibrary(appContext).any { it.id == rollbackEntryId }
            }
            require(backupExists) { "Rollback backup is missing" }
            emitProgress(onProgress, CloudThemeOperationStage.RollingBack)
            withContext(NonCancellable) {
                val rollback = withContext(Dispatchers.IO) {
                    applyThemeFromLibrary(
                        appContext,
                        rollbackEntryId,
                        clearCloudThemeState = false,
                    )
                }
                if (!rollback.success) {
                    throw rollback.error ?: IllegalStateException("Unable to restore rollback backup")
                }
                withContext(Dispatchers.IO) {
                    repository.recordRollback(themeId)
                }
                CloudThemeOperationResult(
                    success = true,
                    rollbackEntryId = rollbackEntryId,
                    rollbackPerformed = true,
                    warnings = rollback.packageResult.orEmptyWarnings(),
                )
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            CloudThemeOperationResult(success = false, error = error)
        }
    }

    private suspend fun prepareTheme(
        theme: CloudTheme,
        onProgress: (CloudThemeOperationProgress) -> Unit,
    ): PreparedCloudTheme {
        val state = repository.readLocalState()
        val current = state.record(theme.id)
        val existingEntryId = current
            ?.takeIf { it.versionCode == theme.versionCode && it.sha256 == theme.sha256 }
            ?.libraryEntryId
        if (existingEntryId != null) {
            val entryExists = withContext(Dispatchers.IO) {
                readThemeLibrary(appContext).any { it.id == existingEntryId }
            }
            if (entryExists) return PreparedCloudTheme(existingEntryId, emptyList())
        }

        val temporaryFile = downloadThemePackage(theme, onProgress)
        try {
            emitProgress(onProgress, CloudThemeOperationStage.Verifying)
            val validation = withContext(Dispatchers.IO) {
                validateThemeStorePackage(appContext, Uri.fromFile(temporaryFile))
            }
            if (!validation.success) {
                throw validation.error ?: IllegalArgumentException("Cloud theme package validation failed")
            }
            val preview = withContext(Dispatchers.IO) {
                previewThemeStorePackage(
                    context = appContext,
                    source = Uri.fromFile(temporaryFile),
                    requireCloudSafe = true,
                )
            }
            if (!preview.success || preview.preview == null) {
                throw preview.error ?: IllegalArgumentException("Cloud theme package preview failed")
            }
            require(preview.preview.version == theme.packageVersion) {
                "Cloud theme package version does not match its catalog entry"
            }

            emitProgress(onProgress, CloudThemeOperationStage.Importing)
            val imported = withContext(Dispatchers.IO + NonCancellable) {
                val result = importThemeToLibrary(
                    context = appContext,
                    source = Uri.fromFile(temporaryFile),
                    preferredName = "${theme.name} ${theme.versionName}",
                )
                if (!result.success || result.entry == null) {
                    throw result.error ?: IllegalStateException("Unable to save cloud theme")
                }
                repository.recordDownload(theme, result.entry.id)
                result
            }
            return PreparedCloudTheme(
                libraryEntryId = checkNotNull(imported.entry).id,
                warnings = (
                    validation.warnings +
                        preview.warnings +
                        imported.packageResult.orEmptyWarnings()
                    ).distinctBy { it.assetId to it.reason },
            )
        } finally {
            temporaryFile.delete()
        }
    }

    private suspend fun downloadThemePackage(
        theme: CloudTheme,
        onProgress: (CloudThemeOperationProgress) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        val validatedUrl = validateCloudThemeUrl(theme.downloadUrl, allowPackage = true)
        require(theme.sizeBytes in 1..CLOUD_THEME_MAX_PACKAGE_BYTES) {
            "Cloud theme package size is invalid"
        }
        val downloadDir = File(appContext.cacheDir, "cloud-theme-downloads").apply { mkdirs() }
        val temporaryFile = File(
            downloadDir,
            ".${theme.id}-${theme.versionCode}-${System.nanoTime()}.kstheme.part",
        )
        val request = Request.Builder()
            .url(validatedUrl)
            .header("Accept", "application/zip, application/octet-stream")
            .build()
        val call = client.newCall(request)
        val cancellationWatcher = watchCloudThemeCallCancellation(call)
        try {
            emitProgress(onProgress, CloudThemeOperationStage.Downloading, 0L, theme.sizeBytes)
            call.execute().use { response ->
                if (!response.isSuccessful) throw IOException("Cloud theme download HTTP ${response.code}")
                require(
                    response.request.url.scheme == "https" &&
                        isAllowedCloudThemeHost(response.request.url.host)
                ) {
                    "Cloud theme download redirected to an unsupported host"
                }
                val responseLength = response.body.contentLength()
                require(responseLength < 0L || responseLength <= CLOUD_THEME_MAX_PACKAGE_BYTES) {
                    "Cloud theme package response is too large"
                }
                require(responseLength < 0L || responseLength == theme.sizeBytes) {
                    "Cloud theme package byte count does not match the catalog"
                }

                val digest = MessageDigest.getInstance("SHA-256")
                var copied = 0L
                var lastReported = -1
                response.body.byteStream().use { input ->
                    FileOutputStream(temporaryFile).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            coroutineContext.ensureActive()
                            val read = input.read(buffer)
                            if (read < 0) break
                            copied += read
                            require(copied <= CLOUD_THEME_MAX_PACKAGE_BYTES) {
                                "Cloud theme package is too large"
                            }
                            require(copied <= theme.sizeBytes) {
                                "Cloud theme package byte count exceeds the catalog value"
                            }
                            output.write(buffer, 0, read)
                            digest.update(buffer, 0, read)
                            val progress = ((copied * 100L) / theme.sizeBytes).toInt().coerceIn(0, 100)
                            if (progress != lastReported) {
                                lastReported = progress
                                emitProgress(
                                    onProgress,
                                    CloudThemeOperationStage.Downloading,
                                    copied,
                                    theme.sizeBytes,
                                )
                            }
                        }
                        output.flush()
                        output.fd.sync()
                    }
                }
                require(copied == theme.sizeBytes) {
                    "Cloud theme package byte count does not match the catalog"
                }
                val actualHash = digest.digest().joinToString("") { byte ->
                    (byte.toInt() and 0xff).toString(16).padStart(2, '0')
                }
                require(actualHash.equals(theme.sha256, ignoreCase = true)) {
                    "Cloud theme package SHA-256 verification failed"
                }
                require(temporaryFile.isFile && temporaryFile.length() == theme.sizeBytes) {
                    "Cloud theme package was not saved completely"
                }
            }
            temporaryFile
        } catch (error: Throwable) {
            temporaryFile.delete()
            throw error
        } finally {
            cancellationWatcher.cancel()
        }
    }

    private fun ensureCompatible(theme: CloudTheme) {
        require(theme.isCompatible(BuildConfig.VERSION_CODE.toLong())) {
            "This cloud theme is not compatible with the current Manager version"
        }
        require(theme.status == CloudThemePublicationStatus.Published) {
            "This cloud theme is no longer available for installation"
        }
    }

    private suspend fun emitProgress(
        callback: (CloudThemeOperationProgress) -> Unit,
        stage: CloudThemeOperationStage,
        bytesComplete: Long = 0L,
        totalBytes: Long = 0L,
    ) {
        withContext(Dispatchers.Main.immediate) {
            callback(CloudThemeOperationProgress(stage, bytesComplete, totalBytes))
        }
    }
}

private data class PreparedCloudTheme(
    val libraryEntryId: String,
    val warnings: List<ThemeStorePackageWarning>,
)

private fun ThemeStorePackageResult?.orEmptyWarnings(): List<ThemeStorePackageWarning> =
    this?.warnings.orEmpty()
