package me.weishu.kernelsu.ui.util

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ThemeStorePackageTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun extractThemeStoreArchive_extractsValidatedMetadataAndAssets() {
        val archive = createArchive(
            "theme.json" to "{\"schema\":\"test\"}".toByteArray(),
            "assets/cards/home.bin" to byteArrayOf(1, 2, 3, 4),
        )
        val tempDir = temporaryFolder.newFolder("valid")
        val assetsDir = tempDir.resolve("assets").apply { mkdirs() }

        val result = extractThemeStoreArchive(ByteArrayInputStream(archive), tempDir, assetsDir)

        assertEquals("{\"schema\":\"test\"}", result.themeJson)
        assertEquals(4L, result.assetsBytes)
        assertArrayEquals(byteArrayOf(1, 2, 3, 4), assetsDir.resolve("cards/home.bin").readBytes())
    }

    @Test
    fun extractThemeStoreArchive_rejectsTraversalPaths() {
        val archive = createArchive(
            "theme.json" to "{}".toByteArray(),
            "assets/../outside.bin" to byteArrayOf(1),
        )
        val tempDir = temporaryFolder.newFolder("traversal")
        val assetsDir = tempDir.resolve("assets").apply { mkdirs() }

        assertThrows(IllegalArgumentException::class.java) {
            extractThemeStoreArchive(ByteArrayInputStream(archive), tempDir, assetsDir)
        }
    }

    @Test
    fun extractThemeStoreArchive_requiresThemeMetadata() {
        val archive = createArchive("assets/home.bin" to byteArrayOf(1))
        val tempDir = temporaryFolder.newFolder("missing-metadata")
        val assetsDir = tempDir.resolve("assets").apply { mkdirs() }

        assertThrows(IllegalStateException::class.java) {
            extractThemeStoreArchive(ByteArrayInputStream(archive), tempDir, assetsDir)
        }
    }

    @Test
    fun validateThemeStoreArchiveEntryName_rejectsNonPortablePaths() {
        assertEquals(
            "assets/cards/home.png",
            validateThemeStoreArchiveEntryName("assets/cards/home.png"),
        )
        listOf(
            "../theme.json",
            "assets/../private.bin",
            "assets\\private.bin",
            "/assets/private.bin",
            "assets//private.bin",
            "C:/assets/private.bin",
        ).forEach { path ->
            assertThrows(path, IllegalArgumentException::class.java) {
                validateThemeStoreArchiveEntryName(path)
            }
        }
    }

    @Test
    fun validateThemeStoreConfig_acceptsCurrentAndLegacyVersions() {
        listOf(1, 2, 3, 4).forEach { version ->
            val config = JSONObject()
                .put("schema", "io.github.fixz.apkesu.theme")
                .put("version", version)
            if (version >= 3) {
                config
                    .put(
                        "startupSound",
                        JSONObject().put("durationSeconds", 5).put("volume", 1.0),
                    )
                    .put("clickSound", JSONObject().put("volume", 1.0))
                    .put("backgroundMusic", JSONObject().put("volume", 0.35))
                    .put("startupAnimation", JSONObject())
            }
            if (version >= 4) {
                config.put(
                    "author",
                    JSONObject()
                        .put("displayName", "ApkeSU user")
                        .put("realName", "")
                        .put("gender", "unspecified")
                        .put("bio", ""),
                )
            }
            validateThemeStoreConfig(config)
        }
    }

    @Test
    fun validateThemeStoreConfig_rejectsFutureVersion() {
        assertThrows(IllegalArgumentException::class.java) {
            validateThemeStoreConfig(
                JSONObject()
                    .put("schema", "io.github.fixz.apkesu.theme")
                    .put("version", 5)
            )
        }
    }

    @Test
    fun validateThemeStoreConfig_rejectsIncompleteV3AudioState() {
        assertThrows(IllegalArgumentException::class.java) {
            validateThemeStoreConfig(
                JSONObject()
                    .put("schema", "io.github.fixz.apkesu.theme")
                    .put("version", 3)
                    .put("startupSound", JSONObject())
                    .put("startupAnimation", JSONObject())
            )
        }
    }

    @Test
    fun validateEmbeddedThemeStoreAssets_rejectsMissingReferencedFile() {
        val assetsDir = temporaryFolder.newFolder("missing-referenced-asset")
        val config = JSONObject()
            .put(
                "wallpaper",
                JSONObject().put(
                    "asset",
                    JSONObject().put("path", "assets/wallpaper.png")
                )
            )

        assertThrows(IllegalArgumentException::class.java) {
            validateEmbeddedThemeStoreAssets(config, assetsDir)
        }
    }

    @Test
    fun parseThemeStorePackageAuthor_readsV4Metadata() {
        val config = JSONObject()
            .put("version", 4)
            .put(
                "author",
                JSONObject()
                    .put("displayName", "  Theme author  ")
                    .put("realName", "Author Name")
                    .put("gender", "other")
                    .put("bio", "Profile bio"),
            )

        val author = requireNotNull(parseThemeStorePackageAuthor(config))

        assertEquals("Theme author", author.displayName)
        assertEquals("Author Name", author.realName)
        assertEquals(ThemeAuthorGender.Other, author.gender)
        assertEquals("Profile bio", author.bio)
    }

    @Test
    fun countConfiguredThemeStoreResources_countsConfiguredOwnersOnly() {
        val config = JSONObject()
            .put(
                "cards",
                JSONObject()
                    .put("lkm", JSONObject().put("videoUri", "content://card-video"))
                    .put("module", JSONObject()),
            )
            .put("wallpaper", JSONObject().put("asset", JSONObject().put("path", "assets/wallpaper.png")))
            .put("startupSound", JSONObject().put("uri", "content://sound"))
            .put("clickSound", JSONObject())

        assertEquals(3, countConfiguredThemeStoreResources(config))
    }

    private fun createArchive(vararg entries: Pair<String, ByteArray>): ByteArray {
        return ByteArrayOutputStream().use { output ->
            ZipOutputStream(output).use { zip ->
                entries.forEach { (name, bytes) ->
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(bytes)
                    zip.closeEntry()
                }
            }
            output.toByteArray()
        }
    }
}
