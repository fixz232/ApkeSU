package me.weishu.kernelsu.ui.screen.appprofile

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.Natives
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.LocalUiMode
import me.weishu.kernelsu.ui.UiMode
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.navigation3.Route
import me.weishu.kernelsu.ui.util.ensureManagerRegistered
import me.weishu.kernelsu.ui.util.forceStopApp
import me.weishu.kernelsu.ui.util.getSepolicy
import me.weishu.kernelsu.ui.util.launchApp
import me.weishu.kernelsu.ui.util.restartApp
import me.weishu.kernelsu.ui.util.setSepolicy
import me.weishu.kernelsu.ui.viewmodel.SuperUserViewModel
import me.weishu.kernelsu.ui.viewmodel.getTemplateInfoById
import java.util.concurrent.atomic.AtomicLong

@Composable
fun AppProfileScreen(uid: Int) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val materialSnackbarHost = remember { androidx.compose.material3.SnackbarHostState() }
    val viewModel: SuperUserViewModel = viewModel()
    val appGroupState = remember(uid) {
        derivedStateOf {
            viewModel.uiState.value.groupedApps.find { it.uid == uid } ?: SuperUserViewModel.getGroupedApp(uid)
        }
    }
    val appGroup = appGroupState.value
    val primaryAppInfo = appGroup?.primary
    if (primaryAppInfo == null) {
        LaunchedEffect(Unit) {
            navigator.pop()
        }
        return
    }

    val packageName = primaryAppInfo.packageName
    val sharedUserId = remember(uid) {
        primaryAppInfo.packageInfo.sharedUserId
            ?: appGroup.apps.firstOrNull { it.packageInfo.sharedUserId != null }?.packageInfo?.sharedUserId
            ?: ""
    }

    val initialProfile = remember(uid, packageName) {
        (Natives.getAppProfile(packageName, uid) ?: Natives.Profile(packageName, uid)).also {
            if (it.allowSu) {
                it.rules = getSepolicy(packageName)
            }
        }
    }
    var profile by rememberSaveable(uid, packageName) {
        mutableStateOf(initialProfile)
    }
    var persistedProfile by remember(uid, packageName) {
        mutableStateOf(initialProfile)
    }
    val profileWriteMutex = remember(uid, packageName) { Mutex() }
    val profileWriteGeneration = remember(uid, packageName) { AtomicLong(0L) }

    val failToUpdateAppProfile = stringResource(R.string.failed_to_update_app_profile).format(primaryAppInfo.label)
    val failToUpdateSepolicy = stringResource(R.string.failed_to_update_sepolicy).format(primaryAppInfo.label)
    val suNotAllowed = stringResource(R.string.su_not_allowed).format(primaryAppInfo.label)

    fun showMessage(message: String) {
        scope.launch {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    val state = AppProfileUiState(
        uid = uid,
        packageName = packageName,
        profile = profile,
        appGroup = appGroup,
        sharedUserId = sharedUserId,
    )

    val actions = AppProfileActions(
        onBack = dropUnlessResumed { navigator.pop() },
        onLaunchApp = ::launchApp,
        onForceStopApp = ::forceStopApp,
        onRestartApp = ::restartApp,
        onViewTemplate = { templateId ->
            getTemplateInfoById(templateId)?.let { info ->
                navigator.push(Route.TemplateEditor(info, true))
            }
        },
        onManageTemplate = {
            navigator.push(Route.AppProfileTemplate)
        },
        onProfileChange = profileChange@ { updatedProfile ->
            if (updatedProfile.allowSu && uid < 2000 && uid != 1000) {
                showMessage(suNotAllowed)
                return@profileChange
            }

            val generation = profileWriteGeneration.incrementAndGet()
            profile = updatedProfile

            scope.launch {
                profileWriteMutex.withLock {
                    // Drop queued intermediate clicks. A write already in progress is allowed to
                    // finish, then the newest profile is applied after it.
                    if (generation != profileWriteGeneration.get()) return@withLock

                    val desiredRules = updatedProfile.rules.takeIf {
                        updatedProfile.allowSu && !updatedProfile.rootUseDefault
                    }.orEmpty()
                    val sepolicyUpdated = withContext(Dispatchers.IO) {
                        setSepolicy(updatedProfile.name, desiredRules)
                    }
                    if (!sepolicyUpdated) {
                        if (generation == profileWriteGeneration.get()) {
                            profile = persistedProfile
                            showMessage(failToUpdateSepolicy)
                        }
                        return@withLock
                    }

                    val updated = withContext(Dispatchers.IO) {
                        if (Natives.setAppProfile(updatedProfile)) {
                            true
                        } else {
                            ensureManagerRegistered() && Natives.setAppProfile(updatedProfile)
                        }
                    }
                    if (updated) {
                        persistedProfile = updatedProfile
                        if (generation == profileWriteGeneration.get()) {
                            profile = updatedProfile
                        }
                    } else {
                        withContext(Dispatchers.IO) {
                            setSepolicy(persistedProfile.name, persistedProfile.rules.takeIf {
                                persistedProfile.allowSu && !persistedProfile.rootUseDefault
                            }.orEmpty())
                        }
                        if (generation == profileWriteGeneration.get()) {
                            profile = persistedProfile
                            showMessage(failToUpdateAppProfile)
                        }
                    }
                }
            }
        },
    )

    when (LocalUiMode.current) {
        UiMode.Miuix -> AppProfileScreenMiuix(state, actions)
        UiMode.Material -> AppProfileScreenMaterial(state, actions, materialSnackbarHost)
    }
}
