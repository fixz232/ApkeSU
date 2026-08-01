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
    fun creatorApplicationUrl_prefillsTheIssueForm() {
        val url = buildCloudThemeCreatorApplicationUrl("Alice-Theme", "Alice Creator")

        assertTrue(url.startsWith("https://github.com/fixz232/ApkeSU-ThemeStore/issues/new?"))
        assertTrue(url.contains("template=cloud_theme_creator_application.yml"))
        assertTrue(url.contains("github_login=alice-theme"))
        assertTrue(url.contains("display_name=Alice%20Creator"))
    }

    @Test
    fun creatorPackagePicker_acceptsEveryMimeTypeAndCanonicalizesTheName() {
        assertEquals("*/*", CLOUD_THEME_CREATOR_PICKER_MIME_TYPE)
        assertEquals(
            "my.theme.kstheme",
            canonicalCloudThemePackageFileName("my.theme.unrestricted-format"),
        )
        assertEquals(
            "apkesu-cloud-theme.kstheme",
            canonicalCloudThemePackageFileName(".unknown"),
        )
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
        val theme = manifest.getJSONObject("theme")
        val author = theme.getJSONObject("author")

        assertEquals(CLOUD_THEME_SUBMISSION_SCHEMA, manifest.getString("schema"))
        assertEquals("alice-theme", author.getString("github"))
        assertFalse(author.has("realName"))
        assertFalse(author.has("gender"))
        assertEquals(2, theme.getJSONArray("tags").length())
        assertEquals(1L, theme.getLong("minManagerVersionCode"))
        assertTrue(theme.isNull("maxManagerVersionCode"))
    }

    @Test
    fun submissionManifest_treatsOptionalUrlPlaceholdersAsEmpty() {
        val draft = validDraft().copy(
            authorProfileUrl = "\u65e0",
            authorAvatarUrl = "none",
            screenshotUrlsText = "\u65e0\nN/A\n-",
        )

        val theme = JSONObject(buildCloudThemeSubmissionManifest(draft)).getJSONObject("theme")
        val author = theme.getJSONObject("author")

        assertEquals(0, theme.getJSONArray("screenshots").length())
        assertEquals("https://github.com/alice-theme", author.getString("profileUrl"))
        assertFalse(author.has("avatarUrl"))
    }

    @Test
    fun submissionManifest_identifiesTheInvalidMediaField() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            buildCloudThemeSubmissionManifest(
                validDraft().copy(screenshotUrlsText = "not-a-url")
            )
        }

        assertTrue(error.message.orEmpty().startsWith("Screenshot URL 1:"))
    }

    @Test
    fun submissionManifest_usesFirstScreenshotWhenCoverIsBlank() {
        val manifest = JSONObject(
            buildCloudThemeSubmissionManifest(validDraft().copy(coverUrl = ""))
        )
        val theme = manifest.getJSONObject("theme")

        assertEquals(
            theme.getJSONArray("screenshots").getString(0),
            theme.getString("coverUrl"),
        )
    }

    @Test
    fun submissionManifest_usesDefaultCoverWhenMediaIsBlank() {
        val manifest = JSONObject(
            buildCloudThemeSubmissionManifest(
                validDraft().copy(coverUrl = "", screenshotUrlsText = "\u65e0")
            )
        )

        assertEquals(
            CLOUD_THEME_DEFAULT_COVER_URL,
            manifest.getJSONObject("theme").getString("coverUrl"),
        )
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
    fun creatorPackageUrlRequiresTheApprovedAccountRelease() {
        assertEquals(
            "https://github.com/alice-theme/themes/releases/download/v1/aurora.kstheme",
            validateCloudThemeCreatorPackageUrl(
                "https://github.com/alice-theme/themes/releases/download/v1/aurora.kstheme",
                "Alice-Theme",
            ),
        )
        assertThrows(IllegalArgumentException::class.java) {
            validateCloudThemeCreatorPackageUrl(
                "https://github.com/another-user/themes/releases/download/v1/aurora.kstheme",
                "alice-theme",
            )
        }
    }

    @Test
    fun submissionIssueUrl_prefillsMachineReadableManifest() {
        val url = buildCloudThemeSubmissionIssueUrl(validDraft())

        assertTrue(url.startsWith("https://github.com/fixz232/ApkeSU-ThemeStore/issues/new?"))
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
                "html_url": "https://github.com/fixz232/ApkeSU-ThemeStore/issues/10",
                "updated_at": "2026-07-24T01:02:03Z",
                "user": {"login": "Alice-Theme"},
                "labels": [{"name": "creator-active"}],
                "body": ""
              },
              {
                "number": 12,
                "title": "[Cloud theme] aurora-night - Aurora Night",
                "state": "closed",
                "html_url": "https://github.com/fixz232/ApkeSU-ThemeStore/issues/12",
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

    @Test
    fun creatorActivity_marksBlankApplicationAsNeedingChanges() {
        val activity = parseCloudThemeCreatorActivity(
            """
            [
              {
                "number": 81,
                "title": "[Creator application] alice-theme",
                "state": "open",
                "html_url": "https://github.com/fixz232/ApkeSU-ThemeStore/issues/81",
                "updated_at": "2026-07-24T06:28:48Z",
                "user": {"login": "alice-theme"},
                "labels": [],
                "body": null
              }
            ]
            """.trimIndent(),
            "alice-theme",
        )

        assertEquals(
            CloudThemeCreatorApplicationStatus.NeedsChanges,
            activity.applicationStatus,
        )
    }

    @Test
    fun creatorActivity_acceptsCompletePendingApplication() {
        val activity = parseCloudThemeCreatorActivity(
            """
            [
              {
                "number": 82,
                "title": "[Creator application] alice-theme",
                "state": "open",
                "html_url": "https://github.com/fixz232/ApkeSU-ThemeStore/issues/82",
                "updated_at": "2026-07-24T06:30:00Z",
                "user": {"login": "alice-theme"},
                "labels": [{"name": "theme-creator-application"}],
                "body": "### GitHub login\n\nalice-theme\n\n### Public creator name\n\nAlice\n\n### Introduction\n\nOriginal themes.\n\n### Declarations\n\n- [x] one\n- [x] two\n- [x] three"
              }
            ]
            """.trimIndent(),
            "alice-theme",
        )

        assertEquals(CloudThemeCreatorApplicationStatus.Pending, activity.applicationStatus)
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
