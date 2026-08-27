package me.weishu.kernelsu.ui.component.pixel

import android.graphics.Bitmap
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.util.LruCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import java.io.BufferedInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.LinkedHashMap
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

internal sealed interface PixelPetSpritePackState {
    data object Unprepared : PixelPetSpritePackState
    data object Ready : PixelPetSpritePackState
    data class Unavailable(val reason: String) : PixelPetSpritePackState
}

private const val PIXEL_PET_ATLAS_PIVOT_X = 16f
private const val PIXEL_PET_ATLAS_BASELINE_Y = 29f
private const val PIXEL_PET_VIEW_PIVOT_X = 6f
private const val PIXEL_PET_VIEW_BASELINE_Y = 10.35f
private const val PIXEL_PET_ATTACHMENT_SLOT_COUNT = 6
private const val PIXEL_PET_ATTACHMENT_BYTES = PIXEL_PET_ATTACHMENT_SLOT_COUNT * 3

internal data class PixelPetSpriteCell(val x: Int, val y: Int, val value: Char)

internal data class PixelPetSpriteFrame(
    val width: Int,
    val height: Int,
    val cells: List<PixelPetSpriteCell>,
    val pivotCellX: Int = PIXEL_PET_ATLAS_PIVOT_X.toInt(),
    val baselineCellY: Int = PIXEL_PET_ATLAS_BASELINE_Y.toInt(),
    val attachments: PixelPetSpriteAttachments? = null,
)

internal data class PixelPetSpriteLayout(
    val minX: Int,
    val minY: Int,
    val contentWidth: Int,
    val contentHeight: Int,
    val cellUnit: Int,
    val originX: Float,
    val originY: Float,
    val logicalCell: Float,
    val pivotCellX: Float,
    val baselineCellY: Float,
)

internal data class PixelPetAccessoryAnchor(
    val x: Float,
    val y: Float,
    val scale: Float,
)

/**
 * Attachment metadata lives on an authored frame rather than being inferred
 * from its visible bounds. This keeps equipment aligned while limbs, tails,
 * and action particles change the silhouette.
 */
internal enum class PixelPetAccessoryRenderLayer {
    BehindModel,
    BodyOverlay,
    FrontModel,
}

internal data class PixelPetSpriteAttachment(
    val x: Int,
    val y: Int,
    val layer: PixelPetAccessoryRenderLayer,
)

internal data class PixelPetSpriteAttachments(
    val head: PixelPetSpriteAttachment,
    val back: PixelPetSpriteAttachment,
    val hand: PixelPetSpriteAttachment,
    val neck: PixelPetSpriteAttachment,
    val tail: PixelPetSpriteAttachment,
    val trail: PixelPetSpriteAttachment,
) {
    fun forSlot(slot: PixelPetAccessorySlot): PixelPetSpriteAttachment = when (slot) {
        PixelPetAccessorySlot.Head -> head
        PixelPetAccessorySlot.Back -> back
        PixelPetAccessorySlot.Hand -> hand
        PixelPetAccessorySlot.Neck -> neck
        PixelPetAccessorySlot.Tail -> tail
        PixelPetAccessorySlot.Trail -> trail
    }
}

private fun pixelPetSpriteAttachments(
    positions: IntArray,
    layers: IntArray,
    width: Int,
    height: Int,
): PixelPetSpriteAttachments {
    require(positions.size == PIXEL_PET_ATTACHMENT_SLOT_COUNT)
    require(layers.size == PIXEL_PET_ATTACHMENT_SLOT_COUNT)
    fun attachment(slot: PixelPetAccessorySlot): PixelPetSpriteAttachment {
        val position = positions[slot.ordinal]
        val layer = PixelPetAccessoryRenderLayer.entries.getOrElse(layers[slot.ordinal]) {
            PixelPetAccessoryRenderLayer.FrontModel
        }
        return PixelPetSpriteAttachment(
            x = (position % width).coerceIn(0, width - 1),
            y = (position / width).coerceIn(0, height - 1),
            layer = layer,
        )
    }
    return PixelPetSpriteAttachments(
        head = attachment(PixelPetAccessorySlot.Head),
        back = attachment(PixelPetAccessorySlot.Back),
        hand = attachment(PixelPetAccessorySlot.Hand),
        neck = attachment(PixelPetAccessorySlot.Neck),
        tail = attachment(PixelPetAccessorySlot.Tail),
        trail = attachment(PixelPetAccessorySlot.Trail),
    )
}

private data class PixelPetGridPoint(val x: Int, val y: Int) {
    operator fun plus(other: PixelPetGridPoint): PixelPetGridPoint =
        PixelPetGridPoint(x + other.x, y + other.y)
}

private data class PixelPetAccessoryAnchorCells(
    val head: PixelPetGridPoint,
    val back: PixelPetGridPoint,
    val hand: PixelPetGridPoint,
    val neck: PixelPetGridPoint = head,
    val tail: PixelPetGridPoint = back,
    val trail: PixelPetGridPoint = PixelPetGridPoint(16, 29),
) {
    fun point(slot: PixelPetAccessorySlot): PixelPetGridPoint = when (slot) {
        PixelPetAccessorySlot.Head -> head
        PixelPetAccessorySlot.Back -> back
        PixelPetAccessorySlot.Hand -> hand
        PixelPetAccessorySlot.Neck -> neck
        PixelPetAccessorySlot.Tail -> tail
        PixelPetAccessorySlot.Trail -> trail
    }

    operator fun plus(other: PixelPetAccessoryAnchorCells): PixelPetAccessoryAnchorCells =
        PixelPetAccessoryAnchorCells(
            head = head + other.head,
            back = back + other.back,
            hand = hand + other.hand,
            neck = neck + other.neck,
            tail = tail + other.tail,
            trail = trail + other.trail,
        )
}

internal enum class PixelPetFacing {
    Front,
    Back,
    Left,
    Right,
}

/**
 * Native 32x32 pixel atlas. Base silhouettes are authored directly on the
 * output pixel grid and finalized frame recipes produce one immutable pixel
 * map before rendering; no vector primitives are used for the model.
 */
internal object PixelPetSpriteAtlas {
    /** Legacy v1/v2 artboard size and the target on-card footprint. */
    internal const val GRID = 32
    internal const val FRAME_CACHE_BYTE_BUDGET = 384 * 1024
    private const val BAKED_PACK_MANIFEST_ASSET = "pixel_pet/v5/manifest.properties"
    private const val BAKED_SHEET_MAGIC = 0x50505431 // PPT1
    private const val BAKED_SHEET_VERSION_V1 = 1
    private const val BAKED_SHEET_VERSION_V2 = 2
    private const val BAKED_SHEET_VERSION_V3 = 3
    private const val MAX_SPRITE_DIMENSION = 64
    private const val MAX_RESIDENT_BAKED_PACKS = 2

    private data class FrameKey(
        val species: PixelPetSpecies,
        val stage: PixelPetGrowthStage,
        val action: PixelPetAction,
        val frame: Int,
        val facing: PixelPetFacing,
    )

    private val frameCache = LinkedHashMap<FrameKey, PixelPetSpriteFrame>(96, 0.75f, true)
    private var frameCacheBytes = 0
    private val bakedFramePacks = LinkedHashMap<PixelPetSpecies, PixelPetBakedFrameSheet>(4, 0.75f, true)
    @Volatile
    private var bakedPackManifest: Map<PixelPetSpecies, PixelPetSpritePackInfo>? = null
    @Volatile
    private var bakedPackManifestLoadFailed = false
    private val bakedPackLoadFailures = mutableMapOf<PixelPetSpecies, String>()
    @Volatile
    private var memoryCallbacksRegistered = false

    private data class PixelPetSpritePackInfo(
        val assetPath: String,
        val sha256: String,
        val frameCount: Int,
        val formatVersion: Int,
    )

    fun registerMemoryCallbacks(context: Context) {
        if (memoryCallbacksRegistered) return
        synchronized(this) {
            if (memoryCallbacksRegistered) return
            context.applicationContext.registerComponentCallbacks(object : ComponentCallbacks2 {
                override fun onConfigurationChanged(newConfig: Configuration) = Unit

                @Deprecated("Required by ComponentCallbacks2 on supported Android releases")
                override fun onLowMemory() = clearCaches(dropBakedSheet = true)

                @Suppress("DEPRECATION")
                @Deprecated("Required by ComponentCallbacks2 on supported Android releases")
                override fun onTrimMemory(level: Int) {
                    when {
                        level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> clearCaches(dropBakedSheet = true)
                        level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> trimCaches(keepFraction = 0.25f)
                        level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> trimCaches(keepFraction = 0.5f)
                    }
                }
            })
            memoryCallbacksRegistered = true
        }
    }

    fun clearCaches(dropBakedSheet: Boolean = false) {
        synchronized(frameCache) {
            frameCache.clear()
            frameCacheBytes = 0
        }
        PixelPetSpriteBitmapCache.clear()
        if (dropBakedSheet) {
            synchronized(this) {
                bakedFramePacks.clear()
                bakedPackManifest = null
                bakedPackManifestLoadFailed = false
                bakedPackLoadFailures.clear()
            }
        }
    }

    /** Keeps verified packs while making local Sprite Studio edits visible immediately. */
    internal fun invalidateEditedFrames() = clearCaches(dropBakedSheet = false)

    internal fun cacheStats(): Pair<Int, Int> = synchronized(frameCache) {
        frameCache.size to frameCacheBytes
    }

    private fun trimCaches(keepFraction: Float) {
        synchronized(frameCache) {
            val targetBytes = (FRAME_CACHE_BYTE_BUDGET * keepFraction).roundToInt()
            while (frameCacheBytes > targetBytes && frameCache.isNotEmpty()) {
                val iterator = frameCache.entries.iterator()
                val eldest = iterator.next()
                frameCacheBytes -= frameByteCost(eldest.value)
                iterator.remove()
            }
        }
        PixelPetSpriteBitmapCache.trimToFraction(keepFraction)
    }

    /**
     * Verifies and indexes one shipped native pack. Call this from a background
     * coroutine before rendering; Canvas must never perform asset I/O.
     */
    fun prepare(context: Context, species: PixelPetSpecies): PixelPetSpritePackState {
        registerMemoryCallbacks(context)
        synchronized(this) {
            if (species in bakedFramePacks) return PixelPetSpritePackState.Ready
            bakedPackLoadFailures[species]?.let { return PixelPetSpritePackState.Unavailable(it) }
            return runCatching { loadVerifiedPack(context.applicationContext, species) }
                .fold(
                    onSuccess = { sheet ->
                        PixelPetSpriteDraftStore.preload(context.applicationContext, species)
                        bakedFramePacks[species] = sheet
                        while (bakedFramePacks.size > MAX_RESIDENT_BAKED_PACKS) {
                            bakedFramePacks.entries.iterator().run {
                                if (hasNext()) {
                                    next()
                                    remove()
                                }
                            }
                        }
                        PixelPetSpritePackState.Ready
                    },
                    onFailure = { error ->
                        val reason = error.message ?: "Unable to load verified pixel pet frames"
                        bakedPackLoadFailures[species] = reason
                        PixelPetSpritePackState.Unavailable(reason)
                    },
                )
        }
    }

    fun packState(species: PixelPetSpecies): PixelPetSpritePackState = synchronized(this) {
        when {
            species in bakedFramePacks -> PixelPetSpritePackState.Ready
            species in bakedPackLoadFailures -> PixelPetSpritePackState.Unavailable(
                checkNotNull(bakedPackLoadFailures[species]),
            )
            else -> PixelPetSpritePackState.Unprepared
        }
    }

    /** Returns a frame only when its verified pack has already been prepared. */
    fun loadedFrame(
        species: PixelPetSpecies,
        stage: PixelPetGrowthStage,
        action: PixelPetAction,
        frame: Int,
        facing: PixelPetFacing = PixelPetFacing.Front,
    ): PixelPetSpriteFrame? {
        val key = FrameKey(species, stage, action, normalizeFrame(action, frame), facing)
        synchronized(frameCache) {
            frameCache[key]?.let { return it }
            val built = synchronized(this) {
                bakedFramePacks[species]?.frame(species, stage, action, key.frame, facing)
            } ?: return null
            val edited = PixelPetSpriteDraftStore.applyLoaded(
                key = PixelPetSpriteEditorKey(species, stage, action, key.frame, facing),
                frame = built,
            )
            val cost = frameByteCost(edited)
            while (frameCacheBytes + cost > FRAME_CACHE_BYTE_BUDGET && frameCache.isNotEmpty()) {
                val iterator = frameCache.entries.iterator()
                val eldest = iterator.next()
                frameCacheBytes -= frameByteCost(eldest.value)
                iterator.remove()
            }
            frameCache[key] = edited
            frameCacheBytes += cost
            return edited
        }
    }

    /**
     * Compatibility accessor for code that already runs after a background
     * prepare call. It intentionally performs no asset or preferences I/O.
     */
    internal fun frame(
        species: PixelPetSpecies,
        stage: PixelPetGrowthStage,
        action: PixelPetAction,
        frame: Int,
        facing: PixelPetFacing = PixelPetFacing.Front,
    ): PixelPetSpriteFrame = requireNotNull(loadedFrame(species, stage, action, frame, facing)) {
        "Pixel pet sprite pack for $species has not been prepared"
    }

    /** JVM tests install only verified decoded data; production uses prepare(). */
    internal fun installVerifiedPacksForTest(packs: Map<PixelPetSpecies, PixelPetBakedFrameSheet>) {
        synchronized(this) {
            bakedFramePacks.clear()
            bakedFramePacks.putAll(packs)
            bakedPackLoadFailures.clear()
        }
        synchronized(frameCache) {
            frameCache.clear()
            frameCacheBytes = 0
        }
    }

    private fun loadVerifiedPack(context: Context, species: PixelPetSpecies): PixelPetBakedFrameSheet {
        val pack = loadBakedPackManifest(context)[species]
            ?: error("Missing $species sprite pack declaration")
        val raw = context.assets.open(pack.assetPath).use { input ->
            BufferedInputStream(input).readBytes()
        }
        require(raw.sha256() == pack.sha256) { "Invalid $species sprite pack checksum" }
        return PixelPetBakedFrameSheet(raw).also { sheet ->
            require(sheet.frameCount == pack.frameCount) {
                "Invalid $species sprite pack frame count"
            }
            require(sheet.formatVersion == pack.formatVersion) {
                "Invalid $species sprite pack format"
            }
            require(sheet.formatVersion == BAKED_SHEET_VERSION_V3) {
                "Pixel pet v5 packs must include native frame geometry"
            }
            sheet.requireCompleteAuthoredFrames(species)
        }
    }

    private fun loadBakedPackManifest(context: Context): Map<PixelPetSpecies, PixelPetSpritePackInfo> {
        bakedPackManifest?.let { return it }
        check(!bakedPackManifestLoadFailed) { "Pixel pet sprite pack manifest is unavailable" }
        val loaded = runCatching {
            context.assets.open(BAKED_PACK_MANIFEST_ASSET).bufferedReader().useLines { lines ->
                val values = lines
                    .map(String::trim)
                    .filter { it.isNotEmpty() && !it.startsWith("#") }
                    .mapNotNull { line ->
                        val separator = line.indexOf('=')
                        line.takeIf { separator > 0 }?.let {
                            it.substring(0, separator) to it.substring(separator + 1)
                        }
                    }
                    .toMap()
                require(values["version"] == "5") { "Unsupported pixel pet sprite pack manifest" }
                PixelPetSpecies.entries.associateWith { species ->
                    val prefix = species.name.lowercase()
                    PixelPetSpritePackInfo(
                        assetPath = requireNotNull(values["$prefix.asset"]),
                        sha256 = requireNotNull(values["$prefix.sha256"]),
                        frameCount = requireNotNull(values["$prefix.frames"]?.toIntOrNull()),
                        formatVersion = requireNotNull(values["$prefix.format"]?.toIntOrNull()),
                    )
                }
            }
        }
        bakedPackManifest = loaded.getOrNull()
        bakedPackManifestLoadFailed = loaded.isFailure
        return requireNotNull(bakedPackManifest) { "Pixel pet sprite pack manifest is unavailable" }
    }

    internal class PixelPetBakedFrameSheet(private val raw: ByteArray) {
        private data class FrameOffset(
            val start: Int,
            val cellCount: Int,
            val width: Int,
            val height: Int,
            val pivotCellX: Int,
            val baselineCellY: Int,
            val attachments: PixelPetSpriteAttachments?,
        )

        private val offsets = HashMap<FrameKey, FrameOffset>()

        val frameCount: Int get() = offsets.size
        val formatVersion: Int

        init {
            val buffer = ByteBuffer.wrap(raw).order(ByteOrder.BIG_ENDIAN)
            require(buffer.int == BAKED_SHEET_MAGIC) { "Invalid pixel pet sprite sheet" }
            formatVersion = buffer.get().toInt() and 0xFF
            require(formatVersion in BAKED_SHEET_VERSION_V1..BAKED_SHEET_VERSION_V3) {
                "Unsupported pixel pet sprite sheet"
            }
            val count = buffer.int
            repeat(count) {
                val species = PixelPetSpecies.entries.getOrNull(buffer.get().toInt() and 0xFF)
                val stage = PixelPetGrowthStage.entries.getOrNull(buffer.get().toInt() and 0xFF)
                val action = PixelPetAction.entries.getOrNull(buffer.get().toInt() and 0xFF)
                val facing = PixelPetFacing.entries.getOrNull(buffer.get().toInt() and 0xFF)
                val frame = buffer.get().toInt() and 0xFF
                val width: Int
                val height: Int
                val pivotCellX: Int
                val baselineCellY: Int
                if (formatVersion >= BAKED_SHEET_VERSION_V3) {
                    width = buffer.get().toInt() and 0xFF
                    height = buffer.get().toInt() and 0xFF
                    pivotCellX = buffer.get().toInt() and 0xFF
                    baselineCellY = buffer.get().toInt() and 0xFF
                    require(width in 1..MAX_SPRITE_DIMENSION && height in 1..MAX_SPRITE_DIMENSION) {
                        "Invalid native pixel pet artboard"
                    }
                    require(pivotCellX in 0 until width && baselineCellY in 0 until height) {
                        "Invalid native pixel pet frame anchors"
                    }
                } else {
                    width = GRID
                    height = GRID
                    pivotCellX = PIXEL_PET_ATLAS_PIVOT_X.toInt()
                    baselineCellY = PIXEL_PET_ATLAS_BASELINE_Y.toInt()
                }
                val cellCount = buffer.short.toInt() and 0xFFFF
                val attachments = if (formatVersion >= BAKED_SHEET_VERSION_V2) {
                    require(buffer.remaining() >= PIXEL_PET_ATTACHMENT_BYTES + cellCount * 2) {
                        "Truncated pixel pet attachment data"
                    }
                    val positions = IntArray(PixelPetAccessorySlot.entries.size) {
                        buffer.short.toInt() and 0xFFFF
                    }
                    val layers = IntArray(PixelPetAccessorySlot.entries.size) {
                        buffer.get().toInt() and 0xFF
                    }
                    pixelPetSpriteAttachments(positions, layers, width, height)
                } else {
                    null
                }
                val start = buffer.position()
                require(cellCount <= width * height) { "Invalid pixel pet cell count" }
                require(buffer.remaining() >= cellCount * 2) { "Truncated pixel pet sprite sheet" }
                if (species != null && stage != null && action != null && facing != null) {
                    offsets[FrameKey(species, stage, action, normalizeFrame(action, frame), facing)] =
                        FrameOffset(
                            start = start,
                            cellCount = cellCount,
                            width = width,
                            height = height,
                            pivotCellX = pivotCellX,
                            baselineCellY = baselineCellY,
                            attachments = attachments,
                        )
                }
                buffer.position(start + cellCount * 2)
            }
        }

        private fun frame(key: FrameKey): PixelPetSpriteFrame? {
            val offset = offsets[key] ?: return null
            val cells = ArrayList<PixelPetSpriteCell>(offset.cellCount)
            var cursor = offset.start
            repeat(offset.cellCount) {
                val packed = ((raw[cursor].toInt() and 0xFF) shl 8) or (raw[cursor + 1].toInt() and 0xFF)
                cursor += 2
                val position = packed ushr 4
                val value = BAKED_CELL_VALUES.getOrNull(packed and 0x0F) ?: return@repeat
                if (value == '\u0000') return@repeat
                cells += PixelPetSpriteCell(position % offset.width, position / offset.width, value)
            }
            return PixelPetSpriteFrame(
                width = offset.width,
                height = offset.height,
                cells = cells,
                pivotCellX = offset.pivotCellX,
                baselineCellY = offset.baselineCellY,
                attachments = offset.attachments,
            )
        }

        fun frame(
            species: PixelPetSpecies,
            stage: PixelPetGrowthStage,
            action: PixelPetAction,
            frame: Int,
            facing: PixelPetFacing,
        ): PixelPetSpriteFrame? = frame(
            FrameKey(species, stage, action, normalizeFrame(action, frame), facing),
        )

        fun requireCompleteAuthoredFrames(species: PixelPetSpecies) {
            PixelPetGrowthStage.entries.forEach { stage ->
                PixelPetAction.entries.forEach { action ->
                    PixelPetFacing.entries.forEach { facing ->
                        repeat(frameCount(action)) { frame ->
                            val authored = requireNotNull(frame(species, stage, action, frame, facing)) {
                                "Missing authored frame $species/$stage/$action/$facing/$frame"
                            }
                            require(authored.attachments != null) {
                                "Missing attachment metadata for $species/$stage/$action/$facing/$frame"
                            }
                        }
                    }
                }
            }
        }
    }

    private fun ByteArray.sha256(): String = MessageDigest.getInstance("SHA-256")
        .digest(this)
        .joinToString(separator = "") { byte -> (byte.toInt() and 0xFF).toString(16).padStart(2, '0') }

    private val BAKED_CELL_VALUES = charArrayOf(
        '\u0000', 'o', 'b', 's', 'c', 'h', 'a', 'm', 'r', 'e', 'x',
    )

    fun frameCount(action: PixelPetAction): Int = when (action) {
        PixelPetAction.Idle -> 8
        PixelPetAction.Walking,
        PixelPetAction.Eating,
        PixelPetAction.Happy,
        PixelPetAction.Sleeping,
        PixelPetAction.Exploring,
        -> 10
        PixelPetAction.Hatching,
        PixelPetAction.Frightened,
        PixelPetAction.Petted,
        PixelPetAction.Playing,
        PixelPetAction.Watching,
        PixelPetAction.Cleaning,
        PixelPetAction.Calling,
        -> 6
    }

    fun normalizeFrame(action: PixelPetAction, frame: Int): Int = frame.mod(frameCount(action))

    private fun frameByteCost(frame: PixelPetSpriteFrame): Int =
        96 + frame.cells.size * 16

    /**
     * Authoring-space accessory anchors. These points deliberately never read
     * a rendered frame's bounds: a tail, particle, raised paw, or sleeping Z
     * can therefore never move an equipped item.
     */
    private fun frontAccessoryAnchors(
        species: PixelPetSpecies,
        stage: PixelPetGrowthStage,
    ): PixelPetAccessoryAnchorCells = when (species) {
        PixelPetSpecies.Cat -> when (stage) {
            PixelPetGrowthStage.Egg -> PixelPetAccessoryAnchorCells(
                PixelPetGridPoint(16, 17), PixelPetGridPoint(19, 21), PixelPetGridPoint(20, 23),
            )
            PixelPetGrowthStage.Baby -> PixelPetAccessoryAnchorCells(
                PixelPetGridPoint(16, 16), PixelPetGridPoint(20, 21), PixelPetGridPoint(20, 24),
            )
            PixelPetGrowthStage.Young -> PixelPetAccessoryAnchorCells(
                PixelPetGridPoint(16, 13), PixelPetGridPoint(21, 19), PixelPetGridPoint(21, 23),
            )
            PixelPetGrowthStage.Adult -> PixelPetAccessoryAnchorCells(
                PixelPetGridPoint(16, 10), PixelPetGridPoint(22, 18), PixelPetGridPoint(22, 23),
            )
        }
        PixelPetSpecies.Dog -> when (stage) {
            PixelPetGrowthStage.Egg -> PixelPetAccessoryAnchorCells(
                PixelPetGridPoint(16, 17), PixelPetGridPoint(19, 21), PixelPetGridPoint(20, 23),
            )
            PixelPetGrowthStage.Baby -> PixelPetAccessoryAnchorCells(
                PixelPetGridPoint(16, 16), PixelPetGridPoint(20, 21), PixelPetGridPoint(20, 24),
            )
            PixelPetGrowthStage.Young -> PixelPetAccessoryAnchorCells(
                PixelPetGridPoint(16, 13), PixelPetGridPoint(21, 19), PixelPetGridPoint(21, 23),
            )
            PixelPetGrowthStage.Adult -> PixelPetAccessoryAnchorCells(
                PixelPetGridPoint(16, 10), PixelPetGridPoint(22, 18), PixelPetGridPoint(22, 23),
            )
        }
        PixelPetSpecies.Bird -> when (stage) {
            PixelPetGrowthStage.Egg -> PixelPetAccessoryAnchorCells(
                PixelPetGridPoint(16, 17), PixelPetGridPoint(19, 21), PixelPetGridPoint(20, 23),
            )
            PixelPetGrowthStage.Baby -> PixelPetAccessoryAnchorCells(
                PixelPetGridPoint(16, 17), PixelPetGridPoint(20, 21), PixelPetGridPoint(20, 23),
            )
            PixelPetGrowthStage.Young -> PixelPetAccessoryAnchorCells(
                PixelPetGridPoint(16, 15), PixelPetGridPoint(21, 20), PixelPetGridPoint(21, 23),
            )
            PixelPetGrowthStage.Adult -> PixelPetAccessoryAnchorCells(
                PixelPetGridPoint(16, 12), PixelPetGridPoint(22, 19), PixelPetGridPoint(22, 23),
            )
        }
        PixelPetSpecies.Rabbit -> when (stage) {
            PixelPetGrowthStage.Egg -> PixelPetAccessoryAnchorCells(
                PixelPetGridPoint(16, 17), PixelPetGridPoint(19, 21), PixelPetGridPoint(20, 23),
            )
            PixelPetGrowthStage.Baby -> PixelPetAccessoryAnchorCells(
                PixelPetGridPoint(16, 13), PixelPetGridPoint(20, 21), PixelPetGridPoint(20, 24),
            )
            PixelPetGrowthStage.Young -> PixelPetAccessoryAnchorCells(
                PixelPetGridPoint(16, 10), PixelPetGridPoint(21, 19), PixelPetGridPoint(21, 23),
            )
            PixelPetGrowthStage.Adult -> PixelPetAccessoryAnchorCells(
                PixelPetGridPoint(16, 7), PixelPetGridPoint(22, 18), PixelPetGridPoint(22, 23),
            )
        }
        PixelPetSpecies.Penguin -> when (stage) {
            PixelPetGrowthStage.Egg -> PixelPetAccessoryAnchorCells(
                PixelPetGridPoint(16, 17), PixelPetGridPoint(19, 21), PixelPetGridPoint(20, 23),
            )
            PixelPetGrowthStage.Baby -> PixelPetAccessoryAnchorCells(
                PixelPetGridPoint(16, 16), PixelPetGridPoint(20, 21), PixelPetGridPoint(20, 24),
            )
            PixelPetGrowthStage.Young -> PixelPetAccessoryAnchorCells(
                PixelPetGridPoint(16, 14), PixelPetGridPoint(21, 20), PixelPetGridPoint(21, 23),
            )
            PixelPetGrowthStage.Adult -> PixelPetAccessoryAnchorCells(
                PixelPetGridPoint(16, 11), PixelPetGridPoint(22, 19), PixelPetGridPoint(22, 23),
            )
        }
        PixelPetSpecies.Hamster -> when (stage) {
            PixelPetGrowthStage.Egg -> PixelPetAccessoryAnchorCells(
                PixelPetGridPoint(16, 17), PixelPetGridPoint(19, 21), PixelPetGridPoint(20, 23),
            )
            PixelPetGrowthStage.Baby -> PixelPetAccessoryAnchorCells(
                PixelPetGridPoint(16, 17), PixelPetGridPoint(20, 21), PixelPetGridPoint(21, 23),
            )
            PixelPetGrowthStage.Young -> PixelPetAccessoryAnchorCells(
                PixelPetGridPoint(16, 14), PixelPetGridPoint(21, 19), PixelPetGridPoint(21, 22),
            )
            PixelPetGrowthStage.Adult -> PixelPetAccessoryAnchorCells(
                PixelPetGridPoint(16, 12), PixelPetGridPoint(22, 19), PixelPetGridPoint(22, 22),
            )
        }
    }

    private fun actionAccessoryOffset(action: PixelPetAction): PixelPetAccessoryAnchorCells = when (action) {
        PixelPetAction.Eating -> PixelPetAccessoryAnchorCells(
            head = PixelPetGridPoint(0, 1),
            back = PixelPetGridPoint(0, 1),
            hand = PixelPetGridPoint(0, -2),
            neck = PixelPetGridPoint(0, 1),
            tail = PixelPetGridPoint(0, 0),
            trail = PixelPetGridPoint(0, 0),
        )
        PixelPetAction.Happy,
        PixelPetAction.Playing,
        -> PixelPetAccessoryAnchorCells(
            head = PixelPetGridPoint(0, -1),
            back = PixelPetGridPoint(0, -1),
            hand = PixelPetGridPoint(0, -1),
            neck = PixelPetGridPoint(0, -1),
            tail = PixelPetGridPoint(0, -1),
            trail = PixelPetGridPoint(0, 0),
        )
        PixelPetAction.Sleeping -> PixelPetAccessoryAnchorCells(
            head = PixelPetGridPoint(2, 3),
            back = PixelPetGridPoint(-1, 3),
            hand = PixelPetGridPoint(2, 2),
            neck = PixelPetGridPoint(2, 3),
            tail = PixelPetGridPoint(-1, 3),
            trail = PixelPetGridPoint(0, 0),
        )
        PixelPetAction.Petted,
        PixelPetAction.Watching,
        PixelPetAction.Cleaning,
        PixelPetAction.Calling,
        -> PixelPetAccessoryAnchorCells(
            head = PixelPetGridPoint(-1, -1),
            back = PixelPetGridPoint(0, 0),
            hand = PixelPetGridPoint(0, -1),
            neck = PixelPetGridPoint(-1, -1),
            tail = PixelPetGridPoint(0, 0),
            trail = PixelPetGridPoint(0, 0),
        )
        PixelPetAction.Frightened -> PixelPetAccessoryAnchorCells(
            head = PixelPetGridPoint(0, 1),
            back = PixelPetGridPoint(0, 1),
            hand = PixelPetGridPoint(0, 1),
            neck = PixelPetGridPoint(0, 1),
            tail = PixelPetGridPoint(0, 1),
            trail = PixelPetGridPoint(0, 0),
        )
        else -> PixelPetAccessoryAnchorCells(
            head = PixelPetGridPoint(0, 0),
            back = PixelPetGridPoint(0, 0),
            hand = PixelPetGridPoint(0, 0),
            neck = PixelPetGridPoint(0, 0),
            tail = PixelPetGridPoint(0, 0),
            trail = PixelPetGridPoint(0, 0),
        )
    }

    /**
     * Recovery frames do not have a baked v3 attachment table. Keep their
     * anchors frame-specific anyway so a held item follows a step, chew, or
     * sleep breath while an editable pack is loading.
     */
    private fun frameAccessoryOffset(
        action: PixelPetAction,
        frame: Int,
    ): PixelPetAccessoryAnchorCells {
        val index = normalizeFrame(action, frame)
        val swing = if (index % 2 == 0) -1 else 1
        return when (action) {
            PixelPetAction.Walking,
            PixelPetAction.Exploring,
            -> PixelPetAccessoryAnchorCells(
                head = PixelPetGridPoint(0, if (index % 4 == 1) -1 else 0),
                back = PixelPetGridPoint(0, 0),
                hand = PixelPetGridPoint(swing, if (index % 4 == 1) -1 else 1),
                neck = PixelPetGridPoint(0, 0),
                tail = PixelPetGridPoint(-swing, if (index % 3 == 0) -1 else 0),
                trail = PixelPetGridPoint(swing * 2, 0),
            )
            PixelPetAction.Eating -> PixelPetAccessoryAnchorCells(
                head = PixelPetGridPoint(0, if (index % 3 == 1) 1 else 0),
                back = PixelPetGridPoint(0, 0),
                hand = PixelPetGridPoint(swing, -1 - index % 2),
                neck = PixelPetGridPoint(0, 0),
                tail = PixelPetGridPoint(0, 0),
                trail = PixelPetGridPoint(0, 0),
            )
            PixelPetAction.Sleeping -> PixelPetAccessoryAnchorCells(
                head = PixelPetGridPoint(0, if (index % 4 == 0) 1 else 0),
                back = PixelPetGridPoint(0, 0),
                hand = PixelPetGridPoint(0, if (index % 2 == 0) 1 else 0),
                neck = PixelPetGridPoint(0, 0),
                tail = PixelPetGridPoint(0, 0),
                trail = PixelPetGridPoint(0, 0),
            )
            PixelPetAction.Cleaning -> PixelPetAccessoryAnchorCells(
                head = PixelPetGridPoint(0, if (index % 2 == 0) -1 else 0),
                back = PixelPetGridPoint(0, 0),
                hand = PixelPetGridPoint(swing, -2),
                neck = PixelPetGridPoint(0, 0),
                tail = PixelPetGridPoint(0, 0),
                trail = PixelPetGridPoint(0, 0),
            )
            PixelPetAction.Happy,
            PixelPetAction.Petted,
            PixelPetAction.Playing,
            -> PixelPetAccessoryAnchorCells(
                head = PixelPetGridPoint(0, -index % 2),
                back = PixelPetGridPoint(0, -index % 2),
                hand = PixelPetGridPoint(swing, -2 - index % 2),
                neck = PixelPetGridPoint(0, -index % 2),
                tail = PixelPetGridPoint(-swing, -index % 2),
                trail = PixelPetGridPoint(0, 0),
            )
            else -> PixelPetAccessoryAnchorCells(
                head = PixelPetGridPoint(0, 0),
                back = PixelPetGridPoint(0, 0),
                hand = PixelPetGridPoint(0, 0),
                neck = PixelPetGridPoint(0, 0),
                tail = PixelPetGridPoint(0, 0),
                trail = PixelPetGridPoint(0, 0),
            )
        }
    }

    private fun facingAccessoryAnchors(
        front: PixelPetAccessoryAnchorCells,
        facing: PixelPetFacing,
    ): PixelPetAccessoryAnchorCells {
        fun side(point: PixelPetGridPoint) = PixelPetGridPoint(point.x - 3, point.y)
        fun mirror(point: PixelPetGridPoint) = PixelPetGridPoint(GRID - 1 - point.x, point.y)
        return when (facing) {
            PixelPetFacing.Front -> front
            PixelPetFacing.Back -> PixelPetAccessoryAnchorCells(
                head = PixelPetGridPoint(front.head.x, front.head.y + 1),
                back = PixelPetGridPoint(16, front.back.y),
                hand = PixelPetGridPoint(front.hand.x, front.hand.y),
                neck = PixelPetGridPoint(front.neck.x, front.neck.y + 1),
                tail = PixelPetGridPoint(16, front.tail.y),
                trail = front.trail,
            )
            PixelPetFacing.Left -> PixelPetAccessoryAnchorCells(
                head = side(front.head),
                back = side(front.back),
                hand = side(front.hand),
                neck = side(front.neck),
                tail = side(front.tail),
                trail = side(front.trail),
            )
            PixelPetFacing.Right -> {
                val left = facingAccessoryAnchors(front, PixelPetFacing.Left)
                PixelPetAccessoryAnchorCells(
                    head = mirror(left.head),
                    back = mirror(left.back),
                    hand = mirror(left.hand),
                    neck = mirror(left.neck),
                    tail = mirror(left.tail),
                    trail = mirror(left.trail),
                )
            }
        }
    }

    private fun fallbackAttachment(
        species: PixelPetSpecies,
        stage: PixelPetGrowthStage,
        action: PixelPetAction,
        slot: PixelPetAccessorySlot,
        facing: PixelPetFacing,
    ): PixelPetSpriteAttachment {
        val cells = facingAccessoryAnchors(frontAccessoryAnchors(species, stage), facing) + actionAccessoryOffset(action)
        val point = cells.point(slot)
        return PixelPetSpriteAttachment(
            x = point.x.coerceIn(0, GRID - 1),
            y = point.y.coerceIn(0, GRID - 1),
            layer = pixelPetAccessoryLayer(slot, facing, action),
        )
    }

    private fun generatedAttachments(
        species: PixelPetSpecies,
        stage: PixelPetGrowthStage,
        action: PixelPetAction,
        frame: Int,
        facing: PixelPetFacing,
    ): PixelPetSpriteAttachments {
        val cells = facingAccessoryAnchors(frontAccessoryAnchors(species, stage), facing) +
            actionAccessoryOffset(action) + frameAccessoryOffset(action, frame)
        fun attachment(slot: PixelPetAccessorySlot): PixelPetSpriteAttachment {
            val point = cells.point(slot)
            return PixelPetSpriteAttachment(
                x = point.x.coerceIn(0, GRID - 1),
                y = point.y.coerceIn(0, GRID - 1),
                layer = pixelPetAccessoryLayer(slot, facing, action),
            )
        }
        return PixelPetSpriteAttachments(
            head = attachment(PixelPetAccessorySlot.Head),
            back = attachment(PixelPetAccessorySlot.Back),
            hand = attachment(PixelPetAccessorySlot.Hand),
            neck = attachment(PixelPetAccessorySlot.Neck),
            tail = attachment(PixelPetAccessorySlot.Tail),
            trail = attachment(PixelPetAccessorySlot.Trail),
        )
    }

    fun accessoryAttachment(
        spriteFrame: PixelPetSpriteFrame?,
        species: PixelPetSpecies,
        stage: PixelPetGrowthStage,
        action: PixelPetAction,
        slot: PixelPetAccessorySlot,
        facing: PixelPetFacing = PixelPetFacing.Front,
    ): PixelPetSpriteAttachment = spriteFrame?.attachments?.forSlot(slot)
        ?: fallbackAttachment(species, stage, action, slot, facing)

    fun accessoryAnchor(
        spriteFrame: PixelPetSpriteFrame?,
        species: PixelPetSpecies,
        stage: PixelPetGrowthStage,
        action: PixelPetAction,
        slot: PixelPetAccessorySlot,
        facing: PixelPetFacing = PixelPetFacing.Front,
        unit: Float = 5f,
    ): PixelPetAccessoryAnchor {
        val attachment = accessoryAttachment(spriteFrame, species, stage, action, slot, facing)
        val sourceWidth = spriteFrame?.width ?: GRID
        val pivotCellX = spriteFrame?.pivotCellX?.toFloat() ?: PIXEL_PET_ATLAS_PIVOT_X
        val baselineCellY = spriteFrame?.baselineCellY?.toFloat() ?: PIXEL_PET_ATLAS_BASELINE_Y
        val logicalCell = pixelPetIntegerScale(unit, sourceWidth).toFloat() / unit.coerceAtLeast(1f)
        fun snap(value: Float): Float = (value * unit).roundToInt() / unit
        fun clampStage(value: Float): Float = value.coerceIn(0f, 12f)
        return PixelPetAccessoryAnchor(
            // Attachments on a tiny native cel can otherwise land one physical
            // pixel above the compact stage after integer up-scaling. Clamp
            // only the final logical stage coordinate; the authored integer
            // anchor and nearest-neighbour scale remain unchanged.
            x = clampStage(snap(PIXEL_PET_VIEW_PIVOT_X + (attachment.x - pivotCellX) * logicalCell)),
            y = clampStage(snap(PIXEL_PET_VIEW_BASELINE_Y + (attachment.y - baselineCellY) * logicalCell)),
            scale = 1f,
        )
    }

    fun accessoryAnchor(
        species: PixelPetSpecies,
        stage: PixelPetGrowthStage,
        action: PixelPetAction,
        frame: Int,
        slot: PixelPetAccessorySlot,
        facing: PixelPetFacing = PixelPetFacing.Front,
        unit: Float = 5f,
    ): PixelPetAccessoryAnchor {
        return accessoryAnchor(
            spriteFrame = loadedFrame(species, stage, action, frame, facing),
            species = species,
            stage = stage,
            action = action,
            slot = slot,
            facing = facing,
            unit = unit,
        )
    }

    fun prewarm(
        species: PixelPetSpecies,
        stage: PixelPetGrowthStage,
        action: PixelPetAction,
        facing: PixelPetFacing,
        colors: PixelPetModelColors,
        startFrame: Int = 0,
        windowSize: Int = 3,
    ) {
        repeat(windowSize.coerceIn(1, frameCount(action))) { offset ->
            val frameIndex = normalizeFrame(action, startFrame + offset)
            loadedFrame(species, stage, action, frameIndex, facing)?.let { frame ->
                PixelPetSpriteBitmapCache.image(frame, colors)
            }
        }
    }

    fun resolveFacingLeft(previousX: Float, targetX: Float, current: Boolean): Boolean = when {
        targetX < previousX - 0.5f -> true
        targetX > previousX + 0.5f -> false
        else -> current
    }

    fun resolveFacing(deltaX: Float, deltaY: Float, current: PixelPetFacing): PixelPetFacing = when {
        max(abs(deltaX), abs(deltaY)) < 0.5f -> current
        abs(deltaX) >= abs(deltaY) -> if (deltaX < 0f) PixelPetFacing.Left else PixelPetFacing.Right
        deltaY < 0f -> PixelPetFacing.Back
        else -> PixelPetFacing.Front
    }

    private val EGG = """
        .......oooooo.......
        .....ooccccccoo.....
        ....occcccccccco....
        ...occcchhccccccco...
        ..occccccccccccccco..
        ..occccccccccccccco..
        .occcccccccccccccccco.
        .occcccccccccccccccco.
        .occcccccccccccccccco.
        .occcccccssccccccccco.
        .occccccsssccccccccco.
        ..occcccssccccccccco..
        ..occccccccccccccco..
        ...occcccccccccccco...
        ....oocccccccccoo....
        ......oooooooo.......
    """

    private val CAT_BABY = """
        ...oo......oo...
        ..obbo....obbo..
        .obbbbo..obbbbo.
        obbbbbboobbbbbbo
        obbbbbbbbbbbbbbo
        obbeebbbbbeeebbo
        obbhbbbbbbbhbbbo
        obbbbbbabbbbbbbo
        .obbbbmmmmbbbbo.
        .obccccccccccbo.
        ..obbbbbbbbbbo..
        ..obbbbbbbbbbo..
        ...obbo..obbo...
        ...obbo..obbo...
        ....oo....oo....
    """

    private val CAT_YOUNG = """
        ...ooo........ooo...
        ..obbbo......obbbo..
        .obbbbbo....obbbbbo.
        obbbbbbbo..obbbbbbbo
        obbbbbbbboobbbbbbbbo
        obbbbbbbbbbbbbbbbbbo
        obbbeebbbbbbbbeebbbo
        obbbhebbbbbbbbhebbbo
        obbbbbbbbaabbbbbbbbo
        .obbbbsmmmmssbbbbbo.
        .obccccccccccccccbo.
        .obbbbbbbbbbbbbbbbo.
        ..obbbbbbbbbbbbbbo..
        ..obbbsbbbbbsbbbbo..
        ...obbbboobbbbbo....
        ...obbbo..obbbo.....
        ....ooo....ooo......
    """

    private val CAT_ADULT = """
        ...oooo..........oooo...
        ..obbbbo........obbbbo..
        .obbbbbbo......obbbbbbo.
        obbbbbbbbo....obbbbbbbbo
        obbbbbbbbbo..obbbbbbbbbo
        obbbbbbbbbboobbbbbbbbbbo
        obbbbbbbbbbbbbbbbbbbbbbo
        obbbbeebbbbbbbbbbeebbbbo
        obbbbhebbbbbbbbbbhebbbbo
        obbbbbbbbbabbbbbbbbbbbbo
        .obbbbsbbmmmmbbsbbbbbo..
        .obccccccccccccccccccbo.
        .obbbbbbbbbbbbbbbbbbbbo.
        .obbbbbssssssssbbbbbbbo.
        ..obbbbbbbbbbbbbbbbbbo..
        ..obbbbbbbbbbbbbbbbbbo..
        ...obbbbboooobbbbbbo....
        ...obbbbo....obbbbo.....
        ...obbbbo....obbbbo.....
        ....oooo......oooo......
    """

    private val DOG_BABY = """
        .oooo........oooo.
        obbbbo......obbbbo
        obbbbboooooobbbbbo
        .obbbbbbbbbbbbbbo.
        .obbeebbbbeebbbbo.
        .obbhebbbbhebbbbo.
        .obbbbbccbbbbbbbo.
        .obbbbcmmbcbbbbbo.
        .obbbbbbbbbbbbbbo.
        ..obbbbbbbbbbbbo..
        ..obbbssssbbbbo...
        ...obbbo.obbbo....
        ...obbbo.obbbo....
        ....ooo...ooo.....
    """

    private val DOG_YOUNG = """
        .ooooo..........ooooo.
        obbbbbo........obbbbbo
        obbbbbbo......obbbbbbo
        .obbbbbboooooobbbbbbo.
        .obbbbbbbbbbbbbbbbbbo.
        .obbbeebbbbbbbbeebbbo.
        .obbbhebbbbbbbbhebbbo.
        .obbbbbbbccccbbbbbbbo.
        .obbbbbbcmmbcbbbbbbbo.
        .obbbbbbbbbbbbbbbbbbo.
        ..obbbbbssssbbbbbbbo..
        ..obbbbbbbbbbbbbbbbo..
        ...obbbbboobbbbbbo....
        ...obbbo....obbbo.....
        ...obbbo....obbbo.....
        ....ooo......ooo......
    """

    private val DOG_ADULT = """
        oooooo............oooooo
        obbbbbo..........obbbbbo
        obbbbbbo.........obbbbbo
        .obbbbbbo........obbbbbo
        .obbbbbbbboooooobbbbbbbo
        .obbbbbbbbbbbbbbbbbbbbbo
        .obbbbeebbbbbbbbbbeebbbbo
        .obbbbhebbbbbbbbbbhebbbbo
        .obbbbbbbbccccbbbbbbbbbo
        .obbbbbbbcmmbcbbbbbbbbbo
        .obbbbbbbbbbbbbbbbbbbbbo
        ..obbbbbbssssbbbbbbbbo.
        ..obbbbbbbbbbbbbbbbbbo.
        ..obbbbbbbbbbbbbbbbbbo.
        ...obbbbbboobbbbbbbo...
        ...obbbbbo..obbbbbo....
        ...obbbbbo..obbbbbo....
        ....ooooo....ooooo.....
    """

    private val BIRD_BABY = """
        .......oo.......
        ......obbo......
        ....oobbbboo....
        ...obbbbbbbbbo...
        ..obbeebbbbbboaa.
        ..obbhebbbbbbbaaa
        .obbbbbbbbbbbboaa
        .obbbbccccbbbbo..
        .obbbccccccbbbo..
        ..obbbssssbbbo...
        ...obbbbbbbbo....
        ....obb..obbo....
        ....aaa..aaa.....
    """

    private val BIRD_YOUNG = """
        .........oo.........
        ........obbo........
        ......oobbbboo......
        .....obbbbbbbbbo.....
        ....obbeebbbbbboaaa..
        ....obbhebbbbbbaaaaa.
        ..oobbbbbbbbbbbboaaa.
        .obbsbbbbbbbbbbbbo...
        obbssbbbccccbbbbbbbo.
        .obbsbbccccccbbbbbo..
        ..obbbbssssbbbbbbbo..
        ...obbbbbbbbbbbbbo...
        ....obbbboobbbbo....
        .....aaa..aaa.......
    """

    private val BIRD_ADULT = """
        ..........ooo..........
        .........obbbo.........
        .......oobbbbboo.......
        ......obbbbbbbbbbo......
        .....obbeebbbbbbboaaaa..
        .....obbhebbbbbbbaaaaaa.
        ...oobbbbbbbbbbbbbboaaaa
        ..obbssbbbbbbbbbbbbbo...
        .obbssssbbbbbbbbbbbbbo..
        obbssssbbbccccbbbbbbbbo.
        .obbssbbbccccccbbbbbbo..
        ..obbbbbccssssccbbbbbo..
        ...obbbbbbbbbbbbbbbbo...
        ....obbbbbbbbbbbbbo.....
        .....obbbboobbbbo.......
        ......aaaa.aaaa.........
    """

    private val RABBIT_BABY = """
        ...ooo....ooo...
        ..ocbo....obco..
        ..ocbo....obco..
        ..obbo....obbo..
        .obbbbo..obbbbo.
        obbbbbbbbbbbbbbo
        obbeebbbbbeeebbo
        obbhbbbbbbbhbbbo
        obbbbbbabbbbbbbo
        .obbbbmmmmbbbbo.
        .occcccccccccco.
        ..obbbbbbbbbbo..
        ...obbo..obbo...
        ...obbo..obbo...
        ....oo....oo....
    """

    private val RABBIT_YOUNG = """
        ...ooo......ooo...
        ..ocbo......obco..
        ..ocbo......obco..
        ..ocbo......obco..
        ..obbo......obbo..
        .obbbbbo..obbbbbo.
        obbbbbbbbbbbbbbbbo
        obbbeebbbbbbbeebbbo
        obbbhebbbbbbbbhebbbo
        obbbbbbbbaabbbbbbbbo
        .obbbbbmmmmbbbbbbo.
        .occccccccccccccco.
        ..obbbbbbbbbbbbbo..
        ...obbbboobbbbo....
        ...obbbo..obbbo.....
        ....ooo....ooo......
    """

    private val RABBIT_ADULT = """
        ...oooo........oooo...
        ..ocbbo........obbco..
        ..ocbbo........obbco..
        ..ocbbo........obbco..
        ..ocbbo........obbco..
        ..obbbo........obbbo..
        .obbbbbo......obbbbbo.
        obbbbbbbbooooobbbbbbbo
        obbbbeebbbbbbbbbbeebbbo
        obbbbhebbbbbbbbbbhebbbbo
        obbbbbbbbbaabbbbbbbbbbo
        .obbbbbbbmmmmbbbbbbbo.
        .occcccccccccccccccco.
        .obbbbbbbbbbbbbbbbbo.
        ..obbbbbbbbbbbbbbbo..
        ...obbbbboobbbbbo....
        ...obbbbo..obbbo.....
        ....oooo....oooo.....
    """

    private val HAMSTER_BABY = """
        .....oo....oo.....
        ....obbo..obbo....
        ...obbbbbbbbbbo...
        ..obbbccccbbbbbo..
        ..obbeebbbbeebbo..
        ..obbhbbbbbbhbbo..
        ..obbbbbmmmbbbbo..
        ..obbbccccccbbbo..
        ...obbbbbbbbbbo...
        ....obbb...bbbo...
        ....obbo...obbo...
        .....oo.....oo....
    """

    private val HAMSTER_YOUNG = """
        .....ooo....ooo.....
        ....obbbo..obbbo....
        ...obbbbbbbbbbbbo...
        ..obbbbccccbbbbbo...
        ..obbbeebbbbeebbbo..
        ..obbbhebbbbbbhbbbo.
        ..obbbbbmmmmbbbbbo..
        ..obbbbccccccbbbbo..
        ..obbbbbbbbbbbbbbo..
        ...obbbbbssssbbbbo..
        ...obbbbo...obbbbo..
        ....obbbo...obbbo...
        .....ooo.....ooo....
    """

    private val HAMSTER_ADULT = """
        ......oooo....oooo......
        .....obbbo..obbbo.....
        ....obbbbbbbbbbbbo....
        ...obbbbbccccbbbbbo...
        ...obbbeebbbbbeebbbo..
        ...obbbhebbbbbbhebbbo.
        ...obbbbbmmmmbbbbbbo..
        ...obbbbccccccbbbbbo..
        ...obbbbbbbbbbbbbbbbo.
        ....obbbbbssssbbbbbo..
        ....obbbbbbbbbbbbbbo..
        .....obbbbbo.obbbbbo..
        .....obbbo.....obbbo..
        ......oooo.....oooo...
    """

    private val PENGUIN_BABY = """
        .....oooooo.....
        ...oobbbbbboo...
        ..obbbbbbbbbbo..
        .obbccbbbbccbbo.
        .obceebbbbeecbo.
        .obchebbbbhecbo.
        .obccccacccccbo.
        .obccccccccccbo.
        .obbbccccccbbbo.
        ..obbbccccbbbo..
        ..obbbbbbbbbbo..
        ...obbo..obbo...
        ...aaaa..aaaa...
    """

    private val PENGUIN_YOUNG = """
        ......oooooooo......
        ....oobbbbbbbboo....
        ...obbbbbbbbbbbbo...
        ..obbbbccccbbbbbbbo..
        .obbbbccbbbbccbbbbo.
        .obbbceebbbbeecbbbo.
        .obbbchebbbbhecbbbo.
        .obbbccccacccccbbbo.
        .obbbccccccccccbbbo.
        .obbbbccccccccbbbbo.
        ..obbbbccccccbbbbo..
        ..obbbbbbbbbbbbbbo..
        ...obbbbbbbbbbbbo...
        ....obbbo..obbbo....
        ....aaaa..aaaa......
    """

    private val PENGUIN_ADULT = """
        .......oooooooooo.......
        .....oobbbbbbbbbboo.....
        ....obbbbbbbbbbbbbbo....
        ...obbbbbccccccbbbbbbo...
        ..obbbbccbbbbbbccbbbbo..
        ..obbbceebbbbbbeecbbbo..
        .obbbbchebbbbbbhecbbbbo.
        .obbbbcccccaaccccccbbbbo.
        .obbbbccccccccccccbbbbo.
        .obbbbbccccccccccbbbbbo.
        .obbbbbccccccccccbbbbbo.
        ..obbbbbccccccccbbbbbo..
        ..obbbbbbbbbbbbbbbbbbo..
        ...obbbbbbbbbbbbbbbbo...
        ....obbbbbbbbbbbbbo.....
        .....obbbo..obbbo.......
        .....aaaa..aaaa.........
    """

}

internal fun pixelPetIntegerScale(
    unit: Float,
    sourceWidth: Int = PixelPetSpriteAtlas.GRID,
): Int =
    (unit * 10.8f).let { targetWidth ->
        require(sourceWidth > 0)
        // A native 48x48 cel must not round up to two physical pixels per
        // source pixel unless the available stage is actually wide enough.
        // Smaller atlases retain the established readable up-scaling policy.
        if (sourceWidth >= 48) {
            floor(targetWidth / sourceWidth).toInt()
        } else if (targetWidth >= 48f) {
            kotlin.math.ceil(targetWidth / sourceWidth).toInt()
        } else {
            floor(targetWidth / sourceWidth).toInt()
        }
    }.coerceAtLeast(1)

internal fun pixelPetSpriteLayout(frame: PixelPetSpriteFrame, unit: Float): PixelPetSpriteLayout {
    val minX = frame.cells.minOfOrNull(PixelPetSpriteCell::x) ?: 0
    val maxX = frame.cells.maxOfOrNull(PixelPetSpriteCell::x) ?: minX
    val minY = frame.cells.minOfOrNull(PixelPetSpriteCell::y) ?: 0
    val maxY = frame.cells.maxOfOrNull(PixelPetSpriteCell::y) ?: minY
    val contentWidth = (maxX - minX + 1).coerceAtLeast(1)
    val contentHeight = (maxY - minY + 1).coerceAtLeast(1)
    // Crop transparent cells only when drawing. The physical pixel scale is
    // selected from the complete native artboard so changing a pose's visible
    // bounds can never resize the pet.
    val cellUnit = pixelPetIntegerScale(unit, frame.width)
    val logicalCell = cellUnit / unit.coerceAtLeast(1f)
    // Keep every frame on its authored pivot and baseline. The visible
    // bounds are still cropped for efficiency, but their origin is derived
    // from atlas coordinates rather than from the current frame's center.
    val originX = PIXEL_PET_VIEW_PIVOT_X - (frame.pivotCellX - minX) * logicalCell
    val originY = PIXEL_PET_VIEW_BASELINE_Y - (frame.baselineCellY - minY) * logicalCell
    return PixelPetSpriteLayout(
        minX = minX,
        minY = minY,
        contentWidth = contentWidth,
        contentHeight = contentHeight,
        cellUnit = cellUnit,
        originX = originX,
        originY = originY,
        logicalCell = logicalCell,
        pivotCellX = frame.pivotCellX.toFloat(),
        baselineCellY = frame.baselineCellY.toFloat(),
    )
}

private data class PixelPetBitmapKey(
    val width: Int,
    val height: Int,
    val cells: List<PixelPetSpriteCell>,
    val outline: Int,
    val base: Int,
    val shade: Int,
    val cream: Int,
    val highlight: Int,
    val accent: Int,
    val reflection: Int,
    val eye: Int,
)

private const val PIXEL_PET_OUTLINE = 'o'
private val PIXEL_PET_NEIGHBOUR_OFFSETS = listOf(
    -1 to -1,
    0 to -1,
    1 to -1,
    -1 to 0,
    1 to 0,
    -1 to 1,
    0 to 1,
    1 to 1,
)

private fun isPixelPetBodyValue(value: Char): Boolean = value in setOf(
    'b',
    's',
    'c',
    'h',
    'a',
    'm',
    'r',
)

/**
 * Removes only the outer contour of an authored frame. Interior outline
 * pixels remain intact, while replaced edge pixels borrow the nearest body
 * colour so the silhouette keeps its original footprint without a dark rim.
 */
internal fun pixelPetCellsWithoutOuterOutline(frame: PixelPetSpriteFrame): List<PixelPetSpriteCell> {
    val values = Array(frame.height) { CharArray(frame.width) }
    frame.cells.forEach { cell ->
        if (cell.x in 0 until frame.width && cell.y in 0 until frame.height) {
            values[cell.y][cell.x] = cell.value
        }
    }
    val outside = Array(frame.height) { BooleanArray(frame.width) }
    val queue = ArrayDeque<Pair<Int, Int>>()
    fun enqueueIfOutside(x: Int, y: Int) {
        if (x !in 0 until frame.width || y !in 0 until frame.height) return
        if (values[y][x] == '\u0000' || values[y][x] == ' ') {
            if (!outside[y][x]) {
                outside[y][x] = true
                queue += x to y
            }
        }
    }
    for (x in 0 until frame.width) {
        enqueueIfOutside(x, 0)
        enqueueIfOutside(x, frame.height - 1)
    }
    for (y in 0 until frame.height) {
        enqueueIfOutside(0, y)
        enqueueIfOutside(frame.width - 1, y)
    }
    while (queue.isNotEmpty()) {
        val (x, y) = queue.removeFirst()
        PIXEL_PET_NEIGHBOUR_OFFSETS.forEach { (dx, dy) ->
            enqueueIfOutside(x + dx, y + dy)
        }
    }

    fun isOuterOutline(cell: PixelPetSpriteCell): Boolean =
        cell.value == PIXEL_PET_OUTLINE && PIXEL_PET_NEIGHBOUR_OFFSETS.any { (dx, dy) ->
            val x = cell.x + dx
            val y = cell.y + dy
            x !in 0 until frame.width || y !in 0 until frame.height || outside[y][x]
        }

    fun nearestBodyValue(cell: PixelPetSpriteCell): Char? {
        val maxRadius = max(frame.width, frame.height)
        for (radius in 1..maxRadius) {
            for (dy in -radius..radius) {
                for (dx in -radius..radius) {
                    if (max(abs(dx), abs(dy)) != radius) continue
                    val x = cell.x + dx
                    val y = cell.y + dy
                    if (x !in 0 until frame.width || y !in 0 until frame.height) continue
                    val value = values[y][x]
                    if (isPixelPetBodyValue(value)) {
                        return value
                    }
                }
            }
        }
        return null
    }

    return frame.cells.mapNotNull { cell ->
        if (!isOuterOutline(cell)) {
            cell
        } else {
            nearestBodyValue(cell)?.let { replacement -> cell.copy(value = replacement) }
        }
    }
}

private object PixelPetSpriteBitmapCache {
    private const val MAX_BITMAP_BYTES = 2 * 1024 * 1024
    private val cache = object : LruCache<PixelPetBitmapKey, ImageBitmap>(MAX_BITMAP_BYTES) {
        override fun sizeOf(key: PixelPetBitmapKey, value: ImageBitmap): Int =
            (value.width * value.height * 4).coerceAtLeast(1)
    }

    fun image(frame: PixelPetSpriteFrame, colors: PixelPetModelColors): ImageBitmap {
        val key = PixelPetBitmapKey(
            width = frame.width,
            height = frame.height,
            cells = frame.cells,
            outline = colors.outline.toArgb(),
            base = colors.base.toArgb(),
            shade = colors.shade.toArgb(),
            cream = colors.cream.toArgb(),
            highlight = colors.highlight.toArgb(),
            accent = colors.accent.toArgb(),
            reflection = colors.reflection.toArgb(),
            eye = colors.eye.toArgb(),
        )
        synchronized(cache) {
            cache.get(key)?.let { return it }
            val pixels = IntArray(frame.width * frame.height)
            pixelPetCellsWithoutOuterOutline(frame).forEach { cell ->
                pixels[cell.y * frame.width + cell.x] = when (cell.value) {
                    PIXEL_PET_OUTLINE -> key.outline
                    'b' -> key.base
                    's' -> key.shade
                    'c' -> key.cream
                    'h' -> key.highlight
                    'a', 'm' -> key.accent
                    'r' -> key.reflection
                    'e', 'x' -> key.eye
                    else -> Color.Transparent.toArgb()
                }
            }
            val bitmap = Bitmap.createBitmap(frame.width, frame.height, Bitmap.Config.ARGB_8888).apply {
                setPixels(pixels, 0, frame.width, 0, 0, frame.width, frame.height)
            }.asImageBitmap()
            cache.put(key, bitmap)
            return bitmap
        }
    }

    fun trimToFraction(keepFraction: Float) = synchronized(cache) {
        cache.trimToSize((MAX_BITMAP_BYTES * keepFraction.coerceIn(0f, 1f)).roundToInt())
    }

    fun clear() = synchronized(cache) { cache.evictAll() }
}

internal fun DrawScope.drawPixelPetSpriteFrame(
    frame: PixelPetSpriteFrame,
    colors: PixelPetModelColors,
    unit: Float,
) {
    val layout = pixelPetSpriteLayout(frame, unit)
    drawImage(
        image = PixelPetSpriteBitmapCache.image(frame, colors),
        srcOffset = IntOffset(layout.minX, layout.minY),
        srcSize = IntSize(layout.contentWidth, layout.contentHeight),
        dstOffset = IntOffset(
            (layout.originX * unit).roundToInt(),
            (layout.originY * unit).roundToInt(),
        ),
        dstSize = IntSize(
            layout.contentWidth * layout.cellUnit,
            layout.contentHeight * layout.cellUnit,
        ),
        filterQuality = FilterQuality.None,
    )
}
