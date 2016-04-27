package com.azi.tethermote;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.annotation.Nullable;

public class BluetoothService extends Service {
    public static final String Name = "com.azi.tethermote.BLUETOOTH_SERVICE";
    private static boolean running;
    private static BluetoothThread mainThread;

    private final BroadcastReceiver mIntentReceiver;

    public BluetoothService() {
        mIntentReceiver = new StateReceiver(this);
    }

    @Override
    public void onCreate() {
        IntentFilter stateFilter = new IntentFilter();
        stateFilter.addAction(Intent.ACTION_SCREEN_ON);
        stateFilter.addAction(Intent.ACTION_SCREEN_OFF);
        stateFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mIntentReceiver, stateFilter);

        startThread();
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