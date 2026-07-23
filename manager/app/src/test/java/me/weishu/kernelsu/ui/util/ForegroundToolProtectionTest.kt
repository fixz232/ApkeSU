package me.weishu.kernelsu.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ForegroundToolProtectionTest {
    @Test
    fun packageValidationRejectsShellInputAndMalformedNames() {
        assertTrue(isValidForegroundToolPackage("io.github.fixz.apkesu"))
        assertTrue(isValidForegroundToolPackage("github.ColdAsSunny.Kernel"))
        assertFalse(isValidForegroundToolPackage("apkesu"))
        assertFalse(isValidForegroundToolPackage("1bad.package"))
        assertFalse(isValidForegroundToolPackage("com.example.app;reboot"))
        assertFalse(isValidForegroundToolPackage("com.example.app\nreboot"))
    }

    @Test
    fun normalizationTrimsDeduplicatesAndDropsInvalidEntries() {
        assertEquals(
            linkedSetOf("com.example.first", "com.example.second"),
            normalizeForegroundToolPackages(
                listOf(
                    " com.example.first ",
                    "invalid",
                    "com.example.first",
                    "com.example.second",
                ),
            ),
        )
    }

    @Test
    fun propertyParserKeepsValuesContainingEqualsAndIgnoresInvalidKeys() {
        assertEquals(
            mapOf("version" to "2", "state" to "active", "event" to "target=entered"),
            parseForegroundToolProperties(
                listOf("version=2", "state=active", "event=target=entered", "BAD=value", "missing_separator"),
            ),
        )
    }

    @Test
    fun enabledLegacyServiceIsUpgradedWithoutTouchingDisabledConfig() {
        val enabledConfig = ForegroundToolConfig(
            enabled = true,
            targets = setOf("com.example.target"),
            tools = setOf("com.example.tool"),
        )
        assertTrue(
            shouldUpgradeForegroundToolService(
                ForegroundToolStatus(
                    config = enabledConfig,
                    configPresent = true,
                    serviceVersion = 0,
                ),
            ),
        )
        assertFalse(
            shouldUpgradeForegroundToolService(
                ForegroundToolStatus(
                    config = enabledConfig,
                    configPresent = true,
                    serviceVersion = 2,
                ),
            ),
        )
        assertFalse(
            shouldUpgradeForegroundToolService(
                ForegroundToolStatus(
                    config = enabledConfig.copy(enabled = false),
                    configPresent = true,
                    serviceVersion = 0,
                ),
            ),
        )
    }

    @Test
    fun enableValidationRequiresBothRolesWithoutOverlap() {
        assertEquals(
            ForegroundToolFailure.TargetRequired,
            validateForegroundToolEnable(emptySet(), setOf("com.example.tool")),
        )
        assertEquals(
            ForegroundToolFailure.ToolRequired,
            validateForegroundToolEnable(setOf("com.example.target"), emptySet()),
        )
        assertEquals(
            ForegroundToolFailure.SelectionConflict,
            validateForegroundToolEnable(setOf("com.example.same"), setOf("com.example.same")),
        )
        assertNull(
            validateForegroundToolEnable(
                setOf("com.example.target"),
                setOf("com.example.tool"),
            ),
        )
    }

    @Test
    fun installScriptStreamsConfigWithoutReadingAppPrivateCache() {
        val script = buildForegroundToolInstallScript(
            targetsText = "com.example.target\n",
            toolsText = "com.example.tool\n",
            serviceText = "#!/system/bin/sh\nexit 0\n",
        )

        assertFalse(script.contains("/data/user/"))
        assertFalse(script.contains("/cache/foreground-tool-protection"))
        assertTrue(script.contains("cat > '/data/adb/apkesu/foreground_tools/targets.list.tmp'"))
        assertTrue(script.contains("cat > '/data/adb/service.d/97-apkesu-foreground-tools.sh.tmp'"))
        assertTrue(script.contains("com.example.target"))
        assertTrue(script.contains("__APKESU_FOREGROUND_TOOL_INSTALL_OK__"))
    }

    @Test
    fun installScriptPreservesEmptyDisabledConfigurationFiles() {
        val script = buildForegroundToolInstallScript(
            targetsText = "",
            toolsText = "",
            serviceText = "#!/system/bin/sh\nexit 0\n",
        )

        assertTrue(script.contains(": > '/data/adb/apkesu/foreground_tools/targets.list.tmp'"))
        assertTrue(script.contains(": > '/data/adb/apkesu/foreground_tools/tools.list.tmp'"))
        assertTrue(script.contains("[ \"${'$'}actual_bytes\" -eq 0 ]"))
    }
}
