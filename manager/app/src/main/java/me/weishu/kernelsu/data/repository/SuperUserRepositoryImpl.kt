package me.weishu.kernelsu.data.repository

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Handler
import android.os.IBinder
import android.os.Build
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ipc.RootService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import me.weishu.kernelsu.IKsuInterface
import me.weishu.kernelsu.Natives
import me.weishu.kernelsu.data.model.AppInfo
import me.weishu.kernelsu.ksuApp
import me.weishu.kernelsu.ui.KsuService
import me.weishu.kernelsu.ui.util.KsuCli
import kotlin.coroutines.resume

class SuperUserRepositoryImpl : SuperUserRepository {

    companion object {
        private const val TAG = "SuperUserRepository"
        private const val ROOT_APP_LIST_TIMEOUT_MILLIS = 8_000L
    }

    override suspend fun getAppList(): Result<Pair<List<AppInfo>, List<Int>>> = withContext(Dispatchers.IO) {
        runCatching {
            val start = SystemClock.elapsedRealtime()
            val rootResult = runCatching {
                withTimeout(ROOT_APP_LIST_TIMEOUT_MILLIS) {
                    getAppListFromKsuService()
                }
            }.onFailure {
                Log.w(TAG, "root package query failed, falling back to current user", it)
            }.getOrNull()

            if (rootResult != null && rootResult.first.isNotEmpty()) {
                return@runCatching rootResult
            }

            val allowedUids = getAllowListUids()
            val localPackages = getInstalledPackagesLocal(0)
            val localApps = localPackages.mapNotNull { packageInfo ->
                parseAppInfo(packageInfo, allowedUids)
            }
            Log.i(
                TAG,
                "load local fallback cost: ${SystemClock.elapsedRealtime() - start}, " +
                    "packages: ${localApps.size}, allowed: ${allowedUids.size}"
            )
            localApps to listOf(0)
        }
    }

    private suspend fun getAppListFromKsuService(): Pair<List<AppInfo>, List<Int>> {
        val result = connectKsuService {
            Log.w(TAG, "KsuService disconnected")
        }

        var currentBinder = result.first
        var currentConnection = result.second

        try {
            suspend fun reconnect(): IKsuInterface {
                withContext(Dispatchers.Main) {
                    RootService.unbind(currentConnection)
                }
                val retry = connectKsuService { Log.w(TAG, "KsuService disconnected") }
                currentBinder = retry.first
                currentConnection = retry.second
                return IKsuInterface.Stub.asInterface(currentBinder)
            }

            val start = SystemClock.elapsedRealtime()

            var iface = IKsuInterface.Stub.asInterface(currentBinder)
            val idsArray = try {
                iface.userIds
            } catch (_: Exception) {
                iface = reconnect()
                iface.userIds
            }

            val slice = try {
                iface.getPackages(0)
            } catch (_: Exception) {
                iface = reconnect()
                iface.getPackages(0)
            }

            val packages = slice.list.orEmpty()
            val allowedUids = getAllowListUids()
            val newApps = packages.mapNotNull { packageInfo ->
                parseAppInfo(packageInfo, allowedUids)
            }

            Log.i(TAG, "load root service cost: ${SystemClock.elapsedRealtime() - start}, packages: ${newApps.size}")
            return newApps to idsArray.toList()
        } finally {
            withContext(Dispatchers.Main) {
                RootService.unbind(currentConnection)
            }
        }
    }

    private fun getInstalledPackagesLocal(flags: Int): List<PackageInfo> {
        return runCatching {
            val pm = ksuApp.packageManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(flags.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledPackages(flags)
            }
        }.getOrElse {
            Log.w(TAG, "local package query failed", it)
            emptyList()
        }
    }

    override suspend fun refreshProfiles(currentApps: List<AppInfo>): Result<List<AppInfo>> = withContext(Dispatchers.IO) {
        runCatching {
            if (currentApps.isEmpty()) return@runCatching emptyList()

            val allowedUids = getAllowListUids()
            currentApps.map { app ->
                val profile = runCatching<Natives.Profile?> {
                    Natives.getAppProfile(app.packageName, app.uid)
                }.getOrElse {
                    Log.w(TAG, "get profile failed for ${app.packageName}", it)
                    null
                }
                app.copy(profile = profile, isGrantedByKernel = app.uid in allowedUids)
            }
        }
    }

    private fun parseAppInfo(packageInfo: PackageInfo, allowedUids: Set<Int>): AppInfo? {
        return runCatching {
            val appInfo = packageInfo.applicationInfo ?: return null
            if (appInfo.isResourceOverlay) return null

            val pm = ksuApp.packageManager
            val uid = appInfo.uid
            val profile = runCatching<Natives.Profile?> {
                Natives.getAppProfile(packageInfo.packageName, uid)
            }.getOrElse {
                Log.w(TAG, "get profile failed for ${packageInfo.packageName}", it)
                null
            }

            AppInfo(
                label = runCatching { appInfo.loadLabel(pm).toString() }
                    .getOrDefault(packageInfo.packageName),
                packageInfo = packageInfo,
                profile = profile,
                isGrantedByKernel = uid in allowedUids,
            )
        }.getOrElse {
            Log.w(TAG, "skip package ${packageInfo.packageName}", it)
            null
        }
    }

    private fun getAllowListUids(): Set<Int> {
        return runCatching {
            Natives.getAllowList().toSet()
        }.getOrElse {
            Log.w(TAG, "get allow list failed", it)
            emptySet()
        }
    }

    private suspend inline fun connectKsuService(
        crossinline onDisconnect: () -> Unit = {}
    ): Pair<IBinder, ServiceConnection> = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { cont ->
            val connection = object : ServiceConnection {
                override fun onServiceDisconnected(name: ComponentName?) {
                    onDisconnect()
                }

                override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                    if (cont.isActive) {
                        if (binder != null) {
                            cont.resume(binder to this)
                        } else {
                            cont.cancel(IllegalStateException("KsuService returned null binder"))
                        }
                    }
                }
            }

            cont.invokeOnCancellation {
                if (Looper.myLooper() == Looper.getMainLooper()) {
                    RootService.unbind(connection)
                } else {
                    Handler(Looper.getMainLooper()).post {
                        RootService.unbind(connection)
                    }
                }
            }

            val intent = Intent(ksuApp, KsuService::class.java)

            val task = RootService.bindOrTask(
                intent,
                Shell.EXECUTOR,
                connection,
            )
            val shell = KsuCli.SHELL
            task?.let { shell.submitTask(it) }
        }
    }
}
