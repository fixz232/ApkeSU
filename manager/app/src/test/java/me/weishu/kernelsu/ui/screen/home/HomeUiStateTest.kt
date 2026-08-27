package me.weishu.kernelsu.ui.screen.home

import me.weishu.kernelsu.KernelVersion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeUiStateTest {
    @Test
    fun rootRuntimeStateKeepsEachFailureDistinct() {
        assertEquals(
            RootRuntimeState.DriverDisconnected,
            RootRuntimeState.resolve(
                driverConnected = false,
                managerRegistered = false,
                daemonRootAvailable = false,
                blockingVersionMismatch = false,
            ),
        )
        assertEquals(
            RootRuntimeState.ManagerUnregistered,
            RootRuntimeState.resolve(
                driverConnected = true,
                managerRegistered = false,
                daemonRootAvailable = true,
                blockingVersionMismatch = false,
            ),
        )
        assertEquals(
            RootRuntimeState.DaemonError,
            RootRuntimeState.resolve(
                driverConnected = true,
                managerRegistered = true,
                daemonRootAvailable = false,
                blockingVersionMismatch = false,
            ),
        )
        assertEquals(
            RootRuntimeState.VersionMismatch,
            RootRuntimeState.resolve(
                driverConnected = true,
                managerRegistered = true,
                daemonRootAvailable = true,
                blockingVersionMismatch = true,
            ),
        )
        assertEquals(
            RootRuntimeState.Running,
            RootRuntimeState.resolve(
                driverConnected = true,
                managerRegistered = true,
                daemonRootAvailable = true,
                blockingVersionMismatch = false,
            ),
        )
    }

    @Test
    fun incompatibleKernelOrOlderManagerBlocksRunningState() {
        assertTrue(
            hasBlockingRootVersionMismatch(
                managerVersionCode = 32699,
                driverVersion = 32700,
                requiresNewKernel = false,
                uapiMismatch = false,
            )
        )
        assertTrue(
            hasBlockingRootVersionMismatch(
                managerVersionCode = 32700,
                driverVersion = 32700,
                requiresNewKernel = true,
                uapiMismatch = false,
            )
        )
        assertTrue(
            hasBlockingRootVersionMismatch(
                managerVersionCode = 32700,
                driverVersion = 32700,
                requiresNewKernel = false,
                uapiMismatch = true,
            )
        )
        assertFalse(
            hasBlockingRootVersionMismatch(
                managerVersionCode = 32700,
                driverVersion = 32699,
                requiresNewKernel = false,
                uapiMismatch = false,
            )
        )
    }

    @Test
    fun uapiMismatchDoesNotAlsoClaimKernelVersionIsTooLow() {
        val mismatch = homeState(
            requiresNewKernel = true,
            uapiMismatch = true,
        )

        assertTrue(mismatch.showUAPIMisMatchWarning)
        assertFalse(mismatch.showRequireKernelWarning)

        val oldKernel = homeState(
            requiresNewKernel = true,
            uapiMismatch = false,
        )

        assertFalse(oldKernel.showUAPIMisMatchWarning)
        assertTrue(oldKernel.showRequireKernelWarning)
    }

    private fun homeState(
        requiresNewKernel: Boolean,
        uapiMismatch: Boolean,
    ) = HomeUiState(
        kernelVersion = KernelVersion(6, 1, 0),
        ksuVersion = 32720,
        managerUAPIVersion = 2,
        kernelUAPIVersion = if (uapiMismatch) 1 else 2,
        lkmMode = true,
        isManager = true,
        isManagerPrBuild = false,
        isKernelPrBuild = false,
        requiresNewKernel = requiresNewKernel,
        uapiMismatch = uapiMismatch,
        isRootAvailable = true,
        isSafeMode = false,
        isLateLoadMode = false,
        currentManagerVersionCode = 32720,
        showVersionMismatchWarningSetting = true,
        superuserCount = 0,
        moduleCount = 0,
        systemInfo = SystemInfo(
            kernelVersion = "6.1.0",
            managerVersion = "2.7.1 (32720)",
            deviceModel = "test",
            fingerprint = "test",
            selinuxStatus = "Enforcing",
            seccompStatus = 2,
        ),
    )
}
