package me.weishu.kernelsu.ui.component.pixel

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * The supplied design-board sprites remain available for source auditing and
 * recovery. The normal renderer uses the verified native pixel-cell packs for all
 * stages, facings, actions, and timing cels.
 */
internal object PixelPetReferenceSprites {
    private const val ASSET_ROOT = "pixel_pet/reference"
    // A full six-species set now keeps high-detail source artboards. This is
    // still below one decoded screen-sized wallpaper, while avoiding reload
    // churn as the card switches stage or action.
    private const val MAX_CACHE_BYTES = 1_024 * 1024

    private val cache = object : LruCache<String, PixelPetReferenceSprite>(MAX_CACHE_BYTES) {
        override fun sizeOf(key: String, value: PixelPetReferenceSprite): Int =
            (value.image.width * value.image.height * 4).coerceAtLeast(1)
    }

    @Volatile
    private var memoryCallbacksRegistered = false

    fun image(
        context: Context,
        species: PixelPetSpecies,
        stage: PixelPetGrowthStage,
    ): PixelPetReferenceSprite? = referenceAsset(species, stage).let { asset ->
        load(context, asset.name, asset.canvasSize)
    }

    /** The wheel is habitat furniture, never part of the draggable hamster. */
    fun hamsterWheel(context: Context): ImageBitmap? = load(
        context = context,
        name = "hamster_wheel",
        expectedCanvasSize = PixelPetGrowthStage.Young.sourceCanvasSize,
    )?.image

    internal fun usesDetachedHamsterWheel(
        species: PixelPetSpecies,
        stage: PixelPetGrowthStage,
    ): Boolean = species == PixelPetSpecies.Hamster && stage == PixelPetGrowthStage.Adult

    fun shouldRender(
        _stage: PixelPetGrowthStage,
        _action: PixelPetAction,
        _facing: PixelPetFacing = PixelPetFacing.Front,
    ): Boolean = false

    /**
     * The eight care actions use 8-10 authored timing poses. Existing atlas
     * files may contain fewer fallback frames; callers normalize only if the
     * direct reference asset is unavailable.
     */
    fun frameCount(action: PixelPetAction): Int = when (action) {
        PixelPetAction.Idle -> 8
        PixelPetAction.Walking,
        PixelPetAction.Eating,
        PixelPetAction.Happy,
        PixelPetAction.Sleeping,
        PixelPetAction.Exploring,
        -> 10
        else -> 8
    }

    fun motion(action: PixelPetAction, frame: Int): PixelPetReferenceMotion {
        val count = frameCount(action)
        val index = frame.mod(count)
        fun pick(vararg values: Int): Int = values[index.mod(values.size)]
        return when (action) {
            PixelPetAction.Idle -> PixelPetReferenceMotion(
                count, index, offsetYCells = pick(0, 0, -1, 0, 0, 0, -1, 0),
                detailPhase = index,
            )
            PixelPetAction.Walking -> PixelPetReferenceMotion(
                count, index,
                offsetXCells = pick(-1, 0, 1, 1, 1, 0, -1, -1, -1, 0),
                offsetYCells = pick(0, -1, 0, 0, -1, 0, 0, -1, 0, 0),
                detailPhase = index,
            )
            PixelPetAction.Eating -> PixelPetReferenceMotion(
                count, index,
                offsetYCells = pick(0, -1, -1, 0, 0, -1, 0, -1, 0, 0),
                detailPhase = index,
            )
            PixelPetAction.Sleeping -> PixelPetReferenceMotion(
                count, index,
                offsetYCells = pick(1, 1, 1, 0, 1, 1, 1, 0, 1, 1),
                detailPhase = index,
            )
            PixelPetAction.Happy,
            PixelPetAction.Petted,
            PixelPetAction.Playing,
            -> PixelPetReferenceMotion(
                count, index,
                offsetYCells = pick(0, -1, -2, -3, -2, -1, 0, 0, -1, 0),
                detailPhase = index,
            )
            PixelPetAction.Frightened -> PixelPetReferenceMotion(
                count, index,
                offsetXCells = pick(0, -1, 1, -1, 1, 0, -1, 1),
                detailPhase = index,
            )
            PixelPetAction.Cleaning -> PixelPetReferenceMotion(
                count, index,
                offsetYCells = pick(0, 0, -1, 0, 0, -1, 0, 0),
                detailPhase = index,
            )
            PixelPetAction.Exploring,
            PixelPetAction.Watching,
            PixelPetAction.Calling,
            -> PixelPetReferenceMotion(
                count, index,
                offsetXCells = pick(0, 1, 1, 0, -1, -1, 0, 1, 0, -1),
                offsetYCells = pick(0, -1, 0, 0, -1, 0, 0, -1, 0, 0),
                detailPhase = index,
            )
            PixelPetAction.Hatching -> PixelPetReferenceMotion(
                count, index,
                offsetXCells = pick(0, -1, 1, -1, 1, 0, -1, 1),
                detailPhase = index,
            )
        }
    }

    /**
     * Feedback cels deliberately live outside the reference Sprite. They give
     * an action readable timing without painting a generic body over the
     * imported species artwork.
     */
    fun authoredFrame(
        species: PixelPetSpecies,
        stage: PixelPetGrowthStage,
        action: PixelPetAction,
        frame: Int,
        facing: PixelPetFacing,
    ): PixelPetReferenceFrame {
        val motion = motion(action, frame)
        // Reference PNGs own the complete silhouette. The legacy 32-cell
        // body cels were authored for the fallback atlas and can land across
        // a high-detail chest or face as opaque blocks. Keep only sparse
        // feedback outside the silhouette for direct reference rendering.
        val cels = pixelPetReferenceFeedbackCels(
            species = species,
            stage = stage,
            action = action,
            motion = motion,
            facing = facing,
        )
        return PixelPetReferenceFrame(motion, cels)
    }

    /**
     * Reference PNGs and editable Sprite sheets share one authored attachment
     * table. The v3 sheet stores a position and render layer per frame, so a
     * hat, scarf, bag, and held item stay with the moving body instead of the
     * visible bounds of a frame.
     */
    fun accessoryAttachment(
        species: PixelPetSpecies,
        stage: PixelPetGrowthStage,
        action: PixelPetAction,
        frame: Int,
        slot: PixelPetAccessorySlot,
        facing: PixelPetFacing,
    ): PixelPetSpriteAttachment {
        val spriteFrame = PixelPetSpriteAtlas.loadedFrame(species, stage, action, frame, facing)
        return PixelPetSpriteAtlas.accessoryAttachment(
            spriteFrame = spriteFrame,
            species = species,
            stage = stage,
            action = action,
            slot = slot,
            facing = facing,
        )
    }

    fun accessoryAnchor(
        species: PixelPetSpecies,
        stage: PixelPetGrowthStage,
        action: PixelPetAction,
        frame: Int,
        slot: PixelPetAccessorySlot,
        facing: PixelPetFacing,
        unit: Float,
    ): PixelPetAccessoryAnchor = PixelPetSpriteAtlas.accessoryAnchor(
        spriteFrame = PixelPetSpriteAtlas.loadedFrame(species, stage, action, frame, facing),
        species = species,
        stage = stage,
        action = action,
        slot = slot,
        facing = facing,
        unit = unit,
    )

    fun registerMemoryCallbacks(context: Context) {
        if (memoryCallbacksRegistered) return
        synchronized(this) {
            if (memoryCallbacksRegistered) return
            context.applicationContext.registerComponentCallbacks(object : ComponentCallbacks2 {
                override fun onConfigurationChanged(newConfig: Configuration) = Unit

                override fun onLowMemory() = clear()

                override fun onTrimMemory(level: Int) {
                    when {
                        level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> clear()
                        level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> cache.trimToSize(MAX_CACHE_BYTES / 4)
                        level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> cache.trimToSize(MAX_CACHE_BYTES / 2)
                    }
                }
            })
            memoryCallbacksRegistered = true
        }
    }

    internal fun clear() = synchronized(cache) { cache.evictAll() }

    private fun load(context: Context, name: String, expectedCanvasSize: Int): PixelPetReferenceSprite? {
        synchronized(cache) {
            cache.get(name)?.let { return it }
            val decoded = runCatching {
                context.assets.open("$ASSET_ROOT/$name.png").use { input ->
                    BitmapFactory.decodeStream(input, null, BitmapFactory.Options().apply {
                        inScaled = false
                        inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
                    })
                }
            }.getOrNull() ?: return null
            if (decoded.width != expectedCanvasSize || decoded.height != expectedCanvasSize) {
                decoded.recycle()
                return null
            }
            // Design-board imports carry an authored alpha mask. Do not run a
            // color-key pass here: pale eggs, white rabbit fur, and highlights
            // intentionally share the board's warm palette.
            return PixelPetReferenceSprite(
                image = decoded.asImageBitmap(),
                opaqueBounds = pixelPetOpaqueBounds(decoded),
            ).also { cache.put(name, it) }
        }
    }

    internal data class PixelPetReferenceAsset(
        val name: String,
        val canvasSize: Int,
    )

    /**
     * The reference board for the high-form hamster includes its exercise
     * wheel. Keep that wheel in the habitat layer and load a clean 48px body
     * Sprite for the draggable pet.
     */
    internal fun referenceAsset(
        species: PixelPetSpecies,
        stage: PixelPetGrowthStage,
    ): PixelPetReferenceAsset = when {
        usesDetachedHamsterWheel(species, stage) -> PixelPetReferenceAsset(
            name = "hamster_adult_body",
            canvasSize = stage.sourceCanvasSize,
        )
        else -> PixelPetReferenceAsset(
            name = "${species.name.lowercase()}_${stage.assetName()}",
            canvasSize = stage.sourceCanvasSize,
        )
    }

    private fun PixelPetGrowthStage.assetName(): String = when (this) {
        PixelPetGrowthStage.Egg -> "egg"
        PixelPetGrowthStage.Baby -> "baby"
        PixelPetGrowthStage.Young -> "young"
        PixelPetGrowthStage.Adult -> "adult"
    }

}

/**
 * Removes a pasted screenshot matte without keying out the pet's own light
 * pixels. A candidate must be connected to the image edge, close to its seed
 * color, and occupy a substantial part of the Sprite.
 */
internal fun removeReferenceBackdrop(source: android.graphics.Bitmap): android.graphics.Bitmap {
    val width = source.width
    val height = source.height
    if (width <= 2 || height <= 2) return source
    val pixels = IntArray(width * height)
    source.getPixels(pixels, 0, width, 0, 0, width, height)
    val cleaned = removeReferenceBackdropPixels(pixels, width, height)
    if (cleaned.contentEquals(pixels)) return source
    return android.graphics.Bitmap.createBitmap(cleaned, width, height, android.graphics.Bitmap.Config.ARGB_8888)
}

/** Pure pixel form keeps the transparency rule covered by local unit tests. */
internal fun removeReferenceBackdropPixels(
    source: IntArray,
    width: Int,
    height: Int,
): IntArray {
    require(source.size == width * height)
    val result = source.copyOf()
    val visited = BooleanArray(source.size)
    val queue = IntArray(source.size)
    val component = IntArray(source.size)
    val opaqueCount = source.count(::isOpaqueReferencePixel)
    val minimumMatteArea = (source.size / 8).coerceAtLeast(24)

    source.indices.forEach { start ->
        if (visited[start] || !isOpaqueReferencePixel(source[start]) || !touchesTransparentEdge(source, width, height, start)) {
            return@forEach
        }
        var head = 0
        var tail = 0
        var componentSize = 0
        queue[tail++] = start
        visited[start] = true
        val seed = source[start]
        while (head < tail) {
            val index = queue[head++]
            component[componentSize++] = index
            referenceNeighbours(index, width, height).forEach { neighbour ->
                if (
                    !visited[neighbour] &&
                    isOpaqueReferencePixel(source[neighbour]) &&
                    isReferenceMatteNeighbour(seed, source[neighbour])
                ) {
                    visited[neighbour] = true
                    queue[tail++] = neighbour
                }
            }
        }
        // A pet can split a pasted screenshot matte into several edge regions.
        // Each region is still much larger than a legitimate border detail.
        if (componentSize >= minimumMatteArea && componentSize * 8 >= opaqueCount) {
            repeat(componentSize) { index ->
                result[component[index]] = result[component[index]] and 0x00FFFFFF
            }
        }
    }
    return result
}

private fun isOpaqueReferencePixel(pixel: Int): Boolean = ((pixel ushr 24) and 0xFF) > 0

private fun touchesTransparentEdge(source: IntArray, width: Int, height: Int, index: Int): Boolean {
    val x = index % width
    val y = index / width
    if (x == 0 || y == 0 || x == width - 1 || y == height - 1) return true
    return referenceNeighbours(index, width, height).any { !isOpaqueReferencePixel(source[it]) }
}

private fun referenceNeighbours(index: Int, width: Int, height: Int): IntArray {
    val x = index % width
    val y = index / width
    return buildList(4) {
        if (x > 0) add(index - 1)
        if (x < width - 1) add(index + 1)
        if (y > 0) add(index - width)
        if (y < height - 1) add(index + width)
    }.toIntArray()
}

private fun isReferenceMatteNeighbour(seed: Int, candidate: Int): Boolean {
    val seedRed = (seed shr 16) and 0xFF
    val seedGreen = (seed shr 8) and 0xFF
    val seedBlue = seed and 0xFF
    val red = (candidate shr 16) and 0xFF
    val green = (candidate shr 8) and 0xFF
    val blue = candidate and 0xFF
    val difference = (seedRed - red) * (seedRed - red) +
        (seedGreen - green) * (seedGreen - green) +
        (seedBlue - blue) * (seedBlue - blue)
    return difference <= REFERENCE_MATTE_COLOR_DISTANCE_SQUARED
}

// Screenshot compression shifts the paper matte by roughly 50-60 RGB units.
// A 75-level Euclidean threshold still excludes the orange fur, dark outline,
// and white highlights used by the supplied reference art.
private const val REFERENCE_MATTE_COLOR_DISTANCE_SQUARED = 5_625

internal data class PixelPetReferenceMotion(
    val frameCount: Int,
    val frame: Int,
    val offsetXCells: Int = 0,
    val offsetYCells: Int = 0,
    val detailPhase: Int = 0,
)

internal enum class PixelPetReferenceInk {
    Outline,
    Base,
    Shade,
    Cream,
    Highlight,
    Accent,
    Reflection,
}

/** A single editable cel in the common 32x32 authoring grid. */
internal data class PixelPetReferenceCel(
    val x: Int,
    val y: Int,
    val width: Int = 1,
    val height: Int = 1,
    val ink: PixelPetReferenceInk,
)

/**
 * A common-grid cel mapped to the native source-artboard grid. Baby art uses
 * 16px, growing art uses 32px, and adult art uses 48px. Keeping this mapping
 * discrete prevents a 32px overlay from cutting a 16px source pixel in half.
 */
internal data class PixelPetReferenceNativeRect(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
)

internal fun pixelPetReferenceNativeRect(
    stage: PixelPetGrowthStage,
    x: Int,
    y: Int,
    width: Int = 1,
    height: Int = 1,
): PixelPetReferenceNativeRect {
    val scale = stage.sourceCanvasSize.toFloat() / PixelPetSpriteAtlas.GRID
    val left = floor(x * scale).toInt()
    val top = floor(y * scale).toInt()
    val right = ceil((x + width) * scale).toInt()
    val bottom = ceil((y + height) * scale).toInt()
    return PixelPetReferenceNativeRect(
        x = left,
        y = top,
        width = (right - left).coerceAtLeast(1),
        height = (bottom - top).coerceAtLeast(1),
    )
}

internal data class PixelPetReferenceFrame(
    val motion: PixelPetReferenceMotion,
    val cels: List<PixelPetReferenceCel>,
)

/**
 * Action cues are intentionally kept off the model body. The actual animal is
 * always the imported reference Sprite, so a cat cannot become a generic
 * orange animal just because it is walking, sleeping, or facing sideways.
 */
private fun pixelPetReferenceFeedbackCels(
    species: PixelPetSpecies,
    stage: PixelPetGrowthStage,
    action: PixelPetAction,
    motion: PixelPetReferenceMotion,
    facing: PixelPetFacing,
): List<PixelPetReferenceCel> {
    val phase = motion.detailPhase
    val direction = if (facing == PixelPetFacing.Left) -1 else 1
    val bodyOffsetX = motion.offsetXCells
    val bodyOffsetY = motion.offsetYCells
    val mouthX = if (direction < 0) 6 else 26
    val headY = when (stage) {
        PixelPetGrowthStage.Egg -> 10
        PixelPetGrowthStage.Baby -> 10
        PixelPetGrowthStage.Young -> 7
        PixelPetGrowthStage.Adult -> 5
    }
    fun cue(
        x: Int,
        y: Int,
        width: Int = 1,
        height: Int = 1,
        ink: PixelPetReferenceInk,
    ) = PixelPetReferenceCel(
        x = x + bodyOffsetX,
        y = y + bodyOffsetY,
        width = width,
        height = height,
        ink = ink,
    )

    return when (action) {
        PixelPetAction.Walking,
        PixelPetAction.Exploring,
        -> listOf(
            cue(
                if (direction < 0) {
                    if (phase % 2 == 0) 24 else 6
                } else {
                    if (phase % 2 == 0) 6 else 24
                },
                29,
                2,
                1,
                PixelPetReferenceInk.Shade,
            ),
            cue(
                if (direction < 0) {
                    if (phase % 2 == 0) 7 else 25
                } else {
                    if (phase % 2 == 0) 25 else 7
                },
                30,
                1,
                1,
                PixelPetReferenceInk.Reflection,
            ),
        )
        PixelPetAction.Eating -> listOf(
            cue(mouthX, 20 + phase % 2, 2, 2, PixelPetReferenceInk.Accent),
            cue(mouthX + direction, 19 + phase % 2, 1, 1, PixelPetReferenceInk.Highlight),
        )
        PixelPetAction.Sleeping -> listOf(
            cue(if (direction < 0) 5 else 25, headY - phase % 3, 2, 1, PixelPetReferenceInk.Reflection),
            cue(if (direction < 0) 7 else 27, headY - phase % 3 - 2, 1, 1, PixelPetReferenceInk.Highlight),
        )
        PixelPetAction.Happy,
        PixelPetAction.Petted,
        PixelPetAction.Playing,
        -> listOf(
            cue(6, headY + phase % 2, 1, 1, PixelPetReferenceInk.Accent),
            cue(25, headY + 2 - phase % 2, 1, 1, PixelPetReferenceInk.Highlight),
        )
        PixelPetAction.Frightened -> listOf(
            cue(mouthX, headY, 1, 3, PixelPetReferenceInk.Reflection),
            cue(mouthX, headY - 2, 1, 1, PixelPetReferenceInk.Accent),
        )
        PixelPetAction.Cleaning -> listOf(
            cue(mouthX, headY + 2 - phase % 3, 1, 1, PixelPetReferenceInk.Reflection),
            cue(mouthX + direction, headY + 4 - phase % 2, 1, 1, PixelPetReferenceInk.Highlight),
        )
        PixelPetAction.Hatching -> listOf(
            cue(7 + phase % 2, 8, 1, 1, PixelPetReferenceInk.Highlight),
            cue(24 - phase % 2, 11, 1, 1, PixelPetReferenceInk.Accent),
        )
        PixelPetAction.Watching,
        PixelPetAction.Calling,
        PixelPetAction.Idle,
        -> if (species == PixelPetSpecies.Bird && phase % 4 == 0) {
            listOf(cue(25, headY + 1, 1, 1, PixelPetReferenceInk.Reflection))
        } else {
            emptyList()
        }
    }
}

/**
 * Full-body action cels live beside the editable reference source. The pet is
 * still a crisp 32px Sprite, but each frame owns visible legs, paws, wings,
 * ears, and posture instead of relying on an expression or particle overlay.
 */
private fun pixelPetFullBodyActionCels(
    species: PixelPetSpecies,
    stage: PixelPetGrowthStage,
    action: PixelPetAction,
    motion: PixelPetReferenceMotion,
    facing: PixelPetFacing,
): List<PixelPetReferenceCel> {
    if (stage == PixelPetGrowthStage.Baby) {
        return pixelPetBabyActionCels(action, motion, facing)
    }
    val frame = motion.detailPhase
    val side = if (facing == PixelPetFacing.Left) -1 else 1
    val nearX = if (facing == PixelPetFacing.Left) 10 else 20
    val farX = if (facing == PixelPetFacing.Left) 20 else 12
    val babyLift = when (stage) {
        PixelPetGrowthStage.Baby -> 1
        PixelPetGrowthStage.Egg -> 2
        else -> 0
    }
    val stride = if (frame % 2 == 0) -1 else 1
    val limbInk = when (species) {
        PixelPetSpecies.Bird,
        PixelPetSpecies.Penguin,
        -> PixelPetReferenceInk.Shade
        else -> PixelPetReferenceInk.Base
    }
    val outline = PixelPetReferenceInk.Outline
    val highlight = PixelPetReferenceInk.Highlight
    val accent = PixelPetReferenceInk.Accent

    fun limbs(
        nearY: Int = 25,
        farY: Int = 25,
        nearShift: Int = 0,
        farShift: Int = 0,
    ): List<PixelPetReferenceCel> = listOf(
        PixelPetReferenceCel(nearX + nearShift, nearY - babyLift, 3, 1, outline),
        PixelPetReferenceCel(nearX + nearShift + side, nearY + 1 - babyLift, 2, 1, limbInk),
        PixelPetReferenceCel(farX + farShift, farY - babyLift, 3, 1, outline),
        PixelPetReferenceCel(farX + farShift - side, farY + 1 - babyLift, 2, 1, limbInk),
    )

    fun raisedPaws(height: Int, reach: Int): List<PixelPetReferenceCel> = listOf(
        PixelPetReferenceCel(nearX + side * reach, height - babyLift, 2, 3, outline),
        PixelPetReferenceCel(nearX + side * reach, height + 1 - babyLift, 1, 2, limbInk),
        PixelPetReferenceCel(farX - side * (reach - 1), height + 1 - babyLift, 2, 2, highlight),
    )

    val speciesMotion = when (species) {
        PixelPetSpecies.Cat -> listOf(
            PixelPetReferenceCel(25 + side * 2, 20 + stride, 1, 6, outline),
            PixelPetReferenceCel(26 + side * 2, 20 + stride, 1, 5, limbInk),
            PixelPetReferenceCel(12, 7 + (frame % 3), 1, 2, highlight),
        )
        PixelPetSpecies.Dog -> listOf(
            PixelPetReferenceCel(25 + side * 2, 22 + stride, 1, 4, outline),
            PixelPetReferenceCel(26 + side * 2, 22 + stride, 1, 3, limbInk),
            PixelPetReferenceCel(9 + side, 10 + (frame % 2), 2, 2, limbInk),
        )
        PixelPetSpecies.Bird -> listOf(
            PixelPetReferenceCel(7 + stride, 16, 5, 2, outline),
            PixelPetReferenceCel(8 + stride, 16, 4, 1, limbInk),
            PixelPetReferenceCel(20 + side, 20 + (frame % 2), 2, 2, accent),
        )
        PixelPetSpecies.Rabbit -> listOf(
            PixelPetReferenceCel(11 + side, 4 + (frame % 2), 1, 8, outline),
            PixelPetReferenceCel(12 + side, 5 + (frame % 2), 1, 7, highlight),
            PixelPetReferenceCel(20 - side, 4 + ((frame + 1) % 2), 1, 8, outline),
        )
        PixelPetSpecies.Penguin -> listOf(
            PixelPetReferenceCel(7 + stride, 18, 4, 2, outline),
            PixelPetReferenceCel(8 + stride, 18, 3, 1, limbInk),
            PixelPetReferenceCel(21 - stride, 18, 4, 2, outline),
        )
        PixelPetSpecies.Hamster -> listOf(
            PixelPetReferenceCel(9 + stride, 20, 3, 2, limbInk),
            PixelPetReferenceCel(20 - stride, 20, 3, 2, limbInk),
            PixelPetReferenceCel(24 + side, 23 + stride, 2, 1, accent),
        )
    }

    val bodyPose = when (action) {
        PixelPetAction.Idle -> limbs() + listOf(
            PixelPetReferenceCel(14, 22 - (frame % 4) / 3 - babyLift, 4, 1, highlight),
        )
        PixelPetAction.Walking,
        PixelPetAction.Exploring,
        -> limbs(
            nearY = 25 + if (stride > 0) 1 else 0,
            farY = 25 + if (stride < 0) 1 else 0,
            nearShift = stride * 2,
            farShift = -stride * 2,
        ) + listOf(
            PixelPetReferenceCel(nearX + side * 2, 21 - babyLift, 2, 3, outline),
            PixelPetReferenceCel(nearX + side * 2, 22 - babyLift, 1, 2, limbInk),
            PixelPetReferenceCel(15, 24 - babyLift, 2, 1, highlight),
        )
        PixelPetAction.Eating -> limbs(nearY = 26, farY = 26) + raisedPaws(
            height = 18 + (frame % 2),
            reach = 2,
        ) + listOf(
            PixelPetReferenceCel(if (side < 0) 8 else 22, 21 + (frame % 2), 2, 2, accent),
            PixelPetReferenceCel(14, 25 - babyLift, 4, 1, PixelPetReferenceInk.Shade),
        )
        PixelPetAction.Sleeping -> listOf(
            PixelPetReferenceCel(10, 25 - babyLift, 12, 2, outline),
            PixelPetReferenceCel(11, 24 - babyLift, 10, 2, limbInk),
            PixelPetReferenceCel(12, 27 - babyLift, 8, 1, PixelPetReferenceInk.Cream),
            PixelPetReferenceCel(nearX, 23 - babyLift, 3, 1, highlight),
        )
        PixelPetAction.Happy,
        PixelPetAction.Petted,
        PixelPetAction.Playing,
        -> limbs(nearY = 25 + (frame % 2), farY = 25 + ((frame + 1) % 2)) +
            raisedPaws(height = 15 - (frame % 3), reach = 3) + listOf(
                PixelPetReferenceCel(14, 23 - babyLift - (frame % 2), 4, 1, highlight),
                PixelPetReferenceCel(8, 10 - (frame % 3), 1, 1, accent),
                PixelPetReferenceCel(23, 11 - ((frame + 1) % 3), 1, 1, highlight),
            )
        PixelPetAction.Frightened -> limbs(nearY = 27, farY = 27) + listOf(
            PixelPetReferenceCel(11, 20 - babyLift, 10, 3, outline),
            PixelPetReferenceCel(12, 21 - babyLift, 8, 2, PixelPetReferenceInk.Shade),
            PixelPetReferenceCel(nearX + side, 18 - babyLift, 2, 3, highlight),
        )
        PixelPetAction.Cleaning -> limbs(nearY = 26, farY = 26) + raisedPaws(
            height = 16 + (frame % 3),
            reach = 2,
        ) + listOf(
            PixelPetReferenceCel(if (side < 0) 8 else 23, 13 - (frame % 3), 1, 1, PixelPetReferenceInk.Reflection),
        )
        PixelPetAction.Watching,
        PixelPetAction.Calling,
        -> limbs(nearY = 26, farY = 26) + listOf(
            PixelPetReferenceCel(nearX + side * 2, 17 - babyLift, 2, 4, outline),
            PixelPetReferenceCel(nearX + side * 2, 18 - babyLift, 1, 3, limbInk),
            PixelPetReferenceCel(if (side < 0) 7 else 24, 11, 1, 2, PixelPetReferenceInk.Reflection),
        )
        PixelPetAction.Hatching -> listOf(
            PixelPetReferenceCel(10, 23, 12, 3, outline),
            PixelPetReferenceCel(11, 22, 10, 2, PixelPetReferenceInk.Cream),
            PixelPetReferenceCel(15 + stride, 16, 2, 6, accent),
        )
    }
    return bodyPose + speciesMotion
}

/**
 * Baby artwork already owns its ears, paws, tail, and face in the 16px
 * source. Only sparse care feedback is added here; the previous 32px body
 * overlays were the source of the blocky, mismatched juvenile silhouette.
 */
private fun pixelPetBabyActionCels(
    action: PixelPetAction,
    motion: PixelPetReferenceMotion,
    facing: PixelPetFacing,
): List<PixelPetReferenceCel> {
    val phase = motion.detailPhase
    val side = if (facing == PixelPetFacing.Left) -1 else 1
    val directionMarker = when (facing) {
        PixelPetFacing.Front -> emptyList()
        PixelPetFacing.Back -> listOf(
            PixelPetReferenceCel(15, 28, 1, 1, PixelPetReferenceInk.Shade),
        )
        PixelPetFacing.Left -> listOf(
            PixelPetReferenceCel(7, 27, 1, 1, PixelPetReferenceInk.Highlight),
        )
        PixelPetFacing.Right -> listOf(
            PixelPetReferenceCel(24, 27, 1, 1, PixelPetReferenceInk.Highlight),
        )
    }
    val actionEffects = when (action) {
        PixelPetAction.Idle -> listOf(
            PixelPetReferenceCel(15, 8 + phase.mod(3), 2, 1, PixelPetReferenceInk.Highlight),
        )
        PixelPetAction.Walking,
        PixelPetAction.Exploring,
        -> listOf(
            PixelPetReferenceCel(if (phase % 2 == 0) 8 else 23, 29, 2, 1, PixelPetReferenceInk.Shade),
        )
        PixelPetAction.Eating -> listOf(
            PixelPetReferenceCel(if (side < 0) 7 else 23, 22 + phase.mod(2), 2, 2, PixelPetReferenceInk.Accent),
        )
        PixelPetAction.Sleeping -> listOf(
            PixelPetReferenceCel(if (side < 0) 8 else 23, 8 - phase.mod(3) * 2, 2, 1, PixelPetReferenceInk.Reflection),
        )
        PixelPetAction.Happy,
        PixelPetAction.Petted,
        PixelPetAction.Playing,
        -> listOf(
            PixelPetReferenceCel(7, 9 - phase.mod(3), 1, 1, PixelPetReferenceInk.Accent),
            PixelPetReferenceCel(24, 10 - (phase + 1).mod(3), 1, 1, PixelPetReferenceInk.Highlight),
        )
        PixelPetAction.Frightened -> listOf(
            PixelPetReferenceCel(if (side < 0) 8 else 23, 11 + phase.mod(2), 1, 2, PixelPetReferenceInk.Reflection),
        )
        PixelPetAction.Cleaning -> listOf(
            PixelPetReferenceCel(if (side < 0) 8 else 23, 10 - phase.mod(3), 1, 1, PixelPetReferenceInk.Reflection),
        )
        PixelPetAction.Watching,
        PixelPetAction.Calling,
        -> listOf(
            PixelPetReferenceCel(if (side < 0) 7 else 24, 10 + phase.mod(2), 1, 2, PixelPetReferenceInk.Highlight),
        )
        PixelPetAction.Hatching -> listOf(
            PixelPetReferenceCel(15 + if (phase % 2 == 0) 0 else 1, 17, 1, 2, PixelPetReferenceInk.Highlight),
        )
    }
    return actionEffects + directionMarker
}

private fun pixelPetActionCels(
    species: PixelPetSpecies,
    stage: PixelPetGrowthStage,
    action: PixelPetAction,
    motion: PixelPetReferenceMotion,
    facing: PixelPetFacing,
): List<PixelPetReferenceCel> {
    if (stage == PixelPetGrowthStage.Baby) return emptyList()
    val side = if (facing == PixelPetFacing.Left) -1 else 1
    val stride = if (motion.detailPhase % 2 == 0) -1 else 1
    return when (action) {
        PixelPetAction.Idle -> listOf(
            PixelPetReferenceCel(11, 23 + (motion.detailPhase % 4) / 3, 2, 1, PixelPetReferenceInk.Highlight),
            PixelPetReferenceCel(20, 23 + ((motion.detailPhase + 2) % 4) / 3, 2, 1, PixelPetReferenceInk.Highlight),
        )
        PixelPetAction.Walking -> listOf(
            PixelPetReferenceCel(11 + stride * 2, 28, 3, 1, PixelPetReferenceInk.Outline),
            PixelPetReferenceCel(19 - stride * 2, 28, 3, 1, PixelPetReferenceInk.Outline),
            PixelPetReferenceCel(14 + stride, 27, 2, 1, PixelPetReferenceInk.Cream),
        )
        PixelPetAction.Eating -> listOf(
            PixelPetReferenceCel(if (side < 0) 8 else 22, 22, 2, 2, PixelPetReferenceInk.Accent),
            PixelPetReferenceCel(if (side < 0) 10 else 21, 20 + (motion.detailPhase % 2), 2, 2, PixelPetReferenceInk.Cream),
            PixelPetReferenceCel(15, 25 + (motion.detailPhase % 2), 2, 1, PixelPetReferenceInk.Shade),
        )
        PixelPetAction.Sleeping -> listOf(
            PixelPetReferenceCel(12, 26, 8, 2, PixelPetReferenceInk.Cream),
            PixelPetReferenceCel(if (side < 0) 8 else 22, 8 - (motion.detailPhase % 3) * 2, 2, 1, PixelPetReferenceInk.Reflection),
        )
        PixelPetAction.Happy,
        PixelPetAction.Petted,
        PixelPetAction.Playing,
        -> listOf(
            PixelPetReferenceCel(8, 10 - (motion.detailPhase % 3), 1, 1, PixelPetReferenceInk.Accent),
            PixelPetReferenceCel(23, 11 - ((motion.detailPhase + 1) % 3), 1, 1, PixelPetReferenceInk.Highlight),
            PixelPetReferenceCel(15, 26 - (motion.detailPhase % 2), 2, 1, PixelPetReferenceInk.Cream),
        )
        PixelPetAction.Frightened -> listOf(
            PixelPetReferenceCel(9 + stride, 12, 1, 2, PixelPetReferenceInk.Reflection),
            PixelPetReferenceCel(22 - stride, 12, 1, 2, PixelPetReferenceInk.Reflection),
            PixelPetReferenceCel(15, 24, 2, 2, PixelPetReferenceInk.Shade),
        )
        PixelPetAction.Cleaning -> listOf(
            PixelPetReferenceCel(if (side < 0) 9 else 22, 14 - (motion.detailPhase % 3), 1, 1, PixelPetReferenceInk.Reflection),
            PixelPetReferenceCel(if (side < 0) 11 else 20, 18 + (motion.detailPhase % 2), 2, 2, PixelPetReferenceInk.Cream),
            PixelPetReferenceCel(16, 26, 2, 1, PixelPetReferenceInk.Highlight),
        )
        PixelPetAction.Exploring,
        PixelPetAction.Watching,
        PixelPetAction.Calling,
        -> listOf(
            PixelPetReferenceCel(10 + stride, 28, 2, 1, PixelPetReferenceInk.Outline),
            PixelPetReferenceCel(20 - stride, 28, 2, 1, PixelPetReferenceInk.Outline),
            PixelPetReferenceCel(if (side < 0) 7 else 24, 13, 1, 2, PixelPetReferenceInk.Reflection),
        )
        PixelPetAction.Hatching -> listOf(
            PixelPetReferenceCel(14 + stride, 12, 3, 1, PixelPetReferenceInk.Highlight),
            PixelPetReferenceCel(12 - stride, 19, 1, 2, PixelPetReferenceInk.Accent),
            PixelPetReferenceCel(19 + stride, 19, 1, 2, PixelPetReferenceInk.Accent),
        )
    }
}

private fun pixelPetSpeciesCels(
    species: PixelPetSpecies,
    stage: PixelPetGrowthStage,
    action: PixelPetAction,
    motion: PixelPetReferenceMotion,
    facing: PixelPetFacing,
): List<PixelPetReferenceCel> {
    if (stage == PixelPetGrowthStage.Baby) return emptyList()
    val side = if (facing == PixelPetFacing.Left) -1 else 1
    val swing = if (motion.detailPhase % 2 == 0) -1 else 1
    val cels = when (species) {
        PixelPetSpecies.Cat -> listOf(
            PixelPetReferenceCel(24 + side * 2, 21 + swing, 1, 5, PixelPetReferenceInk.Outline),
            PixelPetReferenceCel(25 + side * 2, 21 + swing, 1, 4, PixelPetReferenceInk.Base),
            PixelPetReferenceCel(13, 8 + if (motion.detailPhase % 3 == 0) 1 else 0, 1, 1, PixelPetReferenceInk.Highlight),
        )
        PixelPetSpecies.Dog -> listOf(
            PixelPetReferenceCel(24 + side * 2, 22 + swing, 1, 4, PixelPetReferenceInk.Outline),
            PixelPetReferenceCel(25 + side * 2, 22 + swing, 1, 3, PixelPetReferenceInk.Base),
            PixelPetReferenceCel(10, 10 + (motion.detailPhase % 2), 2, 2, PixelPetReferenceInk.Shade),
        )
        PixelPetSpecies.Bird -> listOf(
            PixelPetReferenceCel(8 + swing, 16, 4, 2, PixelPetReferenceInk.Shade),
            PixelPetReferenceCel(9 + swing, 15, 2, 1, PixelPetReferenceInk.Highlight),
            PixelPetReferenceCel(21, 19 + (motion.detailPhase % 2), 2, 1, PixelPetReferenceInk.Accent),
        )
        PixelPetSpecies.Rabbit -> listOf(
            PixelPetReferenceCel(12 + side, 4, 1, 7, PixelPetReferenceInk.Outline),
            PixelPetReferenceCel(13 + side, 5, 1, 6, PixelPetReferenceInk.Cream),
            PixelPetReferenceCel(20 - side, 5 + (motion.detailPhase % 2), 1, 6, PixelPetReferenceInk.Shade),
        )
        PixelPetSpecies.Penguin -> listOf(
            PixelPetReferenceCel(8 + swing, 18, 3, 2, PixelPetReferenceInk.Shade),
            PixelPetReferenceCel(21 - swing, 18, 3, 2, PixelPetReferenceInk.Shade),
            PixelPetReferenceCel(15, 23, 2, 1, PixelPetReferenceInk.Accent),
        )
        PixelPetSpecies.Hamster -> listOf(
            PixelPetReferenceCel(10, 18, 2, 2, PixelPetReferenceInk.Cream),
            PixelPetReferenceCel(21, 18, 2, 2, PixelPetReferenceInk.Cream),
            PixelPetReferenceCel(24 + side, 23 + swing, 1, 2, PixelPetReferenceInk.Accent),
        )
    }
    return if (stage == PixelPetGrowthStage.Adult) {
        cels + listOf(
            PixelPetReferenceCel(14, 6 + (motion.detailPhase % 2), 1, 1, PixelPetReferenceInk.Reflection),
            PixelPetReferenceCel(18, 7 + ((motion.detailPhase + 1) % 2), 1, 1, PixelPetReferenceInk.Accent),
        )
    } else {
        cels
    }
}

private fun pixelPetDirectionCels(
    species: PixelPetSpecies,
    stage: PixelPetGrowthStage,
    action: PixelPetAction,
    motion: PixelPetReferenceMotion,
    facing: PixelPetFacing,
): List<PixelPetReferenceCel> {
    if (stage == PixelPetGrowthStage.Baby) return emptyList()
    return when (facing) {
    PixelPetFacing.Front -> emptyList()
    PixelPetFacing.Back -> {
        val backTone = if (species == PixelPetSpecies.Penguin) PixelPetReferenceInk.Shade else PixelPetReferenceInk.Base
        listOf(
            PixelPetReferenceCel(11, 13, 10, 5, backTone),
            PixelPetReferenceCel(13, 12, 6, 1, PixelPetReferenceInk.Outline),
            PixelPetReferenceCel(14, 15 + (motion.detailPhase % 2), 4, 1, PixelPetReferenceInk.Highlight),
            PixelPetReferenceCel(12, 23, 8, 2, PixelPetReferenceInk.Shade),
        )
    }
    PixelPetFacing.Left,
    PixelPetFacing.Right,
    -> {
        val nearX = if (facing == PixelPetFacing.Left) 10 else 21
        val farX = if (facing == PixelPetFacing.Left) 19 else 12
        val tailX = if (facing == PixelPetFacing.Left) 7 else 24
        val outline = when (species) {
            PixelPetSpecies.Bird -> PixelPetReferenceInk.Shade
            PixelPetSpecies.Penguin -> PixelPetReferenceInk.Cream
            else -> PixelPetReferenceInk.Outline
        }
        listOf(
            PixelPetReferenceCel(nearX, 14, 2, 2, PixelPetReferenceInk.Highlight),
            PixelPetReferenceCel(farX, 15, 1, 1, PixelPetReferenceInk.Shade),
            PixelPetReferenceCel(tailX, 21 + (motion.detailPhase % 2), 2, 3, outline),
            PixelPetReferenceCel(nearX, 25, 3, 1, PixelPetReferenceInk.Cream),
        )
    }
    }
}

/**
 * Species-specific direction cels deliberately redraw the parts that read
 * differently from the front: ears and tails for mammals, folded wings for
 * the bird, a dark back for the penguin, and cheek/stripe details for the
 * hamster. These remain editable grid cells rather than mirrored vector art.
 */
private fun pixelPetDirectionalSilhouetteCels(
    species: PixelPetSpecies,
    stage: PixelPetGrowthStage,
    action: PixelPetAction,
    motion: PixelPetReferenceMotion,
    facing: PixelPetFacing,
): List<PixelPetReferenceCel> {
    if (stage == PixelPetGrowthStage.Baby) return emptyList()
    if (facing == PixelPetFacing.Front) return emptyList()
    val phase = motion.detailPhase
    val stride = if (action in setOf(PixelPetAction.Walking, PixelPetAction.Exploring) && phase % 2 == 0) 1 else 0
    val crownY = when (stage) {
        PixelPetGrowthStage.Egg -> 15
        PixelPetGrowthStage.Baby -> 12
        PixelPetGrowthStage.Young -> 9
        PixelPetGrowthStage.Adult -> 7
    }
    if (facing == PixelPetFacing.Back) {
        return when (species) {
            PixelPetSpecies.Cat -> listOf(
                PixelPetReferenceCel(10, crownY, 2, 5, PixelPetReferenceInk.Outline),
                PixelPetReferenceCel(20, crownY, 2, 5, PixelPetReferenceInk.Outline),
                PixelPetReferenceCel(11, 15, 10, 7, PixelPetReferenceInk.Shade),
                PixelPetReferenceCel(13, 16, 6, 1, PixelPetReferenceInk.Highlight),
                PixelPetReferenceCel(22, 20 + stride, 3, 5, PixelPetReferenceInk.Outline),
            )
            PixelPetSpecies.Dog -> listOf(
                PixelPetReferenceCel(9, crownY + 2, 3, 5, PixelPetReferenceInk.Shade),
                PixelPetReferenceCel(20, crownY + 2, 3, 5, PixelPetReferenceInk.Shade),
                PixelPetReferenceCel(11, 15, 10, 8, PixelPetReferenceInk.Base),
                PixelPetReferenceCel(13, 16, 6, 1, PixelPetReferenceInk.Highlight),
                PixelPetReferenceCel(22, 22 + stride, 4, 3, PixelPetReferenceInk.Outline),
            )
            PixelPetSpecies.Bird -> listOf(
                PixelPetReferenceCel(10, 13, 12, 10, PixelPetReferenceInk.Shade),
                PixelPetReferenceCel(11, 15, 5, 6, PixelPetReferenceInk.Base),
                PixelPetReferenceCel(17, 15, 5, 6, PixelPetReferenceInk.Base),
                PixelPetReferenceCel(15, 20, 2, 4, PixelPetReferenceInk.Highlight),
            )
            PixelPetSpecies.Rabbit -> listOf(
                PixelPetReferenceCel(11, crownY - 3, 2, 10, PixelPetReferenceInk.Outline),
                PixelPetReferenceCel(19, crownY - 3, 2, 10, PixelPetReferenceInk.Outline),
                PixelPetReferenceCel(12, crownY - 2, 1, 8, PixelPetReferenceInk.Cream),
                PixelPetReferenceCel(19, crownY - 2, 1, 8, PixelPetReferenceInk.Shade),
                PixelPetReferenceCel(11, 15, 10, 8, PixelPetReferenceInk.Base),
            )
            PixelPetSpecies.Penguin -> listOf(
                PixelPetReferenceCel(10, 12, 12, 12, PixelPetReferenceInk.Shade),
                PixelPetReferenceCel(12, 14, 8, 8, PixelPetReferenceInk.Outline),
                PixelPetReferenceCel(14, 15, 4, 2, PixelPetReferenceInk.Reflection),
                PixelPetReferenceCel(12, 24 + stride, 3, 2, PixelPetReferenceInk.Accent),
                PixelPetReferenceCel(18, 24 + stride, 3, 2, PixelPetReferenceInk.Accent),
            )
            PixelPetSpecies.Hamster -> listOf(
                PixelPetReferenceCel(10, 14, 12, 9, PixelPetReferenceInk.Base),
                PixelPetReferenceCel(12, 15, 2, 6, PixelPetReferenceInk.Shade),
                PixelPetReferenceCel(18, 15, 2, 6, PixelPetReferenceInk.Shade),
                PixelPetReferenceCel(22, 20 + stride, 2, 2, PixelPetReferenceInk.Accent),
            )
        }
    }

    val side = if (facing == PixelPetFacing.Left) -1 else 1
    val faceX = if (side < 0) 9 else 21
    val tailX = if (side < 0) 6 else 24
    return when (species) {
        PixelPetSpecies.Cat -> listOf(
            PixelPetReferenceCel(faceX, crownY, 3, 4, PixelPetReferenceInk.Outline),
            PixelPetReferenceCel(faceX + side, crownY + 1, 1, 3, PixelPetReferenceInk.Cream),
            PixelPetReferenceCel(tailX, 19 + stride, 2, 7, PixelPetReferenceInk.Outline),
            PixelPetReferenceCel(tailX + side, 20 + stride, 1, 5, PixelPetReferenceInk.Base),
        )
        PixelPetSpecies.Dog -> listOf(
            PixelPetReferenceCel(faceX, crownY + 2, 4, 3, PixelPetReferenceInk.Shade),
            PixelPetReferenceCel(faceX + side, crownY + 3, 2, 2, PixelPetReferenceInk.Base),
            PixelPetReferenceCel(tailX, 21 + stride, 3, 4, PixelPetReferenceInk.Outline),
            PixelPetReferenceCel(tailX + side, 21 + stride, 2, 3, PixelPetReferenceInk.Base),
        )
        PixelPetSpecies.Bird -> listOf(
            PixelPetReferenceCel(faceX, 16, 3, 2, PixelPetReferenceInk.Highlight),
            PixelPetReferenceCel(11 + side, 15, 7, 7, PixelPetReferenceInk.Shade),
            PixelPetReferenceCel(12 + side, 16, 5, 5, PixelPetReferenceInk.Base),
            PixelPetReferenceCel(tailX, 20 + stride, 2, 3, PixelPetReferenceInk.Accent),
        )
        PixelPetSpecies.Rabbit -> listOf(
            PixelPetReferenceCel(faceX, crownY - 3, 2, 11, PixelPetReferenceInk.Outline),
            PixelPetReferenceCel(faceX + side, crownY - 2, 1, 9, PixelPetReferenceInk.Cream),
            PixelPetReferenceCel(tailX, 21 + stride, 3, 3, PixelPetReferenceInk.Cream),
        )
        PixelPetSpecies.Penguin -> listOf(
            PixelPetReferenceCel(10 + side, 15, 5, 8, PixelPetReferenceInk.Shade),
            PixelPetReferenceCel(11 + side, 16, 3, 6, PixelPetReferenceInk.Base),
            PixelPetReferenceCel(faceX, 16, 2, 2, PixelPetReferenceInk.Accent),
            PixelPetReferenceCel(tailX, 23 + stride, 2, 2, PixelPetReferenceInk.Accent),
        )
        PixelPetSpecies.Hamster -> listOf(
            PixelPetReferenceCel(faceX, 16, 2, 3, PixelPetReferenceInk.Cream),
            PixelPetReferenceCel(faceX + side, 18, 1, 2, PixelPetReferenceInk.Accent),
            PixelPetReferenceCel(10 + side, 18, 6, 4, PixelPetReferenceInk.Shade),
            PixelPetReferenceCel(tailX, 22 + stride, 2, 2, PixelPetReferenceInk.Accent),
        )
    }
}

/** Design-board high forms retain one compact live layer while idling. */
internal fun pixelPetHighFormCels(
    species: PixelPetSpecies,
    stage: PixelPetGrowthStage,
    action: PixelPetAction,
    motion: PixelPetReferenceMotion,
    facing: PixelPetFacing,
): List<PixelPetReferenceCel> {
    if (stage != PixelPetGrowthStage.Adult) return emptyList()
    val phase = motion.detailPhase
    val side = if (facing == PixelPetFacing.Left) -1 else 1
    val active = action in setOf(PixelPetAction.Happy, PixelPetAction.Playing, PixelPetAction.Exploring)
    return when (species) {
        PixelPetSpecies.Cat -> listOf(
            PixelPetReferenceCel(14, 4 + phase % 2, 1, 1, PixelPetReferenceInk.Highlight),
            PixelPetReferenceCel(18, 5 + (phase + 1) % 2, 1, 1, PixelPetReferenceInk.Accent),
            PixelPetReferenceCel(8 + side, 20, 1, 1, PixelPetReferenceInk.Accent),
        )
        PixelPetSpecies.Dog -> listOf(
            PixelPetReferenceCel(21 - side, 17, 3, 2, PixelPetReferenceInk.Shade),
            PixelPetReferenceCel(22 - side, 18, 1, 1, PixelPetReferenceInk.Highlight),
            PixelPetReferenceCel(10, 24 + phase % 2, 1, 1, PixelPetReferenceInk.Accent),
        )
        PixelPetSpecies.Bird -> listOf(
            PixelPetReferenceCel(5, 14 + phase % 2, 2, 1, PixelPetReferenceInk.Reflection),
            PixelPetReferenceCel(25, 14 + (phase + 1) % 2, 2, 1, PixelPetReferenceInk.Reflection),
            PixelPetReferenceCel(15, 5, 2, 1, PixelPetReferenceInk.Highlight),
        )
        PixelPetSpecies.Penguin -> listOf(
            PixelPetReferenceCel(14, 3, 4, 1, PixelPetReferenceInk.Accent),
            PixelPetReferenceCel(15, 2, 2, 1, PixelPetReferenceInk.Highlight),
            PixelPetReferenceCel(22, 20 + phase % 2, 1, 1, PixelPetReferenceInk.Reflection),
        )
        PixelPetSpecies.Rabbit -> listOf(
            PixelPetReferenceCel(7 + side, 19, 2, 3, PixelPetReferenceInk.Accent),
            PixelPetReferenceCel(8 + side, 18, 1, 1, PixelPetReferenceInk.Highlight),
            PixelPetReferenceCel(23 - side, 10 + phase % 2, 1, 1, PixelPetReferenceInk.Reflection),
        )
        PixelPetSpecies.Hamster -> listOf(
            PixelPetReferenceCel(14, 6, 4, 1, PixelPetReferenceInk.Accent),
            PixelPetReferenceCel(15, 5, 2, 1, PixelPetReferenceInk.Highlight),
            PixelPetReferenceCel(8, 11 + phase % 2, 1, 1, PixelPetReferenceInk.Reflection),
        )
    }.let { cels ->
        if (active) cels + PixelPetReferenceCel(24, 8 + phase % 3, 1, 1, PixelPetReferenceInk.Highlight) else cels
    }
}

private data class ReferencePoint(val x: Int, val y: Int) {
    operator fun plus(other: ReferencePoint): ReferencePoint =
        ReferencePoint(x + other.x, y + other.y)
}

private const val REFERENCE_ATLAS_PIVOT_X = 16
private const val REFERENCE_ATLAS_BASELINE_Y = 29
private const val REFERENCE_VIEW_PIVOT_X = 6f
private const val REFERENCE_VIEW_BASELINE_Y = 10.35f

internal data class PixelPetReferenceSprite(
    val image: ImageBitmap,
    val opaqueBounds: PixelPetReferenceOpaqueBounds,
)

/** Bounds of the actual character, excluding transparent board padding. */
internal data class PixelPetReferenceOpaqueBounds(
    val minX: Int,
    val minY: Int,
    val maxX: Int,
    val maxY: Int,
) {
    init {
        require(minX <= maxX)
        require(minY <= maxY)
    }

    val centerX: Float get() = (minX + maxX) / 2f
}

internal fun pixelPetOpaqueBounds(bitmap: Bitmap): PixelPetReferenceOpaqueBounds {
    val width = bitmap.width
    val height = bitmap.height
    require(width > 0 && height > 0)
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
    var minX = width
    var minY = height
    var maxX = -1
    var maxY = -1
    pixels.forEachIndexed { index, argb ->
        if ((argb ushr 24) == 0) return@forEachIndexed
        val x = index % width
        val y = index / width
        minX = minOf(minX, x)
        minY = minOf(minY, y)
        maxX = maxOf(maxX, x)
        maxY = maxOf(maxY, y)
    }
    return if (maxX >= minX && maxY >= minY) {
        PixelPetReferenceOpaqueBounds(minX, minY, maxX, maxY)
    } else {
        PixelPetReferenceOpaqueBounds(0, 0, width - 1, height - 1)
    }
}

internal data class PixelPetReferenceRenderLayout(
    val sourcePixel: Int,
    val destinationSize: Int,
    val sourcePivot: Float,
    val sourceBaseline: Float,
)

/**
 * A reference artboard is allowed to use its native pixels only while the
 * complete canvas fits inside the current avatar. Small summary cards used to
 * round a 96px adult board up to 192px, so the right side could be clipped.
 */
internal data class PixelPetReferencePlacement(
    val sourcePixel: Int,
    val destinationSize: Int,
    val offsetX: Int,
    val offsetY: Int,
)

internal fun pixelPetReferencePlacement(
    sourceCanvasSize: Int,
    unit: Float,
    canvasSize: Size,
    motion: PixelPetReferenceMotion,
): PixelPetReferencePlacement = pixelPetReferencePlacement(
    sourceCanvasSize = sourceCanvasSize,
    opaqueBounds = PixelPetReferenceOpaqueBounds(0, 0, sourceCanvasSize - 1, sourceCanvasSize - 1),
    unit = unit,
    canvasSize = canvasSize,
    motion = motion,
)

internal fun pixelPetReferencePlacement(
    sourceCanvasSize: Int,
    opaqueBounds: PixelPetReferenceOpaqueBounds,
    unit: Float,
    canvasSize: Size,
    motion: PixelPetReferenceMotion,
): PixelPetReferencePlacement {
    require(sourceCanvasSize > 0)
    val preferredSourcePixel = pixelPetReferenceRenderLayout(sourceCanvasSize, unit).sourcePixel
    val motionCell = pixelPetIntegerScale(unit)
    val motionX = motion.offsetXCells * motionCell
    val motionY = motion.offsetYCells * motionCell
    require(opaqueBounds.minX >= 0 && opaqueBounds.maxX < sourceCanvasSize)
    require(opaqueBounds.minY >= 0 && opaqueBounds.maxY < sourceCanvasSize)
    val anchorX = canvasSize.width / 2f
    val anchorY = REFERENCE_VIEW_BASELINE_Y * unit

    fun candidate(scale: Int): PixelPetReferencePlacement {
        val destinationSize = sourceCanvasSize * scale
        return PixelPetReferencePlacement(
            sourcePixel = scale,
            destinationSize = destinationSize,
            // Center the visible drawing, rather than the transparent artboard.
            // Its bottom edge is the common foot baseline for every stage.
            offsetX = (anchorX - opaqueBounds.centerX * scale + motionX).roundToInt(),
            offsetY = (anchorY - opaqueBounds.maxY * scale + motionY).roundToInt(),
        )
    }

    for (sourcePixel in preferredSourcePixel downTo 1) {
        val placement = candidate(sourcePixel)
        if (
            placement.offsetX + opaqueBounds.minX * sourcePixel >= 0 &&
            placement.offsetY + opaqueBounds.minY * sourcePixel >= 0 &&
            placement.offsetX + (opaqueBounds.maxX + 1) * sourcePixel <= canvasSize.width.roundToInt() &&
            placement.offsetY + (opaqueBounds.maxY + 1) * sourcePixel <= canvasSize.height.roundToInt()
        ) {
            return placement
        }
    }

    // A tiny host may not fit even a 1:1 source artboard. Keep its center and
    // foot baseline stable, then clamp the final placement instead of drawing
    // beyond the canvas and losing a side of the character.
    val placement = candidate(1)
    val minOffsetX = -opaqueBounds.minX
    val maxOffsetX = (canvasSize.width.roundToInt() - (opaqueBounds.maxX + 1)).coerceAtLeast(minOffsetX)
    val minOffsetY = -opaqueBounds.minY
    val maxOffsetY = (canvasSize.height.roundToInt() - (opaqueBounds.maxY + 1)).coerceAtLeast(minOffsetY)
    return placement.copy(
        offsetX = placement.offsetX.coerceIn(minOffsetX, maxOffsetX),
        offsetY = placement.offsetY.coerceIn(minOffsetY, maxOffsetY),
    )
}

/**
 * Keep the reference sprite's on-screen footprint stable while preserving the
 * authored source pixels. A 64px young form therefore has twice the visible
 * body detail of the former 32px export, rather than being sampled back down
 * onto the old 32-cell target.
 */
internal fun pixelPetReferenceRenderLayout(
    sourceCanvasSize: Int,
    unit: Float,
): PixelPetReferenceRenderLayout {
    require(sourceCanvasSize > 0)
    val commonCell = pixelPetIntegerScale(unit)
    val intendedDisplaySize = PixelPetSpriteAtlas.GRID * commonCell
    val sourcePixel = (intendedDisplaySize.toFloat() / sourceCanvasSize)
        .roundToInt()
        .coerceAtLeast(1)
    return PixelPetReferenceRenderLayout(
        sourcePixel = sourcePixel,
        destinationSize = sourceCanvasSize * sourcePixel,
        sourcePivot = sourceCanvasSize / 2f,
        sourceBaseline = REFERENCE_ATLAS_BASELINE_Y.toFloat() * sourceCanvasSize /
            PixelPetSpriteAtlas.GRID,
    )
}

/** Renders the supplied reference sprite without replacing its body artwork. */
internal fun DrawScope.drawPixelPetReferenceSprite(
    sprite: PixelPetReferenceSprite,
    unit: Float,
    species: PixelPetSpecies,
    stage: PixelPetGrowthStage,
    action: PixelPetAction,
    frame: Int,
    facing: PixelPetFacing,
    colors: PixelPetModelColors,
) {
    // Draw the full native artboard. Earlier builds always forced 16/32/48px
    // source images onto 32 target cells, which erased the face, paws, wings,
    // and tail before the LKM card enlarged it.
    val authoredFrame = PixelPetReferenceSprites.authoredFrame(species, stage, action, frame, facing)
    val motion = authoredFrame.motion
    val placement = pixelPetReferencePlacement(
        sourceCanvasSize = sprite.image.width,
        opaqueBounds = sprite.opaqueBounds,
        unit = unit,
        canvasSize = size,
        motion = motion,
    )
    val pivot = Offset(REFERENCE_VIEW_PIVOT_X * unit, REFERENCE_VIEW_BASELINE_Y * unit)
    withTransform({
        if (facing == PixelPetFacing.Right) {
            scale(scaleX = -1f, scaleY = 1f, pivot = pivot)
        }
    }) {
        drawImage(
            image = sprite.image,
            dstOffset = IntOffset(placement.offsetX, placement.offsetY),
            dstSize = IntSize(
                placement.destinationSize,
                placement.destinationSize,
            ),
            filterQuality = FilterQuality.None,
        )
    }
    // Cels are constrained to external interaction cues. They must never
    // rebuild a species silhouette or overwrite the hand-authored face.
    drawPixelPetReferenceCels(authoredFrame.cels, colors, unit, stage)
}

/**
 * Environmental material is painted on authored body coordinates, not on the
 * card background. Each pass uses sparse, integer-grid pixels so a wet coat,
 * moon rim light, lamp glow, and lagoon reflection follow the pet itself.
 */
internal fun DrawScope.drawPixelPetReferenceEnvironmentMaterial(
    species: PixelPetSpecies,
    stage: PixelPetGrowthStage,
    action: PixelPetAction,
    frame: Int,
    facing: PixelPetFacing,
    habitat: PixelPetHabitat,
    weather: PixelPetWeather,
    night: Boolean,
    warmLight: Boolean,
    colors: PixelPetModelColors,
    unit: Float,
) {
    if (stage == PixelPetGrowthStage.Egg || stage == PixelPetGrowthStage.Baby) return
    val profile = pixelPetReferenceMaterialProfile(species, stage, facing)
    val moving = action in setOf(PixelPetAction.Walking, PixelPetAction.Exploring)
    val phase = frame.mod(PixelPetReferenceSprites.frameCount(action))

    if (weather == PixelPetWeather.Drizzle) {
        profile.wetDrops.forEachIndexed { index, point ->
            val dropShift = if ((phase + index) % 3 == 0) 1 else 0
            referencePixel(colors.reflection.copy(alpha = 0.72f), unit, point.x, point.y + dropShift)
            if ((phase + index) % 2 == 0) {
                referencePixel(colors.highlight.copy(alpha = 0.46f), unit, point.x + 1, point.y + dropShift)
            }
        }
        val rippleY = profile.footY + if (moving && phase % 2 == 0) 1 else 0
        referencePixel(colors.reflection.copy(alpha = 0.52f), unit, profile.centerX - 5, rippleY, 3, 1)
        referencePixel(colors.reflection.copy(alpha = 0.34f), unit, profile.centerX + 2, rippleY, 3, 1)
    }

    if (night) {
        profile.coldRim.forEachIndexed { index, point ->
            val alpha = if ((phase + index) % 3 == 0) 0.64f else 0.42f
            referencePixel(colors.reflection.copy(alpha = alpha), unit, point.x, point.y)
        }
    }

    if (warmLight) {
        profile.warmRim.forEachIndexed { index, point ->
            val alpha = if ((phase + index) % 2 == 0) 0.58f else 0.40f
            referencePixel(Color(0xFFFFC777).copy(alpha = alpha), unit, point.x, point.y)
        }
        referencePixel(Color(0xFFFFE6A4).copy(alpha = 0.48f), unit, profile.centerX, profile.headY + 2)
    }

    if (habitat == PixelPetHabitat.Lagoon) {
        profile.lagoonReflection.forEachIndexed { index, point ->
            val alpha = if ((phase + index) % 2 == 0) 0.52f else 0.34f
            referencePixel(colors.reflection.copy(alpha = alpha), unit, point.x, point.y)
        }
        if (moving) {
            referencePixel(colors.accent.copy(alpha = 0.32f), unit, profile.centerX - 2, profile.footY + 1, 4, 1)
        }
    }
}

private data class PixelPetReferenceMaterialProfile(
    val centerX: Int,
    val headY: Int,
    val footY: Int,
    val wetDrops: List<ReferencePoint>,
    val coldRim: List<ReferencePoint>,
    val warmRim: List<ReferencePoint>,
    val lagoonReflection: List<ReferencePoint>,
)

private fun pixelPetReferenceMaterialProfile(
    species: PixelPetSpecies,
    stage: PixelPetGrowthStage,
    facing: PixelPetFacing,
): PixelPetReferenceMaterialProfile {
    val headY = when (stage) {
        PixelPetGrowthStage.Egg -> 16
        PixelPetGrowthStage.Baby -> 13
        PixelPetGrowthStage.Young -> 10
        PixelPetGrowthStage.Adult -> 8
    }
    val footY = when (stage) {
        PixelPetGrowthStage.Baby -> 27
        else -> 29
    }
    val centerX = when (facing) {
        PixelPetFacing.Left -> 14
        PixelPetFacing.Right -> 18
        PixelPetFacing.Front,
        PixelPetFacing.Back,
        -> 16
    }
    val left = centerX - 5
    val right = centerX + 5
    val litSide = when (facing) {
        PixelPetFacing.Left -> left
        PixelPetFacing.Right -> right
        PixelPetFacing.Back -> left
        PixelPetFacing.Front -> right
    }
    val speciesShift = when (species) {
        PixelPetSpecies.Bird -> -1
        PixelPetSpecies.Rabbit -> -2
        PixelPetSpecies.Penguin -> 1
        PixelPetSpecies.Hamster -> 1
        PixelPetSpecies.Cat,
        PixelPetSpecies.Dog,
        -> 0
    }
    return PixelPetReferenceMaterialProfile(
        centerX = centerX,
        headY = headY,
        footY = footY,
        wetDrops = listOf(
            ReferencePoint(centerX - 3, headY + 4 + speciesShift),
            ReferencePoint(centerX + 2, headY + 7),
        ),
        coldRim = listOf(
            ReferencePoint(left, headY + 4),
            ReferencePoint(left - 1, headY + 8),
            ReferencePoint(left, headY + 12),
            ReferencePoint(right, headY + 10),
        ),
        warmRim = listOf(
            ReferencePoint(litSide, headY + 5),
            ReferencePoint(litSide + if (litSide < centerX) 1 else -1, headY + 9),
            ReferencePoint(litSide, headY + 13),
        ),
        lagoonReflection = listOf(
            ReferencePoint(centerX - 3, footY - 3),
            ReferencePoint(centerX - 1, footY - 2),
            ReferencePoint(centerX + 2, footY - 3),
        ),
    )
}

internal fun DrawScope.drawPixelPetHamsterWheel(
    image: ImageBitmap,
    phase: Float,
) {
    val targetSize = PixelPetSpriteAtlas.GRID * (size.minDimension / 42f).coerceAtLeast(1f).roundToInt()
    val sourcePixel = (targetSize.toFloat() / image.width).roundToInt().coerceAtLeast(1)
    val width = image.width * sourcePixel
    val height = image.height * sourcePixel
    val centerX = (size.width * 0.76f).roundToInt()
    val baseline = (size.height * 0.735f).roundToInt()
    val wobble = if ((phase * 10f).toInt() % 2 == 0) 0 else sourcePixel
    drawImage(
        image = image,
        dstOffset = IntOffset(centerX - width / 2, baseline - height + wobble),
        dstSize = IntSize(width, height),
        filterQuality = FilterQuality.None,
    )
}

private fun DrawScope.drawPixelPetReferenceDirectionDetails(
    species: PixelPetSpecies,
    stage: PixelPetGrowthStage,
    action: PixelPetAction,
    motion: PixelPetReferenceMotion,
    facing: PixelPetFacing,
    colors: PixelPetModelColors,
    unit: Float,
) {
    if (stage == PixelPetGrowthStage.Baby) return
    if (facing == PixelPetFacing.Front) return
    val direction = if (facing == PixelPetFacing.Left) -1 else 1
    val tailSwing = if (motion.detailPhase % 2 == 0) 1 else -1
    when (species) {
        PixelPetSpecies.Cat -> {
            referencePixel(colors.outline, unit, 24 + direction * 2, 22 + tailSwing, 1, 5)
            referencePixel(colors.base, unit, 25 + direction * 2, 22 + tailSwing, 1, 4)
        }
        PixelPetSpecies.Dog -> {
            referencePixel(colors.outline, unit, 24 + direction * 2, 22 + tailSwing, 1, 4)
            referencePixel(colors.base, unit, 25 + direction * 2, 21 + tailSwing, 1, 3)
        }
        PixelPetSpecies.Bird -> {
            referencePixel(colors.shade, unit, 9, 16 + tailSwing, 4, 2)
            referencePixel(colors.highlight, unit, 10, 16 + tailSwing, 2, 1)
        }
        PixelPetSpecies.Rabbit -> {
            referencePixel(colors.outline, unit, 13 + direction, 4, 1, 6)
            referencePixel(colors.cream, unit, 14 + direction, 5, 1, 5)
        }
        PixelPetSpecies.Penguin -> {
            referencePixel(colors.shade, unit, 13, 12, 6, 2)
            referencePixel(colors.reflection, unit, 14, 12, 3, 1)
        }
        PixelPetSpecies.Hamster -> {
            referencePixel(colors.accent, unit, 22 + direction, 17, 1, 1)
            referencePixel(colors.cream, unit, 11, 18, 3, 2)
        }
    }
    if (facing == PixelPetFacing.Back) {
        referencePixel(colors.shade.copy(alpha = 0.78f), unit, 12, 14, 8, 3)
        referencePixel(colors.highlight.copy(alpha = 0.42f), unit, 14, 14, 3, 1)
    }
    if (stage == PixelPetGrowthStage.Adult && action in setOf(PixelPetAction.Happy, PixelPetAction.Playing)) {
        referencePixel(colors.accent.copy(alpha = 0.78f), unit, 15, 5 + (motion.detailPhase % 2), 2, 1)
    }
}

private fun DrawScope.drawPixelPetReferenceActionDetails(
    species: PixelPetSpecies,
    stage: PixelPetGrowthStage,
    action: PixelPetAction,
    motion: PixelPetReferenceMotion,
    facing: PixelPetFacing,
    colors: PixelPetModelColors,
    unit: Float,
) {
    if (stage == PixelPetGrowthStage.Baby) return
    val direction = if (facing == PixelPetFacing.Left) -1 else 1
    when (action) {
        PixelPetAction.Walking,
        PixelPetAction.Exploring,
        -> {
            val stepX = if (motion.detailPhase % 2 == 0) 12 else 19
            referencePixel(colors.shade.copy(alpha = 0.52f), unit, stepX, 29, 2, 1)
            if (species == PixelPetSpecies.Bird) {
                referencePixel(colors.highlight, unit, 8 + direction, 17 + (motion.detailPhase % 2), 2, 1)
            }
        }
        PixelPetAction.Eating -> {
            val foodX = if (facing == PixelPetFacing.Left) 9 else 21
            referencePixel(colors.accent, unit, foodX, 22 + (motion.detailPhase % 2), 2, 2)
            referencePixel(colors.highlight, unit, foodX + direction, 22, 1, 1)
        }
        PixelPetAction.Sleeping -> {
            val zX = if (facing == PixelPetFacing.Left) 9 else 22
            val zY = 8 - (motion.detailPhase % 3) * 2
            referencePixel(colors.reflection.copy(alpha = 0.88f), unit, zX, zY, 2, 1)
            referencePixel(colors.reflection.copy(alpha = 0.72f), unit, zX + 1, zY - 1, 1, 2)
        }
        PixelPetAction.Happy,
        PixelPetAction.Petted,
        PixelPetAction.Playing,
        -> {
            referencePixel(colors.accent.copy(alpha = 0.84f), unit, 8, 10 - (motion.detailPhase % 3), 1, 1)
            referencePixel(colors.highlight.copy(alpha = 0.92f), unit, 23, 12 - (motion.detailPhase % 2), 1, 1)
        }
        PixelPetAction.Frightened -> {
            referencePixel(colors.reflection.copy(alpha = 0.88f), unit, 22, 10 + (motion.detailPhase % 2), 1, 2)
        }
        PixelPetAction.Cleaning -> {
            val bubbleX = if (facing == PixelPetFacing.Left) 10 else 21
            referencePixel(colors.reflection.copy(alpha = 0.72f), unit, bubbleX, 13 - (motion.detailPhase % 3), 1, 1)
            referencePixel(colors.highlight.copy(alpha = 0.66f), unit, bubbleX + direction, 15 - (motion.detailPhase % 2), 1, 1)
        }
        PixelPetAction.Hatching -> {
            referencePixel(colors.highlight.copy(alpha = 0.88f), unit, 14 + motion.offsetXCells, 12, 3, 1)
        }
        else -> Unit
    }
    if (stage == PixelPetGrowthStage.Adult) {
        val glow = if (action in setOf(PixelPetAction.Happy, PixelPetAction.Playing, PixelPetAction.Exploring)) 0.72f else 0.42f
        referencePixel(colors.reflection.copy(alpha = glow), unit, 10, 7 + (motion.detailPhase % 2), 1, 1)
        referencePixel(colors.accent.copy(alpha = glow), unit, 21, 8 + ((motion.detailPhase + 1) % 2), 1, 1)
    }
}

private fun DrawScope.drawPixelPetReferenceCels(
    cels: List<PixelPetReferenceCel>,
    colors: PixelPetModelColors,
    unit: Float,
    stage: PixelPetGrowthStage,
) {
    cels.forEach { cel ->
        if (cel.x !in -2..33 || cel.y !in -2..33) return@forEach
        val color = when (cel.ink) {
            PixelPetReferenceInk.Outline -> colors.outline
            PixelPetReferenceInk.Base -> colors.base
            PixelPetReferenceInk.Shade -> colors.shade
            PixelPetReferenceInk.Cream -> colors.cream
            PixelPetReferenceInk.Highlight -> colors.highlight
            PixelPetReferenceInk.Accent -> colors.accent
            PixelPetReferenceInk.Reflection -> colors.reflection
        }
        referencePixel(color, unit, stage, cel.x, cel.y, cel.width, cel.height)
    }
}

/**
 * Eyes, mouth, and cheeks are a separate layer from the 32px body source.
 * It lets wellbeing, weather, and furniture actions change expression without
 * repainting a species' body or invalidating its equipment anchors.
 */
internal fun DrawScope.drawPixelPetReferenceFaceLayer(
    species: PixelPetSpecies,
    stage: PixelPetGrowthStage,
    action: PixelPetAction,
    frame: Int,
    facing: PixelPetFacing,
    expression: PixelPetExpression,
    colors: PixelPetModelColors,
    unit: Float,
) {
    if (
        stage == PixelPetGrowthStage.Egg ||
            stage == PixelPetGrowthStage.Baby ||
            facing == PixelPetFacing.Back
    ) return
    val anchor = PixelPetReferenceSprites.accessoryAnchor(
        species = species,
        stage = stage,
        action = action,
        frame = frame,
        slot = PixelPetAccessorySlot.Head,
        facing = facing,
        unit = unit,
    )
    val side = when (facing) {
        PixelPetFacing.Left -> -1
        PixelPetFacing.Right -> 1
        else -> 0
    }
    val eyeSpacing = when (species) {
        PixelPetSpecies.Bird,
        PixelPetSpecies.Hamster,
        -> 1
        PixelPetSpecies.Rabbit -> 2
        else -> 2
    }
    val eyeY = if (stage == PixelPetGrowthStage.Baby) 0 else -1
    fun facePixel(
        color: Color,
        dx: Int,
        dy: Int,
        width: Int = 1,
        height: Int = 1,
    ) = referenceAnchorPixel(color, unit, anchor, dx, dy, width, height)

    val front = facing == PixelPetFacing.Front
    val blink = action == PixelPetAction.Idle && frame.mod(PixelPetReferenceSprites.frameCount(action)) in 5..6
    if (expression == PixelPetExpression.Sleepy || blink) {
        facePixel(colors.outline.copy(alpha = 0.86f), -eyeSpacing, eyeY, 1, 1)
        if (front) facePixel(colors.outline.copy(alpha = 0.86f), eyeSpacing, eyeY, 1, 1)
    } else if (expression == PixelPetExpression.Startled) {
        facePixel(colors.reflection, -eyeSpacing, eyeY - 1, 1, 2)
        if (front) facePixel(colors.reflection, eyeSpacing, eyeY - 1, 1, 2)
        facePixel(colors.accent, side, 2, 1, 1)
    } else {
        facePixel(colors.eye, -eyeSpacing, eyeY, 1, 1)
        if (front) facePixel(colors.eye, eyeSpacing, eyeY, 1, 1)
        if (expression == PixelPetExpression.Curious) {
            facePixel(colors.reflection, -eyeSpacing, eyeY, 1, 1)
            if (front) facePixel(colors.reflection, eyeSpacing, eyeY, 1, 1)
        }
    }
    when (expression) {
        PixelPetExpression.Hungry -> {
            facePixel(colors.accent.copy(alpha = 0.78f), -eyeSpacing - 1, 1)
            if (front) facePixel(colors.accent.copy(alpha = 0.78f), eyeSpacing + 1, 1)
        }
        PixelPetExpression.Delighted -> {
            facePixel(colors.accent.copy(alpha = 0.82f), -eyeSpacing - 1, 1)
            if (front) facePixel(colors.accent.copy(alpha = 0.82f), eyeSpacing + 1, 1)
            facePixel(colors.highlight, side, 2)
        }
        PixelPetExpression.Curious -> facePixel(colors.reflection.copy(alpha = 0.72f), side, 2)
        PixelPetExpression.Sleepy -> facePixel(colors.shade.copy(alpha = 0.78f), side, 2)
        PixelPetExpression.Content,
        PixelPetExpression.Startled,
        -> Unit
    }
}

private fun DrawScope.referenceAnchorPixel(
    color: Color,
    unit: Float,
    anchor: PixelPetAccessoryAnchor,
    dx: Int,
    dy: Int,
    width: Int = 1,
    height: Int = 1,
) {
    val cell = pixelPetIntegerScale(unit).toFloat()
    val left = (anchor.x * unit + dx * cell).roundToInt().toFloat()
    val top = (anchor.y * unit + dy * cell).roundToInt().toFloat()
    drawRect(color, Offset(left, top), Size(cell * width, cell * height))
}

private fun DrawScope.referencePixel(
    color: Color,
    unit: Float,
    stage: PixelPetGrowthStage,
    cellX: Int,
    cellY: Int,
    width: Int = 1,
    height: Int = 1,
) {
    val native = pixelPetReferenceNativeRect(stage, cellX, cellY, width, height)
    val layout = pixelPetReferenceRenderLayout(stage.sourceCanvasSize, unit)
    val sourcePixel = layout.sourcePixel.toFloat()
    val sourcePivot = layout.sourcePivot
    val sourceBaseline = layout.sourceBaseline
    val left = (
        REFERENCE_VIEW_PIVOT_X * unit + (native.x - sourcePivot) * sourcePixel
        ).roundToInt().toFloat()
    val top = (
        REFERENCE_VIEW_BASELINE_Y * unit + (native.y - sourceBaseline) * sourcePixel
        ).roundToInt().toFloat()
    drawRect(
        color,
        Offset(left, top),
        Size(sourcePixel * native.width, sourcePixel * native.height),
    )
}

private fun DrawScope.referencePixel(
    color: Color,
    unit: Float,
    cellX: Int,
    cellY: Int,
    width: Int = 1,
    height: Int = 1,
) {
    val cell = pixelPetIntegerScale(unit).toFloat()
    val left = (REFERENCE_VIEW_PIVOT_X * unit + (cellX - REFERENCE_ATLAS_PIVOT_X) * cell).roundToInt().toFloat()
    val top = (REFERENCE_VIEW_BASELINE_Y * unit + (cellY - REFERENCE_ATLAS_BASELINE_Y) * cell).roundToInt().toFloat()
    drawRect(color, Offset(left, top), Size(cell * width, cell * height))
}
