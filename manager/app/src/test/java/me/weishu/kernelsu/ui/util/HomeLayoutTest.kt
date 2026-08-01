package me.weishu.kernelsu.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeLayoutTest {
    @Test
    fun sanitizeHomeLayoutItem_clampsGeometry() {
        val item = sanitizeHomeLayoutItem(
            HomeLayoutItem(
                card = HomeLayoutCard.Lkm,
                x = -2f,
                y = 8f,
                width = 2f,
                scale = 2f,
                aspectRatio = Float.NaN,
                height = 20f,
                visible = true,
                zIndex = 99,
            ),
        )

        assertEquals(0f, item.x)
        assertEquals(6f, item.y)
        assertEquals(1f, item.width)
        assertEquals(1f, item.scale)
        assertEquals(1f, item.aspectRatio)
        assertEquals(4f, item.height)
        assertEquals(HomeLayoutCard.entries.lastIndex, item.zIndex)
        assertTrue(item.visible)
    }

    @Test
    fun sanitizeHomeLayoutItem_restoresNonFiniteGeometryAndClearsUnsupportedRatio() {
        val item = sanitizeHomeLayoutItem(
            HomeLayoutItem(
                card = HomeLayoutCard.Superuser,
                x = Float.NaN,
                y = Float.POSITIVE_INFINITY,
                width = Float.NEGATIVE_INFINITY,
                scale = Float.NaN,
                aspectRatio = 1.7f,
                visible = true,
                zIndex = 1,
            ),
        )

        val fallback = defaultHomeLayoutItems().first { it.card == HomeLayoutCard.Superuser }
        assertEquals(fallback.x, item.x)
        assertEquals(fallback.y, item.y)
        assertEquals(fallback.width, item.width)
        assertEquals(fallback.scale, item.scale)
        assertEquals(0f, item.aspectRatio)
        assertEquals(0f, item.height)
    }

    @Test
    fun sanitizeHomeLayoutItem_foldsLegacyScaleIntoWidth() {
        val fallback = defaultHomeLayoutItems().first { it.card == HomeLayoutCard.Module }
        val item = sanitizeHomeLayoutItem(fallback.copy(width = 0.5f, scale = 1.2f))

        assertEquals(0.6f, item.width, 0.0001f)
        assertEquals(1f, item.scale)
    }

    @Test
    fun sanitizeHomeLayoutItem_sanitizesTextAndStickers() {
        val fallback = defaultHomeLayoutItems().first { it.card == HomeLayoutCard.Module }
        val item = sanitizeHomeLayoutItem(
            fallback.copy(
                width = 1f,
                x = 1f,
                customTitle = " x ".repeat(60),
                customSubtitle = " y ".repeat(100),
                textScale = 9f,
                wallpaperFit = HomeLayoutWallpaperFit.Fit,
                stickers = listOf(
                    HomeLayoutSticker(
                        id = "",
                        uriString = "file:///sticker.png",
                        x = -1f,
                        y = 2f,
                        width = 3f,
                        opacity = 0f,
                    ),
                ),
            ),
        )

        assertEquals(0f, item.x)
        assertEquals(80, item.customTitle.length)
        assertEquals(160, item.customSubtitle.length)
        assertEquals(1.25f, item.textScale)
        assertEquals(HomeLayoutWallpaperFit.Fit, item.wallpaperFit)
        assertEquals(1, item.stickers.size)
        assertEquals(0f, item.stickers.single().x)
        assertEquals(1f, item.stickers.single().y)
        assertEquals(1f, item.stickers.single().width)
        assertEquals(0.1f, item.stickers.single().opacity)
        assertTrue(item.stickers.single().id.isNotBlank())
    }

    @Test
    fun jsonRoundTrip_preservesCustomLayoutFields() {
        val source = defaultHomeLayoutItems().map { item ->
            if (item.card == HomeLayoutCard.Lkm) {
                item.copy(
                    customTitle = "Custom root",
                    customSubtitle = "Ready",
                    textScale = 0.9f,
                    wallpaperFit = HomeLayoutWallpaperFit.Stretch,
                    stickers = listOf(
                        HomeLayoutSticker(
                            id = "sticker-1",
                            uriString = "file:///sticker.png",
                            x = 0.2f,
                            y = 0.7f,
                            width = 0.35f,
                            opacity = 0.6f,
                        ),
                    ),
                )
            } else {
                item
            }
        }

        val decoded = decodeHomeLayoutItems(encodeHomeLayoutItems(source))
        val lkm = decoded.first { it.card == HomeLayoutCard.Lkm }

        assertEquals("Custom root", lkm.customTitle)
        assertEquals("Ready", lkm.customSubtitle)
        assertEquals(0.9f, lkm.textScale, 0.0001f)
        assertEquals(HomeLayoutWallpaperFit.Stretch, lkm.wallpaperFit)
        assertEquals(source.first().stickers, lkm.stickers)
    }

    @Test
    fun legacyRecords_stillDecode() {
        val decoded = decodeHomeLayoutItems("lkm|0.25|0.5|0.6|1.0|1.5|1|2|1.2")
        val item = decoded.single()

        assertEquals(HomeLayoutCard.Lkm, item.card)
        assertEquals(0.25f, item.x)
        assertEquals(1.2f, item.height)
        assertEquals("", item.customTitle)
        assertTrue(item.stickers.isEmpty())
    }

    @Test
    fun orientationHelpers_keepPortraitAndLandscapeIndependent() {
        val original = HomeLayoutState(autoSnap = false)
        val changedLandscape = original.landscapeItems.map { item ->
            if (item.card == HomeLayoutCard.Lkm) item.copy(width = 0.7f) else item
        }
        val updated = original.withItemsForOrientation(true, changedLandscape)

        assertEquals(original.items, updated.itemsForOrientation(false))
        assertEquals(changedLandscape, updated.itemsForOrientation(true))
        assertFalse(updated.autoSnap)
    }

    @Test
    fun portableLayoutRoundTrip_keepsBothOrientationsAndStickers() {
        val source = HomeLayoutState(
            enabled = true,
            autoSnap = false,
            autoAvoidOverlap = false,
            items = defaultHomeLayoutItems().map { item ->
                if (item.card == HomeLayoutCard.Lkm) {
                    item.copy(
                        customTitle = "Root ready",
                        stickers = listOf(
                            HomeLayoutSticker(
                                id = "portrait-sticker",
                                uriString = "content://stickers/portrait",
                            ),
                        ),
                    )
                } else {
                    item
                }
            },
            landscapeItems = defaultLandscapeHomeLayoutItems().map { item ->
                if (item.card == HomeLayoutCard.Module) item.copy(width = 0.44f) else item
            },
        )

        val decoded = decodeHomeLayoutState(encodeHomeLayoutState(source))

        assertEquals(source.enabled, decoded?.enabled)
        assertEquals(source.autoSnap, decoded?.autoSnap)
        assertEquals(source.autoAvoidOverlap, decoded?.autoAvoidOverlap)
        assertEquals(source.items, decoded?.items)
        assertEquals(source.landscapeItems, decoded?.landscapeItems)
    }

    @Test
    fun portableLayoutRejectsUnknownSchema() {
        val encoded = encodeHomeLayoutState(HomeLayoutState())
            .replace(HOME_LAYOUT_TRANSFER_SCHEMA, "invalid.schema")

        assertEquals(null, decodeHomeLayoutState(encoded))
    }

    @Test
    fun defaultHomeLayoutItems_containsAllCardsInStableOrder() {
        val defaults = defaultHomeLayoutItems()

        assertEquals(HomeLayoutCard.entries.size, defaults.size)
        assertEquals(HomeLayoutCard.Lkm, defaults[0].card)
        assertEquals(HomeLayoutCard.Superuser, defaults[1].card)
        assertEquals(HomeLayoutCard.Module, defaults[2].card)
        assertEquals(HomeLayoutCard.StatusMonitor, defaults[3].card)
        assertEquals(HomeLayoutCard.SystemInfo, defaults[4].card)
        assertEquals(1f, defaults[0].aspectRatio)
        assertTrue(defaults[1].x > defaults[0].x)
        assertTrue(defaults[2].x > defaults[0].x)
        assertFalse(defaults.any { it.width <= 0f || it.scale <= 0f })
        assertTrue(defaults.all { it.height == 0f })
    }

    @Test
    fun presets_keepEveryCardAndValidGeometry() {
        HomeLayoutPreset.entries.forEach { preset ->
            val items = homeLayoutItemsForPreset(preset)

            assertEquals(HomeLayoutCard.entries.toSet(), items.map { it.card }.toSet())
            assertTrue(items.all { it.visible && it.width in 0.28f..1f })
        }
    }

    @Test
    fun snapHomeLayoutItem_snapsToCenterAndPeerRow() {
        val items = defaultHomeLayoutItems()
        val module = items.first { it.card == HomeLayoutCard.Module }
            .copy(x = 0.47f, y = 0.03f)
        val snapped = snapHomeLayoutItem(module, items)

        assertEquals(0.5f, snapped.x)
        assertEquals(0f, snapped.y)
    }

    @Test
    fun moveHomeLayoutCardLayer_movesOneStepAndKeepsUniqueOrder() {
        val moved = moveHomeLayoutCardLayer(
            defaultHomeLayoutItems(),
            HomeLayoutCard.Superuser,
            direction = 1,
        )

        assertEquals(2, moved.first { it.card == HomeLayoutCard.Superuser }.zIndex)
        assertEquals(1, moved.first { it.card == HomeLayoutCard.Module }.zIndex)
        assertEquals(HomeLayoutCard.entries.indices.toSet(), moved.map { it.zIndex }.toSet())
    }

    @Test
    fun moveHomeLayoutItem_usesCanvasCoordinatesForWideCards() {
        val item = defaultHomeLayoutItems()
            .first { it.card == HomeLayoutCard.StatusMonitor }
            .copy(width = 0.9f, x = 0.5f, y = 1f)

        val moved = moveHomeLayoutItem(
            item = item,
            horizontalDelta = 0.04f,
            verticalDeltaRows = 0.2f,
        )

        val oldLeft = (1f - item.width) * item.x
        val newLeft = (1f - moved.width) * moved.x
        assertEquals(oldLeft + 0.04f, newLeft, 0.0001f)
        assertEquals(1.2f, moved.y, 0.0001f)
    }

    @Test
    fun resizeFromLeft_keepsRightEdgeAnchored() {
        val item = defaultHomeLayoutItems().first { it.card == HomeLayoutCard.Superuser }
        val oldRight = (1f - item.width) * item.x + item.width
        val resized = resizeHomeLayoutItem(
            item = item,
            edge = HomeLayoutResizeEdge.Left,
            horizontalDelta = -0.10f,
            verticalDeltaRows = 0f,
            renderedHeightRows = 0.62f,
        )
        val newRight = (1f - resized.width) * resized.x + resized.width

        assertEquals(oldRight, newRight, 0.0001f)
        assertEquals(item.width + 0.10f, resized.width, 0.0001f)
    }

    @Test
    fun resizeFromRight_keepsLeftEdgeAnchored() {
        val item = defaultHomeLayoutItems().first { it.card == HomeLayoutCard.Lkm }
        val oldLeft = (1f - item.width) * item.x
        val resized = resizeHomeLayoutItem(
            item = item,
            edge = HomeLayoutResizeEdge.Right,
            horizontalDelta = 0.12f,
            verticalDeltaRows = 0f,
            renderedHeightRows = 1f,
        )
        val newLeft = (1f - resized.width) * resized.x

        assertEquals(oldLeft, newLeft, 0.0001f)
        assertEquals(item.width + 0.12f, resized.width, 0.0001f)
    }

    @Test
    fun fullWidthCard_canShrinkFromEitherHorizontalEdge() {
        val item = defaultHomeLayoutItems().first { it.card == HomeLayoutCard.StatusMonitor }
        val fromLeft = resizeHomeLayoutItem(
            item = item,
            edge = HomeLayoutResizeEdge.Left,
            horizontalDelta = 0.15f,
            verticalDeltaRows = 0f,
            renderedHeightRows = 1f,
        )
        val fromRight = resizeHomeLayoutItem(
            item = item,
            edge = HomeLayoutResizeEdge.Right,
            horizontalDelta = -0.15f,
            verticalDeltaRows = 0f,
            renderedHeightRows = 1f,
        )

        assertEquals(0.85f, fromLeft.width, 0.0001f)
        assertEquals(1f, fromLeft.x, 0.0001f)
        assertEquals(0.85f, fromRight.width, 0.0001f)
        assertEquals(0f, fromRight.x, 0.0001f)
    }

    @Test
    fun fullWidthCard_sanitizationRemovesMeaninglessHorizontalOffset() {
        val item = defaultHomeLayoutItems()
            .first { it.card == HomeLayoutCard.StatusMonitor }
            .copy(x = 0.8f)

        assertEquals(0f, sanitizeHomeLayoutItem(item).x)
    }

    @Test
    fun resizeFromTop_keepsBottomEdgeAnchored() {
        val item = defaultHomeLayoutItems().first { it.card == HomeLayoutCard.StatusMonitor }
            .copy(y = 1.5f, height = 1f)
        val oldBottom = item.y + item.height
        val resized = resizeHomeLayoutItem(
            item = item,
            edge = HomeLayoutResizeEdge.Top,
            horizontalDelta = 0f,
            verticalDeltaRows = -0.25f,
            renderedHeightRows = 1f,
        )

        assertEquals(oldBottom, resized.y + resized.height, 0.0001f)
        assertEquals(1.25f, resized.height, 0.0001f)
    }

    @Test
    fun resizeFromBottom_convertsAutomaticHeightToCustomHeight() {
        val item = defaultHomeLayoutItems().first { it.card == HomeLayoutCard.Module }
        val resized = resizeHomeLayoutItem(
            item = item,
            edge = HomeLayoutResizeEdge.Bottom,
            horizontalDelta = 0f,
            verticalDeltaRows = 0.2f,
            renderedHeightRows = 0.7f,
        )

        assertEquals(0.9f, resized.height, 0.0001f)
        assertEquals(item.y, resized.y, 0.0001f)
    }

    @Test
    fun resolveCollisions_writesStableVerticalPositions() {
        val items = defaultHomeLayoutItems().map { item ->
            when (item.card) {
                HomeLayoutCard.Superuser -> item.copy(x = 0f, y = 0f, width = 0.48f)
                HomeLayoutCard.Module -> item.copy(x = 0f, y = 0.1f, width = 0.48f)
                else -> item.copy(visible = false)
            }
        }

        val resolved = resolveHomeLayoutCollisions(items)
        val superuser = resolved.first { it.card == HomeLayoutCard.Superuser }
        val module = resolved.first { it.card == HomeLayoutCard.Module }

        assertTrue(module.y >= superuser.y + suggestedHomeLayoutHeight(superuser.card))
        assertEquals(resolved, resolveHomeLayoutCollisions(resolved))
    }

    @Test
    fun autoArrangeHomeLayoutItems_preservesCardsAndRemovesSimpleOverlap() {
        val items = defaultHomeLayoutItems().map { item ->
            when (item.card) {
                HomeLayoutCard.Lkm,
                HomeLayoutCard.Superuser -> item.copy(x = 0f, y = 0f, width = 0.48f)
                else -> item.copy(visible = false)
            }
        }

        val arranged = autoArrangeHomeLayoutItems(items)

        assertEquals(HomeLayoutCard.entries.toSet(), arranged.map { it.card }.toSet())
        val visible = arranged.filter { it.visible }
        assertEquals(arranged, autoArrangeHomeLayoutItems(arranged))
        assertFalse(
            visible[0].y == visible[1].y &&
                ((1f - visible[0].width) * visible[0].x) ==
                ((1f - visible[1].width) * visible[1].x),
        )
    }

    @Test
    fun landscapePresets_keepEveryCardAndUseWideDefault() {
        val landscape = homeLayoutItemsForPreset(HomeLayoutPreset.DualColumn, isLandscape = true)

        assertEquals(HomeLayoutCard.entries.toSet(), landscape.map { it.card }.toSet())
        assertEquals(defaultLandscapeHomeLayoutItems(), landscape)
        assertTrue(landscape.count { it.y == 0f } >= 3)
    }
}
