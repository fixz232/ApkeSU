package me.weishu.kernelsu.ui.screen.home

import me.weishu.kernelsu.ui.InterfaceStyle
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MiuixHomeLayoutTest {
    @Test
    fun currentLayoutRemainsTheDefault() {
        assertFalse(
            shouldUseClassicMiuixHomeLayout(
                interfaceStyle = InterfaceStyle.Miuix.value,
                requested = false,
                customHomeLayoutEnabled = false,
            )
        )
    }

    @Test
    fun classicLayoutOnlyAppliesToMiuix() {
        assertTrue(
            shouldUseClassicMiuixHomeLayout(
                interfaceStyle = InterfaceStyle.Miuix.value,
                requested = true,
                customHomeLayoutEnabled = false,
            )
        )
        assertFalse(
            shouldUseClassicMiuixHomeLayout(
                interfaceStyle = InterfaceStyle.Pixel.value,
                requested = true,
                customHomeLayoutEnabled = false,
            )
        )
    }

    @Test
    fun customLayoutKeepsPriority() {
        assertFalse(
            shouldUseClassicMiuixHomeLayout(
                interfaceStyle = InterfaceStyle.Miuix.value,
                requested = true,
                customHomeLayoutEnabled = true,
            )
        )
    }
}
