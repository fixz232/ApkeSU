package me.weishu.kernelsu.ui.webui

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.view.MotionEvent
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader
import com.topjohnwu.superuser.io.SuFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.R
import me.weishu.kernelsu.data.repository.ModuleRepositoryImpl
import me.weishu.kernelsu.ui.util.HYBRID_MOUNT_MODULE_ID
import me.weishu.kernelsu.ui.util.createRootShell
import me.weishu.kernelsu.ui.util.getBuiltinMountStatus
import me.weishu.kernelsu.ui.viewmodel.SuperUserViewModel
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

private const val WEB_DOMAIN = "mui.kernelsu.org"
private const val KSU_SCHEME = "ksu"
private const val ICON_HOST = "icon"
private const val DOWNLOAD_JS = """
    (function() {
        if (window.ksu_download_enabled) return;
        window.ksu_download_enabled = true;
        const blobMap = new Map();
        const originalCreateObjectURL = URL.createObjectURL;
        URL.createObjectURL = (obj) => {
            const url = originalCreateObjectURL(obj);
            if (obj instanceof Blob) blobMap.set(url, obj);
            return url;
        };
        const originalRevokeObjectURL = URL.revokeObjectURL;
        URL.revokeObjectURL = (url) => {
            setTimeout(() => blobMap.delete(url), 10000);
            return originalRevokeObjectURL(url);
        };
        const handleDownload = async (anchor) => {
            const url = new URL(anchor.href, location.href);
            const fileName = anchor.download || url.pathname.split("/").pop().split("?")[0] || "download.bin";
            const isInternal = url.hostname === 'mui.kernelsu.org';
            if (url.protocol === 'blob:' || url.protocol === 'data:' || isInternal) {
                const blob = (url.protocol === 'blob:' && blobMap.has(url.href)) ? blobMap.get(url.href) : await (await fetch(url.href, { credentials: 'include' })).blob();
                const base64 = await new Promise((resolve, reject) => {
                    const reader = new FileReader();
                    reader.onload = () => resolve(reader.result.split(',')[1] || '');
                    reader.onerror = () => reject(reader.error || new Error('Failed to read blob'));
                    reader.readAsDataURL(blob);
                });
                ksu_download.save(base64, fileName);
                return;
            }
            ksu_download.download(url.href, fileName, anchor.type || null);
        };
        document.addEventListener('click', (event) => {
            const anchor = event.target.closest('a[download]');
            if (!anchor || !anchor.href) return;
            event.preventDefault();
            handleDownload(anchor).catch((error) => console.error('KernelSU download failed', error));
        }, true);
        const originalClick = HTMLAnchorElement.prototype.click;
        HTMLAnchorElement.prototype.click = function() {
            if (this.hasAttribute('download') && this.href) {
                handleDownload(this).catch((error) => console.error('KernelSU download failed', error));
                return;
            }
            return originalClick.apply(this, arguments);
        };
    })();
"""

fun Activity.setTaskDescription(label: String) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        @Suppress("DEPRECATION")
        setTaskDescription(ActivityManager.TaskDescription(label))
    } else {
        val taskDescription = ActivityManager.TaskDescription.Builder()
            .setLabel(label)
            .build()
        setTaskDescription(taskDescription)
    }
}

@SuppressLint("SetJavaScriptEnabled")
internal suspend fun prepareWebView(
    activity: Activity,
    moduleId: String,
    webUIState: WebUIState,
    allowPendingUpdate: Boolean = false,
) {
    val loadGeneration = webUIState.beginLoading()
    withContext(Dispatchers.IO) {
        val repo = ModuleRepositoryImpl()
        val modulesResult = repo.getModules()
        if (modulesResult.isFailure) {
            val message = modulesResult.exceptionOrNull()?.message.orEmpty()
                .ifBlank { activity.getString(R.string.module_failed_to_load) }
            withContext(Dispatchers.Main) {
                webUIState.reportError(loadGeneration, message)
            }
            return@withContext
        }
        val modules = modulesResult.getOrThrow()
        val moduleInfo = modules.find { info -> info.id == moduleId }

        val moduleName: String
        val moduleVersion: String
        val moduleVersionCode: String
        val modDir: String
        val hasWebUiHint: Boolean
        val isBuiltinModule: Boolean

        if (moduleInfo == null) {
            if (moduleId != HYBRID_MOUNT_MODULE_ID) {
                withContext(Dispatchers.Main) {
                    webUIState.reportError(loadGeneration, activity.getString(R.string.no_such_module, moduleId))
                }
                return@withContext
            }

            val builtinMountStatus = getBuiltinMountStatus()
            moduleName = builtinMountStatus.moduleName
            moduleVersion = builtinMountStatus.version
            moduleVersionCode = builtinMountStatus.versionCode
            modDir = builtinMountStatus.modulePath
            hasWebUiHint = builtinMountStatus.webUi
            isBuiltinModule = true

            if (!builtinMountStatus.enabled) {
                withContext(Dispatchers.Main) {
                    webUIState.reportError(loadGeneration, activity.getString(R.string.module_unavailable, moduleName))
                }
                return@withContext
            }
        } else if (!moduleInfo.enabled || (!allowPendingUpdate && moduleInfo.update) || moduleInfo.remove) {
            withContext(Dispatchers.Main) {
                webUIState.reportError(loadGeneration, activity.getString(R.string.module_unavailable, moduleInfo.name))
            }
            return@withContext
        } else {
            moduleName = moduleInfo.name
            moduleVersion = moduleInfo.version
            moduleVersionCode = moduleInfo.versionCode.toString()
            modDir = "/data/adb/modules/${moduleId}"
            hasWebUiHint = moduleInfo.hasWebUi
            isBuiltinModule = false
        }

        val shell = createRootShell(true)
        val hasWebRoot = hasWebUiHint || SuFile("$modDir/webroot").apply {
            setShell(shell)
        }.isDirectory

        if (!hasWebRoot) {
            shell.close()
            withContext(Dispatchers.Main) {
                webUIState.reportError(loadGeneration, activity.getString(R.string.module_unavailable, moduleName))
            }
            return@withContext
        }

        withContext(Dispatchers.Main) {
            if (!webUIState.applyModuleInfo(
                    loadGeneration = loadGeneration,
                    moduleId = moduleId,
                    moduleName = moduleName,
                    moduleVersion = moduleVersion,
                    moduleVersionCode = moduleVersionCode,
                    modDir = modDir,
                    isBuiltinModule = isBuiltinModule,
                )
            ) {
                shell.close()
                return@withContext
            }
            activity.setTaskDescription(activity.getString(R.string.app_name) + " - ${moduleName}")

            val webView = WebView(activity)
            webView.setBackgroundColor(Color.TRANSPARENT)
            webView.setOnTouchListener { view, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN,
                    MotionEvent.ACTION_MOVE -> view.parent?.requestDisallowInterceptTouchEvent(true)

                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_CANCEL -> view.parent?.requestDisallowInterceptTouchEvent(false)
                }
                false
            }

            val prefs = activity.getSharedPreferences("settings", Context.MODE_PRIVATE)
            val enableWebDebugging = prefs.getBoolean("enable_web_debugging", false)
            WebView.setWebContentsDebuggingEnabled(enableWebDebugging)

            webView.settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = false
            }

            if (SuperUserViewModel.apps.isEmpty()) {
                webUIState.preload(loadGeneration) {
                    SuperUserViewModel().fetchAppList()
                }
            }

            val webRoot = File("${webUIState.modDir}/webroot")
            val kpmWallpaperAssetHandler = KpmWallpaperAssetHandler(activity)
            val webViewAssetLoader = WebViewAssetLoader.Builder()
                .setDomain(WEB_DOMAIN)
                .addPathHandler(
                    "/",
                    SuFilePathHandler(
                        activity,
                        webRoot,
                        shell,
                        { webUIState.currentInsets },
                        { enable -> webUIState.isInsetsEnabled = enable })
                )
                .addPathHandler("/apkesu-kpm/", kpmWallpaperAssetHandler)
                .build()

            // WebViewClient
            webView.webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                    val url = request.url
                    if (url.scheme.equals(KSU_SCHEME, ignoreCase = true) && url.host.equals(ICON_HOST, ignoreCase = true)) {
                        val packageName = url.path?.substring(1)
                        if (!packageName.isNullOrEmpty() && packageName.matches(Regex("[a-zA-Z0-9._]+"))) {
                            val icon = AppIconUtil.loadAppIconSync(activity, packageName, 512)
                            if (icon != null) {
                                val stream = ByteArrayOutputStream()
                                icon.compress(Bitmap.CompressFormat.PNG, 100, stream)
                                return WebResourceResponse(
                                    "image/png", null, 200, "OK",
                                    mapOf("Access-Control-Allow-Origin" to "*"),
                                    ByteArrayInputStream(stream.toByteArray())
                                )
                            }
                        }
                    }
                    return webViewAssetLoader.shouldInterceptRequest(url)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    if (!webUIState.owns(view)) return
                    if (enableWebDebugging) {
                        view?.evaluateJavascript(erudaConsole(activity), null)
                        view?.evaluateJavascript("eruda.init();", null)
                    }
                    view?.let(webUIState::onPageFinished)
                }

                override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                    if (!webUIState.owns(view)) return
                    webUIState.webCanGoBack = view?.canGoBack() ?: false
                    if (webUIState.isInsetsEnabled) webUIState.webView?.evaluateJavascript(webUIState.currentInsets.js, null)
                    view?.evaluateJavascript(DOWNLOAD_JS, null)
                    super.doUpdateVisitedHistory(view, url, isReload)
                }
            }

            // WebChromeClient
            webView.webChromeClient = object : WebChromeClient() {
                override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                    if (!webUIState.owns(view) || message == null || result == null) return false
                    webUIState.uiEvent = WebUIEvent.ShowAlert(message, result)
                    return true
                }

                override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                    if (!webUIState.owns(view) || message == null || result == null) return false
                    webUIState.uiEvent = WebUIEvent.ShowConfirm(message, result)
                    return true
                }

                override fun onJsPrompt(
                    view: WebView?,
                    url: String?,
                    message: String?,
                    defaultValue: String?,
                    result: JsPromptResult?
                ): Boolean {
                    if (!webUIState.owns(view) || message == null || result == null || defaultValue == null) return false
                    webUIState.uiEvent = WebUIEvent.ShowPrompt(message, defaultValue, result)
                    return true
                }

                override fun onShowFileChooser(
                    webView: WebView?, filePathCallback: ValueCallback<Array<Uri>>?, fileChooserParams: FileChooserParams?
                ): Boolean {
                    if (!webUIState.owns(webView)) return false
                    webUIState.filePathCallback?.onReceiveValue(null)
                    webUIState.filePathCallback = filePathCallback

                    val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply { type = "*/*" }
                    if (fileChooserParams?.mode == FileChooserParams.MODE_OPEN_MULTIPLE) {
                        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                    }
                    webUIState.uiEvent = WebUIEvent.ShowFileChooser(intent)
                    return true
                }
            }

            // JS Interface
            val webviewInterface = WebViewInterface(webUIState)
            val downloadInterface = WebUIDownloadInterface(webUIState)
            if (!webUIState.attachWebView(
                    loadGeneration = loadGeneration,
                    view = webView,
                    rootShell = shell,
                    webViewInterface = webviewInterface,
                    downloadInterface = downloadInterface,
                    kpmWallpaperAssetHandler = kpmWallpaperAssetHandler,
                )
            ) {
                downloadInterface.destroy()
                webviewInterface.destroy()
                webView.destroy()
                shell.close()
                return@withContext
            }
            webView.addJavascriptInterface(webviewInterface, "ksu")
            webView.addJavascriptInterface(downloadInterface, "ksu_download")
            webView.setDownloadListener { url, _, contentDisposition, mimetype, _ ->
                val fileName = android.webkit.URLUtil.guessFileName(url, contentDisposition, mimetype)
                downloadInterface.download(url, fileName, mimetype)
            }
            webView.evaluateJavascript(DOWNLOAD_JS, null)
            webUIState.markWebViewReady(loadGeneration)
        }
    }
}

private fun erudaConsole(context: Context): String {
    return context.assets.open("eruda.min.js").bufferedReader().use { it.readText() }
}
