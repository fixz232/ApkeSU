package me.weishu.kernelsu.ui.webui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.WebView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.util.KpmEntry

sealed class WebUIEvent {
    data object Loading : WebUIEvent()
    data object WebViewReady : WebUIEvent()
    data class Error(val message: String) : WebUIEvent()
    data object Close : WebUIEvent()
    data class ShowAlert(val message: String, val result: JsResult) : WebUIEvent()
    data class ShowConfirm(val message: String, val result: JsResult) : WebUIEvent()
    data class ShowPrompt(val message: String, val defaultValue: String, val result: JsPromptResult) : WebUIEvent()
    data class ShowFileChooser(val intent: Intent) : WebUIEvent()
}

enum class KPatchNextSection(
    val elementId: String,
    val pageId: String,
) {
    Kpm("KPM", "kpm-page"),
    Exclude("exclude", "exclude-page"),
}

internal fun kpatchNextSectionScript(section: KPatchNextSection): String = """
    (function() {
        const sectionId = '${section.elementId}';
        const pageId = '${section.pageId}';
        const styleId = 'apkesu-kpatch-next-embedded';
        const routeToken = (window.__apkesuKpatchRouteToken || 0) + 1;
        window.__apkesuKpatchRouteToken = routeToken;
        const hideHostChrome = () => {
            if (!document.head) return;
            if (!document.getElementById(styleId)) {
                const style = document.createElement('style');
                style.id = styleId;
                style.textContent = 'html,body,#app{background:transparent!important}.top-bar{display:none!important}.bottom-bar{display:none!important}.content{margin-top:0!important;margin-bottom:0!important}.page{transition:none!important;animation:none!important}.page:not(.active){visibility:hidden!important}.page.active{visibility:visible!important}';
                document.head.appendChild(style);
            }
        };
        const activateDirectly = () => {
            if (window.__apkesuKpatchRouteToken !== routeToken) return false;
            const targetPage = document.getElementById(pageId);
            if (!targetPage) return false;
            document.querySelectorAll('.page').forEach((page) => {
                page.classList.toggle('active', page.id === pageId);
            });
            document.querySelectorAll('.bottom-bar-item').forEach((item) => {
                item.toggleAttribute('selected', item.id === sectionId);
            });
            return targetPage.classList.contains('active');
        };
        let attempts = 0;
        let clickFallbackUsed = false;
        const openSection = () => {
            if (window.__apkesuKpatchRouteToken !== routeToken) return;
            hideHostChrome();
            if (activateDirectly()) {
                return;
            }

            // Some KPatch-Next builds create their pages after the load callback.
            // Use the host router only once as a fallback; repeatedly clicking it
            // restarts its own page animation and produces a visible flash.
            if (!clickFallbackUsed) {
                const button = document.getElementById(sectionId);
                if (button) {
                    clickFallbackUsed = true;
                    button.click();
                }
            }
            if (++attempts < 40) {
                setTimeout(openSection, 50);
            }
        };
        openSection();
    })();
""".trimIndent()

private data class EmbeddedKpmPalette(
    val accent: String,
    val surface: String,
    val content: String,
    val surfaceStrong: String,
    val muted: String,
    val outline: String,
    val accentContainer: String,
    val onAccent: String,
    val secondaryContainer: String,
    val onSecondaryContainer: String,
)

internal fun embeddedKpmThemeScript(theme: String, dark: Boolean): String {
    val safeTheme = theme.replace("\\", "\\\\").replace("'", "\\'")
    val palette = when (theme) {
        "pixel" -> if (dark) EmbeddedKpmPalette("#B58CFF", "#0B0A10", "#E8E1F5", "#1A1722", "#B8ADC7", "#5F566B", "#38265E", "#FFFFFF", "#2E2440", "#F4EDFF")
        else EmbeddedKpmPalette("#6A3BC2", "#F4F1F8", "#211B2A", "#E9E3F0", "#665F70", "#C9C0D1", "#E5D9FF", "#FFFFFF", "#E9E2F0", "#35264A")
        "rain" -> if (dark) EmbeddedKpmPalette("#8FB9D6", "#19232D", "#E2ECF2", "#263640", "#B6C5CD", "#5E7482", "#274E69", "#10212D", "#293A46", "#DDECF4")
        else EmbeddedKpmPalette("#4F708D", "#DDE8F1", "#1D2A35", "#CBD9E4", "#5C6E7D", "#99AFBF", "#C6DDF0", "#FFFFFF", "#D0DEE8", "#263B4C")
        "ink" -> if (dark) EmbeddedKpmPalette("#C8A9D8", "#171517", "#F0E8F1", "#29242A", "#C4B9C5", "#746B75", "#513B58", "#28192F", "#392C3E", "#F7ECF9")
        else EmbeddedKpmPalette("#765D7B", "#EEE9E8", "#29232A", "#E1D8DF", "#70646F", "#B5A8B1", "#E1D0E3", "#FFFFFF", "#E6DDE3", "#3B2C3F")
        "snow" -> if (dark) EmbeddedKpmPalette("#A8D8E8", "#15252D", "#E7F3F7", "#223841", "#B8CCD3", "#607A86", "#2B6070", "#12303A", "#2A434C", "#E5F7FC")
        else EmbeddedKpmPalette("#477B91", "#E8F2F5", "#1A2A31", "#D7E6EB", "#5D7179", "#A9C2CB", "#C6E4ED", "#FFFFFF", "#DDECEF", "#233A45")
        "liquid_glass" -> if (dark) EmbeddedKpmPalette("#B8D9EA", "#182127", "#EDF7FB", "#27343B", "#B9CBD3", "#647982", "#31576A", "#102B37", "#2D3D45", "#E9F8FE")
        else EmbeddedKpmPalette("#527D93", "#EAF1F5", "#1B2931", "#D9E5EA", "#5A707B", "#A5BDC8", "#CDE2EC", "#FFFFFF", "#E0EBF0", "#233A45")
        "skrootpro" -> if (dark) EmbeddedKpmPalette("#9D63FF", "#18191E", "#F0ECF5", "#292A31", "#B7B0C0", "#6E6875", "#3A216D", "#F7F0FF", "#482A53", "#FDEEFF")
        else EmbeddedKpmPalette("#7000F5", "#FFFFFF", "#1E1E1E", "#F5F6F8", "#7B7B7B", "#D9D9D9", "#E9DDFF", "#FFFFFF", "#F8E9F9", "#48134E")
        else -> if (dark) EmbeddedKpmPalette("#BCA7FF", "#17171A", "#F2F0F7", "#27252B", "#C8C2D0", "#6E6975", "#3F3561", "#251C3A", "#37313F", "#F2EDFF")
        else EmbeddedKpmPalette("#6750A4", "#F7F5FA", "#242229", "#ECE8F0", "#625D68", "#C9C3CD", "#E8DEF8", "#FFFFFF", "#E8DEF8", "#34265A")
    }
    val skrootproPageStyle = if (theme == "skrootpro") {
        "#kpm-page .page-content{background:var(--apkesu-surface)!important}" +
            "#exclude-page,#exclude-page .page-content,#exclude-page .app-list{background:var(--apkesu-surface)!important;color:var(--apkesu-content)!important}" +
            "#exclude-page .app-item{background:var(--apkesu-surface)!important;border-bottom:1px solid var(--apkesu-outline)!important}" +
            "#exclude-page .app-item:nth-child(even){background:var(--apkesu-surface-strong)!important}" +
            "#exclude-page .app-label{color:var(--apkesu-content)!important}" +
            "#exclude-page .app-package{color:var(--apkesu-muted)!important}" +
            "#exclude-page .icon-container{background:var(--apkesu-surface-strong)!important;border-radius:12px}" +
            "#exclude-page md-switch.app-switch{--md-switch-track-color:var(--apkesu-surface-strong);--md-switch-track-outline-color:var(--apkesu-outline);--md-switch-selected-track-color:var(--apkesu-accent);--md-switch-selected-track-outline-color:var(--apkesu-accent);--md-switch-selected-handle-color:var(--apkesu-on-accent)}"
    } else {
        ""
    }
    return """
        (function() {
          const root = document.documentElement;
          const body = document.body;
          if (!root || !body) return;
          root.dataset.apkesuTheme = '$safeTheme';
          root.dataset.apkesuDark = '${dark}';
          const styleId = 'apkesu-kpm-theme';
          let style = document.getElementById(styleId);
          if (!style) {
            style = document.createElement('style');
            style.id = styleId;
            (document.head || root).appendChild(style);
          }
          style.textContent = ':root{' +
            '--apkesu-accent:${palette.accent};--apkesu-surface:${palette.surface};--apkesu-content:${palette.content};--apkesu-surface-strong:${palette.surfaceStrong};--apkesu-muted:${palette.muted};--apkesu-outline:${palette.outline};--apkesu-accent-container:${palette.accentContainer};--apkesu-on-accent:${palette.onAccent};' +
            '--primary:${palette.accent};--onPrimary:${palette.onAccent};--primaryContainer:${palette.accentContainer};--onPrimaryContainer:${palette.content};--secondary:${palette.accent};--onSecondary:${palette.onAccent};--secondaryContainer:${palette.secondaryContainer};--onSecondaryContainer:${palette.onSecondaryContainer};' +
            '--background:${palette.surface};--onBackground:${palette.content};--surface:${palette.surface};--onSurface:${palette.content};--surfaceVariant:${palette.surfaceStrong};--onSurfaceVariant:${palette.muted};--outline:${palette.outline};--outlineVariant:${palette.outline};--inverseSurface:${palette.content};--inverseOnSurface:${palette.surface};--inversePrimary:${palette.accent};' +
            '--surfaceBright:${palette.surface};--surfaceDim:${palette.surfaceStrong};--tonalSurface:${palette.surfaceStrong};--surfaceContainer:${palette.surfaceStrong};--surfaceContainerLowest:${palette.surface};--surfaceContainerLow:${palette.surface};--surfaceContainerHigh:${palette.surfaceStrong};--surfaceContainerHighest:${palette.surfaceStrong};' +
            '--md-sys-color-primary:${palette.accent};--md-sys-color-on-primary:${palette.onAccent};--md-sys-color-primary-container:${palette.accentContainer};--md-sys-color-on-primary-container:${palette.content};--md-sys-color-secondary:${palette.accent};--md-sys-color-on-secondary:${palette.onAccent};--md-sys-color-secondary-container:${palette.secondaryContainer};--md-sys-color-on-secondary-container:${palette.onSecondaryContainer};--md-sys-color-background:${palette.surface};--md-sys-color-on-background:${palette.content};--md-sys-color-surface:${palette.surface};--md-sys-color-on-surface:${palette.content};--md-sys-color-surface-variant:${palette.surfaceStrong};--md-sys-color-on-surface-variant:${palette.muted};--md-sys-color-outline:${palette.outline};--md-sys-color-outline-variant:${palette.outline};--md-sys-color-tonal-surface:${palette.surfaceStrong};--md-sys-color-surface-container:${palette.surfaceStrong};--md-sys-color-surface-container-lowest:${palette.surface};--md-sys-color-surface-container-low:${palette.surface};--md-sys-color-surface-container-high:${palette.surfaceStrong};--md-sys-color-surface-container-highest:${palette.surfaceStrong};' +
            'color-scheme:${if (dark) "dark" else "light"}}' +
            'html,body,#app{background:transparent!important}' +
            'body{--md-sys-color-primary:var(--apkesu-accent);--primary:var(--apkesu-accent);--accent:var(--apkesu-accent)}' +
            '[class*="top-bar"],[class*="bottom-bar"]{background:transparent!important}' +
            '$skrootproPageStyle';
        })();
    """.trimIndent()
}

private fun escapeJavaScriptString(value: String): String = value
    .replace("\\", "\\\\")
    .replace("'", "\\'")
    .replace("\r", "\\r")
    .replace("\n", "\\n")
    .replace("\u2028", "\\u2028")
    .replace("\u2029", "\\u2029")

internal fun kpatchNextSwitchStyleScript(
    switchStyle: String,
    customStyleJson: String? = null,
    dark: Boolean = false,
    theme: String? = null,
): String {
    val safeStyle = escapeJavaScriptString(switchStyle)
    val safeCustomStyle = customStyleJson?.let(::escapeJavaScriptString)
    val safeTheme = theme?.let(::escapeJavaScriptString)
    return """
        (function() {
          const styleName = '$safeStyle';
          const styleId = 'apkesu-kpatch-switch-style';
          const observerKey = '__apkesuKpatchSwitchObserver';
          let custom = null;
          try {
            custom = ${if (safeCustomStyle == null) "null" else "JSON.parse('$safeCustomStyle')"};
          } catch (_) {
            custom = null;
          }

          const argb = (value, fallback) => {
            if (typeof value !== 'number' || !Number.isFinite(value)) return fallback;
            const channel = (shift) => (value >>> shift) & 255;
            return 'rgba(' + channel(16) + ',' + channel(8) + ',' + channel(0) + ',' +
              (channel(24) / 255).toFixed(3) + ')';
          };
          const gridColor = (grid, fallback) => {
            const pixels = Array.isArray(grid && grid.pixels) ? grid.pixels : [];
            const pixel = pixels.find((value) => typeof value === 'number' && ((value >>> 24) & 255) > 0);
            return argb(pixel, fallback);
          };
          const colorsFor = () => {
            const configuredTheme = ${if (safeTheme == null) "null" else "'$safeTheme'"};
            const isSkrootpro = (configuredTheme || document.documentElement.dataset.apkesuTheme) === 'skrootpro';
            const isDark = configuredTheme === null
              ? document.documentElement.dataset.apkesuDark === 'true'
              : ${if (dark) "true" else "false"};
            const defaults = {
              offTrack: isSkrootpro ? (isDark ? '#292A31' : '#F5F6F8') : '#2196F3',
              onTrack: isSkrootpro ? (isDark ? '#9D63FF' : '#7000F5') : '#090B12',
              offHandle: '#FFFFFF',
              onHandle: isSkrootpro ? '#FFFFFF' : '#E5E7EB',
              outline: isSkrootpro ? (isDark ? '#6E6875' : '#D9D9D9') : 'rgba(255,255,255,.35)',
              glow: isSkrootpro ? (isDark ? '0 0 10px rgba(157,99,255,.30)' : '0 0 8px rgba(112,0,245,.20)') : 'none',
              radius: '999px',
              pixel: false,
            };
            if (styleName === 'capsule') {
              return { ...defaults, offTrack: '#D8DEE8', onTrack: '#34C77B', outline: 'rgba(31,41,55,.28)' };
            }
            if (styleName === 'bb8') {
              return { ...defaults, offTrack: '#7FA9C0', onTrack: '#111A39', outline: 'rgba(127,169,192,.55)', glow: '0 0 8px rgba(127,169,192,.24)' };
            }
            if (styleName === 'sparkle') {
              return { ...defaults, offTrack: '#121212', onTrack: '#172A3D', outline: 'rgba(181,140,255,.55)', glow: '0 0 9px rgba(181,140,255,.28)' };
            }
            if (styleName === 'custom' && custom) {
              const offTrack = argb(custom.track_off_color, gridColor(custom.track_off, argb(custom.track_base_color, '#3D4450')));
              const onTrack = argb(custom.track_on_color, gridColor(custom.track_on, argb(custom.track_base_color, '#3D4450')));
              const offHandle = argb(custom.thumb_off_color, gridColor(custom.thumb_off, argb(custom.thumb_base_color, '#FFFFFF')));
              const onHandle = argb(custom.thumb_on_color, gridColor(custom.thumb_on, argb(custom.thumb_base_color, '#FFFFFF')));
              const border = argb(custom.border_color, 'rgba(255,255,255,.35)');
              const radius = Math.round(Math.max(0, Math.min(1, Number(custom.corner_radius_fraction ?? .5))) * 999) + 'px';
              return {
                ...defaults,
                offTrack,
                onTrack,
                offHandle,
                onHandle,
                outline: border,
                radius,
                pixel: custom.source === 'pixel',
              };
            }
            return defaults;
          };
          const colors = colorsFor();
          const styleText = `
            #exclude-page md-switch.app-switch {
              --md-switch-track-width: 58px;
              --md-switch-track-height: 32px;
              --md-switch-track-shape: ${'$'}{colors.radius};
              --md-switch-track-color: ${'$'}{colors.offTrack};
              --md-switch-selected-track-color: ${'$'}{colors.onTrack};
              --md-switch-track-outline-color: ${'$'}{colors.outline};
              --md-switch-selected-track-outline-color: ${'$'}{colors.outline};
              --md-switch-track-outline-width: 1px;
              --md-switch-handle-width: 24px;
              --md-switch-handle-height: 24px;
              --md-switch-selected-handle-width: 26px;
              --md-switch-selected-handle-height: 26px;
              --md-switch-handle-shape: ${'$'}{colors.radius};
              --md-switch-handle-color: ${'$'}{colors.offHandle};
              --md-switch-selected-handle-color: ${'$'}{colors.onHandle};
              --md-switch-state-layer-size: 42px;
              box-shadow: ${'$'}{colors.glow};
              transition: box-shadow 220ms ease, transform 220ms ease;
            }
            #exclude-page md-switch.app-switch:hover { transform: translateY(-1px); }
            #exclude-page md-switch.app-switch:focus-within {
              --md-switch-focus-track-outline-color: var(--apkesu-accent, #6750A4);
            }
            #exclude-page md-switch.app-switch[disabled] { opacity: .48; }
            ${'$'}{colors.pixel ? '#exclude-page md-switch.app-switch { image-rendering: pixelated; --md-switch-track-shape: 4px; --md-switch-handle-shape: 3px; }' : ''}
          `;
          let style = document.getElementById(styleId);
          if (!style) {
            style = document.createElement('style');
            style.id = styleId;
            (document.head || document.documentElement).appendChild(style);
          }
          style.textContent = styleText;
          document.documentElement.dataset.apkesuSwitchStyle = styleName;
          document.documentElement.dataset.apkesuSwitchDark = '$dark';

          if (window[observerKey]) window[observerKey].disconnect();
          const root = document.getElementById('exclude-page');
          if (root && window.MutationObserver) {
            window[observerKey] = new MutationObserver(() => {
              const page = document.getElementById('exclude-page');
              if (page) page.querySelectorAll('md-switch.app-switch').forEach((item) => {
                item.style.visibility = 'visible';
              });
            });
            window[observerKey].observe(root, { childList: true, subtree: true });
          }
        })();
    """.trimIndent()
}

class WebUIState {
    var webView: WebView? = null
    var rootShell: Shell? = null
    var webViewInterface: WebViewInterface? = null
    var downloadInterface: WebUIDownloadInterface? = null
    private var kpmWallpaperAssetHandler: KpmWallpaperAssetHandler? = null
    lateinit var modDir: String
    var moduleId: String = ""
    var moduleName: String = ""
    var moduleVersion: String = ""
    var moduleVersionCode: String = ""
    var isBuiltinModule: Boolean = false

    var uiEvent by mutableStateOf<WebUIEvent>(WebUIEvent.Loading)
    var isUrlLoaded = false
    @Volatile
    var currentInsets: Insets = Insets(0, 0, 0, 0)
    var isInsetsEnabled by mutableStateOf(false)
    var webCanGoBack by mutableStateOf(false)
    var filePathCallback: android.webkit.ValueCallback<Array<Uri>>? = null
    private var embeddedKpatchSection: KPatchNextSection? = null
    private var embeddedKpatchPageLoaded = false
    private var appliedEmbeddedKpatchSection: KPatchNextSection? = null
    @Volatile
    private var generation = 0L
    @Volatile
    private var disposed = false
    private var preloadJob: Job? = null
    @Volatile
    private var kpmWallpaperConfigJson: String = "[]"
    private var kpmWallpaperDark = false
    private var embeddedSwitchStyle: String? = null
    private var embeddedCustomSwitchStyleJson: String? = null
    private var embeddedSwitchDark = false
    private var embeddedTheme: String? = null
    private var embeddedThemeDark = false

    fun beginLoading(): Long {
        generation += 1
        preloadJob?.cancel()
        preloadJob = null
        disposeAttachedWebView()
        disposed = false
        isUrlLoaded = false
        webCanGoBack = false
        embeddedKpatchPageLoaded = false
        appliedEmbeddedKpatchSection = null
        kpmWallpaperAssetHandler = null
        kpmWallpaperConfigJson = "[]"
        uiEvent = WebUIEvent.Loading
        return generation
    }

    fun isActive(loadGeneration: Long): Boolean = !disposed && generation == loadGeneration

    fun owns(view: WebView?): Boolean = !disposed && view != null && webView === view

    fun reportError(loadGeneration: Long, message: String) {
        if (isActive(loadGeneration)) {
            uiEvent = WebUIEvent.Error(message)
        }
    }

    fun applyModuleInfo(
        loadGeneration: Long,
        moduleId: String,
        moduleName: String,
        moduleVersion: String,
        moduleVersionCode: String,
        modDir: String,
        isBuiltinModule: Boolean,
    ): Boolean {
        if (!isActive(loadGeneration)) return false
        this.moduleId = moduleId
        this.moduleName = moduleName
        this.moduleVersion = moduleVersion
        this.moduleVersionCode = moduleVersionCode
        this.modDir = modDir
        this.isBuiltinModule = isBuiltinModule
        return true
    }

    internal fun attachWebView(
        loadGeneration: Long,
        view: WebView,
        rootShell: Shell,
        webViewInterface: WebViewInterface,
        downloadInterface: WebUIDownloadInterface,
        kpmWallpaperAssetHandler: KpmWallpaperAssetHandler,
    ): Boolean {
        if (!isActive(loadGeneration)) return false
        disposeAttachedWebView()
        embeddedKpatchPageLoaded = false
        appliedEmbeddedKpatchSection = null
        webView = view
        this.rootShell = rootShell
        this.webViewInterface = webViewInterface
        this.downloadInterface = downloadInterface
        this.kpmWallpaperAssetHandler = kpmWallpaperAssetHandler
        return true
    }

    fun markWebViewReady(loadGeneration: Long) {
        if (isActive(loadGeneration)) {
            uiEvent = WebUIEvent.WebViewReady
        }
    }

    fun preload(loadGeneration: Long, block: suspend () -> Unit) {
        if (!isActive(loadGeneration)) return
        preloadJob?.cancel()
        preloadJob = CoroutineScope(Dispatchers.IO).launch {
            runCatching { block() }
        }
    }

    fun configureKpatchNextEmbedded(initialSection: KPatchNextSection = KPatchNextSection.Kpm) {
        embeddedKpatchSection = initialSection
    }

    fun configureEmbeddedTheme(theme: String, dark: Boolean) {
        embeddedTheme = theme
        embeddedThemeDark = dark
        if (embeddedKpatchPageLoaded) {
            applyEmbeddedTheme()
            applyEmbeddedSwitchStyle()
        }
    }

    fun configureEmbeddedSwitchStyle(
        style: String?,
        customStyleJson: String? = null,
        dark: Boolean = false,
    ) {
        embeddedSwitchStyle = style
        embeddedCustomSwitchStyleJson = customStyleJson
        embeddedSwitchDark = dark
        if (embeddedKpatchPageLoaded && style != null) {
            applyEmbeddedSwitchStyle()
        }
    }

    fun navigateToKpatchSection(section: KPatchNextSection) {
        if (disposed) return
        if (embeddedKpatchSection == section && appliedEmbeddedKpatchSection == section) return
        embeddedKpatchSection = section
        if (embeddedKpatchPageLoaded) {
            applyKpatchSection(section)
        }
    }

    fun onPageFinished(view: WebView) {
        if (!owns(view)) return
        embeddedKpatchPageLoaded = true
        applyEmbeddedTheme()
        embeddedKpatchSection?.let { section ->
            applyKpatchSection(section)
        }
        if (embeddedSwitchStyle != null) {
            applyEmbeddedSwitchStyle()
        }
        applyKpmWallpaperConfig()
    }

    fun setKpmWallpaperEntries(entries: List<KpmEntry>, dark: Boolean) {
        if (disposed) return
        kpmWallpaperDark = dark
        kpmWallpaperConfigJson = kpmWallpaperAssetHandler?.update(entries, dark) ?: "[]"
        applyKpmWallpaperConfig()
    }

    fun onAlertResult() {
        val event = uiEvent
        if (event is WebUIEvent.ShowAlert) {
            event.result.confirm()
            uiEvent = WebUIEvent.WebViewReady
        }
    }

    fun onConfirmResult(confirmed: Boolean) {
        val event = uiEvent
        if (event is WebUIEvent.ShowConfirm) {
            if (confirmed) event.result.confirm() else event.result.cancel()
            uiEvent = WebUIEvent.WebViewReady
        }
    }

    fun onPromptResult(result: String?) {
        val event = uiEvent
        if (event is WebUIEvent.ShowPrompt) {
            if (result != null) event.result.confirm(result) else event.result.cancel()
            uiEvent = WebUIEvent.WebViewReady
        }
    }

    fun onFileChooserResult(uris: Array<Uri>?) {
        if (disposed) return
        filePathCallback?.onReceiveValue(uris)
        filePathCallback = null
        uiEvent = WebUIEvent.WebViewReady
    }

    fun requestExit() {
        if (disposed) return
        uiEvent = WebUIEvent.Close
    }

    fun dispose(activity: Activity) {
        generation += 1
        disposed = true
        preloadJob?.cancel()
        preloadJob = null
        activity.setTaskDescription(activity.getString(R.string.app_name))
        disposeAttachedWebView()
        webCanGoBack = false
        isUrlLoaded = false
        embeddedKpatchPageLoaded = false
        appliedEmbeddedKpatchSection = null
        embeddedKpatchSection = null
        embeddedTheme = null
        embeddedThemeDark = false
        kpmWallpaperAssetHandler = null
        kpmWallpaperConfigJson = "[]"
        filePathCallback?.onReceiveValue(null)
        filePathCallback = null
    }

    private fun disposeAttachedWebView() {
        filePathCallback?.onReceiveValue(null)
        filePathCallback = null
        downloadInterface?.destroy()
        downloadInterface = null
        webViewInterface?.destroy()
        webViewInterface = null
        webView?.let { view ->
            (view.parent as? android.view.ViewGroup)?.removeView(view)
            view.destroy()
        }
        webView = null
        rootShell?.close()
        rootShell = null
    }

    private fun applyKpatchSection(section: KPatchNextSection) {
        val view = webView ?: return
        if (!owns(view) || appliedEmbeddedKpatchSection == section) return
        appliedEmbeddedKpatchSection = section
        view.evaluateJavascript(kpatchNextSectionScript(section), null)
    }

    private fun applyEmbeddedTheme() {
        val view = webView ?: return
        val theme = embeddedTheme ?: return
        if (!owns(view) || !embeddedKpatchPageLoaded) return
        view.evaluateJavascript(embeddedKpmThemeScript(theme, embeddedThemeDark), null)
    }

    private fun applyKpmWallpaperConfig() {
        val view = webView ?: return
        if (!owns(view) || !embeddedKpatchPageLoaded) return
        view.evaluateJavascript(
            kpmWallpaperApplyScript(kpmWallpaperConfigJson, kpmWallpaperDark),
            null,
        )
    }

    private fun applyEmbeddedSwitchStyle() {
        val view = webView ?: return
        val style = embeddedSwitchStyle ?: return
        if (!owns(view) || !embeddedKpatchPageLoaded) return
        view.evaluateJavascript(
            kpatchNextSwitchStyleScript(
                switchStyle = style,
                customStyleJson = embeddedCustomSwitchStyleJson,
                dark = embeddedSwitchDark,
                theme = embeddedTheme,
            ),
            null,
        )
    }
}
