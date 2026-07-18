package me.weishu.kernelsu.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PatchedImageOutputTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun prepareCreatesEmptyApplicationOwnedOutput() {
        val output = preparePatchedImageOutput(temporaryFolder.root, "apkesu_patched.img")

        assertTrue(output.isFile)
        assertEquals(0L, output.length())
        assertEquals("file is empty", validatePatchedImageOutput(output))
    }

    @Test
    fun prepareReplacesStaleOutput() {
        val stale = temporaryFolder.newFile("apkesu_patched.img")
        stale.writeText("stale")

        val output = preparePatchedImageOutput(temporaryFolder.root, stale.name)

        assertEquals(0L, output.length())
    }

    @Test
    fun validationAcceptsReadableNonEmptyOutput() {
        val output = preparePatchedImageOutput(temporaryFolder.root, "apkesu_patched.img")
        output.writeBytes(byteArrayOf(1, 2, 3))

        assertNull(validatePatchedImageOutput(output))
    }

    @Test
    fun validationRejectsMissingOutput() {
        val output = temporaryFolder.root.resolve("missing.img")

        assertEquals("file is missing", validatePatchedImageOutput(output))
    }
}
