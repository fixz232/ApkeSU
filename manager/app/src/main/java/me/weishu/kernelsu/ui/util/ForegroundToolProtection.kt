package me.weishu.kernelsu.ui.util

import android.content.Context
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import me.weishu.kernelsu.ksuApp
import java.io.ByteArrayInputStream

private const val FOREGROUND_TOOL_BASE = "/data/adb/apkesu/foreground_tools"
private const val FOREGROUND_TOOL_ENABLED = "$FOREGROUND_TOOL_BASE/enabled"
private const val FOREGROUND_TOOL_TARGETS = "$FOREGROUND_TOOL_BASE/targets.list"
private const val FOREGROUND_TOOL_TOOLS = "$FOREGROUND_TOOL_BASE/tools.list"
private const val FOREGROUND_TOOL_STATUS = "$FOREGROUND_TOOL_BASE/status.properties"
private const val FOREGROUND_TOOL_LOG = "$FOREGROUND_TOOL_BASE/events.log"
private const val FOREGROUND_TOOL_PID = "$FOREGROUND_TOOL_BASE/service.pid"
private const val FOREGROUND_TOOL_SERVICE = "/data/adb/service.d/97-apkesu-foreground-tools.sh"
private const val FOREGROUND_TOOL_SERVICE_ASSET = "foreground_tool_service.sh"
private const val FOREGROUND_TOOL_SERVICE_VERSION = 2
private const val FOREGROUND_TOOL_DAEMON_MARKER = "APKESU_FOREGROUND_TOOL_DAEMON=1"
private const val FOREGROUND_TOOL_COMMAND_TIMEOUT_MILLIS = 12_000L
private const val FOREGROUND_TOOL_RESTART_GRACE_MILLIS = 2_200L
private const val FOREGROUND_TOOL_INSTALL_SUCCESS_MARKER = "__APKESU_FOREGROUND_TOOL_INSTALL_OK__"
private const val FOREGROUND_TOOL_INSTALL_FAILURE_MARKER = "__APKESU_FOREGROUND_TOOL_INSTALL_FAILED__:"

val DEFAULT_FOREGROUND_TOOL_PACKAGES = setOf(
    "bin.mt.plus",
    "bin.mt.termex",
    "bin.mt.plus.canary",
    "me.bmax.apatch",
    "github.ColdAsSunny.Kernel",
    "mi.yuki.folk",
    "me.yuki.folk",
    "me.weishu.kernelsu",
    "com.sukisu.ultra",
    "io.github.fixz.apkesu",
)

enum class ForegroundToolRuntimeState {
    Disabled,
    Waiting,
    Active,
    Error,
}

enum class ForegroundToolFailure {
    RootUnavailable,
    InvalidPackage,
    TargetRequired,
    ToolRequired,
    SelectionConflict,
    CommandTimeout,
    CommandFailed,
    ServiceStartFailed,
    InstallPreparationFailed,
    ConfigStagingFailed,
    ServiceInstallFailed,
    InstallVerificationFailed,
}

class ForegroundToolException(
    val failure: ForegroundToolFailure,
    message: String = failure.name,
) : IllegalStateException(message)

data class ForegroundToolConfig(
    val enabled: Boolean = false,
    val targets: Set<String> = emptySet(),
    val tools: Set<String> = emptySet(),
)

data class ForegroundToolStatus(
    val config: ForegroundToolConfig = ForegroundToolConfig(),
    val configPresent: Boolean = false,
    val runtimeState: ForegroundToolRuntimeState = ForegroundToolRuntimeState.Disabled,
    val foregroundPackage: String = "",
    val matchedTarget: String = "",
    val stoppedCount: Int = 0,
    val failedCount: Int = 0,
    val lastEvent: String = "",
    val updatedAt: String = "",
    val serviceInstalled: Boolean = false,
    val serviceRunning: Boolean = false,
    val serviceVersion: Int = 0,
    val recentLog: List<String> = emptyList(),
    val error: ForegroundToolFailure? = null,
)

private val FOREGROUND_TOOL_PACKAGE_REGEX =
    Regex("^[A-Za-z][A-Za-z0-9_]*(?:\\.[A-Za-z][A-Za-z0-9_]*)+$")

internal fun isValidForegroundToolPackage(packageName: String): Boolean =
    FOREGROUND_TOOL_PACKAGE_REGEX.matches(packageName)

internal fun normalizeForegroundToolPackages(packages: Iterable<String>): Set<String> =
    packages.asSequence()
        .map(String::trim)
        .filter(::isValidForegroundToolPackage)
        .toCollection(linkedSetOf())

internal fun validateForegroundToolEnable(
    targets: Set<String>,
    tools: Set<String>,
): ForegroundToolFailure? = when {
    targets.isEmpty() -> ForegroundToolFailure.TargetRequired
    tools.isEmpty() -> ForegroundToolFailure.ToolRequired
    targets.any(tools::contains) -> ForegroundToolFailure.SelectionConflict
    else -> null
}

internal fun parseForegroundToolProperties(lines: List<String>): Map<String, String> =
    lines.mapNotNull { line ->
        val separator = line.indexOf('=')
        if (separator <= 0) return@mapNotNull null
        val key = line.substring(0, separator).trim()
        val value = line.substring(separator + 1).trim()
        key.takeIf { it.matches(Regex("^[a-z_]+$")) }?.let { it to value }
    }.toMap()

internal fun shouldUpgradeForegroundToolService(status: ForegroundToolStatus): Boolean =
    status.config.enabled &&
        status.configPresent &&
        status.serviceVersion != FOREGROUND_TOOL_SERVICE_VERSION

class ForegroundToolProtectionRepository(
    context: Context = ksuApp,
) {
    private val appContext = context.applicationContext

    suspend fun getStatus(): ForegroundToolStatus = withRootShell { shell ->
        var status = readStatus(shell)
        if (shouldUpgradeForegroundToolService(status)) {
            installConfig(shell, status.config.targets, status.config.tools)
            restartEnabledService(shell)
            repeat(8) {
                delay(500L)
                status = readStatus(shell)
                if (
                    status.config.enabled &&
                    status.serviceRunning &&
                    status.serviceVersion == FOREGROUND_TOOL_SERVICE_VERSION
                ) {
                    return@withRootShell status
                }
            }
            throw ForegroundToolException(ForegroundToolFailure.ServiceStartFailed)
        }
        status
    }

    suspend fun saveConfig(
        targets: Set<String>,
        tools: Set<String>,
    ): ForegroundToolStatus = withRootShell { shell ->
        val normalizedTargets = validatePackages(targets)
        val normalizedTools = validatePackages(tools)
        ensureNoConflict(normalizedTargets, normalizedTools)
        installConfig(shell, normalizedTargets, normalizedTools)
        if (normalizedTargets.isEmpty() || normalizedTools.isEmpty()) {
            stopService(shell)
        } else {
            startServiceIfEnabled(shell)
        }
        readStatus(shell)
    }

    suspend fun setEnabled(
        enabled: Boolean,
        targets: Set<String>,
        tools: Set<String>,
    ): ForegroundToolStatus {
        val normalizedTargets = validatePackages(targets)
        val normalizedTools = validatePackages(tools)
        ensureNoConflict(normalizedTargets, normalizedTools)
        if (enabled) {
            validateForegroundToolEnable(normalizedTargets, normalizedTools)?.let { failure ->
                throw ForegroundToolException(failure)
            }
        }

        return withRootShell { shell ->
            installConfig(shell, normalizedTargets, normalizedTools)
            if (enabled) {
                runRootCommand(
                    shell,
                    "touch ${shellQuote(FOREGROUND_TOOL_ENABLED)} && " +
                        "chmod 0600 ${shellQuote(FOREGROUND_TOOL_ENABLED)}",
                )
                startService(shell)
            } else {
                stopService(shell)
            }

            var status = readStatus(shell)
            if (enabled) {
                repeat(6) {
                    if (
                        status.serviceRunning &&
                        status.config.enabled &&
                        status.serviceVersion == FOREGROUND_TOOL_SERVICE_VERSION
                    ) {
                        return@withRootShell status
                    }
                    delay(500L)
                    status = readStatus(shell)
                }
                stopService(shell)
                throw ForegroundToolException(ForegroundToolFailure.ServiceStartFailed)
            }
            status
        }
    }

    suspend fun clearLog(): ForegroundToolStatus = withRootShell { shell ->
        runRootCommand(
            shell,
            ": > ${shellQuote(FOREGROUND_TOOL_LOG)}; chmod 0600 ${shellQuote(FOREGROUND_TOOL_LOG)} 2>/dev/null || true",
        )
        readStatus(shell)
    }

    private suspend fun installConfig(
        shell: Shell,
        targets: Set<String>,
        tools: Set<String>,
    ) {
        val serviceText = appContext.assets.open(FOREGROUND_TOOL_SERVICE_ASSET)
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        val output = executeScript(
            shell,
            buildForegroundToolInstallScript(
                targetsText = configText(targets),
                toolsText = configText(tools),
                serviceText = serviceText,
            ),
        )
        if (output.success && FOREGROUND_TOOL_INSTALL_SUCCESS_MARKER in output.stdout) return

        val failure = when {
            output.error.contains("${FOREGROUND_TOOL_INSTALL_FAILURE_MARKER}prepare") ->
                ForegroundToolFailure.InstallPreparationFailed
            output.error.contains("${FOREGROUND_TOOL_INSTALL_FAILURE_MARKER}config") ->
                ForegroundToolFailure.ConfigStagingFailed
            output.error.contains("${FOREGROUND_TOOL_INSTALL_FAILURE_MARKER}service") ->
                ForegroundToolFailure.ServiceInstallFailed
            output.success -> ForegroundToolFailure.InstallVerificationFailed
            else -> ForegroundToolFailure.CommandFailed
        }
        throw ForegroundToolException(failure, output.error.ifBlank { "exit ${output.code}" })
    }

    private suspend fun readStatus(shell: Shell): ForegroundToolStatus {
        val command = """
            if [ -f ${shellQuote(FOREGROUND_TOOL_ENABLED)} ]; then echo '__ENABLED__=1'; else echo '__ENABLED__=0'; fi
            if [ -f ${shellQuote(FOREGROUND_TOOL_SERVICE)} ]; then echo '__INSTALLED__=1'; else echo '__INSTALLED__=0'; fi
            if [ -f ${shellQuote(FOREGROUND_TOOL_TARGETS)} ] && [ -f ${shellQuote(FOREGROUND_TOOL_TOOLS)} ]; then echo '__CONFIGURED__=1'; else echo '__CONFIGURED__=0'; fi
            running=0
            pid=${'$'}(cat ${shellQuote(FOREGROUND_TOOL_PID)} 2>/dev/null)
            case "${'$'}pid" in
                ''|*[!0-9]*) ;;
                *)
                    if [ -r "/proc/${'$'}pid/cmdline" ] && tr '\000' ' ' < "/proc/${'$'}pid/cmdline" 2>/dev/null | grep -Fq ${shellQuote(FOREGROUND_TOOL_SERVICE)}; then
                        running=1
                    elif [ -r "/proc/${'$'}pid/environ" ] && tr '\000' '\n' < "/proc/${'$'}pid/environ" 2>/dev/null | grep -Fxq ${shellQuote(FOREGROUND_TOOL_DAEMON_MARKER)}; then
                        running=1
                    fi
                    ;;
            esac
            echo "__RUNNING__=${'$'}running"
            if [ -f ${shellQuote(FOREGROUND_TOOL_TARGETS)} ]; then sed 's/^/__TARGET__=/' ${shellQuote(FOREGROUND_TOOL_TARGETS)}; fi
            if [ -f ${shellQuote(FOREGROUND_TOOL_TOOLS)} ]; then sed 's/^/__TOOL__=/' ${shellQuote(FOREGROUND_TOOL_TOOLS)}; fi
            if [ -f ${shellQuote(FOREGROUND_TOOL_STATUS)} ]; then sed 's/^/__STATUS__=/' ${shellQuote(FOREGROUND_TOOL_STATUS)}; fi
            if [ -f ${shellQuote(FOREGROUND_TOOL_LOG)} ]; then tail -n 30 ${shellQuote(FOREGROUND_TOOL_LOG)} | sed 's/^/__LOG__=/' ; fi
        """.trimIndent()
        val output = execute(shell, command)
        if (!output.success) {
            throw ForegroundToolException(
                ForegroundToolFailure.CommandFailed,
                output.error.ifBlank { "exit ${output.code}" },
            )
        }

        val enabled = output.stdout.any { it == "__ENABLED__=1" }
        val installed = output.stdout.any { it == "__INSTALLED__=1" }
        val configured = output.stdout.any { it == "__CONFIGURED__=1" }
        val running = output.stdout.any { it == "__RUNNING__=1" }
        val targets = normalizeForegroundToolPackages(
            output.stdout.filter { it.startsWith("__TARGET__=") }.map { it.substringAfter('=') },
        )
        val tools = normalizeForegroundToolPackages(
            output.stdout.filter { it.startsWith("__TOOL__=") }.map { it.substringAfter('=') },
        )
        val properties = parseForegroundToolProperties(
            output.stdout.filter { it.startsWith("__STATUS__=") }.map { it.substringAfter('=') },
        )
        val runtime = when {
            !enabled -> ForegroundToolRuntimeState.Disabled
            targets.isEmpty() || tools.isEmpty() -> ForegroundToolRuntimeState.Error
            !running -> ForegroundToolRuntimeState.Error
            properties["state"] == "active" -> ForegroundToolRuntimeState.Active
            properties["state"] == "error" -> ForegroundToolRuntimeState.Error
            else -> ForegroundToolRuntimeState.Waiting
        }
        val error = when {
            enabled && targets.isEmpty() -> ForegroundToolFailure.TargetRequired
            enabled && tools.isEmpty() -> ForegroundToolFailure.ToolRequired
            enabled && !running -> ForegroundToolFailure.ServiceStartFailed
            runtime == ForegroundToolRuntimeState.Error -> ForegroundToolFailure.CommandFailed
            else -> null
        }
        return ForegroundToolStatus(
            config = ForegroundToolConfig(enabled = enabled, targets = targets, tools = tools),
            configPresent = configured,
            runtimeState = runtime,
            foregroundPackage = properties["foreground"].orEmpty(),
            matchedTarget = properties["matched"].orEmpty(),
            stoppedCount = properties["stopped"]?.toIntOrNull()?.coerceAtLeast(0) ?: 0,
            failedCount = properties["failed"]?.toIntOrNull()?.coerceAtLeast(0) ?: 0,
            lastEvent = properties["event"].orEmpty(),
            updatedAt = properties["updated"].orEmpty(),
            serviceInstalled = installed,
            serviceRunning = running,
            serviceVersion = properties["version"]?.toIntOrNull()?.coerceAtLeast(0) ?: 0,
            recentLog = output.stdout.filter { it.startsWith("__LOG__=") }.map { it.substringAfter('=') },
            error = error,
        )
    }

    private suspend fun startServiceIfEnabled(shell: Shell) {
        val command = "if [ -f ${shellQuote(FOREGROUND_TOOL_ENABLED)} ]; then " + startCommand() + "; fi"
        runRootCommand(shell, command)
    }

    private suspend fun startService(shell: Shell) {
        runRootCommand(shell, startCommand())
    }

    private suspend fun restartEnabledService(shell: Shell) {
        stopService(shell)
        delay(FOREGROUND_TOOL_RESTART_GRACE_MILLIS)
        runRootCommand(
            shell,
            "touch ${shellQuote(FOREGROUND_TOOL_ENABLED)} && " +
                "chmod 0600 ${shellQuote(FOREGROUND_TOOL_ENABLED)}",
        )
        startService(shell)
    }

    private fun startCommand(): String =
        "sh ${shellQuote(FOREGROUND_TOOL_SERVICE)}"

    private suspend fun stopService(shell: Shell) {
        val command = """
            rm -f ${shellQuote(FOREGROUND_TOOL_ENABLED)}
            pid=${'$'}(cat ${shellQuote(FOREGROUND_TOOL_PID)} 2>/dev/null)
            case "${'$'}pid" in
                ''|*[!0-9]*) ;;
                *)
                    is_service=0
                    if [ -r "/proc/${'$'}pid/cmdline" ] && tr '\000' ' ' < "/proc/${'$'}pid/cmdline" 2>/dev/null | grep -Fq ${shellQuote(FOREGROUND_TOOL_SERVICE)}; then
                        is_service=1
                    elif [ -r "/proc/${'$'}pid/environ" ] && tr '\000' '\n' < "/proc/${'$'}pid/environ" 2>/dev/null | grep -Fxq ${shellQuote(FOREGROUND_TOOL_DAEMON_MARKER)}; then
                        is_service=1
                    fi
                    if [ "${'$'}is_service" -eq 1 ]; then
                        kill -TERM "${'$'}pid" 2>/dev/null || true
                    fi
                    ;;
            esac
        """.trimIndent()
        runRootCommand(shell, command)
    }

    private fun validatePackages(packages: Set<String>): Set<String> {
        val normalized = normalizeForegroundToolPackages(packages)
        if (normalized.size != packages.map(String::trim).filter(String::isNotEmpty).distinct().size) {
            throw ForegroundToolException(ForegroundToolFailure.InvalidPackage)
        }
        return normalized
    }

    private fun ensureNoConflict(targets: Set<String>, tools: Set<String>) {
        if (targets.any { it in tools }) {
            throw ForegroundToolException(ForegroundToolFailure.SelectionConflict)
        }
    }

    private fun configText(values: Set<String>): String =
        values.sorted().joinToString(separator = "\n", postfix = if (values.isEmpty()) "" else "\n")

    private suspend fun runRootCommand(shell: Shell, command: String) {
        val output = execute(shell, command)
        if (!output.success) {
            throw ForegroundToolException(
                ForegroundToolFailure.CommandFailed,
                output.error.ifBlank { "exit ${output.code}" },
            )
        }
    }

    private suspend fun execute(shell: Shell, command: String): RootCommandOutput {
        val stdout = ArrayList<String>()
        val stderr = ArrayList<String>()
        val result = withTimeoutOrNull(FOREGROUND_TOOL_COMMAND_TIMEOUT_MILLIS) {
            shell.newJob().add(command).to(stdout, stderr).exec()
        } ?: throw ForegroundToolException(ForegroundToolFailure.CommandTimeout)
        return RootCommandOutput(
            success = result.isSuccess,
            code = result.code,
            stdout = stdout,
            error = stderr.joinToString("\n"),
        )
    }

    private suspend fun executeScript(shell: Shell, script: String): RootCommandOutput {
        val stdout = ArrayList<String>()
        val stderr = ArrayList<String>()
        val result = withTimeoutOrNull(FOREGROUND_TOOL_COMMAND_TIMEOUT_MILLIS) {
            shell.newJob()
                .add(ByteArrayInputStream(script.toByteArray(Charsets.UTF_8)))
                .to(stdout, stderr)
                .exec()
        } ?: throw ForegroundToolException(ForegroundToolFailure.CommandTimeout)
        return RootCommandOutput(
            success = result.isSuccess,
            code = result.code,
            stdout = stdout,
            error = stderr.joinToString("\n"),
        )
    }

    private suspend fun <T> withRootShell(block: suspend (Shell) -> T): T = withContext(Dispatchers.IO) {
        val shell = createRootShell(globalMnt = true)
        try {
            if (!runCatching { shell.isRoot }.getOrDefault(false)) {
                throw ForegroundToolException(ForegroundToolFailure.RootUnavailable)
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

internal fun buildForegroundToolInstallScript(
    targetsText: String,
    toolsText: String,
    serviceText: String,
): String {
    val targetsTemp = "$FOREGROUND_TOOL_TARGETS.tmp"
    val toolsTemp = "$FOREGROUND_TOOL_TOOLS.tmp"
    val serviceTemp = "$FOREGROUND_TOOL_SERVICE.tmp"
    val targetsPayload = normalizeForegroundToolPayload(targetsText)
    val toolsPayload = normalizeForegroundToolPayload(toolsText)
    val servicePayload = normalizeForegroundToolPayload(serviceText)

    return buildString {
        appendLine("umask 077")
        appendLine("install_fail() {")
        appendLine(
            "    rm -f ${shellQuote(targetsTemp)} ${shellQuote(toolsTemp)} ${shellQuote(serviceTemp)}",
        )
        appendLine("    printf '$FOREGROUND_TOOL_INSTALL_FAILURE_MARKER%s\\n' \"${'$'}1\" >&2")
        appendLine("    exit 1")
        appendLine("}")
        appendLine(
            "mkdir -p ${shellQuote(FOREGROUND_TOOL_BASE)} /data/adb/service.d || install_fail prepare",
        )
        appendLine("chmod 0700 ${shellQuote(FOREGROUND_TOOL_BASE)} || install_fail prepare")
        appendLine(
            "rm -f ${shellQuote(targetsTemp)} ${shellQuote(toolsTemp)} ${shellQuote(serviceTemp)} || " +
                "install_fail prepare",
        )
        appendForegroundToolFile(targetsTemp, targetsPayload, "TARGETS", "config")
        appendForegroundToolSizeCheck(targetsTemp, targetsPayload, "config")
        appendForegroundToolFile(toolsTemp, toolsPayload, "TOOLS", "config")
        appendForegroundToolSizeCheck(toolsTemp, toolsPayload, "config")
        appendForegroundToolFile(serviceTemp, servicePayload, "SERVICE", "service")
        appendForegroundToolSizeCheck(serviceTemp, servicePayload, "service")
        appendLine(
            "chmod 0600 ${shellQuote(targetsTemp)} ${shellQuote(toolsTemp)} || install_fail config",
        )
        appendLine("chmod 0700 ${shellQuote(serviceTemp)} || install_fail service")
        appendLine("mv -f ${shellQuote(targetsTemp)} ${shellQuote(FOREGROUND_TOOL_TARGETS)} || install_fail config")
        appendLine("mv -f ${shellQuote(toolsTemp)} ${shellQuote(FOREGROUND_TOOL_TOOLS)} || install_fail config")
        appendLine("mv -f ${shellQuote(serviceTemp)} ${shellQuote(FOREGROUND_TOOL_SERVICE)} || install_fail service")
        appendLine("if command -v restorecon >/dev/null 2>&1; then")
        appendLine(
            "    restorecon ${shellQuote(FOREGROUND_TOOL_BASE)} ${shellQuote(FOREGROUND_TOOL_SERVICE)} " +
                ">/dev/null 2>&1 || true",
        )
        appendLine("fi")
        appendLine("printf '$FOREGROUND_TOOL_INSTALL_SUCCESS_MARKER\\n'")
    }
}

private fun StringBuilder.appendForegroundToolFile(
    target: String,
    payload: String,
    label: String,
    failureStage: String,
) {
    if (payload.isEmpty()) {
        appendLine(": > ${shellQuote(target)} || install_fail $failureStage")
        return
    }
    val delimiter = foregroundToolHeredocDelimiter(label, payload)
    appendLine("cat > ${shellQuote(target)} <<'$delimiter' || install_fail $failureStage")
    append(payload)
    appendLine(delimiter)
}

private fun StringBuilder.appendForegroundToolSizeCheck(
    target: String,
    payload: String,
    failureStage: String,
) {
    val expectedBytes = payload.toByteArray(Charsets.UTF_8).size
    appendLine(
        "actual_bytes=${'$'}(wc -c < ${shellQuote(target)} 2>/dev/null) && " +
            "[ \"${'$'}actual_bytes\" -eq $expectedBytes ] 2>/dev/null || install_fail $failureStage",
    )
}

private fun normalizeForegroundToolPayload(value: String): String = when {
    value.isEmpty() -> ""
    value.endsWith('\n') -> value
    else -> "$value\n"
}

private fun foregroundToolHeredocDelimiter(label: String, payload: String): String {
    var delimiter = "__APKESU_FOREGROUND_TOOL_${label}_EOF__"
    val lines = payload.lineSequence().toSet()
    while (delimiter in lines) delimiter += "_"
    return delimiter
}

private fun shellQuote(value: String): String = "'${value.replace("'", "'\"'\"'")}'"
