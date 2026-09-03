package me.weishu.kernelsu.ui.webui

import android.app.Activity
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.Window
import android.webkit.JavascriptInterface
import android.widget.Toast
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.topjohnwu.superuser.CallbackList
import com.topjohnwu.superuser.ShellUtils
import com.topjohnwu.superuser.internal.UiThreadHandler
import me.weishu.kernelsu.ui.util.createRootShell
import me.weishu.kernelsu.ui.util.listModules
import me.weishu.kernelsu.ui.util.withNewRootShell
import me.weishu.kernelsu.ksuApp
import me.weishu.kernelsu.ui.viewmodel.SuperUserViewModel
import me.weishu.kernelsu.ui.webui.file.KsuIO
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.CompletableFuture

class WebViewInterface(private val state: WebUIState) {
    private val webView get() = state.webView
    private val modDir get() = state.modDir

    @JavascriptInterface
    fun exec(cmd: String): String {
        return withNewRootShell(true) { ShellUtils.fastCmd(this, cmd) }
    }

    @JavascriptInterface
    fun exec(cmd: String, callbackFunc: String) {
        exec(cmd, null, callbackFunc)
    }

    private fun processOptions(sb: StringBuilder, options: String?) {
        val opts = if (options == null) JSONObject() else {
            JSONObject(options)
        }

        val cwd = opts.optString("cwd")
        if (!TextUtils.isEmpty(cwd)) {
            sb.append("cd ${cwd};")
        }

        opts.optJSONObject("env")?.let { env ->
            env.keys().forEach { key ->
                sb.append("export ${key}=${env.getString(key)};")
            }
        }
    }

    @JavascriptInterface
    fun exec(
        cmd: String,
        options: String?,
        callbackFunc: String
    ) {
        val finalCommand = StringBuilder()
        processOptions(finalCommand, options)
        finalCommand.append(cmd)

        val result = withNewRootShell(true) {
            newJob().add(finalCommand.toString()).to(ArrayList(), ArrayList()).exec()
        }
        val stdout = result.out.joinToString(separator = "\n")
        val stderr = result.err.joinToString(separator = "\n")

        val jsCode =
            "javascript: (function() { try { ${callbackFunc}(${result.code}, ${
                JSONObject.quote(
                    stdout
                )
            }, ${JSONObject.quote(stderr)}); } catch(e) { console.error(e); } })();"
        webView?.post {
            webView?.loadUrl(jsCode)
        }
    }

    @JavascriptInterface
    fun spawn(command: String, args: String, options: String?, callbackFunc: String) {
        val finalCommand = StringBuilder()

        processOptions(finalCommand, options)

        if (!TextUtils.isEmpty(args)) {
            finalCommand.append(command).append(" ")
            JSONArray(args).let { argsArray ->
                for (i in 0 until argsArray.length()) {
                    finalCommand.append(argsArray.getString(i))
                    finalCommand.append(" ")
                }
            }
        } else {
            finalCommand.append(command)
        }

        val shell = createRootShell(true)

        val emitData = fun(name: String, data: String) {
            val jsCode =
                "javascript: (function() { try { ${callbackFunc}.${name}.emit('data', ${
                    JSONObject.quote(
                        data
                    )
                }); } catch(e) { console.error('emitData', e); } })();"
            webView?.post {
                webView?.loadUrl(jsCode)
            }
        }

        val stdout = object : CallbackList<String>(UiThreadHandler::runAndWait) {
            override fun onAddElement(s: String) {
                emitData("stdout", s)
            }
        }

        val stderr = object : CallbackList<String>(UiThreadHandler::runAndWait) {
            override fun onAddElement(s: String) {
                emitData("stderr", s)
            }
        }

        val future = shell.newJob().add(finalCommand.toString()).to(stdout, stderr).enqueue()
        val completableFuture = CompletableFuture.supplyAsync {
            future.get()
        }

        completableFuture.thenAccept { result ->
            val emitExitCode =
                "javascript: (function() { try { ${callbackFunc}.emit('exit', ${result.code}); } catch(e) { console.error(`emitExit error: \${e}`); } })();"
            webView?.post {
                webView?.loadUrl(emitExitCode)
            }

            if (result.code != 0) {
                val emitErrCode =
                    "javascript: (function() { try { var err = new Error(); err.exitCode = ${result.code}; err.message = ${
                        JSONObject.quote(
                            result.err.joinToString(
                                "\n"
                            )
                        )
                    };${callbackFunc}.emit('error', err); } catch(e) { console.error('emitErr', e); } })();"
                webView?.post {
                    webView?.loadUrl(emitErrCode)
                }
            }
        }.whenComplete { _, _ ->
            runCatching { shell.close() }
        }
    }

    @JavascriptInterface
    fun toast(msg: String) {
        webView?.post {
            webView?.let { Toast.makeText(it.context, msg, Toast.LENGTH_SHORT).show() }
        }
    }

    @JavascriptInterface
    fun fullScreen(enable: Boolean) {
        val context = webView?.context
        if (context is Activity) {
            Handler(Looper.getMainLooper()).post {
                if (enable) {
                    hideSystemUI(context.window)
                } else {
                    showSystemUI(context.window)
                }
            }
        }
        enableEdgeToEdge(enable)
    }

    @JavascriptInterface
    fun enableEdgeToEdge(enable: Boolean = true) {
        state.isInsetsEnabled = enable
    }

    @JavascriptInterface
    fun moduleInfo(): String {
        val moduleInfos = JSONArray(listModules())
        val currentModuleInfo = JSONObject()
        currentModuleInfo.put("moduleDir", modDir)
        if (state.isBuiltinModule) {
            currentModuleInfo.put("id", state.moduleId)
            currentModuleInfo.put("name", state.moduleName)
            currentModuleInfo.put("version", state.moduleVersion)
            currentModuleInfo.put("versionCode", state.moduleVersionCode)
            currentModuleInfo.put("author", "Hybrid Mount Developers")
            currentModuleInfo.put("description", "Built-in Hybrid Mount Lite")
            currentModuleInfo.put("enabled", true)
            currentModuleInfo.put("update", false)
            currentModuleInfo.put("remove", false)
            currentModuleInfo.put("web", true)
            currentModuleInfo.put("metamodule", true)
            return currentModuleInfo.toString()
        }
        val moduleId = File(modDir).name
        for (i in 0 until moduleInfos.length()) {
            val currentInfo = moduleInfos.getJSONObject(i)

            if (currentInfo.getString("id") != moduleId) {
                continue
            }

            val keys = currentInfo.keys()
            for (key in keys) {
                currentModuleInfo.put(key, currentInfo.get(key))
            }
            break
        }
        return currentModuleInfo.toString()
    }

    @JavascriptInterface
    fun listPackages(type: String): String {
        val packageNames = packageRecords()
            .filter { record ->
                when (type.lowercase()) {
                    "system" -> record.isSystem
                    "user" -> !record.isSystem
                    else -> true
                }
            }
            .map { it.packageName }
            .sorted()

        val jsonArray = JSONArray()
        for (pkgName in packageNames) {
            jsonArray.put(pkgName)
        }
        return jsonArray.toString()
    }

    @JavascriptInterface
    fun getPackagesInfo(packageNamesJson: String): String {
        val packageNames = JSONArray(packageNamesJson)
        val jsonArray = JSONArray()
        val appMap = packageRecords().associateBy { it.packageName }
        for (i in 0 until packageNames.length()) {
            val pkgName = packageNames.getString(i)
            val appInfo = appMap[pkgName]
            if (appInfo != null) {
                val obj = JSONObject()
                obj.put("packageName", appInfo.packageName)
                obj.put("versionName", appInfo.versionName)
                obj.put("versionCode", appInfo.versionCode)
                obj.put("appLabel", appInfo.label)
                obj.put("isSystem", appInfo.isSystem)
                obj.put("uid", appInfo.uid)
                jsonArray.put(obj)
            } else {
                val obj = JSONObject()
                obj.put("packageName", pkgName)
                obj.put("error", "Package not found or inaccessible")
                jsonArray.put(obj)
            }
        }
        return jsonArray.toString()
    }

    private data class PackageRecord(
        val packageName: String,
        val versionName: String,
        val versionCode: Long,
        val label: String,
        val isSystem: Boolean,
        val uid: Int,
    )

    /**
     * WebUI calls this synchronously during its first render. The root-backed
     * SuperUser cache may still be loading, so use PackageManager as an immediate
     * fallback instead of returning an empty list once.
     */
    private fun packageRecords(): List<PackageRecord> {
        val records = LinkedHashMap<String, PackageRecord>()
        val cached = SuperUserViewModel.apps.filterNot { it.special }.mapNotNull { appInfo ->
            val packageInfo = appInfo.packageInfo
            val applicationInfo = packageInfo.applicationInfo ?: return@mapNotNull null
            PackageRecord(
                packageName = packageInfo.packageName,
                versionName = packageInfo.versionName.orEmpty(),
                versionCode = PackageInfoCompat.getLongVersionCode(packageInfo),
                label = appInfo.label,
                isSystem = (applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                uid = applicationInfo.uid,
            )
        }
        cached.forEach { record -> records[record.packageName] = record }

        val installed = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ksuApp.packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                ksuApp.packageManager.getInstalledPackages(0)
            }
        }.getOrDefault(emptyList())

        installed.mapNotNull { packageInfo ->
            val applicationInfo = packageInfo.applicationInfo ?: return@mapNotNull null
            PackageRecord(
                packageName = packageInfo.packageName,
                versionName = packageInfo.versionName.orEmpty(),
                versionCode = PackageInfoCompat.getLongVersionCode(packageInfo),
                label = runCatching {
                    applicationInfo.loadLabel(ksuApp.packageManager).toString()
                }.getOrDefault(packageInfo.packageName),
                isSystem = (applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                uid = applicationInfo.uid,
            )
        }.forEach { record -> records[record.packageName] = record }

        return records.values.toList()
    }

    @JavascriptInterface
    fun exit() {
        state.requestExit()
    }

    @JavascriptInterface
    fun io() = KsuIO

    fun destroy() {
        KsuIO.destroy()
    }
}

fun hideSystemUI(window: Window) =
    WindowInsetsControllerCompat(window, window.decorView).let { controller ->
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

fun showSystemUI(window: Window) =
    WindowInsetsControllerCompat(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
