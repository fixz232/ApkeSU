package me.weishu.kernelsu.ui.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ForegroundToolProtectionViewModelTest {
    @Test
    fun choosingTargetRemovesSamePackageFromTools() {
        val selection = updateForegroundToolSelection(
            targets = emptySet(),
            tools = setOf("com.example.shared", "com.example.tool"),
            packageName = "com.example.shared",
            role = ForegroundToolRole.Target,
            selected = true,
        )

        assertEquals(setOf("com.example.shared"), selection.targets)
        assertEquals(setOf("com.example.tool"), selection.tools)
        assertEquals(ForegroundToolRole.Tool, selection.conflictRemovedFrom)
    }

    @Test
    fun choosingToolRemovesSamePackageFromTargets() {
        val selection = updateForegroundToolSelection(
            targets = setOf("com.example.shared", "com.example.target"),
            tools = emptySet(),
            packageName = "com.example.shared",
            role = ForegroundToolRole.Tool,
            selected = true,
        )

        assertEquals(setOf("com.example.target"), selection.targets)
        assertEquals(setOf("com.example.shared"), selection.tools)
        assertEquals(ForegroundToolRole.Target, selection.conflictRemovedFrom)
    }

    @Test
    fun filteringKeepsMissingAndSelectedSystemEntriesRemovable() {
        val visible = filterForegroundToolApps(
            apps = listOf(
                ForegroundToolApp("com.example.user", "User app"),
                ForegroundToolApp("com.example.system", "System app", isSystem = true),
                ForegroundToolApp("com.example.hidden", "Hidden system", isSystem = true),
            ),
            selectedPackages = setOf("com.example.system", "com.example.missing"),
            query = "",
            showSystemApps = false,
        )

        assertTrue(visible.any { it.packageName == "com.example.user" })
        assertTrue(visible.any { it.packageName == "com.example.system" })
        assertTrue(visible.any { it.packageName == "com.example.missing" && !it.installed })
        assertFalse(visible.any { it.packageName == "com.example.hidden" })
    }
}
