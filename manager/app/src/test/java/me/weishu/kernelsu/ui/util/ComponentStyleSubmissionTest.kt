package me.weishu.kernelsu.ui.util

import me.weishu.kernelsu.ui.component.custom.ComponentStyleKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ComponentStyleSubmissionTest {
    @Test
    fun cloudStyleIdIsStableAndCatalogSafe() {
        val id = cloudComponentStyleId(
            ComponentStyleKind.Switch,
            "  My Switch / 夏日  ",
        )

        assertEquals("switch.my-switch", id)
        assertTrue(Regex("[a-z0-9][a-z0-9._-]{1,79}").matches(id))
    }

    @Test
    fun cloudStyleIdKeepsGeneratedStyleIdentity() {
        val id = cloudComponentStyleId(
            ComponentStyleKind.Card,
            "card-123e4567-e89b-12d3-a456-426614174000",
        )

        assertEquals("card.card-123e4567-e89b-12d3-a456-426614174000", id)
    }

    @Test
    fun componentSubmissionReplacesPackageStateButKeepsCreatorIdentity() {
        val previous = CloudThemeSubmissionDraft(
            githubLogin = "fixz232",
            authorName = "Existing creator",
            authorBio = "Profile bio",
            authorProfileUrl = "https://github.com/fixz232",
            packageUrl = "https://github.com/fixz232/old.kstheme",
            coverUrl = "https://example.com/old.png",
            screenshotUrlsText = "https://example.com/shot.png",
            changelog = "Old changes",
            remoteVerifiedUrl = "https://github.com/fixz232/old.kstheme",
            remoteVerifiedSha256 = "b".repeat(64),
            remoteVerifiedAt = 123L,
        )
        val inspection = CloudThemeCreatorPackageInspection(
            uriString = "file:///validated/new.kstheme",
            displayName = "new.kstheme",
            sha256 = "a".repeat(64),
            sizeBytes = 4096L,
            packageVersion = 4,
            configuredResourceCount = 1,
            authorDisplayName = null,
            warnings = emptyList(),
        )

        val draft = buildComponentStyleSubmissionDraft(
            previous = previous,
            inspection = inspection,
            kind = ComponentStyleKind.Card,
            styleId = "card-example",
            styleName = "Example",
            styleAuthor = "",
            description = "Pixel cards",
            categoryId = "component-card",
            categoryName = "Card styles",
            tags = "component,pixel,card",
            packageName = "example.kstheme",
        )

        assertEquals("fixz232", draft.githubLogin)
        assertEquals("Existing creator", draft.authorName)
        assertEquals("Profile bio", draft.authorBio)
        assertEquals(inspection.sha256, draft.packageSha256)
        assertEquals("component-card", draft.categoryId)
        assertEquals("", draft.packageUrl)
        assertEquals("", draft.coverUrl)
        assertEquals("", draft.screenshotUrlsText)
        assertFalse(draft.isRemoteVerified)
    }
}
