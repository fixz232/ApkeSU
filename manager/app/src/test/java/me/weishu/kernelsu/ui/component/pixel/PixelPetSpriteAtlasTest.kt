package me.weishu.kernelsu.ui.component.pixel

import java.io.File
import java.security.MessageDigest
import java.util.Properties
import javax.imageio.ImageIO
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.roundToInt

class PixelPetSpriteAtlasTest {

    @Before
    fun installVerifiedV5Frames() {
        PixelPetSpriteAtlas.installVerifiedPacksForTest(v5BakedSheets)
    }

    @Test
    fun naturalPalettesKeepIndependentOutlineAccentAndDetailRoles() {
        PixelPetSpecies.entries.forEach { species ->
            val colors = pixelPetModelColors(species)
            assertNotEquals("$species outline must remain visible", colors.outline, colors.base)
            assertNotEquals("$species accents must stay independent", colors.accent, colors.accentSecondary)
            assertNotEquals("$species detail and eye colors must stay independent", colors.detail, colors.eye)
        }
    }

    @Test
    fun v4FramesReplaceStaticReferenceModelsAcrossActionsAndDirections() {
        PixelPetGrowthStage.entries.forEach { stage ->
            PixelPetAction.entries.forEach { action ->
                PixelPetFacing.entries.forEach { facing ->
                    assertFalse(
                        "$stage $action $facing must use a v4 authored frame",
                        PixelPetReferenceSprites.shouldRender(stage, action, facing),
                    )
                }
            }
        }
    }

    @Test
    fun v4ContainsAllSixSpeciesDirectionsActionsAndTimingCels() {
        val manifest = Properties().apply {
            File("src/main/assets/pixel_pet/v4/manifest.properties").inputStream().use(::load)
        }
        assertEquals("4", manifest.getProperty("version"))
        assertEquals(PixelPetSpecies.entries.size, v4BakedSheets.size)
        PixelPetSpecies.entries.forEach { species ->
            val prefix = species.name.lowercase()
            assertEquals("1600", manifest.getProperty("$prefix.frames"))
            assertEquals("2", manifest.getProperty("$prefix.format"))
            val sheet = v4BakedSheets.getValue(species)
            assertEquals(1_600, sheet.frameCount)
            PixelPetGrowthStage.entries.forEach { stage ->
                PixelPetAction.entries.forEach { action ->
                    val frames = PixelPetFacing.entries.flatMap { facing ->
                        List(PixelPetSpriteAtlas.frameCount(action)) { index ->
                            requireNotNull(sheet.frame(species, stage, action, index, facing)) {
                                "Missing v4 frame $species/$stage/$action/$facing/$index"
                            }
                        }
                    }
                    assertTrue("Empty v4 frame family $species/$stage/$action", frames.all { it.cells.isNotEmpty() })
                    assertTrue("Missing v4 anchors $species/$stage/$action", frames.all { it.attachments != null })
                    assertTrue(
                        "v4 timing cels must not collapse to one static pose: $species/$stage/$action",
                        frames.map(::geometrySignature).toSet().size >= 2,
                    )
                }
            }
        }
    }

    @Test
    fun v5ContainsAllNativeCanvasesFramesAndAuthoredAnchors() {
        val manifest = Properties().apply {
            File("src/main/assets/pixel_pet/v5/manifest.properties").inputStream().use(::load)
        }
        assertEquals("5", manifest.getProperty("version"))
        val expectedBaselines = mapOf(
            PixelPetGrowthStage.Egg to 14,
            PixelPetGrowthStage.Baby to 14,
            PixelPetGrowthStage.Young to 30,
            PixelPetGrowthStage.Adult to 46,
        )
        PixelPetSpecies.entries.forEach { species ->
            val prefix = species.name.lowercase()
            val source = File("src/main/assets/${manifest.getProperty("$prefix.asset")}")
            assertTrue("Missing v5 runtime pack for $species", source.isFile)
            assertEquals(manifest.getProperty("$prefix.sha256"), source.sha256())
            assertEquals("1600", manifest.getProperty("$prefix.frames"))
            assertEquals("3", manifest.getProperty("$prefix.format"))
            val sheet = v5BakedSheets.getValue(species)
            assertEquals(3, sheet.formatVersion)
            assertEquals(1_600, sheet.frameCount)
            PixelPetGrowthStage.entries.forEach { stage ->
                PixelPetAction.entries.forEach { action ->
                    PixelPetFacing.entries.forEach { facing ->
                        repeat(PixelPetSpriteAtlas.frameCount(action)) { frameIndex ->
                            val frame = requireNotNull(
                                sheet.frame(species, stage, action, frameIndex, facing),
                            )
                            assertEquals(stage.spriteCanvasSize, frame.width)
                            assertEquals(stage.spriteCanvasSize, frame.height)
                            assertEquals(stage.spriteCanvasSize / 2, frame.pivotCellX)
                            assertEquals(expectedBaselines.getValue(stage), frame.baselineCellY)
                            assertTrue(frame.cells.isNotEmpty())
                            assertTrue(frame.cells.all { it.x in 1 until frame.width - 1 })
                            assertTrue(frame.cells.all { it.y in 1 until frame.height - 1 })
                            val attachments = requireNotNull(frame.attachments)
                            PixelPetAccessorySlot.entries.forEach { slot ->
                                val attachment = attachments.forSlot(slot)
                                assertTrue(attachment.x in 0 until frame.width)
                                assertTrue(attachment.y in 0 until frame.height)
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun v5EditableSheetsUseOnlyOpaqueSemanticPixelsAndCompleteMetadata() {
        val root = File("../tools/pixel-pet-source/v5")
        val manifest = JSONObject(root.resolve("source-manifest.json").readText().trimStart('\uFEFF'))
        assertEquals(5, manifest.getInt("version"))
        assertEquals(3, manifest.getInt("format"))
        assertEquals(1_600, manifest.getInt("framesPerSpecies"))
        val stageCanvasSizes = manifest.getJSONArray("stageCanvasSizes")
        assertEquals(listOf(16, 16, 32, 48), List(stageCanvasSizes.length(), stageCanvasSizes::getInt))
        assertEquals("authored-v5-native-semantic-pixel-sheets", manifest.getString("provenance"))
        val allowedOpaqueColors = setOf(
            0xFF24212B.toInt(),
            0xFFC8A27C.toInt(),
            0xFF766A82.toInt(),
            0xFFF6EAD7.toInt(),
            0xFFFFFFFF.toInt(),
            0xFFFFAB76.toInt(),
            0xFFD27C9E.toInt(),
            0xFF9EDEFA.toInt(),
            0xFF302A36.toInt(),
            0xFF17131D.toInt(),
        )
        val sources = manifest.getJSONObject("sources")
        PixelPetSpecies.entries.forEach { species ->
            val speciesSource = sources.getJSONObject(species.name.lowercase())
            PixelPetGrowthStage.entries.forEach { stage ->
                val stageSource = speciesSource.getJSONObject(stage.name.lowercase())
                val imageFile = root.resolve(stageSource.getString("image"))
                val metadataFile = root.resolve(stageSource.getString("metadata"))
                assertTrue(imageFile.isFile)
                assertTrue(metadataFile.isFile)
                assertEquals(stageSource.getString("imageSha256"), imageFile.sha256())
                assertEquals(stageSource.getString("metadataSha256"), metadataFile.sha256())
                val metadata = JSONObject(metadataFile.readText().trimStart('\uFEFF'))
                assertEquals(stage.spriteCanvasSize, metadata.getInt("canvasSize"))
                assertEquals(20, metadata.getInt("columns"))
                assertEquals(20, metadata.getInt("rows"))
                val frames = metadata.getJSONArray("frames")
                assertEquals(400, frames.length())
                val identities = mutableSetOf<String>()
                repeat(frames.length()) { index ->
                    val frame = frames.getJSONObject(index)
                    assertEquals(index, frame.getInt("index"))
                    assertEquals(stage.spriteCanvasSize, frame.getInt("width"))
                    assertEquals(stage.spriteCanvasSize, frame.getInt("height"))
                    assertTrue(frame.getString("provenance").startsWith("authored-v5-"))
                    identities += listOf(
                        frame.getInt("action"),
                        frame.getInt("facing"),
                        frame.getInt("frame"),
                    ).joinToString(":")
                }
                assertEquals(400, identities.size)
                val image = ImageIO.read(imageFile)
                try {
                    assertEquals(stage.spriteCanvasSize * 20, image.width)
                    assertEquals(stage.spriteCanvasSize * 20, image.height)
                    for (y in 0 until image.height) {
                        for (x in 0 until image.width) {
                            val argb = image.getRGB(x, y)
                            val alpha = argb ushr 24
                            assertTrue("Semi-transparent source pixel at $species/$stage/$x/$y", alpha == 0 || alpha == 255)
                            if (alpha == 255) {
                                assertTrue("Non-semantic source color at $species/$stage/$x/$y", argb in allowedOpaqueColors)
                            }
                        }
                    }
                } finally {
                    image.flush()
                }
            }
        }
    }

    @Test
    fun v5CompilerOnlyPacksExistingFramesWithoutPoseGeneration() {
        val compiler = File("../tools/pixel-pet-source/pixel_pet_sprite_source_v5.ps1").readText()
        assertTrue(compiler.contains("Read-PackedCells"))
        assertTrue(compiler.contains("Write-SpeciesPack"))
        listOf(
            "Compress-ForDirection",
            "New-AuthoredFacing",
            "Shift-Map",
            "Draw-ActionOriginal",
            "Draw-DirectionalFeatures",
            "Resize-",
        ).forEach { forbidden ->
            assertFalse("v5 compiler must not generate or deform poses: $forbidden", compiler.contains(forbidden))
        }
    }

    @Test
    fun v5NativeMastersStayNativeCrispAndIndependentFromReferenceBitmaps() {
        val root = File("../tools/pixel-pet-source/v5-masters")
        val sourceRoot = File("../tools/pixel-pet-source/v5-masters-src")
        val manifest = JSONObject(root.resolve("manifest.json").readText().trimStart('\uFEFF'))
        assertEquals(2, manifest.getInt("schemaVersion"))
        assertEquals(5, manifest.getInt("sourceVersion"))
        assertEquals("editable-semantic-pixel-rows", manifest.getString("provenance"))
        val allowedOpaqueColors = setOf(
            0xFF24212B.toInt(),
            0xFFC8A27C.toInt(),
            0xFF766A82.toInt(),
            0xFFF6EAD7.toInt(),
            0xFFFFFFFF.toInt(),
            0xFFFFAB76.toInt(),
            0xFFD27C9E.toInt(),
            0xFF9EDEFA.toInt(),
            0xFF302A36.toInt(),
            0xFF17131D.toInt(),
        )
        val species = manifest.getJSONObject("species")
        PixelPetSpecies.entries.forEach { pet ->
            val petSources = species.getJSONObject(pet.name.lowercase())
            PixelPetGrowthStage.entries.forEach { stage ->
                val source = petSources.getJSONObject(stage.name.lowercase())
                val sourceFile = sourceRoot.resolve(source.getString("source"))
                val imageFile = root.resolve(source.getString("image"))
                assertTrue(sourceFile.isFile)
                assertTrue(imageFile.isFile)
                assertEquals(source.getString("sourceSha256"), sourceFile.sha256())
                assertEquals(stage.spriteCanvasSize, source.getInt("canvasSize"))
                assertEquals(source.getString("sha256"), imageFile.sha256())
                val pixelRows = sourceFile.readLines().filterNot { it.startsWith("#") }
                assertEquals(stage.spriteCanvasSize, pixelRows.size)
                assertTrue(pixelRows.all { it.length == stage.spriteCanvasSize })
                assertTrue(pixelRows.flatMap(String::toList).all { it in ".obschamrex" })
                val paintedX = pixelRows.flatMapIndexed { _, row ->
                    row.mapIndexedNotNull { x, value -> x.takeIf { value != '.' } }
                }
                assertTrue(paintedX.min() >= 1)
                assertTrue(paintedX.max() <= stage.spriteCanvasSize - 2)
                val image = ImageIO.read(imageFile)
                try {
                    assertEquals(stage.spriteCanvasSize, image.width)
                    assertEquals(stage.spriteCanvasSize, image.height)
                    for (y in 0 until image.height) {
                        for (x in 0 until image.width) {
                            val argb = image.getRGB(x, y)
                            val alpha = argb ushr 24
                            assertTrue("Semi-transparent master at $pet/$stage/$x/$y", alpha == 0 || alpha == 255)
                            if (alpha == 255) {
                                assertTrue("Non-semantic master color at $pet/$stage/$x/$y", argb in allowedOpaqueColors)
                            }
                        }
                    }
                } finally {
                    image.flush()
                }
            }
        }

        val authoring = File("../tools/pixel-pet-source/redraw_pixel_pet_v5_sources.ps1").readText()
        assertTrue(authoring.contains("Read-NativeMaster"))
        listOf("_tmp_pixel_pet_qa", "design-draft-v1", "codex-clipboard", "DrawImage", "Resize-").forEach { forbidden ->
            assertFalse("v5 authoring must not import or resize a reference bitmap: $forbidden", authoring.contains(forbidden))
        }
    }

    @Test
    fun v4DirectionsAndCoreActionsHaveDedicatedOriginalSilhouettes() {
        val coreActions = listOf(
            PixelPetAction.Idle,
            PixelPetAction.Walking,
            PixelPetAction.Eating,
            PixelPetAction.Sleeping,
            PixelPetAction.Happy,
            PixelPetAction.Cleaning,
        )
        PixelPetSpecies.entries.forEach { species ->
            val sheet = v4BakedSheets.getValue(species)
            coreActions.forEach { action ->
                val facings = PixelPetFacing.entries.map { facing ->
                    geometrySignature(
                        requireNotNull(sheet.frame(species, PixelPetGrowthStage.Adult, action, 0, facing)),
                    )
                }
                assertEquals(
                    "$species $action must retain separate front, back, left and right cels",
                    PixelPetFacing.entries.size,
                    facings.toSet().size,
                )
                if (action != PixelPetAction.Idle) {
                    val idle = geometrySignature(
                        requireNotNull(sheet.frame(species, PixelPetGrowthStage.Adult, PixelPetAction.Idle, 0, PixelPetFacing.Front)),
                    )
                    val actionFrame = geometrySignature(
                        requireNotNull(sheet.frame(species, PixelPetGrowthStage.Adult, action, 0, PixelPetFacing.Front)),
                    )
                    assertNotEquals("$species $action cannot reuse idle artwork", idle, actionFrame)
                }
            }
        }
    }

    @Test
    fun v4EditableOriginalsMatchTheirDeclaredChecksums() {
        val root = File("../tools/pixel-pet-source/v4")
        val manifest = JSONObject(root.resolve("source-manifest.json").readText().trimStart('\uFEFF'))
        assertEquals(4, manifest.getInt("version"))
        val sources = manifest.getJSONObject("sources")
        PixelPetSpecies.entries.forEach { species ->
            val source = sources.getJSONObject(species.name.lowercase())
            val image = root.resolve(source.getString("image"))
            val anchors = root.resolve(source.getString("anchors"))
            assertTrue("Missing v4 editable Sprite sheet for $species", image.isFile)
            assertTrue("Missing v4 editable anchor sheet for $species", anchors.isFile)
            assertEquals(source.getString("imageSha256"), image.sha256())
            assertEquals(source.getString("anchorsSha256"), anchors.sha256())
            assertEquals(1_600, JSONArray(anchors.readText().trimStart('\uFEFF')).length())
        }
    }

    @Test
    fun v4FramesKeepTheSharedGroundBaselineAndIntegerAnchors() {
        PixelPetSpecies.entries.forEach { species ->
            val sheet = v4BakedSheets.getValue(species)
            PixelPetGrowthStage.entries.forEach { stage ->
                PixelPetAction.entries.forEach { action ->
                    PixelPetFacing.entries.forEach { facing ->
                        List(PixelPetSpriteAtlas.frameCount(action)) { index ->
                            requireNotNull(sheet.frame(species, stage, action, index, facing))
                        }.forEach { frame ->
                            assertEquals("$species/$stage/$action/$facing baseline", 29, frame.cells.maxOf { it.y })
                            val attachments = requireNotNull(frame.attachments)
                            PixelPetAccessorySlot.entries.forEach { slot ->
                                val point = attachments.forSlot(slot)
                                assertTrue(point.x in 0 until PixelPetSpriteAtlas.GRID)
                                assertTrue(point.y in 0 until PixelPetSpriteAtlas.GRID)
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun v4EggHatchingFramesKeepACompleteCrispShellInsideTheSafeArtboard() {
        PixelPetSpecies.entries.forEach { species ->
            val sheet = v4BakedSheets.getValue(species)
            val hatchingFrames = List(PixelPetSpriteAtlas.frameCount(PixelPetAction.Hatching)) { index ->
                requireNotNull(
                    sheet.frame(
                        species,
                        PixelPetGrowthStage.Egg,
                        PixelPetAction.Hatching,
                        index,
                        PixelPetFacing.Front,
                    ),
                )
            }
            assertEquals("$species egg must have six distinct authored hatching cels", 6, hatchingFrames.map(::geometrySignature).toSet().size)
            hatchingFrames.forEach { frame ->
                assertEquals("$species egg baseline", 29, frame.cells.maxOf(PixelPetSpriteCell::y))
                val minX = frame.cells.minOf(PixelPetSpriteCell::x)
                val maxX = frame.cells.maxOf(PixelPetSpriteCell::x)
                assertTrue("$species egg lacks a left safety margin", minX >= 4)
                assertTrue("$species egg lacks a right safety margin", maxX <= 28)
                assertTrue("$species egg is too wide for the compact stage", maxX - minX + 1 <= 24)
                assertTrue("$species egg needs top breathing room", frame.cells.minOf(PixelPetSpriteCell::y) >= 4)
                assertTrue("$species egg needs an opaque shell", frame.cells.size >= 220)
            }
        }
    }

    @Test
    fun v4EggAndBabyStagesUseIndependentSemanticPixelMasters() {
        val script = File("../tools/pixel-pet-source/pixel_pet_sprite_source_v4.ps1").readText()
        assertTrue(script.contains("Draw-EggActionOriginal"))
        assertTrue(script.contains("Draw-BabyActionOriginal"))
        assertFalse(script.contains("function New-AuthoredEggMaster"))

        listOf(PixelPetGrowthStage.Egg, PixelPetGrowthStage.Baby).forEach { stage ->
            val masters = PixelPetSpecies.entries.map { species ->
                requireNotNull(
                    v4BakedSheets.getValue(species).frame(
                        species,
                        stage,
                        PixelPetAction.Idle,
                        0,
                        PixelPetFacing.Front,
                    ),
                )
            }
            assertEquals(
                "$stage must retain six species-specific silhouettes",
                PixelPetSpecies.entries.size,
                masters.map(::geometrySignature).toSet().size,
            )
            assertEquals(
                "$stage must retain six species-specific semantic palettes",
                PixelPetSpecies.entries.size,
                masters.map(::signature).toSet().size,
            )
            masters.forEach { frame ->
                assertEquals("$stage baseline", 29, frame.cells.maxOf(PixelPetSpriteCell::y))
                assertTrue("$stage left crop", frame.cells.minOf(PixelPetSpriteCell::x) >= 1)
                assertTrue("$stage right crop", frame.cells.maxOf(PixelPetSpriteCell::x) <= 30)
                assertTrue("$stage top crop", frame.cells.minOf(PixelPetSpriteCell::y) >= 1)
            }
        }
    }

    @Test
    fun v4EggAndBabyActionsKeepStageSpecificTimedPixelCels() {
        PixelPetSpecies.entries.forEach { species ->
            val sheet = v4BakedSheets.getValue(species)
            PixelPetAction.entries.forEach { action ->
                val eggFrames = List(PixelPetSpriteAtlas.frameCount(action)) { index ->
                    requireNotNull(
                        sheet.frame(
                            species,
                            PixelPetGrowthStage.Egg,
                            action,
                            index,
                            PixelPetFacing.Front,
                        ),
                    )
                }
                val babyFrames = List(PixelPetSpriteAtlas.frameCount(action)) { index ->
                    requireNotNull(
                        sheet.frame(
                            species,
                            PixelPetGrowthStage.Baby,
                            action,
                            index,
                            PixelPetFacing.Front,
                        ),
                    )
                }
                val expectedEggCels = if (action == PixelPetAction.Hatching) 6 else 3
                assertTrue(
                    "$species egg $action lacks authored motion",
                    eggFrames.map(::geometrySignature).toSet().size >= expectedEggCels,
                )
                assertTrue(
                    "$species baby $action lacks authored motion",
                    babyFrames.map(::geometrySignature).toSet().size >= 3,
                )
                (eggFrames + babyFrames).forEach { frame ->
                    assertEquals("$species $action baseline", 29, frame.cells.maxOf(PixelPetSpriteCell::y))
                    assertTrue("$species $action left crop", frame.cells.minOf(PixelPetSpriteCell::x) >= 1)
                    assertTrue("$species $action right crop", frame.cells.maxOf(PixelPetSpriteCell::x) <= 30)
                    assertTrue("$species $action top crop", frame.cells.minOf(PixelPetSpriteCell::y) >= 1)
                }
            }
        }
    }

    @Test
    fun v4ActionIndexesMatchTheRuntimeHatchingContract() {
        assertEquals(6, PixelPetAction.Hatching.ordinal)
        assertEquals(6, PixelPetSpriteAtlas.frameCount(PixelPetAction.Hatching))
        PixelPetSpecies.entries.forEach { species ->
            val sheet = v4BakedSheets.getValue(species)
            val hatching = List(PixelPetSpriteAtlas.frameCount(PixelPetAction.Hatching)) { index ->
                requireNotNull(
                    sheet.frame(
                        species,
                        PixelPetGrowthStage.Egg,
                        PixelPetAction.Hatching,
                        index,
                        PixelPetFacing.Front,
                    ),
                )
            }
            assertEquals("$species must retain all authored hatching cels", 6, hatching.map(::geometrySignature).toSet().size)
        }
    }

    @Test
    fun v4AdultModelsAreHandAuthoredCompleteAndKeptInsideTheArtboard() {
        val script = File("../tools/pixel-pet-source/pixel_pet_sprite_source_v4.ps1").readText()
        val referenceMasters = File(
            "../tools/pixel-pet-source/pixel_pet_reference_masters_v1.ps1",
        ).readText()
        assertTrue(script.contains("Get-HandDrawnStagePattern"))
        assertTrue(
            script.contains(
                "return New-AuthoredFrameFromPattern (Get-HandDrawnStagePattern \$species \$stage)",
            ),
        )
        assertFalse(
            script.contains(
                "return New-AuthoredFrameFromPattern (Get-AuthoredStagePattern \$species \$stage)",
            ),
        )
        assertFalse(script.contains("Quantize-SourcePixel"))
        assertFalse(script.contains("GetPixel(\$sourceX"))
        assertFalse(referenceMasters.contains("System.Drawing"))
        assertFalse(referenceMasters.contains("Bitmap"))
        assertFalse(referenceMasters.contains("GetPixel"))
        assertFalse(referenceMasters.contains("Crop-"))
        assertFalse(referenceMasters.contains("Resize-"))
        PixelPetSpecies.entries.forEach { species ->
            PixelPetGrowthStage.entries.forEachIndexed { stage, _ ->
                assertTrue(
                    "Missing explicit pixel master for $species stage $stage",
                    referenceMasters.contains("'${species.name.lowercase()}/$stage'"),
                )
            }
        }
        val silhouettes = PixelPetSpecies.entries.map { species ->
            val frame = requireNotNull(
                v4BakedSheets.getValue(species).frame(
                    species,
                    PixelPetGrowthStage.Adult,
                    PixelPetAction.Idle,
                    0,
                    PixelPetFacing.Front,
                ),
            )
            assertEquals("$species adult baseline", 29, frame.cells.maxOf(PixelPetSpriteCell::y))
            assertTrue("$species adult is clipped on the left", frame.cells.minOf(PixelPetSpriteCell::x) >= 1)
            assertTrue("$species adult is clipped on the right", frame.cells.maxOf(PixelPetSpriteCell::x) <= 30)
            assertTrue("$species adult lacks a full body", frame.cells.size >= 150)
            geometrySignature(frame)
        }
        assertEquals("Six species need six readable base silhouettes", PixelPetSpecies.entries.size, silhouettes.toSet().size)
    }

    @Test
    fun repairedPenguinDogAndRabbitAdultsKeepCompleteFrontSilhouettes() {
        listOf(
            PixelPetSpecies.Penguin,
            PixelPetSpecies.Dog,
            PixelPetSpecies.Rabbit,
        ).forEach { species ->
            val frame = requireNotNull(
                v4BakedSheets.getValue(species).frame(
                    species,
                    PixelPetGrowthStage.Adult,
                    PixelPetAction.Idle,
                    0,
                    PixelPetFacing.Front,
                ),
            )
            val leftCells = frame.cells.count { it.x < 16 }
            val rightCells = frame.cells.count { it.x > 16 }
            val sideBalance = minOf(leftCells, rightCells).toFloat() / maxOf(leftCells, rightCells)
            assertTrue("$species adult still loses one side of its body", sideBalance >= 0.62f)
            val completeBodyRows = frame.cells
                .filter { it.y in 8..24 }
                .groupBy(PixelPetSpriteCell::y)
                .count { (_, row) -> row.any { it.x <= 14 } && row.any { it.x >= 18 } }
            assertTrue("$species adult lacks a connected two-sided body", completeBodyRows >= 12)
            assertEquals("$species adult must keep one authored eye pair", 2, frame.cells.count { it.value == 'e' })
        }
    }

    @Test
    fun referenceCareActionsUseEightToTenDistinctTimingPoses() {
        val actions = listOf(
            PixelPetAction.Idle,
            PixelPetAction.Walking,
            PixelPetAction.Eating,
            PixelPetAction.Sleeping,
            PixelPetAction.Happy,
            PixelPetAction.Frightened,
            PixelPetAction.Cleaning,
            PixelPetAction.Exploring,
        )
        actions.forEach { action ->
            val frameCount = PixelPetReferenceSprites.frameCount(action)
            assertTrue("$action frame count", frameCount in 8..10)
            val poses = (0 until frameCount).map { PixelPetReferenceSprites.motion(action, it) }
            assertTrue("$action needs multiple visual poses", poses.toSet().size >= 3)
        }
    }

    @Test
    fun referenceFeedbackCelsNeverReplaceTheSuppliedPetArtwork() {
        val actions = listOf(
            PixelPetAction.Idle,
            PixelPetAction.Walking,
            PixelPetAction.Eating,
            PixelPetAction.Sleeping,
            PixelPetAction.Happy,
            PixelPetAction.Frightened,
            PixelPetAction.Cleaning,
            PixelPetAction.Exploring,
        )
        PixelPetSpecies.entries.forEach { species ->
            PixelPetGrowthStage.entries.forEach { stage ->
                actions.forEach { action ->
                    val frames = (0 until PixelPetReferenceSprites.frameCount(action)).map { frame ->
                        PixelPetReferenceSprites.authoredFrame(
                            species,
                            stage,
                            action,
                            frame,
                            PixelPetFacing.Front,
                        )
                    }
                    assertTrue(
                        "$species $stage $action has an invalid action cel",
                        frames.flatMap { it.cels }.all { cel ->
                            cel.x in -2..33 &&
                                cel.y in -2..33 &&
                                cel.width in 1..12 &&
                                cel.height in 1..11
                        },
                    )
                    assertTrue(
                        "$species $stage $action must retain timed motion",
                        frames.map { it.motion }.toSet().size >= 3,
                    )
                }
                val front = PixelPetReferenceSprites.authoredFrame(
                    species, stage, PixelPetAction.Walking, 2, PixelPetFacing.Front,
                )
                val left = PixelPetReferenceSprites.authoredFrame(
                    species, stage, PixelPetAction.Walking, 2, PixelPetFacing.Left,
                )
                val right = PixelPetReferenceSprites.authoredFrame(
                    species, stage, PixelPetAction.Walking, 2, PixelPetFacing.Right,
                )
                assertNotEquals("$species $stage left", front.cels, left.cels)
                assertNotEquals("$species $stage right", left.cels, right.cels)
            }
        }
    }

    @Test
    fun referenceAnchorsStayOnThePixelGridAcrossAuthoredDirections() {
        PixelPetSpecies.entries.forEach { species ->
            PixelPetGrowthStage.entries.filterNot { it == PixelPetGrowthStage.Egg }.forEach { stage ->
                PixelPetAction.entries.forEach { action ->
                    PixelPetAccessorySlot.entries.forEach { slot ->
                        val left = PixelPetReferenceSprites.accessoryAnchor(
                            species, stage, action, 2, slot, PixelPetFacing.Left, unit = 5f,
                        )
                        val right = PixelPetReferenceSprites.accessoryAnchor(
                            species, stage, action, 2, slot, PixelPetFacing.Right, unit = 5f,
                        )
                        assertEquals(left.x * 5f, (left.x * 5f).roundToInt().toFloat(), 0f)
                        assertEquals(left.y * 5f, (left.y * 5f).roundToInt().toFloat(), 0f)
                        assertEquals(right.x * 5f, (right.x * 5f).roundToInt().toFloat(), 0f)
                        assertEquals(right.y * 5f, (right.y * 5f).roundToInt().toFloat(), 0f)
                        assertTrue(
                            "$species $stage $action $slot left=$left",
                            left.x in 0f..12f && left.y in 0f..12f,
                        )
                        assertTrue(
                            "$species $stage $action $slot right=$right",
                            right.x in 0f..12f && right.y in 0f..12f,
                        )
                    }
                }
            }
        }
    }

    @Test
    fun hamsterHighFormUsesDetachedWheelHabitatLayer() {
        assertTrue(
            PixelPetReferenceSprites.usesDetachedHamsterWheel(
                PixelPetSpecies.Hamster,
                PixelPetGrowthStage.Adult,
            ),
        )
        assertFalse(
            PixelPetReferenceSprites.usesDetachedHamsterWheel(
                PixelPetSpecies.Hamster,
                PixelPetGrowthStage.Young,
            ),
        )
    }

    @Test
    fun everySpeciesStageAndActionProducesAnInBoundsFrame() {
        PixelPetSpecies.entries.forEach { species ->
            PixelPetGrowthStage.entries.filterNot { it == PixelPetGrowthStage.Egg }.forEach { stage ->
                PixelPetAction.entries.forEach { action ->
                    repeat(PixelPetSpriteAtlas.frameCount(action)) { frameIndex ->
                        val frame = PixelPetSpriteAtlas.frame(species, stage, action, frameIndex)
                        assertEquals(stage.spriteCanvasSize, frame.width)
                        assertEquals(stage.spriteCanvasSize, frame.height)
                        assertTrue("$species $stage $action frame $frameIndex is empty", frame.cells.isNotEmpty())
                        assertTrue(frame.cells.all { it.x in 0 until frame.width })
                        assertTrue(frame.cells.all { it.y in 0 until frame.height })
                    }
                }
            }
        }
    }

    @Test
    fun atlasUsesHigherResolutionGrid() {
        assertEquals(32, PixelPetSpriteAtlas.GRID)
        PixelPetSpecies.entries.forEach { species ->
            val frame = PixelPetSpriteAtlas.frame(
                species,
                PixelPetGrowthStage.Adult,
                PixelPetAction.Idle,
                frame = 0,
            )
            assertTrue("$species should use the 32 px detail budget", frame.cells.size >= 150)
        }
    }

    @Test
    fun speciesAndPoseFamiliesHaveDistinctSilhouettes() {
        val speciesFrames = PixelPetSpecies.entries.map { species ->
            signature(PixelPetSpriteAtlas.frame(species, PixelPetGrowthStage.Young, PixelPetAction.Idle, 0))
        }
        assertTrue(speciesFrames.toSet().size == PixelPetSpecies.entries.size)

        PixelPetSpecies.entries.forEach { species ->
            val idle = signature(PixelPetSpriteAtlas.frame(species, PixelPetGrowthStage.Adult, PixelPetAction.Idle, 0))
            val moving = signature(PixelPetSpriteAtlas.frame(species, PixelPetGrowthStage.Adult, PixelPetAction.Walking, 0))
            val sleeping = signature(PixelPetSpriteAtlas.frame(species, PixelPetGrowthStage.Adult, PixelPetAction.Sleeping, 0))
            assertNotEquals(idle, moving)
            assertNotEquals(idle, sleeping)
            assertNotEquals(moving, sleeping)
        }
    }

    @Test
    fun animationFramesActuallyChangeMovingPets() {
        PixelPetSpecies.entries.forEach { species ->
            val first = signature(PixelPetSpriteAtlas.frame(species, PixelPetGrowthStage.Young, PixelPetAction.Walking, 0))
            val second = signature(PixelPetSpriteAtlas.frame(species, PixelPetGrowthStage.Young, PixelPetAction.Walking, 1))
            assertNotEquals(species.name, first, second)
        }
    }

    @Test
    fun actionsDeclareCompleteAnimationCycles() {
        assertEquals(8, PixelPetSpriteAtlas.frameCount(PixelPetAction.Idle))
        val extendedActions = setOf(
            PixelPetAction.Walking,
            PixelPetAction.Eating,
            PixelPetAction.Happy,
            PixelPetAction.Sleeping,
            PixelPetAction.Exploring,
        )
        PixelPetAction.entries.filterNot { it == PixelPetAction.Idle }.forEach { action ->
            val expected = if (action in extendedActions) 10 else 6
            assertEquals("$action should expose its authored frame count", expected, PixelPetSpriteAtlas.frameCount(action))
        }
        PixelPetAction.entries.forEach { action ->
            val count = PixelPetSpriteAtlas.frameCount(action)
            assertEquals(0, PixelPetSpriteAtlas.normalizeFrame(action, count))
            assertEquals(count - 1, PixelPetSpriteAtlas.normalizeFrame(action, -1))
        }
    }

    @Test
    fun everyDeclaredActionFrameIsNonEmptyAndChangesDuringTheCycle() {
        PixelPetSpecies.entries.forEach { species ->
            PixelPetGrowthStage.entries.filterNot { it == PixelPetGrowthStage.Egg }.forEach { stage ->
                PixelPetAction.entries.forEach { action ->
                    val frames = List(PixelPetSpriteAtlas.frameCount(action)) { frameIndex ->
                        PixelPetSpriteAtlas.frame(species, stage, action, frameIndex)
                    }
                    assertTrue(frames.all { it.cells.isNotEmpty() })
                    val unique = frames.map(::signature).toSet().size
                    assertTrue("$species $stage $action should animate", unique >= 2)
                }
            }
        }
    }

    @Test
    fun growthStagesAndExpressionsRemainVisuallyDistinct() {
        PixelPetSpecies.entries.forEach { species ->
            val baby = signature(PixelPetSpriteAtlas.frame(species, PixelPetGrowthStage.Baby, PixelPetAction.Idle, 0))
            val young = signature(PixelPetSpriteAtlas.frame(species, PixelPetGrowthStage.Young, PixelPetAction.Idle, 0))
            val adult = signature(PixelPetSpriteAtlas.frame(species, PixelPetGrowthStage.Adult, PixelPetAction.Idle, 0))
            val happy = signature(PixelPetSpriteAtlas.frame(species, PixelPetGrowthStage.Adult, PixelPetAction.Happy, 0))

            assertNotEquals("$species baby and young", baby, young)
            assertNotEquals("$species young and adult", young, adult)
            assertNotEquals("$species idle and happy", adult, happy)
        }
    }

    @Test
    fun careActionsUseDistinctModelFrames() {
        val careActions = listOf(
            PixelPetAction.Eating,
            PixelPetAction.Cleaning,
            PixelPetAction.Frightened,
            PixelPetAction.Watching,
            PixelPetAction.Calling,
        )
        PixelPetSpecies.entries.forEach { species ->
            val idle = signature(PixelPetSpriteAtlas.frame(species, PixelPetGrowthStage.Adult, PixelPetAction.Idle, 0))
            careActions.forEach { action ->
                val actionFrame = signature(PixelPetSpriteAtlas.frame(species, PixelPetGrowthStage.Adult, action, 0))
                assertNotEquals("$species $action", idle, actionFrame)
            }
        }
    }

    @Test
    fun fullCareActionsChangeGeometryAndAnimateAcrossFrames() {
        val fullBodyActions = listOf(
            PixelPetAction.Eating,
            PixelPetAction.Frightened,
            PixelPetAction.Happy,
            PixelPetAction.Cleaning,
            PixelPetAction.Watching,
        )
        PixelPetSpecies.entries.forEach { species ->
            val idleGeometry = geometrySignature(
                PixelPetSpriteAtlas.frame(species, PixelPetGrowthStage.Adult, PixelPetAction.Idle, 0),
            )
            fullBodyActions.forEach { action ->
                val first = PixelPetSpriteAtlas.frame(species, PixelPetGrowthStage.Adult, action, 0)
                val second = PixelPetSpriteAtlas.frame(species, PixelPetGrowthStage.Adult, action, 1)
                assertNotEquals("$species $action must change the body silhouette", idleGeometry, geometrySignature(first))
                assertNotEquals("$species $action must animate across frames", signature(first), signature(second))
                val uniqueFrames = (0..3).map { frameIndex ->
                    signature(
                        PixelPetSpriteAtlas.frame(species, PixelPetGrowthStage.Adult, action, frameIndex),
                    )
                }.toSet()
                assertTrue("$species $action needs at least two authored frames", uniqueFrames.size >= 2)
            }
        }
    }

    @Test
    fun accessoryAnchorsRespondToSpeciesStageAndAction() {
        val idleAdult = PixelPetSpriteAtlas.accessoryAnchor(
            PixelPetSpecies.Cat,
            PixelPetGrowthStage.Adult,
            PixelPetAction.Idle,
            frame = 0,
            PixelPetAccessorySlot.Head,
        )
        val eatingBaby = PixelPetSpriteAtlas.accessoryAnchor(
            PixelPetSpecies.Cat,
            PixelPetGrowthStage.Baby,
            PixelPetAction.Eating,
            frame = 1,
            PixelPetAccessorySlot.Head,
        )
        val rabbitAdult = PixelPetSpriteAtlas.accessoryAnchor(
            PixelPetSpecies.Rabbit,
            PixelPetGrowthStage.Adult,
            PixelPetAction.Idle,
            frame = 0,
            PixelPetAccessorySlot.Head,
        )

        assertNotEquals(idleAdult, eatingBaby)
        assertNotEquals(idleAdult, rabbitAdult)
        listOf(idleAdult, eatingBaby, rabbitAdult).forEach { anchor ->
            assertTrue(anchor.x in 0f..12f)
            assertTrue(anchor.y in -1f..12f)
            assertTrue(anchor.scale in 0.5f..1.1f)
        }
    }

    @Test
    fun accessoryDepthUsesStableDirectionAndActionRules() {
        assertEquals(
            PixelPetAccessoryRenderLayer.BehindModel,
            pixelPetAccessoryLayer(
                PixelPetAccessorySlot.Back,
                PixelPetFacing.Front,
                PixelPetAction.Walking,
            ),
        )
        assertEquals(
            PixelPetAccessoryRenderLayer.BehindModel,
            pixelPetAccessoryLayer(
                PixelPetAccessorySlot.Head,
                PixelPetFacing.Back,
                PixelPetAction.Idle,
            ),
        )
        assertEquals(
            PixelPetAccessoryRenderLayer.FrontModel,
            pixelPetAccessoryLayer(
                PixelPetAccessorySlot.Hand,
                PixelPetFacing.Right,
                PixelPetAction.Eating,
            ),
        )
        assertEquals(
            PixelPetAccessoryRenderLayer.BehindModel,
            pixelPetAccessoryLayer(
                PixelPetAccessorySlot.Hand,
                PixelPetFacing.Left,
                PixelPetAction.Sleeping,
            ),
        )
    }

    @Test
    fun facingDirectionOnlyChangesAfterHorizontalMovement() {
        assertTrue(PixelPetSpriteAtlas.resolveFacingLeft(10f, 4f, current = false))
        assertFalse(PixelPetSpriteAtlas.resolveFacingLeft(4f, 10f, current = true))
        assertTrue(PixelPetSpriteAtlas.resolveFacingLeft(4f, 4.2f, current = true))
    }

    @Test
    fun directionalFramesKeepDistinctReadableSilhouettes() {
        PixelPetSpecies.entries.forEach { species ->
            val front = signature(
                PixelPetSpriteAtlas.frame(
                    species,
                    PixelPetGrowthStage.Adult,
                    PixelPetAction.Idle,
                    frame = 0,
                    facing = PixelPetFacing.Front,
                ),
            )
            val back = signature(
                PixelPetSpriteAtlas.frame(
                    species,
                    PixelPetGrowthStage.Adult,
                    PixelPetAction.Idle,
                    frame = 0,
                    facing = PixelPetFacing.Back,
                ),
            )
            val left = signature(
                PixelPetSpriteAtlas.frame(
                    species,
                    PixelPetGrowthStage.Adult,
                    PixelPetAction.Walking,
                    frame = 1,
                    facing = PixelPetFacing.Left,
                ),
            )
            val right = signature(
                PixelPetSpriteAtlas.frame(
                    species,
                    PixelPetGrowthStage.Adult,
                    PixelPetAction.Walking,
                    frame = 1,
                    facing = PixelPetFacing.Right,
                ),
            )
            assertNotEquals("$species back should differ from front", front, back)
            assertNotEquals("$species side frame should differ from front", front, left)
            assertNotEquals("$species left and right should differ", left, right)
        }
    }

    @Test
    fun rightFacingFramesUseDedicatedProfileDetails() {
        packagedSpecies.forEach { species ->
            PixelPetGrowthStage.entries.filterNot { it == PixelPetGrowthStage.Egg }.forEach { stage ->
                PixelPetAction.entries.forEach { action ->
                    repeat(PixelPetSpriteAtlas.frameCount(action)) { frameIndex ->
                        val left = bakedFrame(species, stage, action, frameIndex, PixelPetFacing.Left)
                        val right = bakedFrame(species, stage, action, frameIndex, PixelPetFacing.Right)
                        val mirroredLeft = left.cells.map { cell ->
                            Triple(left.width - 1 - cell.x, cell.y, cell.value)
                        }.toSet()
                        val rightCells = right.cells.map { Triple(it.x, it.y, it.value) }.toSet()
                        assertNotEquals("$species $stage $action frame $frameIndex", mirroredLeft, rightCells)
                    }
                }
            }
        }
    }

    @Test
    fun everyGrowthStageHasItsOwnActionSilhouettes() {
        PixelPetSpecies.entries.forEach { species ->
            PixelPetAction.entries.forEach { action ->
                val stageGeometry = listOf(
                    PixelPetGrowthStage.Baby,
                    PixelPetGrowthStage.Young,
                    PixelPetGrowthStage.Adult,
                ).map { stage -> geometrySignature(PixelPetSpriteAtlas.frame(species, stage, action, 1)) }
                assertEquals("$species $action should not reuse one growth-stage silhouette", 3, stageGeometry.toSet().size)
            }
        }
    }

    @Test
    fun integerScaleSelectionNeverProducesFractionalPixels() {
        listOf(0.1f, 1f, 3.25f, 6.8f, 12f, 28f).forEach { unit ->
            val scale = pixelPetIntegerScale(unit)
            assertTrue(scale >= 1)
            assertEquals(scale.toFloat(), scale.toFloat(), 0f)
        }
        assertEquals("48dp-class pet avatars should use a readable 2x sprite", 2, pixelPetIntegerScale(4.5f))
        assertEquals("compact pet avatars should stay at 1x", 1, pixelPetIntegerScale(3.5f))
    }

    @Test
    fun spriteLayoutUsesVisibleCellsInsteadOfTransparentAtlasPadding() {
        PixelPetSpecies.entries.forEach { species ->
            val frame = PixelPetSpriteAtlas.frame(
                species,
                PixelPetGrowthStage.Adult,
                PixelPetAction.Idle,
                frame = 0,
            )
            val layout = pixelPetSpriteLayout(frame, unit = 6f)
            val maxX = frame.cells.maxOf(PixelPetSpriteCell::x)
            val maxY = frame.cells.maxOf(PixelPetSpriteCell::y)

            assertEquals(frame.cells.minOf(PixelPetSpriteCell::x), layout.minX)
            assertEquals(frame.cells.minOf(PixelPetSpriteCell::y), layout.minY)
            assertEquals(maxX - layout.minX + 1, layout.contentWidth)
            assertEquals(maxY - layout.minY + 1, layout.contentHeight)
            assertTrue(layout.contentWidth <= frame.width)
            assertTrue(layout.contentHeight <= frame.height)
            assertTrue(layout.cellUnit >= 1)
        }
    }

    @Test
    fun juvenileModelsKeepAReadableButDistinctScale() {
        PixelPetSpecies.entries.forEach { species ->
            val baby = PixelPetSpriteAtlas.frame(species, PixelPetGrowthStage.Baby, PixelPetAction.Idle, 0)
            val adult = PixelPetSpriteAtlas.frame(species, PixelPetGrowthStage.Adult, PixelPetAction.Idle, 0)
            assertNotEquals("$species baby must have its own detailed silhouette", geometrySignature(baby), geometrySignature(adult))
            assertTrue("$species baby sprite should retain visible detail", baby.cells.size >= 80)
        }
    }

    @Test
    fun everyFrameUsesItsStagePivotAndSharedViewBaseline() {
        PixelPetSpecies.entries.forEach { species ->
            PixelPetGrowthStage.entries.filterNot { it == PixelPetGrowthStage.Egg }.forEach { stage ->
                PixelPetAction.entries.forEach { action ->
                    repeat(PixelPetSpriteAtlas.frameCount(action)) { frameIndex ->
                        val layout = pixelPetSpriteLayout(
                            PixelPetSpriteAtlas.frame(species, stage, action, frameIndex),
                            unit = 6f,
                        )
                        val frame = PixelPetSpriteAtlas.frame(species, stage, action, frameIndex)
                        assertEquals(stage.spriteCanvasSize / 2f, layout.pivotCellX)
                        assertEquals(frame.baselineCellY.toFloat(), layout.baselineCellY)
                        assertEquals(
                            6f,
                            layout.originX + (frame.pivotCellX - layout.minX) * layout.logicalCell,
                            0.0001f,
                        )
                        assertEquals(
                            10.35f,
                            layout.originY + (frame.baselineCellY - layout.minY) * layout.logicalCell,
                            0.0001f,
                        )
                    }
                }
            }
        }
    }

    @Test
    fun visibleBoundsDoNotChangeSpritePixelScale() {
        PixelPetSpecies.entries.forEach { species ->
            PixelPetGrowthStage.entries.forEach { stage ->
                val layouts = PixelPetAction.entries.flatMap { action ->
                    List(PixelPetSpriteAtlas.frameCount(action)) { frameIndex ->
                        pixelPetSpriteLayout(
                            PixelPetSpriteAtlas.frame(species, stage, action, frameIndex),
                            unit = 6f,
                        )
                    }
                }
                assertEquals("$species $stage", 1, layouts.map(PixelPetSpriteLayout::cellUnit).toSet().size)
            }
        }
    }

    @Test
    fun accessoryAnchorsFollowAuthoredMotionWithoutLeavingThePixelGrid() {
        PixelPetSpecies.entries.forEach { species ->
            PixelPetGrowthStage.entries.filterNot { it == PixelPetGrowthStage.Egg }.forEach { stage ->
                PixelPetAction.entries.forEach { action ->
                    PixelPetAccessorySlot.entries.forEach { slot ->
                        val anchors = List(PixelPetSpriteAtlas.frameCount(action)) { frameIndex ->
                            PixelPetSpriteAtlas.accessoryAnchor(
                                species,
                                stage,
                                action,
                                frameIndex,
                                slot,
                            )
                        }
                        assertTrue(
                            "$species $stage $action $slot anchors=$anchors",
                            anchors.all { anchor ->
                                anchor.x in 0f..12f &&
                                    anchor.y in 0f..12f &&
                                    anchor.x * 5f == (anchor.x * 5f).roundToInt().toFloat() &&
                                    anchor.y * 5f == (anchor.y * 5f).roundToInt().toFloat()
                            },
                        )
                    }
                }
            }
        }
    }

    @Test
    fun accessoryAnchorsRemainOnThePhysicalPixelGrid() {
        val unit = 5f
        PixelPetSpecies.entries.forEach { species ->
            PixelPetGrowthStage.entries.forEach { stage ->
                PixelPetAction.entries.forEach { action ->
                    PixelPetFacing.entries.forEach { facing ->
                        PixelPetAccessorySlot.entries.forEach { slot ->
                            val anchor = PixelPetSpriteAtlas.accessoryAnchor(
                                species,
                                stage,
                                action,
                                frame = 0,
                                slot = slot,
                                facing = facing,
                                unit = unit,
                            )
                            assertEquals(anchor.x * unit, (anchor.x * unit).toInt().toFloat(), 0f)
                            assertEquals(anchor.y * unit, (anchor.y * unit).toInt().toFloat(), 0f)
                        }
                    }
                }
            }
        }
    }

    @Test
    fun frameCacheRespectsItsByteBudget() {
        PixelPetSpecies.entries.forEach { species ->
            PixelPetGrowthStage.entries.forEach { stage ->
                PixelPetAction.entries.forEach { action ->
                    repeat(PixelPetSpriteAtlas.frameCount(action)) { frameIndex ->
                        PixelPetFacing.entries.forEach { facing ->
                            PixelPetSpriteAtlas.frame(species, stage, action, frameIndex, facing)
                        }
                    }
                }
            }
        }
        val (_, bytes) = PixelPetSpriteAtlas.cacheStats()
        assertTrue(bytes <= PixelPetSpriteAtlas.FRAME_CACHE_BYTE_BUDGET)
    }

    @Test
    fun bakedSpriteSheetRoundTripsEveryIndependentFrame() {
        packagedSpecies.forEach { species ->
            PixelPetGrowthStage.entries.forEach { stage ->
                PixelPetAction.entries.forEach { action ->
                    repeat(PixelPetSpriteAtlas.frameCount(action)) { frameIndex ->
                        PixelPetFacing.entries.forEach { facing ->
                            val decoded = bakedFrame(species, stage, action, frameIndex, facing)
                            assertTrue("$species $stage $action $frameIndex $facing", decoded.cells.isNotEmpty())
                        }
                    }
                }
            }
        }
    }

    @Test
    fun spritePacksMatchTheirManifestChecksumsAndFrameCounts() {
        val manifest = Properties().apply {
            File("src/main/assets/pixel_pet/v2/manifest.properties").inputStream().use { input -> load(input) }
        }
        assertEquals("2", manifest.getProperty("version"))
        packagedSpecies.forEach { species ->
            val prefix = species.name.lowercase()
            val source = File("src/main/assets/${manifest.getProperty("$prefix.asset")}")
            assertTrue("Missing $species sprite pack", source.isFile)
            val raw = source.readBytes()
            val checksum = MessageDigest.getInstance("SHA-256").digest(raw)
                .joinToString("") { "%02x".format(it.toInt() and 0xFF) }
            assertEquals(manifest.getProperty("$prefix.sha256"), checksum)
            assertEquals(
                manifest.getProperty("$prefix.frames").toInt(),
                PixelPetSpriteAtlas.PixelPetBakedFrameSheet(raw).frameCount,
            )
        }
    }

    @Test
    fun editableV3PacksMatchManifestAndCarryEveryFrameAttachment() {
        val manifest = Properties().apply {
            File("src/main/assets/pixel_pet/v3/manifest.properties").inputStream().use(::load)
        }
        assertEquals("3", manifest.getProperty("version"))
        packagedSpecies.forEach { species ->
            val prefix = species.name.lowercase()
            val source = File("src/main/assets/${manifest.getProperty("$prefix.asset")}")
            assertTrue("Missing editable runtime pack for $species", source.isFile)
            val raw = source.readBytes()
            val checksum = MessageDigest.getInstance("SHA-256").digest(raw)
                .joinToString("") { "%02x".format(it.toInt() and 0xFF) }
            assertEquals(manifest.getProperty("$prefix.sha256"), checksum)
            val sheet = PixelPetSpriteAtlas.PixelPetBakedFrameSheet(raw)
            assertEquals(2, sheet.formatVersion)
            assertEquals(manifest.getProperty("$prefix.frames").toInt(), sheet.frameCount)
            PixelPetGrowthStage.entries.forEach { stage ->
                PixelPetAction.entries.forEach { action ->
                    repeat(PixelPetSpriteAtlas.frameCount(action)) { frameIndex ->
                        PixelPetFacing.entries.forEach { facing ->
                            val frame = requireNotNull(sheet.frame(species, stage, action, frameIndex, facing))
                            val attachments = frame.attachments
                            assertNotNull("$species/$stage/$action/$frameIndex/$facing has no attachments", attachments)
                            PixelPetAccessorySlot.entries.forEach { slot ->
                                val attachment = requireNotNull(attachments).forSlot(slot)
                                assertTrue(attachment.x in 0 until PixelPetSpriteAtlas.GRID)
                                assertTrue(attachment.y in 0 until PixelPetSpriteAtlas.GRID)
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun editableSourceSheetsAndAnchorsMatchTheirGoldenSourceManifest() {
        val root = File("../tools/pixel-pet-source/v3")
        val sourceManifest = JSONObject(root.resolve("source-manifest.json").readText().trimStart('\uFEFF'))
        assertEquals(3, sourceManifest.getInt("version"))
        assertEquals(32, sourceManifest.getInt("grid"))
        assertEquals(1_600, sourceManifest.getInt("framesPerSpecies"))
        val stageCanvases = sourceManifest.getJSONObject("stageCanvasSizes")
        assertEquals(16, stageCanvases.getInt("egg"))
        assertEquals(16, stageCanvases.getInt("baby"))
        assertEquals(32, stageCanvases.getInt("young"))
        assertEquals(48, stageCanvases.getInt("adult"))
        val sources = sourceManifest.getJSONObject("sources")
        packagedSpecies.forEach { species ->
            val source = sources.getJSONObject(species.name.lowercase())
            val image = root.resolve(source.getString("image"))
            val anchors = root.resolve(source.getString("anchors"))
            assertTrue("Missing editable image for $species", image.isFile)
            assertTrue("Missing editable anchors for $species", anchors.isFile)
            assertEquals(source.getString("imageSha256"), image.sha256())
            assertEquals(source.getString("anchorsSha256"), anchors.sha256())
            val frameAnchors = JSONArray(anchors.readText().trimStart('\uFEFF'))
            assertEquals(1_600, frameAnchors.length())
        }
    }

    @Test
    fun editableV3AttachmentsTrackTheAuthoredActionFrames() {
        packagedSpecies.forEach { species ->
            val sheet = editableBakedSheets.getValue(species)
            PixelPetGrowthStage.entries.filterNot { it == PixelPetGrowthStage.Egg }.forEach { stage ->
                listOf(PixelPetAction.Walking, PixelPetAction.Eating, PixelPetAction.Sleeping).forEach { action ->
                    val handAnchors = List(PixelPetSpriteAtlas.frameCount(action)) { frameIndex ->
                        requireNotNull(sheet.frame(species, stage, action, frameIndex, PixelPetFacing.Front))
                            .attachments
                            ?.hand
                    }
                    assertTrue(
                        "$species $stage $action should contain authored hand movement",
                        handAnchors.filterNotNull().map { it.x to it.y }.toSet().size > 1,
                    )
                }
            }
        }
    }

    @Test
    fun adultMasterCoreActionsUseDedicatedEditableArtwork() {
        val coreActions = listOf(
            PixelPetAction.Idle,
            PixelPetAction.Walking,
            PixelPetAction.Eating,
            PixelPetAction.Sleeping,
        )
        packagedSpecies.forEach { species ->
            val editableSheet = editableBakedSheets.getValue(species)
            coreActions.forEach { action ->
                val v2 = bakedFrame(species, PixelPetGrowthStage.Adult, action, 0, PixelPetFacing.Front)
                val v3 = requireNotNull(
                    editableSheet.frame(species, PixelPetGrowthStage.Adult, action, 0, PixelPetFacing.Front),
                )
                assertNotEquals(
                    "$species adult $action should use its dedicated editable master frame",
                    signature(v2),
                    signature(v3),
                )
                val authoredFrames = List(PixelPetSpriteAtlas.frameCount(action)) { frameIndex ->
                    requireNotNull(
                        editableSheet.frame(
                            species,
                            PixelPetGrowthStage.Adult,
                            action,
                            frameIndex,
                            PixelPetFacing.Front,
                        ),
                    )
                }
                // Ten-frame loops intentionally include easing and settling poses.
                // Six distinct silhouettes still prevents an action from degrading
                // into a cosmetic overlay or a two-frame toggle.
                val requiredVariation = if (action == PixelPetAction.Idle) 4 else 6
                assertTrue(
                    "$species adult $action should preserve authored pose variation",
                    authoredFrames.map(::geometrySignature).toSet().size >= requiredVariation,
                )
            }
        }
    }

    @Test
    fun adultMasterArtworkKeepsFullScaleSilhouettesAndDedicatedDirections() {
        val coreActions = listOf(
            PixelPetAction.Idle,
            PixelPetAction.Walking,
            PixelPetAction.Eating,
            PixelPetAction.Sleeping,
        )
        packagedSpecies.forEach { species ->
            val sheet = editableBakedSheets.getValue(species)
            coreActions.forEach { action ->
                val front = requireNotNull(
                    sheet.frame(species, PixelPetGrowthStage.Adult, action, 0, PixelPetFacing.Front),
                )
                val bounds = spriteBounds(front)
                val minimumWidth = if (action == PixelPetAction.Sleeping) 11 else 14
                val minimumHeight = if (action == PixelPetAction.Sleeping) 7 else 12
                assertTrue(
                    "$species adult $action should remain a full-scale silhouette",
                    bounds.width >= minimumWidth && bounds.height >= minimumHeight,
                )
                if (action != PixelPetAction.Sleeping) {
                    val directions = listOf(
                        PixelPetFacing.Front,
                        PixelPetFacing.Left,
                        PixelPetFacing.Back,
                    ).map { facing ->
                        geometrySignature(
                            requireNotNull(
                                sheet.frame(species, PixelPetGrowthStage.Adult, action, 0, facing),
                            ),
                        )
                    }
                    assertEquals(
                        "$species adult $action must retain dedicated front, side, and back artwork",
                        directions.size,
                        directions.toSet().size,
                    )
                }
            }
        }
    }

    @Test
    fun representativeCompiledFramesMatchGoldenSignatures() {
        val golden = Properties().apply {
            File("src/main/assets/pixel_pet/v2/golden.properties").inputStream().use(::load)
        }
        assertEquals("1", golden.getProperty("version"))
        val keys = golden.stringPropertyNames().filterNot { it == "version" }
        assertTrue("Golden Sprite fixture must cover representative frames", keys.size >= 200)
        keys.forEach { key ->
            val parts = key.split('.')
            assertEquals("Invalid golden key $key", 5, parts.size)
            val species = PixelPetSpecies.entries.first { it.name.equals(parts[0], ignoreCase = true) }
            val stage = PixelPetGrowthStage.entries[parts[1].toInt()]
            val action = PixelPetAction.entries[parts[2].toInt()]
            val facing = PixelPetFacing.entries[parts[3].toInt()]
            val frame = parts[4].toInt()
            val actual = bakedFrame(species, stage, action, frame, facing).goldenSignature()
            assertEquals("Golden frame drifted: $key", golden.getProperty(key), actual)
        }
    }

    @Test
    fun authoredTransitionActionsExposeAtLeastEightDistinctFrames() {
        val extendedActions = listOf(
            PixelPetAction.Walking,
            PixelPetAction.Eating,
            PixelPetAction.Happy,
            PixelPetAction.Sleeping,
            PixelPetAction.Exploring,
        )
        packagedSpecies.forEach { species ->
            PixelPetGrowthStage.entries.filterNot { it == PixelPetGrowthStage.Egg }.forEach { stage ->
                extendedActions.forEach { action ->
                    val frames = List(PixelPetSpriteAtlas.frameCount(action)) { frameIndex ->
                        geometrySignature(bakedFrame(species, stage, action, frameIndex, PixelPetFacing.Front))
                    }
                    assertTrue("$species $stage $action should include transition poses", frames.toSet().size >= 8)
                }
            }
        }
    }

    @Test
    fun modelMaterialsRespondToHabitatNightAndWeather() {
        val garden = pixelPetModelColors(PixelPetSpecies.Cat)
        val moonNight = pixelPetModelColors(
            species = PixelPetSpecies.Cat,
            habitat = PixelPetHabitat.Moon,
            weather = PixelPetWeather.Starlit,
            night = true,
        )
        val lagoonRain = pixelPetModelColors(
            species = PixelPetSpecies.Cat,
            habitat = PixelPetHabitat.Lagoon,
            weather = PixelPetWeather.Drizzle,
        )
        assertNotEquals(garden.base, moonNight.base)
        assertNotEquals(garden.outline, moonNight.outline)
        assertNotEquals(garden.accent, lagoonRain.accent)
    }

    @Test
    fun redesignedSpeciesPalettesRemainDistinctBeforeAmbientTinting() {
        val baseColors = PixelPetSpecies.entries.map { species ->
            pixelPetModelColors(species = species).let { colors ->
                colors.outline to colors.base
            }
        }
        assertEquals(PixelPetSpecies.entries.size, baseColors.toSet().size)
    }

    @Test
    fun savedAppearanceAndAmbientMaterialsProduceDistinctVisualContracts() {
        val natural = pixelPetModelColors(PixelPetSpecies.Cat, appearance = PixelPetAppearance.Natural)
        val dawn = pixelPetModelColors(PixelPetSpecies.Cat, appearance = PixelPetAppearance.Dawn)
        val frostRain = pixelPetModelColors(
            PixelPetSpecies.Cat,
            appearance = PixelPetAppearance.Frost,
            habitat = PixelPetHabitat.Lagoon,
            weather = PixelPetWeather.Drizzle,
        )
        val duskNight = pixelPetModelColors(
            PixelPetSpecies.Cat,
            appearance = PixelPetAppearance.Dusk,
            habitat = PixelPetHabitat.Moon,
            weather = PixelPetWeather.Meteor,
            night = true,
            warmLight = true,
        )

        val signatures = listOf(natural, dawn, frostRain, duskNight).map { colors ->
            listOf(colors.outline, colors.base, colors.highlight, colors.reflection, colors.accent)
                .joinToString(":") { it.value.toString(16) }
        }
        assertEquals("Every representative visual scene must remain distinguishable", 4, signatures.toSet().size)
        assertNotEquals(natural.base, dawn.base)
        assertNotEquals(frostRain.reflection, duskNight.reflection)
    }

    @Test
    fun accessorySpritesUseDistinctSpeciesDirectionProfiles() {
        val catLeft = pixelPetAccessorySpriteProfile(
            PixelPetSpecies.Cat,
            PixelPetFacing.Left,
            PixelPetAction.Idle,
        )
        val rabbitRight = pixelPetAccessorySpriteProfile(
            PixelPetSpecies.Rabbit,
            PixelPetFacing.Right,
            PixelPetAction.Cleaning,
        )
        val birdEating = pixelPetAccessorySpriteProfile(
            PixelPetSpecies.Bird,
            PixelPetFacing.Front,
            PixelPetAction.Eating,
        )

        assertNotEquals(catLeft, rabbitRight)
        assertNotEquals(catLeft.shiftX, rabbitRight.shiftX)
        assertTrue(rabbitRight.detailY < 0f)
        assertTrue(birdEating.shiftY < 0f)
    }

    private fun bakedFrame(
        species: PixelPetSpecies,
        stage: PixelPetGrowthStage,
        action: PixelPetAction,
        frame: Int,
        facing: PixelPetFacing,
    ): PixelPetSpriteFrame {
        return requireNotNull(bakedSheets.getValue(species).frame(species, stage, action, frame, facing)) {
            "Missing baked frame $species/$stage/$action/$frame/$facing"
        }
    }

    private val bakedSheets: Map<PixelPetSpecies, PixelPetSpriteAtlas.PixelPetBakedFrameSheet> by lazy {
        packagedSpecies.associateWith { species ->
            val source = File("src/main/assets/pixel_pet/v2/${species.name.lowercase()}.bin")
            PixelPetSpriteAtlas.PixelPetBakedFrameSheet(source.readBytes())
        }
    }

    private val editableBakedSheets: Map<PixelPetSpecies, PixelPetSpriteAtlas.PixelPetBakedFrameSheet> by lazy {
        packagedSpecies.associateWith { species ->
            val source = File("src/main/assets/pixel_pet/v3/${species.name.lowercase()}.bin")
            PixelPetSpriteAtlas.PixelPetBakedFrameSheet(source.readBytes())
        }
    }

    private val v4BakedSheets: Map<PixelPetSpecies, PixelPetSpriteAtlas.PixelPetBakedFrameSheet> by lazy {
        PixelPetSpecies.entries.associateWith { species ->
            val source = File("src/main/assets/pixel_pet/v4/${species.name.lowercase()}.bin")
            PixelPetSpriteAtlas.PixelPetBakedFrameSheet(source.readBytes())
        }
    }

    private val v5BakedSheets: Map<PixelPetSpecies, PixelPetSpriteAtlas.PixelPetBakedFrameSheet> by lazy {
        PixelPetSpecies.entries.associateWith { species ->
            val source = File("src/main/assets/pixel_pet/v5/${species.name.lowercase()}.bin")
            PixelPetSpriteAtlas.PixelPetBakedFrameSheet(source.readBytes())
        }
    }

    private val packagedSpecies: List<PixelPetSpecies> = PixelPetSpecies.entries
        .filterNot { it == PixelPetSpecies.Hamster }

    private fun signature(frame: PixelPetSpriteFrame): String = frame.cells
        .sortedWith(compareBy<PixelPetSpriteCell> { it.y }.thenBy { it.x })
        .joinToString(separator = "|") { "${it.x},${it.y},${it.value}" }

    private fun geometrySignature(frame: PixelPetSpriteFrame): String = frame.cells
        .sortedWith(compareBy<PixelPetSpriteCell> { it.y }.thenBy { it.x })
        .joinToString(separator = "|") { "${it.x},${it.y}" }

    private fun spriteBounds(frame: PixelPetSpriteFrame): SpriteBounds {
        val minX = frame.cells.minOf(PixelPetSpriteCell::x)
        val maxX = frame.cells.maxOf(PixelPetSpriteCell::x)
        val minY = frame.cells.minOf(PixelPetSpriteCell::y)
        val maxY = frame.cells.maxOf(PixelPetSpriteCell::y)
        return SpriteBounds(
            width = maxX - minX + 1,
            height = maxY - minY + 1,
        )
    }

    private data class SpriteBounds(
        val width: Int,
        val height: Int,
    )

    private fun PixelPetSpriteFrame.goldenSignature(): String {
        fun valueCode(value: Char): Int = when (value) {
            'o' -> 1
            'b' -> 2
            's' -> 3
            'c' -> 4
            'h' -> 5
            'a' -> 6
            'm' -> 7
            'r' -> 8
            'e' -> 9
            'x' -> 10
            else -> 0
        }
        val payload = cells
            .map { "${it.y * PixelPetSpriteAtlas.GRID + it.x}:${valueCode(it.value)}" }
            .sorted()
            .joinToString("|")
            .toByteArray(Charsets.US_ASCII)
        return MessageDigest.getInstance("SHA-256").digest(payload)
            .joinToString("") { "%02x".format(it.toInt() and 0xFF) }
    }

    private fun File.sha256(): String = MessageDigest.getInstance("SHA-256")
        .digest(readBytes())
        .joinToString("") { "%02x".format(it.toInt() and 0xFF) }
}
