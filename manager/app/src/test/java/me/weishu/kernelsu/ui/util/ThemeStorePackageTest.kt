package me.weishu.kernelsu.ui.util

import me.weishu.kernelsu.ui.component.custom.CustomCardStyle
import me.weishu.kernelsu.ui.component.custom.CustomSwitchSource
import me.weishu.kernelsu.ui.component.custom.CustomSwitchStyle
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
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
    fun validateEmbeddedThemeStoreAssets_validatesCustomFontHashAndSize() {
        val assetsDir = temporaryFolder.newFolder("font-assets")
        val fontBytes = byteArrayOf(0, 1, 0, 0, 10, 20, 30, 40)
        val fontFile = assetsDir.resolve("font_custom.ttf").apply { writeBytes(fontBytes) }
        val sha256 = MessageDigest.getInstance("SHA-256")
            .digest(fontBytes)
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        val config = themeConfigWithFont(
            JSONObject()
                .put("preset", "custom")
                .put("name", "Example.ttf")
                .put("asset", JSONObject().put("path", "assets/font_custom.ttf"))
                .put("sha256", sha256)
                .put("sizeBytes", fontFile.length())
        )

        validateThemeStoreConfig(config)
        validateEmbeddedThemeStoreAssets(config, assetsDir)

        config.getJSONObject("font").put("sha256", "b".repeat(64))
        assertThrows(IllegalArgumentException::class.java) {
            validateEmbeddedThemeStoreAssets(config, assetsDir)
        }
    }

    @Test
    fun validateThemeStoreConfig_rejectsCustomFontWithoutEmbeddedAsset() {
        val config = themeConfigWithFont(
            JSONObject()
                .put("preset", "custom")
                .put("name", "Example.ttf")
                .put("asset", null)
                .put("sha256", "a".repeat(64))
                .put("sizeBytes", 8L)
        )

        assertThrows(IllegalArgumentException::class.java) {
            validateThemeStoreConfig(config)
        }
    }

    @Test
    fun countConfiguredThemeStoreResources_countsNonDefaultFont() {
        val config = JSONObject().put(
            "font",
            JSONObject().put("preset", "monospace"),
        )

        assertEquals(1, countConfiguredThemeStoreResources(config))
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
                    .put(
                        "install_options",
                        JSONObject().put("asset", JSONObject().put("path", "assets/options.png")),
                    )
                    .put("module", JSONObject()),
            )
            .put("wallpaper", JSONObject().put("asset", JSONObject().put("path", "assets/wallpaper.png")))
            .put("startupSound", JSONObject().put("uri", "content://sound"))
            .put("clickSound", JSONObject())

        assertEquals(4, countConfiguredThemeStoreResources(config))
    }

    @Test
    fun themeStoreImageSlots_defineThreeVideoCapableInstallCards() {
        val slots = ThemeStoreImageSlot.entries.filter { it.group == ThemeStoreImageGroup.Install }

        assertEquals(
            listOf(
                ThemeStoreImageSlot.InstallImage,
                ThemeStoreImageSlot.InstallMethods,
                ThemeStoreImageSlot.InstallOptions,
            ),
            slots,
        )
        assertTrue(slots.all { !it.videoUriKey.isNullOrBlank() })
    }

    @Test
    fun componentStylesAreValidatedAndCountedAsThemeResources() {
        val config = currentThemeConfig()
            .put(
                "components",
                JSONObject()
                    .put("cardStyle", CustomCardStyle(id = "card-package-test").toJson())
                    .put(
                        "switchStyle",
                        JSONObject()
                            .put(
                                "style",
                                CustomSwitchStyle(id = "switch-package-test")
                                    .toJson(includeLocalImageUri = false),
                            )
                            .put("imageAsset", null)
                            .put("imageUri", null),
                    ),
            )

        validateThemeStoreConfig(config)

        assertEquals(2, countConfiguredThemeStoreResources(config))
    }

    @Test
    fun componentOnlyPackageRequiresExactlyOneStyle() {
        val config = currentThemeConfig()
            .put("packageType", "component")
            .put(
                "components",
                JSONObject()
                    .put("cardStyle", CustomCardStyle(id = "card-only-test").toJson())
                    .put(
                        "switchStyle",
                        JSONObject()
                            .put(
                                "style",
                                CustomSwitchStyle(id = "switch-only-test")
                                    .toJson(includeLocalImageUri = false),
                            )
                            .put("imageAsset", null)
                            .put("imageUri", null),
                    ),
            )

        assertThrows(IllegalArgumentException::class.java) {
            validateThemeStoreConfig(config)
        }
    }

    @Test
    fun imageSwitchRequiresEmbeddedOrLegacyImageReference() {
        val config = currentThemeConfig().put(
            "components",
            JSONObject().put(
                "switchStyle",
                JSONObject()
                    .put(
                        "style",
                        CustomSwitchStyle(
                            id = "switch-image-test",
                            source = CustomSwitchSource.Image,
                            imageSha256 = "a".repeat(64),
                        ).toJson(includeLocalImageUri = false),
                    )
                    .put("imageAsset", null)
                    .put("imageUri", null),
            ),
        )

        assertThrows(IllegalArgumentException::class.java) {
            validateThemeStoreConfig(config)
        }
    }

    @Test
    fun embeddedSwitchImageMustMatchDeclaredHash() {
        val assetsDir = temporaryFolder.newFolder("component-image-assets")
        val imageBytes = "validated component image".toByteArray()
        assetsDir.resolve("component_switch_image.png").writeBytes(imageBytes)
        val expectedHash = MessageDigest.getInstance("SHA-256")
            .digest(imageBytes)
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        val styleJson = CustomSwitchStyle(
            id = "switch-image-hash-test",
            source = CustomSwitchSource.Image,
            imageSha256 = expectedHash,
        ).toJson(includeLocalImageUri = false)
        val config = currentThemeConfig().put(
            "components",
            JSONObject().put(
                "switchStyle",
                JSONObject()
                    .put("style", styleJson)
                    .put(
                        "imageAsset",
                        JSONObject().put("path", "assets/component_switch_image.png"),
                    )
                    .put("imageUri", null),
            ),
        )

        validateThemeStoreConfig(config)
        validateEmbeddedThemeStoreAssets(config, assetsDir)

        styleJson.put("image_sha256", "b".repeat(64))
        assertThrows(IllegalArgumentException::class.java) {
            validateEmbeddedThemeStoreAssets(config, assetsDir)
        }
    }

    @Test
    fun validateThemeStoreConfigForCloud_rejectsPrivateProfileAndDeviceUri() {
        val config = cloudUnsafeConfig()

        assertThrows(IllegalArgumentException::class.java) {
            validateThemeStoreConfigForCloud(config)
        }
    }

    @Test
    fun sanitizeThemeStoreConfigForCloud_removesPrivateAndDeviceSpecificData() {
        val config = cloudUnsafeConfig()

        sanitizeThemeStoreConfigForCloud(config)
        validateThemeStoreConfigForCloud(config)

        val author = config.getJSONObject("author")
        assertEquals("", author.getString("realName"))
        assertEquals("unspecified", author.getString("gender"))
        assertEquals("", config.getJSONObject("cards").getJSONObject("lkm").optString("uri"))
        assertEquals(
            "",
            config.getJSONObject("cards").getJSONObject("install_methods").optString("videoUri"),
        )
        val switchOwner = config.getJSONObject("components").getJSONObject("switchStyle")
        assertEquals("", switchOwner.optString("imageUri"))
        assertEquals("", switchOwner.getJSONObject("style").optString("image_uri"))
    }

    private fun cloudUnsafeConfig(): JSONObject {
        return JSONObject()
            .put("version", 4)
            .put(
                "author",
                JSONObject()
                    .put("displayName", "Creator")
                    .put("realName", "Private Name")
                    .put("gender", "other")
                    .put("bio", "Bio"),
            )
            .put(
                "cards",
                JSONObject()
                    .put(
                        "lkm",
                        JSONObject()
                            .put("asset", JSONObject().put("path", "assets/lkm.png"))
                            .put("uri", "content://private/lkm.png")
                            .put("videoAsset", JSONObject.NULL)
                            .put("videoUri", JSONObject.NULL),
                    )
                    .put(
                        "install_methods",
                        JSONObject()
                            .put("asset", JSONObject.NULL)
                            .put("uri", JSONObject.NULL)
                            .put("videoAsset", JSONObject().put("path", "assets/install.mp4"))
                            .put("videoUri", "content://private/install.mp4"),
                    ),
            )
            .put(
                "components",
                JSONObject().put(
                    "switchStyle",
                    JSONObject()
                        .put("imageAsset", JSONObject().put("path", "assets/component.png"))
                        .put("imageUri", "file:///private/outer.png")
                        .put("style", JSONObject().put("image_uri", "file:///private/inner.png")),
                ),
            )
    }

    private fun currentThemeConfig(): JSONObject {
        return JSONObject()
            .put("schema", "io.github.fixz.apkesu.theme")
            .put("version", 4)
            .put(
                "author",
                JSONObject()
                    .put("displayName", "Creator")
                    .put("realName", "")
                    .put("gender", "unspecified")
                    .put("bio", ""),
            )
            .put("startupSound", JSONObject().put("durationSeconds", 5).put("volume", 1.0))
            .put("clickSound", JSONObject().put("volume", 1.0))
            .put("backgroundMusic", JSONObject().put("volume", 0.35))
            .put("startupAnimation", JSONObject())
    }

    private fun themeConfigWithFont(font: JSONObject): JSONObject {
        return currentThemeConfig()
            .put("font", font)
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
