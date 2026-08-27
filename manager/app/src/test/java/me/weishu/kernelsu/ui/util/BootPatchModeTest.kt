package me.weishu.kernelsu.ui.util

import org.junit.Assert.assertEquals
import org.junit.Test

class BootPatchModeTest {
    @Test
    fun normalPatchAddsNoSpecialArguments() {
        assertEquals("", BootPatchMode.Normal.cliArguments())
    }

    @Test
    fun hiddenPathPatchAddsOnlyPathmaskArgument() {
        assertEquals(" --pathmask-lkm", BootPatchMode.HiddenPath.cliArguments())
    }
}
