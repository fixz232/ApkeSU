package me.weishu.kernelsu.ui.util

import android.os.Process
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.ByteArrayInputStream
import java.security.SecureRandom
import java.util.UUID

private const val DEVICE_IDENTITY_ROOT = "/data/adb/apkesu/device_identity"
private const val DEVICE_IDENTITY_ORIGINAL_DIR = "$DEVICE_IDENTITY_ROOT/original"
private const val DEVICE_IDENTITY_TARGET_DIR = "$DEVICE_IDENTITY_ROOT/target"
private const val DEVICE_IDENTITY_POST_FS_SCRIPT =
    "/data/adb/post-fs-data.d/96-apkesu-device-identity.sh"
private const val DEVICE_IDENTITY_SERVICE_SCRIPT =
    "/data/adb/service.d/96-apkesu-device-identity.sh"
private const val DEVICE_IDENTITY_TIMEOUT_MILLIS = 15_000L
private const val DEVICE_IDENTITY_ERROR_MARKER = "__APKESU_DEVICE_IDENTITY_ERROR__:"
private const val ABSENT_VALUE = "__APKESU_ABSENT__"

const val DEVICE_IDENTITY_SERIAL_MAX_BYTES = 64
const val DEVICE_IDENTITY_OAID_MAX_BYTES = 128

enum class DeviceIdentifierKind {
    SerialNumber,
    AndroidId,
    WifiMac,
    BluetoothAddress,
    Oaid,
}

enum class DeviceIdentifierSupport {
    Supported,
    Missing,
    UnsafeFormat,
    ToolUnavailable,
}

enum class DeviceIdentityValidationError {
    Empty,
    InvalidSerial,
    InvalidAndroidId,
    InvalidMac,
    MulticastMac,
    InvalidOaid,
}

enum class DeviceIdentityFailure {
    RootUnavailable,
    Unsupported,
    InvalidValue,
    BackupMissing,
    BackupFailed,
    PersistenceFailed,
    CommandTimeout,
    CommandFailed,
    VerificationFailed,
    RollbackFailed,
}

class DeviceIdentityException(
    val failure: DeviceIdentityFailure,
    message: String = failure.name,
) : IllegalStateException(message)

data class DeviceIdentityValidation(
    val normalizedValue: String,
    val error: DeviceIdentityValidationError? = null,
) {
    val isValid: Boolean
        get() = error == null
}

data class DeviceIdentifierState(
    val kind: DeviceIdentifierKind,
    val currentValue: String = "",
    val configuredValue: String = "",
    val originalValue: String = "",
    val hasBackup: Boolean = false,
    val support: DeviceIdentifierSupport = DeviceIdentifierSupport.Supported,
    val source: String = "",
    val persistent: Boolean = false,
    val runtimeVerified: Boolean? = null,
) {
    val hasConfiguredValue: Boolean
        get() = configuredValue.isNotBlank()

    val applied: Boolean
        get() = hasConfiguredValue &&
            (runtimeVerified ?: currentValue.equals(configuredValue, ignoreCase = true))
}

data class DeviceIdentitySnapshot(
    val rootAvailable: Boolean = false,
    val userId: Int = 0,
    val serialNumber: String = "",
    val bootSerialNumber: String = "",
    val identifiers: List<DeviceIdentifierState> = DeviceIdentifierKind.entries.map {
        DeviceIdentifierState(it, support = DeviceIdentifierSupport.Missing)
    },
    val error: String = "",
) {
    fun identifier(kind: DeviceIdentifierKind): DeviceIdentifierState =
        identifiers.firstOrNull { it.kind == kind }
            ?: DeviceIdentifierState(kind, support = DeviceIdentifierSupport.Missing)
}

data class DeviceIdentityActionResult(
    val success: Boolean,
    val snapshot: DeviceIdentitySnapshot,
    val failure: DeviceIdentityFailure? = null,
    val detail: String = "",
    val failedKinds: Set<DeviceIdentifierKind> = emptySet(),
)

fun validateDeviceIdentifier(
    kind: DeviceIdentifierKind,
    rawValue: String,
): DeviceIdentityValidation {
    val trimmed = rawValue.trim()
    if (trimmed.isEmpty()) {
        return DeviceIdentityValidation("", DeviceIdentityValidationError.Empty)
    }
    return when (kind) {
        DeviceIdentifierKind.SerialNumber -> {
            val valid = trimmed.length in 4..DEVICE_IDENTITY_SERIAL_MAX_BYTES &&
                trimmed.toByteArray(Charsets.UTF_8).size <= DEVICE_IDENTITY_SERIAL_MAX_BYTES &&
                trimmed.all { it.isAsciiLetterOrDigit() || it == '.' || it == '_' || it == '-' }
            DeviceIdentityValidation(
                normalizedValue = trimmed,
                error = if (valid) null else DeviceIdentityValidationError.InvalidSerial,
            )
        }

        DeviceIdentifierKind.AndroidId -> {
            val normalized = trimmed.lowercase()
            val valid = normalized.length == 16 && normalized.all(Char::isHexDigit)
            DeviceIdentityValidation(
                normalizedValue = normalized,
                error = if (valid) null else DeviceIdentityValidationError.InvalidAndroidId,
            )
        }

        DeviceIdentifierKind.WifiMac,
        DeviceIdentifierKind.BluetoothAddress,
        -> validateMacAddress(trimmed)

        DeviceIdentifierKind.Oaid -> {
            val valid = trimmed.length in 8..DEVICE_IDENTITY_OAID_MAX_BYTES &&
                trimmed.toByteArray(Charsets.UTF_8).size <= DEVICE_IDENTITY_OAID_MAX_BYTES &&
                trimmed.all { it.isAsciiLetterOrDigit() || it == '-' || it == '_' }
            DeviceIdentityValidation(
                normalizedValue = trimmed,
                error = if (valid) null else DeviceIdentityValidationError.InvalidOaid,
            )
        }
    }
}

fun generateDeviceIdentifier(
    kind: DeviceIdentifierKind,
    secureRandom: SecureRandom = SecureRandom(),
): String = when (kind) {
    DeviceIdentifierKind.SerialNumber -> randomHex(secureRandom, 8).uppercase()
    DeviceIdentifierKind.AndroidId -> randomHex(secureRandom, 8)
    DeviceIdentifierKind.WifiMac,
    DeviceIdentifierKind.BluetoothAddress,
    -> ByteArray(6).also(secureRandom::nextBytes).apply {
        this[0] = ((this[0].toInt() and 0xfc) or 0x02).toByte()
    }.joinToString(":") { "%02X".format(it.toInt() and 0xff) }

    DeviceIdentifierKind.Oaid -> UUID.randomUUID().toString()
}

class DeviceIdentityRepository(
    private val userId: Int = Process.myUid() / 100000,
) {
    suspend fun getSnapshot(): DeviceIdentitySnapshot = withRootShell { shell ->
        readSnapshot(shell)
    }

    suspend fun applyIdentifier(
        kind: DeviceIdentifierKind,
        rawValue: String,
    ): DeviceIdentityActionResult {
        val validation = validateDeviceIdentifier(kind, rawValue)
        if (!validation.isValid) {
            val snapshot = runCatching { getSnapshot() }.getOrDefault(DeviceIdentitySnapshot(userId = userId))
            return DeviceIdentityActionResult(
                success = false,
                snapshot = snapshot,
                failure = DeviceIdentityFailure.InvalidValue,
            )
        }
        return runAction { shell, before ->
            val identifier = before.identifier(kind)
            if (identifier.support != DeviceIdentifierSupport.Supported) {
                throw DeviceIdentityException(DeviceIdentityFailure.Unsupported)
            }
            val script = when (kind) {
                DeviceIdentifierKind.SerialNumber -> buildApplySerialScript(before, validation.normalizedValue)
                DeviceIdentifierKind.AndroidId -> buildApplySecureSettingScript(
                    before = before,
                    kind = kind,
                    settingName = "android_id",
                    targetPath = androidIdTargetPath(userId),
                    backupPath = androidIdBackupPath(userId),
                    value = validation.normalizedValue,
                )

                DeviceIdentifierKind.WifiMac -> buildApplyWifiScript(before, validation.normalizedValue)
                DeviceIdentifierKind.BluetoothAddress -> buildApplySecureSettingScript(
                    before = before,
                    kind = kind,
                    settingName = "bluetooth_address",
                    targetPath = bluetoothTargetPath(userId),
                    backupPath = bluetoothBackupPath(userId),
                    value = validation.normalizedValue,
                )

                DeviceIdentifierKind.Oaid -> buildApplyOaidScript(before, validation.normalizedValue)
            }
            runOperationScript(shell, script)
        }
    }

    suspend fun restoreIdentifier(kind: DeviceIdentifierKind): DeviceIdentityActionResult =
        runAction { shell, before ->
            val identifier = before.identifier(kind)
            if (!identifier.hasBackup) {
                throw DeviceIdentityException(DeviceIdentityFailure.BackupMissing)
            }
            val script = when (kind) {
                DeviceIdentifierKind.SerialNumber -> buildRestoreSerialScript(before.userId)
                DeviceIdentifierKind.AndroidId -> buildRestoreSecureSettingScript(
                    userId = before.userId,
                    settingName = "android_id",
                    targetPath = androidIdTargetPath(userId),
                    backupPath = androidIdBackupPath(userId),
                )

                DeviceIdentifierKind.WifiMac -> buildRestoreWifiScript(before.userId, identifier.source)
                DeviceIdentifierKind.BluetoothAddress -> buildRestoreSecureSettingScript(
                    userId = before.userId,
                    settingName = "bluetooth_address",
                    targetPath = bluetoothTargetPath(userId),
                    backupPath = bluetoothBackupPath(userId),
                )

                DeviceIdentifierKind.Oaid -> buildRestoreOaidScript(before.userId, identifier.source)
            }
            runOperationScript(shell, script)
        }

    suspend fun restoreAll(): DeviceIdentityActionResult {
        var snapshot = runCatching { getSnapshot() }.getOrElse { error ->
            return actionFailure(error, DeviceIdentitySnapshot(userId = userId))
        }
        val failed = linkedSetOf<DeviceIdentifierKind>()
        DeviceIdentifierKind.entries.forEach { kind ->
            if (!snapshot.identifier(kind).hasBackup) return@forEach
            val result = restoreIdentifier(kind)
            snapshot = result.snapshot
            if (!result.success) failed += kind
        }
        return DeviceIdentityActionResult(
            success = failed.isEmpty(),
            snapshot = snapshot,
            failure = if (failed.isEmpty()) null else DeviceIdentityFailure.CommandFailed,
            failedKinds = failed,
        )
    }

    private suspend fun runAction(
        operation: suspend (Shell, DeviceIdentitySnapshot) -> Unit,
    ): DeviceIdentityActionResult = try {
        withRootShell { shell ->
            val before = readSnapshot(shell)
            operation(shell, before)
            DeviceIdentityActionResult(
                success = true,
                snapshot = readSnapshot(shell),
            )
        }
    } catch (error: CancellationException) {
        throw error
    } catch (error: Throwable) {
        val snapshot = runCatching { getSnapshot() }.getOrDefault(DeviceIdentitySnapshot(userId = userId))
        actionFailure(error, snapshot)
    }

    private suspend fun readSnapshot(shell: Shell): DeviceIdentitySnapshot {
        val output = executeScript(shell, buildStatusScript(userId))
        if (!output.success) {
            throw DeviceIdentityException(
                DeviceIdentityFailure.CommandFailed,
                output.error.ifBlank { "status exited ${output.code}" },
            )
        }
        return parseDeviceIdentityStatus(output.stdout, userId)
    }

    private suspend fun runOperationScript(shell: Shell, script: String) {
        val output = executeScript(shell, script)
        if (output.success) return
        val detail = output.error.ifBlank { "operation exited ${output.code}" }
        throw DeviceIdentityException(mapOperationFailure(detail), detail)
    }

    private suspend fun executeScript(shell: Shell, script: String): RootCommandOutput {
        val stdout = ArrayList<String>()
        val stderr = ArrayList<String>()
        val result = withTimeoutOrNull(DEVICE_IDENTITY_TIMEOUT_MILLIS) {
            shell.newJob()
                .add(ByteArrayInputStream(script.toByteArray(Charsets.UTF_8)))
                .to(stdout, stderr)
                .exec()
        } ?: throw DeviceIdentityException(DeviceIdentityFailure.CommandTimeout)
        return RootCommandOutput(result.isSuccess, result.code, stdout, stderr.joinToString("\n"))
    }

    private suspend fun <T> withRootShell(block: suspend (Shell) -> T): T =
        withContext(Dispatchers.IO) {
            val shell = createRootShell(globalMnt = true)
            try {
                if (!runCatching { shell.isRoot }.getOrDefault(false)) {
                    throw DeviceIdentityException(DeviceIdentityFailure.RootUnavailable)
                }
                block(shell)
            } finally {
                runCatching { shell.close() }
            }
        }

    private data class RootCommandOutput(
        val success: Boolean,
        val code: Int,
        val stdout: List<String>,
        val error: String,
    )
}

internal fun parseDeviceIdentityStatus(
    lines: List<String>,
    userId: Int,
): DeviceIdentitySnapshot {
    val values = lines.mapNotNull { line ->
        val separator = line.indexOf('=')
        if (separator <= 0) null else line.substring(0, separator) to line.substring(separator + 1)
    }.toMap()

    fun flag(name: String): Boolean = values[name] == "1"
    fun value(name: String): String = values[name].orEmpty().takeUnless { it == "null" }.orEmpty()
    fun support(prefix: String): DeviceIdentifierSupport = when (values["${prefix}_support"]) {
        "supported" -> DeviceIdentifierSupport.Supported
        "unsafe_format" -> DeviceIdentifierSupport.UnsafeFormat
        "tool_unavailable" -> DeviceIdentifierSupport.ToolUnavailable
        else -> DeviceIdentifierSupport.Missing
    }

    val rawSerial = value("serial_current")
    val rawBootSerial = value("boot_serial_current")
    val serialCurrent = rawSerial.ifBlank { rawBootSerial }
    val serialTarget = value("serial_target")
    val androidIdCurrent = value("android_id_current")
    val androidIdTarget = value("android_id_target")
    val wifiCurrent = value("wifi_current").uppercase()
    val wifiTarget = value("wifi_target").uppercase()
    val bluetoothCurrent = value("bluetooth_current").uppercase()
    val bluetoothTarget = value("bluetooth_target").uppercase()
    val oaidCurrent = value("oaid_current")
    val oaidTarget = value("oaid_target")
    return DeviceIdentitySnapshot(
        rootAvailable = true,
        userId = userId,
        serialNumber = rawSerial,
        bootSerialNumber = rawBootSerial,
        identifiers = listOf(
            DeviceIdentifierState(
                kind = DeviceIdentifierKind.SerialNumber,
                currentValue = serialCurrent,
                configuredValue = serialTarget,
                originalValue = value("serial_original"),
                hasBackup = flag("serial_backup"),
                support = support("serial"),
                source = "ro.serialno / ro.boot.serialno",
                persistent = flag("serial_persistent"),
                runtimeVerified = serialTarget.isNotBlank() &&
                    rawSerial == serialTarget && rawBootSerial == serialTarget,
            ),
            DeviceIdentifierState(
                kind = DeviceIdentifierKind.AndroidId,
                currentValue = androidIdCurrent,
                configuredValue = androidIdTarget,
                originalValue = value("android_id_original").takeUnless { it == ABSENT_VALUE }.orEmpty(),
                hasBackup = flag("android_id_backup"),
                support = support("android_id"),
                source = "Settings.Secure.android_id (user $userId)",
                persistent = flag("android_id_target_exists"),
                runtimeVerified = androidIdTarget.isNotBlank() && androidIdCurrent == androidIdTarget,
            ),
            DeviceIdentifierState(
                kind = DeviceIdentifierKind.WifiMac,
                currentValue = wifiCurrent,
                configuredValue = wifiTarget,
                originalValue = value("wifi_original").uppercase(),
                hasBackup = flag("wifi_backup"),
                support = support("wifi"),
                source = value("wifi_interface"),
                persistent = flag("wifi_persistent"),
                runtimeVerified = wifiTarget.isNotBlank() && wifiCurrent == wifiTarget,
            ),
            DeviceIdentifierState(
                kind = DeviceIdentifierKind.BluetoothAddress,
                currentValue = bluetoothCurrent,
                configuredValue = bluetoothTarget,
                originalValue = value("bluetooth_original")
                    .takeUnless { it == ABSENT_VALUE }
                    .orEmpty()
                    .uppercase(),
                hasBackup = flag("bluetooth_backup"),
                support = support("bluetooth"),
                source = "Settings.Secure.bluetooth_address (user $userId)",
                persistent = flag("bluetooth_target_exists"),
                runtimeVerified = bluetoothTarget.isNotBlank() && bluetoothCurrent == bluetoothTarget,
            ),
            DeviceIdentifierState(
                kind = DeviceIdentifierKind.Oaid,
                currentValue = oaidCurrent,
                configuredValue = oaidTarget,
                originalValue = value("oaid_original"),
                hasBackup = flag("oaid_backup"),
                support = support("oaid"),
                source = value("oaid_path"),
                persistent = flag("oaid_target_exists"),
                runtimeVerified = oaidTarget.isNotBlank() && oaidCurrent == oaidTarget,
            ),
        ),
    )
}

internal fun buildStatusScript(userId: Int): String = """
    ROOT=${shellQuote(DEVICE_IDENTITY_ROOT)}
    ORIGINAL=${shellQuote(DEVICE_IDENTITY_ORIGINAL_DIR)}
    TARGET=${shellQuote(DEVICE_IDENTITY_TARGET_DIR)}
    USER_ID=$userId

    read_value() {
      [ -f "${'$'}1" ] && cat "${'$'}1" 2>/dev/null
    }
    print_value() {
      printf '%s=%s\n' "${'$'}1" "${'$'}2"
    }
    file_flag() {
      [ -f "${'$'}1" ] && printf '1' || printf '0'
    }
    executable_flag() {
      [ -x "${'$'}1" ] && printf '1' || printf '0'
    }

    serial_current="${'$'}(getprop ro.serialno 2>/dev/null)"
    boot_serial_current="${'$'}(getprop ro.boot.serialno 2>/dev/null)"
    if [ -x /data/adb/ksu/bin/resetprop ] || [ -x /data/adb/ksud ]; then
      serial_support=supported
    else
      serial_support=tool_unavailable
    fi

    if command -v settings >/dev/null 2>&1; then
      android_id_support=supported
      bluetooth_support=supported
      android_id_current="${'$'}(settings --user "${'$'}USER_ID" get secure android_id 2>/dev/null)"
      bluetooth_current="${'$'}(settings --user "${'$'}USER_ID" get secure bluetooth_address 2>/dev/null)"
      [ "${'$'}android_id_current" = null ] && android_id_current=
      [ "${'$'}bluetooth_current" = null ] && bluetooth_current=
    else
      android_id_support=tool_unavailable
      bluetooth_support=tool_unavailable
      android_id_current=
      bluetooth_current=
    fi

    wifi_interface=
    wifi_current=
    for candidate in /sys/class/net/wlan*; do
      [ -r "${'$'}candidate/address" ] || continue
      wifi_interface="${'$'}{candidate##*/}"
      wifi_current="${'$'}(cat "${'$'}candidate/address" 2>/dev/null)"
      break
    done
    if [ -n "${'$'}wifi_interface" ] && command -v ip >/dev/null 2>&1; then
      wifi_support=supported
    elif [ -n "${'$'}wifi_interface" ]; then
      wifi_support=tool_unavailable
    else
      wifi_support=missing
    fi

    oaid_path=
    oaid_current=
    oaid_support=missing
    for candidate in /data/system/oaid_persistence_"${'$'}USER_ID" /data/system/oaid_persistence_0 /data/system/oaid_persistence; do
      [ -f "${'$'}candidate" ] || continue
      oaid_path="${'$'}candidate"
      oaid_size="${'$'}(wc -c < "${'$'}candidate" 2>/dev/null | tr -d '[:space:]')"
      case "${'$'}oaid_size" in
        ''|*[!0-9]*) oaid_support=unsafe_format ;;
        *)
          if [ "${'$'}oaid_size" -le 256 ]; then
            oaid_current="${'$'}(tr -d '\r\n' < "${'$'}candidate" 2>/dev/null)"
            if printf '%s' "${'$'}oaid_current" | grep -Eq '^[A-Za-z0-9_-]{8,128}${'$'}'; then
              oaid_support=supported
            else
              oaid_current=
              oaid_support=unsafe_format
            fi
          else
            oaid_support=unsafe_format
          fi
          ;;
      esac
      break
    done

    print_value serial_support "${'$'}serial_support"
    print_value serial_current "${'$'}serial_current"
    print_value boot_serial_current "${'$'}boot_serial_current"
    print_value serial_target "${'$'}(read_value "${'$'}TARGET/serial")"
    print_value serial_original "${'$'}(read_value "${'$'}ORIGINAL/serial_ro")"
    print_value serial_backup "${'$'}(file_flag "${'$'}ORIGINAL/serial_ro")"
    if [ -f "${'$'}TARGET/serial" ] && [ -x ${shellQuote(DEVICE_IDENTITY_POST_FS_SCRIPT)} ]; then
      print_value serial_persistent 1
    else
      print_value serial_persistent 0
    fi

    print_value android_id_support "${'$'}android_id_support"
    print_value android_id_current "${'$'}android_id_current"
    print_value android_id_target "${'$'}(read_value "${'$'}TARGET/android_id_${'$'}USER_ID")"
    print_value android_id_original "${'$'}(read_value "${'$'}ORIGINAL/android_id_${'$'}USER_ID")"
    print_value android_id_backup "${'$'}(file_flag "${'$'}ORIGINAL/android_id_${'$'}USER_ID")"
    print_value android_id_target_exists "${'$'}(file_flag "${'$'}TARGET/android_id_${'$'}USER_ID")"

    print_value wifi_support "${'$'}wifi_support"
    print_value wifi_interface "${'$'}wifi_interface"
    print_value wifi_current "${'$'}wifi_current"
    print_value wifi_target "${'$'}(read_value "${'$'}TARGET/wifi_mac")"
    print_value wifi_original "${'$'}(read_value "${'$'}ORIGINAL/wifi_mac")"
    print_value wifi_backup "${'$'}(file_flag "${'$'}ORIGINAL/wifi_mac")"
    if [ -f "${'$'}TARGET/wifi_mac" ] && [ -x ${shellQuote(DEVICE_IDENTITY_SERVICE_SCRIPT)} ]; then
      print_value wifi_persistent 1
    else
      print_value wifi_persistent 0
    fi

    print_value bluetooth_support "${'$'}bluetooth_support"
    print_value bluetooth_current "${'$'}bluetooth_current"
    print_value bluetooth_target "${'$'}(read_value "${'$'}TARGET/bluetooth_${'$'}USER_ID")"
    print_value bluetooth_original "${'$'}(read_value "${'$'}ORIGINAL/bluetooth_${'$'}USER_ID")"
    print_value bluetooth_backup "${'$'}(file_flag "${'$'}ORIGINAL/bluetooth_${'$'}USER_ID")"
    print_value bluetooth_target_exists "${'$'}(file_flag "${'$'}TARGET/bluetooth_${'$'}USER_ID")"

    print_value oaid_support "${'$'}oaid_support"
    print_value oaid_path "${'$'}oaid_path"
    print_value oaid_current "${'$'}oaid_current"
    print_value oaid_target "${'$'}(read_value "${'$'}TARGET/oaid")"
    print_value oaid_original "${'$'}(read_value "${'$'}ORIGINAL/oaid")"
    print_value oaid_backup "${'$'}(file_flag "${'$'}ORIGINAL/oaid")"
    print_value oaid_target_exists "${'$'}(file_flag "${'$'}TARGET/oaid")"
""".trimIndent()

private fun buildApplySerialScript(
    before: DeviceIdentitySnapshot,
    value: String,
): String {
    val identifier = before.identifier(DeviceIdentifierKind.SerialNumber)
    val previousTarget = identifier.configuredValue.takeIf(String::isNotBlank)
    val serialCurrent = before.serialNumber
    val bootCurrent = before.bootSerialNumber
    return buildString {
        appendOperationHeader(before.userId)
        appendLine("ensure_backup ${shellQuote("$DEVICE_IDENTITY_ORIGINAL_DIR/serial_ro")} ${shellQuote(serialCurrent)} || fail backup_failed")
        appendLine("ensure_backup ${shellQuote("$DEVICE_IDENTITY_ORIGINAL_DIR/serial_boot")} ${shellQuote(bootCurrent)} || fail backup_failed")
        appendAtomicPayload(DEVICE_IDENTITY_POST_FS_SCRIPT, SERIAL_POST_FS_SCRIPT, "0755")
        appendLine("if ! write_value ${shellQuote("$DEVICE_IDENTITY_TARGET_DIR/serial")} ${shellQuote(value)} 0600; then")
        if (previousTarget == null) appendLine("  rm -f ${shellQuote(DEVICE_IDENTITY_POST_FS_SCRIPT)}")
        appendLine("  fail persistence_failed")
        appendLine("fi")
        appendLine("if ! apply_serial_pair ${shellQuote(value)} ${shellQuote(value)}; then")
        appendTargetRollback("$DEVICE_IDENTITY_TARGET_DIR/serial", previousTarget)
        if (previousTarget == null) appendLine("  rm -f ${shellQuote(DEVICE_IDENTITY_POST_FS_SCRIPT)}")
        appendLine("  apply_serial_pair ${shellQuote(serialCurrent)} ${shellQuote(bootCurrent)} >/dev/null 2>&1 || fail rollback_failed")
        appendLine("  fail verification_failed")
        appendLine("fi")
    }
}

private fun buildRestoreSerialScript(userId: Int): String = buildString {
    appendOperationHeader(userId)
    appendLine("[ -f ${shellQuote("$DEVICE_IDENTITY_ORIGINAL_DIR/serial_ro")} ] || fail backup_missing")
    appendLine("[ -f ${shellQuote("$DEVICE_IDENTITY_ORIGINAL_DIR/serial_boot")} ] || fail backup_missing")
    appendLine("original_ro=\"${'$'}(cat ${shellQuote("$DEVICE_IDENTITY_ORIGINAL_DIR/serial_ro")})\"")
    appendLine("original_boot=\"${'$'}(cat ${shellQuote("$DEVICE_IDENTITY_ORIGINAL_DIR/serial_boot")})\"")
    appendLine("apply_serial_pair \"${'$'}original_ro\" \"${'$'}original_boot\" || fail verification_failed")
    appendLine("rm -f ${shellQuote("$DEVICE_IDENTITY_TARGET_DIR/serial")} ${shellQuote(DEVICE_IDENTITY_POST_FS_SCRIPT)}")
    appendLine("rm -f ${shellQuote("$DEVICE_IDENTITY_ORIGINAL_DIR/serial_ro")} ${shellQuote("$DEVICE_IDENTITY_ORIGINAL_DIR/serial_boot")}")
}

private fun buildApplySecureSettingScript(
    before: DeviceIdentitySnapshot,
    kind: DeviceIdentifierKind,
    settingName: String,
    targetPath: String,
    backupPath: String,
    value: String,
): String {
    val identifier = before.identifier(kind)
    val previousTarget = identifier.configuredValue.takeIf(String::isNotBlank)
    val originalForBackup = identifier.currentValue.ifBlank { ABSENT_VALUE }
    return buildString {
        appendOperationHeader(before.userId)
        appendLine("ensure_backup ${shellQuote(backupPath)} ${shellQuote(originalForBackup)} || fail backup_failed")
        appendLine("write_value ${shellQuote(targetPath)} ${shellQuote(value)} 0600 || fail persistence_failed")
        appendLine("if ! put_secure_setting ${shellQuote(settingName)} ${shellQuote(value)}; then")
        appendTargetRollback(targetPath, previousTarget)
        appendSecureSettingRollback(settingName, identifier.currentValue)
        appendLine("  fail verification_failed")
        appendLine("fi")
    }
}

private fun buildRestoreSecureSettingScript(
    userId: Int,
    settingName: String,
    targetPath: String,
    backupPath: String,
): String = buildString {
    appendOperationHeader(userId)
    appendLine("[ -f ${shellQuote(backupPath)} ] || fail backup_missing")
    appendLine("original=\"${'$'}(cat ${shellQuote(backupPath)})\"")
    appendLine("if [ \"${'$'}original\" = ${shellQuote(ABSENT_VALUE)} ]; then")
    appendLine("  delete_secure_setting ${shellQuote(settingName)} || fail verification_failed")
    appendLine("else")
    appendLine("  put_secure_setting ${shellQuote(settingName)} \"${'$'}original\" || fail verification_failed")
    appendLine("fi")
    appendLine("rm -f ${shellQuote(targetPath)} ${shellQuote(backupPath)}")
}

private fun buildApplyWifiScript(
    before: DeviceIdentitySnapshot,
    value: String,
): String {
    val identifier = before.identifier(DeviceIdentifierKind.WifiMac)
    val previousTarget = identifier.configuredValue.takeIf(String::isNotBlank)
    return buildString {
        appendOperationHeader(before.userId)
        appendLine("iface=${shellQuote(identifier.source)}")
        appendLine("[ -n \"${'$'}iface\" ] || fail unsupported")
        appendLine("ensure_backup ${shellQuote("$DEVICE_IDENTITY_ORIGINAL_DIR/wifi_mac")} ${shellQuote(identifier.currentValue)} || fail backup_failed")
        appendAtomicPayload(DEVICE_IDENTITY_SERVICE_SCRIPT, WIFI_SERVICE_SCRIPT, "0755")
        appendLine("if ! write_value ${shellQuote("$DEVICE_IDENTITY_TARGET_DIR/wifi_mac")} ${shellQuote(value)} 0600; then")
        if (previousTarget == null) appendLine("  rm -f ${shellQuote(DEVICE_IDENTITY_SERVICE_SCRIPT)}")
        appendLine("  fail persistence_failed")
        appendLine("fi")
        appendLine("if ! apply_interface_mac \"${'$'}iface\" ${shellQuote(value)}; then")
        appendTargetRollback("$DEVICE_IDENTITY_TARGET_DIR/wifi_mac", previousTarget)
        if (previousTarget == null) appendLine("  rm -f ${shellQuote(DEVICE_IDENTITY_SERVICE_SCRIPT)}")
        appendLine("  apply_interface_mac \"${'$'}iface\" ${shellQuote(identifier.currentValue)} >/dev/null 2>&1 || fail rollback_failed")
        appendLine("  fail verification_failed")
        appendLine("fi")
    }
}

private fun buildRestoreWifiScript(userId: Int, interfaceName: String): String = buildString {
    appendOperationHeader(userId)
    appendLine("[ -n ${shellQuote(interfaceName)} ] || fail unsupported")
    appendLine("[ -f ${shellQuote("$DEVICE_IDENTITY_ORIGINAL_DIR/wifi_mac")} ] || fail backup_missing")
    appendLine("original=\"${'$'}(cat ${shellQuote("$DEVICE_IDENTITY_ORIGINAL_DIR/wifi_mac")})\"")
    appendLine("apply_interface_mac ${shellQuote(interfaceName)} \"${'$'}original\" || fail verification_failed")
    appendLine("rm -f ${shellQuote("$DEVICE_IDENTITY_TARGET_DIR/wifi_mac")} ${shellQuote(DEVICE_IDENTITY_SERVICE_SCRIPT)}")
    appendLine("rm -f ${shellQuote("$DEVICE_IDENTITY_ORIGINAL_DIR/wifi_mac")}")
}

private fun buildApplyOaidScript(
    before: DeviceIdentitySnapshot,
    value: String,
): String {
    val identifier = before.identifier(DeviceIdentifierKind.Oaid)
    val previousTarget = identifier.configuredValue.takeIf(String::isNotBlank)
    return buildString {
        appendOperationHeader(before.userId)
        appendLine("oaid_path=${shellQuote(identifier.source)}")
        appendLine("[ -f \"${'$'}oaid_path\" ] || fail unsupported")
        appendLine("ensure_backup ${shellQuote("$DEVICE_IDENTITY_ORIGINAL_DIR/oaid")} ${shellQuote(identifier.currentValue)} || fail backup_failed")
        appendLine("ensure_backup ${shellQuote("$DEVICE_IDENTITY_ORIGINAL_DIR/oaid_path")} ${shellQuote(identifier.source)} || fail backup_failed")
        appendLine("write_value ${shellQuote("$DEVICE_IDENTITY_TARGET_DIR/oaid")} ${shellQuote(value)} 0600 || fail persistence_failed")
        appendLine("if ! write_oaid \"${'$'}oaid_path\" ${shellQuote(value)}; then")
        appendTargetRollback("$DEVICE_IDENTITY_TARGET_DIR/oaid", previousTarget)
        appendLine("  write_oaid \"${'$'}oaid_path\" ${shellQuote(identifier.currentValue)} >/dev/null 2>&1 || fail rollback_failed")
        appendLine("  fail verification_failed")
        appendLine("fi")
    }
}

private fun buildRestoreOaidScript(userId: Int, currentPath: String): String = buildString {
    appendOperationHeader(userId)
    appendLine("[ -f ${shellQuote("$DEVICE_IDENTITY_ORIGINAL_DIR/oaid")} ] || fail backup_missing")
    appendLine("original=\"${'$'}(cat ${shellQuote("$DEVICE_IDENTITY_ORIGINAL_DIR/oaid")})\"")
    appendLine("saved_path=\"${'$'}(cat ${shellQuote("$DEVICE_IDENTITY_ORIGINAL_DIR/oaid_path")} 2>/dev/null)\"")
    appendLine("[ -n \"${'$'}saved_path\" ] || saved_path=${shellQuote(currentPath)}")
    appendLine("[ -f \"${'$'}saved_path\" ] || fail unsupported")
    appendLine("write_oaid \"${'$'}saved_path\" \"${'$'}original\" || fail verification_failed")
    appendLine("rm -f ${shellQuote("$DEVICE_IDENTITY_TARGET_DIR/oaid")} ${shellQuote("$DEVICE_IDENTITY_ORIGINAL_DIR/oaid")} ${shellQuote("$DEVICE_IDENTITY_ORIGINAL_DIR/oaid_path")}")
}

private fun StringBuilder.appendOperationHeader(userId: Int) {
    appendLine("umask 077")
    appendLine("ROOT=${shellQuote(DEVICE_IDENTITY_ROOT)}")
    appendLine("ORIGINAL=${shellQuote(DEVICE_IDENTITY_ORIGINAL_DIR)}")
    appendLine("TARGET=${shellQuote(DEVICE_IDENTITY_TARGET_DIR)}")
    appendLine("USER_ID=$userId")
    appendLine("mkdir -p \"${'$'}ORIGINAL\" \"${'$'}TARGET\" /data/adb/post-fs-data.d /data/adb/service.d || exit 1")
    appendLine("chmod 0700 \"${'$'}ROOT\" \"${'$'}ORIGINAL\" \"${'$'}TARGET\" 2>/dev/null")
    appendLine("fail() { printf '${DEVICE_IDENTITY_ERROR_MARKER}%s\\n' \"${'$'}1\" >&2; exit 1; }")
    appendLine("write_value() {")
    appendLine("  wv_path=\"${'$'}1\"; wv_value=\"${'$'}2\"; wv_mode=\"${'$'}3\"; wv_tmp=\"${'$'}{wv_path}.tmp.${'$'}${'$'}\"")
    appendLine("  printf '%s' \"${'$'}wv_value\" > \"${'$'}wv_tmp\" || return 1")
    appendLine("  chmod \"${'$'}wv_mode\" \"${'$'}wv_tmp\" 2>/dev/null || { rm -f \"${'$'}wv_tmp\"; return 1; }")
    appendLine("  mv -f \"${'$'}wv_tmp\" \"${'$'}wv_path\" || { rm -f \"${'$'}wv_tmp\"; return 1; }")
    appendLine("}")
    appendLine("ensure_backup() { [ -f \"${'$'}1\" ] || write_value \"${'$'}1\" \"${'$'}2\" 0600; }")
    appendLine("run_resetprop() {")
    appendLine("  if [ -x /data/adb/ksu/bin/resetprop ]; then /data/adb/ksu/bin/resetprop \"${'$'}@\"")
    appendLine("  elif [ -x /data/adb/ksud ]; then /data/adb/ksud resetprop \"${'$'}@\"")
    appendLine("  else return 1; fi")
    appendLine("}")
    appendLine("apply_serial_pair() {")
    appendLine("  run_resetprop ro.serialno \"${'$'}1\" >/dev/null 2>&1 || return 1")
    appendLine("  run_resetprop ro.boot.serialno \"${'$'}2\" >/dev/null 2>&1 || return 1")
    appendLine("  [ \"${'$'}(getprop ro.serialno 2>/dev/null)\" = \"${'$'}1\" ] || return 1")
    appendLine("  [ \"${'$'}(getprop ro.boot.serialno 2>/dev/null)\" = \"${'$'}2\" ] || return 1")
    appendLine("}")
    appendLine("secure_value() { value=\"${'$'}(settings --user \"${'$'}USER_ID\" get secure \"${'$'}1\" 2>/dev/null)\"; [ \"${'$'}value\" = null ] && value=; printf '%s' \"${'$'}value\"; }")
    appendLine("put_secure_setting() { settings --user \"${'$'}USER_ID\" put secure \"${'$'}1\" \"${'$'}2\" >/dev/null 2>&1 || return 1; [ \"${'$'}(secure_value \"${'$'}1\")\" = \"${'$'}2\" ]; }")
    appendLine("delete_secure_setting() { settings --user \"${'$'}USER_ID\" delete secure \"${'$'}1\" >/dev/null 2>&1 || return 1; [ -z \"${'$'}(secure_value \"${'$'}1\")\" ]; }")
    appendLine("apply_interface_mac() {")
    appendLine("  am_iface=\"${'$'}1\"; am_value=\"${'$'}2\"; am_up=0")
    appendLine("  ip link show dev \"${'$'}am_iface\" 2>/dev/null | grep -q '<[^>]*UP[^>]*>' && am_up=1")
    appendLine("  ip link set dev \"${'$'}am_iface\" down >/dev/null 2>&1 || return 1")
    appendLine("  ip link set dev \"${'$'}am_iface\" address \"${'$'}am_value\" >/dev/null 2>&1 || { [ \"${'$'}am_up\" = 1 ] && ip link set dev \"${'$'}am_iface\" up >/dev/null 2>&1; return 1; }")
    appendLine("  [ \"${'$'}am_up\" = 1 ] && ip link set dev \"${'$'}am_iface\" up >/dev/null 2>&1")
    appendLine("  am_current=\"${'$'}(cat \"/sys/class/net/${'$'}am_iface/address\" 2>/dev/null | tr '[:lower:]' '[:upper:]')\"")
    appendLine("  am_expected=\"${'$'}(printf '%s' \"${'$'}am_value\" | tr '[:lower:]' '[:upper:]')\"")
    appendLine("  [ \"${'$'}am_current\" = \"${'$'}am_expected\" ]")
    appendLine("}")
    appendLine("write_oaid() {")
    appendLine("  printf '%s' \"${'$'}2\" > \"${'$'}1\" || return 1")
    appendLine("  command -v restorecon >/dev/null 2>&1 && restorecon \"${'$'}1\" >/dev/null 2>&1")
    appendLine("  sync \"${'$'}1\" >/dev/null 2>&1 || sync >/dev/null 2>&1")
    appendLine("  [ \"${'$'}(tr -d '\\r\\n' < \"${'$'}1\" 2>/dev/null)\" = \"${'$'}2\" ]")
    appendLine("}")
}

private fun StringBuilder.appendAtomicPayload(path: String, payload: String, mode: String) {
    val normalizedPayload = if (payload.endsWith('\n')) payload else "$payload\n"
    val delimiter = heredocDelimiter("IDENTITY", normalizedPayload)
    appendLine("payload_tmp=${shellQuote("$path.tmp")}.${'$'}${'$'}")
    appendLine("cat > \"${'$'}payload_tmp\" <<'$delimiter' || fail persistence_failed")
    append(normalizedPayload)
    appendLine(delimiter)
    appendLine("chmod $mode \"${'$'}payload_tmp\" 2>/dev/null || { rm -f \"${'$'}payload_tmp\"; fail persistence_failed; }")
    appendLine("mv -f \"${'$'}payload_tmp\" ${shellQuote(path)} || { rm -f \"${'$'}payload_tmp\"; fail persistence_failed; }")
}

private fun StringBuilder.appendTargetRollback(path: String, previousTarget: String?) {
    if (previousTarget == null) {
        appendLine("  rm -f ${shellQuote(path)}")
    } else {
        appendLine("  write_value ${shellQuote(path)} ${shellQuote(previousTarget)} 0600 >/dev/null 2>&1")
    }
}

private fun StringBuilder.appendSecureSettingRollback(settingName: String, currentValue: String) {
    if (currentValue.isBlank()) {
        appendLine("  delete_secure_setting ${shellQuote(settingName)} >/dev/null 2>&1 || fail rollback_failed")
    } else {
        appendLine("  put_secure_setting ${shellQuote(settingName)} ${shellQuote(currentValue)} >/dev/null 2>&1 || fail rollback_failed")
    }
}

private fun actionFailure(error: Throwable, snapshot: DeviceIdentitySnapshot): DeviceIdentityActionResult {
    val exception = error as? DeviceIdentityException
    return DeviceIdentityActionResult(
        success = false,
        snapshot = snapshot,
        failure = exception?.failure ?: DeviceIdentityFailure.CommandFailed,
        detail = error.message.orEmpty(),
    )
}

private fun mapOperationFailure(detail: String): DeviceIdentityFailure {
    val code = detail.lineSequence()
        .firstOrNull { DEVICE_IDENTITY_ERROR_MARKER in it }
        ?.substringAfter(DEVICE_IDENTITY_ERROR_MARKER)
        ?.trim()
    return when (code) {
        "unsupported" -> DeviceIdentityFailure.Unsupported
        "backup_missing" -> DeviceIdentityFailure.BackupMissing
        "backup_failed" -> DeviceIdentityFailure.BackupFailed
        "persistence_failed" -> DeviceIdentityFailure.PersistenceFailed
        "verification_failed" -> DeviceIdentityFailure.VerificationFailed
        "rollback_failed" -> DeviceIdentityFailure.RollbackFailed
        else -> DeviceIdentityFailure.CommandFailed
    }
}

private fun validateMacAddress(value: String): DeviceIdentityValidation {
    val normalized = value.replace('-', ':').uppercase()
    val parts = normalized.split(':')
    if (parts.size != 6 || parts.any { part ->
            part.length != 2 || part.any { character -> !character.isHexDigit() }
        }
    ) {
        return DeviceIdentityValidation(normalized, DeviceIdentityValidationError.InvalidMac)
    }
    val bytes = parts.map { it.toInt(16) }
    if (bytes.all { it == 0 } || bytes.all { it == 0xff }) {
        return DeviceIdentityValidation(normalized, DeviceIdentityValidationError.InvalidMac)
    }
    if (bytes.first() and 0x01 != 0) {
        return DeviceIdentityValidation(normalized, DeviceIdentityValidationError.MulticastMac)
    }
    return DeviceIdentityValidation(normalized)
}

private fun Char.isHexDigit(): Boolean = this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'

private fun Char.isAsciiLetterOrDigit(): Boolean =
    this in '0'..'9' || this in 'a'..'z' || this in 'A'..'Z'

private fun randomHex(random: SecureRandom, byteCount: Int): String =
    ByteArray(byteCount).also(random::nextBytes).joinToString("") {
        "%02x".format(it.toInt() and 0xff)
    }

private fun androidIdTargetPath(userId: Int): String = "$DEVICE_IDENTITY_TARGET_DIR/android_id_$userId"
private fun androidIdBackupPath(userId: Int): String = "$DEVICE_IDENTITY_ORIGINAL_DIR/android_id_$userId"
private fun bluetoothTargetPath(userId: Int): String = "$DEVICE_IDENTITY_TARGET_DIR/bluetooth_$userId"
private fun bluetoothBackupPath(userId: Int): String = "$DEVICE_IDENTITY_ORIGINAL_DIR/bluetooth_$userId"

private fun shellQuote(value: String): String = "'${value.replace("'", "'\"'\"'")}'"

private fun heredocDelimiter(label: String, payload: String): String {
    var delimiter = "__APKESU_${label}_EOF__"
    val lines = payload.lineSequence().toSet()
    while (delimiter in lines) delimiter += "_"
    return delimiter
}

private val SERIAL_POST_FS_SCRIPT = """
    #!/system/bin/sh
    TARGET=/data/adb/apkesu/device_identity/target/serial

    run_resetprop() {
      if [ -x /data/adb/ksu/bin/resetprop ]; then
        /data/adb/ksu/bin/resetprop "${'$'}@"
      elif [ -x /data/adb/ksud ]; then
        /data/adb/ksud resetprop "${'$'}@"
      else
        return 1
      fi
    }

    [ -r "${'$'}TARGET" ] || exit 0
    value="${'$'}(cat "${'$'}TARGET" 2>/dev/null)"
    [ -n "${'$'}value" ] || exit 0
    run_resetprop ro.serialno "${'$'}value" >/dev/null 2>&1 || exit 1
    run_resetprop ro.boot.serialno "${'$'}value" >/dev/null 2>&1 || exit 1
    exit 0
""".trimIndent()

private val WIFI_SERVICE_SCRIPT = """
    #!/system/bin/sh
    TARGET=/data/adb/apkesu/device_identity/target/wifi_mac

    [ -r "${'$'}TARGET" ] || exit 0
    value="${'$'}(cat "${'$'}TARGET" 2>/dev/null | tr '[:lower:]' '[:upper:]')"
    [ -n "${'$'}value" ] || exit 0

    (
      boot_wait=0
      while [ "${'$'}(getprop sys.boot_completed 2>/dev/null)" != 1 ] && [ "${'$'}boot_wait" -lt 60 ]; do
        boot_wait="${'$'}((boot_wait + 1))"
        sleep 2
      done

      attempt=0
      while [ "${'$'}attempt" -lt 15 ]; do
        iface=
        for candidate in /sys/class/net/wlan*; do
          [ -r "${'$'}candidate/address" ] || continue
          iface="${'$'}{candidate##*/}"
          break
        done
        if [ -n "${'$'}iface" ]; then
          was_up=0
          ip link show dev "${'$'}iface" 2>/dev/null | grep -q '<[^>]*UP[^>]*>' && was_up=1
          ip link set dev "${'$'}iface" down >/dev/null 2>&1 || exit 1
          ip link set dev "${'$'}iface" address "${'$'}value" >/dev/null 2>&1 || {
            [ "${'$'}was_up" = 1 ] && ip link set dev "${'$'}iface" up >/dev/null 2>&1
            exit 1
          }
          [ "${'$'}was_up" = 1 ] && ip link set dev "${'$'}iface" up >/dev/null 2>&1
          current="${'$'}(cat "/sys/class/net/${'$'}iface/address" 2>/dev/null | tr '[:lower:]' '[:upper:]')"
          [ "${'$'}current" = "${'$'}value" ] && exit 0
          exit 1
        fi
        attempt="${'$'}((attempt + 1))"
        sleep 2
      done
      exit 1
    ) >/dev/null 2>&1 &
    exit 0
""".trimIndent()
