package me.weishu.kernelsu.ui.component

import android.animation.ValueAnimator
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.InterfaceStyle
import me.weishu.kernelsu.ui.component.pixel.PixelStyle
import me.weishu.kernelsu.ui.component.rain.RainStyle
import me.weishu.kernelsu.ui.component.snow.SeasonStyle
import me.weishu.kernelsu.ui.component.ink.InkStyle
import kotlin.math.abs
import kotlin.math.round

const val PAGE_TRANSITION_EFFECT_KEY = "page_transition_effect"

enum class PageTransitionEffect(
    val value: String,
    @StringRes val labelRes: Int,
) {
    Off("off", R.string.settings_page_transition_effect_off),
    Depth("depth", R.string.settings_page_transition_effect_depth),
    Liquid("liquid", R.string.settings_page_transition_effect_liquid),
    Stack("stack", R.string.settings_page_transition_effect_stack),
    Glass("glass", R.string.settings_page_transition_effect_glass),
    StyleLinked("style_linked", R.string.settings_page_transition_effect_style_linked);

    companion object {
        val Default = Depth
        const val DEFAULT_VALUE = "depth"

        fun fromValue(value: String?): PageTransitionEffect {
            return entries.firstOrNull { it.value == value } ?: Default
        }

        fun fromIndex(index: Int): PageTransitionEffect {
            return entries.getOrElse(index) { Default }
        }

        fun selectedIndex(value: String?): Int {
            return entries.indexOf(fromValue(value)).coerceAtLeast(0)
        }
    }
}

val LocalPageTransitionEffect = staticCompositionLocalOf { PageTransitionEffect.Default }

@Composable
fun rememberSystemAnimationsEnabled(): Boolean {
    val context = LocalContext.current
    var animationsEnabled by remember { mutableStateOf(ValueAnimator.areAnimatorsEnabled()) }

    DisposableEffect(context) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                animationsEnabled = ValueAnimator.areAnimatorsEnabled()
            }
        }
        val registered = runCatching {
            context.contentResolver.registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
                false,
                observer,
            )
            true
        }.getOrDefault(false)

        onDispose {
            if (registered) {
                runCatching { context.contentResolver.unregisterContentObserver(observer) }
            }
        }
    }

    LifecycleResumeEffect(context) {
        animationsEnabled = ValueAnimator.areAnimatorsEnabled()
        onPauseOrDispose { }
    }

    return animationsEnabled
}

internal enum class PageTransitionVisual {
    Off,
    Depth,
    Liquid,
    Stack,
    Glass,
    Pixel,
    Season,
    Rain,
    Ink,
}

internal data class PageTransitionTransform(
    val translationXFraction: Float = 0f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val alpha: Float = 1f,
    val rotationY: Float = 0f,
    val shadowElevationDp: Float = 0f,
)

internal fun resolvePageTransitionVisual(
    effect: PageTransitionEffect,
    interfaceStyle: String,
): PageTransitionVisual {
    return when (effect) {
        PageTransitionEffect.Off -> PageTransitionVisual.Off
        PageTransitionEffect.Depth -> PageTransitionVisual.Depth
        PageTransitionEffect.Liquid -> PageTransitionVisual.Liquid
        PageTransitionEffect.Stack -> PageTransitionVisual.Stack
        PageTransitionEffect.Glass -> PageTransitionVisual.Glass
        PageTransitionEffect.StyleLinked -> when (InterfaceStyle.normalizeValue(interfaceStyle)) {
            InterfaceStyle.Pixel.value -> PageTransitionVisual.Pixel
            InterfaceStyle.Snow.value -> PageTransitionVisual.Season
            InterfaceStyle.Rain.value -> PageTransitionVisual.Rain
            InterfaceStyle.Ink.value -> PageTransitionVisual.Ink
            InterfaceStyle.LiquidGlass.value -> PageTransitionVisual.Glass
            else -> PageTransitionVisual.Depth
        }
    }
}

internal fun resolvePageTransitionTransform(
    visual: PageTransitionVisual,
    pageOffset: Float,
    animationsEnabled: Boolean,
): PageTransitionTransform {
    if (!animationsEnabled || visual == PageTransitionVisual.Off) return PageTransitionTransform()

    val offset = pageOffset.coerceIn(-1f, 1f)
    val distance = abs(offset)
    return when (visual) {
        PageTransitionVisual.Off -> PageTransitionTransform()
        PageTransitionVisual.Depth,
        PageTransitionVisual.Season,
        PageTransitionVisual.Rain,
        PageTransitionVisual.Ink -> PageTransitionTransform(
            translationXFraction = -0.052f * offset,
            scaleX = 1f - 0.018f * distance,
            scaleY = 1f - 0.018f * distance,
            alpha = 1f - 0.12f * distance,
            rotationY = -1.4f * offset,
        )
        PageTransitionVisual.Liquid -> PageTransitionTransform(
            translationXFraction = -0.038f * offset,
            scaleX = 1f + 0.018f * distance,
            scaleY = 1f - 0.007f * distance,
            alpha = 1f - 0.06f * distance,
        )
        PageTransitionVisual.Stack -> PageTransitionTransform(
            translationXFraction = -0.105f * offset,
            scaleX = 1f - 0.04f * distance,
            scaleY = 1f - 0.04f * distance,
            alpha = 1f - 0.14f * distance,
            rotationY = -2.2f * offset,
            shadowElevationDp = 8f * distance,
        )
        PageTransitionVisual.Glass -> PageTransitionTransform(
            translationXFraction = -0.06f * offset,
            scaleX = 1f - 0.022f * distance,
            scaleY = 1f - 0.022f * distance,
            alpha = 1f - 0.1f * distance,
            rotationY = -2.8f * offset,
            shadowElevationDp = 3f * distance,
        )
        PageTransitionVisual.Pixel -> {
            val steppedOffset = round(offset * PIXEL_MOTION_STEPS) / PIXEL_MOTION_STEPS
            PageTransitionTransform(
                translationXFraction = -0.045f * steppedOffset,
                scaleX = 1f - 0.014f * distance,
                scaleY = 1f - 0.014f * distance,
                alpha = 1f - 0.09f * distance,
            )
        }
    }
}

internal fun resolveMainPageTransitionTransform(
    visual: PageTransitionVisual,
    interfaceStyle: String,
    pageOffset: Float,
    animationsEnabled: Boolean,
): PageTransitionTransform {
    val transform = resolvePageTransitionTransform(
        visual = visual,
        pageOffset = pageOffset,
        animationsEnabled = animationsEnabled,
    )
    if (InterfaceStyle.normalizeValue(interfaceStyle) != InterfaceStyle.Material.value) {
        return transform
    }

    // HorizontalPager already provides Material's directional motion. Transforming the
    // whole translucent page creates an offscreen layer that can expose a black backing
    // surface while two pages are visible during navigation.
    return PageTransitionTransform()
}

@Composable
fun Modifier.mainPageTransition(
    effect: PageTransitionEffect,
    interfaceStyle: String,
    seasonStyle: SeasonStyle,
    rainStyle: RainStyle,
    inkStyle: InkStyle,
    pixelStyle: PixelStyle,
    animationsEnabled: Boolean,
    pageOffset: () -> Float,
): Modifier {
    val visual = resolvePageTransitionVisual(effect, interfaceStyle)
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val accent = remember(visual, seasonStyle, rainStyle, inkStyle, pixelStyle, primary) {
        when (visual) {
            PageTransitionVisual.Season -> Color(seasonStyle.keyColor)
            PageTransitionVisual.Rain -> Color(rainStyle.keyColor)
            PageTransitionVisual.Ink -> Color(inkStyle.keyColor)
            PageTransitionVisual.Pixel -> Color(pixelStyle.keyColor)
            else -> primary
        }
    }

    if (!animationsEnabled || visual == PageTransitionVisual.Off) return this

    return this
        .graphicsLayer {
            val offset = pageOffset()
            val transform = resolveMainPageTransitionTransform(
                visual = visual,
                interfaceStyle = interfaceStyle,
                pageOffset = offset,
                animationsEnabled = animationsEnabled,
            )
            translationX = size.width * transform.translationXFraction
            scaleX = transform.scaleX
            scaleY = transform.scaleY
            alpha = transform.alpha
            rotationY = transform.rotationY
            shadowElevation = transform.shadowElevationDp.dp.toPx()
            cameraDistance = 18f * density
            transformOrigin = TransformOrigin(
                pivotFractionX = if (offset >= 0f) 0.92f else 0.08f,
                pivotFractionY = 0.5f,
            )
        }
        .drawWithContent {
            drawContent()
            val offset = pageOffset().coerceIn(-1f, 1f)
            val progress = abs(offset)
            if (progress <= MIN_VISIBLE_PROGRESS) return@drawWithContent

            when (visual) {
                PageTransitionVisual.Glass -> drawGlassPageEdge(offset, progress, accent, secondary)
                PageTransitionVisual.Pixel -> drawPixelPageEdge(offset, progress, accent, secondary)
                PageTransitionVisual.Season -> drawSeasonPageEdge(offset, progress, accent, secondary)
                PageTransitionVisual.Rain -> drawRainPageEdge(offset, progress, accent, secondary)
                PageTransitionVisual.Ink -> drawInkPageEdge(offset, progress, accent, secondary)
                else -> Unit
            }
        }
}

private fun DrawScope.drawGlassPageEdge(
    offset: Float,
    progress: Float,
    primary: Color,
    secondary: Color,
) {
    val widthPx = (10.dp.toPx() + 18.dp.toPx() * progress).coerceAtMost(size.width * 0.12f)
    val edgeX = transitionEdgeX(offset, widthPx)
    val colors = if (offset >= 0f) {
        listOf(
            Color.White.copy(alpha = 0.2f * progress),
            primary.copy(alpha = 0.16f * progress),
            secondary.copy(alpha = 0.08f * progress),
            Color.Transparent,
        )
    } else {
        listOf(
            Color.Transparent,
            secondary.copy(alpha = 0.08f * progress),
            primary.copy(alpha = 0.16f * progress),
            Color.White.copy(alpha = 0.2f * progress),
        )
    }
    drawRect(
        brush = Brush.horizontalGradient(colors),
        topLeft = Offset(edgeX, 0f),
        size = Size(widthPx, size.height),
    )
    val seamX = if (offset >= 0f) edgeX else edgeX + widthPx
    drawLine(
        color = Color.White.copy(alpha = 0.34f * progress),
        start = Offset(seamX, size.height * 0.08f),
        end = Offset(seamX, size.height * 0.92f),
        strokeWidth = 0.75.dp.toPx(),
    )
}

private fun DrawScope.drawPixelPageEdge(
    offset: Float,
    progress: Float,
    primary: Color,
    secondary: Color,
) {
    val direction = if (offset >= 0f) 1f else -1f
    val edge = if (offset >= 0f) 0f else size.width
    val pixel = 1.dp.toPx().coerceAtLeast(1f)
    repeat(9) { index ->
        val y = size.height * (0.08f + index * 0.105f)
        val length = (8 + (index % 4) * 5).dp.toPx() * progress
        drawRect(
            color = if (index % 3 == 0) secondary.copy(alpha = 0.46f * progress)
            else primary.copy(alpha = 0.38f * progress),
            topLeft = Offset(if (direction > 0f) edge else edge - length, y),
            size = Size(length, pixel),
        )
    }
    val scanX = edge + direction * 5.dp.toPx() * progress
    drawLine(
        color = primary.copy(alpha = 0.5f * progress),
        start = Offset(scanX, size.height * 0.04f),
        end = Offset(scanX, size.height * 0.96f),
        strokeWidth = pixel,
    )
}

private fun DrawScope.drawSeasonPageEdge(
    offset: Float,
    progress: Float,
    primary: Color,
    secondary: Color,
) {
    val direction = if (offset >= 0f) 1f else -1f
    val edge = if (offset >= 0f) 0f else size.width
    repeat(7) { index ->
        val drift = (5 + index % 3 * 4).dp.toPx() * progress
        val x = edge + direction * drift
        val y = size.height * (0.12f + index * 0.12f) + (index % 2) * 4.dp.toPx() * progress
        drawCircle(
            color = if (index % 3 == 0) secondary.copy(alpha = 0.34f * progress)
            else primary.copy(alpha = 0.4f * progress),
            radius = (1.2f + index % 2 * 0.7f).dp.toPx(),
            center = Offset(x, y),
        )
    }
}

private fun DrawScope.drawRainPageEdge(
    offset: Float,
    progress: Float,
    primary: Color,
    secondary: Color,
) {
    val direction = if (offset >= 0f) 1f else -1f
    val edge = if (offset >= 0f) 0f else size.width
    repeat(6) { index ->
        val x = edge + direction * (4 + index * 3).dp.toPx() * progress
        val y = size.height * (0.09f + index * 0.13f)
        drawLine(
            color = if (index % 2 == 0) primary.copy(alpha = 0.36f * progress)
            else secondary.copy(alpha = 0.28f * progress),
            start = Offset(x, y),
            end = Offset(x - direction * 1.8.dp.toPx(), y + 10.dp.toPx()),
            strokeWidth = 0.85.dp.toPx(),
        )
    }
    val radius = 15.dp.toPx() + 9.dp.toPx() * progress
    val centerX = edge + direction * 12.dp.toPx()
    drawOval(
        color = primary.copy(alpha = 0.24f * progress),
        topLeft = Offset(centerX - radius, size.height - 22.dp.toPx()),
        size = Size(radius * 2f, 7.dp.toPx()),
        style = Stroke(width = 0.9.dp.toPx()),
    )
}

private fun DrawScope.drawInkPageEdge(
    offset: Float,
    progress: Float,
    primary: Color,
    secondary: Color,
) {
    val direction = if (offset >= 0f) 1f else -1f
    val edge = if (offset >= 0f) 0f else size.width
    val washWidth = (12.dp.toPx() + 16.dp.toPx() * progress).coerceAtMost(size.width * 0.10f)
    val washX = if (direction > 0f) edge else edge - washWidth
    drawRect(
        brush = Brush.horizontalGradient(
            colors = if (direction > 0f) {
                listOf(primary.copy(alpha = 0.18f * progress), Color.Transparent)
            } else {
                listOf(Color.Transparent, primary.copy(alpha = 0.18f * progress))
            },
            startX = washX,
            endX = washX + washWidth,
        ),
        topLeft = Offset(washX, 0f),
        size = Size(washWidth, size.height),
    )
    repeat(5) { index ->
        val y = size.height * (0.17f + index * 0.145f)
        val reach = (5 + index % 3 * 4).dp.toPx() * progress
        drawLine(
            color = if (index % 2 == 0) {
                primary.copy(alpha = 0.36f * progress)
            } else {
                secondary.copy(alpha = 0.26f * progress)
            },
            start = Offset(edge, y + 4.dp.toPx()),
            end = Offset(edge + direction * reach, y),
            strokeWidth = 0.75.dp.toPx(),
        )
    }
    val waterY = size.height * 0.86f
    drawLine(
        color = primary.copy(alpha = 0.30f * progress),
        start = Offset(edge, waterY),
        end = Offset(edge + direction * 18.dp.toPx() * progress, waterY),
        strokeWidth = 0.7.dp.toPx(),
    )
}

private fun DrawScope.transitionEdgeX(offset: Float, widthPx: Float): Float {
    return if (offset >= 0f) 0f else size.width - widthPx
}

private const val PIXEL_MOTION_STEPS = 12f
private const val MIN_VISIBLE_PROGRESS = 0.015f
