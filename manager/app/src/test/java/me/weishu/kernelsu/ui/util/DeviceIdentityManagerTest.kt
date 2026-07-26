package me.weishu.kernelsu.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceIdentityManagerTest {
    @Test
    fun androidIdIsNormalizedAndValidated() {
        val result = validateDeviceIdentifier(
            DeviceIdentifierKind.AndroidId,
            " A1B2C3D4E5F60718 ",
        )

        assertTrue(result.isValid)
        assertEquals("a1b2c3d4e5f60718", result.normalizedValue)
        assertFalse(
            validateDeviceIdentifier(DeviceIdentifierKind.AndroidId, "1234").isValid,
        )
    }

    @Test
    fun serialRejectsShellAndControlCharacters() {
        assertTrue(
            validateDeviceIdentifier(DeviceIdentifierKind.SerialNumber, "APKESU-2026_01").isValid,
        )
        assertFalse(
            validateDeviceIdentifier(DeviceIdentifierKind.SerialNumber, "abc'; reboot").isValid,
        )
        assertFalse(
            validateDeviceIdentifier(DeviceIdentifierKind.SerialNumber, "abc\ndef").isValid,
        )
        assertFalse(
            validateDeviceIdentifier(
                DeviceIdentifierKind.SerialNumber,
                "\u8bbe\u5907\u5e8f\u5217\u53f7",
            ).isValid,
        )
    }

    @Test
    fun macValidationRejectsMulticastAndSentinelValues() {
        assertTrue(
            validateDeviceIdentifier(DeviceIdentifierKind.WifiMac, "02-11-22-33-44-55").isValid,
        )
        assertFalse(
            validateDeviceIdentifier(DeviceIdentifierKind.WifiMac, "01:11:22:33:44:55").isValid,
        )
        assertFalse(
            validateDeviceIdentifier(DeviceIdentifierKind.WifiMac, "00:00:00:00:00:00").isValid,
        )
        assertFalse(
            validateDeviceIdentifier(DeviceIdentifierKind.WifiMac, "FF:FF:FF:FF:FF:FF").isValid,
        )
    }

    @Test
    fun generatedMacIsLocallyAdministeredUnicast() {
        repeat(64) {
            val value = generateDeviceIdentifier(DeviceIdentifierKind.WifiMac)
            val validation = validateDeviceIdentifier(DeviceIdentifierKind.WifiMac, value)
            val firstByte = value.substringBefore(':').toInt(16)

            assertTrue(validation.isValid)
            assertEquals(0, firstByte and 0x01)
            assertEquals(0x02, firstByte and 0x02)
        }
    }

    @Test
    fun generatedValuesMatchEveryIdentifierRule() {
        DeviceIdentifierKind.entries.forEach { kind ->
            repeat(8) {
                assertTrue(
                    "$kind generated an invalid value",
                    validateDeviceIdentifier(kind, generateDeviceIdentifier(kind)).isValid,
                )
            }
        }
        assertFalse(
            validateDeviceIdentifier(
                DeviceIdentifierKind.Oaid,
                "\u5382\u5546\u5e7f\u544a\u6807\u8bc6_12345678",
            ).isValid,
        )
    }

    @Test
    fun statusParserKeepsCurrentConfiguredAndBackupValuesSeparate() {
        val snapshot = parseDeviceIdentityStatus(
            lines = listOf(
                "serial_support=supported",
                "serial_current=REAL1234",
                "boot_serial_current=BOOT1234",
                "serial_target=FAKE5678",
                "serial_original=REAL1234",
                "serial_backup=1",
                "serial_persistent=1",
                "android_id_support=supported",
                "android_id_current=0123456789abcdef",
                "android_id_target=",
                "android_id_original=__APKESU_ABSENT__",
                "android_id_backup=1",
                "android_id_target_exists=0",
                "wifi_support=missing",
                "bluetooth_support=tool_unavailable",
                "oaid_support=unsafe_format",
            ),
            userId = 10,
        )

        val serial = snapshot.identifier(DeviceIdentifierKind.SerialNumber)
        assertEquals("REAL1234", serial.currentValue)
        assertEquals("BOOT1234", snapshot.bootSerialNumber)
        assertEquals("FAKE5678", serial.configuredValue)
        assertEquals("REAL1234", serial.originalValue)
        assertTrue(serial.hasBackup)
        assertTrue(serial.persistent)
        assertFalse(serial.applied)
        assertEquals(
            DeviceIdentifierSupport.UnsafeFormat,
            snapshot.identifier(DeviceIdentifierKind.Oaid).support,
        )
        assertEquals(
            "",
            snapshot.identifier(DeviceIdentifierKind.AndroidId).originalValue,
        )
    }

    @Test
    fun statusScriptUsesRequestedAndroidUser() {
        val script = buildStatusScript(userId = 12)

        assertTrue(script.contains("USER_ID=12"))
        assertTrue(script.contains("settings --user"))
        assertTrue(script.contains("oaid_persistence_"))
    }
}
