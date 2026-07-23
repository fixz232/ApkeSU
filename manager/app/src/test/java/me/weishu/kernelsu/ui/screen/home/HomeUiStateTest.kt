package me.weishu.kernelsu.ui.screen.home

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
}
