package me.weishu.kernelsu.ui.component

import me.weishu.kernelsu.ui.InterfaceStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class PageTransitionEffectTest {
    @Test
    fun storedValuesMapToStableEffects() {
        PageTransitionEffect.entries.forEachIndexed { index, effect ->
            assertEquals(effect, PageTransitionEffect.fromValue(effect.value))
            assertEquals(effect, PageTransitionEffect.fromIndex(index))
            assertEquals(index, PageTransitionEffect.selectedIndex(effect.value))
        }
        assertEquals(PageTransitionEffect.Default, PageTransitionEffect.fromValue("unknown"))
        assertEquals(PageTransitionEffect.Default, PageTransitionEffect.fromValue(null))
        assertEquals(PageTransitionEffect.Default, PageTransitionEffect.fromIndex(-1))
    }

    @Test
    fun linkedModeUsesInterfaceSpecificVisuals() {
        assertEquals(
            PageTransitionVisual.Pixel,
            resolvePageTransitionVisual(PageTransitionEffect.StyleLinked, InterfaceStyle.Pixel.value),
        )
        assertEquals(
            PageTransitionVisual.Season,
            resolvePageTransitionVisual(PageTransitionEffect.StyleLinked, InterfaceStyle.Snow.value),
        )
        assertEquals(
            PageTransitionVisual.Rain,
            resolvePageTransitionVisual(PageTransitionEffect.StyleLinked, InterfaceStyle.Rain.value),
        )
        assertEquals(
            PageTransitionVisual.Ink,
            resolvePageTransitionVisual(PageTransitionEffect.StyleLinked, InterfaceStyle.Ink.value),
        )
        assertEquals(
            PageTransitionVisual.Glass,
            resolvePageTransitionVisual(PageTransitionEffect.StyleLinked, InterfaceStyle.LiquidGlass.value),
        )
        assertEquals(
            PageTransitionVisual.Depth,
            resolvePageTransitionVisual(PageTransitionEffect.StyleLinked, InterfaceStyle.Miuix.value),
        )
    }

    @Test
    fun disabledAnimationsAlwaysReturnIdentityTransform() {
        PageTransitionVisual.entries.forEach { visual ->
            assertEquals(
                PageTransitionTransform(),
                resolvePageTransitionTransform(visual, pageOffset = 0.72f, animationsEnabled = false),
            )
        }
    }

    @Test
    fun transformsStayWithinSubtleBounds() {
        PageTransitionVisual.entries.forEach { visual ->
            listOf(-2f, -0.5f, 0f, 0.5f, 2f).forEach { offset ->
                val transform = resolvePageTransitionTransform(visual, offset, animationsEnabled = true)
                assertTrue(transform.alpha in 0.8f..1f)
                assertTrue(transform.scaleX in 0.94f..1.03f)
                assertTrue(transform.scaleY in 0.94f..1f)
                assertTrue(abs(transform.translationXFraction) <= 0.11f)
                assertTrue(abs(transform.rotationY) <= 3f)
            }
        }
    }

    @Test
    fun materialPagerKeepsFullOpaquePageBoundsDuringTransition() {
        PageTransitionVisual.entries.forEach { visual ->
            listOf(-0.75f, -0.25f, 0.25f, 0.75f).forEach { offset ->
                assertEquals(
                    PageTransitionTransform(),
                    resolveMainPageTransitionTransform(
                        visual = visual,
                        interfaceStyle = InterfaceStyle.Material.value,
                        pageOffset = offset,
                        animationsEnabled = true,
                    ),
                )
            }
        }
    }

    @Test
    fun nonMaterialPagerRetainsSelectedTransition() {
        val expected = resolvePageTransitionTransform(
            visual = PageTransitionVisual.Depth,
            pageOffset = 0.5f,
            animationsEnabled = true,
        )
        assertEquals(
            expected,
            resolveMainPageTransitionTransform(
                visual = PageTransitionVisual.Depth,
                interfaceStyle = InterfaceStyle.Miuix.value,
                pageOffset = 0.5f,
                animationsEnabled = true,
            ),
        )
    }
}
