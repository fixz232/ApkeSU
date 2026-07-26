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
            assertTrue(items.all { it.visible && it.width in 0.36f..1f })
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
}
