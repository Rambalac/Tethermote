package com.azi.tethermote;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;

public class StateReceiver extends BroadcastReceiver {
    private final BluetoothService service;

    public StateReceiver(BluetoothService service) {
        this.service = service;
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        // Handle receiver
        final String mAction = intent.getAction();

        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean enableOnScreenOn = PreferenceManager.getDefaultSharedPreferences(context)
                        .getBoolean("enable_on_screen_on", false);
                boolean disableOnScreenOff = PreferenceManager.getDefaultSharedPreferences(context)
                        .getBoolean("disable_on_screen_off", false);

                if (mAction.equals(Intent.ACTION_SCREEN_ON) && enableOnScreenOn) {
                    android.net.wifi.WifiManager m = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                    android.net.wifi.SupplicantState s = m.getConnectionInfo().getSupplicantState();
                    NetworkInfo.DetailedState state = WifiInfo.getDetailedStateOf(s);
                    if (state != NetworkInfo.DetailedState.CONNECTED) {
                        int result = WirelessTools.enableRemoteTethering(context, true);
                        TetherRemoteWidget.updateWidgets(context, result);
                    }
                } else if (mAction.equals(Intent.ACTION_SCREEN_OFF) && disableOnScreenOff) {
                    int result = WirelessTools.enableRemoteTethering(context, false);
                    TetherRemoteWidget.updateWidgets(context, result);
                } else if (mAction.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                    final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                    switch (state) {
                        case BluetoothAdapter.STATE_ON:
                            service.startThread();
                            break;
                    }

                }
            }
        }).start();
    }
}
