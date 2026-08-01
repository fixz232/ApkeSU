package me.weishu.kernelsu.ui.util

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.ui.component.custom.ComponentStyleKind
import me.weishu.kernelsu.ui.component.custom.CustomCardStyle
import me.weishu.kernelsu.ui.component.custom.CustomSwitchStyle
import java.io.File

suspend fun prepareCardStyleCloudSubmission(
    context: Context,
    style: CustomCardStyle,
    description: String,
    categoryName: String,
): CloudThemeSubmissionDraft = prepareComponentStyleCloudSubmission(
    context = context,
    kind = ComponentStyleKind.Card,
    styleId = style.id,
    styleName = style.name,
    styleAuthor = style.author,
    description = description,
    categoryId = "component-card",
    categoryName = categoryName,
    tags = "component,pixel,card,xiaomi",
    export = { destination -> exportCardComponentStylePackage(context, style, destination) },
)

suspend fun prepareSwitchStyleCloudSubmission(
    context: Context,
    style: CustomSwitchStyle,
    description: String,
    categoryName: String,
): CloudThemeSubmissionDraft = prepareComponentStyleCloudSubmission(
    context = context,
    kind = ComponentStyleKind.Switch,
    styleId = style.id,
    styleName = style.name,
    styleAuthor = style.author,
    description = description,
    categoryId = "component-switch",
    categoryName = categoryName,
    tags = "component,pixel,switch,control",
    export = { destination -> exportSwitchComponentStylePackage(context, style, destination) },
)

private suspend fun prepareComponentStyleCloudSubmission(
    context: Context,
    kind: ComponentStyleKind,
    styleId: String,
    styleName: String,
    styleAuthor: String,
    description: String,
    categoryId: String,
    categoryName: String,
    tags: String,
    export: (Uri) -> ThemeStorePackageResult,
): CloudThemeSubmissionDraft {
    val appContext = context.applicationContext
    val repository = CloudThemeCreatorRepository(appContext)
    val temporary = File(
        appContext.cacheDir,
        "component-submission-${kind.value}-${System.nanoTime()}.$THEME_STORE_FILE_EXTENSION",
    )
    return try {
        val exportResult = withContext(Dispatchers.IO) {
            export(Uri.fromFile(temporary))
        }
        require(exportResult.success) {
            exportResult.error?.message ?: "Unable to create component package"
        }
        require(exportResult.warnings.isEmpty()) {
            "Component package contains unavailable resources"
        }
        val inspection = repository.inspectPackage(Uri.fromFile(temporary))
        val previous = repository.readDraft()
        val packageName = canonicalCloudThemePackageFileName(styleName)
        val draft = buildComponentStyleSubmissionDraft(
            previous = previous,
            inspection = inspection,
            kind = kind,
            styleId = styleId,
            styleName = styleName,
            styleAuthor = styleAuthor,
            description = description,
            categoryId = categoryId,
            categoryName = categoryName,
            tags = tags,
            packageName = packageName,
        )
        repository.saveDraft(draft)
        draft
    } finally {
        temporary.delete()
    }
}

internal fun buildComponentStyleSubmissionDraft(
    previous: CloudThemeSubmissionDraft,
    inspection: CloudThemeCreatorPackageInspection,
    kind: ComponentStyleKind,
    styleId: String,
    styleName: String,
    styleAuthor: String,
    description: String,
    categoryId: String,
    categoryName: String,
    tags: String,
    packageName: String,
): CloudThemeSubmissionDraft = previous.copy(
    authorName = styleAuthor.ifBlank { previous.authorName },
    packageUri = inspection.uriString,
    packageName = packageName,
    packageSha256 = inspection.sha256,
    packageSizeBytes = inspection.sizeBytes,
    packageVersion = inspection.packageVersion,
    packageResourceCount = inspection.configuredResourceCount,
    themeId = cloudComponentStyleId(kind, styleId),
    themeName = styleName,
    description = description,
    categoryId = categoryId,
    categoryName = categoryName,
    tagsText = tags,
    versionCodeText = "1",
    versionName = "1.0.0",
    minManagerVersionCodeText = "1",
    maxManagerVersionCodeText = "",
    packageUrl = "",
    coverUrl = "",
    screenshotUrlsText = "",
    changelog = "",
    remoteVerifiedUrl = "",
    remoteVerifiedSha256 = "",
    remoteVerifiedAt = 0L,
)

internal fun cloudComponentStyleId(kind: ComponentStyleKind, rawId: String): String {
    val prefix = if (kind == ComponentStyleKind.Card) "card" else "switch"
    val normalized = rawId
        .trim()
        .lowercase()
        .replace(Regex("[^a-z0-9._-]+"), "-")
        .trim('-', '.', '_')
        .take(72)
        .ifBlank { "style" }
    val safe = if (normalized.firstOrNull()?.isLetterOrDigit() == true) normalized else "style-$normalized"
    return "$prefix.$safe".take(80)
}
