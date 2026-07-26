package me.weishu.kernelsu.ui.component.custom

import androidx.compose.runtime.Immutable
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

const val CUSTOM_CARD_STYLE_LIBRARY_KEY = "custom_card_style_library"
const val CUSTOM_CARD_STYLE_ACTIVE_ID_KEY = "custom_card_style_active_id"
const val CUSTOM_SWITCH_STYLE_LIBRARY_KEY = "custom_switch_style_library"
const val CUSTOM_SWITCH_STYLE_ACTIVE_ID_KEY = "custom_switch_style_active_id"

enum class ComponentStyleKind(val value: String) {
    Card("card_style"),
    Switch("switch_style");

    companion object {
        fun fromValue(value: String?): ComponentStyleKind? = entries.firstOrNull { it.value == value }
    }
}

enum class CustomCardTarget(val value: String) {
    Default("default"),
    Lkm("lkm"),
    Superuser("superuser"),
    Module("module"),
    StatusMonitor("status_monitor"),
    SystemInfo("system_info"),
    RebootMenu("reboot_menu");

    companion object {
        fun fromValue(value: String?): CustomCardTarget? = entries.firstOrNull { it.value == value }
    }
}

enum class CardPixelLayer(val value: String) {
    Top("top"),
    Border("border"),
    Interior("interior"),
}

enum class NavigationPixelLayer(val value: String) {
    Top("top"),
    Border("border"),
}

enum class PixelMotionMode(val value: String) {
    Static("static"),
    Pulse("pulse"),
    Drift("drift"),
    Scan("scan");

    companion object {
        fun fromValue(value: String?): PixelMotionMode = entries.firstOrNull { it.value == value } ?: Static
    }
}

enum class PixelMotionRepeat(val value: String) {
    Restart("restart"),
    Reverse("reverse");

    companion object {
        fun fromValue(value: String?): PixelMotionRepeat = entries.firstOrNull { it.value == value } ?: Reverse
    }
}

enum class CustomSwitchSource(val value: String) {
    Pixel("pixel"),
    Image("image");

    companion object {
        fun fromValue(value: String?): CustomSwitchSource = entries.firstOrNull { it.value == value } ?: Pixel
    }
}

enum class SwitchImageScale(val value: String) {
    Crop("crop"),
    Fit("fit");

    companion object {
        fun fromValue(value: String?): SwitchImageScale = entries.firstOrNull { it.value == value } ?: Crop
    }
}

@Immutable
data class PixelGrid(
    val width: Int,
    val height: Int,
    val pixels: List<Long>,
) {
    init {
        require(width in 1..MAX_PIXEL_GRID_SIDE) { "Pixel grid width is invalid" }
        require(height in 1..MAX_PIXEL_GRID_SIDE) { "Pixel grid height is invalid" }
        require(width * height <= MAX_PIXEL_GRID_CELLS) { "Pixel grid is too large" }
        require(pixels.size == width * height) { "Pixel grid data is incomplete" }
        require(pixels.all(::isValidArgb)) { "Pixel grid contains an invalid color" }
    }

    fun colorAt(x: Int, y: Int): Long = pixels[y * width + x]

    fun withPixel(x: Int, y: Int, argb: Long): PixelGrid {
        if (x !in 0 until width || y !in 0 until height || !isValidArgb(argb)) return this
        val index = y * width + x
        if (pixels[index] == argb) return this
        return copy(pixels = pixels.toMutableList().apply { this[index] = argb })
    }

    fun cleared(): PixelGrid = blank(width, height)

    internal fun toJson(): JSONObject = JSONObject()
        .put("width", width)
        .put("height", height)
        .put("pixels", JSONArray().apply { pixels.forEach(::put) })

    companion object {
        fun blank(width: Int, height: Int): PixelGrid = PixelGrid(
            width = width,
            height = height,
            pixels = List(width * height) { TRANSPARENT_PIXEL },
        )

        internal fun fromJson(json: JSONObject, expectedWidth: Int, expectedHeight: Int): PixelGrid {
            require(json.optInt("width", -1) == expectedWidth) { "Pixel grid width does not match this layer" }
            require(json.optInt("height", -1) == expectedHeight) { "Pixel grid height does not match this layer" }
            val source = json.optJSONArray("pixels") ?: error("Pixel grid data is missing")
            require(source.length() == expectedWidth * expectedHeight) { "Pixel grid data is incomplete" }
            return PixelGrid(
                width = expectedWidth,
                height = expectedHeight,
                pixels = List(source.length()) { index ->
                    source.optLong(index, INVALID_PIXEL).also {
                        require(isValidArgb(it)) { "Pixel grid contains an invalid color" }
                    }
                },
            )
        }
    }
}

@Immutable
data class PixelMotionRule(
    val enabled: Boolean = false,
    val mode: PixelMotionMode = PixelMotionMode.Static,
    val durationMillis: Int = DEFAULT_PIXEL_MOTION_DURATION_MS,
    val amplitudeCells: Int = DEFAULT_PIXEL_MOTION_AMPLITUDE,
    val repeat: PixelMotionRepeat = PixelMotionRepeat.Reverse,
) {
    fun normalized(): PixelMotionRule = copy(
        enabled = enabled && mode != PixelMotionMode.Static,
        durationMillis = durationMillis.coerceIn(MIN_PIXEL_MOTION_DURATION_MS, MAX_PIXEL_MOTION_DURATION_MS),
        amplitudeCells = amplitudeCells.coerceIn(0, MAX_PIXEL_MOTION_AMPLITUDE),
    )

    internal fun toJson(): JSONObject = normalized().let { value ->
        JSONObject()
            .put("enabled", value.enabled)
            .put("mode", value.mode.value)
            .put("duration_ms", value.durationMillis)
            .put("amplitude_cells", value.amplitudeCells)
            .put("repeat", value.repeat.value)
    }

    companion object {
        internal fun fromJson(json: JSONObject?): PixelMotionRule {
            if (json == null) return PixelMotionRule()
            return PixelMotionRule(
                enabled = json.optBoolean("enabled", false),
                mode = PixelMotionMode.fromValue(json.optString("mode")),
                durationMillis = json.optInt("duration_ms", DEFAULT_PIXEL_MOTION_DURATION_MS),
                amplitudeCells = json.optInt("amplitude_cells", DEFAULT_PIXEL_MOTION_AMPLITUDE),
                repeat = PixelMotionRepeat.fromValue(json.optString("repeat")),
            ).normalized()
        }
    }
}

@Immutable
data class CustomCardLayers(
    val top: PixelGrid = PixelGrid.blank(CARD_GRID_WIDTH, CARD_TOP_GRID_HEIGHT),
    val border: PixelGrid = PixelGrid.blank(CARD_GRID_WIDTH, CARD_BODY_GRID_HEIGHT),
    val interior: PixelGrid = PixelGrid.blank(CARD_GRID_WIDTH, CARD_BODY_GRID_HEIGHT),
) {
    fun layer(type: CardPixelLayer): PixelGrid = when (type) {
        CardPixelLayer.Top -> top
        CardPixelLayer.Border -> border
        CardPixelLayer.Interior -> interior
    }

    fun withLayer(type: CardPixelLayer, grid: PixelGrid): CustomCardLayers = when (type) {
        CardPixelLayer.Top -> copy(top = grid.requireSize(CARD_GRID_WIDTH, CARD_TOP_GRID_HEIGHT))
        CardPixelLayer.Border -> copy(border = grid.requireSize(CARD_GRID_WIDTH, CARD_BODY_GRID_HEIGHT))
        CardPixelLayer.Interior -> copy(interior = grid.requireSize(CARD_GRID_WIDTH, CARD_BODY_GRID_HEIGHT))
    }

    val isBlank: Boolean
        get() = top.isBlank() && border.isBlank() && interior.isBlank()

    internal fun toJson(): JSONObject = JSONObject()
        .put("top", top.toJson())
        .put("border", border.toJson())
        .put("interior", interior.toJson())

    companion object {
        internal fun fromJson(json: JSONObject): CustomCardLayers = CustomCardLayers(
            top = PixelGrid.fromJson(
                json.optJSONObject("top") ?: error("Card top layer is missing"),
                CARD_GRID_WIDTH,
                CARD_TOP_GRID_HEIGHT,
            ),
            border = PixelGrid.fromJson(
                json.optJSONObject("border") ?: error("Card border layer is missing"),
                CARD_GRID_WIDTH,
                CARD_BODY_GRID_HEIGHT,
            ),
            interior = PixelGrid.fromJson(
                json.optJSONObject("interior") ?: error("Card interior layer is missing"),
                CARD_GRID_WIDTH,
                CARD_BODY_GRID_HEIGHT,
            ),
        )
    }
}

@Immutable
data class CustomNavigationLayers(
    val top: PixelGrid = PixelGrid.blank(NAVIGATION_GRID_WIDTH, NAVIGATION_TOP_GRID_HEIGHT),
    val border: PixelGrid = PixelGrid.blank(NAVIGATION_GRID_WIDTH, NAVIGATION_BODY_GRID_HEIGHT),
) {
    fun layer(type: NavigationPixelLayer): PixelGrid = when (type) {
        NavigationPixelLayer.Top -> top
        NavigationPixelLayer.Border -> border
    }

    fun withLayer(type: NavigationPixelLayer, grid: PixelGrid): CustomNavigationLayers = when (type) {
        NavigationPixelLayer.Top -> copy(
            top = grid.requireSize(NAVIGATION_GRID_WIDTH, NAVIGATION_TOP_GRID_HEIGHT)
        )
        NavigationPixelLayer.Border -> copy(
            border = grid.requireSize(NAVIGATION_GRID_WIDTH, NAVIGATION_BODY_GRID_HEIGHT)
        )
    }

    val isBlank: Boolean
        get() = top.isBlank() && border.isBlank()

    internal fun toJson(): JSONObject = JSONObject()
        .put("top", top.toJson())
        .put("border", border.toJson())

    companion object {
        internal fun fromJson(json: JSONObject): CustomNavigationLayers = CustomNavigationLayers(
            top = PixelGrid.fromJson(
                json.optJSONObject("top") ?: error("Navigation top layer is missing"),
                NAVIGATION_GRID_WIDTH,
                NAVIGATION_TOP_GRID_HEIGHT,
            ),
            border = PixelGrid.fromJson(
                json.optJSONObject("border") ?: error("Navigation border layer is missing"),
                NAVIGATION_GRID_WIDTH,
                NAVIGATION_BODY_GRID_HEIGHT,
            ),
        )
    }
}

@Immutable
data class CustomCardStyle(
    val id: String = newComponentStyleId("card"),
    val name: String = "Pixel card",
    val author: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val defaultLayers: CustomCardLayers = CustomCardLayers(),
    val cardOverrides: Map<CustomCardTarget, CustomCardLayers> = emptyMap(),
    val bottomBar: CustomNavigationLayers = CustomNavigationLayers(),
    val floatingBottomBar: CustomNavigationLayers = CustomNavigationLayers(),
    val palette: List<Long> = DEFAULT_PIXEL_PALETTE,
    val motion: PixelMotionRule = PixelMotionRule(),
) {
    fun normalized(): CustomCardStyle {
        val safeOverrides = CustomCardTarget.entries
            .filter { it != CustomCardTarget.Default }
            .mapNotNull { target -> cardOverrides[target]?.let { target to it } }
            .toMap()
        return copy(
            id = sanitizeComponentStyleId(id, "card"),
            name = sanitizeComponentStyleName(name).ifBlank { "Pixel card" },
            author = sanitizeComponentStyleAuthor(author),
            updatedAt = updatedAt.coerceAtLeast(0L),
            cardOverrides = safeOverrides,
            palette = sanitizePixelPalette(palette),
            motion = motion.normalized(),
        )
    }

    fun layersFor(target: CustomCardTarget): CustomCardLayers =
        if (target == CustomCardTarget.Default) defaultLayers else cardOverrides[target] ?: defaultLayers

    fun withLayers(target: CustomCardTarget, layers: CustomCardLayers): CustomCardStyle {
        return if (target == CustomCardTarget.Default) {
            copy(defaultLayers = layers)
        } else {
            copy(cardOverrides = cardOverrides.toMutableMap().apply { this[target] = layers })
        }
    }

    fun toJsonString(): String = toJson().toString()

    internal fun toJson(): JSONObject = normalized().let { value ->
        JSONObject()
            .put("schema", COMPONENT_STYLE_SCHEMA)
            .put("version", COMPONENT_STYLE_VERSION)
            .put("kind", ComponentStyleKind.Card.value)
            .put("id", value.id)
            .put("name", value.name)
            .put("author", value.author)
            .put("updated_at", value.updatedAt)
            .put("palette", value.palette.toJsonArray())
            .put("motion", value.motion.toJson())
            .put("default", value.defaultLayers.toJson())
            .put("overrides", JSONObject().apply {
                value.cardOverrides.toSortedMap(compareBy(CustomCardTarget::ordinal)).forEach { (target, layers) ->
                    put(target.value, layers.toJson())
                }
            })
            .put("bottom_bar", value.bottomBar.toJson())
            .put("floating_bottom_bar", value.floatingBottomBar.toJson())
    }

    companion object {
        fun fromJsonString(raw: String): CustomCardStyle {
            requireComponentJsonSize(raw)
            return fromJson(JSONObject(raw))
        }

        internal fun fromJson(json: JSONObject): CustomCardStyle {
            validateComponentHeader(json, ComponentStyleKind.Card)
            val overridesJson = json.optJSONObject("overrides") ?: JSONObject()
            require(overridesJson.length() <= CustomCardTarget.entries.size - 1) {
                "Card style contains too many target overrides"
            }
            val overrides = buildMap {
                val keys = overridesJson.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val target = CustomCardTarget.fromValue(key)
                        ?.takeIf { it != CustomCardTarget.Default }
                        ?: error("Card style contains an unknown target")
                    put(target, CustomCardLayers.fromJson(overridesJson.getJSONObject(key)))
                }
            }
            return CustomCardStyle(
                id = json.optString("id"),
                name = json.optString("name"),
                author = json.optString("author"),
                updatedAt = json.optLong("updated_at", 0L),
                defaultLayers = CustomCardLayers.fromJson(
                    json.optJSONObject("default") ?: error("Default card layers are missing")
                ),
                cardOverrides = overrides,
                bottomBar = CustomNavigationLayers.fromJson(
                    json.optJSONObject("bottom_bar") ?: error("Bottom bar layers are missing")
                ),
                floatingBottomBar = CustomNavigationLayers.fromJson(
                    json.optJSONObject("floating_bottom_bar")
                        ?: error("Floating bottom bar layers are missing")
                ),
                palette = parsePixelPalette(json.optJSONArray("palette")),
                motion = PixelMotionRule.fromJson(json.optJSONObject("motion")),
            ).normalized()
        }
    }
}

@Immutable
data class CustomSwitchStyle(
    val id: String = newComponentStyleId("switch"),
    val name: String = "Pixel switch",
    val author: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val source: CustomSwitchSource = CustomSwitchSource.Pixel,
    val trackOff: PixelGrid = PixelGrid.blank(SWITCH_TRACK_GRID_WIDTH, SWITCH_TRACK_GRID_HEIGHT),
    val trackOn: PixelGrid = PixelGrid.blank(SWITCH_TRACK_GRID_WIDTH, SWITCH_TRACK_GRID_HEIGHT),
    val thumbOff: PixelGrid = PixelGrid.blank(SWITCH_THUMB_GRID_SIZE, SWITCH_THUMB_GRID_SIZE),
    val thumbOn: PixelGrid = PixelGrid.blank(SWITCH_THUMB_GRID_SIZE, SWITCH_THUMB_GRID_SIZE),
    val imageUri: String? = null,
    val imageSha256: String? = null,
    val imageMimeType: String? = null,
    val imageScale: SwitchImageScale = SwitchImageScale.Crop,
    val imageOpacity: Float = 1f,
    val palette: List<Long> = DEFAULT_PIXEL_PALETTE,
    val motion: PixelMotionRule = PixelMotionRule(),
) {
    fun normalized(): CustomSwitchStyle {
        val usesImage = source == CustomSwitchSource.Image
        return copy(
            id = sanitizeComponentStyleId(id, "switch"),
            name = sanitizeComponentStyleName(name).ifBlank { "Pixel switch" },
            author = sanitizeComponentStyleAuthor(author),
            updatedAt = updatedAt.coerceAtLeast(0L),
            imageUri = imageUri
                ?.takeIf { usesImage }
                ?.trim()
                ?.take(MAX_COMPONENT_IMAGE_URI_LENGTH)
                ?.takeIf(String::isNotBlank),
            imageSha256 = imageSha256
                ?.takeIf { usesImage }
                ?.lowercase()
                ?.takeIf { COMPONENT_SHA256_PATTERN.matches(it) },
            imageMimeType = imageMimeType
                ?.takeIf { usesImage }
                ?.trim()
                ?.lowercase()
                ?.take(MAX_COMPONENT_MIME_LENGTH)
                ?.takeIf(String::isNotBlank),
            imageOpacity = imageOpacity.takeIf(Float::isFinite)?.coerceIn(0.1f, 1f) ?: 1f,
            palette = sanitizePixelPalette(palette),
            motion = motion.normalized(),
        )
    }

    fun toJsonString(includeLocalImageUri: Boolean = true): String =
        toJson(includeLocalImageUri).toString()

    internal fun toJson(includeLocalImageUri: Boolean): JSONObject = normalized().let { value ->
        JSONObject()
            .put("schema", COMPONENT_STYLE_SCHEMA)
            .put("version", COMPONENT_STYLE_VERSION)
            .put("kind", ComponentStyleKind.Switch.value)
            .put("id", value.id)
            .put("name", value.name)
            .put("author", value.author)
            .put("updated_at", value.updatedAt)
            .put("source", value.source.value)
            .put("track_off", value.trackOff.toJson())
            .put("track_on", value.trackOn.toJson())
            .put("thumb_off", value.thumbOff.toJson())
            .put("thumb_on", value.thumbOn.toJson())
            .put("image_uri", if (includeLocalImageUri) value.imageUri else null)
            .put("image_sha256", value.imageSha256)
            .put("image_mime", value.imageMimeType)
            .put("image_scale", value.imageScale.value)
            .put("image_opacity", value.imageOpacity.toDouble())
            .put("palette", value.palette.toJsonArray())
            .put("motion", value.motion.toJson())
    }

    companion object {
        fun fromJsonString(raw: String, allowLocalImageUri: Boolean = true): CustomSwitchStyle {
            requireComponentJsonSize(raw)
            return fromJson(JSONObject(raw), allowLocalImageUri)
        }

        internal fun fromJson(json: JSONObject, allowLocalImageUri: Boolean): CustomSwitchStyle {
            validateComponentHeader(json, ComponentStyleKind.Switch)
            return CustomSwitchStyle(
                id = json.optString("id"),
                name = json.optString("name"),
                author = json.optString("author"),
                updatedAt = json.optLong("updated_at", 0L),
                source = CustomSwitchSource.fromValue(json.optString("source")),
                trackOff = PixelGrid.fromJson(
                    json.optJSONObject("track_off") ?: error("Switch off track is missing"),
                    SWITCH_TRACK_GRID_WIDTH,
                    SWITCH_TRACK_GRID_HEIGHT,
                ),
                trackOn = PixelGrid.fromJson(
                    json.optJSONObject("track_on") ?: error("Switch on track is missing"),
                    SWITCH_TRACK_GRID_WIDTH,
                    SWITCH_TRACK_GRID_HEIGHT,
                ),
                thumbOff = PixelGrid.fromJson(
                    json.optJSONObject("thumb_off") ?: error("Switch off thumb is missing"),
                    SWITCH_THUMB_GRID_SIZE,
                    SWITCH_THUMB_GRID_SIZE,
                ),
                thumbOn = PixelGrid.fromJson(
                    json.optJSONObject("thumb_on") ?: error("Switch on thumb is missing"),
                    SWITCH_THUMB_GRID_SIZE,
                    SWITCH_THUMB_GRID_SIZE,
                ),
                imageUri = json.optString("image_uri").takeIf { allowLocalImageUri && it.isNotBlank() },
                imageSha256 = json.optString("image_sha256"),
                imageMimeType = json.optString("image_mime"),
                imageScale = SwitchImageScale.fromValue(json.optString("image_scale")),
                imageOpacity = json.optDouble("image_opacity", 1.0).toFloat(),
                palette = parsePixelPalette(json.optJSONArray("palette")),
                motion = PixelMotionRule.fromJson(json.optJSONObject("motion")),
            ).normalized()
        }
    }
}

internal fun encodeCardStyleLibrary(styles: List<CustomCardStyle>): String = JSONObject()
    .put("schema", COMPONENT_LIBRARY_SCHEMA)
    .put("version", COMPONENT_LIBRARY_VERSION)
    .put("kind", ComponentStyleKind.Card.value)
    .put("items", JSONArray().apply {
        styles.distinctBy(CustomCardStyle::id).take(MAX_SAVED_COMPONENT_STYLES).forEach { put(it.toJson()) }
    })
    .toString()

internal fun decodeCardStyleLibrary(raw: String?): List<CustomCardStyle> {
    if (raw.isNullOrBlank()) return emptyList()
    requireComponentJsonSize(raw, MAX_COMPONENT_LIBRARY_JSON_BYTES)
    val root = JSONObject(raw)
    validateLibraryHeader(root, ComponentStyleKind.Card)
    val items = root.optJSONArray("items") ?: error("Card style library is missing")
    require(items.length() <= MAX_SAVED_COMPONENT_STYLES) { "Card style library is too large" }
    return buildList {
        repeat(items.length()) { index -> add(CustomCardStyle.fromJson(items.getJSONObject(index))) }
    }.distinctBy(CustomCardStyle::id)
}

internal fun encodeSwitchStyleLibrary(styles: List<CustomSwitchStyle>): String = JSONObject()
    .put("schema", COMPONENT_LIBRARY_SCHEMA)
    .put("version", COMPONENT_LIBRARY_VERSION)
    .put("kind", ComponentStyleKind.Switch.value)
    .put("items", JSONArray().apply {
        styles.distinctBy(CustomSwitchStyle::id).take(MAX_SAVED_COMPONENT_STYLES).forEach {
            put(it.toJson(includeLocalImageUri = true))
        }
    })
    .toString()

internal fun decodeSwitchStyleLibrary(raw: String?): List<CustomSwitchStyle> {
    if (raw.isNullOrBlank()) return emptyList()
    requireComponentJsonSize(raw, MAX_COMPONENT_LIBRARY_JSON_BYTES)
    val root = JSONObject(raw)
    validateLibraryHeader(root, ComponentStyleKind.Switch)
    val items = root.optJSONArray("items") ?: error("Switch style library is missing")
    require(items.length() <= MAX_SAVED_COMPONENT_STYLES) { "Switch style library is too large" }
    return buildList {
        repeat(items.length()) { index ->
            add(CustomSwitchStyle.fromJson(items.getJSONObject(index), allowLocalImageUri = true))
        }
    }.distinctBy(CustomSwitchStyle::id)
}

fun parseArgbHex(value: String): Long? {
    val normalized = value.trim().removePrefix("#")
    val expanded = when (normalized.length) {
        6 -> "FF$normalized"
        8 -> normalized
        else -> return null
    }
    return expanded.toLongOrNull(16)?.takeIf(::isValidArgb)
}

fun formatArgbHex(argb: Long): String = "#%08X".format(argb and MAX_ARGB)

fun PixelGrid.hasSameDimensionsAs(other: PixelGrid): Boolean =
    width == other.width && height == other.height

private fun PixelGrid.requireSize(expectedWidth: Int, expectedHeight: Int): PixelGrid {
    require(width == expectedWidth && height == expectedHeight) { "Pixel grid does not match this layer" }
    return this
}

private fun PixelGrid.isBlank(): Boolean = pixels.all { it == TRANSPARENT_PIXEL }

private fun validateComponentHeader(json: JSONObject, expectedKind: ComponentStyleKind) {
    require(json.optString("schema") == COMPONENT_STYLE_SCHEMA) { "Unsupported component style" }
    require(json.optInt("version", 0) in 1..COMPONENT_STYLE_VERSION) { "Unsupported component style version" }
    require(ComponentStyleKind.fromValue(json.optString("kind")) == expectedKind) {
        "Component style type does not match"
    }
}

private fun validateLibraryHeader(json: JSONObject, expectedKind: ComponentStyleKind) {
    require(json.optString("schema") == COMPONENT_LIBRARY_SCHEMA) { "Unsupported component library" }
    require(json.optInt("version", 0) == COMPONENT_LIBRARY_VERSION) { "Unsupported component library version" }
    require(ComponentStyleKind.fromValue(json.optString("kind")) == expectedKind) {
        "Component library type does not match"
    }
}

private fun sanitizeComponentStyleId(value: String, prefix: String): String {
    val sanitized = value.trim().take(MAX_COMPONENT_STYLE_ID_LENGTH)
    return sanitized.takeIf(COMPONENT_STYLE_ID_PATTERN::matches) ?: newComponentStyleId(prefix)
}

fun sanitizeComponentStyleName(value: String): String =
    value.trim().replace(Regex("\\s+"), " ").take(MAX_COMPONENT_STYLE_NAME_LENGTH)

fun sanitizeComponentStyleAuthor(value: String): String =
    value.trim().replace(Regex("\\s+"), " ").take(MAX_COMPONENT_STYLE_AUTHOR_LENGTH)

private fun newComponentStyleId(prefix: String): String = "$prefix-${UUID.randomUUID()}"

private fun sanitizePixelPalette(source: List<Long>): List<Long> {
    val normalized = source.filter(::isValidArgb).distinct().take(MAX_PIXEL_PALETTE_COLORS)
    return normalized.ifEmpty { DEFAULT_PIXEL_PALETTE }
}

private fun parsePixelPalette(source: JSONArray?): List<Long> {
    if (source == null) return DEFAULT_PIXEL_PALETTE
    require(source.length() <= MAX_PIXEL_PALETTE_COLORS) { "Pixel palette is too large" }
    return sanitizePixelPalette(List(source.length()) { index -> source.optLong(index, INVALID_PIXEL) })
}

private fun List<Long>.toJsonArray(): JSONArray = JSONArray().apply { this@toJsonArray.forEach(::put) }

private fun isValidArgb(value: Long): Boolean = value in TRANSPARENT_PIXEL..MAX_ARGB

private fun requireComponentJsonSize(raw: String, maxBytes: Int = MAX_COMPONENT_STYLE_JSON_BYTES) {
    require(raw.toByteArray(Charsets.UTF_8).size in 1..maxBytes) { "Component style data is too large" }
}

const val CARD_GRID_WIDTH = 24
const val CARD_TOP_GRID_HEIGHT = 5
const val CARD_BODY_GRID_HEIGHT = 12
const val CARD_BORDER_GRID_CELLS = 2
const val NAVIGATION_GRID_WIDTH = 24
const val NAVIGATION_TOP_GRID_HEIGHT = 4
const val NAVIGATION_BODY_GRID_HEIGHT = 6
const val NAVIGATION_BORDER_GRID_CELLS = 1
const val SWITCH_TRACK_GRID_WIDTH = 28
const val SWITCH_TRACK_GRID_HEIGHT = 12
const val SWITCH_THUMB_GRID_SIZE = 12
const val TRANSPARENT_PIXEL = 0L
const val MIN_PIXEL_MOTION_DURATION_MS = 600
const val MAX_PIXEL_MOTION_DURATION_MS = 12_000
const val DEFAULT_PIXEL_MOTION_DURATION_MS = 2_400
const val DEFAULT_PIXEL_MOTION_AMPLITUDE = 1
const val MAX_PIXEL_MOTION_AMPLITUDE = 4
const val MAX_SAVED_COMPONENT_STYLES = 24
const val MAX_COMPONENT_IMAGE_BYTES = 500L * 1024L * 1024L
const val MAX_COMPONENT_IMAGE_SIDE = 4096
const val MAX_COMPONENT_IMAGE_PIXELS = 16_777_216L

val DEFAULT_PIXEL_PALETTE = listOf(
    TRANSPARENT_PIXEL,
    0xFFFFFFFFL,
    0xFF15171CL,
    0xFF5457ECL,
    0xFF9D7BF7L,
    0xFF42B9F5L,
    0xFF48C78EL,
    0xFFF0C85AL,
    0xFFEF6C82L,
    0xFF8D6E63L,
)

private const val COMPONENT_STYLE_SCHEMA = "io.github.fixz.apkesu.component-style"
private const val COMPONENT_STYLE_VERSION = 1
private const val COMPONENT_LIBRARY_SCHEMA = "io.github.fixz.apkesu.component-style-library"
private const val COMPONENT_LIBRARY_VERSION = 1
private const val MAX_PIXEL_GRID_SIDE = 48
private const val MAX_PIXEL_GRID_CELLS = 1_024
private const val MAX_PIXEL_PALETTE_COLORS = 24
private const val MAX_COMPONENT_STYLE_NAME_LENGTH = 48
private const val MAX_COMPONENT_STYLE_AUTHOR_LENGTH = 64
private const val MAX_COMPONENT_STYLE_ID_LENGTH = 80
private const val MAX_COMPONENT_IMAGE_URI_LENGTH = 1_024
private const val MAX_COMPONENT_MIME_LENGTH = 80
private const val MAX_COMPONENT_STYLE_JSON_BYTES = 192 * 1024
private const val MAX_COMPONENT_LIBRARY_JSON_BYTES = 2 * 1024 * 1024
private const val MAX_ARGB = 0xFFFFFFFFL
private const val INVALID_PIXEL = -1L
private val COMPONENT_STYLE_ID_PATTERN = Regex("[a-zA-Z0-9][a-zA-Z0-9._-]{2,79}")
private val COMPONENT_SHA256_PATTERN = Regex("[a-f0-9]{64}")
