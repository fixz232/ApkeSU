package me.weishu.kernelsu.ui.util

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudThemeCreatorCatalogTest {
    @Test
    fun parseRegistry_normalizesLoginAndFindsApprovedCreator() {
        val registry = parseCloudThemeCreatorRegistry(validRegistryJson())

        assertEquals("fixz232", registry.reviewer)
        assertEquals("alice-theme", registry.creators.single().github)
        assertTrue(registry.isApproved("Alice-Theme"))
        assertFalse(registry.isApproved("unknown"))
    }

    @Test
    fun parseRegistry_rejectsDuplicateLoginIgnoringCase() {
        val root = JSONObject(validRegistryJson())
        root.getJSONArray("creators").put(
            JSONObject()
                .put("github", "ALICE-theme")
                .put("displayName", "Duplicate")
                .put("approvedAt", 2)
                .put("status", "approved")
        )

        assertThrows(IllegalArgumentException::class.java) {
            parseCloudThemeCreatorRegistry(root.toString())
        }
    }

    @Test
    fun submissionDraft_roundTripsRemoteVerification() {
        val draft = validDraft()

        val decoded = decodeCloudThemeSubmissionDraft(encodeCloudThemeSubmissionDraft(draft))

        assertEquals(draft, decoded)
        assertTrue(decoded.hasInspectedPackage)
        assertTrue(decoded.isRemoteVerified)
    }

    @Test
    fun submissionManifest_containsOnlyPublicAuthorData() {
        val manifest = JSONObject(buildCloudThemeSubmissionManifest(validDraft()))
        val author = manifest.getJSONObject("theme").getJSONObject("author")

        assertEquals(CLOUD_THEME_SUBMISSION_SCHEMA, manifest.getString("schema"))
        assertEquals("alice-theme", author.getString("github"))
        assertFalse(author.has("realName"))
        assertFalse(author.has("gender"))
        assertEquals(2, manifest.getJSONObject("theme").getJSONArray("tags").length())
    }

    @Test
    fun submissionManifest_requiresRemotePackageMatch() {
        val unverified = validDraft().copy(remoteVerifiedSha256 = "b".repeat(64))

        assertThrows(IllegalArgumentException::class.java) {
            buildCloudThemeSubmissionManifest(unverified)
        }
    }

    @Test
    fun submissionManifest_rejectsReleaseOwnedByAnotherAccount() {
        val packageUrl =
            "https://github.com/another-user/themes/releases/download/v1/aurora.kstheme"
        val wrongOwner = validDraft().copy(
            packageUrl = packageUrl,
            remoteVerifiedUrl = packageUrl,
        )

        assertThrows(IllegalArgumentException::class.java) {
            buildCloudThemeSubmissionManifest(wrongOwner)
        }
    }

    @Test
    fun submissionIssueUrl_prefillsMachineReadableManifest() {
        val url = buildCloudThemeSubmissionIssueUrl(validDraft())

        assertTrue(url.startsWith("https://github.com/fixz232/ApkeSU/issues/new?"))
        assertTrue(url.contains("template=cloud_theme_submission.yml"))
        assertTrue(url.contains("manifest="))
        assertTrue(url.contains("theme_id=aurora-night"))
    }

    @Test
    fun creatorActivity_mapsApplicationAndSubmissionLabels() {
        val activity = parseCloudThemeCreatorActivity(
            """
            [
              {
                "number": 10,
                "title": "[Creator application] alice-theme",
                "state": "closed",
                "html_url": "https://github.com/fixz232/ApkeSU/issues/10",
                "updated_at": "2026-07-24T01:02:03Z",
                "user": {"login": "Alice-Theme"},
                "labels": [{"name": "creator-active"}],
                "body": ""
              },
              {
                "number": 12,
                "title": "[Cloud theme] aurora-night - Aurora Night",
                "state": "closed",
                "html_url": "https://github.com/fixz232/ApkeSU/issues/12",
                "updated_at": "2026-07-24T02:03:04Z",
                "user": {"login": "alice-theme"},
                "labels": [{"name": "theme-published"}],
                "body": "### Theme ID\n\naurora-night"
              }
            ]
            """.trimIndent(),
            "alice-theme",
        )

        assertEquals(CloudThemeCreatorApplicationStatus.Approved, activity.applicationStatus)
        assertEquals("alice-theme", activity.githubLogin)
        assertEquals(1, activity.submissions.size)
        assertEquals(CloudThemeSubmissionReviewStatus.Published, activity.submissions.single().status)
        assertEquals("aurora-night", activity.submissions.single().themeId)
    }

    private fun validRegistryJson(): String = """
        {
          "schema":"io.github.fixz.apkesu.theme-creators",
          "version":1,
          "generatedAt":1,
          "reviewer":"fixz232",
          "creators":[
            {
              "github":"Alice-Theme",
              "displayName":"Alice",
              "approvedAt":1,
              "status":"approved"
            }
          ]
        }
    """.trimIndent()

    private fun validDraft(): CloudThemeSubmissionDraft {
        val packageUrl =
            "https://github.com/alice-theme/themes/releases/download/v1/aurora.kstheme"
        return CloudThemeSubmissionDraft(
            githubLogin = "alice-theme",
            authorName = "Alice",
            authorBio = "Theme creator",
            authorProfileUrl = "https://github.com/alice-theme",
            packageUri = "content://theme/aurora",
            packageName = "aurora.kstheme",
            packageSha256 = "a".repeat(64),
            packageSizeBytes = 4096,
            packageVersion = 4,
            packageResourceCount = 8,
            themeId = "aurora-night",
            themeName = "Aurora Night",
            description = "A complete cloud theme.",
            categoryId = "appearance",
            categoryName = "Appearance",
            tagsText = "dark, aurora",
            versionCodeText = "1",
            versionName = "1.0.0",
            minManagerVersionCodeText = "32700",
            packageUrl = packageUrl,
            coverUrl = "https://raw.githubusercontent.com/alice-theme/themes/main/cover.png",
            screenshotUrlsText =
                "https://raw.githubusercontent.com/alice-theme/themes/main/screen.png",
            license = "CC-BY-4.0",
            changelog = "Initial release",
            remoteVerifiedUrl = packageUrl,
            remoteVerifiedSha256 = "a".repeat(64),
            remoteVerifiedAt = 1,
        )
    }
}
