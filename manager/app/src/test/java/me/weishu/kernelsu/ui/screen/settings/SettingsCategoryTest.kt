package me.weishu.kernelsu.ui.screen.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsCategoryTest {
    @Test
    fun routeValuesRestoreEveryCategory() {
        SettingsCategory.entries.forEach { category ->
            assertEquals(category, SettingsCategory.fromRouteValue(category.routeValue))
        }
    }

    @Test
    fun missingOrUnknownRouteFallsBackToAppearance() {
        assertEquals(SettingsCategory.Appearance, SettingsCategory.fromRouteValue(null))
        assertEquals(SettingsCategory.Appearance, SettingsCategory.fromRouteValue("unknown"))
    }

    @Test
    fun settingsPageModeRestoresSavedValueAndDefaultsToCategories() {
        SettingsPageMode.entries.forEach { mode ->
            assertEquals(mode, SettingsPageMode.fromValue(mode.value))
        }
        assertEquals(SettingsPageMode.Categories, SettingsPageMode.fromValue(null))
        assertEquals(SettingsPageMode.Categories, SettingsPageMode.fromValue("unknown"))
    }
}
