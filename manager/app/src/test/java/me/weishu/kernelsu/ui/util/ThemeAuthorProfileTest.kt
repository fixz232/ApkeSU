package me.weishu.kernelsu.ui.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeAuthorProfileTest {
    @Test
    fun sanitizeThemeAuthorProfile_normalizesAndLimitsFields() {
        val profile = sanitizeThemeAuthorProfile(
            ThemeAuthorProfile(
                displayName = "  Theme\u0000   author  ",
                realName = "  Real   Name  ",
                gender = ThemeAuthorGender.Female,
                bio = "Line one\r\nLine two\u0000\nLine three\nLine four\nLine five",
                avatarUriString = "  file:///avatar.image  ",
            )
        )

        assertEquals("Theme author", profile.displayName)
        assertEquals("Real Name", profile.realName)
        assertEquals(ThemeAuthorGender.Female, profile.gender)
        assertEquals("Line one\nLine two\nLine three\nLine four", profile.bio)
        assertEquals("file:///avatar.image", profile.avatarUriString)
    }

    @Test
    fun themeAuthorGender_unknownValueFallsBackToUnspecified() {
        assertEquals(ThemeAuthorGender.Unspecified, ThemeAuthorGender.fromStorageValue("unknown"))
    }
}
