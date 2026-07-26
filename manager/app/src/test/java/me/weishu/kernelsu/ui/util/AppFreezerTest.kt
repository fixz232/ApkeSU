package me.weishu.kernelsu.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class AppFreezerTest {
    @Test
    fun freezeCommandsTargetOnePackageAndAndroidUser() {
        val key = AppFreezeKey("com.example.game", 10)

        assertEquals(
            "cmd package suspend --user 10 'com.example.game'",
            buildAppFreezeCommand(key, frozen = true),
        )
        assertEquals(
            "cmd package unsuspend --user 10 'com.example.game'",
            buildAppFreezeCommand(key, frozen = false),
        )
        assertEquals(
            "am force-stop --user 10 'com.example.game'",
            buildAppForceStopCommand(key),
        )
    }

    @Test
    fun freezeCommandRejectsShellInjectionAndInvalidUsers() {
        assertThrows(AppFreezeException::class.java) {
            buildAppFreezeCommand(AppFreezeKey("com.example.app;reboot", 0), true)
        }
        assertThrows(AppFreezeException::class.java) {
            buildAppFreezeCommand(AppFreezeKey("com.example.app", -1), true)
        }
    }

    @Test
    fun commandOutputIsParsedOnlyForTheRequestedPackage() {
        val output = listOf(
            "Package com.example.first new suspended state: false",
            "Package com.example.second new suspended state: true",
        )

        assertEquals(true, parseAppFreezeState(output, "com.example.second"))
        assertEquals(false, parseAppFreezeState(output, "com.example.first"))
        assertNull(parseAppFreezeState(output, "com.example.missing"))
    }

    @Test
    fun managerLauncherAndCriticalPackagesAreProtected() {
        assertEquals(
            AppFreezeProtection.Manager,
            resolveAppFreezeProtection(
                packageName = "io.github.fixz.apkesu",
                uid = 10_321,
                managerPackage = "io.github.fixz.apkesu",
                launcherPackages = emptySet(),
            ),
        )
        assertEquals(
            AppFreezeProtection.Launcher,
            resolveAppFreezeProtection(
                packageName = "com.example.launcher",
                uid = 10_322,
                managerPackage = "io.github.fixz.apkesu",
                launcherPackages = setOf("com.example.launcher"),
            ),
        )
        assertEquals(
            AppFreezeProtection.CriticalSystem,
            resolveAppFreezeProtection(
                packageName = "com.android.systemui",
                uid = 1_000,
                managerPackage = "io.github.fixz.apkesu",
                launcherPackages = emptySet(),
            ),
        )
        assertEquals(
            AppFreezeProtection.CoreUid,
            resolveAppFreezeProtection(
                packageName = "com.vendor.core",
                uid = 2_000,
                managerPackage = "io.github.fixz.apkesu",
                launcherPackages = emptySet(),
            ),
        )
        assertNull(
            resolveAppFreezeProtection(
                packageName = "com.example.normal",
                uid = 10_500,
                managerPackage = "io.github.fixz.apkesu",
                launcherPackages = emptySet(),
            )
        )
    }
}
