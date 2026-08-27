package me.weishu.kernelsu.ui.webui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebUIStateTest {
    @Test
    fun kpatchSectionsUseOnlyKnownWebUiEntryPoints() {
        assertTrue(kpatchNextSectionScript(KPatchNextSection.Kpm).contains("getElementById(sectionId)"))
        assertTrue(kpatchNextSectionScript(KPatchNextSection.Kpm).contains("sectionId = 'KPM'"))
        assertTrue(kpatchNextSectionScript(KPatchNextSection.Kpm).contains("pageId = 'kpm-page'"))
        assertTrue(kpatchNextSectionScript(KPatchNextSection.Exclude).contains("sectionId = 'exclude'"))
        assertTrue(kpatchNextSectionScript(KPatchNextSection.Exclude).contains("pageId = 'exclude-page'"))
        assertTrue(kpatchNextSectionScript(KPatchNextSection.Exclude).contains("activateDirectly"))
        assertTrue(kpatchNextSectionScript(KPatchNextSection.Exclude).contains("transition:none!important"))
        assertTrue(kpatchNextSectionScript(KPatchNextSection.Exclude).contains("window.__apkesuKpatchRouteToken"))
        assertTrue(kpatchNextSectionScript(KPatchNextSection.Exclude).contains("if (activateDirectly())"))
        assertTrue(kpatchNextSectionScript(KPatchNextSection.Exclude).contains("let clickFallbackUsed = false"))
        assertFalse(kpatchNextSectionScript(KPatchNextSection.Kpm).contains("javascript:"))
    }

    @Test
    fun sectionRouteScriptKeepsOnlyTheRequestedPageVisible() {
        val script = kpatchNextSectionScript(KPatchNextSection.Exclude)

        assertTrue(script.contains("page.classList.toggle('active', page.id === pageId)"))
        assertTrue(script.contains(".page:not(.active){visibility:hidden!important}"))
        assertTrue(script.indexOf("if (activateDirectly())") < script.indexOf("button.click()"))
    }

    @Test
    fun wallpaperScriptKeepsControlsAboveTheWallpaperLayer() {
        val script = kpmWallpaperApplyScript("[]", dark = true)

        assertTrue(script.contains("pointer-events:none!important"))
        assertTrue(script.contains("z-index:1!important"))
        assertTrue(script.contains("MutationObserver"))
        assertTrue(script.contains("carouselEnabled"))
    }

    @Test
    fun embeddedKpmThemeKeepsTheWebUiSurfaceTransparent() {
        val script = embeddedKpmThemeScript("pixel", dark = true)

        assertTrue(script.contains("html,body,#app{background:transparent!important}"))
        assertTrue(script.contains("[class*=\"top-bar\"],[class*=\"bottom-bar\"]{background:transparent!important}"))
    }

    @Test
    fun skrootproEmbeddedKpmThemeUsesItsOwnPageAndControlPalette() {
        val light = embeddedKpmThemeScript("skrootpro", dark = false)
        val dark = embeddedKpmThemeScript("skrootpro", dark = true)

        assertTrue(light.contains("#7000F5"))
        assertTrue(light.contains("#F5F6F8"))
        assertTrue(light.contains("--md-sys-color-surface"))
        assertTrue(light.contains("#exclude-page,#exclude-page .page-content,#exclude-page .app-list"))
        assertTrue(light.contains("#exclude-page .app-item"))
        assertTrue(light.contains("--md-switch-selected-track-color"))
        assertTrue(dark.contains("#9D63FF"))
        assertTrue(dark.contains("#18191E"))
        assertTrue(light.contains("html,body,#app{background:transparent!important}"))
    }

    @Test
    fun kpatchExcludeSwitchScriptKeepsNativeControlsAndSupportsDynamicApps() {
        val script = kpatchNextSwitchStyleScript("custom", "{\"source\":\"pixel\"}", dark = true)

        assertTrue(script.contains("#exclude-page md-switch.app-switch"))
        assertTrue(script.contains("--md-switch-selected-track-color"))
        assertTrue(script.contains("selected"))
        assertTrue(script.contains("MutationObserver"))
        assertTrue(script.contains("__apkesuKpatchSwitchObserver"))
        assertTrue(script.contains("dataset.apkesuSwitchDark"))
        assertTrue(script.contains("isSkrootpro"))
    }

    @Test
    fun kpatchExcludeSwitchScriptUsesAnExplicitEmbeddedThemeWhenAvailable() {
        val script = kpatchNextSwitchStyleScript(
            switchStyle = "original",
            dark = false,
            theme = "skrootpro",
        )

        assertTrue(script.contains("const configuredTheme = 'skrootpro'"))
        assertTrue(script.contains("configuredTheme || document.documentElement.dataset.apkesuTheme"))
    }
}
