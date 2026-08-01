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

enum class SwitchImageBlend(val value: String) {
    Normal("normal"),
    Multiply("multiply"),
    Screen("screen"),
    Add("add");

    companion object {
        fun fromValue(value: String?): SwitchImageBlend = entries.firstOrNull { it.value == value } ?: Normal
    }
}

enum class SwitchTransitionEasing(val value: String) {
    Standard("standard"),
    Linear("linear"),
    Accelerate("accelerate"),
    Decelerate("decelerate");

    companion object {
        fun fromValue(value: String?): SwitchTransitionEasing =
            entries.firstOrNull { it.value == value } ?: Standard
    }
}

@Immutable
data class SwitchImageAppearance(
    val scale: SwitchImageScale = SwitchImageScale.Crop,
    val opacity: Float = 1f,
    val zoom: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val rotationDegrees: Float = 0f,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val tint: Long? = null,
    val saturation: Float = 1f,
    val brightness: Float = 0f,
    val blend: SwitchImageBlend = SwitchImageBlend.Normal,
) {
    fun normalized(): SwitchImageAppearance = copy(
        opacity = opacity.takeIf(Float::isFinite)?.coerceIn(0.1f, 1f) ?: 1f,
        zoom = zoom.takeIf(Float::isFinite)?.coerceIn(0.5f, 3f) ?: 1f,
        offsetX = offsetX.takeIf(Float::isFinite)?.coerceIn(-1f, 1f) ?: 0f,
        offsetY = offsetY.takeIf(Float::isFinite)?.coerceIn(-1f, 1f) ?: 0f,
        rotationDegrees = rotationDegrees.takeIf(Float::isFinite)?.coerceIn(-180f, 180f) ?: 0f,
        tint = tint?.takeIf(::isValidArgb),
        saturation = saturation.takeIf(Float::isFinite)?.coerceIn(0f, 2f) ?: 1f,
        brightness = brightness.takeIf(Float::isFinite)?.coerceIn(-1f, 1f) ?: 0f,
    )

    internal fun toJson(): JSONObject = normalized().let { value ->
        JSONObject()
            .put("scale", value.scale.value)
            .put("opacity", value.opacity.toDouble())
            .put("zoom", value.zoom.toDouble())
            .put("offset_x", value.offsetX.toDouble())
            .put("offset_y", value.offsetY.toDouble())
            .put("rotation", value.rotationDegrees.toDouble())
            .put("flip_horizontal", value.flipHorizontal)
            .put("flip_vertical", value.flipVertical)
            .put("tint", value.tint)
            .put("saturation", value.saturation.toDouble())
            .put("brightness", value.brightness.toDouble())
            .put("blend", value.blend.value)
    }

    companion object {
        internal fun fromJson(json: JSONObject?): SwitchImageAppearance? = json?.let {
            SwitchImageAppearance(
                scale = SwitchImageScale.fromValue(it.optString("scale")),
                opacity = it.optDouble("opacity", 1.0).toFloat(),
                zoom = it.optDouble("zoom", 1.0).toFloat(),
                offsetX = it.optDouble("offset_x", 0.0).toFloat(),
                offsetY = it.optDouble("offset_y", 0.0).toFloat(),
                rotationDegrees = it.optDouble("rotation", 0.0).toFloat(),
                flipHorizontal = it.optBoolean("flip_horizontal", false),
                flipVertical = it.optBoolean("flip_vertical", false),
                tint = it.optLongOrNull("tint"),
                saturation = it.optDouble("saturation", 1.0).toFloat(),
                brightness = it.optDouble("brightness", 0.0).toFloat(),
                blend = SwitchImageBlend.fromValue(it.optString("blend")),
            ).normalized()
        }
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
    val imageOnUri: String? = null,
    val imageOnSha256: String? = null,
    val imageOnMimeType: String? = null,
    val imageScale: SwitchImageScale = SwitchImageScale.Crop,
    val imageOpacity: Float = 1f,
    val imageZoom: Float = 1f,
    val imageOffsetX: Float = 0f,
    val imageOffsetY: Float = 0f,
    val imageRotationDegrees: Float = 0f,
    val imageFlipHorizontal: Boolean = false,
    val imageFlipVertical: Boolean = false,
    val imageTint: Long? = null,
    val imageSaturation: Float = 1f,
    val imageBrightness: Float = 0f,
    val imageBlend: SwitchImageBlend = SwitchImageBlend.Normal,
    // Optional state-specific image settings keep older styles compatible while
    // allowing the disabled and enabled images to be authored independently.
    val imageOffAppearance: SwitchImageAppearance? = null,
    val imageOnAppearance: SwitchImageAppearance? = null,
    val trackScaleX: Float = 1f,
    val trackScaleY: Float = 1f,
    val trackBaseColor: Long = 0xFF3D4450L,
    // Optional state-specific colors keep legacy styles compatible while allowing
    // the off and on appearances to be authored independently.
    val trackOffColorOverride: Long? = null,
    val trackOnColorOverride: Long? = null,
    val cornerRadiusFraction: Float = 0.5f,
    val borderColor: Long = 0x52FFFFFFL,
    val borderOffColorOverride: Long? = null,
    val borderOnColorOverride: Long? = null,
    val borderWidthDp: Float = 1f,
    val thumbScale: Float = 1f,
    val thumbPaddingDp: Float = 3f,
    val thumbTravel: Float = 1f,
    val thumbBaseColor: Long = 0xFFFFFFFFL,
    val thumbOffColorOverride: Long? = null,
    val thumbOnColorOverride: Long? = null,
    val shadowColor: Long = 0x66000000L,
    val shadowRadiusDp: Float = 0f,
    val glowColor: Long = 0x668A7DFFL,
    val glowRadiusDp: Float = 0f,
    val disabledAlpha: Float = 0.45f,
    val transitionDurationMillis: Int = 220,
    val transitionEasing: SwitchTransitionEasing = SwitchTransitionEasing.Standard,
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
            imageOnUri = imageOnUri
                ?.takeIf { usesImage }
                ?.trim()
                ?.take(MAX_COMPONENT_IMAGE_URI_LENGTH)
                ?.takeIf(String::isNotBlank),
            imageOnSha256 = imageOnSha256
                ?.takeIf { usesImage }
                ?.lowercase()
                ?.takeIf { COMPONENT_SHA256_PATTERN.matches(it) },
            imageOnMimeType = imageOnMimeType
                ?.takeIf { usesImage }
                ?.trim()
                ?.lowercase()
                ?.take(MAX_COMPONENT_MIME_LENGTH)
                ?.takeIf(String::isNotBlank),
            imageOpacity = imageOpacity.takeIf(Float::isFinite)?.coerceIn(0.1f, 1f) ?: 1f,
            imageZoom = imageZoom.takeIf(Float::isFinite)?.coerceIn(0.5f, 3f) ?: 1f,
            imageOffsetX = imageOffsetX.takeIf(Float::isFinite)?.coerceIn(-1f, 1f) ?: 0f,
            imageOffsetY = imageOffsetY.takeIf(Float::isFinite)?.coerceIn(-1f, 1f) ?: 0f,
            imageRotationDegrees = imageRotationDegrees.takeIf(Float::isFinite)?.coerceIn(-180f, 180f) ?: 0f,
            imageTint = imageTint?.takeIf(::isValidArgb),
            imageSaturation = imageSaturation.takeIf(Float::isFinite)?.coerceIn(0f, 2f) ?: 1f,
            imageBrightness = imageBrightness.takeIf(Float::isFinite)?.coerceIn(-1f, 1f) ?: 0f,
            imageOffAppearance = imageOffAppearance?.normalized(),
            imageOnAppearance = imageOnAppearance?.normalized(),
            trackScaleX = trackScaleX.takeIf(Float::isFinite)?.coerceIn(0.65f, 1f) ?: 1f,
            trackScaleY = trackScaleY.takeIf(Float::isFinite)?.coerceIn(0.55f, 1f) ?: 1f,
            trackBaseColor = trackBaseColor.takeIf(::isValidArgb) ?: 0xFF3D4450L,
            trackOffColorOverride = trackOffColorOverride?.takeIf(::isValidArgb),
            trackOnColorOverride = trackOnColorOverride?.takeIf(::isValidArgb),
            cornerRadiusFraction = cornerRadiusFraction.takeIf(Float::isFinite)?.coerceIn(0f, 0.5f) ?: 0.5f,
            borderColor = borderColor.takeIf(::isValidArgb) ?: 0x52FFFFFFL,
            borderOffColorOverride = borderOffColorOverride?.takeIf(::isValidArgb),
            borderOnColorOverride = borderOnColorOverride?.takeIf(::isValidArgb),
            borderWidthDp = borderWidthDp.takeIf(Float::isFinite)?.coerceIn(0f, 4f) ?: 1f,
            thumbScale = thumbScale.takeIf(Float::isFinite)?.coerceIn(0.55f, 1.1f) ?: 1f,
            thumbPaddingDp = thumbPaddingDp.takeIf(Float::isFinite)?.coerceIn(0f, 8f) ?: 3f,
            thumbTravel = thumbTravel.takeIf(Float::isFinite)?.coerceIn(0.5f, 1f) ?: 1f,
            thumbBaseColor = thumbBaseColor.takeIf(::isValidArgb) ?: 0xFFFFFFFFL,
            thumbOffColorOverride = thumbOffColorOverride?.takeIf(::isValidArgb),
            thumbOnColorOverride = thumbOnColorOverride?.takeIf(::isValidArgb),
            shadowColor = shadowColor.takeIf(::isValidArgb) ?: 0x66000000L,
            shadowRadiusDp = shadowRadiusDp.takeIf(Float::isFinite)?.coerceIn(0f, 8f) ?: 0f,
            glowColor = glowColor.takeIf(::isValidArgb) ?: 0x668A7DFFL,
            glowRadiusDp = glowRadiusDp.takeIf(Float::isFinite)?.coerceIn(0f, 8f) ?: 0f,
            disabledAlpha = disabledAlpha.takeIf(Float::isFinite)?.coerceIn(0.2f, 1f) ?: 0.45f,
            transitionDurationMillis = transitionDurationMillis.coerceIn(
                MIN_SWITCH_TRANSITION_DURATION_MS,
                MAX_SWITCH_TRANSITION_DURATION_MS,
            ),
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
            .put("image_on_uri", if (includeLocalImageUri) value.imageOnUri else null)
            .put("image_on_sha256", value.imageOnSha256)
            .put("image_on_mime", value.imageOnMimeType)
            .put("image_scale", value.imageScale.value)
            .put("image_opacity", value.imageOpacity.toDouble())
            .put("image_zoom", value.imageZoom.toDouble())
            .put("image_offset_x", value.imageOffsetX.toDouble())
            .put("image_offset_y", value.imageOffsetY.toDouble())
            .put("image_rotation", value.imageRotationDegrees.toDouble())
            .put("image_flip_horizontal", value.imageFlipHorizontal)
            .put("image_flip_vertical", value.imageFlipVertical)
            .put("image_tint", value.imageTint)
            .put("image_saturation", value.imageSaturation.toDouble())
            .put("image_brightness", value.imageBrightness.toDouble())
            .put("image_blend", value.imageBlend.value)
            .put("image_off_appearance", value.imageOffAppearance?.toJson())
            .put("image_on_appearance", value.imageOnAppearance?.toJson())
            .put("track_scale_x", value.trackScaleX.toDouble())
            .put("track_scale_y", value.trackScaleY.toDouble())
            .put("track_base_color", value.trackBaseColor)
            .put("track_off_color", value.trackOffColorOverride)
            .put("track_on_color", value.trackOnColorOverride)
            .put("corner_radius_fraction", value.cornerRadiusFraction.toDouble())
            .put("border_color", value.borderColor)
            .put("border_off_color", value.borderOffColorOverride)
            .put("border_on_color", value.borderOnColorOverride)
            .put("border_width_dp", value.borderWidthDp.toDouble())
            .put("thumb_scale", value.thumbScale.toDouble())
            .put("thumb_padding_dp", value.thumbPaddingDp.toDouble())
            .put("thumb_travel", value.thumbTravel.toDouble())
            .put("thumb_base_color", value.thumbBaseColor)
            .put("thumb_off_color", value.thumbOffColorOverride)
            .put("thumb_on_color", value.thumbOnColorOverride)
            .put("shadow_color", value.shadowColor)
            .put("shadow_radius_dp", value.shadowRadiusDp.toDouble())
            .put("glow_color", value.glowColor)
            .put("glow_radius_dp", value.glowRadiusDp.toDouble())
            .put("disabled_alpha", value.disabledAlpha.toDouble())
            .put("transition_duration_ms", value.transitionDurationMillis)
            .put("transition_easing", value.transitionEasing.value)
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
                imageOnUri = json.optString("image_on_uri").takeIf { allowLocalImageUri && it.isNotBlank() },
                imageOnSha256 = json.optString("image_on_sha256"),
                imageOnMimeType = json.optString("image_on_mime"),
                imageScale = SwitchImageScale.fromValue(json.optString("image_scale")),
                imageOpacity = json.optDouble("image_opacity", 1.0).toFloat(),
                imageZoom = json.optDouble("image_zoom", 1.0).toFloat(),
                imageOffsetX = json.optDouble("image_offset_x", 0.0).toFloat(),
                imageOffsetY = json.optDouble("image_offset_y", 0.0).toFloat(),
                imageRotationDegrees = json.optDouble("image_rotation", 0.0).toFloat(),
                imageFlipHorizontal = json.optBoolean("image_flip_horizontal", false),
                imageFlipVertical = json.optBoolean("image_flip_vertical", false),
                imageTint = json.optLongOrNull("image_tint"),
                imageSaturation = json.optDouble("image_saturation", 1.0).toFloat(),
                imageBrightness = json.optDouble("image_brightness", 0.0).toFloat(),
                imageBlend = SwitchImageBlend.fromValue(json.optString("image_blend")),
                imageOffAppearance = SwitchImageAppearance.fromJson(
                    json.optJSONObject("image_off_appearance"),
                ),
                imageOnAppearance = SwitchImageAppearance.fromJson(
                    json.optJSONObject("image_on_appearance"),
                ),
                trackScaleX = json.optDouble("track_scale_x", 1.0).toFloat(),
                trackScaleY = json.optDouble("track_scale_y", 1.0).toFloat(),
                trackBaseColor = json.optLong("track_base_color", 0xFF3D4450L),
                trackOffColorOverride = json.optLongOrNull("track_off_color"),
                trackOnColorOverride = json.optLongOrNull("track_on_color"),
                cornerRadiusFraction = json.optDouble("corner_radius_fraction", 0.5).toFloat(),
                borderColor = json.optLong("border_color", 0x52FFFFFFL),
                borderOffColorOverride = json.optLongOrNull("border_off_color"),
                borderOnColorOverride = json.optLongOrNull("border_on_color"),
                borderWidthDp = json.optDouble("border_width_dp", 1.0).toFloat(),
                thumbScale = json.optDouble("thumb_scale", 1.0).toFloat(),
                thumbPaddingDp = json.optDouble("thumb_padding_dp", 3.0).toFloat(),
                thumbTravel = json.optDouble("thumb_travel", 1.0).toFloat(),
                thumbBaseColor = json.optLong("thumb_base_color", 0xFFFFFFFFL),
                thumbOffColorOverride = json.optLongOrNull("thumb_off_color"),
                thumbOnColorOverride = json.optLongOrNull("thumb_on_color"),
                shadowColor = json.optLong("shadow_color", 0x66000000L),
                shadowRadiusDp = json.optDouble("shadow_radius_dp", 0.0).toFloat(),
                glowColor = json.optLong("glow_color", 0x668A7DFFL),
                glowRadiusDp = json.optDouble("glow_radius_dp", 0.0).toFloat(),
                disabledAlpha = json.optDouble("disabled_alpha", 0.45).toFloat(),
                transitionDurationMillis = json.optInt("transition_duration_ms", 220),
                transitionEasing = SwitchTransitionEasing.fromValue(json.optString("transition_easing")),
                palette = parsePixelPalette(json.optJSONArray("palette")),
                motion = PixelMotionRule.fromJson(json.optJSONObject("motion")),
            ).normalized()
        }
    }
}

fun CustomSwitchStyle.imageAppearanceFor(on: Boolean): SwitchImageAppearance {
    val legacy = SwitchImageAppearance(
        scale = imageScale,
        opacity = imageOpacity,
        zoom = imageZoom,
        offsetX = imageOffsetX,
        offsetY = imageOffsetY,
        rotationDegrees = imageRotationDegrees,
        flipHorizontal = imageFlipHorizontal,
        flipVertical = imageFlipVertical,
        tint = imageTint,
        saturation = imageSaturation,
        brightness = imageBrightness,
        blend = imageBlend,
    ).normalized()
    return (if (on) imageOnAppearance else imageOffAppearance)?.normalized() ?: legacy
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

private fun JSONObject.optLongOrNull(key: String): Long? {
    val raw = opt(key)
    if (raw == null || raw === JSONObject.NULL) return null
    return (raw as? Number)?.toLong() ?: raw.toString().toLongOrNull()
}

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
private const val COMPONENT_STYLE_VERSION = 2
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
const val MIN_SWITCH_TRANSITION_DURATION_MS = 150
const val MAX_SWITCH_TRANSITION_DURATION_MS = 250
private const val MAX_COMPONENT_STYLE_JSON_BYTES = 192 * 1024
private const val MAX_COMPONENT_LIBRARY_JSON_BYTES = 2 * 1024 * 1024
private const val MAX_ARGB = 0xFFFFFFFFL
private const val INVALID_PIXEL = -1L
private val COMPONENT_STYLE_ID_PATTERN = Regex("[a-zA-Z0-9][a-zA-Z0-9._-]{2,79}")
private val COMPONENT_SHA256_PATTERN = Regex("[a-f0-9]{64}")
