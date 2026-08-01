package me.weishu.kernelsu.ui.util

import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

internal const val CLOUD_THEME_CREATOR_REGISTRY_SCHEMA =
    "io.github.fixz.apkesu.theme-creators"
internal const val CLOUD_THEME_CREATOR_REGISTRY_VERSION = 1
internal const val CLOUD_THEME_SUBMISSION_SCHEMA =
    "io.github.fixz.apkesu.theme-submission"
internal const val CLOUD_THEME_SUBMISSION_VERSION = 1
internal const val CLOUD_THEME_DEFAULT_CREATOR_REGISTRY_URL =
    "https://raw.githubusercontent.com/fixz232/ApkeSU-ThemeStore/main/theme-store/creators/v1/creators.json"
internal const val CLOUD_THEME_GITHUB_REPOSITORY_URL =
    "https://github.com/fixz232/ApkeSU-ThemeStore"
internal const val CLOUD_THEME_DEFAULT_COVER_URL =
    "https://raw.githubusercontent.com/fixz232/ApkeSU-ThemeStore/main/theme-store/assets/default-cover.png"
internal const val CLOUD_THEME_CREATOR_REVIEWER = "fixz232"
internal const val CLOUD_THEME_MAX_CREATOR_REGISTRY_BYTES = 512L * 1024L
internal const val CLOUD_THEME_MAX_GITHUB_ISSUES_BYTES = 2L * 1024L * 1024L
internal const val CLOUD_THEME_CREATOR_PICKER_MIME_TYPE = "*/*"

private const val CLOUD_THEME_DRAFT_SCHEMA = "io.github.fixz.apkesu.theme-submission-draft"
private const val CLOUD_THEME_DRAFT_VERSION = 1
private val cloudThemeGithubLoginPattern =
    Regex("^(?!.*--)[A-Za-z0-9](?:[A-Za-z0-9-]{0,37}[A-Za-z0-9])?$")
private val cloudThemeSubmissionIdPattern = Regex("[a-z0-9][a-z0-9._-]{1,79}")
private val cloudThemeSubmissionCategoryPattern = Regex("[a-z0-9][a-z0-9_-]{1,39}")
private val cloudThemeSubmissionHashPattern = Regex("[a-fA-F0-9]{64}")
private val cloudThemeSubmissionLicensePattern = Regex("[A-Za-z0-9.+-]{1,48}")
private val cloudThemeOptionalUrlPlaceholders = setOf(
    "-",
    "--",
    "none",
    "null",
    "n/a",
    "na",
    "\u65e0",
    "\u6ca1\u6709",
    "\u65e0\u9700",
    "\u4e0d\u9002\u7528",
)

data class CloudThemeCreator(
    val github: String,
    val displayName: String,
    val approvedAt: Long,
)

data class CloudThemeCreatorRegistry(
    val generatedAt: Long,
    val reviewer: String,
    val creators: List<CloudThemeCreator>,
) {
    fun creator(github: String): CloudThemeCreator? {
        val normalized = github.trim().lowercase()
        return creators.firstOrNull { it.github == normalized }
    }

    fun isApproved(github: String): Boolean = creator(github) != null
}

enum class CloudThemeCreatorRegistrySource {
    Network,
    Cache,
    Bundled,
}

data class CloudThemeCreatorRegistrySnapshot(
    val registry: CloudThemeCreatorRegistry,
    val source: CloudThemeCreatorRegistrySource,
    val fetchedAt: Long,
    val offline: Boolean,
    val errorMessage: String? = null,
)

enum class CloudThemeCreatorApplicationStatus {
    NotApplied,
    Pending,
    NeedsChanges,
    Rejected,
    RegistryPending,
    Unavailable,
    Approved,
}

enum class CloudThemeSubmissionReviewStatus {
    Pending,
    Approved,
    NeedsChanges,
    Rejected,
    Published,
}

data class CloudThemeSubmissionReview(
    val issueNumber: Int,
    val themeId: String,
    val title: String,
    val url: String,
    val updatedAt: String,
    val status: CloudThemeSubmissionReviewStatus,
)

data class CloudThemeCreatorActivity(
    val githubLogin: String,
    val applicationStatus: CloudThemeCreatorApplicationStatus,
    val applicationUrl: String? = null,
    val submissions: List<CloudThemeSubmissionReview> = emptyList(),
)

data class CloudThemeSubmissionDraft(
    val githubLogin: String = "",
    val authorName: String = "",
    val authorBio: String = "",
    val authorProfileUrl: String = "",
    val authorAvatarUrl: String = "",
    val packageUri: String = "",
    val packageName: String = "",
    val packageSha256: String = "",
    val packageSizeBytes: Long = 0L,
    val packageVersion: Int = 0,
    val packageResourceCount: Int = 0,
    val themeId: String = "",
    val themeName: String = "",
    val description: String = "",
    val categoryId: String = "",
    val categoryName: String = "",
    val tagsText: String = "",
    val versionCodeText: String = "1",
    val versionName: String = "1.0.0",
    val minManagerVersionCodeText: String = "",
    val maxManagerVersionCodeText: String = "",
    val packageUrl: String = "",
    val coverUrl: String = "",
    val screenshotUrlsText: String = "",
    val license: String = "CC-BY-4.0",
    val changelog: String = "",
    val remoteVerifiedUrl: String = "",
    val remoteVerifiedSha256: String = "",
    val remoteVerifiedAt: Long = 0L,
) {
    val hasInspectedPackage: Boolean
        get() = packageUri.isNotBlank() &&
            packageSizeBytes in 1..CLOUD_THEME_MAX_PACKAGE_BYTES &&
            packageVersion in 1..THEME_STORE_VERSION &&
            cloudThemeSubmissionHashPattern.matches(packageSha256)

    val isRemoteVerified: Boolean
        get() = remoteVerifiedAt > 0L &&
            packageUrl.isNotBlank() &&
            remoteVerifiedUrl == packageUrl.trim() &&
            remoteVerifiedSha256.equals(packageSha256, ignoreCase = true)

    fun invalidateRemoteVerification(): CloudThemeSubmissionDraft = copy(
        remoteVerifiedUrl = "",
        remoteVerifiedSha256 = "",
        remoteVerifiedAt = 0L,
    )
}

data class CloudThemeCreatorPackageInspection(
    val uriString: String,
    val displayName: String,
    val sha256: String,
    val sizeBytes: Long,
    val packageVersion: Int,
    val configuredResourceCount: Int,
    val authorDisplayName: String?,
    val warnings: List<ThemeStorePackageWarning>,
)

data class CloudThemeRemotePackageVerification(
    val sha256: String,
    val sizeBytes: Long,
    val verifiedAt: Long,
)

internal fun isValidCloudThemeGithubLogin(value: String): Boolean =
    cloudThemeGithubLoginPattern.matches(value.trim())

internal fun normalizeCloudThemeGithubLogin(value: String): String {
    val normalized = value.trim()
    require(isValidCloudThemeGithubLogin(normalized)) { "Invalid GitHub login" }
    return normalized.lowercase()
}

internal fun canonicalCloudThemePackageFileName(displayName: String): String {
    val trimmed = displayName.trim()
    val baseName = trimmed.substringBeforeLast('.', missingDelimiterValue = trimmed)
    val safeBaseName = buildString {
        baseName.forEach { character ->
            when {
                character in "\\/:*?\"<>|" ||
                    Character.isISOControl(character) -> append('_')
                else -> append(character)
            }
        }
    }.trim().trimEnd('.').take(140).ifBlank { "apkesu-cloud-theme" }
    return "$safeBaseName.$THEME_STORE_FILE_EXTENSION"
}

internal fun validateCloudThemeCreatorPackageUrl(rawUrl: String, githubLogin: String): String {
    val github = normalizeCloudThemeGithubLogin(githubLogin)
    val validated = validateCloudThemeSubmissionUrl(
        rawUrl = rawUrl,
        fieldName = "Theme package URL",
        allowPackage = true,
    )
    val uri = URI(validated)
    require(uri.host.equals("github.com", ignoreCase = true)) {
        "Theme package must use a GitHub Release URL"
    }
    val pathParts = uri.rawPath.orEmpty().split('/')
    require(pathParts.firstOrNull().orEmpty().isEmpty() && pathParts.drop(1).none(String::isEmpty)) {
        "Theme package has an invalid GitHub Release path"
    }
    val segments = pathParts.drop(1)
    require(
        segments.size >= 6 &&
            segments[0].equals(github, ignoreCase = true) &&
            segments[2] == "releases" &&
            segments[3] == "download" &&
            segments.drop(4).none { it == "." || it == ".." }
    ) {
        "Theme package must be uploaded to a Release under the creator's GitHub account"
    }
    return validated
}

internal fun parseCloudThemeCreatorRegistry(json: String): CloudThemeCreatorRegistry {
    require(json.toByteArray(Charsets.UTF_8).size <= CLOUD_THEME_MAX_CREATOR_REGISTRY_BYTES) {
        "Creator registry is too large"
    }
    val root = JSONObject(json)
    require(root.optString("schema") == CLOUD_THEME_CREATOR_REGISTRY_SCHEMA) {
        "Unsupported creator registry"
    }
    require(root.optInt("version", -1) == CLOUD_THEME_CREATOR_REGISTRY_VERSION) {
        "Unsupported creator registry version"
    }
    val generatedAt = root.requiredCreatorLong("generatedAt", 0L)
    val reviewer = normalizeCloudThemeGithubLogin(root.requiredCreatorString("reviewer", 39))
    require(reviewer == CLOUD_THEME_CREATOR_REVIEWER) { "Unexpected creator reviewer" }
    val creatorsJson = root.optJSONArray("creators") ?: error("Creator list is missing")
    require(creatorsJson.length() <= 500) { "Creator list is too large" }
    val creators = buildList {
        for (index in 0 until creatorsJson.length()) {
            val item = creatorsJson.optJSONObject(index) ?: error("Creator $index is invalid")
            require(item.optString("status") == "approved") { "Creator status is invalid" }
            add(
                CloudThemeCreator(
                    github = normalizeCloudThemeGithubLogin(
                        item.requiredCreatorString("github", 39)
                    ),
                    displayName = item.requiredCreatorString("displayName", 64),
                    approvedAt = item.requiredCreatorLong("approvedAt", 0L),
                )
            )
        }
    }
    require(creators.distinctBy { it.github }.size == creators.size) {
        "Creator GitHub logins must be unique"
    }
    return CloudThemeCreatorRegistry(
        generatedAt = generatedAt,
        reviewer = reviewer,
        creators = creators,
    )
}

internal fun buildCloudThemeSubmissionManifest(draft: CloudThemeSubmissionDraft): String {
    val github = normalizeCloudThemeGithubLogin(draft.githubLogin)
    val themeId = draft.themeId.requiredDraftText("Theme ID", 80)
    require(cloudThemeSubmissionIdPattern.matches(themeId)) { "Invalid theme ID" }
    val themeName = draft.themeName.requiredDraftText("Theme name", 80)
    val description = draft.description.requiredDraftText("Description", 1000)
    val categoryId = draft.categoryId.requiredDraftText("Category ID", 40)
    require(cloudThemeSubmissionCategoryPattern.matches(categoryId)) { "Invalid category ID" }
    val categoryName = draft.categoryName.requiredDraftText("Category name", 48)
    val authorName = draft.authorName.requiredDraftText("Creator name", 64)
    val authorBio = draft.authorBio.optionalDraftText("Creator bio", 512)
    val versionCode = draft.versionCodeText.trim().toLongOrNull()
        ?.takeIf { it >= 1L }
        ?: error("Invalid theme version code")
    val versionName = draft.versionName.requiredDraftText("Version name", 40)
    val minimumManagerVersion = 1L
    require(draft.hasInspectedPackage) { "Select and verify a .kstheme package first" }
    val packageUrl = validateCloudThemeCreatorPackageUrl(draft.packageUrl, github)
    require(draft.isRemoteVerified) { "Verify that the remote package matches the local package" }
    val screenshots = draft.screenshotUrlsText
        .lineSequence()
        .map(String::trim)
        .filterNot(String::isCloudThemeOptionalUrlPlaceholder)
        .toList()
    require(screenshots.size <= 8 && screenshots.distinct().size == screenshots.size) {
        "Use at most eight unique screenshot URLs"
    }
    val validatedScreenshots = screenshots.mapIndexed { index, url ->
        validateCloudThemeSubmissionUrl(
            rawUrl = url,
            fieldName = "Screenshot URL ${index + 1}",
            allowPackage = false,
        )
    }
    val coverUrl = draft.coverUrl
        .takeUnless(String::isCloudThemeOptionalUrlPlaceholder)
        ?.let {
            validateCloudThemeSubmissionUrl(
                rawUrl = it,
                fieldName = "Cover image URL",
                allowPackage = false,
            )
        }
        ?: validatedScreenshots.firstOrNull()
        ?: CLOUD_THEME_DEFAULT_COVER_URL
    val tags = draft.tagsText
        .split(Regex("[,，\\n]"))
        .map(String::trim)
        .filter(String::isNotBlank)
        .distinctBy(String::lowercase)
    require(tags.size <= 12 && tags.all { it.length <= 32 }) { "Invalid theme tags" }
    val license = draft.license.requiredDraftText("Asset license", 48)
    require(cloudThemeSubmissionLicensePattern.matches(license)) { "Invalid asset license" }
    val profileUrl = draft.authorProfileUrl
        .takeUnless(String::isCloudThemeOptionalUrlPlaceholder)
        ?: "https://github.com/$github"
    val validatedProfileUrl = validateCloudThemeSubmissionUrl(
        rawUrl = profileUrl,
        fieldName = "Creator profile URL",
        allowPackage = false,
    )
    val validatedAvatarUrl = draft.authorAvatarUrl.trim()
        .takeUnless(String::isCloudThemeOptionalUrlPlaceholder)
        ?.let {
            validateCloudThemeSubmissionUrl(
                rawUrl = it,
                fieldName = "Creator avatar URL",
                allowPackage = false,
            )
        }

    val author = JSONObject()
        .put("github", github)
        .put("name", authorName)
        .put("profileUrl", validatedProfileUrl)
        .put("bio", authorBio)
    validatedAvatarUrl?.let { author.put("avatarUrl", it) }

    val theme = JSONObject()
        .put("id", themeId)
        .put("name", themeName)
        .put("description", description)
        .put("category", JSONObject().put("id", categoryId).put("name", categoryName))
        .put("tags", JSONArray(tags))
        .put("versionCode", versionCode)
        .put("versionName", versionName)
        .put("packageSchema", CLOUD_THEME_PACKAGE_SCHEMA)
        .put("packageVersion", draft.packageVersion)
        .put("minManagerVersionCode", minimumManagerVersion)
        .put("maxManagerVersionCode", JSONObject.NULL)
        .put("coverUrl", coverUrl)
        .put("screenshots", JSONArray(validatedScreenshots))
        .put("packageUrl", packageUrl)
        .put("sha256", draft.packageSha256.lowercase())
        .put("sizeBytes", draft.packageSizeBytes)
        .put("license", license)
        .put("changelog", draft.changelog.optionalDraftText("Changelog", 4000))
        .put("author", author)

    return JSONObject()
        .put("schema", CLOUD_THEME_SUBMISSION_SCHEMA)
        .put("version", CLOUD_THEME_SUBMISSION_VERSION)
        .put("theme", theme)
        .toString(2)
}

private fun String.isCloudThemeOptionalUrlPlaceholder(): Boolean {
    val normalized = trim().lowercase()
    return normalized.isEmpty() || normalized in cloudThemeOptionalUrlPlaceholders
}

private fun validateCloudThemeSubmissionUrl(
    rawUrl: String,
    fieldName: String,
    allowPackage: Boolean,
): String {
    val normalized = rawUrl.trim()
    require(normalized.isNotEmpty()) { "$fieldName is required" }
    return try {
        validateCloudThemeUrl(normalized, allowPackage)
    } catch (error: RuntimeException) {
        throw IllegalArgumentException(
            "$fieldName: ${error.message ?: "invalid URL"}",
            error,
        )
    }
}

internal fun buildCloudThemeCreatorApplicationUrl(
    githubLogin: String,
    displayName: String,
): String {
    val github = normalizeCloudThemeGithubLogin(githubLogin)
    val publicName = displayName.requiredDraftText("Creator name", 64)
    return buildCloudThemeIssueUrl(
        template = "cloud_theme_creator_application.yml",
        title = "[Creator application] $github",
        fields = linkedMapOf(
            "github_login" to github,
            "display_name" to publicName,
        ),
    )
}

internal fun buildCloudThemeSubmissionIssueUrl(
    draft: CloudThemeSubmissionDraft,
    manifest: String = buildCloudThemeSubmissionManifest(draft),
): String {
    val titleName = draft.themeName.requiredDraftText("Theme name", 80)
    return buildCloudThemeIssueUrl(
        template = "cloud_theme_submission.yml",
        title = "[Cloud theme] ${draft.themeId} - $titleName",
        fields = linkedMapOf(
            "theme_id" to draft.themeId.trim(),
            "category" to "${draft.categoryId.trim()} | ${draft.categoryName.trim()}",
            "package_url" to draft.packageUrl.trim(),
            "manifest" to manifest,
        ),
    )
}

internal fun encodeCloudThemeSubmissionDraft(draft: CloudThemeSubmissionDraft): String {
    return JSONObject()
        .put("schema", CLOUD_THEME_DRAFT_SCHEMA)
        .put("version", CLOUD_THEME_DRAFT_VERSION)
        .put("githubLogin", draft.githubLogin)
        .put("authorName", draft.authorName)
        .put("authorBio", draft.authorBio)
        .put("authorProfileUrl", draft.authorProfileUrl)
        .put("authorAvatarUrl", draft.authorAvatarUrl)
        .put("packageUri", draft.packageUri)
        .put("packageName", draft.packageName)
        .put("packageSha256", draft.packageSha256)
        .put("packageSizeBytes", draft.packageSizeBytes)
        .put("packageVersion", draft.packageVersion)
        .put("packageResourceCount", draft.packageResourceCount)
        .put("themeId", draft.themeId)
        .put("themeName", draft.themeName)
        .put("description", draft.description)
        .put("categoryId", draft.categoryId)
        .put("categoryName", draft.categoryName)
        .put("tagsText", draft.tagsText)
        .put("versionCodeText", draft.versionCodeText)
        .put("versionName", draft.versionName)
        .put("minManagerVersionCodeText", draft.minManagerVersionCodeText)
        .put("maxManagerVersionCodeText", draft.maxManagerVersionCodeText)
        .put("packageUrl", draft.packageUrl)
        .put("coverUrl", draft.coverUrl)
        .put("screenshotUrlsText", draft.screenshotUrlsText)
        .put("license", draft.license)
        .put("changelog", draft.changelog)
        .put("remoteVerifiedUrl", draft.remoteVerifiedUrl)
        .put("remoteVerifiedSha256", draft.remoteVerifiedSha256)
        .put("remoteVerifiedAt", draft.remoteVerifiedAt)
        .toString()
}

internal fun decodeCloudThemeSubmissionDraft(json: String): CloudThemeSubmissionDraft {
    require(json.toByteArray(Charsets.UTF_8).size <= 128 * 1024) { "Draft is too large" }
    val root = JSONObject(json)
    require(root.optString("schema") == CLOUD_THEME_DRAFT_SCHEMA) { "Unsupported draft" }
    require(root.optInt("version", -1) == CLOUD_THEME_DRAFT_VERSION) {
        "Unsupported draft version"
    }
    return CloudThemeSubmissionDraft(
        githubLogin = root.safeDraftString("githubLogin", 39),
        authorName = root.safeDraftString("authorName", 64),
        authorBio = root.safeDraftString("authorBio", 512),
        authorProfileUrl = root.safeDraftString("authorProfileUrl", 768),
        authorAvatarUrl = root.safeDraftString("authorAvatarUrl", 768),
        packageUri = root.safeDraftString("packageUri", 2048),
        packageName = root.safeDraftString("packageName", 160),
        packageSha256 = root.safeDraftString("packageSha256", 64),
        packageSizeBytes = root.optLong("packageSizeBytes", 0L).coerceAtLeast(0L),
        packageVersion = root.optInt("packageVersion", 0).coerceIn(0, THEME_STORE_VERSION),
        packageResourceCount = root.optInt("packageResourceCount", 0).coerceAtLeast(0),
        themeId = root.safeDraftString("themeId", 80),
        themeName = root.safeDraftString("themeName", 80),
        description = root.safeDraftString("description", 1000),
        categoryId = root.safeDraftString("categoryId", 40),
        categoryName = root.safeDraftString("categoryName", 48),
        tagsText = root.safeDraftString("tagsText", 512),
        versionCodeText = root.safeDraftString("versionCodeText", 24).ifBlank { "1" },
        versionName = root.safeDraftString("versionName", 40).ifBlank { "1.0.0" },
        minManagerVersionCodeText = root.safeDraftString("minManagerVersionCodeText", 24),
        maxManagerVersionCodeText = root.safeDraftString("maxManagerVersionCodeText", 24),
        packageUrl = root.safeDraftString("packageUrl", 768),
        coverUrl = root.safeDraftString("coverUrl", 768),
        screenshotUrlsText = root.safeDraftString("screenshotUrlsText", 8 * 768 + 8),
        license = root.safeDraftString("license", 48).ifBlank { "CC-BY-4.0" },
        changelog = root.safeDraftString("changelog", 4000),
        remoteVerifiedUrl = root.safeDraftString("remoteVerifiedUrl", 768),
        remoteVerifiedSha256 = root.safeDraftString("remoteVerifiedSha256", 64),
        remoteVerifiedAt = root.optLong("remoteVerifiedAt", 0L).coerceAtLeast(0L),
    )
}

internal fun parseCloudThemeCreatorActivity(
    json: String,
    githubLogin: String,
): CloudThemeCreatorActivity {
    require(json.toByteArray(Charsets.UTF_8).size <= CLOUD_THEME_MAX_GITHUB_ISSUES_BYTES) {
        "GitHub issue response is too large"
    }
    val github = normalizeCloudThemeGithubLogin(githubLogin)
    val issues = JSONArray(json)
    require(issues.length() <= 100) { "Too many GitHub issues" }
    var applicationStatus = CloudThemeCreatorApplicationStatus.NotApplied
    var applicationUrl: String? = null
    var applicationNumber = -1
    val submissions = mutableListOf<CloudThemeSubmissionReview>()

    for (index in 0 until issues.length()) {
        val issue = issues.optJSONObject(index) ?: continue
        if (issue.has("pull_request")) continue
        val author = issue.optJSONObject("user")?.optString("login").orEmpty()
        if (!author.equals(github, ignoreCase = true)) continue
        val number = issue.optInt("number", -1)
        if (number < 1) continue
        val title = issue.optString("title").take(180)
        val url = runCatching {
            validateCloudThemeUrl(issue.optString("html_url"), allowPackage = false)
        }.getOrNull() ?: continue
        val labels = issue.optJSONArray("labels").cloudThemeIssueLabels()
        val closed = issue.optString("state") == "closed"
        val updatedAt = issue.optString("updated_at").take(40)

        if (title.startsWith("[Creator application]")) {
            if (number > applicationNumber) {
                applicationNumber = number
                applicationUrl = url
                val applicationBodyValid =
                    title.equals("[Creator application] $github", ignoreCase = true) &&
                        isValidCloudThemeCreatorApplicationBody(
                            body = issue.optString("body"),
                            githubLogin = github,
                        )
                applicationStatus = when {
                    "creator-active" in labels -> {
                        CloudThemeCreatorApplicationStatus.Approved
                    }
                    "creator-rejected" in labels -> CloudThemeCreatorApplicationStatus.Rejected
                    "creator-needs-changes" in labels -> {
                        CloudThemeCreatorApplicationStatus.NeedsChanges
                    }
                    !applicationBodyValid -> CloudThemeCreatorApplicationStatus.NeedsChanges
                    closed -> CloudThemeCreatorApplicationStatus.Rejected
                    else -> CloudThemeCreatorApplicationStatus.Pending
                }
            }
            continue
        }
        if (!title.startsWith("[Cloud theme]")) continue
        val themeId = parseCloudThemeIssueField(issue.optString("body"), "Theme ID")
            .takeIf { cloudThemeSubmissionIdPattern.matches(it) }
            ?: title.removePrefix("[Cloud theme]").trim().substringBefore(' ').take(80)
        val status = when {
            "theme-published" in labels -> CloudThemeSubmissionReviewStatus.Published
            "theme-rejected" in labels -> CloudThemeSubmissionReviewStatus.Rejected
            "theme-needs-changes" in labels -> CloudThemeSubmissionReviewStatus.NeedsChanges
            "theme-approved" in labels -> CloudThemeSubmissionReviewStatus.Approved
            closed -> CloudThemeSubmissionReviewStatus.Rejected
            else -> CloudThemeSubmissionReviewStatus.Pending
        }
        submissions += CloudThemeSubmissionReview(
            issueNumber = number,
            themeId = themeId,
            title = title.removePrefix("[Cloud theme]").trim().ifBlank { themeId },
            url = url,
            updatedAt = updatedAt,
            status = status,
        )
    }
    return CloudThemeCreatorActivity(
        githubLogin = github,
        applicationStatus = applicationStatus,
        applicationUrl = applicationUrl,
        submissions = submissions.sortedByDescending { it.issueNumber },
    )
}

private fun buildCloudThemeIssueUrl(
    template: String,
    title: String,
    fields: Map<String, String>,
): String {
    val query = buildList {
        add("template" to template)
        add("title" to title)
        addAll(fields.entries.map { it.key to it.value })
    }.joinToString("&") { (key, value) ->
        "${key.urlEncode()}=${value.urlEncode()}"
    }
    return "$CLOUD_THEME_GITHUB_REPOSITORY_URL/issues/new?$query"
}

private fun String.urlEncode(): String =
    URLEncoder.encode(this, StandardCharsets.UTF_8.name()).replace("+", "%20")

private fun String.requiredDraftText(label: String, maximum: Int): String {
    val value = trim()
    require(value.isNotBlank() && value.length <= maximum && !value.hasUnsafeControlCharacters()) {
        "$label is missing or too long"
    }
    return value
}

private fun String.optionalDraftText(label: String, maximum: Int): String {
    val value = trim()
    require(value.length <= maximum && !value.hasUnsafeControlCharacters()) {
        "$label is too long"
    }
    return value
}

private fun String.hasUnsafeControlCharacters(): Boolean =
    any { it.code < 32 && it != '\n' && it != '\t' }

private fun JSONObject.requiredCreatorString(key: String, maximum: Int): String {
    val value = optString(key).trim()
    require(value.isNotEmpty() && value.length <= maximum && !value.hasUnsafeControlCharacters()) {
        "Creator field $key is invalid"
    }
    return value
}

private fun JSONObject.requiredCreatorLong(key: String, minimum: Long): Long {
    require(has(key) && !isNull(key)) { "Creator field $key is missing" }
    val value = optLong(key, Long.MIN_VALUE)
    require(value >= minimum) { "Creator field $key is invalid" }
    return value
}

private fun JSONObject.safeDraftString(key: String, maximum: Int): String {
    val value = optString(key).take(maximum)
    return if (value.hasUnsafeControlCharacters()) "" else value
}

private fun JSONArray?.cloudThemeIssueLabels(): Set<String> {
    if (this == null) return emptySet()
    return buildSet {
        for (index in 0 until length()) {
            optJSONObject(index)?.optString("name")?.trim()?.lowercase()
                ?.takeIf(String::isNotBlank)
                ?.let(::add)
        }
    }
}

private fun parseCloudThemeIssueField(body: String, heading: String): String {
    if (body.length > 128 * 1024) return ""
    val lines = body.lineSequence().toList()
    val headingLine = "### $heading"
    val start = lines.indexOfFirst { it.trim() == headingLine }
    if (start < 0) return ""
    return lines.drop(start + 1)
        .takeWhile { !it.trim().startsWith("### ") }
        .joinToString("\n")
        .trim()
        .take(1024)
}

private fun isValidCloudThemeCreatorApplicationBody(
    body: String,
    githubLogin: String,
): Boolean = runCatching {
    val declaredLogin = normalizeCloudThemeGithubLogin(
        parseCloudThemeIssueField(body, "GitHub login")
    )
    val displayName = parseCloudThemeIssueField(body, "Public creator name")
    val introduction = parseCloudThemeIssueField(body, "Introduction")
    val declarations = parseCloudThemeIssueField(body, "Declarations").lowercase()
    declaredLogin == githubLogin &&
        displayName.isNotBlank() && displayName.length <= 64 &&
        !displayName.hasUnsafeControlCharacters() &&
        introduction.isNotBlank() &&
        declarations.lineSequence().count { it.trimStart().startsWith("- [x]") } >= 3
}.getOrDefault(false)
