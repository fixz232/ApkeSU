package me.weishu.kernelsu.ui.util

import org.json.JSONArray
import org.json.JSONObject
import java.net.URI

internal const val CLOUD_THEME_CATALOG_SCHEMA = "io.github.fixz.apkesu.theme-catalog"
internal const val CLOUD_THEME_CATALOG_VERSION = 1
internal const val CLOUD_THEME_PACKAGE_SCHEMA = "io.github.fixz.apkesu.theme"
internal const val CLOUD_THEME_MAX_PACKAGE_BYTES = 500L * 1024L * 1024L
internal const val CLOUD_THEME_MAX_CATALOG_BYTES = 2L * 1024L * 1024L
internal const val CLOUD_THEME_DEFAULT_CATALOG_URL =
    "https://raw.githubusercontent.com/fixz232/ApkeSU-ThemeStore/main/theme-store/catalog/v1/catalog.json"

private const val CLOUD_THEME_STATE_SCHEMA = "io.github.fixz.apkesu.cloud-theme-state"
private const val CLOUD_THEME_STATE_VERSION = 1
private val CLOUD_THEME_ID_PATTERN = Regex("[a-z0-9][a-z0-9._-]{1,79}")
private val CLOUD_THEME_AUTHOR_ID_PATTERN = Regex("[a-z0-9][a-z0-9._-]{1,79}")
private val CLOUD_THEME_CATEGORY_ID_PATTERN = Regex("[a-z0-9][a-z0-9_-]{1,39}")
private val CLOUD_THEME_HASH_PATTERN = Regex("[a-fA-F0-9]{64}")
private val CLOUD_THEME_LICENSE_PATTERN = Regex("[A-Za-z0-9.+-]{1,48}")
private val CLOUD_THEME_LIBRARY_ID_PATTERN = Regex("[a-zA-Z0-9_-]{1,80}")

data class CloudThemeCategory(
    val id: String,
    val name: String,
)

data class CloudThemeAuthor(
    val id: String,
    val name: String,
    val profileUrl: String?,
    val avatarUrl: String?,
    val bio: String,
)

enum class CloudThemePublicationStatus {
    Published,
    Deprecated,
}

data class CloudTheme(
    val id: String,
    val name: String,
    val author: CloudThemeAuthor,
    val description: String,
    val categoryId: String,
    val tags: List<String>,
    val versionCode: Long,
    val versionName: String,
    val packageSchema: String,
    val packageVersion: Int,
    val minManagerVersionCode: Long,
    val maxManagerVersionCode: Long?,
    val coverUrl: String,
    val screenshotUrls: List<String>,
    val downloadUrl: String,
    val sha256: String,
    val sizeBytes: Long,
    val license: String,
    val changelog: String,
    val publishedAt: Long,
    val status: CloudThemePublicationStatus,
    val featured: Boolean,
    val downloadCount: Long,
) {
    fun isCompatible(managerVersionCode: Long): Boolean {
        return managerVersionCode >= minManagerVersionCode &&
            (maxManagerVersionCode == null || managerVersionCode <= maxManagerVersionCode)
    }
}

data class CloudThemeCatalog(
    val generatedAt: Long,
    val categories: List<CloudThemeCategory>,
    val themes: List<CloudTheme>,
) {
    fun theme(themeId: String): CloudTheme? = themes.firstOrNull { it.id == themeId }

    fun categoryName(categoryId: String): String =
        categories.firstOrNull { it.id == categoryId }?.name ?: categoryId
}

enum class CloudThemeCatalogSource {
    Network,
    Cache,
    Bundled,
}

data class CloudThemeCatalogSnapshot(
    val catalog: CloudThemeCatalog,
    val source: CloudThemeCatalogSource,
    val fetchedAt: Long,
    val offline: Boolean,
    val errorMessage: String? = null,
)

data class CloudThemeLocalRecord(
    val themeId: String,
    val versionCode: Long,
    val versionName: String,
    val sha256: String,
    val libraryEntryId: String,
    val downloadedAt: Long,
    val appliedVersionCode: Long? = null,
    val appliedSha256: String? = null,
    val appliedLibraryEntryId: String? = null,
    val appliedAt: Long? = null,
    val rollbackEntryId: String? = null,
    val rolledBackAt: Long? = null,
)

data class CloudThemeLocalState(
    val favorites: Set<String> = emptySet(),
    val records: Map<String, CloudThemeLocalRecord> = emptyMap(),
    val activeThemeId: String? = null,
    val lastRollbackThemeId: String? = null,
) {
    fun isFavorite(themeId: String): Boolean = themeId in favorites
    fun record(themeId: String): CloudThemeLocalRecord? = records[themeId]
    fun isActive(themeId: String): Boolean = activeThemeId == themeId
    fun canRollback(themeId: String): Boolean =
        lastRollbackThemeId == themeId && records[themeId]?.rollbackEntryId != null
}

internal fun parseCloudThemeCatalog(json: String): CloudThemeCatalog {
    require(json.toByteArray(Charsets.UTF_8).size <= CLOUD_THEME_MAX_CATALOG_BYTES) {
        "Cloud theme catalog is too large"
    }
    val root = JSONObject(json)
    require(root.requiredString("schema", 96) == CLOUD_THEME_CATALOG_SCHEMA) {
        "Unsupported cloud theme catalog"
    }
    require(root.requiredInt("version") == CLOUD_THEME_CATALOG_VERSION) {
        "Unsupported cloud theme catalog version"
    }
    val generatedAt = root.requiredLong("generatedAt", minimum = 0L)
    val categoriesJson = root.requiredArray("categories", maximumSize = 64)
    val categories = buildList {
        for (index in 0 until categoriesJson.length()) {
            val item = categoriesJson.optJSONObject(index)
                ?: error("Cloud theme category $index is invalid")
            val id = item.requiredString("id", 40)
            require(CLOUD_THEME_CATEGORY_ID_PATTERN.matches(id)) {
                "Cloud theme category id is invalid: $id"
            }
            add(CloudThemeCategory(id = id, name = item.requiredString("name", 48)))
        }
    }
    require(categories.distinctBy(CloudThemeCategory::id).size == categories.size) {
        "Cloud theme category ids must be unique"
    }
    val categoryIds = categories.mapTo(mutableSetOf(), CloudThemeCategory::id)

    val themesJson = root.requiredArray("themes", maximumSize = 500)
    val themes = buildList {
        for (index in 0 until themesJson.length()) {
            val item = themesJson.optJSONObject(index)
                ?: error("Cloud theme item $index is invalid")
            add(parseCloudTheme(item, categoryIds))
        }
    }
    require(themes.distinctBy(CloudTheme::id).size == themes.size) {
        "Cloud theme ids must be unique"
    }
    return CloudThemeCatalog(
        generatedAt = generatedAt,
        categories = categories,
        themes = themes,
    )
}

private fun parseCloudTheme(item: JSONObject, categoryIds: Set<String>): CloudTheme {
    val id = item.requiredString("id", 80)
    require(CLOUD_THEME_ID_PATTERN.matches(id)) { "Cloud theme id is invalid: $id" }

    val authorJson = item.optJSONObject("author") ?: error("Cloud theme $id has no author")
    val authorId = authorJson.requiredString("id", 80)
    require(CLOUD_THEME_AUTHOR_ID_PATTERN.matches(authorId)) {
        "Cloud theme author id is invalid: $authorId"
    }
    val profileUrl = authorJson.optionalString("profileUrl", 512)
        ?.let { validateCloudThemeUrl(it, allowPackage = false) }
    val avatarUrl = authorJson.optionalString("avatarUrl", 512)
        ?.let { validateCloudThemeUrl(it, allowPackage = false) }
    val author = CloudThemeAuthor(
        id = authorId,
        name = authorJson.requiredString("name", 64),
        profileUrl = profileUrl,
        avatarUrl = avatarUrl,
        bio = authorJson.optionalString("bio", 512).orEmpty(),
    )

    val categoryId = item.requiredString("category", 40)
    require(categoryId in categoryIds) { "Cloud theme $id references an unknown category" }
    val tagsJson = item.requiredArray("tags", maximumSize = 12)
    val tags = buildList {
        for (index in 0 until tagsJson.length()) {
            val tag = tagsJson.opt(index) as? String
                ?: error("Cloud theme $id has an invalid tag")
            add(sanitizeCatalogText(tag, 32, "tag"))
        }
    }
    require(tags.distinctBy(String::lowercase).size == tags.size) {
        "Cloud theme $id has duplicate tags"
    }

    val versionCode = item.requiredLong("versionCode", minimum = 1L)
    val packageSchema = item.requiredString("packageSchema", 96)
    require(packageSchema == CLOUD_THEME_PACKAGE_SCHEMA) {
        "Cloud theme $id uses an unsupported package schema"
    }
    val packageVersion = item.requiredInt("packageVersion")
    require(packageVersion in 1..THEME_STORE_VERSION) {
        "Cloud theme $id uses an unsupported package version"
    }
    val minManagerVersionCode = item.requiredLong("minManagerVersionCode", minimum = 1L)
    val maxManagerVersionCode = item.optionalLong("maxManagerVersionCode", minimum = 1L)
    require(maxManagerVersionCode == null || maxManagerVersionCode >= minManagerVersionCode) {
        "Cloud theme $id has an invalid Manager version range"
    }

    val coverUrl = validateCloudThemeUrl(
        item.requiredString("coverUrl", 512),
        allowPackage = false,
    )
    val screenshotsJson = item.requiredArray("screenshots", maximumSize = 8)
    val screenshotUrls = buildList {
        for (index in 0 until screenshotsJson.length()) {
            val value = screenshotsJson.opt(index) as? String
                ?: error("Cloud theme $id has an invalid screenshot URL")
            add(validateCloudThemeUrl(sanitizeCatalogText(value, 512, "screenshot URL"), false))
        }
    }
    require(screenshotUrls.distinct().size == screenshotUrls.size) {
        "Cloud theme $id has duplicate screenshots"
    }
    val downloadUrl = validateCloudThemeUrl(
        item.requiredString("downloadUrl", 768),
        allowPackage = true,
    )
    val sha256 = item.requiredString("sha256", 64).lowercase()
    require(CLOUD_THEME_HASH_PATTERN.matches(sha256)) { "Cloud theme $id has an invalid SHA-256" }
    val sizeBytes = item.requiredLong("sizeBytes", minimum = 1L)
    require(sizeBytes <= CLOUD_THEME_MAX_PACKAGE_BYTES) { "Cloud theme $id package is too large" }
    val license = item.requiredString("license", 48)
    require(CLOUD_THEME_LICENSE_PATTERN.matches(license)) {
        "Cloud theme $id has an invalid license identifier"
    }
    val status = when (item.requiredString("status", 20).lowercase()) {
        "published" -> CloudThemePublicationStatus.Published
        "deprecated" -> CloudThemePublicationStatus.Deprecated
        else -> error("Cloud theme $id has an invalid publication status")
    }

    return CloudTheme(
        id = id,
        name = item.requiredString("name", 80),
        author = author,
        description = item.requiredString("description", 1000),
        categoryId = categoryId,
        tags = tags,
        versionCode = versionCode,
        versionName = item.requiredString("versionName", 40),
        packageSchema = packageSchema,
        packageVersion = packageVersion,
        minManagerVersionCode = minManagerVersionCode,
        maxManagerVersionCode = maxManagerVersionCode,
        coverUrl = coverUrl,
        screenshotUrls = screenshotUrls,
        downloadUrl = downloadUrl,
        sha256 = sha256,
        sizeBytes = sizeBytes,
        license = license,
        changelog = item.optionalString("changelog", 4000).orEmpty(),
        publishedAt = item.requiredLong("publishedAt", minimum = 0L),
        status = status,
        featured = item.optionalBoolean("featured", false),
        downloadCount = item.optionalLong("downloadCount", minimum = 0L) ?: 0L,
    )
}

internal fun validateCloudThemeUrl(rawUrl: String, allowPackage: Boolean): String {
    val uri = runCatching { URI(rawUrl) }.getOrElse { error("Cloud theme URL is invalid") }
    require(uri.scheme.equals("https", ignoreCase = true)) { "Cloud theme URL must use HTTPS" }
    require(uri.userInfo == null && uri.fragment == null) { "Cloud theme URL contains unsupported data" }
    val host = uri.host?.lowercase() ?: error("Cloud theme URL has no host")
    require(isAllowedCloudThemeHost(host)) { "Cloud theme URL host is not allowed: $host" }
    require(!uri.path.isNullOrBlank()) { "Cloud theme URL has no path" }
    if (allowPackage) {
        require(uri.path.lowercase().endsWith(".kstheme")) {
            "Cloud theme download must be a .kstheme package"
        }
    }
    return uri.toASCIIString()
}

internal fun isAllowedCloudThemeHost(host: String): Boolean {
    val normalized = host.trim().lowercase()
    return normalized == "github.com" ||
        normalized == "raw.githubusercontent.com" ||
        normalized == "objects.githubusercontent.com" ||
        normalized == "release-assets.githubusercontent.com" ||
        normalized == "githubusercontent.com" ||
        normalized.endsWith(".githubusercontent.com")
}

internal fun encodeCloudThemeLocalState(state: CloudThemeLocalState): String {
    val favorites = JSONArray()
    state.favorites.sorted().forEach(favorites::put)
    val records = JSONArray()
    state.records.values.sortedBy(CloudThemeLocalRecord::themeId).forEach { record ->
        val item = JSONObject()
            .put("themeId", record.themeId)
            .put("versionCode", record.versionCode)
            .put("versionName", record.versionName)
            .put("sha256", record.sha256)
            .put("libraryEntryId", record.libraryEntryId)
            .put("downloadedAt", record.downloadedAt)
        item.putIfNotNull("appliedVersionCode", record.appliedVersionCode)
        item.putIfNotNull("appliedSha256", record.appliedSha256)
        item.putIfNotNull("appliedLibraryEntryId", record.appliedLibraryEntryId)
        item.putIfNotNull("appliedAt", record.appliedAt)
        item.putIfNotNull("rollbackEntryId", record.rollbackEntryId)
        item.putIfNotNull("rolledBackAt", record.rolledBackAt)
        records.put(item)
    }
    val root = JSONObject()
        .put("schema", CLOUD_THEME_STATE_SCHEMA)
        .put("version", CLOUD_THEME_STATE_VERSION)
        .put("favorites", favorites)
        .put("records", records)
    root.putIfNotNull("lastRollbackThemeId", state.lastRollbackThemeId)
    root.putIfNotNull("activeThemeId", state.activeThemeId)
    return root.toString()
}

internal fun decodeCloudThemeLocalState(json: String): CloudThemeLocalState {
    val root = JSONObject(json)
    require(root.optString("schema") == CLOUD_THEME_STATE_SCHEMA) { "Invalid cloud theme state" }
    require(root.optInt("version", 0) == CLOUD_THEME_STATE_VERSION) {
        "Unsupported cloud theme state"
    }
    val favoritesJson = root.optJSONArray("favorites") ?: JSONArray()
    val favorites = buildSet {
        for (index in 0 until favoritesJson.length()) {
            val id = favoritesJson.opt(index) as? String ?: continue
            if (CLOUD_THEME_ID_PATTERN.matches(id)) add(id)
        }
    }
    val recordsJson = root.optJSONArray("records") ?: JSONArray()
    val records = buildMap {
        for (index in 0 until recordsJson.length()) {
            val item = recordsJson.optJSONObject(index) ?: continue
            val themeId = item.optString("themeId")
            val hash = item.optString("sha256").lowercase()
            val libraryEntryId = item.optString("libraryEntryId")
            val versionCode = item.optLong("versionCode", 0L)
            val versionName = item.optString("versionName").trim()
            if (!CLOUD_THEME_ID_PATTERN.matches(themeId) ||
                !CLOUD_THEME_HASH_PATTERN.matches(hash) ||
                !CLOUD_THEME_LIBRARY_ID_PATTERN.matches(libraryEntryId) ||
                versionCode <= 0L || versionName.isBlank()
            ) {
                continue
            }
            val appliedSha = item.optionalStoredString("appliedSha256")
                ?.lowercase()
                ?.takeIf(CLOUD_THEME_HASH_PATTERN::matches)
            val appliedLibraryId = item.optionalStoredString("appliedLibraryEntryId")
                ?.takeIf(CLOUD_THEME_LIBRARY_ID_PATTERN::matches)
            val appliedVersion = item.optionalStoredLong("appliedVersionCode")
                ?.takeIf { it > 0L }
            val completeAppliedState = appliedSha != null && appliedLibraryId != null && appliedVersion != null
            val rollbackEntryId = item.optionalStoredString("rollbackEntryId")
                ?.takeIf(CLOUD_THEME_LIBRARY_ID_PATTERN::matches)
            val record = CloudThemeLocalRecord(
                themeId = themeId,
                versionCode = versionCode,
                versionName = versionName.take(40),
                sha256 = hash,
                libraryEntryId = libraryEntryId,
                downloadedAt = item.optLong("downloadedAt", 0L).coerceAtLeast(0L),
                appliedVersionCode = appliedVersion.takeIf { completeAppliedState },
                appliedSha256 = appliedSha.takeIf { completeAppliedState },
                appliedLibraryEntryId = appliedLibraryId.takeIf { completeAppliedState },
                appliedAt = item.optionalStoredLong("appliedAt")
                    ?.takeIf { completeAppliedState && it >= 0L },
                rollbackEntryId = rollbackEntryId,
                rolledBackAt = item.optionalStoredLong("rolledBackAt")?.takeIf { it >= 0L },
            )
            put(themeId, record)
        }
    }
    val lastRollbackThemeId = root.optionalStoredString("lastRollbackThemeId")
        ?.takeIf { it in records && records[it]?.rollbackEntryId != null }
    val activeThemeId = root.optionalStoredString("activeThemeId")
        ?.takeIf { id ->
            val record = records[id]
            record?.appliedVersionCode != null && record.appliedSha256 != null
        }
    return CloudThemeLocalState(
        favorites = favorites,
        records = records,
        activeThemeId = activeThemeId,
        lastRollbackThemeId = lastRollbackThemeId,
    )
}

internal fun clearCloudThemeAppliedState(state: CloudThemeLocalState): CloudThemeLocalState {
    val clearedRecords = state.records.mapValues { (_, record) ->
        record.copy(
            appliedVersionCode = null,
            appliedSha256 = null,
            appliedLibraryEntryId = null,
            appliedAt = null,
            rollbackEntryId = null,
        )
    }
    return state.copy(
        records = clearedRecords,
        activeThemeId = null,
        lastRollbackThemeId = null,
    )
}

private fun JSONObject.requiredString(key: String, maximumLength: Int): String {
    val value = opt(key) as? String ?: error("Cloud theme catalog is missing $key")
    return sanitizeCatalogText(value, maximumLength, key)
}

private fun JSONObject.optionalString(key: String, maximumLength: Int): String? {
    if (!has(key) || isNull(key)) return null
    val value = opt(key) as? String ?: error("Cloud theme catalog has an invalid $key")
    if (value.isBlank()) return null
    return sanitizeCatalogText(value, maximumLength, key)
}

private fun sanitizeCatalogText(value: String, maximumLength: Int, field: String): String {
    val result = value.trim()
    require(result.isNotBlank()) { "Cloud theme catalog has a blank $field" }
    require(result.length <= maximumLength) { "Cloud theme catalog $field is too long" }
    require(result.none { it.code < 0x20 && it != '\n' && it != '\t' }) {
        "Cloud theme catalog $field contains control characters"
    }
    return result
}

private fun JSONObject.requiredArray(key: String, maximumSize: Int): JSONArray {
    val result = optJSONArray(key) ?: error("Cloud theme catalog is missing $key")
    require(result.length() <= maximumSize) { "Cloud theme catalog has too many $key" }
    return result
}

private fun JSONObject.requiredInt(key: String): Int {
    val value = requiredLong(key, Int.MIN_VALUE.toLong())
    require(value <= Int.MAX_VALUE) { "Cloud theme catalog $key is too large" }
    return value.toInt()
}

private fun JSONObject.requiredLong(key: String, minimum: Long): Long {
    val value = opt(key) ?: error("Cloud theme catalog is missing $key")
    val parsed = when (value) {
        is Byte, is Short, is Int, is Long -> (value as Number).toLong()
        else -> null
    } ?: error("Cloud theme catalog has an invalid $key")
    require(parsed >= minimum) { "Cloud theme catalog has an invalid $key" }
    return parsed
}

private fun JSONObject.optionalLong(key: String, minimum: Long): Long? {
    if (!has(key) || isNull(key)) return null
    return requiredLong(key, minimum)
}

private fun JSONObject.optionalBoolean(key: String, fallback: Boolean): Boolean {
    if (!has(key) || isNull(key)) return fallback
    return opt(key) as? Boolean ?: error("Cloud theme catalog has an invalid $key")
}

private fun JSONObject.optionalStoredString(key: String): String? {
    if (!has(key) || isNull(key)) return null
    return (opt(key) as? String)?.takeIf { it.isNotBlank() }
}

private fun JSONObject.optionalStoredLong(key: String): Long? {
    if (!has(key) || isNull(key)) return null
    val value = opt(key)
    return if (value is Byte || value is Short || value is Int || value is Long) {
        (value as Number).toLong()
    } else {
        null
    }
}

private fun JSONObject.putIfNotNull(key: String, value: Any?) {
    if (value != null) put(key, value)
}
