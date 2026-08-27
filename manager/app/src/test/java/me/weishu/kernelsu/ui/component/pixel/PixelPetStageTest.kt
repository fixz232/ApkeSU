package me.weishu.kernelsu.ui.component.pixel

import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Size
import java.io.File
import javax.imageio.ImageIO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PixelPetStageTest {
    @Test
    fun referenceArtboardsFollowTheGrowthCanvasContract() {
        assertEquals(32, PixelPetGrowthStage.Egg.sourceCanvasSize)
        assertEquals(48, PixelPetGrowthStage.Baby.sourceCanvasSize)
        assertEquals(64, PixelPetGrowthStage.Young.sourceCanvasSize)
        assertEquals(96, PixelPetGrowthStage.Adult.sourceCanvasSize)
        assertEquals(16, PixelPetGrowthStage.Egg.spriteCanvasSize)
        assertEquals(16, PixelPetGrowthStage.Baby.spriteCanvasSize)
        assertEquals(32, PixelPetGrowthStage.Young.spriteCanvasSize)
        assertEquals(48, PixelPetGrowthStage.Adult.spriteCanvasSize)

        PixelPetSpecies.entries.forEach { species ->
            PixelPetGrowthStage.entries.forEach { stage ->
                val source = File(
                    "src/main/assets/pixel_pet/reference/${species.name.lowercase()}_${stage.name.lowercase()}.png",
                )
                val image = ImageIO.read(source)
                assertEquals("$species/$stage width", stage.sourceCanvasSize, image.width)
                assertEquals("$species/$stage height", stage.sourceCanvasSize, image.height)
            }
        }
    }

    @Test
    fun babyReferenceActionsStayWithinTheCompactNativeArtwork() {
        PixelPetSpecies.entries.forEach { species ->
            PixelPetAction.entries.forEach { action ->
                val cels = PixelPetReferenceSprites.authoredFrame(
                    species = species,
                    stage = PixelPetGrowthStage.Baby,
                    action = action,
                    frame = 0,
                    facing = PixelPetFacing.Front,
                ).cels

                assertTrue(
                    "$species/$action has an oversized baby action cel",
                    cels.all { it.width <= 2 && it.height <= 3 },
                )
            }
        }
    }

    @Test
    fun referenceEffectsSnapToTheNativeStagePixelGrid() {
        val baby = pixelPetReferenceNativeRect(PixelPetGrowthStage.Baby, 14, 20, 4, 2)
        assertEquals(PixelPetReferenceNativeRect(21, 30, 6, 3), baby)

        val young = pixelPetReferenceNativeRect(PixelPetGrowthStage.Young, 14, 20, 4, 2)
        assertEquals(PixelPetReferenceNativeRect(28, 40, 8, 4), young)

        val adult = pixelPetReferenceNativeRect(PixelPetGrowthStage.Adult, 14, 20, 4, 2)
        assertEquals(PixelPetReferenceNativeRect(42, 60, 12, 6), adult)
    }

    @Test
    fun highDetailReferenceArtboardsKeepSourcePixelsAtCardScale() {
        val egg = pixelPetReferenceRenderLayout(32, unit = 14f)
        val young = pixelPetReferenceRenderLayout(64, unit = 14f)
        val adult = pixelPetReferenceRenderLayout(96, unit = 14f)

        assertEquals(5, egg.sourcePixel)
        assertEquals(3, young.sourcePixel)
        assertEquals(2, adult.sourcePixel)
        assertEquals(160, egg.destinationSize)
        assertEquals(192, young.destinationSize)
        assertEquals(192, adult.destinationSize)
    }

    @Test
    fun referencePlacementKeepsTheEntireArtboardInsideCompactAvatars() {
        val compactCanvas = Size(162f, 162f)
        val unit = compactCanvas.minDimension / 12f
        PixelPetGrowthStage.entries.forEach { stage ->
            val placement = pixelPetReferencePlacement(
                sourceCanvasSize = stage.sourceCanvasSize,
                unit = unit,
                canvasSize = compactCanvas,
                motion = PixelPetReferenceSprites.motion(PixelPetAction.Idle, frame = 0),
            )
            assertTrue("$stage left crop", placement.offsetX >= 0)
            assertTrue("$stage top crop", placement.offsetY >= 0)
            assertTrue("$stage right crop", placement.offsetX + placement.destinationSize <= compactCanvas.width)
            assertTrue("$stage bottom crop", placement.offsetY + placement.destinationSize <= compactCanvas.height)
        }
    }

    @Test
    fun referencePlacementCentersOpaqueArtAndKeepsItsFootlineVisible() {
        val canvas = Size(162f, 162f)
        val bounds = PixelPetReferenceOpaqueBounds(minX = 3, minY = 3, maxX = 92, maxY = 89)
        val placement = pixelPetReferencePlacement(
            sourceCanvasSize = 96,
            opaqueBounds = bounds,
            unit = canvas.minDimension / 12f,
            canvasSize = canvas,
            motion = PixelPetReferenceSprites.motion(PixelPetAction.Idle, frame = 0),
        )

        assertTrue(placement.offsetX + bounds.minX * placement.sourcePixel >= 0)
        assertTrue(placement.offsetX + (bounds.maxX + 1) * placement.sourcePixel <= canvas.width)
        assertTrue(placement.offsetY + bounds.minY * placement.sourcePixel >= 0)
        assertTrue(placement.offsetY + (bounds.maxY + 1) * placement.sourcePixel <= canvas.height)
        assertTrue(placement.destinationSize >= 96)
    }

    @Test
    fun v4FramePacksPreventStaticReferenceFallbackAcrossActionsAndDirections() {
        assertFalse(
            PixelPetReferenceSprites.shouldRender(
                PixelPetGrowthStage.Adult,
                PixelPetAction.Idle,
                PixelPetFacing.Front,
            ),
        )
        assertFalse(
            PixelPetReferenceSprites.shouldRender(
                PixelPetGrowthStage.Egg,
                PixelPetAction.Hatching,
                PixelPetFacing.Front,
            ),
        )
        assertFalse(
            PixelPetReferenceSprites.shouldRender(
                PixelPetGrowthStage.Adult,
                PixelPetAction.Walking,
                PixelPetFacing.Front,
            ),
        )
        assertFalse(
            PixelPetReferenceSprites.shouldRender(
                PixelPetGrowthStage.Adult,
                PixelPetAction.Idle,
                PixelPetFacing.Left,
            ),
        )
    }

    @Test
    fun adultHamsterUsesTheDetachedWheelBodySource() {
        val asset = PixelPetReferenceSprites.referenceAsset(
            species = PixelPetSpecies.Hamster,
            stage = PixelPetGrowthStage.Adult,
        )
        assertEquals("hamster_adult_body", asset.name)
        assertEquals(96, asset.canvasSize)

        val image = ImageIO.read(File("src/main/assets/pixel_pet/reference/${asset.name}.png"))
        assertEquals(96, image.width)
        assertEquals(96, image.height)
    }

    @Test
    fun furnitureActivitySeparatesApproachFromInteraction() {
        val approaching = PixelPetState(
            enabled = true,
            hatched = true,
            species = PixelPetSpecies.Cat,
            activeFurnitureId = "bowl",
            lastAction = PixelPetAction.Walking,
            lastActionAt = 1_000L,
            queuedAction = PixelPetAction.Eating,
        ).activeFurnitureActivity(now = 1_600L)

        assertEquals("bowl", approaching?.furnitureId)
        assertEquals(PixelPetAction.Walking, approaching?.action)
        assertTrue(approaching?.approaching == true)
        assertTrue(approaching?.progress ?: 0f > 0f)

        val interacting = PixelPetState(
            enabled = true,
            hatched = true,
            species = PixelPetSpecies.Cat,
            activeFurnitureId = "bowl",
            lastAction = PixelPetAction.Eating,
            lastActionAt = 2_000L,
        ).activeFurnitureActivity(now = 2_900L)

        assertEquals(PixelPetAction.Eating, interacting?.action)
        assertFalse(interacting?.approaching == true)
        assertTrue((interacting?.progress ?: 0f) in 0f..1f)
    }

    @Test
    fun immersiveStageUsesAReadablePetHitTarget() {
        assertEquals(164.dp, pixelPetLkmInteractiveAvatarSize(false, PixelPetStageMode.Immersive))
        assertEquals(192.dp, pixelPetLkmInteractiveHitSize(false, PixelPetStageMode.Immersive))
        assertTrue(
            pixelPetLkmInteractiveAvatarSize(false, PixelPetStageMode.Immersive) >
                pixelPetLkmInteractiveAvatarSize(false, PixelPetStageMode.Card),
        )
    }

    @Test
    fun stageFocusTracksTheDraggablePetAtViewportEdges() {
        assertEquals(96f, pixelPetStageFocusPosition(0f, viewportSize = 1_000f, hitSize = 192f))
        assertEquals(500f, pixelPetStageFocusPosition(0.5f, viewportSize = 1_000f, hitSize = 192f))
        assertEquals(904f, pixelPetStageFocusPosition(1f, viewportSize = 1_000f, hitSize = 192f))
        assertEquals(96f, pixelPetStageFocusPosition(-4f, viewportSize = 1_000f, hitSize = 192f))
        assertEquals(904f, pixelPetStageFocusPosition(4f, viewportSize = 1_000f, hitSize = 192f))
    }

    @Test
    fun referenceActionCuesDoNotObscureAuthoredArtwork() {
        val idle = PixelPetReferenceSprites.authoredFrame(
            PixelPetSpecies.Cat,
            PixelPetGrowthStage.Adult,
            PixelPetAction.Idle,
            frame = 0,
            facing = PixelPetFacing.Front,
        ).cels
        val walking = PixelPetReferenceSprites.authoredFrame(
            PixelPetSpecies.Cat,
            PixelPetGrowthStage.Adult,
            PixelPetAction.Walking,
            frame = 3,
            facing = PixelPetFacing.Front,
        ).cels
        val sleeping = PixelPetReferenceSprites.authoredFrame(
            PixelPetSpecies.Cat,
            PixelPetGrowthStage.Adult,
            PixelPetAction.Sleeping,
            frame = 4,
            facing = PixelPetFacing.Front,
        ).cels

        assertTrue(idle.isEmpty())
        assertNotEquals(walking, sleeping)
        assertTrue(walking.all { it.width <= 2 && it.height <= 1 })
        assertTrue(sleeping.all { it.width <= 2 && it.height <= 2 })
    }

    @Test
    fun designDraftAssetsStayTransparentAndSpeciesSpecific() {
        PixelPetSpecies.entries.forEach { species ->
            val image = ImageIO.read(
                File("src/main/assets/pixel_pet/reference/${species.name.lowercase()}_adult.png"),
            )
            val opaqueColors = buildSet {
                for (y in 0 until image.height) {
                    for (x in 0 until image.width) {
                        val argb = image.getRGB(x, y)
                        if ((argb ushr 24) != 0) add(argb and 0x00FFFFFF)
                    }
                }
            }
            assertEquals("$species top-left", 0, image.getRGB(0, 0) ushr 24)
            assertEquals("$species top-right", 0, image.getRGB(image.width - 1, 0) ushr 24)
            assertEquals("$species bottom-left", 0, image.getRGB(0, image.height - 1) ushr 24)
            assertTrue("$species needs a deliberate palette", opaqueColors.size >= 5)
            assertTrue("$species adult source must stay pixel-art sized", opaqueColors.size <= 40)
        }
    }

    @Test
    fun importedDesignArtboardsKeepACompactIntentionalPalette() {
        val maximumColors = mapOf(
            PixelPetGrowthStage.Egg to 28,
            PixelPetGrowthStage.Baby to 28,
            PixelPetGrowthStage.Young to 36,
            PixelPetGrowthStage.Adult to 40,
        )
        PixelPetSpecies.entries.forEach { species ->
            PixelPetGrowthStage.entries.forEach { stage ->
                val image = ImageIO.read(
                    File("src/main/assets/pixel_pet/reference/${species.name.lowercase()}_${stage.name.lowercase()}.png"),
                )
                val opaqueColors = buildSet {
                    for (y in 0 until image.height) {
                        for (x in 0 until image.width) {
                            val argb = image.getRGB(x, y)
                            if ((argb ushr 24) != 0) add(argb and 0x00FFFFFF)
                        }
                    }
                }
                assertTrue(
                    "$species/$stage has an unexpectedly broad palette",
                    opaqueColors.size <= checkNotNull(maximumColors[stage]),
                )
                assertTrue("$species/$stage must retain authored detail", opaqueColors.size >= 5)
            }
        }
    }

    @Test
    fun editableUserDesignDraftMatchesRuntimeReferenceAssets() {
        val draftDirectory = File("../tools/pixel-pet-source/design-draft-v1")
        assertTrue("editable design draft is missing", draftDirectory.isDirectory)

        PixelPetSpecies.entries.forEach { species ->
            PixelPetGrowthStage.entries.forEach { stage ->
                val name = "${species.name.lowercase()}_${stage.name.lowercase()}.png"
                val draft = ImageIO.read(File(draftDirectory, name))
                val runtime = ImageIO.read(File("src/main/assets/pixel_pet/reference/$name"))
                assertEquals("$name width", draft.width, runtime.width)
                assertEquals("$name height", draft.height, runtime.height)
                assertEquals("$name top-left alpha", 0, draft.getRGB(0, 0) ushr 24)
                assertTrue("$name needs a non-empty authored silhouette", opaquePixelCount(draft) >= 48)
            }
        }

        val hamsterBody = ImageIO.read(File(draftDirectory, "hamster_adult_body.png"))
        assertEquals(96, hamsterBody.width)
        assertEquals(96, hamsterBody.height)
        assertEquals(0, hamsterBody.getRGB(0, 0) ushr 24)
        assertTrue(opaquePixelCount(hamsterBody) >= 256)
    }

    @Test
    fun screenshotMatteBecomesTransparentWithoutRemovingThePet() {
        val matte = 0xFFEED9AC.toInt()
        val outline = 0xFF3B2A22.toInt()
        val coat = 0xFFF39B53.toInt()
        val source = IntArray(36) { matte }.apply {
            this[14] = outline
            this[15] = outline
            this[20] = coat
            this[21] = coat
        }

        val cleaned = removeReferenceBackdropPixels(source, width = 6, height = 6)

        assertEquals(0, cleaned[0] ushr 24)
        assertEquals(0, cleaned[35] ushr 24)
        assertEquals(0xFF, cleaned[14] ushr 24)
        assertEquals(0xFF, cleaned[15] ushr 24)
        assertEquals(0xFF, cleaned[20] ushr 24)
        assertEquals(0xFF, cleaned[21] ushr 24)
    }

    private fun opaquePixelCount(image: java.awt.image.BufferedImage): Int {
        var count = 0
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                if ((image.getRGB(x, y) ushr 24) != 0) count++
            }
        }
        return count
    }
}
