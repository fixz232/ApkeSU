package me.weishu.kernelsu.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudThemeCatalogTest {
    @Test
    fun parseCatalog_doesNotRestrictDownloadsByManagerVersion() {
        val catalog = parseCloudThemeCatalog(validCatalogJson())

        assertEquals(1, catalog.themes.size)
        val theme = catalog.themes.single()
        assertEquals("aurora-night", theme.id)
        assertEquals("Appearance", catalog.categoryName(theme.categoryId))
        assertTrue(theme.isCompatible(32700L))
        assertTrue(theme.isCompatible(1L))
        assertTrue(theme.isCompatible(Long.MAX_VALUE))
    }

    @Test
    fun parseCatalog_rejectsDuplicateThemeIds() {
        val theme = validThemeJson()
        val duplicate = validCatalogJson().replace(theme, "$theme,\n$theme")

        assertThrows(IllegalArgumentException::class.java) {
            parseCloudThemeCatalog(duplicate)
        }
    }

    @Test
    fun parseCatalog_rejectsNonGithubDownloadHost() {
        val unsafe = validCatalogJson().replace(
            "https://github.com/fixz232/ApkeSU-ThemeStore/releases/download/theme-1/aurora.kstheme",
            "https://example.com/aurora.kstheme",
        )

        assertThrows(IllegalArgumentException::class.java) {
            parseCloudThemeCatalog(unsafe)
        }
    }

    @Test
    fun parseCatalog_rejectsDuplicateTagsIgnoringCase() {
        val duplicateTags = validCatalogJson().replace(
            "\"tags\":[\"dark\",\"aurora\"]",
            "\"tags\":[\"dark\",\"Dark\"]",
        )

        assertThrows(IllegalArgumentException::class.java) {
            parseCloudThemeCatalog(duplicateTags)
        }
    }

    @Test
    fun parseCatalog_rejectsDuplicateScreenshots() {
        val screenshot =
            "https://raw.githubusercontent.com/fixz232/ApkeSU-ThemeStore/main/theme-store/media/aurora-1.png"
        val duplicateScreenshots = validCatalogJson().replace(
            "\"screenshots\":[\"$screenshot\"]",
            "\"screenshots\":[\"$screenshot\",\"$screenshot\"]",
        )

        assertThrows(IllegalArgumentException::class.java) {
            parseCloudThemeCatalog(duplicateScreenshots)
        }
    }

    @Test
    fun parseCatalog_clampsInvalidUsageCounts() {
        val negative = parseCloudThemeCatalog(
            validCatalogJson().replace("\"downloadCount\":42", "\"downloadCount\":-8")
        )
        val overflow = parseCloudThemeCatalog(
            validCatalogJson().replace(
                "\"downloadCount\":42",
                "\"downloadCount\":9223372036854775808",
            )
        )

        assertEquals(0L, negative.themes.single().downloadCount)
        assertEquals(Long.MAX_VALUE, overflow.themes.single().downloadCount)
    }

    @Test
    fun calculateUsageStatistics_sortsDeterministically() {
        val baseCatalog = parseCloudThemeCatalog(validCatalogJson())
        val base = baseCatalog.themes.single()
        val themes = listOf(
            base.copy(id = "older", name = "Older", downloadCount = 20L, publishedAt = 100L),
            base.copy(id = "zeta", name = "Zeta", downloadCount = 20L, publishedAt = 200L),
            base.copy(id = "alpha-b", name = "Alpha", downloadCount = 20L, publishedAt = 200L),
            base.copy(id = "alpha-a", name = "Alpha", downloadCount = 20L, publishedAt = 200L),
            base.copy(id = "popular", name = "Popular", downloadCount = 21L, publishedAt = 50L),
        )

        val statistics = baseCatalog.copy(themes = themes).calculateUsageStatistics()

        assertEquals(
            listOf("popular", "alpha-a", "alpha-b", "zeta", "older"),
            statistics.rankedThemes.map(CloudTheme::id),
        )
    }

    @Test
    fun calculateUsageStatistics_countsPublishedDataAndSaturatesTotal() {
        val baseCatalog = parseCloudThemeCatalog(validCatalogJson())
        val base = baseCatalog.themes.single()
        val secondCategory = CloudThemeCategory("utility", "Utility")
        val themes = listOf(
            base.copy(downloadCount = Long.MAX_VALUE),
            base.copy(
                id = "second-theme",
                author = base.author.copy(id = "second-author", name = "Second Author"),
                categoryId = secondCategory.id,
                downloadCount = 50L,
            ),
            base.copy(id = "negative-theme", downloadCount = -10L),
            base.copy(
                id = "deprecated-theme",
                status = CloudThemePublicationStatus.Deprecated,
                downloadCount = 999L,
            ),
        )

        val statistics = baseCatalog.copy(
            categories = baseCatalog.categories + secondCategory,
            themes = themes,
        ).calculateUsageStatistics()

        assertEquals(Long.MAX_VALUE, statistics.totalUsageCount)
        assertEquals(3, statistics.publishedThemeCount)
        assertEquals(2, statistics.creatorCount)
        assertEquals(2, statistics.categoryCount)
        assertFalse(statistics.rankedThemes.any { it.id == "deprecated-theme" })
        assertEquals("negative-theme", statistics.rankedThemes.last().id)
    }

    @Test
    fun calculateUsageStatistics_handlesEmptyCatalog() {
        val catalog = parseCloudThemeCatalog(validCatalogJson()).copy(themes = emptyList())

        val statistics = catalog.calculateUsageStatistics()

        assertEquals(0L, statistics.totalUsageCount)
        assertEquals(0, statistics.publishedThemeCount)
        assertEquals(0, statistics.creatorCount)
        assertEquals(0, statistics.categoryCount)
        assertTrue(statistics.rankedThemes.isEmpty())
    }

    @Test
    fun cloudThemeState_roundTripsFavoriteAndRollbackMetadata() {
        val state = CloudThemeLocalState(
            favorites = setOf("aurora-night"),
            records = mapOf(
                "aurora-night" to CloudThemeLocalRecord(
                    themeId = "aurora-night",
                    versionCode = 3L,
                    versionName = "1.2.0",
                    sha256 = "a".repeat(64),
                    libraryEntryId = "download-entry",
                    downloadedAt = 100L,
                    appliedVersionCode = 3L,
                    appliedSha256 = "a".repeat(64),
                    appliedLibraryEntryId = "download-entry",
                    appliedAt = 110L,
                    rollbackEntryId = "rollback-entry",
                )
            ),
            activeThemeId = "aurora-night",
            lastRollbackThemeId = "aurora-night",
        )

        val decoded = decodeCloudThemeLocalState(encodeCloudThemeLocalState(state))

        assertEquals(state, decoded)
        assertTrue(decoded.isFavorite("aurora-night"))
        assertEquals("rollback-entry", decoded.record("aurora-night")?.rollbackEntryId)
        assertTrue(decoded.canRollback("aurora-night"))
        assertTrue(decoded.isActive("aurora-night"))
        assertNull(decoded.record("missing"))
    }

    @Test
    fun clearCloudThemeAppliedState_keepsDownloadsAndFavorites() {
        val record = CloudThemeLocalRecord(
            themeId = "aurora-night",
            versionCode = 3L,
            versionName = "1.2.0",
            sha256 = "a".repeat(64),
            libraryEntryId = "download-entry",
            downloadedAt = 100L,
            appliedVersionCode = 3L,
            appliedSha256 = "a".repeat(64),
            appliedLibraryEntryId = "download-entry",
            appliedAt = 110L,
            rollbackEntryId = "rollback-entry",
        )
        val state = CloudThemeLocalState(
            favorites = setOf(record.themeId),
            records = mapOf(record.themeId to record),
            activeThemeId = record.themeId,
            lastRollbackThemeId = record.themeId,
        )

        val cleared = clearCloudThemeAppliedState(state)

        assertTrue(cleared.isFavorite(record.themeId))
        assertEquals("download-entry", cleared.record(record.themeId)?.libraryEntryId)
        assertFalse(cleared.isActive(record.themeId))
        assertFalse(cleared.canRollback(record.themeId))
        assertNull(cleared.record(record.themeId)?.appliedVersionCode)
        assertNull(cleared.record(record.themeId)?.rollbackEntryId)
    }

    private fun validCatalogJson(): String = """
        {
          "schema":"io.github.fixz.apkesu.theme-catalog",
          "version":1,
          "generatedAt":1784800000000,
          "categories":[{"id":"appearance","name":"Appearance"}],
          "themes":[
            ${validThemeJson()}
          ]
        }
    """.trimIndent()

    private fun validThemeJson(): String = """
        {
          "id":"aurora-night",
          "name":"Aurora Night",
          "author":{
            "id":"fixz232",
            "name":"fixz232",
            "profileUrl":"https://github.com/fixz232",
            "avatarUrl":"https://avatars.githubusercontent.com/u/1",
            "bio":"Theme author"
          },
          "description":"A complete test theme.",
          "category":"appearance",
          "tags":["dark","aurora"],
          "versionCode":3,
          "versionName":"1.2.0",
          "packageSchema":"io.github.fixz.apkesu.theme",
          "packageVersion":4,
          "minManagerVersionCode":32700,
          "maxManagerVersionCode":33000,
          "coverUrl":"https://raw.githubusercontent.com/fixz232/ApkeSU-ThemeStore/main/theme-store/media/aurora.png",
          "screenshots":["https://raw.githubusercontent.com/fixz232/ApkeSU-ThemeStore/main/theme-store/media/aurora-1.png"],
          "downloadUrl":"https://github.com/fixz232/ApkeSU-ThemeStore/releases/download/theme-1/aurora.kstheme",
          "sha256":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
          "sizeBytes":1024,
          "license":"CC-BY-4.0",
          "changelog":"Initial release",
          "publishedAt":1784800000000,
          "status":"published",
          "featured":true,
          "downloadCount":42
        }
    """.trimIndent()
}
