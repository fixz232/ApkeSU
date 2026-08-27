package me.weishu.kernelsu.ui.util

import com.materialkolor.dynamiccolor.ColorSpec
import me.weishu.kernelsu.ui.component.custom.CustomCardStyle
import me.weishu.kernelsu.ui.component.custom.CustomSwitchSource
import me.weishu.kernelsu.ui.component.custom.CustomSwitchStyle
import me.weishu.kernelsu.ui.theme.ColorMode
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
        (1..THEME_STORE_VERSION).forEach { version ->
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
            if (version >= 5) {
                config.put("audioSettings", AppAudioSettings().toJson())
                config.getJSONObject("startupAnimation")
                    .put("settings", StartupAnimationSettings().toJson())
                config.put("cards", currentCardsConfig())
            }
            validateThemeStoreConfig(config)
        }
    }

    @Test
    fun resolveImportedAudioSettings_enablesTracksForLegacyPackages() {
        val current = AppAudioSettings(
            masterEnabled = false,
            startup = AudioTrackSettings(enabled = false),
            click = AudioTrackSettings(enabled = false),
            background = AudioTrackSettings(enabled = false),
        )

        val resolved = resolveImportedAudioSettings(
            current = current,
            packaged = null,
            startupImported = true,
            clickImported = false,
            backgroundImported = true,
        )

        assertEquals(true, resolved?.masterEnabled)
        assertEquals(true, resolved?.startup?.enabled)
        assertEquals(false, resolved?.click?.enabled)
        assertEquals(true, resolved?.background?.enabled)
    }

    @Test
    fun resolveImportedAudioSettings_preservesPackagedPolicy() {
        val packaged = AppAudioSettings(
            masterEnabled = false,
            startup = AudioTrackSettings(enabled = false),
            background = AudioTrackSettings(enabled = false),
        )

        val resolved = resolveImportedAudioSettings(
            current = AppAudioSettings(),
            packaged = packaged,
            startupImported = true,
            clickImported = false,
            backgroundImported = true,
        )

        assertEquals(packaged.normalized(), resolved)
    }

    @Test
    fun validateThemeStoreConfig_rejectsFutureVersion() {
        assertThrows(IllegalArgumentException::class.java) {
            validateThemeStoreConfig(
                JSONObject()
                    .put("schema", "io.github.fixz.apkesu.theme")
                    .put("version", THEME_STORE_VERSION + 1)
            )
        }
    }

    @Test
    fun themeStoreAppearance_fromJsonKeepsMonetModeAndOpacityConsistent() {
        val fallback = ThemeStoreAppearanceState(
            themeMode = ColorMode.LIGHT.value,
            miuixMonet = false,
            keyColor = 0,
            colorStyle = "TonalSpot",
            colorSpec = "Default",
            monetSurfaceOpacity = 1f,
        )

        val parsed = ThemeStoreAppearanceState.fromJson(
            JSONObject()
                .put("themeMode", ColorMode.DARK.value)
                .put("miuixMonet", true)
                .put("keyColor", 0xFF6750A4.toInt())
                .put("colorStyle", "Fidelity")
                .put("colorSpec", "SPEC_2025")
                .put("monetSurfaceOpacity", 0.72),
            fallback,
        )

        assertEquals(ColorMode.MONET_DARK.value, parsed.themeMode)
        assertTrue(parsed.miuixMonet)
        assertEquals(0.72f, parsed.monetSurfaceOpacity, 0.0001f)
    }

    @Test
    fun themeStoreAppearance_acceptsPixelPetFields() {
        val appearance = ThemeStoreAppearanceState(
            themeMode = ColorMode.DARK.value,
            miuixMonet = false,
            keyColor = 0xFF6750A4.toInt(),
            colorStyle = "TonalSpot",
            colorSpec = ColorSpec.SpecVersion.entries.first().name,
            monetSurfaceOpacity = 0.72f,
            pixelStyle = "pet_companion",
            pixelPetEnabled = true,
        )
        val config = currentThemeConfig().put("appearance", appearance.toJson())

        validateThemeStoreConfig(config)

        val parsed = ThemeStoreAppearanceState.fromJson(appearance.toJson(), appearance.copy(pixelPetEnabled = false))
        assertEquals("pet_companion", parsed.pixelStyle)
        assertTrue(parsed.pixelPetEnabled)
    }

    @Test
    fun validateThemeStoreConfig_rejectsInvalidMonetSurfaceOpacity() {
        val config = currentThemeConfig().put(
            "appearance",
            JSONObject()
                .put("themeMode", ColorMode.MONET_DARK.value)
                .put("miuixMonet", true)
                .put("keyColor", 0)
                .put("colorStyle", "TonalSpot")
                .put("colorSpec", "Default")
                .put("monetSurfaceOpacity", 0.2),
        )

        assertThrows(IllegalArgumentException::class.java) {
            validateThemeStoreConfig(config)
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
    fun validateEmbeddedThemeStoreAssets_validatesNightCardAssets() {
        val assetsDir = temporaryFolder.newFolder("night-card-assets")
        assetsDir.resolve("lkm-night.png").writeBytes(byteArrayOf(1, 2, 3))
        val config = JSONObject().put(
            "cards",
            JSONObject().put(
                "lkm",
                JSONObject().put(
                    "nightAsset",
                    JSONObject().put("path", "assets/lkm-night.png"),
                ),
            ),
        )

        validateEmbeddedThemeStoreAssets(config, assetsDir)
        assetsDir.resolve("lkm-night.png").delete()
        assertThrows(IllegalArgumentException::class.java) {
            validateEmbeddedThemeStoreAssets(config, assetsDir)
        }
    }

    @Test
    fun startupAndAudioSettings_roundTripThroughV5Json() {
        val startup = StartupAnimationSettings(
            scaleMode = StartupAnimationScaleMode.Crop,
            brightness = 0.72f,
            playbackSpeed = 1.4f,
            durationMillis = 7_200L,
            allowTapSkip = false,
        )
        val audio = AppAudioSettings(
            masterEnabled = false,
            background = AudioTrackSettings(loop = true, fadeInMs = 900),
        )

        assertEquals(startup.normalized(), StartupAnimationSettings.fromJson(startup.toJson()))
        assertEquals(audio.normalized(), AppAudioSettings.fromJson(audio.toJson()))
    }

    @Test
    fun startupAnimationSettings_normalizesUntrustedValues() {
        val settings = StartupAnimationSettings(
            backgroundArgb = -1L,
            brightness = Float.NaN,
            playbackSpeed = Float.POSITIVE_INFINITY,
            durationMillis = Long.MAX_VALUE,
            portraitCrop = CustomWallpaperCrop(-4f, Float.NaN, 8f, Float.POSITIVE_INFINITY),
        ).normalized()

        assertEquals(0xFFFFFFFFL, settings.backgroundArgb)
        assertEquals(1f, settings.brightness)
        assertEquals(1f, settings.playbackSpeed)
        assertEquals(MAX_STARTUP_ANIMATION_DURATION_MS, settings.durationMillis)
        assertEquals(CustomWallpaperCrop(0f, 0f, 1f, 1f), settings.portraitCrop)
        assertEquals(DEFAULT_CUSTOM_WALLPAPER_OPACITY, sanitizeCustomWallpaperOpacity(Float.NaN))
        assertEquals(
            DEFAULT_CUSTOM_WALLPAPER_PASSTHROUGH_OPACITY,
            sanitizeCustomWallpaperPassthroughOpacity(Float.POSITIVE_INFINITY),
        )
    }

    @Test
    fun savedAudioAndAnimationLibraries_retainReferencedUris() {
        val audioUri = "content://library/click.ogg"
        val animationUri = "content://library/startup.mp4"
        val scheme = AudioScheme(
            id = "audio",
            name = "Audio",
            startupSoundUri = null,
            startupDurationSeconds = 5,
            startupVolume = 1f,
            clickSoundUri = audioUri,
            clickVolume = 0.8f,
            backgroundMusicUri = null,
            backgroundVolume = 0.35f,
            settings = AppAudioSettings(),
        )
        val preset = StartupAnimationPreset(
            id = "startup",
            name = "Startup",
            uriString = animationUri,
            settings = StartupAnimationSettings(),
        )

        assertTrue(isAudioUriReferencedBySchemes(listOf(scheme), audioUri))
        assertTrue(isStartupAnimationUriReferencedByPresets(listOf(preset), animationUri))
        assertEquals(false, isAudioUriReferencedBySchemes(listOf(scheme), "content://unused"))
        assertEquals(false, isStartupAnimationUriReferencedByPresets(listOf(preset), null))
    }

    @Test
    fun navigationIconPresentation_roundTripsThroughThemeJson() {
        val crop = CustomWallpaperCrop(0.1f, 0.2f, 0.8f, 0.9f)
        val state = CustomNavigationIconState(
            uriString = "file:///theme/home.png",
            crop = crop,
            sizeScale = 1.25f,
            innerPaddingDp = 3f,
            verticalOffsetDp = -2f,
            opacity = 0.72f,
            tintArgb = 0xFFAA44CC,
            mask = CustomNavigationIconMask.RoundedSquare,
            labelOverride = "Start",
        ).normalized()

        val restored = state.presentationToJson().toNavigationIconState(state.uriString, crop)

        assertEquals(state, restored)
    }

    @Test
    fun countConfiguredThemeStoreResources_countsNightOnlyCard() {
        val config = JSONObject().put(
            "cards",
            JSONObject().put(
                "module",
                JSONObject().put("nightAsset", JSONObject().put("path", "assets/module-night.png")),
            ),
        )

        assertEquals(1, countConfiguredThemeStoreResources(config))
    }

    @Test
    fun countConfiguredThemeStoreResources_countsKpmPageBackground() {
        val config = JSONObject().put(
            "pageBackgrounds",
            JSONObject().put(
                CustomPageBackgroundTarget.Kpm.id,
                JSONObject().put("asset", JSONObject().put("path", "assets/kpm.png")),
            ),
        )

        assertEquals(1, countConfiguredThemeStoreResources(config))
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
    fun themeStoreImageSlots_defineIndependentClassicMiuixAndMaterialLkmCards() {
        val homeSlots = ThemeStoreImageSlot.entries.filter { it.group == ThemeStoreImageGroup.Home }

        assertTrue(ThemeStoreImageSlot.ClassicMiuixLkm in homeSlots)
        assertTrue(ThemeStoreImageSlot.MaterialLkm in homeSlots)
        assertEquals(
            ThemeStoreImageSlot.entries.size,
            ThemeStoreImageSlot.entries.map { it.id }.toSet().size,
        )
        val preferenceKeys = ThemeStoreImageSlot.entries.flatMap { it.preferenceKeys }
        assertEquals(preferenceKeys.size, preferenceKeys.toSet().size)
    }

    @Test
    fun layoutSpecificAndSharedHomeCards_selectTheirNightVariant() {
        val dayUri = "content://theme/day.png"
        val nightUri = "content://theme/night.png"
        val nightSettings = MediaVisualSettings(overlayAlpha = 0.61f)
        val state = ThemeStoreImageState(
            uriString = dayUri,
            videoUriString = null,
            crop = DEFAULT_CUSTOM_WALLPAPER_CROP,
            nightUriString = nightUri,
            nightVideoUriString = null,
            nightCrop = CustomWallpaperCrop(0.1f, 0.1f, 0.9f, 0.9f),
            nightVisualSettings = nightSettings,
        )

        listOf(
            ThemeStoreImageSlot.ClassicMiuixLkm,
            ThemeStoreImageSlot.MaterialLkm,
            ThemeStoreImageSlot.Superuser,
            ThemeStoreImageSlot.Module,
            ThemeStoreImageSlot.StatusMonitor,
            ThemeStoreImageSlot.SystemInfo,
            ThemeStoreImageSlot.RebootMenu,
        ).forEach { slot ->
            val active = state.activeVariant(isDark = true, seed = slot.id.hashCode())
            assertEquals(slot.id, nightUri, active.uriString)
            assertEquals(slot.id, nightSettings, active.visualSettings)
        }
    }

    @Test
    fun validateThemeStoreConfig_keepsV5PackagesWithoutLayoutSpecificLkmCardsCompatible() {
        val config = currentThemeConfig()
            .put("version", 5)
        config.getJSONObject("cards")
            .remove(ThemeStoreImageSlot.ClassicMiuixLkm.id)
        config.getJSONObject("cards")
            .remove(ThemeStoreImageSlot.MaterialLkm.id)

        validateThemeStoreConfig(config)
    }

    @Test
    fun validateThemeStoreConfig_requiresEveryV6HomeCardConfiguration() {
        ThemeStoreImageSlot.entries
            .filter { it.group == ThemeStoreImageGroup.Home }
            .forEach { slot ->
                val config = currentThemeConfig()
                config.getJSONObject("cards").remove(slot.id)

                assertThrows(slot.id, IllegalStateException::class.java) {
                    validateThemeStoreConfig(config)
                }
            }
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
    fun homeLayoutIsValidatedCountedAndCloudSanitized() {
        val layout = HomeLayoutState(
            enabled = true,
            items = defaultHomeLayoutItems().map { item ->
                if (item.card == HomeLayoutCard.Lkm) {
                    item.copy(
                        stickers = listOf(
                            HomeLayoutSticker(
                                id = "theme-sticker",
                                uriString = "content://device/sticker.png",
                            ),
                        ),
                    )
                } else {
                    item
                }
            },
        )
        val config = currentThemeConfig().put(
            "homeLayout",
            homeLayoutStateToJson(layout) {
                JSONObject()
                    .put("asset", JSONObject().put("path", "assets/home-layout/sticker.png"))
                    .put("uri", it.uriString)
            },
        )

        validateThemeStoreConfig(config)
        assertEquals(1, countConfiguredThemeStoreResources(JSONObject().put("homeLayout", config.getJSONObject("homeLayout"))))

        sanitizeThemeStoreConfigForCloud(config)
        validateThemeStoreConfigForCloud(config)
        val sticker = config.getJSONObject("homeLayout")
            .getJSONObject("portrait")
            .getJSONArray("items")
            .getJSONObject(0)
            .getJSONArray("stickers")
            .getJSONObject(0)
        assertEquals("", sticker.optString("uri"))
    }

    @Test
    fun embeddedHomeLayoutStickerMustExist() {
        val assetsDir = temporaryFolder.newFolder("home-layout-assets")
        val layout = HomeLayoutState(
            items = defaultHomeLayoutItems().map { item ->
                if (item.card == HomeLayoutCard.Lkm) {
                    item.copy(
                        stickers = listOf(
                            HomeLayoutSticker("sticker", "content://device/sticker.png"),
                        ),
                    )
                } else {
                    item
                }
            },
        )
        val config = currentThemeConfig().put(
            "homeLayout",
            homeLayoutStateToJson(layout) {
                JSONObject().put("asset", JSONObject().put("path", "assets/home-layout/sticker.png"))
            },
        )

        validateThemeStoreConfig(config)
        assertThrows(IllegalArgumentException::class.java) {
            validateEmbeddedThemeStoreAssets(config, assetsDir)
        }
        assetsDir.resolve("home-layout").mkdirs()
        assetsDir.resolve("home-layout/sticker.png").writeBytes(byteArrayOf(1, 2, 3))
        validateEmbeddedThemeStoreAssets(config, assetsDir)
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
    fun dualStateSwitchImagesAreValidatedIndependently() {
        val assetsDir = temporaryFolder.newFolder("component-dual-image-assets")
        val offBytes = "switch off state".toByteArray()
        val onBytes = "switch on state".toByteArray()
        assetsDir.resolve("switch-off.png").writeBytes(offBytes)
        assetsDir.resolve("switch-on.png").writeBytes(onBytes)
        fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        val styleJson = CustomSwitchStyle(
            id = "switch-dual-image-test",
            source = CustomSwitchSource.Image,
            imageSha256 = sha256(offBytes),
            imageOnSha256 = sha256(onBytes),
        ).toJson(includeLocalImageUri = false)
        val config = currentThemeConfig().put(
            "components",
            JSONObject().put(
                "switchStyle",
                JSONObject()
                    .put("style", styleJson)
                    .put("imageAsset", JSONObject().put("path", "assets/switch-off.png"))
                    .put("imageUri", null)
                    .put("imageOnAsset", JSONObject().put("path", "assets/switch-on.png"))
                    .put("imageOnUri", null),
            ),
        )

        validateThemeStoreConfig(config)
        validateEmbeddedThemeStoreAssets(config, assetsDir)

        styleJson.put("image_on_sha256", "c".repeat(64))
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
        assertEquals("", config.getJSONObject("cards").getJSONObject("lkm").optString("nightUri"))
        assertEquals(
            "",
            config.getJSONObject("cards").getJSONObject("install_methods").optString("videoUri"),
        )
        val switchOwner = config.getJSONObject("components").getJSONObject("switchStyle")
        assertEquals("", switchOwner.optString("imageUri"))
        assertEquals("", switchOwner.optString("imageOnUri"))
        assertEquals("", switchOwner.getJSONObject("style").optString("image_uri"))
        assertEquals("", switchOwner.getJSONObject("style").optString("image_on_uri"))
    }

    @Test
    fun sanitizeThemeStoreConfigForCloud_rejectsUnembeddedNightUri() {
        val config = currentThemeConfig()
        config.getJSONObject("cards").getJSONObject("lkm")
            .put("nightUri", "content://device/night.png")

        assertThrows(IllegalArgumentException::class.java) {
            sanitizeThemeStoreConfigForCloud(config)
        }
    }

    @Test
    fun sanitizeThemeStoreConfigForCloud_preservesVideoFrameRate() {
        val config = currentThemeConfig()
            .put("wallpaper", JSONObject().put("videoFrameRate", 165))

        sanitizeThemeStoreConfigForCloud(config)
        validateThemeStoreConfigForCloud(config)

        assertEquals(165, config.getJSONObject("wallpaper").getInt("videoFrameRate"))
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
                            .put("videoUri", JSONObject.NULL)
                            .put("nightAsset", JSONObject().put("path", "assets/lkm-night.png"))
                            .put("nightUri", "content://private/lkm-night.png"),
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
                        .put("imageOnAsset", JSONObject().put("path", "assets/component-on.png"))
                        .put("imageOnUri", "file:///private/outer-on.png")
                        .put(
                            "style",
                            JSONObject()
                                .put("image_uri", "file:///private/inner.png")
                                .put("image_on_uri", "file:///private/inner-on.png"),
                        ),
                ),
            )
    }

    private fun currentThemeConfig(): JSONObject {
        return JSONObject()
            .put("schema", "io.github.fixz.apkesu.theme")
            .put("version", THEME_STORE_VERSION)
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
            .put("audioSettings", AppAudioSettings().toJson())
            .put("startupAnimation", JSONObject().put("settings", StartupAnimationSettings().toJson()))
            .put("cards", currentCardsConfig())
    }

    private fun currentCardsConfig(): JSONObject = JSONObject().apply {
        ThemeStoreImageSlot.entries.forEach { slot ->
            put(
                slot.id,
                JSONObject()
                    .put("visualSettings", MediaVisualSettings().toJson())
                    .put("responsiveCrops", ResponsiveCropSet().toJson())
                    .put("nightVisualSettings", MediaVisualSettings().toJson())
                    .put("nightResponsiveCrops", ResponsiveCropSet().toJson())
                    .put("variantSettings", MediaVariantSettings().toJson()),
            )
        }
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
