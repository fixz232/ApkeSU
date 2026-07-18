package me.weishu.kernelsu.ui.component.pixel

import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import me.weishu.kernelsu.R

const val PIXEL_STYLE_KEY = "pixel_style"

enum class PixelStyle(
    val value: String,
    @StringRes val labelRes: Int,
    @StringRes val summaryRes: Int,
    @ColorInt val keyColor: Int,
) {
    ClassicHandheld(
        value = "classic_handheld",
        labelRes = R.string.pixel_style_classic_handheld,
        summaryRes = R.string.pixel_style_classic_handheld_summary,
        keyColor = 0xFF556B3F.toInt(),
    ),
    NeonArcade(
        value = "neon_arcade",
        labelRes = R.string.pixel_style_neon_arcade,
        summaryRes = R.string.pixel_style_neon_arcade_summary,
        keyColor = 0xFF007F7B.toInt(),
    ),
    PastoralFields(
        value = "pastoral_fields",
        labelRes = R.string.pixel_style_pastoral_fields,
        summaryRes = R.string.pixel_style_pastoral_fields_summary,
        keyColor = 0xFF5E7650.toInt(),
    ),
    StarVoyage(
        value = "star_voyage",
        labelRes = R.string.pixel_style_star_voyage,
        summaryRes = R.string.pixel_style_star_voyage_summary,
        keyColor = 0xFF526AC7.toInt(),
    ),
    InkJade(
        value = "ink_jade",
        labelRes = R.string.pixel_style_ink_jade,
        summaryRes = R.string.pixel_style_ink_jade_summary,
        keyColor = 0xFF3D705E.toInt(),
    ),
    RustWasteland(
        value = "rust_wasteland",
        labelRes = R.string.pixel_style_rust_wasteland,
        summaryRes = R.string.pixel_style_rust_wasteland_summary,
        keyColor = 0xFF825038.toInt(),
    ),
    OceanDepths(
        value = "ocean_depths",
        labelRes = R.string.pixel_style_ocean_depths,
        summaryRes = R.string.pixel_style_ocean_depths_summary,
        keyColor = 0xFF167A8A.toInt(),
    ),
    CyberHacker(
        value = "cyber_hacker",
        labelRes = R.string.pixel_style_cyber_hacker,
        summaryRes = R.string.pixel_style_cyber_hacker_summary,
        keyColor = 0xFF9B5CFF.toInt(),
    ),
    ThreeKingdoms(
        value = "three_kingdoms",
        labelRes = R.string.pixel_style_three_kingdoms,
        summaryRes = R.string.pixel_style_three_kingdoms_summary,
        keyColor = 0xFF5C694F.toInt(),
    ),
    BianliangMarket(
        value = "bianliang_market",
        labelRes = R.string.pixel_style_bianliang_market,
        summaryRes = R.string.pixel_style_bianliang_market_summary,
        keyColor = 0xFF73558F.toInt(),
    ),
    FishingHarbor(
        value = "fishing_harbor",
        labelRes = R.string.pixel_style_fishing_harbor,
        summaryRes = R.string.pixel_style_fishing_harbor_summary,
        keyColor = 0xFF6957A5.toInt(),
    ),
    TribalJungle(
        value = "tribal_jungle",
        labelRes = R.string.pixel_style_tribal_jungle,
        summaryRes = R.string.pixel_style_tribal_jungle_summary,
        keyColor = 0xFF526D45.toInt(),
    ),
    LavaValley(
        value = "lava_valley",
        labelRes = R.string.pixel_style_lava_valley,
        summaryRes = R.string.pixel_style_lava_valley_summary,
        keyColor = 0xFFB8442C.toInt(),
    ),
    DunhuangDesert(
        value = "dunhuang_desert",
        labelRes = R.string.pixel_style_dunhuang_desert,
        summaryRes = R.string.pixel_style_dunhuang_desert_summary,
        keyColor = 0xFF7B5C8F.toInt(),
    ),
    VikingSnowfield(
        value = "viking_snowfield",
        labelRes = R.string.pixel_style_viking_snowfield,
        summaryRes = R.string.pixel_style_viking_snowfield_summary,
        keyColor = 0xFF4F6F8A.toInt(),
    ),
    JiangnanWatertown(
        value = "jiangnan_watertown",
        labelRes = R.string.pixel_style_jiangnan_watertown,
        summaryRes = R.string.pixel_style_jiangnan_watertown_summary,
        keyColor = 0xFF58756F.toInt(),
    ),
    CloudTown(
        value = "cloud_town",
        labelRes = R.string.pixel_style_cloud_town,
        summaryRes = R.string.pixel_style_cloud_town_summary,
        keyColor = 0xFF6C7E99.toInt(),
    );

    companion object {
        const val DEFAULT_VALUE = "classic_handheld"

        fun fromValue(value: String?): PixelStyle = when (value) {
            LEGACY_FOREST_QUEST_VALUE -> PastoralFields
            else -> entries.firstOrNull { it.value == value } ?: ClassicHandheld
        }

        fun fromIndex(index: Int): PixelStyle = entries.getOrElse(index) { ClassicHandheld }

        fun selectedIndex(value: String?): Int = entries.indexOf(fromValue(value))

        private const val LEGACY_FOREST_QUEST_VALUE = "forest_quest"
    }
}

val LocalPixelStyle = staticCompositionLocalOf { PixelStyle.ClassicHandheld }

@Immutable
data class PixelPalette(
    val background: Color,
    val backgroundAlt: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val primary: Color,
    val secondary: Color,
    val outline: Color,
    val highlight: Color,
    val shadow: Color,
)

fun pixelPalette(style: PixelStyle, dark: Boolean): PixelPalette = when (style) {
    PixelStyle.ClassicHandheld -> if (dark) {
        PixelPalette(
            background = Color(0xFF121712),
            backgroundAlt = Color(0xFF1D251B),
            surface = Color(0xFF293126),
            surfaceAlt = Color(0xFF20271E),
            primary = Color(0xFFB6D17C),
            secondary = Color(0xFFE08A63),
            outline = Color(0xFF68765B),
            highlight = Color(0xFFF1F4E3),
            shadow = Color(0xFF080B08),
        )
    } else {
        PixelPalette(
            background = Color(0xFFE9EBDD),
            backgroundAlt = Color(0xFFD2D8BC),
            surface = Color(0xFFF8F8ED),
            surfaceAlt = Color(0xFFE1E5D0),
            primary = Color(0xFF556B3F),
            secondary = Color(0xFFA35736),
            outline = Color(0xFF707661),
            highlight = Color(0xFFFFFFFF),
            shadow = Color(0xFF30372C),
        )
    }

    PixelStyle.NeonArcade -> if (dark) {
        PixelPalette(
            background = Color(0xFF090A12),
            backgroundAlt = Color(0xFF121022),
            surface = Color(0xFF1B172A),
            surfaceAlt = Color(0xFF12101D),
            primary = Color(0xFF39E2D0),
            secondary = Color(0xFFFF4F9A),
            outline = Color(0xFF5E527E),
            highlight = Color(0xFFF8F1FF),
            shadow = Color(0xFF05050B),
        )
    } else {
        PixelPalette(
            background = Color(0xFFF1F2F8),
            backgroundAlt = Color(0xFFE2E4F1),
            surface = Color(0xFFFAF9FE),
            surfaceAlt = Color(0xFFECE9F6),
            primary = Color(0xFF007F7B),
            secondary = Color(0xFFB61E5C),
            outline = Color(0xFF6F6588),
            highlight = Color(0xFFFFFFFF),
            shadow = Color(0xFF2B263B),
        )
    }

    PixelStyle.PastoralFields -> if (dark) {
        PixelPalette(
            background = Color(0xFF151713),
            backgroundAlt = Color(0xFF22271D),
            surface = Color(0xFF2B3025),
            surfaceAlt = Color(0xFF22261E),
            primary = Color(0xFFA8BF86),
            secondary = Color(0xFFD0A06C),
            outline = Color(0xFF6D715C),
            highlight = Color(0xFFF1ECDD),
            shadow = Color(0xFF090A08),
        )
    } else {
        PixelPalette(
            background = Color(0xFFECE9DE),
            backgroundAlt = Color(0xFFD9E1D0),
            surface = Color(0xFFF8F5EC),
            surfaceAlt = Color(0xFFE5E5D6),
            primary = Color(0xFF5E7650),
            secondary = Color(0xFF94633D),
            outline = Color(0xFF78776A),
            highlight = Color(0xFFFFFFFF),
            shadow = Color(0xFF38372F),
        )
    }

    PixelStyle.StarVoyage -> if (dark) {
        PixelPalette(
            background = Color(0xFF080D1B),
            backgroundAlt = Color(0xFF111A32),
            surface = Color(0xFF18243E),
            surfaceAlt = Color(0xFF111A30),
            primary = Color(0xFF8FA9FF),
            secondary = Color(0xFFF0C86D),
            outline = Color(0xFF536896),
            highlight = Color(0xFFF3F5FF),
            shadow = Color(0xFF03060F),
        )
    } else {
        PixelPalette(
            background = Color(0xFFEEF1F8),
            backgroundAlt = Color(0xFFDCE4F3),
            surface = Color(0xFFFAFBFE),
            surfaceAlt = Color(0xFFE8EDF8),
            primary = Color(0xFF526AC7),
            secondary = Color(0xFF95681D),
            outline = Color(0xFF65739A),
            highlight = Color(0xFFFFFFFF),
            shadow = Color(0xFF2B3558),
        )
    }

    PixelStyle.InkJade -> if (dark) {
        PixelPalette(
            background = Color(0xFF0E1412),
            backgroundAlt = Color(0xFF17201C),
            surface = Color(0xFF1F2A25),
            surfaceAlt = Color(0xFF17201C),
            primary = Color(0xFF88BCA4),
            secondary = Color(0xFFD1AA5E),
            outline = Color(0xFF4F645A),
            highlight = Color(0xFFF0F4EF),
            shadow = Color(0xFF050807),
        )
    } else {
        PixelPalette(
            background = Color(0xFFE7EBE7),
            backgroundAlt = Color(0xFFD2DBD5),
            surface = Color(0xFFF5F7F4),
            surfaceAlt = Color(0xFFDEE6E1),
            primary = Color(0xFF3D705E),
            secondary = Color(0xFF8A682E),
            outline = Color(0xFF68776F),
            highlight = Color(0xFFFFFFFF),
            shadow = Color(0xFF2C3732),
        )
    }

    PixelStyle.RustWasteland -> if (dark) {
        PixelPalette(
            background = Color(0xFF11110F),
            backgroundAlt = Color(0xFF1B1A17),
            surface = Color(0xFF26241F),
            surfaceAlt = Color(0xFF1C1F1B),
            primary = Color(0xFFB06F46),
            secondary = Color(0xFF65877D),
            outline = Color(0xFF5E574D),
            highlight = Color(0xFFD8D1C4),
            shadow = Color(0xFF070706),
        )
    } else {
        PixelPalette(
            background = Color(0xFFD9D6CF),
            backgroundAlt = Color(0xFFC4BFB4),
            surface = Color(0xFFE8E5DD),
            surfaceAlt = Color(0xFFCEC9BE),
            primary = Color(0xFF825038),
            secondary = Color(0xFF42695F),
            outline = Color(0xFF736B60),
            highlight = Color(0xFFF5F1E9),
            shadow = Color(0xFF302C27),
        )
    }

    PixelStyle.OceanDepths -> if (dark) {
        PixelPalette(
            background = Color(0xFF07161C),
            backgroundAlt = Color(0xFF0C2630),
            surface = Color(0xFF123541),
            surfaceAlt = Color(0xFF0E2933),
            primary = Color(0xFF69D5D3),
            secondary = Color(0xFFFF9B72),
            outline = Color(0xFF477985),
            highlight = Color(0xFFE8FFFF),
            shadow = Color(0xFF02090D),
        )
    } else {
        PixelPalette(
            background = Color(0xFFE4F3F3),
            backgroundAlt = Color(0xFFCBE5E3),
            surface = Color(0xFFF5FCFA),
            surfaceAlt = Color(0xFFD9EFEB),
            primary = Color(0xFF167A8A),
            secondary = Color(0xFFB95742),
            outline = Color(0xFF5F858A),
            highlight = Color(0xFFFFFFFF),
            shadow = Color(0xFF1E4249),
        )
    }

    PixelStyle.CyberHacker -> PixelPalette(
        background = Color(0xFF020204),
        backgroundAlt = Color(0xFF08090D),
        surface = Color(0xFF101017),
        surfaceAlt = Color(0xFF08090F),
        primary = Color(0xFFB56CFF),
        secondary = Color(0xFF39FF88),
        outline = Color(0xFF63D8FF),
        highlight = Color(0xFFEAFBFF),
        shadow = Color(0xFF000000),
    )

    PixelStyle.ThreeKingdoms -> if (dark) {
        PixelPalette(
            background = Color(0xFF11130F),
            backgroundAlt = Color(0xFF1B1D16),
            surface = Color(0xFF28271E),
            surfaceAlt = Color(0xFF1E2018),
            primary = Color(0xFF9AAF91),
            secondary = Color(0xFFC27A4A),
            outline = Color(0xFF806B4E),
            highlight = Color(0xFFF1E5CB),
            shadow = Color(0xFF070806),
        )
    } else {
        PixelPalette(
            background = Color(0xFFE2DDCF),
            backgroundAlt = Color(0xFFC9C3B2),
            surface = Color(0xFFF3EFE4),
            surfaceAlt = Color(0xFFD9D2C0),
            primary = Color(0xFF3F604E),
            secondary = Color(0xFF984E34),
            outline = Color(0xFF766044),
            highlight = Color(0xFFFFFBF0),
            shadow = Color(0xFF29251D),
        )
    }

    PixelStyle.BianliangMarket -> if (dark) {
        PixelPalette(
            background = Color(0xFF100E17),
            backgroundAlt = Color(0xFF1B1726),
            surface = Color(0xFF282036),
            surfaceAlt = Color(0xFF1D1829),
            primary = Color(0xFFC0A2D7),
            secondary = Color(0xFFD49B67),
            outline = Color(0xFF747586),
            highlight = Color(0xFFF4E9FF),
            shadow = Color(0xFF050408),
        )
    } else {
        PixelPalette(
            background = Color(0xFFE5E0D7),
            backgroundAlt = Color(0xFFCED2CF),
            surface = Color(0xFFF6F1E8),
            surfaceAlt = Color(0xFFDED8D0),
            primary = Color(0xFF674D79),
            secondary = Color(0xFF896746),
            outline = Color(0xFF697477),
            highlight = Color(0xFFFFFDF7),
            shadow = Color(0xFF2C2931),
        )
    }

    PixelStyle.FishingHarbor -> if (dark) {
        PixelPalette(
            background = Color(0xFF0D1018),
            backgroundAlt = Color(0xFF191629),
            surface = Color(0xFF242033),
            surfaceAlt = Color(0xFF181C29),
            primary = Color(0xFFC09AE3),
            secondary = Color(0xFFD49A6A),
            outline = Color(0xFF66808D),
            highlight = Color(0xFFF5EEFF),
            shadow = Color(0xFF06070B),
        )
    } else {
        PixelPalette(
            background = Color(0xFFE8F0F2),
            backgroundAlt = Color(0xFFCCDCE0),
            surface = Color(0xFFF7FAF9),
            surfaceAlt = Color(0xFFDEE8E7),
            primary = Color(0xFF69578F),
            secondary = Color(0xFF876044),
            outline = Color(0xFF61767A),
            highlight = Color(0xFFFFFFFF),
            shadow = Color(0xFF283538),
        )
    }

    PixelStyle.TribalJungle -> if (dark) {
        PixelPalette(
            background = Color(0xFF0B1410),
            backgroundAlt = Color(0xFF14241A),
            surface = Color(0xFF1D2C21),
            surfaceAlt = Color(0xFF152219),
            primary = Color(0xFF93B36F),
            secondary = Color(0xFFA879C4),
            outline = Color(0xFF817052),
            highlight = Color(0xFFECE5D2),
            shadow = Color(0xFF050906),
        )
    } else {
        PixelPalette(
            background = Color(0xFFDCE5D8),
            backgroundAlt = Color(0xFFC3D0BC),
            surface = Color(0xFFEFF3EA),
            surfaceAlt = Color(0xFFD4DDCF),
            primary = Color(0xFF496A46),
            secondary = Color(0xFF76518E),
            outline = Color(0xFF70644F),
            highlight = Color(0xFFFBF7EA),
            shadow = Color(0xFF2D342B),
        )
    }

    PixelStyle.LavaValley -> if (dark) {
        PixelPalette(
            background = Color(0xFF0B080A),
            backgroundAlt = Color(0xFF1B1015),
            surface = Color(0xFF25171B),
            surfaceAlt = Color(0xFF170F13),
            primary = Color(0xFFFF7040),
            secondary = Color(0xFFAA78E8),
            outline = Color(0xFF72584F),
            highlight = Color(0xFFFFE5C9),
            shadow = Color(0xFF020102),
        )
    } else {
        PixelPalette(
            background = Color(0xFFE3DDD8),
            backgroundAlt = Color(0xFFCCC1BC),
            surface = Color(0xFFF1ECE8),
            surfaceAlt = Color(0xFFD7CCC7),
            primary = Color(0xFFB83A24),
            secondary = Color(0xFF6E45A0),
            outline = Color(0xFF66534D),
            highlight = Color(0xFFFFF7EC),
            shadow = Color(0xFF2F2523),
        )
    }

    PixelStyle.DunhuangDesert -> if (dark) {
        PixelPalette(
            background = Color(0xFF171018),
            backgroundAlt = Color(0xFF251A2B),
            surface = Color(0xFF33263A),
            surfaceAlt = Color(0xFF241D2B),
            primary = Color(0xFFD1A85C),
            secondary = Color(0xFFB08AC2),
            outline = Color(0xFF87705D),
            highlight = Color(0xFFF2E6D1),
            shadow = Color(0xFF080608),
        )
    } else {
        PixelPalette(
            background = Color(0xFFE4DDD1),
            backgroundAlt = Color(0xFFCFC3AE),
            surface = Color(0xFFF0EAE1),
            surfaceAlt = Color(0xFFD9CEC0),
            primary = Color(0xFF8A6529),
            secondary = Color(0xFF6B527E),
            outline = Color(0xFF776653),
            highlight = Color(0xFFFFFAF2),
            shadow = Color(0xFF342C27),
        )
    }

    PixelStyle.VikingSnowfield -> if (dark) {
        PixelPalette(
            background = Color(0xFF071018),
            backgroundAlt = Color(0xFF101D29),
            surface = Color(0xFF172735),
            surfaceAlt = Color(0xFF101E2A),
            primary = Color(0xFF80BBD2),
            secondary = Color(0xFFB496D8),
            outline = Color(0xFF557286),
            highlight = Color(0xFFEFFBFF),
            shadow = Color(0xFF02070B),
        )
    } else {
        PixelPalette(
            background = Color(0xFFDDE8EF),
            backgroundAlt = Color(0xFFC5D6E1),
            surface = Color(0xFFF2F7F9),
            surfaceAlt = Color(0xFFD7E3EA),
            primary = Color(0xFF365F78),
            secondary = Color(0xFF71578F),
            outline = Color(0xFF657884),
            highlight = Color(0xFFFFFFFF),
            shadow = Color(0xFF25323A),
        )
    }

    PixelStyle.JiangnanWatertown -> if (dark) {
        PixelPalette(
            background = Color(0xFF0D1217),
            backgroundAlt = Color(0xFF171B25),
            surface = Color(0xFF222431),
            surfaceAlt = Color(0xFF191D27),
            primary = Color(0xFF9ABDB6),
            secondary = Color(0xFFB899CB),
            outline = Color(0xFF65727A),
            highlight = Color(0xFFF3EEF8),
            shadow = Color(0xFF050609),
        )
    } else {
        PixelPalette(
            background = Color(0xFFE6ECEA),
            backgroundAlt = Color(0xFFCCD9D8),
            surface = Color(0xFFF5F7F3),
            surfaceAlt = Color(0xFFDDE5E2),
            primary = Color(0xFF426D68),
            secondary = Color(0xFF836694),
            outline = Color(0xFF6C7775),
            highlight = Color(0xFFFFFFFF),
            shadow = Color(0xFF2E3837),
        )
    }

    PixelStyle.CloudTown -> if (dark) {
        PixelPalette(
            background = Color(0xFF171725),
            backgroundAlt = Color(0xFF24243A),
            surface = Color(0xFF303047),
            surfaceAlt = Color(0xFF27283C),
            primary = Color(0xFFA9D3E4),
            secondary = Color(0xFFC0A0DF),
            outline = Color(0xFF7E829F),
            highlight = Color(0xFFFFF7FF),
            shadow = Color(0xFF0E0E17),
        )
    } else {
        PixelPalette(
            background = Color(0xFFDCECF4),
            backgroundAlt = Color(0xFFCBDDEB),
            surface = Color(0xFFF7F8FB),
            surfaceAlt = Color(0xFFE4E8F2),
            primary = Color(0xFF5F8DA6),
            secondary = Color(0xFF8B6FB2),
            outline = Color(0xFF8596A3),
            highlight = Color(0xFFFFFFFF),
            shadow = Color(0xFF5E6973),
        )
    }
}
