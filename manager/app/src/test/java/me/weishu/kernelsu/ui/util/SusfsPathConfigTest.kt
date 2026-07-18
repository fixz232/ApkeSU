package me.weishu.kernelsu.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SusfsPathConfigTest {
    @Test
    fun normalizesAbsolutePathsWithoutChangingTheirContents() {
        assertEquals("/data/local/tmp/example", normalizeSusfsPath("  /data/local/tmp/example/  "))
        assertEquals("/storage/emulated/0/Android/data/example", normalizeSusfsPath("/storage/emulated/0/Android/data/example"))
        assertEquals("/data/adb/custom", normalizeSusfsPath("/data/adb/custom"))
    }

    @Test
    fun rejectsRootRelativeAndControlCharacterPaths() {
        assertNull(normalizeSusfsPath("/"))
        assertNull(normalizeSusfsPath("data/local/tmp"))
        assertNull(normalizeSusfsPath("/data/local/tmp\nnext"))
        assertNull(normalizeSusfsPath("/data/adb/modules"))
        assertNull(normalizeSusfsPath("/data/adb/ksu/bin"))
        assertNull(normalizeSusfsPath("/data/adb/ap"))
        assertNull(normalizeSusfsPath(""))
    }
}
