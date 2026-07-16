package me.weishu.kernelsu.ui.component.decoration

import androidx.compose.runtime.Immutable
import org.json.JSONArray
import org.json.JSONObject

const val UI_DECORATION_CONFIG_KEY = "ui_decoration_config"

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
    Circuit("circuit");

    companion object {
        fun fromValue(value: String?): UiCardDecoration = entries.firstOrNull { it.value == value } ?: Highlight
    }
}

enum class UiBackgroundDecoration(val value: String) {
    None("none"),
    SoftRays("soft_rays"),
    StarMap("star_map"),
    Botanical("botanical"),
    Frost("frost");

    companion object {
        fun fromValue(value: String?): UiBackgroundDecoration = entries.firstOrNull { it.value == value } ?: SoftRays
    }
}

enum class UiTopBarDecoration(val value: String) {
    None("none"),
    FineLine("fine_line"),
    Prism("prism"),
    Seasonal("seasonal"),
    Circuit("circuit");

    companion object {
        fun fromValue(value: String?): UiTopBarDecoration = entries.firstOrNull { it.value == value } ?: FineLine
    }
}

enum class UiNavigationDecoration(val value: String) {
    None("none"),
    UnderGlow("under_glow"),
    LiquidHalo("liquid_halo"),
    Orbit("orbit"),
    MinimalLine("minimal_line");

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
    Tech("tech");

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
}

private fun sanitizeUnitValue(value: Float, fallback: Float): Float {
    return if (value.isFinite()) value.coerceIn(0f, 1f) else fallback
}

const val DEFAULT_UI_DECORATION_INTENSITY = 0.68f
const val DEFAULT_UI_DECORATION_OPACITY = 0.62f
private const val UI_DECORATION_CONFIG_VERSION = 1
