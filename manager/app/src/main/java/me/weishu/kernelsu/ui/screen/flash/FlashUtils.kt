package me.weishu.kernelsu.ui.screen.flash

import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.util.FlashResult
import me.weishu.kernelsu.ui.util.LkmSelection
import me.weishu.kernelsu.ui.util.flashAnyKernelZip
import me.weishu.kernelsu.ui.util.flashModule
import me.weishu.kernelsu.ui.util.installBoot
import me.weishu.kernelsu.ui.util.restoreBoot
import me.weishu.kernelsu.ui.util.uninstallPermanently
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class FlashingStatus {
    FLASHING,
    SUCCESS,
    FAILED
}

enum class UninstallType(val icon: ImageVector, val title: Int, val message: Int) {
    PERMANENT(
        Icons.Rounded.DeleteForever,
        R.string.settings_uninstall_permanent,
        R.string.settings_uninstall_permanent_message
    ),
    RESTORE_STOCK_IMAGE(
        Icons.Rounded.RestartAlt,
        R.string.settings_restore_stock_image,
        R.string.settings_restore_stock_image_message
    )
}

@Parcelize
sealed class FlashIt : Parcelable {
    @Parcelize
    data class FlashBoot(
        val boot: Uri? = null,
        val lkm: LkmSelection,
        val ota: Boolean,
        val partition: String? = null,
        val allowShell: Boolean = false,
        val enableAdb: Boolean = false,
    ) : FlashIt()

    @Parcelize
    data class FlashModules(val uris: List<Uri>) : FlashIt()

    @Parcelize
    data class FlashAnyKernel(val uri: Uri) : FlashIt()

    @Parcelize
    data object FlashRestore : FlashIt()

    @Parcelize
    data object FlashUninstall : FlashIt()
}

fun flashModulesSequentially(
    uris: List<Uri>,
    onStdout: (String) -> Unit,
    onStderr: (String) -> Unit
): FlashResult {
    for (uri in uris) {
        flashModule(uri, onStdout, onStderr).apply {
            if (code != 0) {
                return FlashResult(code, err, showReboot)
            }
        }
    }
    return FlashResult(0, "", true)
}

fun flashIt(
    flashIt: FlashIt,
    onStdout: (String) -> Unit,
    onStderr: (String) -> Unit
): FlashResult {
    return when (flashIt) {
        is FlashIt.FlashBoot -> installBoot(
            flashIt.boot,
            flashIt.lkm,
            flashIt.ota,
            flashIt.partition,
            flashIt.allowShell,
            flashIt.enableAdb,
            onStdout,
            onStderr
        )

        is FlashIt.FlashModules -> {
            flashModulesSequentially(flashIt.uris, onStdout, onStderr)
        }

        is FlashIt.FlashAnyKernel -> {
            flashAnyKernelZip(flashIt.uri, onStdout, onStderr)
        }

        FlashIt.FlashRestore -> restoreBoot(onStdout, onStderr)
        FlashIt.FlashUninstall -> uninstallPermanently(onStdout, onStderr)
    }
}

@Composable
fun FlashEffect(
    flashIt: FlashIt,
    text: String,
    logContent: StringBuilder,
    onTextUpdate: (String) -> Unit,
    onShowRebootChange: (Boolean) -> Unit,
    onFlashingStatusChange: (FlashingStatus) -> Unit,
    onFlashSuccess: () -> Unit,
    enabled: Boolean = true
) {
    val flashErrorCode = stringResource(R.string.flash_error_code)
    val flashCheckLog = stringResource(R.string.flash_check_log)

    LaunchedEffect(enabled) {
        if (!enabled || text.isNotEmpty()) {
            return@LaunchedEffect
        }
        var currentText = text
        val mainHandler = Handler(Looper.getMainLooper())
        withContext(Dispatchers.IO) {
            val result = runCatching {
                flashIt(flashIt, onStdout = {
                    val tempText = "$it\n"
                    if (tempText.startsWith("[H[J")) { // clear command
                        currentText = tempText.substring(6)
                    } else {
                        currentText += tempText
                    }
                    mainHandler.post {
                        onTextUpdate(currentText)
                    }
                    logContent.append(it).append("\n")
                }, onStderr = {
                    logContent.append(it).append("\n")
                })
            }.getOrElse { throwable ->
                val message = throwable.localizedMessage ?: throwable.javaClass.simpleName
                logContent.append(message).append("\n")
                FlashResult(1, message, false)
            }

            result.apply {
                if (code != 0) {
                    currentText += "$flashErrorCode: $code.\n $err $flashCheckLog\n"
                    mainHandler.post {
                        onTextUpdate(currentText)
                    }
                }
                if (showReboot) {
                    currentText += "\n\n\n"
                    mainHandler.post {
                        onTextUpdate(currentText)
                        onShowRebootChange(true)
                    }
                }
                mainHandler.post {
                    if (code == 0) {
                        onFlashSuccess()
                    }
                    onFlashingStatusChange(if (code == 0) FlashingStatus.SUCCESS else FlashingStatus.FAILED)
                }
            }
        }
    }
}

fun saveLog(
    logContent: StringBuilder,
    scope: CoroutineScope,
    savedMessage: String,
    failedMessage: String,
    showMessage: (String) -> Unit
): () -> Unit {
    return {
        scope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val format = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault())
                    val date = format.format(Date())
                    val downloads =
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    if (!downloads.exists()) {
                        downloads.mkdirs()
                    }
                    val file = File(downloads, "KernelSU_install_log_${date}.log")
                    file.writeText(logContent.toString())
                    file.absolutePath
                }
            }
            result.onSuccess { path ->
                showMessage("$savedMessage: $path")
            }.onFailure { throwable ->
                val reason = throwable.localizedMessage ?: throwable.javaClass.simpleName
                showMessage("$failedMessage: $reason")
            }
        }
    }
}

private const val JAILBREAK_WARNING_COUNTDOWN = 10

@Composable
fun JailbreakFlashWarningDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var countdown by remember { mutableIntStateOf(JAILBREAK_WARNING_COUNTDOWN) }

    LaunchedEffect(Unit) {
        while (countdown > 0) {
            delay(1000)
            countdown--
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(android.R.string.dialog_alert_title)) },
        text = {
            Text(
                stringResource(R.string.jailbreak_flash_warning),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = countdown == 0
            ) {
                Text(
                    if (countdown > 0)
                        stringResource(R.string.jailbreak_flash_warning_countdown, countdown)
                    else
                        stringResource(R.string.install_next)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}
