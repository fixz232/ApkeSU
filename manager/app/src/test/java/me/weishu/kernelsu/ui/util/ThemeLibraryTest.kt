package me.weishu.kernelsu.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeLibraryTest {
    @Test
    fun sanitizeThemeLibraryName_normalizesWhitespaceAndControlCharacters() {
        val name = sanitizeThemeLibraryName("  Night\n  theme\u0000  ")

        assertEquals("Night theme", name)
    }

    @Test
    fun themeLibraryIndex_roundTripsMultipleEntries() {
        val entries = listOf(
            ThemeLibraryEntry(
                id = "first-theme",
                name = "First",
                createdAt = 10L,
                updatedAt = 20L,
                lastAppliedAt = 30L,
                sizeBytes = 40L,
            ),
            ThemeLibraryEntry(
                id = "second-theme",
                name = "Second",
                createdAt = 50L,
                updatedAt = 60L,
                lastAppliedAt = null,
                sizeBytes = 70L,
            ),
        )

        val decoded = decodeThemeLibraryIndex(encodeThemeLibraryIndex(entries))

        assertEquals(entries, decoded)
        assertNull(decoded.last().lastAppliedAt)
    }

    @Test
    fun themeLibraryIndex_skipsUnsafeIdsAndBlankNames() {
        val decoded = decodeThemeLibraryIndex(
            """{
                "schema":"io.github.fixz.apkesu.theme-library",
                "version":1,
                "items":[
                    {"id":"../outside","name":"Unsafe"},
                    {"id":"valid-id","name":"   "}
                ]
            }""".trimIndent()
        )

        assertTrue(decoded.isEmpty())
    }
}
