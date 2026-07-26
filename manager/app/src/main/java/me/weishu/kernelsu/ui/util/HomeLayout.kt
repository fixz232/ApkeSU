package me.weishu.kernelsu.ui.util

import android.content.Context
import androidx.compose.runtime.Immutable
import java.util.Locale
import kotlin.math.roundToInt

private const val HOME_LAYOUT_ENABLED_KEY = "home_layout_enabled"
private const val HOME_LAYOUT_RECORDS_KEY = "home_layout_records"
private const val HOME_LAYOUT_AUTO_AVOID_KEY = "home_layout_auto_avoid_overlap"
private const val MIN_HOME_LAYOUT_WIDTH = 0.36f
private const val MAX_HOME_LAYOUT_HEIGHT_ROWS = 4f
private const val MAX_HOME_LAYOUT_Y_ROWS = 6f

enum class HomeLayoutCard(val value: String) {
    Lkm("lkm"),
    Superuser("superuser"),
    Module("module"),
    StatusMonitor("status_monitor"),
    SystemInfo("system_info");

    companion object {
        fun fromValue(value: String): HomeLayoutCard? {
            return entries.firstOrNull { it.value == value }
        }
    }
}

enum class HomeLayoutPreset {
    DualColumn,
    SingleColumn,
    Compact,
}

enum class HomeLayoutResizeEdge {
    Left,
    Top,
    Right,
    Bottom,
}

@Immutable
data class HomeLayoutItem(
    val card: HomeLayoutCard,
    val x: Float,
    val y: Float,
    val width: Float,
    val scale: Float,
    val aspectRatio: Float,
    val height: Float = 0f,
    val visible: Boolean,
    val zIndex: Int,
)

@Immutable
data class HomeLayoutState(
    val enabled: Boolean = false,
    val autoAvoidOverlap: Boolean = true,
    val items: List<HomeLayoutItem> = defaultHomeLayoutItems(),
)

fun readHomeLayoutState(context: Context): HomeLayoutState {
    val prefs = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
    val parsed = prefs.getString(HOME_LAYOUT_RECORDS_KEY, null)
        ?.let(::decodeHomeLayoutItems)
        .orEmpty()
    return HomeLayoutState(
        enabled = prefs.getBoolean(HOME_LAYOUT_ENABLED_KEY, false),
        autoAvoidOverlap = prefs.getBoolean(HOME_LAYOUT_AUTO_AVOID_KEY, true),
        items = mergeHomeLayoutItems(parsed),
    )
}

fun saveHomeLayoutState(context: Context, state: HomeLayoutState): Boolean {
    val sanitized = state.copy(items = mergeHomeLayoutItems(state.items))
    return context.applicationContext
        .getSharedPreferences("settings", Context.MODE_PRIVATE)
        .edit()
        .putBoolean(HOME_LAYOUT_ENABLED_KEY, sanitized.enabled)
        .putBoolean(HOME_LAYOUT_AUTO_AVOID_KEY, sanitized.autoAvoidOverlap)
        .putString(HOME_LAYOUT_RECORDS_KEY, encodeHomeLayoutItems(sanitized.items))
        .commit()
}

fun resetHomeLayoutState(context: Context): Boolean {
    return saveHomeLayoutState(context, HomeLayoutState())
}

fun sanitizeHomeLayoutItem(item: HomeLayoutItem): HomeLayoutItem {
    val fallback = defaultHomeLayoutItems().first { it.card == item.card }
    val baseWidth = item.width.finiteOr(fallback.width).coerceIn(MIN_HOME_LAYOUT_WIDTH, 1f)
    val legacyScale = item.scale.finiteOr(fallback.scale).coerceIn(0.72f, 1.28f)
    val width = (baseWidth * legacyScale).coerceIn(MIN_HOME_LAYOUT_WIDTH, 1f)
    val rawHeight = item.height.finiteOr(fallback.height)
    val height = if (rawHeight <= 0f) {
        0f
    } else {
        rawHeight.coerceIn(minimumHomeLayoutHeight(item.card), MAX_HOME_LAYOUT_HEIGHT_ROWS)
    }
    return item.copy(
        x = item.x.finiteOr(fallback.x).coerceIn(0f, 1f),
        y = item.y.finiteOr(fallback.y).coerceIn(0f, MAX_HOME_LAYOUT_Y_ROWS),
        width = width,
        scale = 1f,
        aspectRatio = if (item.card == HomeLayoutCard.Lkm) {
            item.aspectRatio.finiteOr(fallback.aspectRatio).coerceIn(1f, 2.2f)
        } else {
            0f
        },
        height = height,
        zIndex = item.zIndex.coerceIn(0, HomeLayoutCard.entries.lastIndex),
    )
}

fun minimumHomeLayoutHeight(card: HomeLayoutCard): Float = when (card) {
    HomeLayoutCard.Lkm -> 0.62f
    HomeLayoutCard.Superuser,
    HomeLayoutCard.Module -> 0.56f
    HomeLayoutCard.StatusMonitor -> 0.90f
    HomeLayoutCard.SystemInfo -> 1.75f
}

fun suggestedHomeLayoutHeight(card: HomeLayoutCard): Float = when (card) {
    HomeLayoutCard.Lkm -> 1.16f
    HomeLayoutCard.Superuser,
    HomeLayoutCard.Module -> 0.62f
    HomeLayoutCard.StatusMonitor -> 1f
    HomeLayoutCard.SystemInfo -> 2.16f
}

fun resizeHomeLayoutItem(
    item: HomeLayoutItem,
    edge: HomeLayoutResizeEdge,
    horizontalDelta: Float,
    verticalDeltaRows: Float,
    renderedHeightRows: Float,
): HomeLayoutItem {
    val current = sanitizeHomeLayoutItem(item)
    return when (edge) {
        HomeLayoutResizeEdge.Left,
        HomeLayoutResizeEdge.Right -> resizeHomeLayoutItemHorizontally(
            item = current,
            edge = edge,
            delta = horizontalDelta.takeIf(Float::isFinite) ?: 0f,
        )

        HomeLayoutResizeEdge.Top,
        HomeLayoutResizeEdge.Bottom -> resizeHomeLayoutItemVertically(
            item = current,
            edge = edge,
            deltaRows = verticalDeltaRows.takeIf(Float::isFinite) ?: 0f,
            renderedHeightRows = renderedHeightRows.takeIf(Float::isFinite)
                ?.coerceAtLeast(minimumHomeLayoutHeight(current.card))
                ?: suggestedHomeLayoutHeight(current.card),
        )
    }
}

private fun resizeHomeLayoutItemHorizontally(
    item: HomeLayoutItem,
    edge: HomeLayoutResizeEdge,
    delta: Float,
): HomeLayoutItem {
    val oldLeft = (1f - item.width) * item.x
    val oldRight = oldLeft + item.width
    val newLeft: Float
    val newRight: Float
    if (edge == HomeLayoutResizeEdge.Left) {
        newLeft = (oldLeft + delta).coerceIn(0f, oldRight - MIN_HOME_LAYOUT_WIDTH)
        newRight = oldRight
    } else {
        newLeft = oldLeft
        newRight = (oldRight + delta).coerceIn(oldLeft + MIN_HOME_LAYOUT_WIDTH, 1f)
    }
    val newWidth = (newRight - newLeft).coerceIn(MIN_HOME_LAYOUT_WIDTH, 1f)
    val availableWidth = 1f - newWidth
    val newX = if (availableWidth <= 0.0001f) 0f else (newLeft / availableWidth).coerceIn(0f, 1f)
    return sanitizeHomeLayoutItem(item.copy(width = newWidth, x = newX))
}

private fun resizeHomeLayoutItemVertically(
    item: HomeLayoutItem,
    edge: HomeLayoutResizeEdge,
    deltaRows: Float,
    renderedHeightRows: Float,
): HomeLayoutItem {
    val currentHeight = item.height.takeIf { it > 0f } ?: renderedHeightRows
    val minHeight = minimumHomeLayoutHeight(item.card)
    if (edge == HomeLayoutResizeEdge.Bottom) {
        return sanitizeHomeLayoutItem(
            item.copy(height = (currentHeight + deltaRows).coerceIn(minHeight, MAX_HOME_LAYOUT_HEIGHT_ROWS)),
        )
    }

    val maximumAnchoredHeight = (currentHeight + item.y).coerceAtMost(MAX_HOME_LAYOUT_HEIGHT_ROWS)
    val newHeight = (currentHeight - deltaRows).coerceIn(minHeight, maximumAnchoredHeight)
    val newY = (item.y + currentHeight - newHeight).coerceIn(0f, MAX_HOME_LAYOUT_Y_ROWS)
    return sanitizeHomeLayoutItem(item.copy(y = newY, height = newHeight))
}

fun defaultHomeLayoutItems(): List<HomeLayoutItem> {
    return listOf(
        HomeLayoutItem(
            HomeLayoutCard.Lkm,
            x = 0f,
            y = 0f,
            width = 0.48f,
            scale = 1f,
            aspectRatio = 1f,
            height = 0f,
            visible = true,
            zIndex = 0,
        ),
        HomeLayoutItem(
            HomeLayoutCard.Superuser,
            x = 1f,
            y = 0f,
            width = 0.48f,
            scale = 1f,
            aspectRatio = 0f,
            height = 0f,
            visible = true,
            zIndex = 1,
        ),
        HomeLayoutItem(
            HomeLayoutCard.Module,
            x = 1f,
            y = 0.66f,
            width = 0.48f,
            scale = 1f,
            aspectRatio = 0f,
            height = 0f,
            visible = true,
            zIndex = 2,
        ),
        HomeLayoutItem(
            HomeLayoutCard.StatusMonitor,
            x = 0f,
            y = 1.42f,
            width = 1f,
            scale = 1f,
            aspectRatio = 0f,
            height = 0f,
            visible = true,
            zIndex = 3,
        ),
        HomeLayoutItem(
            HomeLayoutCard.SystemInfo,
            x = 0f,
            y = 2.38f,
            width = 1f,
            scale = 1f,
            aspectRatio = 0f,
            height = 0f,
            visible = true,
            zIndex = 4,
        ),
    )
}

fun homeLayoutItemsForPreset(preset: HomeLayoutPreset): List<HomeLayoutItem> {
    val defaults = defaultHomeLayoutItems().associateBy { it.card }
    fun item(
        card: HomeLayoutCard,
        x: Float,
        y: Float,
        width: Float,
        aspectRatio: Float = defaults.getValue(card).aspectRatio,
    ) = defaults.getValue(card).copy(
        x = x,
        y = y,
        width = width,
        aspectRatio = aspectRatio,
        visible = true,
    )

    return when (preset) {
        HomeLayoutPreset.DualColumn -> defaultHomeLayoutItems()
        HomeLayoutPreset.SingleColumn -> listOf(
            item(HomeLayoutCard.Lkm, x = 0f, y = 0f, width = 1f, aspectRatio = 2f),
            item(HomeLayoutCard.Superuser, x = 0f, y = 1.25f, width = 1f),
            item(HomeLayoutCard.Module, x = 0f, y = 1.92f, width = 1f),
            item(HomeLayoutCard.StatusMonitor, x = 0f, y = 2.62f, width = 1f),
            item(HomeLayoutCard.SystemInfo, x = 0f, y = 3.52f, width = 1f),
        )
        HomeLayoutPreset.Compact -> listOf(
            item(HomeLayoutCard.Lkm, x = 0f, y = 0f, width = 0.58f, aspectRatio = 1f),
            item(HomeLayoutCard.Superuser, x = 1f, y = 0f, width = 0.38f),
            item(HomeLayoutCard.Module, x = 1f, y = 0.62f, width = 0.38f),
            item(HomeLayoutCard.StatusMonitor, x = 0f, y = 1.36f, width = 0.48f),
            item(HomeLayoutCard.SystemInfo, x = 1f, y = 1.36f, width = 0.48f),
        )
    }
}

fun snapHomeLayoutItem(
    item: HomeLayoutItem,
    allItems: List<HomeLayoutItem>,
): HomeLayoutItem {
    val xTargets = listOf(0f, 0.5f, 1f)
    val yTargets = buildList {
        add((item.y * 10f).roundToInt() / 10f)
        allItems.asSequence()
            .filter { it.card != item.card && it.visible }
            .mapTo(this) { it.y }
    }
    return sanitizeHomeLayoutItem(
        item.copy(
            x = item.x.snapToNearest(xTargets, threshold = 0.045f),
            y = item.y.snapToNearest(yTargets, threshold = 0.05f),
        ),
    )
}

fun moveHomeLayoutCardLayer(
    items: List<HomeLayoutItem>,
    card: HomeLayoutCard,
    direction: Int,
): List<HomeLayoutItem> {
    val ordered = normalizeHomeLayoutZOrder(items).sortedBy { it.zIndex }.toMutableList()
    val index = ordered.indexOfFirst { it.card == card }
    if (index < 0) return normalizeHomeLayoutZOrder(items)
    val target = (index + direction.coerceIn(-1, 1)).coerceIn(0, ordered.lastIndex)
    if (target == index) return normalizeHomeLayoutZOrder(items)
    val moved = ordered.removeAt(index)
    ordered.add(target, moved)
    val zByCard = ordered.mapIndexed { z, layoutItem -> layoutItem.card to z }.toMap()
    return items.map { it.copy(zIndex = zByCard.getValue(it.card)) }
}

fun normalizeHomeLayoutZOrder(items: List<HomeLayoutItem>): List<HomeLayoutItem> {
    val ordered = items.sortedWith(compareBy<HomeLayoutItem> { it.zIndex }.thenBy { it.card.ordinal })
    val zByCard = ordered.mapIndexed { z, item -> item.card to z }.toMap()
    return items.map { it.copy(zIndex = zByCard.getValue(it.card)) }
}

private fun mergeHomeLayoutItems(items: List<HomeLayoutItem>): List<HomeLayoutItem> {
    val byCard = items.associateBy { it.card }
    return normalizeHomeLayoutZOrder(defaultHomeLayoutItems().map { fallback ->
        sanitizeHomeLayoutItem(byCard[fallback.card] ?: fallback)
    })
}

private fun encodeHomeLayoutItems(items: List<HomeLayoutItem>): String {
    return mergeHomeLayoutItems(items).joinToString("\n") { item ->
        listOf(
            item.card.value,
            item.x.formatHomeLayoutFloat(),
            item.y.formatHomeLayoutFloat(),
            item.width.formatHomeLayoutFloat(),
            item.scale.formatHomeLayoutFloat(),
            item.aspectRatio.formatHomeLayoutFloat(),
            if (item.visible) "1" else "0",
            item.zIndex.toString(),
            item.height.formatHomeLayoutFloat(),
        ).joinToString("|")
    }
}

private fun decodeHomeLayoutItems(value: String): List<HomeLayoutItem> {
    return value.lineSequence().mapNotNull { line ->
        val parts = line.split('|')
        if (parts.size !in 6..9) return@mapNotNull null
        val card = HomeLayoutCard.fromValue(parts[0]) ?: return@mapNotNull null
        val hasAspectRatio = parts.size >= 7
        val fallback = defaultHomeLayoutItems().first { it.card == card }
        HomeLayoutItem(
            card = card,
            x = parts[1].toFloatOrNull() ?: return@mapNotNull null,
            y = parts[2].toFloatOrNull() ?: return@mapNotNull null,
            width = parts[3].toFloatOrNull() ?: return@mapNotNull null,
            scale = parts[4].toFloatOrNull() ?: return@mapNotNull null,
            aspectRatio = if (hasAspectRatio) {
                parts[5].toFloatOrNull() ?: return@mapNotNull null
            } else {
                fallback.aspectRatio
            },
            visible = parts[if (hasAspectRatio) 6 else 5] == "1",
            zIndex = if (parts.size >= 8) {
                parts[7].toIntOrNull() ?: return@mapNotNull null
            } else {
                fallback.zIndex
            },
            height = if (parts.size == 9) {
                parts[8].toFloatOrNull() ?: return@mapNotNull null
            } else {
                0f
            },
        )
    }.toList()
}

private fun Float.formatHomeLayoutFloat(): String {
    return String.format(Locale.US, "%.3f", this)
}

private fun Float.finiteOr(fallback: Float): Float {
    return if (isFinite()) this else fallback
}

private fun Float.snapToNearest(targets: List<Float>, threshold: Float): Float {
    val nearest = targets.minByOrNull { kotlin.math.abs(this - it) } ?: return this
    return if (kotlin.math.abs(this - nearest) <= threshold) nearest else this
}
