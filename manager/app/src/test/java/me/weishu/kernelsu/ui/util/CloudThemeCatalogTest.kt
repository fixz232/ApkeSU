package me.weishu.kernelsu.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudThemeCatalogTest {
    @Test
    fun parseCatalog_readsPublishedThemeAndCompatibilityRange() {
        val catalog = parseCloudThemeCatalog(validCatalogJson())

        assertEquals(1, catalog.themes.size)
        val theme = catalog.themes.single()
        assertEquals("aurora-night", theme.id)
        assertEquals("Appearance", catalog.categoryName(theme.categoryId))
        assertTrue(theme.isCompatible(32700L))
        assertFalse(theme.isCompatible(32699L))
        assertFalse(theme.isCompatible(33001L))
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
            "https://github.com/fixz232/ApkeSU/releases/download/theme-1/aurora.kstheme",
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
            "https://raw.githubusercontent.com/fixz232/ApkeSU/ApkeSU/theme-store/media/aurora-1.png"
        val duplicateScreenshots = validCatalogJson().replace(
            "\"screenshots\":[\"$screenshot\"]",
            "\"screenshots\":[\"$screenshot\",\"$screenshot\"]",
        )

        assertThrows(IllegalArgumentException::class.java) {
            parseCloudThemeCatalog(duplicateScreenshots)
        }
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
          "coverUrl":"https://raw.githubusercontent.com/fixz232/ApkeSU/ApkeSU/theme-store/media/aurora.png",
          "screenshots":["https://raw.githubusercontent.com/fixz232/ApkeSU/ApkeSU/theme-store/media/aurora-1.png"],
          "downloadUrl":"https://github.com/fixz232/ApkeSU/releases/download/theme-1/aurora.kstheme",
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
