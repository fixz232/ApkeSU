package me.weishu.kernelsu.core.tasks

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.util.zip.GZIPOutputStream
import org.junit.Assert.assertEquals
import org.junit.Test

class BootKernelVersionTest {
    @Test
    fun detectsKmiFromGzipCompressedKernel() {
        val kernel = ByteArrayOutputStream().also { output ->
            GZIPOutputStream(output).use {
                it.write("Linux version 6.6.30-android15-12-gki".toByteArray())
            }
        }.toByteArray()
        val image = ByteArray(4096 + kernel.size)
        val header = ByteBuffer.wrap(image).order(ByteOrder.LITTLE_ENDIAN)
        "ANDROID!".toByteArray().copyInto(image)
        header.putInt(8, kernel.size)
        header.putInt(36, 4096)
        header.putInt(40, 4)
        kernel.copyInto(image, 4096)

        val file = Files.createTempFile("apkesu-kmi-", ".img").toFile()
        try {
            file.writeBytes(image)
            assertEquals("android15-6.6", BootKernelVersion.parseKmiFromBoot(file))
        } finally {
            file.delete()
        }
    }

    @Test
    fun recognizesKernellessInitBootImage() {
        val image = ByteArray(4096)
        "ANDROID!".toByteArray().copyInto(image)
        val file = Files.createTempFile("apkesu-init-boot-", ".img").toFile()
        try {
            file.writeBytes(image)
            assertEquals(true, BootKernelVersion.isKernellessBootImage(file))
        } finally {
            file.delete()
        }
    }
}
