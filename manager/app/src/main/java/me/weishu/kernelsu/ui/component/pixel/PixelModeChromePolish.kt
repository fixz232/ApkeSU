package me.weishu.kernelsu.ui.component.pixel

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

internal fun DrawScope.drawPixelModeHudPolish(
    style: PixelStyle,
    palette: PixelPalette,
    top: Float,
    unit: Float,
) {
    when (style) {
        PixelStyle.ClassicHandheld -> {
            repeat(4) { index ->
                drawRect(
                    color = palette.primary.copy(alpha = 0.34f + index * 0.07f),
                    topLeft = Offset(unit * (5f + index * 1.35f), top + unit * 2f),
                    size = Size(unit * 0.75f, unit * 0.55f),
                )
            }
            drawRect(
                color = palette.secondary.copy(alpha = 0.72f),
                topLeft = Offset(size.width - unit * 9f, top + unit * 2f),
                size = Size(unit, unit),
            )
        }

        PixelStyle.NeonArcade -> {
            repeat(7) { index ->
                val barHeight = unit * (0.45f + index % 4 * 0.38f)
                drawRect(
                    color = (if (index % 2 == 0) palette.primary else palette.secondary)
                        .copy(alpha = 0.72f),
                    topLeft = Offset(
                        size.width / 2f + unit * (index - 3.5f) * 1.25f,
                        top + unit * 2.6f - barHeight,
                    ),
                    size = Size(unit * 0.65f, barHeight),
                )
            }
        }

        PixelStyle.PastoralFields -> {
            val railY = top + unit * 2.4f
            drawRect(
                color = palette.secondary.copy(alpha = 0.48f),
                topLeft = Offset(size.width * 0.34f, railY),
                size = Size(size.width * 0.32f, unit * 0.45f),
            )
            listOf(0.39f, 0.47f, 0.55f, 0.63f).forEachIndexed { index, x ->
                val bladeHeight = unit * (0.7f + index % 3 * 0.4f)
                drawRect(
                    color = palette.primary.copy(alpha = 0.60f),
                    topLeft = Offset(size.width * x, railY - bladeHeight),
                    size = Size(unit * 0.5f, bladeHeight),
                )
            }
        }

        PixelStyle.StarVoyage -> {
            drawRect(
                color = palette.outline.copy(alpha = 0.54f),
                topLeft = Offset(size.width * 0.31f, top + unit * 2f),
                size = Size(size.width * 0.38f, unit * 0.42f),
            )
            listOf(0.36f, 0.50f, 0.64f).forEachIndexed { index, x ->
                drawRect(
                    color = (if (index == 1) palette.secondary else palette.highlight)
                        .copy(alpha = 0.66f),
                    topLeft = Offset(size.width * x, top + unit * (1.4f - index % 2 * 0.45f)),
                    size = Size(unit * 0.7f, unit * 0.7f),
                )
            }
        }

        PixelStyle.InkJade -> {
            listOf(0.36f to 0.28f, 0.41f to 0.18f, 0.46f to 0.08f).forEachIndexed { index, (x, width) ->
                drawRect(
                    color = palette.primary.copy(alpha = 0.56f + index * 0.08f),
                    topLeft = Offset(size.width * x, top + unit * (1.2f + index * 0.52f)),
                    size = Size(size.width * width, unit * 0.42f),
                )
            }
            drawRect(
                color = palette.secondary.copy(alpha = 0.76f),
                topLeft = Offset(size.width * 0.49f, top - unit * 0.5f),
                size = Size(unit, unit),
            )
        }

        PixelStyle.RustWasteland -> {
            listOf(0.30f to 0.15f, 0.48f to 0.10f, 0.61f to 0.12f).forEachIndexed { index, (x, width) ->
                drawRect(
                    color = (if (index == 1) palette.secondary else palette.primary)
                        .copy(alpha = 0.58f),
                    topLeft = Offset(size.width * x, top + unit * (1.1f + index % 2 * 0.75f)),
                    size = Size(size.width * width, unit * 0.72f),
                )
            }
            listOf(0.29f, 0.74f).forEach { x ->
                drawRect(
                    color = palette.outline.copy(alpha = 0.72f),
                    topLeft = Offset(size.width * x, top + unit),
                    size = Size(unit * 0.7f, unit * 0.7f),
                )
            }
        }

        PixelStyle.OceanDepths,
        PixelStyle.CyberHacker,
        -> Unit

        PixelStyle.ThreeKingdoms -> drawThreeKingdomsModeHudPolish(
            palette = palette,
            top = top,
            unit = unit,
        )

        PixelStyle.BianliangMarket -> drawBianliangModeHudPolish(
            palette = palette,
            top = top,
            unit = unit,
        )

        PixelStyle.FishingHarbor -> drawFishingHarborModeHudPolish(
            palette = palette,
            top = top,
            unit = unit,
        )

        PixelStyle.TribalJungle -> drawTribalJungleModeHudPolish(
            palette = palette,
            top = top,
            unit = unit,
        )

        PixelStyle.LavaValley -> drawLavaValleyModeHudPolish(
            palette = palette,
            top = top,
            unit = unit,
        )

        PixelStyle.DunhuangDesert -> drawDunhuangModeHudPolish(
            palette = palette,
            top = top,
            unit = unit,
        )

        PixelStyle.VikingSnowfield -> drawVikingModeHudPolish(
            palette = palette,
            top = top,
            unit = unit,
        )

        PixelStyle.JiangnanWatertown -> drawJiangnanModeHudPolish(
            palette = palette,
            top = top,
            unit = unit,
        )

        PixelStyle.CloudTown -> drawCloudTownModeHudPolish(
            palette = palette,
            top = top,
            unit = unit,
        )
    }
}

internal fun DrawScope.drawPixelModeNavigationFrame(
    style: PixelStyle,
    palette: PixelPalette,
    unit: Float,
) {
    when (style) {
        PixelStyle.ClassicHandheld -> {
            drawRect(
                color = palette.shadow.copy(alpha = 0.74f),
                topLeft = Offset(size.width * 0.32f, 0f),
                size = Size(size.width * 0.36f, unit * 0.55f),
            )
            repeat(5) { index ->
                drawRect(
                    color = palette.primary.copy(alpha = 0.54f),
                    topLeft = Offset(
                        size.width - unit * (9f - index * 1.25f),
                        size.height - unit * 3f,
                    ),
                    size = Size(unit * 0.55f, unit * 0.55f),
                )
            }
        }

        PixelStyle.NeonArcade -> {
            drawRect(
                color = palette.primary.copy(alpha = 0.86f),
                topLeft = Offset(unit * 5f, 0f),
                size = Size(size.width * 0.35f, unit * 0.55f),
            )
            drawRect(
                color = palette.secondary.copy(alpha = 0.86f),
                topLeft = Offset(size.width * 0.60f, 0f),
                size = Size(size.width * 0.35f - unit * 5f, unit * 0.55f),
            )
            repeat(4) { index ->
                drawRect(
                    color = if (index % 2 == 0) palette.secondary else palette.primary,
                    topLeft = Offset(unit * (3f + index * 1.5f), size.height - unit * 2.6f),
                    size = Size(unit * 0.7f, unit * 0.7f),
                )
            }
        }

        PixelStyle.PastoralFields -> {
            val railY = size.height - unit * 1.2f
            drawRect(
                color = palette.secondary.copy(alpha = 0.66f),
                topLeft = Offset(size.width * 0.16f, railY),
                size = Size(size.width * 0.68f, unit * 0.55f),
            )
            repeat(7) { index ->
                val blade = unit * (0.7f + index % 3 * 0.45f)
                drawRect(
                    color = palette.primary.copy(alpha = 0.64f),
                    topLeft = Offset(size.width * (0.20f + index * 0.10f), railY - blade),
                    size = Size(unit * 0.48f, blade),
                )
            }
        }

        PixelStyle.StarVoyage -> {
            drawRect(
                color = palette.outline.copy(alpha = 0.72f),
                topLeft = Offset(size.width * 0.22f, size.height - unit),
                size = Size(size.width * 0.56f, unit * 0.45f),
            )
            listOf(0.14f, 0.50f, 0.86f).forEachIndexed { index, x ->
                val side = if (index == 1) unit else unit * 0.7f
                drawRect(
                    color = (if (index == 1) palette.secondary else palette.highlight)
                        .copy(alpha = 0.72f),
                    topLeft = Offset(size.width * x - side / 2f, unit * (1f + index % 2)),
                    size = Size(side, side),
                )
            }
        }

        PixelStyle.InkJade -> {
            listOf(0.19f to 0.62f, 0.27f to 0.46f, 0.37f to 0.26f).forEachIndexed { index, (x, width) ->
                drawRect(
                    color = palette.primary.copy(alpha = 0.58f + index * 0.08f),
                    topLeft = Offset(size.width * x, size.height - unit * (1f + index * 0.68f)),
                    size = Size(size.width * width, unit * 0.48f),
                )
            }
            drawRect(
                color = palette.secondary.copy(alpha = 0.84f),
                topLeft = Offset(size.width - unit * 5f, size.height - unit * 4f),
                size = Size(unit * 2f, unit * 2f),
                style = Stroke(width = unit * 0.45f),
            )
        }

        PixelStyle.RustWasteland -> {
            listOf(0.07f to 0.21f, 0.34f to 0.16f, 0.56f to 0.31f).forEachIndexed { index, (x, width) ->
                drawRect(
                    color = (if (index == 1) palette.secondary else palette.primary)
                        .copy(alpha = 0.66f),
                    topLeft = Offset(size.width * x, if (index % 2 == 0) 0f else unit * 0.45f),
                    size = Size(size.width * width, unit * 0.65f),
                )
            }
            listOf(unit * 3f, size.width - unit * 4f).forEach { x ->
                drawRect(
                    color = palette.outline.copy(alpha = 0.82f),
                    topLeft = Offset(x, size.height - unit * 3f),
                    size = Size(unit, unit),
                )
            }
        }

        PixelStyle.OceanDepths,
        PixelStyle.CyberHacker,
        -> Unit

        PixelStyle.ThreeKingdoms -> drawThreeKingdomsModeNavigationFrame(
            palette = palette,
            unit = unit,
        )

        PixelStyle.BianliangMarket -> drawBianliangModeNavigationFrame(
            palette = palette,
            unit = unit,
        )

        PixelStyle.FishingHarbor -> drawFishingHarborModeNavigationFrame(
            palette = palette,
            unit = unit,
        )

        PixelStyle.TribalJungle -> drawTribalJungleModeNavigationFrame(
            palette = palette,
            unit = unit,
        )

        PixelStyle.LavaValley -> drawLavaValleyModeNavigationFrame(
            palette = palette,
            unit = unit,
        )

        PixelStyle.DunhuangDesert -> drawDunhuangModeNavigationFrame(
            palette = palette,
            unit = unit,
        )

        PixelStyle.VikingSnowfield -> drawVikingModeNavigationFrame(
            palette = palette,
            unit = unit,
        )

        PixelStyle.JiangnanWatertown -> drawJiangnanModeNavigationFrame(
            palette = palette,
            unit = unit,
        )

        PixelStyle.CloudTown -> drawCloudTownModeNavigationFrame(
            palette = palette,
            unit = unit,
        )
    }
}

internal fun DrawScope.drawPixelModeIndicatorPolish(
    style: PixelStyle,
    palette: PixelPalette,
    unit: Float,
) {
    when (style) {
        PixelStyle.ClassicHandheld -> repeat(3) { index ->
            drawRect(
                color = palette.primary.copy(alpha = 0.62f),
                topLeft = Offset(size.width - unit * (5f - index), size.height - unit * 2f),
                size = Size(unit * 0.45f, unit * 0.45f),
            )
        }

        PixelStyle.NeonArcade -> {
            drawRect(palette.primary.copy(alpha = 0.86f), Offset(0f, size.height - unit), Size(size.width * 0.42f, unit * 0.5f))
            drawRect(
                palette.secondary.copy(alpha = 0.86f),
                Offset(size.width * 0.58f, size.height - unit),
                Size(size.width * 0.42f, unit * 0.5f),
            )
        }

        PixelStyle.PastoralFields -> {
            drawRect(
                color = palette.secondary.copy(alpha = 0.68f),
                topLeft = Offset(size.width * 0.30f, size.height - unit),
                size = Size(size.width * 0.40f, unit * 0.5f),
            )
        }

        PixelStyle.StarVoyage -> {
            drawRect(
                color = palette.secondary.copy(alpha = 0.82f),
                topLeft = Offset(unit * 2f, size.height - unit * 2f),
                size = Size(unit * 0.7f, unit * 0.7f),
            )
        }

        PixelStyle.InkJade -> {
            drawRect(
                color = palette.secondary.copy(alpha = 0.84f),
                topLeft = Offset(size.width - unit * 4f, size.height - unit * 3f),
                size = Size(unit * 1.5f, unit * 1.5f),
                style = Stroke(width = unit * 0.4f),
            )
        }

        PixelStyle.RustWasteland -> {
            listOf(unit * 2f, size.width - unit * 3f).forEach { x ->
                drawRect(
                    color = palette.secondary.copy(alpha = 0.86f),
                    topLeft = Offset(x, size.height - unit * 2f),
                    size = Size(unit * 0.75f, unit * 0.75f),
                )
            }
        }

        PixelStyle.OceanDepths,
        PixelStyle.CyberHacker,
        -> Unit

        PixelStyle.ThreeKingdoms -> drawThreeKingdomsModeIndicatorPolish(
            palette = palette,
            unit = unit,
        )

        PixelStyle.BianliangMarket -> drawBianliangModeIndicatorPolish(
            palette = palette,
            unit = unit,
        )

        PixelStyle.FishingHarbor -> drawFishingHarborModeIndicatorPolish(
            palette = palette,
            unit = unit,
        )

        PixelStyle.TribalJungle -> drawTribalJungleModeIndicatorPolish(
            palette = palette,
            unit = unit,
        )

        PixelStyle.LavaValley -> drawLavaValleyModeIndicatorPolish(
            palette = palette,
            unit = unit,
        )

        PixelStyle.DunhuangDesert -> drawDunhuangModeIndicatorPolish(
            palette = palette,
            unit = unit,
        )

        PixelStyle.VikingSnowfield -> drawVikingModeIndicatorPolish(
            palette = palette,
            unit = unit,
        )

        PixelStyle.JiangnanWatertown -> drawJiangnanModeIndicatorPolish(
            palette = palette,
            unit = unit,
        )

        PixelStyle.CloudTown -> drawCloudTownModeIndicatorPolish(
            palette = palette,
            unit = unit,
        )
    }
}
