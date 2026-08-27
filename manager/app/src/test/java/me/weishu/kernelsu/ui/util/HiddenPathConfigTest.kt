package me.weishu.kernelsu.ui.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HiddenPathConfigTest {
    @Test
    fun editableComparisonIgnoresRuntimeStatus() {
        val saved = HiddenPathConfigState(
            targetPaths = listOf("/data/local/tmp/example"),
            appPackages = listOf("12345"),
            loaded = false,
            currentKmi = "android14-6.1",
        )
        val runtimeUpdated = saved.copy(
            loaded = true,
            resolvedCount = "1",
            activeTargetPaths = "/data/local/tmp/example",
            lastLog = "loaded",
        )

        assertTrue(saved.editableEquals(runtimeUpdated))
    }

    @Test
    fun editableComparisonDetectsPendingPathAndOptionChanges() {
        val saved = HiddenPathConfigState(
            targetPaths = listOf("/data/local/tmp/example"),
            appPackages = listOf("12345"),
        )

        assertFalse(
            saved.editableEquals(
                saved.copy(targetPaths = listOf("/data/local/tmp/other")),
            ),
        )
        assertFalse(saved.editableEquals(saved.copy(hideDirents = false)))
        assertFalse(saved.editableEquals(saved.copy(appPackages = listOf("54321"))))
    }

    @Test
    fun importedConfigPreservesRuntimeFieldsUntilApplied() {
        val current = HiddenPathConfigState(
            loaded = true,
            currentKmi = "android14-6.1",
            resolvedCount = "1",
        )
        val imported = parseHiddenPathConfigJson(
            """
            {
              "targetPaths": ["/data/local/tmp/example"],
              "appPackages": ["12345"],
              "useAppScope": true,
              "hideDirents": false,
              "hideIsolated": true
            }
            """.trimIndent(),
            current,
        )

        assertTrue(imported.loaded)
        assertTrue(imported.currentKmi == "android14-6.1")
        assertTrue(imported.targetPaths == listOf("/data/local/tmp/example"))
        assertFalse(imported.hideDirents)
    }
}
