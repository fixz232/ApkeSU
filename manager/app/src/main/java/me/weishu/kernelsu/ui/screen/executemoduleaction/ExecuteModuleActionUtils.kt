package me.weishu.kernelsu.ui.screen.executemoduleaction

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.R
import me.weishu.kernelsu.data.repository.ModuleRepositoryImpl
import me.weishu.kernelsu.ui.util.runModuleAction
import me.weishu.kernelsu.ui.util.saveTextToDownloads
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ExecuteModuleActionEffect(
    moduleId: String,
    text: String,
    logContent: StringBuilder,
    fromShortcut: Boolean,
    autoCloseOnComplete: Boolean,
    onTextUpdate: (String) -> Unit,
    onComplete: () -> Unit = {},
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val noModule = stringResource(R.string.no_such_module)
    val moduleUnavailable = stringResource(R.string.module_unavailable)
    val moduleActionSuccess = stringResource(R.string.module_action_success)
    val moduleActionFailed = stringResource(R.string.module_action_failed)

    LaunchedEffect(Unit) {
        if (text.isNotEmpty()) {
            return@LaunchedEffect
        }
        val repo = ModuleRepositoryImpl()
        val modules = repo.getModules().getOrDefault(emptyList())
        val moduleInfo = modules.find { info -> info.id == moduleId }
        if (moduleInfo == null) {
            Toast.makeText(context, noModule.format(moduleId), Toast.LENGTH_SHORT).show()
            onExit()
            return@LaunchedEffect
        }
        if (!moduleInfo.hasActionScript) {
            onExit()
            return@LaunchedEffect
        }
        if (!moduleInfo.enabled || moduleInfo.update || moduleInfo.remove) {
            Toast.makeText(context, moduleUnavailable.format(moduleInfo.name), Toast.LENGTH_SHORT).show()
            onExit()
            return@LaunchedEffect
        }
        var actionResult: Boolean
        var currentText = text
        val mainHandler = Handler(Looper.getMainLooper())
        fun appendLine(line: String) {
            val tempText = "$line\n"
            if (tempText.startsWith("\u001B[H\u001B[J")) { // clear command
                currentText = tempText.substring(6)
            } else {
                currentText += tempText
            }
            mainHandler.post {
                onTextUpdate(currentText)
            }
            logContent.append(line).append("\n")
        }
        withContext(Dispatchers.IO) {
            val result = runModuleAction(
                moduleId = moduleId,
                onStdout = {
                    appendLine(it)
                },
                onStderr = {
                    appendLine(it)
                }
            )
            actionResult = result.isSuccess
            if (!result.isSuccess && result.err.isEmpty()) {
                appendLine(moduleActionFailed.format(result.code))
            }
        }
        if (actionResult && fromShortcut) {
            Toast.makeText(
                context,
                moduleActionSuccess,
                Toast.LENGTH_SHORT
            ).show()
        }
        if (actionResult && moduleId == "zygisk_lsposed") {
            openLsposedManager(context)
            onComplete()
            if (autoCloseOnComplete) {
                onExit()
            }
            return@LaunchedEffect
        }
        onComplete()
    }
}

private fun openLsposedManager(context: Context): Boolean {
    val flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    val directIntents = listOf(
        Intent().setComponent(
            ComponentName(
                "org.lsposed.manager",
                "org.lsposed.manager.ui.activity.MainActivity"
            )
        ),
        Intent().setComponent(
            ComponentName(
                "com.android.shell",
                "org.lsposed.manager.ui.activity.MainActivity"
            )
        )
    )

    directIntents.forEach { intent ->
        try {
            context.startActivity(intent.addFlags(flags))
            return true
        } catch (_: ActivityNotFoundException) {
        } catch (_: SecurityException) {
        }
    }

    val action = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
        "android.telephony.action.SECRET_CODE"
    } else {
        "android.provider.Telephony.SECRET_CODE"
    }
    val secretCodeIntent = Intent(action, Uri.parse("android_secret_code://5776733")).apply {
        setPackage("android")
        addFlags(flags)
    }
    return try {
        context.sendBroadcast(secretCodeIntent)
        true
    } catch (_: SecurityException) {
        false
    }
}

fun saveLog(
    context: Context,
    logContent: StringBuilder,
    scope: CoroutineScope,
    showMessage: (String) -> Unit
): () -> Unit {
    return {
        scope.launch {
            val format = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault())
            val date = format.format(Date())
            val result = runCatching {
                saveTextToDownloads(
                    context = context,
                    displayName = "KernelSU_module_action_log_${date}.log",
                    text = logContent.toString(),
                )
            }
            result.onSuccess { path ->
                showMessage("${context.getString(R.string.log_saved)}: $path")
            }.onFailure { throwable ->
                val reason = throwable.localizedMessage ?: throwable.javaClass.simpleName
                showMessage("${context.getString(R.string.log_save_failed)}: $reason")
            }
        }
    }
}
