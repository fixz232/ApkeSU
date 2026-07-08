package me.weishu.kernelsu.ui.viewmodel

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.system.Os
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.BuildConfig
import me.weishu.kernelsu.KernelVersion
import me.weishu.kernelsu.Natives
import me.weishu.kernelsu.data.repository.SettingsRepository
import me.weishu.kernelsu.data.repository.SettingsRepositoryImpl
import me.weishu.kernelsu.data.repository.SHOW_GKI_WARNING_KEY
import me.weishu.kernelsu.data.repository.SHOW_HOME_LEARN_CARD_KEY
import me.weishu.kernelsu.data.repository.SHOW_HOME_SUPPORT_CARD_KEY
import me.weishu.kernelsu.data.repository.SHOW_VERSION_MISMATCH_WARNING_KEY
import me.weishu.kernelsu.getKernelVersion
import me.weishu.kernelsu.ksuApp
import me.weishu.kernelsu.ui.screen.home.HomeUiState
import me.weishu.kernelsu.ui.screen.home.SystemInfo
import me.weishu.kernelsu.ui.screen.home.getManagerVersion
import me.weishu.kernelsu.ui.util.apkeSuRootAvailable
import me.weishu.kernelsu.ui.util.ensureManagerRegistered
import me.weishu.kernelsu.ui.util.getModuleCount
import me.weishu.kernelsu.ui.util.getSELinuxStatusRaw
import me.weishu.kernelsu.ui.util.getSuperuserCount
import me.weishu.kernelsu.ui.util.isHiddenPathLkmMode
import me.weishu.kernelsu.ui.util.resolveDeviceName
import me.weishu.kernelsu.ui.util.rootAvailable

class HomeViewModel(
    private val repo: SettingsRepository = SettingsRepositoryImpl()
) : ViewModel() {

    private val prefs = ksuApp.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == SHOW_VERSION_MISMATCH_WARNING_KEY ||
            key == SHOW_GKI_WARNING_KEY ||
            key == SHOW_HOME_SUPPORT_CARD_KEY ||
            key == SHOW_HOME_LEARN_CARD_KEY
        ) {
            _uiState.update {
                it.copy(
                    showVersionMismatchWarningSetting = repo.showVersionMismatchWarning,
                    showGkiWarningSetting = repo.showGkiWarning,
                    showHomeSupportCard = repo.showHomeSupportCard,
                    showHomeLearnCard = repo.showHomeLearnCard,
                )
            }
        }
    }

    private val _uiState = MutableStateFlow(fallbackState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    override fun onCleared() {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
        super.onCleared()
    }

    fun refresh() {
        viewModelScope.launch {
            val baseState = withContext(Dispatchers.IO) {
                runCatching { Natives.refreshInfo() }
                buildStateSafely()
            }
            _uiState.update { baseState }
        }
    }

    private fun buildStateSafely(): HomeUiState {
        return runCatching { buildState() }.getOrElse { throwable ->
            Log.e(TAG, "build home state failed", throwable)
            fallbackState()
        }
    }

    private fun buildState(): HomeUiState {
        val kernelVersion = getKernelVersion()
        var isManager = runCatching { Natives.isManager }.getOrDefault(false)
        val ksuVersion = runCatching { Natives.version.takeIf { it > 0 } }.getOrNull()
        val isKsuRootAvailable = ksuVersion != null ||
                runCatching { apkeSuRootAvailable() }.getOrDefault(false)
        if (!isManager && isKsuRootAvailable) {
            isManager = runCatching { ensureManagerRegistered() }.getOrDefault(false)
        }
        val kernelUAPIVersion = ksuVersion?.let { runCatching { Natives.kernelUAPIVersion }.getOrNull() }
        val managerUAPIVersion = runCatching { Natives.managerUAPIVersion }.getOrDefault(0)
        val lkmMode = ksuVersion?.let {
            if (kernelVersion.isGKI()) runCatching { Natives.isLkmMode }.getOrNull() else null
        }
        val isRootAvailable = isKsuRootAvailable || runCatching { rootAvailable() }.getOrDefault(false)
        val hiddenPathLkmMode = lkmMode == true &&
            isRootAvailable &&
            runCatching { isHiddenPathLkmMode() }.getOrDefault(false)
        val managerVersion = getManagerVersion(ksuApp)

        return HomeUiState(
            kernelVersion = kernelVersion,
            ksuVersion = ksuVersion,
            isKernelActive = ksuVersion != null || isKsuRootAvailable,
            lkmMode = lkmMode,
            hiddenPathLkmMode = hiddenPathLkmMode,
            isManager = isManager,
            isManagerPrBuild = BuildConfig.IS_PR_BUILD,
            isKernelPrBuild = runCatching { Natives.isPrBuild }.getOrDefault(false),
            requiresNewKernel = isManager && runCatching { Natives.requireNewKernel() }.getOrDefault(false),
            uapiMismatch = isManager && runCatching { Natives.checkUAPIMismatch() }.getOrDefault(false),
            kernelUAPIVersion = kernelUAPIVersion,
            managerUAPIVersion = managerUAPIVersion,
            isRootAvailable = isRootAvailable,
            isSafeMode = runCatching { Natives.isSafeMode }.getOrDefault(false),
            isLateLoadMode = runCatching { Natives.isLateLoadMode }.getOrDefault(false),
            currentManagerVersionCode = managerVersion.versionCode,
            showVersionMismatchWarningSetting = repo.showVersionMismatchWarning,
            showGkiWarningSetting = repo.showGkiWarning,
            showHomeSupportCard = repo.showHomeSupportCard,
            showHomeLearnCard = repo.showHomeLearnCard,
            superuserCount = runCatching { getSuperuserCount() }.getOrDefault(0),
            moduleCount = runCatching { getModuleCount() }.getOrDefault(0),
            systemInfo = SystemInfo(
                kernelVersion = runCatching { Os.uname().release }.getOrDefault("unknown"),
                managerVersion = "${managerVersion.versionName} (${managerVersion.versionCode}-${managerUAPIVersion})",
                deviceModel = runCatching { resolveDeviceName() }.getOrDefault(Build.MODEL),
                fingerprint = Build.FINGERPRINT,
                selinuxStatus = runCatching { getSELinuxStatusRaw() }.getOrDefault("unknown"),
                seccompStatus = runCatching {
                    Os.prctl(21 /* PR_GET_SECCOMP */, 0, 0, 0, 0)
                }.getOrDefault(-1),
            ),
        )
    }

    private fun fallbackState(): HomeUiState {
        val managerVersion = runCatching { getManagerVersion(ksuApp) }.getOrNull()
        val managerUAPIVersion = runCatching { Natives.managerUAPIVersion }.getOrDefault(0)
        return HomeUiState(
            kernelVersion = runCatching { getKernelVersion() }.getOrElse { KernelVersion(0, 0, 0) },
            ksuVersion = null,
            managerUAPIVersion = managerUAPIVersion,
            kernelUAPIVersion = null,
            lkmMode = null,
            isManager = false,
            isManagerPrBuild = BuildConfig.IS_PR_BUILD,
            isKernelPrBuild = false,
            requiresNewKernel = false,
            uapiMismatch = false,
            isRootAvailable = false,
            isSafeMode = false,
            isLateLoadMode = false,
            currentManagerVersionCode = managerVersion?.versionCode ?: BuildConfig.VERSION_CODE.toLong(),
            showVersionMismatchWarningSetting = runCatching {
                repo.showVersionMismatchWarning
            }.getOrDefault(true),
            showGkiWarningSetting = runCatching { repo.showGkiWarning }.getOrDefault(true),
            showHomeSupportCard = runCatching { repo.showHomeSupportCard }.getOrDefault(true),
            showHomeLearnCard = runCatching { repo.showHomeLearnCard }.getOrDefault(true),
            superuserCount = 0,
            moduleCount = 0,
            systemInfo = SystemInfo(
                kernelVersion = runCatching { Os.uname().release }.getOrDefault("unknown"),
                managerVersion = "${managerVersion?.versionName ?: BuildConfig.VERSION_NAME} (${managerVersion?.versionCode ?: BuildConfig.VERSION_CODE}-$managerUAPIVersion)",
                deviceModel = Build.MODEL,
                fingerprint = Build.FINGERPRINT,
                selinuxStatus = "unknown",
                seccompStatus = -1,
            ),
        )
    }

    private companion object {
        const val TAG = "HomeViewModel"
    }
}
