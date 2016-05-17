package com.azi.tethermote;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

public class BluetoothService extends Service {
    private static BluetoothThread mainThread;

    private final BroadcastReceiver mIntentReceiver;
    private final SharedPreferences.OnSharedPreferenceChangeListener sharedPreferecesListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    SwitchNotification.Check(BluetoothService.this);
                }
            }).start();
        }
    };

    public BluetoothService() {
        mIntentReceiver = new StateReceiver(this);
    }

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
        IntentFilter stateFilter = new IntentFilter();
        stateFilter.addAction(Intent.ACTION_SCREEN_ON);
        stateFilter.addAction(Intent.ACTION_SCREEN_OFF);
        stateFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mIntentReceiver, stateFilter);

        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(sharedPreferecesListener);

        SwitchNotification.Check(this);

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