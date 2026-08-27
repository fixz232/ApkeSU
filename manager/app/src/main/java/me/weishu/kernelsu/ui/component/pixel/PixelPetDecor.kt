package me.weishu.kernelsu.ui.component.pixel

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp

internal fun DrawScope.drawPetCompanionScene(
    palette: PixelPalette,
    habitat: PixelPetHabitat,
) {
    val unit = 5.dp.toPx()
    val ground = size.height * 0.70f
    drawRect(
        color = palette.backgroundAlt.copy(alpha = 0.58f),
        topLeft = Offset(0f, ground),
        size = Size(size.width, size.height - ground),
    )
    when (habitat) {
        PixelPetHabitat.Garden -> {
            repeat(7) { index ->
                val x = size.width * (0.08f + index * 0.14f)
                drawRect(
                    color = palette.primary.copy(alpha = 0.16f),
                    topLeft = Offset(x, ground - unit * (1.5f + index % 3)),
                    size = Size(unit * 0.65f, unit * (1.5f + index % 3)),
                )
                drawRect(
                    color = palette.secondary.copy(alpha = 0.15f),
                    topLeft = Offset(x - unit, ground - unit * (2.2f + index % 2)),
                    size = Size(unit * 2.6f, unit * 0.55f),
                )
            }
        }

        PixelPetHabitat.Cloud -> {
            val cloud = palette.highlight.copy(alpha = 0.15f)
            drawRect(cloud, Offset(0f, ground), Size(size.width, unit * 3.4f))
            drawRect(cloud, Offset(size.width * 0.10f, ground - unit * 1.2f), Size(size.width * 0.28f, unit * 1.4f))
            drawRect(cloud, Offset(size.width * 0.58f, ground - unit * 1.8f), Size(size.width * 0.26f, unit * 1.7f))
        }

        PixelPetHabitat.Moon -> {
            drawRect(palette.shadow.copy(alpha = 0.14f), Offset.Zero, Size(size.width, ground))
            drawCircle(
                color = palette.secondary.copy(alpha = 0.26f),
                radius = unit * 3.2f,
                center = Offset(size.width * 0.83f, size.height * 0.18f),
            )
            repeat(5) { index ->
                drawRect(
                    color = palette.highlight.copy(alpha = 0.32f),
                    topLeft = Offset(size.width * (0.08f + index * 0.16f), size.height * (0.18f + index % 2 * 0.17f)),
                    size = Size(unit * 0.55f, unit * 0.55f),
                )
            }
        }

        PixelPetHabitat.Lagoon -> {
            drawRect(palette.primary.copy(alpha = 0.10f), Offset.Zero, Size(size.width, size.height))
            repeat(4) { index ->
                val y = ground - unit * (1f + index * 1.15f)
                drawLine(
                    color = palette.highlight.copy(alpha = 0.22f),
                    start = Offset(size.width * (index % 2 * 0.08f), y),
                    end = Offset(size.width * (0.86f + index % 2 * 0.12f), y),
                    strokeWidth = unit * 0.34f,
                )
            }
            drawCircle(
                color = palette.secondary.copy(alpha = 0.22f),
                radius = unit * 1.1f,
                center = Offset(size.width * 0.18f, size.height * 0.30f),
            )
        }
    }
}

internal fun DrawScope.drawPetCompanionCardMaterial(
    palette: PixelPalette,
    line: Float,
    habitat: PixelPetHabitat,
) {
    when (habitat) {
        PixelPetHabitat.Garden -> {
            drawRect(palette.primary.copy(alpha = 0.13f), Offset.Zero, Size(size.width, line * 3f))
            repeat(5) { index ->
                val x = size.width * (0.10f + index * 0.19f)
                drawRect(palette.primary.copy(alpha = 0.15f), Offset(x, size.height - line * 3f), Size(line, line * 2f))
            }
        }

        PixelPetHabitat.Cloud -> {
            drawRect(palette.highlight.copy(alpha = 0.12f), Offset.Zero, Size(size.width, line * 3.5f))
            drawRect(palette.highlight.copy(alpha = 0.10f), Offset(size.width * 0.60f, size.height - line * 4f), Size(size.width * 0.30f, line * 3f))
        }

        PixelPetHabitat.Moon -> {
            drawRect(palette.shadow.copy(alpha = 0.18f), Offset.Zero, Size(size.width, line * 3f))
            drawCircle(palette.secondary.copy(alpha = 0.20f), radius = line * 2f, center = Offset(size.width - line * 4f, line * 4f))
        }

        PixelPetHabitat.Lagoon -> {
            repeat(3) { index ->
                val y = size.height - line * (2f + index * 1.5f)
                drawLine(palette.primary.copy(alpha = 0.17f), Offset(0f, y), Offset(size.width, y), line * 0.42f)
            }
        }
    }
    drawRect(
        palette.secondary.copy(alpha = 0.10f),
        Offset(size.width * 0.12f, size.height - line * 3f),
        Size(size.width * 0.28f, line * 2f),
    )
    drawRect(
        palette.highlight.copy(alpha = 0.09f),
        Offset(size.width * 0.76f, line * 1.2f),
        Size(line * 1.4f, line * 1.4f),
    )
}
