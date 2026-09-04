package me.weishu.kernelsu.ui.screen.settings

import me.weishu.kernelsu.ui.InterfaceStyle
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsCatalogTest {
    @Test
    fun toolboxCountTracksDynamicEntries() {
        val base = SettingsUiState(uiMode = InterfaceStyle.Material.value)
        assertEquals(6, SettingsCatalog.visibleEntryCount(SettingsCategory.Toolbox, base))
        assertEquals(
            7,
            SettingsCatalog.visibleEntryCount(
                SettingsCategory.Toolbox,
                base.copy(graphicsRendererFeatureEnabled = true),
            ),
        )
        assertEquals(
            7,
            SettingsCatalog.visibleEntryCount(
                SettingsCategory.Toolbox,
                base.copy(isKPatchNextEnabled = true),
            ),
        )
    }

    @Test
    fun appearanceCountTracksInterfaceSpecificEntry() {
        val base = SettingsUiState(uiMode = InterfaceStyle.Material.value)
        assertEquals(3, SettingsCatalog.visibleEntryCount(SettingsCategory.Appearance, base))
        assertEquals(
            4,
            SettingsCatalog.visibleEntryCount(
                SettingsCategory.Appearance,
                base.copy(uiMode = InterfaceStyle.Miuix.value),
            ),
        )
    }

    @Test
    fun rootCountIncludesSoftReboot() {
        assertEquals(
            9,
            SettingsCatalog.visibleEntryCount(SettingsCategory.RootAndPermissions, SettingsUiState()),
        )
    }

    @Test
    fun homeAndManagerCountIncludesDynamicManager() {
        assertEquals(
            6,
            SettingsCatalog.visibleEntryCount(SettingsCategory.HomeAndManager, SettingsUiState()),
        )
    }
}
