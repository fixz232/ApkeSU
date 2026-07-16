package me.weishu.kernelsu.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GraphicsRendererModelsTest {
    @Test
    fun unconfiguredPropertiesAreSystemManaged() {
        val status = parseGraphicsRendererStatus(
            listOf(
                "renderer=",
                "disable_vulkan=",
                "configured_mode=",
                "persistent=0",
            )
        )

        assertNull(status.configuredMode)
        assertEquals(GraphicsRendererMode.SystemDefault, status.currentMode)
        assertTrue(status.applied)
        assertFalse(status.persistent)
    }

    @Test
    fun vulkanConfigurationRequiresMatchingRuntimeProperties() {
        val active = parseGraphicsRendererStatus(
            listOf(
                "renderer=skiavk",
                "disable_vulkan=false",
                "configured_mode=vulkan",
                "vulkan_feature=feature:android.hardware.vulkan.level=1",
                "persistent=1",
                "restart_required=1",
            )
        )
        val mismatched = parseGraphicsRendererStatus(
            listOf(
                "renderer=skiagl",
                "disable_vulkan=true",
                "configured_mode=vulkan",
            )
        )

        assertEquals(GraphicsRendererMode.Vulkan, active.currentMode)
        assertEquals(GraphicsRendererMode.Vulkan, active.configuredMode)
        assertTrue(active.vulkanSupported)
        assertTrue(active.applied)
        assertTrue(active.restartRequired)
        assertFalse(mismatched.applied)
        assertFalse(mismatched.restartRequired)
    }

    @Test
    fun openGlConfigurationRecognizesDisableVulkanProperty() {
        val status = parseGraphicsRendererStatus(
            listOf(
                "renderer=skiagl",
                "disable_vulkan=true",
                "configured_mode=opengl",
                "persistent=0",
            )
        )

        assertEquals(GraphicsRendererMode.OpenGl, status.currentMode)
        assertEquals(GraphicsRendererMode.OpenGl, status.configuredMode)
        assertTrue(status.applied)
    }

    @Test
    fun driverPathAlsoEstablishesVulkanSupport() {
        val status = parseGraphicsRendererStatus(
            listOf("vulkan_driver=/vendor/lib64/hw/vulkan.adreno.so")
        )

        assertTrue(status.vulkanSupported)
    }

    @Test
    fun unknownRendererIsReportedAsCustom() {
        assertEquals(
            GraphicsRendererMode.Custom,
            inferGraphicsRendererMode("unexpected_backend", "false"),
        )
    }

    @Test
    fun parserPreservesOriginalBackupValues() {
        val status = parseGraphicsRendererStatus(
            listOf(
                "original_renderer=skiavk",
                "original_disable_vulkan=false",
                "backup_available=1",
                "broken line",
            )
        )

        assertTrue(status.backupAvailable)
        assertEquals("skiavk", status.originalRendererProperty)
        assertEquals("false", status.originalDisableVulkanProperty)
    }
}
