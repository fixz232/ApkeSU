package me.weishu.kernelsu.ui.component.decoration

import androidx.compose.runtime.Immutable
import org.json.JSONArray
import org.json.JSONObject

const val UI_DECORATION_CONFIG_KEY = "ui_decoration_config"
const val UI_DECORATION_CUSTOM_PRESETS_KEY = "ui_decoration_custom_presets"
const val UI_DECORATION_RECENT_COMPONENTS_KEY = "ui_decoration_recent_components"

enum class UiDecorationScope(val value: String) {
    Home("home"),
    SuperUser("superuser"),
    Modules("modules"),
    Settings("settings"),
    Secondary("secondary");

    companion object {
        fun fromValue(value: String?): UiDecorationScope? = entries.firstOrNull { it.value == value }
    }
}

enum class UiCardDecoration(val value: String) {
    None("none"),
    Highlight("highlight"),
    Blossom("blossom"),
    Lotus("lotus"),
    Maple("maple"),
    Snow("snow"),
    Circuit("circuit"),
    PixelFrame("pixel_frame"),
    PixelHandheld("pixel_handheld"),
    PixelArcade("pixel_arcade"),
    PixelPastoral("pixel_pastoral"),
    PixelStarVoyage("pixel_star_voyage"),
    PixelInkJade("pixel_ink_jade"),
    PixelWasteland("pixel_wasteland"),
    PixelOcean("pixel_ocean"),
    PixelCyber("pixel_cyber"),
    PixelThreeKingdoms("pixel_three_kingdoms"),
    PixelBianliang("pixel_bianliang"),
    PixelFishingHarbor("pixel_fishing_harbor"),
    PixelTribalJungle("pixel_tribal_jungle"),
    PixelLavaValley("pixel_lava_valley"),
    PixelDunhuangDesert("pixel_dunhuang_desert"),
    PixelVikingSnowfield("pixel_viking_snowfield"),
    PixelJiangnanWatertown("pixel_jiangnan_watertown"),
    PixelCloudTown("pixel_cloud_town");

    companion object {
        fun fromValue(value: String?): UiCardDecoration = entries.firstOrNull { it.value == value } ?: Highlight
    }
}

internal val PIXEL_CARD_DECORATIONS: Set<UiCardDecoration> = setOf(
    UiCardDecoration.PixelFrame,
    UiCardDecoration.PixelHandheld,
    UiCardDecoration.PixelArcade,
    UiCardDecoration.PixelPastoral,
    UiCardDecoration.PixelStarVoyage,
    UiCardDecoration.PixelInkJade,
    UiCardDecoration.PixelWasteland,
    UiCardDecoration.PixelOcean,
    UiCardDecoration.PixelCyber,
    UiCardDecoration.PixelThreeKingdoms,
    UiCardDecoration.PixelBianliang,
    UiCardDecoration.PixelFishingHarbor,
    UiCardDecoration.PixelTribalJungle,
    UiCardDecoration.PixelLavaValley,
    UiCardDecoration.PixelDunhuangDesert,
    UiCardDecoration.PixelVikingSnowfield,
    UiCardDecoration.PixelJiangnanWatertown,
    UiCardDecoration.PixelCloudTown,
)

enum class UiBackgroundDecoration(val value: String) {
    None("none"),
    SoftRays("soft_rays"),
    StarMap("star_map"),
    Botanical("botanical"),
    Frost("frost"),
    PixelGrid("pixel_grid");

    companion object {
        fun fromValue(value: String?): UiBackgroundDecoration = entries.firstOrNull { it.value == value } ?: SoftRays
    }
}

enum class UiTopBarDecoration(val value: String) {
    None("none"),
    FineLine("fine_line"),
    Prism("prism"),
    Seasonal("seasonal"),
    Circuit("circuit"),
    PixelHud("pixel_hud");

    companion object {
        fun fromValue(value: String?): UiTopBarDecoration = entries.firstOrNull { it.value == value } ?: FineLine
    }
}

enum class UiNavigationDecoration(val value: String) {
    None("none"),
    UnderGlow("under_glow"),
    LiquidHalo("liquid_halo"),
    Orbit("orbit"),
    MinimalLine("minimal_line"),
    PixelDock("pixel_dock");

    companion object {
        fun fromValue(value: String?): UiNavigationDecoration = entries.firstOrNull { it.value == value } ?: UnderGlow
    }
}

enum class UiDecorationPreset(val value: String) {
    Refined("refined"),
    Blossom("blossom"),
    Lotus("lotus"),
    Autumn("autumn"),
    Winter("winter"),
    Tech("tech"),
    Pixel("pixel");

    companion object {
        fun fromValue(value: String?): UiDecorationPreset? = entries.firstOrNull { it.value == value }
    }
}

@Immutable
data class UiDecorationConfig(
    val enabled: Boolean = false,
    val card: UiCardDecoration = UiCardDecoration.Highlight,
    val background: UiBackgroundDecoration = UiBackgroundDecoration.SoftRays,
    val topBar: UiTopBarDecoration = UiTopBarDecoration.FineLine,
    val navigation: UiNavigationDecoration = UiNavigationDecoration.UnderGlow,
    val intensity: Float = DEFAULT_UI_DECORATION_INTENSITY,
    val opacity: Float = DEFAULT_UI_DECORATION_OPACITY,
    val motionEnabled: Boolean = true,
    val scopes: Set<UiDecorationScope> = UiDecorationScope.entries.toSet(),
) {
    fun normalized(): UiDecorationConfig = copy(
        intensity = sanitizeUnitValue(intensity, DEFAULT_UI_DECORATION_INTENSITY),
        opacity = sanitizeUnitValue(opacity, DEFAULT_UI_DECORATION_OPACITY),
        scopes = scopes.ifEmpty { UiDecorationScope.entries.toSet() },
    )

    fun isActiveFor(scope: UiDecorationScope): Boolean = enabled && scope in scopes

    fun deduplicateNativePixelChrome(pixelStyleActive: Boolean): UiDecorationConfig {
        if (!pixelStyleActive) return this
        return copy(
            background = background.withoutNativeDuplicate(UiBackgroundDecoration.PixelGrid),
            topBar = topBar.withoutNativeDuplicate(UiTopBarDecoration.PixelHud),
            navigation = navigation.withoutNativeDuplicate(UiNavigationDecoration.PixelDock),
        )
    }

    fun effectiveOnNativePixelSurface(pixelStyleActive: Boolean): UiDecorationConfig {
        val chrome = deduplicateNativePixelChrome(pixelStyleActive)
        return if (pixelStyleActive) {
            chrome.copy(card = card.withoutNativeDuplicate(PIXEL_CARD_DECORATIONS))
        } else {
            chrome
        }
    }

    fun withPreset(preset: UiDecorationPreset): UiDecorationConfig {
        val components = presetComponents(preset)
        return copy(
            card = components.card,
            background = components.background,
            topBar = components.topBar,
            navigation = components.navigation,
        )
    }

    fun matchingPreset(): UiDecorationPreset? = UiDecorationPreset.entries.firstOrNull { preset ->
        val components = presetComponents(preset)
        card == components.card &&
            background == components.background &&
            topBar == components.topBar &&
            navigation == components.navigation
    }

    fun toJsonString(): String {
        val normalized = normalized()
        return JSONObject().apply {
            put("version", UI_DECORATION_CONFIG_VERSION)
            put("enabled", normalized.enabled)
            put("card", normalized.card.value)
            put("background", normalized.background.value)
            put("top_bar", normalized.topBar.value)
            put("navigation", normalized.navigation.value)
            put("intensity", normalized.intensity.toDouble())
            put("opacity", normalized.opacity.toDouble())
            put("motion_enabled", normalized.motionEnabled)
            put("scopes", JSONArray().apply {
                UiDecorationScope.entries.filter(normalized.scopes::contains).forEach { put(it.value) }
            })
        }.toString()
    }

    companion object {
        fun fromJsonString(raw: String?): UiDecorationConfig {
            if (raw.isNullOrBlank()) return UiDecorationConfig()
            return runCatching {
                val json = JSONObject(raw)
                val scopesJson = json.optJSONArray("scopes")
                val scopes = buildSet {
                    if (scopesJson != null) {
                        repeat(scopesJson.length()) { index ->
                            UiDecorationScope.fromValue(scopesJson.optString(index))?.let(::add)
                        }
                    }
                }
                UiDecorationConfig(
                    enabled = json.optBoolean("enabled", false),
                    card = UiCardDecoration.fromValue(json.optString("card")),
                    background = UiBackgroundDecoration.fromValue(json.optString("background")),
                    topBar = UiTopBarDecoration.fromValue(json.optString("top_bar")),
                    navigation = UiNavigationDecoration.fromValue(json.optString("navigation")),
                    intensity = json.optDouble("intensity", DEFAULT_UI_DECORATION_INTENSITY.toDouble()).toFloat(),
                    opacity = json.optDouble("opacity", DEFAULT_UI_DECORATION_OPACITY.toDouble()).toFloat(),
                    motionEnabled = json.optBoolean("motion_enabled", true),
                    scopes = scopes,
                ).normalized()
            }.getOrElse { UiDecorationConfig() }
        }
    }
}

@Immutable
data class CustomUiDecorationPreset(
    val id: String,
    val name: String,
    val updatedAt: Long,
    val config: UiDecorationConfig,
) {
    internal fun toJsonObject(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("updated_at", updatedAt)
        put("config", JSONObject(config.normalized().toJsonString()))
    }

    companion object {
        internal fun fromJsonObject(json: JSONObject): CustomUiDecorationPreset {
            val id = json.optString("id").trim().take(MAX_CUSTOM_PRESET_ID_LENGTH)
            val name = sanitizeCustomUiDecorationPresetName(json.optString("name"))
            require(id.isNotBlank()) { "Preset id is missing" }
            require(name.isNotBlank()) { "Preset name is missing" }
            val configJson = json.optJSONObject("config") ?: error("Preset config is missing")
            return CustomUiDecorationPreset(
                id = id,
                name = name,
                updatedAt = json.optLong("updated_at", 0L).coerceAtLeast(0L),
                config = UiDecorationConfig.fromJsonString(configJson.toString()).normalized(),
            )
        }
    }
}

fun customUiDecorationPresetsToJson(presets: List<CustomUiDecorationPreset>): String {
    val normalized = presets
        .distinctBy(CustomUiDecorationPreset::id)
        .take(MAX_CUSTOM_UI_DECORATION_PRESETS)
    return JSONObject().apply {
        put("schema", UI_DECORATION_PRESET_BUNDLE_SCHEMA)
        put("version", UI_DECORATION_PRESET_BUNDLE_VERSION)
        put("presets", JSONArray().apply {
            normalized.forEach { put(it.toJsonObject()) }
        })
    }.toString(2)
}

fun customUiDecorationPresetsFromJson(raw: String?): List<CustomUiDecorationPreset> {
    require(!raw.isNullOrBlank()) { "Preset file is empty" }
    val root = JSONObject(raw)
    require(root.optString("schema") == UI_DECORATION_PRESET_BUNDLE_SCHEMA) {
        "Unsupported preset file"
    }
    require(root.optInt("version", 0) in 1..UI_DECORATION_PRESET_BUNDLE_VERSION) {
        "Unsupported preset version"
    }
    val presetsJson = root.optJSONArray("presets") ?: error("Preset list is missing")
    require(presetsJson.length() <= MAX_IMPORTED_UI_DECORATION_PRESETS) {
        "Preset file contains too many entries"
    }
    return buildList {
        repeat(presetsJson.length()) { index ->
            val presetJson = presetsJson.optJSONObject(index) ?: return@repeat
            runCatching { CustomUiDecorationPreset.fromJsonObject(presetJson) }
                .getOrNull()
                ?.let(::add)
        }
    }.distinctBy(CustomUiDecorationPreset::id)
}

fun sanitizeCustomUiDecorationPresetName(name: String): String =
    name.trim().replace(Regex("\\s+"), " ").take(MAX_CUSTOM_PRESET_NAME_LENGTH)

fun UiDecorationConfig.componentTokens(): List<String> = listOf(
    "card:${card.value}",
    "background:${background.value}",
    "top_bar:${topBar.value}",
    "navigation:${navigation.value}",
)

fun UiDecorationConfig.forPreview(): UiDecorationConfig = copy(
    enabled = true,
    scopes = UiDecorationScope.entries.toSet(),
)

internal fun UiCardDecoration.withoutNativeDuplicate(
    nativeDecorations: Set<UiCardDecoration>,
): UiCardDecoration = if (this in nativeDecorations) UiCardDecoration.None else this

private fun UiBackgroundDecoration.withoutNativeDuplicate(
    nativeDecoration: UiBackgroundDecoration,
): UiBackgroundDecoration = if (this == nativeDecoration) UiBackgroundDecoration.None else this

private fun UiTopBarDecoration.withoutNativeDuplicate(
    nativeDecoration: UiTopBarDecoration,
): UiTopBarDecoration = if (this == nativeDecoration) UiTopBarDecoration.None else this

private fun UiNavigationDecoration.withoutNativeDuplicate(
    nativeDecoration: UiNavigationDecoration,
): UiNavigationDecoration = if (this == nativeDecoration) UiNavigationDecoration.None else this

private data class UiDecorationComponents(
    val card: UiCardDecoration,
    val background: UiBackgroundDecoration,
    val topBar: UiTopBarDecoration,
    val navigation: UiNavigationDecoration,
)

private fun presetComponents(preset: UiDecorationPreset): UiDecorationComponents = when (preset) {
    UiDecorationPreset.Refined -> UiDecorationComponents(
        card = UiCardDecoration.Highlight,
        background = UiBackgroundDecoration.SoftRays,
        topBar = UiTopBarDecoration.FineLine,
        navigation = UiNavigationDecoration.UnderGlow,
    )
    UiDecorationPreset.Blossom -> UiDecorationComponents(
        card = UiCardDecoration.Blossom,
        background = UiBackgroundDecoration.Botanical,
        topBar = UiTopBarDecoration.Seasonal,
        navigation = UiNavigationDecoration.LiquidHalo,
    )
    UiDecorationPreset.Lotus -> UiDecorationComponents(
        card = UiCardDecoration.Lotus,
        background = UiBackgroundDecoration.SoftRays,
        topBar = UiTopBarDecoration.Prism,
        navigation = UiNavigationDecoration.LiquidHalo,
    )
    UiDecorationPreset.Autumn -> UiDecorationComponents(
        card = UiCardDecoration.Maple,
        background = UiBackgroundDecoration.Botanical,
        topBar = UiTopBarDecoration.Seasonal,
        navigation = UiNavigationDecoration.MinimalLine,
    )
    UiDecorationPreset.Winter -> UiDecorationComponents(
        card = UiCardDecoration.Snow,
        background = UiBackgroundDecoration.Frost,
        topBar = UiTopBarDecoration.FineLine,
        navigation = UiNavigationDecoration.UnderGlow,
    )
    UiDecorationPreset.Tech -> UiDecorationComponents(
        card = UiCardDecoration.Circuit,
        background = UiBackgroundDecoration.StarMap,
        topBar = UiTopBarDecoration.Circuit,
        navigation = UiNavigationDecoration.Orbit,
    )
    UiDecorationPreset.Pixel -> UiDecorationComponents(
        card = UiCardDecoration.PixelFrame,
        background = UiBackgroundDecoration.PixelGrid,
        topBar = UiTopBarDecoration.PixelHud,
        navigation = UiNavigationDecoration.PixelDock,
    )
}

private fun sanitizeUnitValue(value: Float, fallback: Float): Float {
    return if (value.isFinite()) value.coerceIn(0f, 1f) else fallback
}

const val DEFAULT_UI_DECORATION_INTENSITY = 0.68f
const val DEFAULT_UI_DECORATION_OPACITY = 0.62f
private const val UI_DECORATION_CONFIG_VERSION = 2
const val MAX_CUSTOM_UI_DECORATION_PRESETS = 50
private const val MAX_IMPORTED_UI_DECORATION_PRESETS = 100
private const val MAX_CUSTOM_PRESET_NAME_LENGTH = 40
private const val MAX_CUSTOM_PRESET_ID_LENGTH = 80
private const val UI_DECORATION_PRESET_BUNDLE_SCHEMA = "apkesu_ui_decoration_presets"
private const val UI_DECORATION_PRESET_BUNDLE_VERSION = 1
