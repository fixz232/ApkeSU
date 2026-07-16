package me.weishu.kernelsu.ui.screen.settings

import me.weishu.kernelsu.BuildConfig
import me.weishu.kernelsu.data.model.Module
import me.weishu.kernelsu.ui.util.RootDiagnosticInfo
import java.util.Locale

internal fun buildModuleAnalysisReport(
    modules: List<Module>,
    root: RootDiagnosticInfo,
): String {
    val findings = buildList {
        if (!root.kernelModuleLoaded && root.driverVersion <= 0) {
            add("CRITICAL: ApkeSU kernel driver is not connected; module state may be incomplete.")
        }
        if (!root.managerRegistered) {
            add("HIGH: This Manager is not registered with the kernel driver.")
        }
        if (root.kernelUapi > 0 && root.managerUapi > 0 && root.kernelUapi != root.managerUapi) {
            add("HIGH: Kernel UAPI ${root.kernelUapi} differs from Manager UAPI ${root.managerUapi}.")
        }
        if (
            root.packagedKsudVersion.isNotBlank() &&
            root.installedKsudVersion.isNotBlank() &&
            root.installedKsudVersion != "missing" &&
            root.packagedKsudVersion != root.installedKsudVersion
        ) {
            add("HIGH: Installed ksud (${root.installedKsudVersion}) differs from packaged ksud (${root.packagedKsudVersion}).")
        }
        val activeMetamodules = modules.filter { it.metamodule && it.enabled && !it.remove }
        if (activeMetamodules.size > 1) {
            add("CRITICAL: More than one active metamodule is reported: ${activeMetamodules.joinToString { it.id }}.")
        }
        modules.filter { it.update && it.remove }.forEach {
            add("HIGH: ${it.id} is marked for both update and removal.")
        }
        modules.filter { it.metamodule && !it.enabled }.forEach {
            add("MEDIUM: Metamodule ${it.id} is disabled.")
        }
        modules.filter { it.updateJson.startsWith("http://") }.forEach {
            add("MEDIUM: ${it.id} uses an unencrypted update URL.")
        }
        modules.groupBy { normalizeModuleName(it.name) }
            .filter { (name, items) -> name.isNotBlank() && items.size > 1 }
            .values
            .forEach { duplicates ->
                add("MEDIUM: Modules share the same normalized name: ${duplicates.joinToString { it.id }}.")
            }
        if (modules.any { it.remove }) {
            add("INFO: ${modules.count { it.remove }} module(s) are pending removal on reboot.")
        }
        if (modules.any { it.update }) {
            add("INFO: ${modules.count { it.update }} module(s) have staged updates.")
        }
        if (modules.any { !it.enabled }) {
            add("INFO: ${modules.count { !it.enabled }} module(s) are disabled.")
        }
    }

    return buildString {
        appendLine("Analyze this local ApkeSU diagnostic report in the user's language.")
        appendLine("Separate confirmed facts from hypotheses. Do not claim a conflict unless the report proves it.")
        appendLine("Give reversible troubleshooting steps first and state which additional log would verify each hypothesis.")
        appendLine("Never suggest destructive flashing or deleting /data/adb without an explicit backup and recovery path.")
        appendLine()
        appendLine("## Runtime")
        appendLine("- manager version code: ${BuildConfig.VERSION_CODE}")
        appendLine("- driver version: ${root.driverVersion}")
        appendLine("- work mode: ${root.workMode}")
        appendLine("- kernel module loaded: ${root.kernelModuleLoaded}")
        appendLine("- Root shell via ksud: ${root.ksuRootShell}")
        appendLine("- fallback Root shell: ${root.fallbackRootShell}")
        appendLine("- manager registered: ${root.managerRegistered}")
        appendLine("- kernel/manager UAPI: ${root.kernelUapi}/${root.managerUapi}")
        appendLine("- packaged/installed ksud: ${root.packagedKsudVersion.ifBlank { "unknown" }}/${root.installedKsudVersion.ifBlank { "unknown" }}")
        appendLine("- KMI: ${root.currentKmi.ifBlank { "unknown" }}")
        appendLine("- slot: ${root.currentSlot.ifBlank { "unknown" }}")
        appendLine("- hidden-path LKM: ${root.hiddenPathLkm}")
        appendLine()
        appendLine("## Deterministic findings")
        if (findings.isEmpty()) {
            appendLine("- No deterministic conflict was found from the available metadata.")
        } else {
            findings.forEach { appendLine("- $it") }
        }
        appendLine()
        appendLine("## Module inventory (${modules.size})")
        modules.sortedBy { it.name.lowercase(Locale.ROOT) }.forEach { module ->
            append("- ").append(module.name).append(" [").append(module.id).append("]")
            append(" version=").append(module.version).append("(").append(module.versionCode).append(")")
            append(" enabled=").append(module.enabled)
            append(" staged_update=").append(module.update)
            append(" staged_remove=").append(module.remove)
            append(" metamodule=").append(module.metamodule)
            append(" webui=").append(module.hasWebUi)
            append(" action=").append(module.hasActionScript)
            append(" author=").append(module.author.take(MODULE_FIELD_LIMIT))
            if (module.updateJson.isNotBlank()) {
                append(" update_host=").append(runCatching {
                    java.net.URI(module.updateJson).host
                }.getOrNull().orEmpty().ifBlank { "invalid" })
            }
            if (module.description.isNotBlank()) {
                append(" description=").append(module.description.replace('\n', ' ').take(MODULE_DESCRIPTION_LIMIT))
            }
            appendLine()
        }
        appendLine()
        appendLine("No module scripts, private app data, full logs, API keys, or file contents are included in this report.")
    }
}

private fun normalizeModuleName(name: String): String =
    name.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+"), "")

private const val MODULE_FIELD_LIMIT = 80
private const val MODULE_DESCRIPTION_LIMIT = 240
