package me.weishu.kernelsu.ui.util

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.content.edit
import me.weishu.kernelsu.R

const val CUSTOM_NAVIGATION_ICON_MAX_SIDE = 256

val DEFAULT_CUSTOM_NAVIGATION_ICON_CROP = CustomWallpaperCrop(
    left = 0f,
    top = 0f,
    right = 1f,
    bottom = 1f,
)

enum class CustomNavigationIconSlot(
    val id: String,
    @StringRes val labelRes: Int,
    @StringRes val titleRes: Int,
    @StringRes val cropTitleRes: Int,
    @StringRes val previewTitleRes: Int,
    val uriKey: String,
    val cropLeftKey: String,
    val cropTopKey: String,
    val cropRightKey: String,
    val cropBottomKey: String,
) {
    Home(
        id = "home",
        labelRes = R.string.home,
        titleRes = R.string.settings_navigation_icon_home,
        cropTitleRes = R.string.settings_navigation_icon_home_crop,
        previewTitleRes = R.string.settings_navigation_icon_home_preview,
        uriKey = "custom_navigation_icon_home_uri",
        cropLeftKey = "custom_navigation_icon_home_crop_left",
        cropTopKey = "custom_navigation_icon_home_crop_top",
        cropRightKey = "custom_navigation_icon_home_crop_right",
        cropBottomKey = "custom_navigation_icon_home_crop_bottom",
    ),
    Kpm(
        id = "kpm",
        labelRes = R.string.kpm_short_title,
        titleRes = R.string.settings_navigation_icon_kpm,
        cropTitleRes = R.string.settings_navigation_icon_kpm_crop,
        previewTitleRes = R.string.settings_navigation_icon_kpm_preview,
        uriKey = "custom_navigation_icon_kpm_uri",
        cropLeftKey = "custom_navigation_icon_kpm_crop_left",
        cropTopKey = "custom_navigation_icon_kpm_crop_top",
        cropRightKey = "custom_navigation_icon_kpm_crop_right",
        cropBottomKey = "custom_navigation_icon_kpm_crop_bottom",
    ),
    Superuser(
        id = "superuser",
        labelRes = R.string.superuser,
        titleRes = R.string.settings_navigation_icon_superuser,
        cropTitleRes = R.string.settings_navigation_icon_superuser_crop,
        previewTitleRes = R.string.settings_navigation_icon_superuser_preview,
        uriKey = "custom_navigation_icon_superuser_uri",
        cropLeftKey = "custom_navigation_icon_superuser_crop_left",
        cropTopKey = "custom_navigation_icon_superuser_crop_top",
        cropRightKey = "custom_navigation_icon_superuser_crop_right",
        cropBottomKey = "custom_navigation_icon_superuser_crop_bottom",
    ),
    Module(
        id = "module",
        labelRes = R.string.module,
        titleRes = R.string.settings_navigation_icon_module,
        cropTitleRes = R.string.settings_navigation_icon_module_crop,
        previewTitleRes = R.string.settings_navigation_icon_module_preview,
        uriKey = "custom_navigation_icon_module_uri",
        cropLeftKey = "custom_navigation_icon_module_crop_left",
        cropTopKey = "custom_navigation_icon_module_crop_top",
        cropRightKey = "custom_navigation_icon_module_crop_right",
        cropBottomKey = "custom_navigation_icon_module_crop_bottom",
    ),
    Settings(
        id = "settings",
        labelRes = R.string.settings,
        titleRes = R.string.settings_navigation_icon_settings,
        cropTitleRes = R.string.settings_navigation_icon_settings_crop,
        previewTitleRes = R.string.settings_navigation_icon_settings_preview,
        uriKey = "custom_navigation_icon_settings_uri",
        cropLeftKey = "custom_navigation_icon_settings_crop_left",
        cropTopKey = "custom_navigation_icon_settings_crop_top",
        cropRightKey = "custom_navigation_icon_settings_crop_right",
        cropBottomKey = "custom_navigation_icon_settings_crop_bottom",
    ),

    ;

    val sizeScaleKey: String get() = "custom_navigation_icon_${id}_size_scale"
    val innerPaddingKey: String get() = "custom_navigation_icon_${id}_inner_padding"
    val verticalOffsetKey: String get() = "custom_navigation_icon_${id}_vertical_offset"
    val opacityKey: String get() = "custom_navigation_icon_${id}_opacity"
    val tintArgbKey: String get() = "custom_navigation_icon_${id}_tint_argb"
    val maskKey: String get() = "custom_navigation_icon_${id}_mask"
    val labelKey: String get() = "custom_navigation_icon_${id}_label"

    val preferenceKeys: Set<String>
        get() = setOf(
            uriKey,
            cropLeftKey,
            cropTopKey,
            cropRightKey,
            cropBottomKey,
            sizeScaleKey,
            innerPaddingKey,
            verticalOffsetKey,
            opacityKey,
            tintArgbKey,
            maskKey,
            labelKey,
        )
}

enum class CustomNavigationIconMask(val value: String) {
    Original("original"),
    Circle("circle"),
    Square("square"),
    RoundedSquare("rounded_square");

    companion object {
        fun fromValue(value: String?): CustomNavigationIconMask {
            return entries.firstOrNull { it.value == value } ?: Original
        }
    }
}

@Immutable
data class CustomNavigationIconState(
    val uriString: String? = null,
    val crop: CustomWallpaperCrop = DEFAULT_CUSTOM_NAVIGATION_ICON_CROP,
    val sizeScale: Float = 1f,
    val innerPaddingDp: Float = 0f,
    val verticalOffsetDp: Float = 0f,
    val opacity: Float = 1f,
    val tintArgb: Long? = null,
    val mask: CustomNavigationIconMask = CustomNavigationIconMask.Original,
    val labelOverride: String? = null,
) {
    val hasSelected: Boolean
        get() = !uriString.isNullOrBlank()

    fun normalized(): CustomNavigationIconState = copy(
        crop = sanitizeCustomWallpaperCrop(crop),
        sizeScale = sizeScale.takeIf(Float::isFinite)?.coerceIn(0.6f, 1.4f) ?: 1f,
        innerPaddingDp = innerPaddingDp.takeIf(Float::isFinite)?.coerceIn(0f, 8f) ?: 0f,
        verticalOffsetDp = verticalOffsetDp.takeIf(Float::isFinite)?.coerceIn(-8f, 8f) ?: 0f,
        opacity = opacity.takeIf(Float::isFinite)?.coerceIn(0.2f, 1f) ?: 1f,
        labelOverride = labelOverride?.trim()?.take(20)?.takeIf(String::isNotBlank),
    )

    fun displayLabel(fallback: String): String = normalized().labelOverride ?: fallback
}

@Immutable
data class CustomNavigationIconSet(
    val home: CustomNavigationIconState = CustomNavigationIconState(),
    val kpm: CustomNavigationIconState = CustomNavigationIconState(),
    val superuser: CustomNavigationIconState = CustomNavigationIconState(),
    val module: CustomNavigationIconState = CustomNavigationIconState(),
    val settings: CustomNavigationIconState = CustomNavigationIconState(),
) {
    val selectedCount: Int
        get() = CustomNavigationIconSlot.entries.count { get(it).hasSelected }

    val hasSelected: Boolean
        get() = selectedCount > 0

    val hasCustomization: Boolean
        get() = CustomNavigationIconSlot.entries.any { slot ->
            get(slot).normalized().let { state ->
                state.hasSelected || state.labelOverride != null || state.sizeScale != 1f ||
                    state.innerPaddingDp != 0f || state.verticalOffsetDp != 0f ||
                    state.opacity != 1f || state.tintArgb != null ||
                    state.mask != CustomNavigationIconMask.Original
            }
        }

    operator fun get(slot: CustomNavigationIconSlot): CustomNavigationIconState {
        return when (slot) {
            CustomNavigationIconSlot.Home -> home
            CustomNavigationIconSlot.Kpm -> kpm
            CustomNavigationIconSlot.Superuser -> superuser
            CustomNavigationIconSlot.Module -> module
            CustomNavigationIconSlot.Settings -> settings
        }
    }
}

val LocalCustomNavigationIcons = staticCompositionLocalOf { CustomNavigationIconSet() }

fun readCustomNavigationIconSet(context: Context): CustomNavigationIconSet {
    return customNavigationIconPrefs(context).readCustomNavigationIconSet()
}

fun setCustomNavigationIcon(context: Context, slot: CustomNavigationIconSlot, uriString: String?) {
    val prefs = customNavigationIconPrefs(context)
    val previous = prefs.getString(slot.uriKey, null)
    prefs.edit(commit = true) {
        if (uriString.isNullOrBlank()) {
            remove(slot.uriKey)
            removeCustomNavigationIconCrop(slot)
        } else {
            putString(slot.uriKey, uriString)
            putCustomNavigationIconCrop(slot, DEFAULT_CUSTOM_NAVIGATION_ICON_CROP)
        }
    }
    if (previous != uriString) {
        releaseCustomImageReference(context, previous)
    }
}

fun setCustomNavigationIconCrop(context: Context, slot: CustomNavigationIconSlot, crop: CustomWallpaperCrop) {
    customNavigationIconPrefs(context).edit(commit = true) {
        putCustomNavigationIconCrop(slot, crop)
    }
}

fun setCustomNavigationIconPresentation(
    context: Context,
    slot: CustomNavigationIconSlot,
    state: CustomNavigationIconState,
) {
    val value = state.normalized()
    customNavigationIconPrefs(context).edit(commit = true) {
        putFloat(slot.sizeScaleKey, value.sizeScale)
        putFloat(slot.innerPaddingKey, value.innerPaddingDp)
        putFloat(slot.verticalOffsetKey, value.verticalOffsetDp)
        putFloat(slot.opacityKey, value.opacity)
        if (value.tintArgb == null) remove(slot.tintArgbKey) else putLong(slot.tintArgbKey, value.tintArgb)
        putString(slot.maskKey, value.mask.value)
        if (value.labelOverride == null) remove(slot.labelKey) else putString(slot.labelKey, value.labelOverride)
    }
}

internal fun SharedPreferences.readCustomNavigationIconSet(): CustomNavigationIconSet {
    return CustomNavigationIconSet(
        home = readCustomNavigationIconState(CustomNavigationIconSlot.Home),
        kpm = readCustomNavigationIconState(CustomNavigationIconSlot.Kpm),
        superuser = readCustomNavigationIconState(CustomNavigationIconSlot.Superuser),
        module = readCustomNavigationIconState(CustomNavigationIconSlot.Module),
        settings = readCustomNavigationIconState(CustomNavigationIconSlot.Settings),
    )
}

internal fun SharedPreferences.readCustomNavigationIconState(
    slot: CustomNavigationIconSlot,
): CustomNavigationIconState {
    return CustomNavigationIconState(
        uriString = getString(slot.uriKey, null),
        crop = sanitizeCustomWallpaperCrop(
            CustomWallpaperCrop(
                left = getFloat(slot.cropLeftKey, DEFAULT_CUSTOM_NAVIGATION_ICON_CROP.left),
                top = getFloat(slot.cropTopKey, DEFAULT_CUSTOM_NAVIGATION_ICON_CROP.top),
                right = getFloat(slot.cropRightKey, DEFAULT_CUSTOM_NAVIGATION_ICON_CROP.right),
                bottom = getFloat(slot.cropBottomKey, DEFAULT_CUSTOM_NAVIGATION_ICON_CROP.bottom),
            )
        ),
        sizeScale = getFloat(slot.sizeScaleKey, 1f),
        innerPaddingDp = getFloat(slot.innerPaddingKey, 0f),
        verticalOffsetDp = getFloat(slot.verticalOffsetKey, 0f),
        opacity = getFloat(slot.opacityKey, 1f),
        tintArgb = if (contains(slot.tintArgbKey)) getLong(slot.tintArgbKey, 0L) else null,
        mask = CustomNavigationIconMask.fromValue(getString(slot.maskKey, null)),
        labelOverride = getString(slot.labelKey, null),
    ).normalized()
}

internal fun SharedPreferences.Editor.putCustomNavigationIconCrop(
    slot: CustomNavigationIconSlot,
    crop: CustomWallpaperCrop,
) {
    val safeCrop = sanitizeCustomWallpaperCrop(crop)
    putFloat(slot.cropLeftKey, safeCrop.left)
    putFloat(slot.cropTopKey, safeCrop.top)
    putFloat(slot.cropRightKey, safeCrop.right)
    putFloat(slot.cropBottomKey, safeCrop.bottom)
}

internal fun SharedPreferences.Editor.removeCustomNavigationIconCrop(slot: CustomNavigationIconSlot) {
    remove(slot.cropLeftKey)
    remove(slot.cropTopKey)
    remove(slot.cropRightKey)
    remove(slot.cropBottomKey)
}

private fun customNavigationIconPrefs(context: Context): SharedPreferences {
    return context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
}
