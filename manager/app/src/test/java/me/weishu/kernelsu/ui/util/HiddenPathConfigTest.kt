package me.weishu.kernelsu.ui.util

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HiddenPathConfigTest {
    @Test
    fun editableComparisonIgnoresRuntimeStatus() {
        val saved = HiddenPathConfigState(
            targetPaths = listOf("/data/local/tmp/example"),
            appPackages = listOf("com.example.app"),
            loaded = false,
            currentKmi = "android14-6.1",
        )
        val runtimeUpdated = saved.copy(
            loaded = true,
            resolvedCount = 1,
            activeTargetPaths = "/data/local/tmp/example",
            lastLog = "loaded",
        )

        assertTrue(saved.editableEquals(runtimeUpdated))
    }

    @Test
    fun editableComparisonDetectsPendingPathAndOptionChanges() {
        val saved = HiddenPathConfigState(
            targetPaths = listOf("/data/local/tmp/example"),
            appPackages = listOf("com.example.app"),
        )

        assertFalse(
            saved.editableEquals(
                saved.copy(targetPaths = listOf("/data/local/tmp/other")),
            ),
        )
        assertFalse(saved.editableEquals(saved.copy(hideDirents = false)))
        assertFalse(saved.editableEquals(saved.copy(appPackages = listOf("com.example.other"))))
        assertFalse(saved.editableEquals(saved.copy(autoLoadDelaySeconds = 15)))
    }

    @Test
    fun importedConfigPreservesRuntimeFieldsUntilApplied() {
        val current = HiddenPathConfigState(
            loaded = true,
            currentKmi = "android14-6.1",
            resolvedCount = 1,
            autoLoadDelaySeconds = 45,
        )
        val imported = parseHiddenPathConfigJson(
            """
            {
              "targetPaths": ["/data/local/tmp/example"],
              "appPackages": ["com.example.app"],
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
        assertEquals(0, imported.autoLoadDelaySeconds)
    }

    @Test
    fun autoLoadDelayRoundTripsThroughExportAndImport() {
        val original = HiddenPathConfigState(
            targetPaths = listOf("/data/local/tmp/example"),
            appPackages = listOf("com.example.app"),
            autoLoadDelaySeconds = 45,
        )

        val exported = original.toConfigJson()
        val imported = parseHiddenPathConfigJson(exported)

        assertEquals(3, JSONObject(exported).getInt("schemaVersion"))
        assertEquals(45, imported.autoLoadDelaySeconds)
    }

    @Test
    fun autoLoadDelayOutsideSupportedRangeIsRejected() {
        val result = runCatching {
            parseHiddenPathConfigJson(
                """
                {
                  "targetPaths": ["/data/local/tmp/example"],
                  "appPackages": ["com.example.app"],
                  "autoLoadDelaySeconds": ${HIDDEN_PATH_MAX_AUTO_LOAD_DELAY_SECONDS + 1}
                }
                """.trimIndent(),
            )
        }

        assertTrue(result.isFailure)

        assertTrue(
            runCatching {
                parseHiddenPathConfigJson(
                    """
                    {
                      "targetPaths": ["/data/local/tmp/example"],
                      "appPackages": ["com.example.app"],
                      "autoLoadDelaySeconds": -1
                    }
                    """.trimIndent(),
                )
            }.isFailure,
        )
    }

    @Test
    fun partialRuntimeCountsMissingAndUnresolvedTargets() {
        val partial = HiddenPathConfigState(
            loaded = true,
            phase = "partial",
            missingTargetPaths = listOf("/system/bin/su", "/vendor/bin/su"),
            unresolvedTargetCount = 1,
        )

        assertTrue(partial.isPartial)
        assertEquals(2, partial.pendingTargetCount)
        assertEquals(3, partial.notEffectiveTargetCount)
    }

    @Test
    fun unloadedRuntimeCountsEverySavedTargetAsNotEffective() {
        val unloaded = HiddenPathConfigState(
            loaded = false,
            savedCount = 12,
            availableCount = 5,
            activeCount = 0,
            missingTargetPaths = List(7) { "/missing/$it" },
            unresolvedTargetCount = 5,
        )

        assertEquals(12, unloaded.notEffectiveTargetCount)
    }

    @Test
    fun statusParserAcceptsShellNoiseAndCompatibleFieldTypes() {
        val result = parseHiddenPathStatusJson(
            """
            shell notice with {non-json braces}
            device:/ # {"targetPaths":["/data/adb/ksu"],"appPackages":["com.example.app"],"loaded":true,"phase":"active","savedCount":"1","availableCount":1,"activeCount":"1","resolvedCount":"1","activeTargetPaths":["/data/adb/ksu"],"lastLog":"kept {inside} a JSON string"}device:/ #
            """.trimIndent(),
        )

        val config = requireNotNull(result.config)
        assertEquals("", result.error)
        assertEquals(1, config.savedCount)
        assertEquals(1, config.resolvedCount)
        assertEquals("/data/adb/ksu", config.activeTargetPaths)
    }

    @Test
    fun malformedStatusReturnsStructuredParseFailure() {
        val result = parseHiddenPathStatusJson("not JSON")

        assertEquals(null, result.config)
        assertEquals("pathmask.status_parse_failed", result.errorCode)
        assertTrue(result.error.isNotBlank())
    }
}
