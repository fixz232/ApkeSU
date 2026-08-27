package me.weishu.kernelsu.ui.component.pixel

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Test

class PixelPetDragTest {
    @Test
    fun dragMovesOnBothAxesAndClampsToTheHabitat() {
        assertEquals(
            Offset(54f, 79f),
            resolvePixelPetDragPosition(
                current = Offset(40f, 60f),
                delta = Offset(14f, 19f),
                maxX = 100f,
                maxY = 120f,
            ),
        )
        assertEquals(
            Offset(0f, 120f),
            resolvePixelPetDragPosition(
                current = Offset(4f, 116f),
                delta = Offset(-20f, 20f),
                maxX = 100f,
                maxY = 120f,
            ),
        )
    }

    @Test
    fun persistedDragPositionIsNormalizedAndHandlesCompactBounds() {
        assertEquals(
            Offset(0.5f, 0.25f),
            normalizePixelPetDragPosition(
                position = Offset(50f, 30f),
                maxX = 100f,
                maxY = 120f,
            ),
        )
        assertEquals(
            Offset.Zero,
            normalizePixelPetDragPosition(
                position = Offset(18f, 24f),
                maxX = 0f,
                maxY = 0f,
            ),
        )
    }

    @Test
    fun furnitureEditorUsesTheSameCenterPointAsTheStage() {
        val viewport = 360f
        val item = 64f
        listOf(0.06f, 0.25f, 0.5f, 0.94f).forEach { center ->
            val offset = pixelPetFurnitureEditorOffset(center, viewport, item)
            assertEquals(center, pixelPetFurnitureCenterFromEditorOffset(offset, viewport, item), 0.0001f)
        }
        assertEquals(0.55f, snapFurniturePosition(0.526f), 0.0001f)
    }
}
