package me.weishu.kernelsu.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.SecureRandom

class AppIdManagerTest {
    @Test
    fun updateTargetsUidWithoutReplacingAnotherAppsEqualValue() {
        val updated = SsaidXmlEditor.replaceEntry(
            xml = sampleXml,
            uid = 10123,
            packageName = "com.example.first",
            newValue = "fedcba9876543210",
        )

        assertEquals(
            "fedcba9876543210",
            SsaidXmlEditor.readEntry(updated, 10123, "com.example.first")?.value,
        )
        assertEquals(
            "0123456789abcdef",
            SsaidXmlEditor.readEntry(updated, 10124, "com.example.second")?.value,
        )
    }

    @Test
    fun packageFallbackSupportsLegacyPackageNamedEntry() {
        val legacy = """
            <?xml version='1.0' encoding='UTF-8' standalone='yes' ?>
            <settings version="1">
                <setting id="1" name="com.example.legacy" value="0123456789abcdef" package="com.example.legacy" />
            </settings>
        """.trimIndent()

        val updated = SsaidXmlEditor.replaceEntry(
            legacy,
            uid = 10456,
            packageName = "com.example.legacy",
            newValue = "1111222233334444",
        )

        assertEquals(
            "1111222233334444",
            SsaidXmlEditor.readEntry(updated, 10456, "com.example.legacy")?.value,
        )
    }

    @Test
    fun missingEntryCanBeInsertedAndRemovedForSystemRegeneration() {
        val inserted = SsaidXmlEditor.replaceEntry(
            sampleXml,
            uid = 10999,
            packageName = "com.example.new",
            newValue = "aaaabbbbccccdddd",
        )
        assertEquals(
            "aaaabbbbccccdddd",
            SsaidXmlEditor.readEntry(inserted, 10999, "com.example.new")?.value,
        )

        val removed = SsaidXmlEditor.replaceEntry(
            inserted,
            uid = 10999,
            packageName = "com.example.new",
            newValue = null,
        )
        assertNull(SsaidXmlEditor.readEntry(removed, 10999, "com.example.new"))
        assertFalse(SsaidXmlEditor.hasValueDifferences(sampleXml, removed))
    }

    @Test
    fun androidBinaryXmlCompanionRootIsPreserved() {
        val convertedAbx = sampleXml + "\n<namespaceHashes><namespaceHash namespace=\"ssaid\" hash=\"42\"/></namespaceHashes>"

        val updated = SsaidXmlEditor.replaceEntry(
            convertedAbx,
            uid = 10123,
            packageName = "com.example.first",
            newValue = "fedcba9876543210",
        )

        assertEquals(
            "fedcba9876543210",
            SsaidXmlEditor.readEntry(updated, 10123, "com.example.first")?.value,
        )
        assertTrue(updated.contains("<namespaceHashes>"))
        assertTrue(updated.contains("namespace=\"ssaid\""))
        SsaidXmlEditor.validate(updated)
    }

    @Test
    fun appIdValidationRequiresExactlySixteenHexCharacters() {
        assertTrue(SsaidXmlEditor.isValidAppId("0123456789abcdef"))
        assertTrue(SsaidXmlEditor.isValidAppId("ABCDEF0123456789"))
        assertFalse(SsaidXmlEditor.isValidAppId("0123456789abcde"))
        assertFalse(SsaidXmlEditor.isValidAppId("0123456789abcdef0"))
        assertFalse(SsaidXmlEditor.isValidAppId("0123456789abcdeg"))
        assertThrows(AppIdException::class.java) {
            SsaidXmlEditor.replaceEntry(sampleXml, 10123, "com.example.first", "not-an-app-id")
        }
    }

    @Test
    fun bulkEntryReadKeepsUidAndLegacyPackageLookups() {
        val entries = SsaidXmlEditor.readEntries(sampleXml)

        assertEquals(2, entries.size)
        assertEquals(
            "0123456789abcdef",
            SsaidXmlEditor.readEntry(entries, 10123, "com.example.first")?.value,
        )
        assertNull(SsaidXmlEditor.readEntry(entries, 10999, "com.example.missing"))
    }

    @Test
    fun randomAppIdRetriesWhenGeneratedValueIsAlreadyInUse() {
        var invocation = 0
        val random = object : SecureRandom() {
            override fun nextBytes(bytes: ByteArray) {
                bytes.fill(if (invocation++ == 0) 0 else 1)
            }
        }

        val generated = createRandomAppId(
            excludedValues = setOf("0000000000000000"),
            secureRandom = random,
        )

        assertEquals("0101010101010101", generated)
        assertTrue(SsaidXmlEditor.isValidAppId(generated))
    }

    @Test
    fun malformedOrDoctypeXmlIsRejectedBeforeWriting() {
        assertThrows(AppIdException::class.java) {
            SsaidXmlEditor.replaceEntry("<settings>", 10123, "com.example.first", "0123456789abcdef")
        }
        assertThrows(AppIdException::class.java) {
            SsaidXmlEditor.replaceEntry(
                "<!DOCTYPE settings [<!ENTITY xxe SYSTEM 'file:///data/local/tmp/x'>]><settings/>",
                10123,
                "com.example.first",
                "0123456789abcdef",
            )
        }
    }

    @Test
    fun stagingScriptStreamsPayloadWithoutReadingAppPrivateCache() {
        val script = buildAppIdStagingScript(userId = 0, xml = sampleXml)

        assertFalse(script.contains("/data/user/"))
        assertFalse(script.contains("/cache/app-id-manager"))
        assertTrue(script.contains("cat > '/data/adb/post-fs-data.d/apkesu-app-id.sh.tmp'"))
        assertTrue(script.contains("cat > '/data/adb/apkesu/app-id/pending/settings_ssaid_0.xml.tmp'"))
        assertTrue(script.contains("__APKESU_APP_ID_STAGE_OK__"))
        assertTrue(script.contains(sampleXml))
        assertFalse(script.contains("exit 1"))
        assertTrue(script.contains("apkesu_stage_main"))
    }

    @Test
    fun optionalRootFileProbeDoesNotTerminatePersistentShell() {
        val command = buildRootFileReadCommand("/data/adb/apkesu/app-id/pending/missing.xml")

        assertFalse(command.contains("exit"))
        assertTrue(command.contains("__APKESU_FILE_EXISTS__"))
        assertTrue(command.contains("__APKESU_FILE_MISSING__"))
        assertTrue(command.contains("/system/bin/abx2xml"))
        assertTrue(command.contains("\$(dd"))
    }

    @Test
    fun bootScriptPreservesAndroidBinaryXmlFormat() {
        val script = buildAppIdStagingScript(userId = 0, xml = sampleXml)

        assertTrue(script.contains("is_abx_file"))
        assertTrue(script.contains("/system/bin/xml2abx"))
        assertTrue(script.contains("cp -p \"\$target\" \"\$temp\""))
    }

    private val sampleXml = """
        <?xml version='1.0' encoding='UTF-8' standalone='yes' ?>
        <settings version="2147483647">
            <setting id="1" name="10123" value="0123456789abcdef" package="com.example.first" defaultValue="0123456789abcdef" defaultSysSet="true" />
            <setting id="2" name="10124" value="0123456789abcdef" package="com.example.second" defaultValue="0123456789abcdef" defaultSysSet="true" />
        </settings>
    """.trimIndent()
}
