package me.weishu.kernelsu.magica;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import androidx.annotation.Nullable;

public class MagicaService extends Service {
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // AppZygotePreload has already forked the detached ksud process before this
        // callback. Do not keep an empty service alive, otherwise a failed attempt
        // cannot create a fresh app zygote on the next retry.
        stopSelf(startId);
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new Binder();
    }
}
