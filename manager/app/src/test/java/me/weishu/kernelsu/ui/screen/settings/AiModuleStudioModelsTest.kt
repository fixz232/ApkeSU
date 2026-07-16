package me.weishu.kernelsu.ui.screen.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class AiModuleStudioModelsTest {
    @Test
    fun completeTemplateBuildsValidRootedModuleProject() {
        val project = createAiModuleProject(
            AiModuleTemplate.Complete,
            AiModuleTemplateMetadata(
                moduleId = "apkesu_test",
                name = "ApkeSU Test",
                version = "2.0.0",
                versionCode = "20",
                author = "Tester",
                description = "Test module",
            ),
            now = 123L,
        )

        val validation = validateAiModuleProject(project)

        assertTrue(validation.canExport)
        assertEquals(MODULE_PROP_PATH, project.files.first().path)
        assertTrue(project.files.any { it.path == SERVICE_PATH })
        assertTrue(project.files.any { it.path == WEB_UI_INDEX_PATH })
        assertEquals("apkesu_test", project.metadata.moduleId)
    }

    @Test
    fun pathValidationRejectsTraversalAbsoluteAndReservedMarkers() {
        assertNull(normalizeModuleFilePath("../service.sh"))
        assertNull(normalizeModuleFilePath("/service.sh"))
        assertNull(normalizeModuleFilePath("folder/../../service.sh"))
        assertNull(normalizeModuleFilePath("META-INF/update-binary"))

        val project = createAiModuleProject(AiModuleTemplate.Basic, AiModuleTemplateMetadata())
            .copy(files = listOf(AiModuleStudioFile("remove", "")))
        val codes = validateAiModuleProject(project).issues.map(AiModuleValidationIssue::code)

        assertTrue(AiModuleIssueCode.ReservedPath in codes)
        assertTrue(AiModuleIssueCode.MissingModuleProp in codes)
    }

    @Test
    fun inspectionFindsHighRiskShellCommands() {
        val base = createAiModuleProject(AiModuleTemplate.Basic, AiModuleTemplateMetadata())
        val project = base.copy(
            files = base.files + AiModuleStudioFile(
                SERVICE_PATH,
                "#!/system/bin/sh\nrm -rf /\ndd if=/dev/zero of=/dev/block/test\n",
            )
        )

        val validation = validateAiModuleProject(project)
        val codes = validation.issues.map(AiModuleValidationIssue::code)

        assertFalse(validation.canExport)
        assertTrue(AiModuleIssueCode.DestructiveRootCommand in codes)
        assertTrue(AiModuleIssueCode.BlockDeviceWrite in codes)
    }

    @Test
    fun validationRejectsVersionCodeOutsideManagerIntegerRange() {
        val base = createAiModuleProject(AiModuleTemplate.Basic, AiModuleTemplateMetadata())
        val moduleProp = base.files.single { it.path == MODULE_PROP_PATH }
            .copy(content = base.files.single { it.path == MODULE_PROP_PATH }.content.replace("versionCode=1", "versionCode=999999999999"))
        val project = base.copy(files = base.files.map { if (it.path == MODULE_PROP_PATH) moduleProp else it })

        assertTrue(
            validateAiModuleProject(project).issues.any { it.code == AiModuleIssueCode.InvalidVersionCode }
        )
    }

    @Test
    fun exportedZipKeepsModuleFilesAtArchiveRoot() {
        val project = createAiModuleProject(AiModuleTemplate.WebUi, AiModuleTemplateMetadata())
        val output = ByteArrayOutputStream()

        writeAiModuleZip(project, output)

        val entries = mutableListOf<String>()
        ZipInputStream(ByteArrayInputStream(output.toByteArray())).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entries += entry.name
                zip.closeEntry()
            }
        }
        assertTrue(MODULE_PROP_PATH in entries)
        assertTrue(WEB_UI_INDEX_PATH in entries)
        assertTrue(entries.none { it.startsWith("META-INF/") })
        assertTrue(entries.none { it.startsWith("/") })
    }

    @Test
    fun encryptedDraftJsonRoundTripPreservesProjectAndRejectsOversize() {
        val original = createAiModuleProject(AiModuleTemplate.BootService, AiModuleTemplateMetadata())
            .copy(
                selectedPath = SERVICE_PATH,
                lastAiPrompt = "review service",
                lastAiResponse = "looks good",
            )

        val restored = parseAiModuleStudioProject(original.toJson().toString())

        assertNotNull(restored)
        assertEquals(original, restored)

        val oversized = original.copy(
            files = listOf(
                AiModuleStudioFile(MODULE_PROP_PATH, "x".repeat(MAX_MODULE_STUDIO_FILE_CHARS + 1))
            )
        )
        assertNull(parseAiModuleStudioProject(oversized.toJson().toString()))
    }

    @Test
    fun encryptedDraftJsonRejectsCaseInsensitiveDuplicatePaths() {
        val project = createAiModuleProject(AiModuleTemplate.Basic, AiModuleTemplateMetadata())
            .copy(
                files = listOf(
                    AiModuleStudioFile(MODULE_PROP_PATH, "id=test"),
                    AiModuleStudioFile("Service.sh", "#!/system/bin/sh\n"),
                    AiModuleStudioFile("service.sh", "#!/system/bin/sh\n"),
                )
            )

        assertNull(parseAiModuleStudioProject(project.toJson().toString()))
    }

    @Test
    fun firstFencedCodeBlockIsExtractedWithoutLanguageMarker() {
        val response = "Before\r\n```sh\r\n#!/system/bin/sh\r\necho ok  \r\n```\r\nAfter"

        val code = extractFirstAiCodeBlock(response)

        assertEquals("#!/system/bin/sh\r\necho ok  ", code)
        assertNull(extractFirstAiCodeBlock("No code here"))
    }

    @Test
    fun templateShellQuotesUntrustedModuleName() {
        val project = createAiModuleProject(
            AiModuleTemplate.Basic,
            AiModuleTemplateMetadata(name = "$(touch /data/local/tmp/pwned) O'Brien"),
        )

        val customize = project.files.single { it.path == CUSTOMIZE_PATH }.content
        val installLine = customize.lineSequence().first { "Installing" in it }

        assertEquals("ui_print '- Installing $(touch /data/local/tmp/pwned) O'\\''Brien'", installLine)
        assertFalse(installLine.contains('"'))
    }

    @Test
    fun metadataUpdatePreservesCustomPropertiesAndComments() {
        val current = """
            # Existing project note
            id=old_id
            name=Old name
            version=1.0
            versionCode=1
            author=Old author
            description=Old description
            updateJson=https://example.com/update.json
        """.trimIndent()

        val updated = updateAiModuleMetadata(
            current,
            AiModuleTemplateMetadata(
                moduleId = "new_id",
                name = "New name",
                version = "2.0",
                versionCode = "20",
                author = "New author",
                description = "New description",
            ),
        )

        assertEquals("new_id", parseModuleMetadata(updated).moduleId)
        assertTrue(updated.contains("# Existing project note"))
        assertTrue(updated.contains("updateJson=https://example.com/update.json"))
        assertEquals(1, Regex("(?m)^id=").findAll(updated).count())
    }

    @Test
    fun exportedTextProjectCanBeImportedWithoutContentLoss() {
        val original = createAiModuleProject(
            AiModuleTemplate.Complete,
            AiModuleTemplateMetadata(moduleId = "round_trip", name = "Round trip"),
            now = 10L,
        )
        val archive = ByteArrayOutputStream().also { writeAiModuleZip(original, it) }.toByteArray()

        val imported = readAiModuleProjectZip(ByteArrayInputStream(archive), now = 20L)

        assertEquals(
            original.files.associate { it.path to it.content },
            imported.files.associate { it.path to it.content },
        )
        assertEquals(MODULE_PROP_PATH, imported.selectedPath)
        assertEquals(20L, imported.createdAt)
    }

    @Test
    fun importStripsOneSharedTopLevelDirectory() {
        val original = createAiModuleProject(AiModuleTemplate.BootService, AiModuleTemplateMetadata())
        val archive = zipBytes(
            original.files.map { file -> "project/${file.path}" to file.content.toByteArray() }
        )

        val imported = readAiModuleProjectZip(ByteArrayInputStream(archive))

        assertTrue(imported.files.any { it.path == MODULE_PROP_PATH })
        assertTrue(imported.files.any { it.path == SERVICE_PATH })
        assertTrue(imported.files.none { it.path.startsWith("project/") })
    }

    @Test
    fun importRejectsTraversalAndBinaryEntries() {
        val unsafeArchive = zipBytes(listOf("../module.prop" to "id=unsafe".toByteArray()))
        val binaryArchive = zipBytes(listOf(MODULE_PROP_PATH to byteArrayOf(0, 1, 2, 3)))

        val unsafeError = runCatching {
            readAiModuleProjectZip(ByteArrayInputStream(unsafeArchive))
        }.exceptionOrNull() as? AiModuleImportException
        val binaryError = runCatching {
            readAiModuleProjectZip(ByteArrayInputStream(binaryArchive))
        }.exceptionOrNull() as? AiModuleImportException

        assertEquals(AiModuleImportError.InvalidPath, unsafeError?.reason)
        assertEquals(AiModuleImportError.BinaryFile, binaryError?.reason)
    }

    private fun zipBytes(entries: List<Pair<String, ByteArray>>): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            entries.forEach { (path, content) ->
                zip.putNextEntry(ZipEntry(path))
                zip.write(content)
                zip.closeEntry()
            }
        }
        return output.toByteArray()
    }
}
