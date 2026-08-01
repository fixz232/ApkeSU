package me.weishu.kernelsu.ui.util

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Immutable
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

private const val HOME_LAYOUT_ENABLED_KEY = "home_layout_enabled"
private const val HOME_LAYOUT_RECORDS_KEY = "home_layout_records"
private const val HOME_LAYOUT_LANDSCAPE_RECORDS_KEY = "home_layout_landscape_records"
private const val HOME_LAYOUT_AUTO_SNAP_KEY = "home_layout_auto_snap"
private const val HOME_LAYOUT_AUTO_AVOID_KEY = "home_layout_auto_avoid_overlap"
private const val MIN_HOME_LAYOUT_WIDTH = 0.28f
private const val MAX_HOME_LAYOUT_HEIGHT_ROWS = 4f
private const val MAX_HOME_LAYOUT_Y_ROWS = 6f
private const val HOME_LAYOUT_RECORD_VERSION = 2
private const val HOME_LAYOUT_COLLISION_GAP_ROWS = 0.08f
internal const val HOME_LAYOUT_TRANSFER_SCHEMA = "io.github.fixz.apkesu.home-layout"
internal const val HOME_LAYOUT_TRANSFER_VERSION = 1

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

enum class HomeLayoutWallpaperFit(val value: String) {
    Crop("crop"),
    Fit("fit"),
    Stretch("stretch");

    companion object {
        fun fromValue(value: String?): HomeLayoutWallpaperFit {
            return entries.firstOrNull { it.value == value } ?: Crop
        }
    }
}

@Immutable
data class HomeLayoutSticker(
    val id: String,
    val uriString: String,
    val x: Float = 0.5f,
    val y: Float = 0.5f,
    val width: Float = 0.28f,
    val opacity: Float = 1f,
)

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
    val customTitle: String = "",
    val customSubtitle: String = "",
    val textScale: Float = 1f,
    val wallpaperFit: HomeLayoutWallpaperFit = HomeLayoutWallpaperFit.Crop,
    val stickers: List<HomeLayoutSticker> = emptyList(),
)

@Immutable
data class HomeLayoutState(
    val enabled: Boolean = false,
    val autoSnap: Boolean = true,
    val autoAvoidOverlap: Boolean = true,
    val items: List<HomeLayoutItem> = defaultHomeLayoutItems(),
    val landscapeItems: List<HomeLayoutItem> = defaultLandscapeHomeLayoutItems(),
)

fun HomeLayoutState.itemsForOrientation(isLandscape: Boolean): List<HomeLayoutItem> {
    return if (isLandscape) landscapeItems else items
}

fun HomeLayoutState.withItemsForOrientation(
    isLandscape: Boolean,
    nextItems: List<HomeLayoutItem>,
): HomeLayoutState {
    return if (isLandscape) copy(landscapeItems = nextItems) else copy(items = nextItems)
}

fun readHomeLayoutState(context: Context): HomeLayoutState {
    val prefs = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
    return prefs.readHomeLayoutState()
}

internal fun SharedPreferences.readHomeLayoutState(): HomeLayoutState {
    val parsed = getString(HOME_LAYOUT_RECORDS_KEY, null)
        ?.let(::decodeHomeLayoutItems)
        .orEmpty()
    val parsedLandscape = getString(HOME_LAYOUT_LANDSCAPE_RECORDS_KEY, null)
        ?.let(::decodeHomeLayoutItems)
        .orEmpty()
    return HomeLayoutState(
        enabled = getBoolean(HOME_LAYOUT_ENABLED_KEY, false),
        autoSnap = getBoolean(HOME_LAYOUT_AUTO_SNAP_KEY, true),
        autoAvoidOverlap = getBoolean(HOME_LAYOUT_AUTO_AVOID_KEY, true),
        items = mergeHomeLayoutItems(parsed),
        landscapeItems = mergeHomeLayoutItems(
            items = parsedLandscape,
            defaults = defaultLandscapeHomeLayoutItems(),
        ),
    )
}

fun saveHomeLayoutState(context: Context, state: HomeLayoutState): Boolean {
    val portrait = mergeHomeLayoutItems(state.items).let { items ->
        if (state.autoAvoidOverlap) resolveHomeLayoutCollisions(items) else items
    }
    val landscape = mergeHomeLayoutItems(
        items = state.landscapeItems,
        defaults = defaultLandscapeHomeLayoutItems(),
    ).let { items ->
        if (state.autoAvoidOverlap) resolveHomeLayoutCollisions(items) else items
    }
    val sanitized = state.copy(items = portrait, landscapeItems = landscape)
    return context.applicationContext
        .getSharedPreferences("settings", Context.MODE_PRIVATE)
        .edit()
        .putHomeLayoutState(sanitized)
        .commit()
}

internal fun SharedPreferences.Editor.putHomeLayoutState(state: HomeLayoutState): SharedPreferences.Editor {
    return putBoolean(HOME_LAYOUT_ENABLED_KEY, state.enabled)
        .putBoolean(HOME_LAYOUT_AUTO_SNAP_KEY, state.autoSnap)
        .putBoolean(HOME_LAYOUT_AUTO_AVOID_KEY, state.autoAvoidOverlap)
        .putString(HOME_LAYOUT_RECORDS_KEY, encodeHomeLayoutItems(state.items))
        .putString(
            HOME_LAYOUT_LANDSCAPE_RECORDS_KEY,
            encodeHomeLayoutItems(state.landscapeItems, defaultLandscapeHomeLayoutItems()),
        )
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
    val sanitizedWidth = width
    return item.copy(
        x = if (sanitizedWidth >= 0.999f) 0f else item.x.finiteOr(fallback.x).coerceIn(0f, 1f),
        y = item.y.finiteOr(fallback.y).coerceIn(0f, MAX_HOME_LAYOUT_Y_ROWS),
        width = sanitizedWidth,
        scale = 1f,
        aspectRatio = if (item.card == HomeLayoutCard.Lkm) {
            item.aspectRatio.finiteOr(fallback.aspectRatio).coerceIn(1f, 2.2f)
        } else {
            0f
        },
        height = height,
        zIndex = item.zIndex.coerceIn(0, HomeLayoutCard.entries.lastIndex),
        customTitle = item.customTitle.take(80),
        customSubtitle = item.customSubtitle.take(160),
        textScale = item.textScale.finiteOr(1f).coerceIn(0.72f, 1.25f),
        stickers = item.stickers
            .map(::sanitizeHomeLayoutSticker)
            .distinctBy { it.id }
            .take(12),
    )
}

fun sanitizeHomeLayoutSticker(sticker: HomeLayoutSticker): HomeLayoutSticker {
    val safeUri = sticker.uriString.trim().take(2048)
    val safeId = sticker.id.trim().take(80).ifBlank {
        "sticker-${safeUri.hashCode().toUInt().toString(16)}"
    }
    return sticker.copy(
        id = safeId,
        uriString = safeUri,
        x = sticker.x.finiteOr(0.5f).coerceIn(0f, 1f),
        y = sticker.y.finiteOr(0.5f).coerceIn(0f, 1f),
        width = sticker.width.finiteOr(0.28f).coerceIn(0.08f, 1f),
        opacity = sticker.opacity.finiteOr(1f).coerceIn(0.1f, 1f),
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

/**
 * Moves a card using canvas-normalized coordinates. Keeping the delta independent of the
 * remaining width prevents a nearly full-width card from jumping across the canvas.
 */
fun moveHomeLayoutItem(
    item: HomeLayoutItem,
    horizontalDelta: Float,
    verticalDeltaRows: Float,
): HomeLayoutItem {
    val current = sanitizeHomeLayoutItem(item)
    val availableWidth = (1f - current.width).coerceAtLeast(0f)
    val currentLeft = availableWidth * current.x
    val nextLeft = (currentLeft + horizontalDelta.finiteOr(0f))
        .coerceIn(0f, availableWidth)
    val nextX = if (availableWidth <= 0.0001f) 0f else nextLeft / availableWidth
    return sanitizeHomeLayoutItem(
        current.copy(
            x = nextX,
            y = current.y + verticalDeltaRows.finiteOr(0f),
        ),
    )
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

fun defaultLandscapeHomeLayoutItems(): List<HomeLayoutItem> {
    val defaults = defaultHomeLayoutItems().associateBy { it.card }
    return listOf(
        defaults.getValue(HomeLayoutCard.Lkm).copy(
            x = 0f,
            y = 0f,
            width = 0.32f,
            aspectRatio = 1.25f,
        ),
        defaults.getValue(HomeLayoutCard.Superuser).copy(x = 0.5f, y = 0f, width = 0.32f),
        defaults.getValue(HomeLayoutCard.Module).copy(x = 1f, y = 0f, width = 0.32f),
        defaults.getValue(HomeLayoutCard.StatusMonitor).copy(x = 0f, y = 1.08f, width = 0.48f),
        defaults.getValue(HomeLayoutCard.SystemInfo).copy(x = 1f, y = 1.08f, width = 0.48f),
    )
}

fun homeLayoutItemsForPreset(
    preset: HomeLayoutPreset,
    isLandscape: Boolean = false,
): List<HomeLayoutItem> {
    val defaultItems = if (isLandscape) defaultLandscapeHomeLayoutItems() else defaultHomeLayoutItems()
    val defaults = defaultItems.associateBy { it.card }
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
        HomeLayoutPreset.DualColumn -> defaultItems
        HomeLayoutPreset.SingleColumn -> listOf(
            item(HomeLayoutCard.Lkm, x = 0f, y = 0f, width = 1f, aspectRatio = 2f),
            item(HomeLayoutCard.Superuser, x = 0f, y = 1.25f, width = 1f),
            item(HomeLayoutCard.Module, x = 0f, y = 1.92f, width = 1f),
            item(HomeLayoutCard.StatusMonitor, x = 0f, y = 2.62f, width = 1f),
            item(HomeLayoutCard.SystemInfo, x = 0f, y = 3.52f, width = 1f),
        )
        HomeLayoutPreset.Compact -> if (isLandscape) {
            listOf(
                item(HomeLayoutCard.Lkm, x = 0f, y = 0f, width = 0.38f, aspectRatio = 1.35f),
                item(HomeLayoutCard.Superuser, x = 0.5f, y = 0f, width = 0.28f),
                item(HomeLayoutCard.Module, x = 1f, y = 0f, width = 0.28f),
                item(HomeLayoutCard.StatusMonitor, x = 0f, y = 1.05f, width = 0.38f),
                item(HomeLayoutCard.SystemInfo, x = 1f, y = 1.05f, width = 0.58f),
            )
        } else {
            listOf(
                item(HomeLayoutCard.Lkm, x = 0f, y = 0f, width = 0.58f, aspectRatio = 1f),
                item(HomeLayoutCard.Superuser, x = 1f, y = 0f, width = 0.38f),
                item(HomeLayoutCard.Module, x = 1f, y = 0.62f, width = 0.38f),
                item(HomeLayoutCard.StatusMonitor, x = 0f, y = 1.36f, width = 0.48f),
                item(HomeLayoutCard.SystemInfo, x = 1f, y = 1.36f, width = 0.48f),
            )
        }
    }
}

fun snapHomeLayoutItem(
    item: HomeLayoutItem,
    allItems: List<HomeLayoutItem>,
): HomeLayoutItem {
    val availableWidth = (1f - item.width).coerceAtLeast(0f)
    fun xFromLeft(left: Float): Float = if (availableWidth <= 0.0001f) {
        0f
    } else {
        (left / availableWidth).coerceIn(0f, 1f)
    }
    val itemHeight = item.renderedHeightRows()
    val xTargets = buildList {
        add(0f)
        add(0.5f)
        add(1f)
        add(xFromLeft(0.5f - item.width / 2f))
        allItems.asSequence()
            .filter { it.card != item.card && it.visible }
            .forEach { other ->
                val otherLeft = (1f - other.width) * other.x
                val otherRight = otherLeft + other.width
                val otherCenter = otherLeft + other.width / 2f
                add(xFromLeft(otherLeft))
                add(xFromLeft(otherRight - item.width))
                add(xFromLeft(otherCenter - item.width / 2f))
            }
    }
    val yTargets = buildList {
        add((item.y * 10f).roundToInt() / 10f)
        allItems.asSequence()
            .filter { it.card != item.card && it.visible }
            .forEach { other ->
                val otherHeight = other.renderedHeightRows()
                add(other.y)
                add(other.y + otherHeight)
                add(other.y + otherHeight / 2f - itemHeight / 2f)
            }
    }
    return sanitizeHomeLayoutItem(
        item.copy(
            x = item.x.snapToNearest(xTargets, threshold = 0.045f),
            y = item.y.snapToNearest(yTargets, threshold = 0.05f),
        ),
    )
}

fun resolveHomeLayoutCollisions(
    items: List<HomeLayoutItem>,
    gapRows: Float = HOME_LAYOUT_COLLISION_GAP_ROWS,
): List<HomeLayoutItem> {
    val sanitized = items.map(::sanitizeHomeLayoutItem)
    val placed = mutableListOf<HomeLayoutItem>()
    val resolvedByCard = mutableMapOf<HomeLayoutCard, HomeLayoutItem>()
    sanitized
        .filter { it.visible }
        .sortedWith(compareBy<HomeLayoutItem> { it.y }.thenBy { it.zIndex })
        .forEach { source ->
            var candidate = source
            while (true) {
                val conflicts = placed.filter { it.overlaps(candidate) }
                if (conflicts.isEmpty()) break
                val nextY = conflicts.maxOf { it.y + it.renderedHeightRows() } + gapRows
                candidate = sanitizeHomeLayoutItem(candidate.copy(y = nextY))
                if (candidate.y >= MAX_HOME_LAYOUT_Y_ROWS) break
            }
            placed += candidate
            resolvedByCard[source.card] = candidate
        }
    return sanitized.map { resolvedByCard[it.card] ?: it }
}

/** Places visible cards in the nearest free row/column without changing their sizes or content. */
fun autoArrangeHomeLayoutItems(items: List<HomeLayoutItem>): List<HomeLayoutItem> {
    val sanitized = items.map(::sanitizeHomeLayoutItem)
    val placed = mutableListOf<HomeLayoutItem>()
    val resolved = mutableMapOf<HomeLayoutCard, HomeLayoutItem>()
    sanitized
        .filter { it.visible }
        .sortedWith(compareBy<HomeLayoutItem> { it.y }.thenBy { it.zIndex })
        .forEach { source ->
            val availableWidth = (1f - source.width).coerceAtLeast(0f)
            fun withLeft(left: Float, y: Float): HomeLayoutItem {
                val x = if (availableWidth <= 0.0001f) 0f else left / availableWidth
                return sanitizeHomeLayoutItem(source.copy(x = x, y = y))
            }
            val xCandidates = buildList {
                add(0f)
                add((availableWidth / 2f).coerceAtLeast(0f))
                add(availableWidth)
                placed.forEach { other ->
                    val otherLeft = (1f - other.width) * other.x
                    add(otherLeft.coerceIn(0f, availableWidth))
                    add((otherLeft + other.width - source.width).coerceIn(0f, availableWidth))
                }
            }.distinct()
            val yCandidates = buildList {
                add(0f)
                placed.forEach { other ->
                    add(other.y)
                    add(other.y + other.renderedHeightRows() + HOME_LAYOUT_COLLISION_GAP_ROWS)
                }
            }.distinct().sorted()
            val candidate = yCandidates.asSequence()
                .flatMap { y -> xCandidates.asSequence().map { left -> withLeft(left, y) } }
                .filter { item -> item.y + item.renderedHeightRows() <= MAX_HOME_LAYOUT_Y_ROWS }
                .filter { item -> placed.none { other -> other.overlaps(item) } }
                .minByOrNull { item ->
                    kotlin.math.abs(item.y - source.y) +
                        kotlin.math.abs(((1f - item.width) * item.x) - ((1f - source.width) * source.x))
                }
                ?: resolveHomeLayoutCollisions(placed + source).last()
            placed += candidate
            resolved[source.card] = candidate
        }
    return sanitized.map { resolved[it.card] ?: it }
}

private fun HomeLayoutItem.renderedHeightRows(): Float {
    return height.takeIf { it > 0f } ?: suggestedHomeLayoutHeight(card)
}

private fun HomeLayoutItem.overlaps(other: HomeLayoutItem): Boolean {
    val left = (1f - width) * x
    val right = left + width
    val otherLeft = (1f - other.width) * other.x
    val otherRight = otherLeft + other.width
    val horizontal = left < otherRight && right > otherLeft
    val vertical = y < other.y + other.renderedHeightRows() &&
        y + renderedHeightRows() > other.y
    return horizontal && vertical
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

private fun mergeHomeLayoutItems(
    items: List<HomeLayoutItem>,
    defaults: List<HomeLayoutItem> = defaultHomeLayoutItems(),
): List<HomeLayoutItem> {
    val byCard = items.associateBy { it.card }
    return normalizeHomeLayoutZOrder(defaults.map { fallback ->
        sanitizeHomeLayoutItem(byCard[fallback.card] ?: fallback)
    })
}

internal fun encodeHomeLayoutItems(
    items: List<HomeLayoutItem>,
    defaults: List<HomeLayoutItem> = defaultHomeLayoutItems(),
): String = JSONObject()
    .put("version", HOME_LAYOUT_RECORD_VERSION)
    .put("items", homeLayoutItemsToJson(items, defaults))
    .toString()

/**
 * Portable representation used by the standalone layout file and Theme Store packages.
 * The resolver can replace local sticker URIs with package asset metadata before export.
 */
internal fun homeLayoutStateToJson(
    state: HomeLayoutState,
    stickerReference: (HomeLayoutSticker) -> JSONObject = { sticker ->
        JSONObject().put("uri", sticker.uriString)
    },
): JSONObject = JSONObject()
    .put("schema", HOME_LAYOUT_TRANSFER_SCHEMA)
    .put("version", HOME_LAYOUT_TRANSFER_VERSION)
    .put("enabled", state.enabled)
    .put("autoSnap", state.autoSnap)
    .put("autoAvoidOverlap", state.autoAvoidOverlap)
    .put(
        "portrait",
        JSONObject().put(
            "items",
            homeLayoutItemsToJson(state.items, defaultHomeLayoutItems(), stickerReference),
        ),
    )
    .put(
        "landscape",
        JSONObject().put(
            "items",
            homeLayoutItemsToJson(
                state.landscapeItems,
                defaultLandscapeHomeLayoutItems(),
                stickerReference,
            ),
        ),
    )

internal fun encodeHomeLayoutState(state: HomeLayoutState): String =
    homeLayoutStateToJson(state).toString()

internal fun decodeHomeLayoutState(value: String): HomeLayoutState? = runCatching {
    homeLayoutStateFromJson(JSONObject(value))
}.getOrNull()

internal fun homeLayoutStateFromJson(
    root: JSONObject,
    stickerUriResolver: (JSONObject) -> String? = { record ->
        record.optString("uri").trim().takeIf(String::isNotBlank)
    },
): HomeLayoutState? {
    val schema = root.optString("schema")
    if (schema.isNotBlank() && schema != HOME_LAYOUT_TRANSFER_SCHEMA) return null
    val version = root.optInt("version", 0)
    if (version !in 1..HOME_LAYOUT_TRANSFER_VERSION) return null
    val portraitRecords = root.optJSONObject("portrait")?.optJSONArray("items")
        ?: root.optJSONArray("items")
        ?: return null
    val landscapeRecords = root.optJSONObject("landscape")?.optJSONArray("items")
        ?: JSONArray()
    val portrait = mergeHomeLayoutItems(
        decodeHomeLayoutItemsJson(portraitRecords, defaultHomeLayoutItems(), stickerUriResolver),
    )
    val landscape = mergeHomeLayoutItems(
        decodeHomeLayoutItemsJson(
            landscapeRecords,
            defaultLandscapeHomeLayoutItems(),
            stickerUriResolver,
        ),
        defaultLandscapeHomeLayoutItems(),
    )
    return HomeLayoutState(
        enabled = root.optBoolean("enabled", false),
        autoSnap = root.optBoolean("autoSnap", true),
        autoAvoidOverlap = root.optBoolean("autoAvoidOverlap", true),
        items = portrait,
        landscapeItems = landscape,
    )
}

private fun homeLayoutItemsToJson(
    items: List<HomeLayoutItem>,
    defaults: List<HomeLayoutItem>,
    stickerReference: (HomeLayoutSticker) -> JSONObject = { sticker ->
        JSONObject().put("uri", sticker.uriString)
    },
): JSONArray {
    val records = JSONArray()
    mergeHomeLayoutItems(items, defaults).forEach { item ->
        val stickers = JSONArray()
        item.stickers.forEach { sticker ->
            val reference = stickerReference(sticker)
            stickers.put(
                reference
                    .put("id", sticker.id)
                    .put("x", sticker.x.toDouble())
                    .put("y", sticker.y.toDouble())
                    .put("width", sticker.width.toDouble())
                    .put("opacity", sticker.opacity.toDouble()),
            )
        }
        records.put(
            JSONObject()
                .put("card", item.card.value)
                .put("x", item.x.toDouble())
                .put("y", item.y.toDouble())
                .put("width", item.width.toDouble())
                .put("scale", item.scale.toDouble())
                .put("aspectRatio", item.aspectRatio.toDouble())
                .put("height", item.height.toDouble())
                .put("visible", item.visible)
                .put("zIndex", item.zIndex)
                .put("customTitle", item.customTitle)
                .put("customSubtitle", item.customSubtitle)
                .put("textScale", item.textScale.toDouble())
                .put("wallpaperFit", item.wallpaperFit.value)
                .put("stickers", stickers),
        )
    }
    return records
}

internal fun decodeHomeLayoutItems(value: String): List<HomeLayoutItem> {
    val trimmed = value.trim()
    if (trimmed.startsWith("{")) {
        return runCatching { decodeJsonHomeLayoutItems(JSONObject(trimmed)) }.getOrDefault(emptyList())
    }
    return decodeLegacyHomeLayoutItems(value)
}

private fun decodeJsonHomeLayoutItems(root: JSONObject): List<HomeLayoutItem> {
    val items = root.optJSONArray("items") ?: return emptyList()
    return decodeHomeLayoutItemsJson(items, defaultHomeLayoutItems())
}

private fun decodeHomeLayoutItemsJson(
    items: JSONArray,
    defaults: List<HomeLayoutItem>,
    stickerUriResolver: (JSONObject) -> String? = { record ->
        record.optString("uri").trim().takeIf(String::isNotBlank)
    },
): List<HomeLayoutItem> {
    return buildList {
        for (index in 0 until items.length()) {
            val record = items.optJSONObject(index) ?: continue
            val card = HomeLayoutCard.fromValue(record.optString("card")) ?: continue
            val fallback = defaults.firstOrNull { it.card == card }
                ?: defaultHomeLayoutItems().first { it.card == card }
            val stickerRecords = record.optJSONArray("stickers") ?: JSONArray()
            val stickers = buildList {
                for (stickerIndex in 0 until stickerRecords.length()) {
                    val sticker = stickerRecords.optJSONObject(stickerIndex) ?: continue
                    val uriString = stickerUriResolver(sticker)?.trim().orEmpty()
                    if (uriString.isEmpty()) continue
                    add(
                        HomeLayoutSticker(
                            id = sticker.optString("id"),
                            uriString = uriString,
                            x = sticker.optDouble("x", 0.5).toFloat(),
                            y = sticker.optDouble("y", 0.5).toFloat(),
                            width = sticker.optDouble("width", 0.28).toFloat(),
                            opacity = sticker.optDouble("opacity", 1.0).toFloat(),
                        ),
                    )
                }
            }
            add(
                sanitizeHomeLayoutItem(
                    HomeLayoutItem(
                        card = card,
                        x = record.optDouble("x", fallback.x.toDouble()).toFloat(),
                        y = record.optDouble("y", fallback.y.toDouble()).toFloat(),
                        width = record.optDouble("width", fallback.width.toDouble()).toFloat(),
                        scale = record.optDouble("scale", 1.0).toFloat(),
                        aspectRatio = record.optDouble("aspectRatio", fallback.aspectRatio.toDouble()).toFloat(),
                        height = record.optDouble("height", 0.0).toFloat(),
                        visible = record.optBoolean("visible", fallback.visible),
                        zIndex = record.optInt("zIndex", fallback.zIndex),
                        customTitle = record.optString("customTitle"),
                        customSubtitle = record.optString("customSubtitle"),
                        textScale = record.optDouble("textScale", 1.0).toFloat(),
                        wallpaperFit = HomeLayoutWallpaperFit.fromValue(record.optString("wallpaperFit")),
                        stickers = stickers,
                    ),
                ),
            )
        }
    }
}

private fun decodeLegacyHomeLayoutItems(value: String): List<HomeLayoutItem> {
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

private fun Float.finiteOr(fallback: Float): Float {
    return if (isFinite()) this else fallback
}

private fun Float.snapToNearest(targets: List<Float>, threshold: Float): Float {
    val nearest = targets.minByOrNull { kotlin.math.abs(this - it) } ?: return this
    return if (kotlin.math.abs(this - nearest) <= threshold) nearest else this
}
