package com.azi.tethermote;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

public class BluetoothService extends Service {
    private static BluetoothThread mainThread;

    @Override
    public void onCreate() {
    }

    public void startThread() {
        if (mainThread != null) {
            mainThread.cancel();
        }
        mainThread = new BluetoothThread(this);
        mainThread.setName("BluetoothServer");
        mainThread.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startThread();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mainThread != null) {
            mainThread.cancel();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onThreadStopped() {

    }
}