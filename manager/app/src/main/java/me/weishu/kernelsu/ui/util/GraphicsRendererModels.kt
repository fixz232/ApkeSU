package me.weishu.kernelsu.ui.util

import androidx.compose.runtime.Immutable

enum class GraphicsRendererMode(val value: String) {
    SystemDefault("system"),
    Vulkan("vulkan"),
    OpenGl("opengl"),
    Custom("custom");

    companion object {
        fun fromValue(value: String): GraphicsRendererMode? = entries.firstOrNull {
            it != Custom && it.value == value.trim().lowercase()
        }
    }
}

@Immutable
data class GraphicsRendererStatus(
    val rootAvailable: Boolean = false,
    val rendererProperty: String = "",
    val disableVulkanProperty: String = "",
    val eglDriver: String = "",
    val hardwareVulkanProperty: String = "",
    val vulkanFeature: String = "",
    val vulkanDriverPath: String = "",
    val originalRendererProperty: String = "",
    val originalDisableVulkanProperty: String = "",
    val backupAvailable: Boolean = false,
    val configuredMode: GraphicsRendererMode? = null,
    val currentMode: GraphicsRendererMode = GraphicsRendererMode.SystemDefault,
    val persistent: Boolean = false,
    val applied: Boolean = false,
    val restartRequired: Boolean = false,
    val error: String = "",
) {
    val vulkanSupported: Boolean
        get() = vulkanFeature.isNotBlank() ||
            vulkanDriverPath.isNotBlank() ||
            hardwareVulkanProperty.isNotBlank()

    val configured: Boolean
        get() = configuredMode != null
}

data class GraphicsRendererCommandResult(
    val success: Boolean,
    val status: GraphicsRendererStatus = GraphicsRendererStatus(),
    val error: String = "",
)

internal fun parseGraphicsRendererStatus(
    lines: List<String>,
    rootAvailable: Boolean = true,
): GraphicsRendererStatus {
    val values = lines.mapNotNull { rawLine ->
        val separator = rawLine.indexOf('=')
        if (separator <= 0) return@mapNotNull null
        rawLine.substring(0, separator).trim() to rawLine.substring(separator + 1).trim()
    }.toMap()
    val renderer = values["renderer"].orEmpty()
    val disableVulkan = values["disable_vulkan"].orEmpty()
    val configuredMode = GraphicsRendererMode.fromValue(values["configured_mode"].orEmpty())
    val currentMode = inferGraphicsRendererMode(renderer, disableVulkan)
    val applied = when (configuredMode) {
        GraphicsRendererMode.Vulkan -> renderer == "skiavk" && !disableVulkan.equals("true", true)
        GraphicsRendererMode.OpenGl -> renderer == "skiagl" && disableVulkan.equals("true", true)
        GraphicsRendererMode.SystemDefault,
        GraphicsRendererMode.Custom,
        null,
        -> configuredMode == null
    }
    return GraphicsRendererStatus(
        rootAvailable = rootAvailable,
        rendererProperty = renderer,
        disableVulkanProperty = disableVulkan,
        eglDriver = values["egl_driver"].orEmpty(),
        hardwareVulkanProperty = values["hardware_vulkan"].orEmpty(),
        vulkanFeature = values["vulkan_feature"].orEmpty(),
        vulkanDriverPath = values["vulkan_driver"].orEmpty(),
        originalRendererProperty = values["original_renderer"].orEmpty(),
        originalDisableVulkanProperty = values["original_disable_vulkan"].orEmpty(),
        backupAvailable = values["backup_available"] == "1",
        configuredMode = configuredMode,
        currentMode = currentMode,
        persistent = values["persistent"] == "1",
        applied = applied,
        restartRequired = values["restart_required"] == "1" && applied,
        error = values["error"].orEmpty(),
    )
}

internal fun inferGraphicsRendererMode(
    rendererProperty: String,
    disableVulkanProperty: String,
): GraphicsRendererMode = when {
    rendererProperty == "skiavk" && !disableVulkanProperty.equals("true", true) -> {
        GraphicsRendererMode.Vulkan
    }
    rendererProperty == "skiagl" || disableVulkanProperty.equals("true", true) -> {
        GraphicsRendererMode.OpenGl
    }
    rendererProperty.isBlank() && disableVulkanProperty.isBlank() -> {
        GraphicsRendererMode.SystemDefault
    }
    else -> GraphicsRendererMode.Custom
}

internal fun GraphicsRendererStatus.matchesRuntimeMode(mode: GraphicsRendererMode): Boolean = when (mode) {
    GraphicsRendererMode.Vulkan -> rendererProperty == "skiavk" &&
        !disableVulkanProperty.equals("true", true)
    GraphicsRendererMode.OpenGl -> rendererProperty == "skiagl" &&
        disableVulkanProperty.equals("true", true)
    GraphicsRendererMode.SystemDefault -> !configured
    GraphicsRendererMode.Custom -> false
}
