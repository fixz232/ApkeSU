package me.weishu.kernelsu.ui.component.pixel

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap
import org.json.JSONObject

internal data class PixelPetSpriteEditorKey(
    val species: PixelPetSpecies,
    val stage: PixelPetGrowthStage,
    val action: PixelPetAction,
    val frame: Int,
    val facing: PixelPetFacing,
) {
    val storageKey: String
        get() = listOf(species.name, stage.name, action.name, frame, facing.name).joinToString(":")
}

internal enum class PixelPetSpriteInk(
    val value: Char,
    val fallbackColor: Color,
) {
    Outline('o', Color(0xFF34313A)),
    Base('b', Color(0xFFC7C4CC)),
    Shade('s', Color(0xFF817E8A)),
    Cream('c', Color(0xFFFFF8F6)),
    Highlight('h', Color(0xFFFFFFFF)),
    Accent('a', Color(0xFFFF9EA8)),
    Mark('m', Color(0xFFFFC0C8)),
    Reflection('r', Color(0xFFD9F5FF)),
    Eye('e', Color(0xFF26232B)),
    Expression('x', Color(0xFF4A3E59)),
    Eraser('\u0000', Color.Transparent),
    ;

    companion object {
        fun fromValue(value: Char): PixelPetSpriteInk = entries.firstOrNull { it.value == value } ?: Base
    }
}

/**
 * Local, non-destructive Sprite overrides used by the visual authoring screen.
 * They never replace shipped packs and can be reset independently at any time.
 */
internal object PixelPetSpriteDraftStore {
    private const val PREFS = "pixel_pet_sprite_drafts"
    private const val MAX_EDITABLE_DIMENSION = 64

    private data class DraftPosition(val x: Int, val y: Int)

    private val cachedOverrides = ConcurrentHashMap<String, Map<DraftPosition, Char?>>()

    /** Canvas-only lookup. Disk reads are performed by preload or editor events. */
    fun applyLoaded(
        key: PixelPetSpriteEditorKey,
        frame: PixelPetSpriteFrame,
    ): PixelPetSpriteFrame {
        val overrides = cachedOverrides[key.storageKey] ?: emptyMap()
        if (overrides.isEmpty()) return frame
        val cells = frame.cells.associateBy { DraftPosition(it.x, it.y) }.toMutableMap()
        overrides.forEach { (position, value) ->
            if (position.x !in 0 until frame.width || position.y !in 0 until frame.height) {
                return@forEach
            }
            if (value == null) {
                cells.remove(position)
            } else {
                cells[position] = PixelPetSpriteCell(position.x, position.y, value)
            }
        }
        return frame.copy(
            cells = cells.values.sortedWith(compareBy<PixelPetSpriteCell> { it.y }.thenBy { it.x }),
        )
    }

    fun preload(context: Context, species: PixelPetSpecies) {
        val prefix = "${species.name}:"
        context.draftPreferences().all.forEach { (storageKey, _) ->
            if (!storageKey.startsWith(prefix)) return@forEach
            val key = storageKey.toPixelPetSpriteEditorKey() ?: return@forEach
            cachedOverrides[storageKey] = read(context, key)
        }
    }

    fun setCell(
        context: Context,
        key: PixelPetSpriteEditorKey,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        ink: PixelPetSpriteInk,
    ) {
        require(width in 1..MAX_EDITABLE_DIMENSION && height in 1..MAX_EDITABLE_DIMENSION)
        require(x in 0 until width && y in 0 until height)
        val json = readJson(context, key)
        val position = "$x:$y"
        if (ink == PixelPetSpriteInk.Eraser) {
            json.put(position, JSONObject.NULL)
        } else {
            json.put(position, ink.value.toString())
        }
        save(context, key, json)
    }

    fun reset(context: Context, key: PixelPetSpriteEditorKey) {
        context.draftPreferences().edit().remove(key.storageKey).apply()
        cachedOverrides.remove(key.storageKey)
        PixelPetSpriteAtlas.invalidateEditedFrames()
    }

    fun editedCellCount(context: Context, key: PixelPetSpriteEditorKey): Int = read(context, key).size

    fun exportPng(
        frame: PixelPetSpriteFrame,
        colors: PixelPetModelColors,
        output: OutputStream,
        pixelSize: Int = 16,
    ) {
        val scale = pixelSize.coerceIn(4, 32)
        val bitmap = Bitmap.createBitmap(
            frame.width * scale,
            frame.height * scale,
            Bitmap.Config.ARGB_8888,
        )
        frame.cells.forEach { cell ->
            val color = colors.colorFor(cell.value).toArgb()
            repeat(scale) { dy ->
                repeat(scale) { dx ->
                    bitmap.setPixel(cell.x * scale + dx, cell.y * scale + dy, color)
                }
            }
        }
        check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) { "Unable to export pixel pet sprite" }
        bitmap.recycle()
    }

    private fun read(context: Context, key: PixelPetSpriteEditorKey): Map<DraftPosition, Char?> {
        val json = readJson(context, key)
        return buildMap {
            json.keys().forEach { position ->
                val parts = position.split(':')
                val x = parts.getOrNull(0)?.toIntOrNull() ?: return@forEach
                val y = parts.getOrNull(1)?.toIntOrNull() ?: return@forEach
                if (x !in 0 until MAX_EDITABLE_DIMENSION || y !in 0 until MAX_EDITABLE_DIMENSION) {
                    return@forEach
                }
                val value = json.optString(position).firstOrNull()
                put(DraftPosition(x, y), if (json.isNull(position)) null else value)
            }
        }
    }

    private fun readJson(context: Context, key: PixelPetSpriteEditorKey): JSONObject = runCatching {
        JSONObject(context.draftPreferences().getString(key.storageKey, "{}") ?: "{}")
    }.getOrDefault(JSONObject())

    private fun save(context: Context, key: PixelPetSpriteEditorKey, json: JSONObject) {
        context.draftPreferences().edit().putString(key.storageKey, json.toString()).apply()
        cachedOverrides[key.storageKey] = read(context, key)
        PixelPetSpriteAtlas.invalidateEditedFrames()
    }

    private fun Context.draftPreferences() = getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun String.toPixelPetSpriteEditorKey(): PixelPetSpriteEditorKey? {
        val parts = split(':')
        return PixelPetSpriteEditorKey(
            species = PixelPetSpecies.entries.firstOrNull { it.name == parts.getOrNull(0) } ?: return null,
            stage = PixelPetGrowthStage.entries.firstOrNull { it.name == parts.getOrNull(1) } ?: return null,
            action = PixelPetAction.entries.firstOrNull { it.name == parts.getOrNull(2) } ?: return null,
            frame = parts.getOrNull(3)?.toIntOrNull() ?: return null,
            facing = PixelPetFacing.entries.firstOrNull { it.name == parts.getOrNull(4) } ?: return null,
        )
    }
}

internal fun PixelPetModelColors.colorFor(value: Char): Color = when (value) {
    'o' -> outline
    'b' -> base
    's' -> shade
    'c' -> cream
    'h' -> highlight
    'a', 'm' -> accent
    'r' -> reflection
    'e', 'x' -> eye
    else -> Color.Transparent
}
