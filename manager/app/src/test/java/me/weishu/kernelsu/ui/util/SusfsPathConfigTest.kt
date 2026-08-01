package me.weishu.kernelsu.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    @Test
    fun bootServiceWaitsForSusfsAndRetriesFailedRestores() {
        val service = susfsPathServiceScript()

        assertTrue(service.contains("find_tool()"))
        assertTrue(service.contains("while [ \"\$attempt\" -lt 30 ]; do"))
        assertTrue(service.contains("add_sus_path \"\$target_path\" >/dev/null 2>&1 || failed=1"))
        assertTrue(service.contains("sleep 1"))
    }
}
