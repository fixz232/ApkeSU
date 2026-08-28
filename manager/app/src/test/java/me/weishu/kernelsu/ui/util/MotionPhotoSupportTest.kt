package me.weishu.kernelsu.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MotionPhotoSupportTest {

    @Test
    fun scanMotionPhotoContainer_readsLegacyMicroVideoOffset() {
        val video = mp4Ftyp("mp42")
        val header = (
            "jpeg<GCamera:MicroVideoOffset>${video.size}</GCamera:MicroVideoOffset>"
            ).toByteArray()
        val file = writeFixture(header + video)

        val scan = scanMotionPhotoContainer(file)

        assertTrue(scan.declaredAsMotionPhoto)
        assertEquals(header.size.toLong(), scan.videoOffsets.first())
    }

    @Test
    fun scanMotionPhotoContainer_readsContainerItemLength() {
        val video = mp4Ftyp("isom")
        val header = (
            "jpeg<Container:Item Item:Semantic=\"MotionPhoto\" " +
                "Item:Mime=\"video/mp4\" Item:Length=\"${video.size}\"/>"
            ).toByteArray()
        val file = writeFixture(header + video)

        val scan = scanMotionPhotoContainer(file)

        assertTrue(scan.declaredAsMotionPhoto)
        assertEquals(header.size.toLong(), scan.videoOffsets.first())
    }

    @Test
    fun scanMotionPhotoContainer_findsSamsungStyleAppendedMp4WithoutXmp() {
        val header = byteArrayOf(0xff.toByte(), 0xd8.toByte(), 1, 2, 3, 4)
        val file = writeFixture(header + mp4Ftyp("isom"))

        val scan = scanMotionPhotoContainer(file)

        assertFalse(scan.declaredAsMotionPhoto)
        assertEquals(listOf(header.size.toLong()), scan.videoOffsets)
    }

    @Test
    fun scanMotionPhotoContainer_doesNotTreatHeicImageContainerAsVideo() {
        val file = writeFixture(mp4Ftyp("heic"))

        val scan = scanMotionPhotoContainer(file)

        assertFalse(scan.declaredAsMotionPhoto)
        assertTrue(scan.videoOffsets.isEmpty())
    }

    @Test
    fun scanMotionPhotoContainer_findsFtypAcrossReadBoundary() {
        val header = ByteArray(128 * 1024 - 6) { 0x2a }
        val file = writeFixture(header + mp4Ftyp("mp42"))

        val scan = scanMotionPhotoContainer(file)

        assertEquals(listOf(header.size.toLong()), scan.videoOffsets)
    }

    private fun writeFixture(bytes: ByteArray): File {
        return File.createTempFile("motion-photo-test-", ".bin").apply {
            writeBytes(bytes)
            deleteOnExit()
        }
    }

    private fun mp4Ftyp(brand: String): ByteArray {
        require(brand.length == 4)
        return byteArrayOf(
            0, 0, 0, 24,
            'f'.code.toByte(), 't'.code.toByte(), 'y'.code.toByte(), 'p'.code.toByte(),
            *brand.toByteArray(Charsets.US_ASCII),
            0, 0, 0, 0,
            'i'.code.toByte(), 's'.code.toByte(), 'o'.code.toByte(), 'm'.code.toByte(),
            'm'.code.toByte(), 'p'.code.toByte(), '4'.code.toByte(), '2'.code.toByte(),
        )
    }
}
