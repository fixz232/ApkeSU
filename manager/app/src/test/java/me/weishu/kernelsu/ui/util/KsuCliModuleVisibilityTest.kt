package me.weishu.kernelsu.ui.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KsuCliModuleVisibilityTest {
    @Test
    fun kpatchNextIsHiddenFromManagerModuleSurfaces() {
        assertTrue(isManagerHiddenModuleId(KPATCH_NEXT_MODULE_ID))
        assertTrue(isManagerHiddenModuleId("kpatch-next"))
        assertFalse(isManagerHiddenModuleId("user-module"))
    }
}
