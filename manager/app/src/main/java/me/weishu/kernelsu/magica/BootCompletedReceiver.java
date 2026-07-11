package me.weishu.kernelsu.magica;

import static me.weishu.kernelsu.magica.AppZygotePreload.TAG;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.UserManager;
import android.util.Log;

import me.weishu.kernelsu.ui.util.KsuCliKt;

public class BootCompletedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }
        var action = intent.getAction();
        if (!Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            return;
        }

        var pendingResult = goAsync();
        var applicationContext = context.getApplicationContext();
        Thread worker = new Thread(() -> {
            try {
                if (KsuCliKt.rootAvailable()) return;
                var userContext = applicationContext;
                var userManager = applicationContext.getSystemService(UserManager.class);
                if (userManager != null && !userManager.isUserUnlocked()) {
                    userContext = applicationContext.createDeviceProtectedStorageContext();
                }
                userContext.startService(new Intent(userContext, MagicaService.class));
                Log.i(TAG, "MagicaService started from boot action: " + action);
            } catch (Throwable e) {
                Log.e(TAG, "Failed to start MagicaService from boot action: " + action, e);
            } finally {
                pendingResult.finish();
            }
        }, "ApkeSU-Magica-Boot");
        worker.start();
    }
}
