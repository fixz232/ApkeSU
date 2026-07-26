package me.weishu.kernelsu.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AppFontTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun ttfSignature_acceptsStandardAndLegacyTrueTypeHeaders() {
        assertTrue(hasSupportedTtfSignature(byteArrayOf(0, 1, 0, 0)))
        assertTrue(hasSupportedTtfSignature("true".toByteArray()))
    }

    @Test
    fun ttfSignature_rejectsOpenTypeCollectionsAndShortFiles() {
        assertFalse(hasSupportedTtfSignature("OTTO".toByteArray()))
        assertFalse(hasSupportedTtfSignature("ttcf".toByteArray()))
        assertFalse(hasSupportedTtfSignature(byteArrayOf(0, 1, 0)))
    }

    @Test
    fun presetParsing_fallsBackToSystem() {
        assertEquals(AppFontPreset.Monospace, AppFontPreset.fromValue("monospace"))
        assertEquals(AppFontPreset.System, AppFontPreset.fromValue("unknown"))
        assertEquals(AppFontPreset.System, AppFontPreset.fromValue(null))
    }

    @Test
    fun displayNameSanitizing_removesPathsControlsAndBlankValues() {
        assertEquals("Example.ttf", sanitizeAppFontDisplayName("folder\\Example.ttf"))
        assertEquals("Font.ttf", sanitizeAppFontDisplayName("content/path/Font\u0000.ttf"))
        assertNull(sanitizeAppFontDisplayName("  \u0000  "))
        assertEquals(96, sanitizeAppFontDisplayName("a".repeat(120))?.length)
    }

    @Test
    fun fileValidation_checksHeaderSizeAndChecksumWithoutAndroidParser() {
        val file = temporaryFolder.newFile("valid.ttf")
        file.writeBytes(byteArrayOf(0, 1, 0, 0, 7, 8, 9))
        val sha256 = appFontSha256(file)

        val metadata = validateAppFontFile(
            file = file,
            expectedSha256 = sha256,
            expectedSizeBytes = file.length(),
            validateTypeface = false,
        )

        assertEquals(file.length(), metadata.sizeBytes)
        assertEquals(sha256, metadata.sha256)
    }
}
