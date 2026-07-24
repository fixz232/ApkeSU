package me.weishu.kernelsu.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.InputStream
import java.io.OutputStream

class CloudThemeCreatorRepositoryTest {
    @Test
    fun packageLimit_is500MiB() {
        assertEquals(500L * 1024L * 1024L, CLOUD_THEME_MAX_PACKAGE_BYTES)
    }

    @Test
    fun packageCopy_acceptsExactConfiguredBoundary() {
        val boundary = 1024L * 1024L
        val output = CountingOutputStream()

        val result = copyAndFingerprintCloudThemePackage(
            input = SizedInputStream(boundary),
            output = output,
            maxBytes = boundary,
        )

        assertEquals(boundary, result.sizeBytes)
        assertEquals(boundary, output.bytesWritten)
        assertTrue(Regex("[a-f0-9]{64}").matches(result.sha256))
    }

    @Test
    fun packageCopy_rejectsTheFirstByteOverTheLimit() {
        val output = CountingOutputStream()

        assertThrows(IllegalArgumentException::class.java) {
            copyAndFingerprintCloudThemePackage(
                input = SizedInputStream(33),
                output = output,
                maxBytes = 32,
            )
        }

        assertEquals(0L, output.bytesWritten)
    }

    @Test
    fun packageCopy_rejectsEmptyFiles() {
        assertThrows(IllegalArgumentException::class.java) {
            copyAndFingerprintCloudThemePackage(
                input = SizedInputStream(0),
                output = CountingOutputStream(),
            )
        }
    }

    private class SizedInputStream(private var remaining: Long) : InputStream() {
        override fun read(): Int {
            if (remaining <= 0L) return -1
            remaining--
            return 0
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            if (remaining <= 0L) return -1
            val count = minOf(remaining, length.toLong()).toInt()
            buffer.fill(0, offset, offset + count)
            remaining -= count
            return count
        }
    }

    private class CountingOutputStream : OutputStream() {
        var bytesWritten: Long = 0L
            private set

        override fun write(value: Int) {
            bytesWritten++
        }

        override fun write(buffer: ByteArray, offset: Int, length: Int) {
            bytesWritten += length
        }
    }
}
