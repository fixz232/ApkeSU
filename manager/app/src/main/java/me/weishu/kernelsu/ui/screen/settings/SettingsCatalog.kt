package me.weishu.kernelsu.ui.screen.settings

import android.content.Context
import androidx.annotation.StringRes
import androidx.core.content.edit
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.InterfaceStyle

private const val SETTINGS_NAVIGATION_PREFS = "settings_navigation"
private const val SETTINGS_LAST_CATEGORY_KEY = "last_category"
private const val SETTINGS_RECENT_CATEGORIES_KEY = "recent_categories"
private const val MAX_RECENT_SETTINGS_CATEGORIES = 3

internal data class SettingsCatalogEntry(
    val key: String,
    val category: SettingsCategory,
    @StringRes val titleRes: Int,
    @StringRes val summaryRes: Int? = null,
    val visible: (SettingsUiState) -> Boolean = { true },
)

internal object SettingsCatalog {
    private val entries = listOf(
        entry("ui_style", SettingsCategory.Appearance, R.string.settings_ui_mode, R.string.settings_ui_mode_summary),
        entry("miuix_classic", SettingsCategory.Appearance, R.string.settings_miuix_classic_home_layout, R.string.settings_miuix_classic_home_layout_summary) {
            it.uiMode == InterfaceStyle.Miuix.value
        },
        entry("alpha_delta", SettingsCategory.Appearance, R.string.settings_alpha_delta_mode, R.string.settings_alpha_delta_mode_summary) {
            it.uiMode == InterfaceStyle.Alpha.value || it.uiMode == InterfaceStyle.Delta.value
        },
        entry("season_style", SettingsCategory.Appearance, R.string.settings_season_style) {
            it.uiMode == InterfaceStyle.Snow.value
        },
        entry("rain_style", SettingsCategory.Appearance, R.string.interface_style_rain) {
            it.uiMode == InterfaceStyle.Rain.value
        },
        entry("pixel_style", SettingsCategory.Appearance, R.string.settings_pixel_style) {
            it.uiMode == InterfaceStyle.Pixel.value
        },
        entry("day_night", SettingsCategory.Appearance, R.string.settings_day_night_switch, R.string.settings_day_night_switch_summary),
        entry("theme_store", SettingsCategory.Appearance, R.string.theme_store, R.string.theme_store_settings_summary),

        entry("manager_identity", SettingsCategory.HomeAndManager, R.string.settings_manager_identity, R.string.settings_manager_identity_summary),
        entry("dynamic_manager", SettingsCategory.HomeAndManager, R.string.dynamic_manager_title, R.string.dynamic_manager_settings_summary),
        entry("home_title", SettingsCategory.HomeAndManager, R.string.settings_home_title, R.string.settings_home_title_default_summary),
        entry("home_layout", SettingsCategory.HomeAndManager, R.string.home_layout_title, R.string.home_layout_settings_summary),
        entry("pixel_pet", SettingsCategory.HomeAndManager, R.string.pixel_pet_title),
        entry("support_card", SettingsCategory.HomeAndManager, R.string.settings_show_home_support_card, R.string.settings_show_home_support_card_summary),
        entry("learn_card", SettingsCategory.HomeAndManager, R.string.settings_show_home_learn_card, R.string.settings_show_home_learn_card_summary),

        entry("profile_template", SettingsCategory.RootAndPermissions, R.string.settings_profile_template, R.string.settings_profile_template_summary),
        entry("su_compat", SettingsCategory.RootAndPermissions, R.string.settings_sucompat, R.string.settings_sucompat_summary),
        entry("kernel_umount", SettingsCategory.RootAndPermissions, R.string.settings_kernel_umount, R.string.settings_kernel_umount_summary),
        entry("webview_umount", SettingsCategory.RootAndPermissions, R.string.settings_webview_zygote_umount, R.string.settings_webview_zygote_umount_summary),
        entry("selinux_hide", SettingsCategory.RootAndPermissions, R.string.settings_selinux_hide, R.string.settings_selinux_hide_summary),
        entry("su_log", SettingsCategory.RootAndPermissions, R.string.settings_sulog, R.string.settings_sulog_summary),
        entry("adb_root", SettingsCategory.RootAndPermissions, R.string.settings_adb_root, R.string.settings_adb_root_summary),
        entry("avc_spoof", SettingsCategory.RootAndPermissions, R.string.settings_avc_spoof, R.string.settings_avc_spoof_summary),
        entry("soft_reboot", SettingsCategory.RootAndPermissions, R.string.settings_soft_reboot, R.string.settings_soft_reboot_summary),

        entry("default_umount", SettingsCategory.MountAndHide, R.string.settings_umount_modules_default, R.string.settings_umount_modules_default_summary),
        entry("builtin_mount", SettingsCategory.MountAndHide, R.string.settings_builtin_mount, R.string.settings_builtin_mount_summary),
        entry("kpatch_next", SettingsCategory.MountAndHide, R.string.settings_kpatch_next, R.string.settings_kpatch_next_summary),
        entry("kpatch_webui", SettingsCategory.MountAndHide, R.string.settings_kpatch_next_webui, R.string.settings_kpatch_next_webui_disabled_summary),
        entry("path_config", SettingsCategory.MountAndHide, R.string.hidden_path_lkm_builtin_title),
        entry("apkesu_hide", SettingsCategory.MountAndHide, R.string.settings_epkesu_hide, R.string.settings_epkesu_hide_summary),

        entry("rescue", SettingsCategory.Toolbox, R.string.rescue_protection, R.string.rescue_protection_summary),
        entry("image_tool", SettingsCategory.Toolbox, R.string.image_tool_title, R.string.image_tool_settings_summary),
        entry("cpu_spoof", SettingsCategory.Toolbox, R.string.settings_cpu_spoof, R.string.settings_cpu_spoof_summary),
        entry("device_identity", SettingsCategory.Toolbox, R.string.settings_device_identity, R.string.settings_device_identity_summary),
        entry("ai_chat", SettingsCategory.Toolbox, R.string.settings_ai_chat, R.string.settings_ai_chat_summary),
        entry("graphics_toggle", SettingsCategory.Toolbox, R.string.settings_graphics_renderer_tool, R.string.settings_graphics_renderer_tool_summary),
        entry("graphics_renderer", SettingsCategory.Toolbox, R.string.settings_graphics_renderer, R.string.settings_graphics_renderer_summary) {
            it.graphicsRendererFeatureEnabled
        },
        entry("kpm", SettingsCategory.Toolbox, R.string.kpm_title, R.string.kpm_settings_summary) {
            it.isKpmSettingsEntryVisible
        },

        entry("language", SettingsCategory.AppAndMaintenance, R.string.settings_language, R.string.settings_language_summary),
        entry("module_updates", SettingsCategory.AppAndMaintenance, R.string.settings_module_check_update, R.string.settings_module_check_update_summary),
        entry("version_warning", SettingsCategory.AppAndMaintenance, R.string.settings_version_mismatch_warning, R.string.settings_version_mismatch_warning_summary),
        entry("gki_warning", SettingsCategory.AppAndMaintenance, R.string.settings_gki_warning, R.string.settings_gki_warning_summary),
        entry("web_debugging", SettingsCategory.AppAndMaintenance, R.string.enable_web_debugging, R.string.enable_web_debugging_summary),
        entry("auto_jailbreak", SettingsCategory.AppAndMaintenance, R.string.settings_auto_jailbreak, R.string.settings_auto_jailbreak_summary),
        entry("uninstall", SettingsCategory.AppAndMaintenance, R.string.settings_uninstall) { it.isLkmMode },
        entry("send_log", SettingsCategory.AppAndMaintenance, R.string.send_log),
        entry("about", SettingsCategory.AppAndMaintenance, R.string.about),
    )

    fun entriesFor(category: SettingsCategory, state: SettingsUiState): List<SettingsCatalogEntry> =
        entries.filter { it.category == category && it.visible(state) }

    fun visibleEntryCount(category: SettingsCategory, state: SettingsUiState): Int =
        entriesFor(category, state).size

    fun categoryMatches(
        context: Context,
        category: SettingsCategory,
        state: SettingsUiState,
        query: String,
    ): Boolean {
        val normalized = query.trim()
        if (normalized.isEmpty()) return true
        if (context.getString(category.titleRes).contains(normalized, ignoreCase = true) ||
            context.getString(category.summaryRes).contains(normalized, ignoreCase = true)
        ) {
            return true
        }
        return entriesFor(category, state).any { item ->
            context.getString(item.titleRes).contains(normalized, ignoreCase = true) ||
                item.summaryRes?.let(context::getString)?.contains(normalized, ignoreCase = true) == true
        }
    }

    private fun entry(
        key: String,
        category: SettingsCategory,
        @StringRes titleRes: Int,
        @StringRes summaryRes: Int? = null,
        visible: (SettingsUiState) -> Boolean = { true },
    ) = SettingsCatalogEntry(key, category, titleRes, summaryRes, visible)
}

internal val SettingsUiState.isKpmSettingsEntryVisible: Boolean
    get() = isKPatchNextEnabled && !isKPatchNextPendingRemove && !isLateLoadMode

internal fun readLastSettingsCategory(context: Context): SettingsCategory? {
    val route = context.applicationContext
        .getSharedPreferences(SETTINGS_NAVIGATION_PREFS, Context.MODE_PRIVATE)
        .getString(SETTINGS_LAST_CATEGORY_KEY, null)
    return route?.let(SettingsCategory::fromRouteValue)
}

internal fun readRecentSettingsCategories(context: Context): List<SettingsCategory> {
    val routes = context.applicationContext
        .getSharedPreferences(SETTINGS_NAVIGATION_PREFS, Context.MODE_PRIVATE)
        .getString(SETTINGS_RECENT_CATEGORIES_KEY, "")
        .orEmpty()
        .split(',')
        .filter(String::isNotBlank)
    return routes.map(SettingsCategory::fromRouteValue).distinct()
}

internal fun recordSettingsCategoryInteraction(context: Context, category: SettingsCategory) {
    val recent = (listOf(category) + readRecentSettingsCategories(context))
        .distinct()
        .take(MAX_RECENT_SETTINGS_CATEGORIES)
    context.applicationContext
        .getSharedPreferences(SETTINGS_NAVIGATION_PREFS, Context.MODE_PRIVATE)
        .edit {
            putString(SETTINGS_LAST_CATEGORY_KEY, category.routeValue)
            putString(SETTINGS_RECENT_CATEGORIES_KEY, recent.joinToString(",") { it.routeValue })
        }
}
