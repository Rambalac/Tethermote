package com.azi.tethermote;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;

public class StateReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, final Intent intent) {
        // Handle receiver
        final String mAction = intent.getAction();

        new Thread(new Runnable() {
            @Override
            public void run() {
                SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
                boolean enableOnScreenOn = defaultSharedPreferences
                        .getBoolean("enable_on_screen_on", false);
                boolean disableOnScreenOff = defaultSharedPreferences
                        .getBoolean("disable_on_screen_off", false);
                boolean notification = defaultSharedPreferences
                        .getBoolean("show_switch_notification", false);


                if (mAction.equals(Intent.ACTION_SCREEN_ON) && enableOnScreenOn) {
                    android.net.wifi.WifiManager m = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                    android.net.wifi.SupplicantState s = m.getConnectionInfo().getSupplicantState();
                    NetworkInfo.DetailedState state = WifiInfo.getDetailedStateOf(s);
                    if (state != NetworkInfo.DetailedState.CONNECTED) {
                        int result = WirelessTools.enableRemoteTethering(context, true);
                        SwitchNotification.Check(context);
                        TetherRemoteWidget.updateWidgets(context, result);
                    }
                } else if (mAction.equals(Intent.ACTION_SCREEN_OFF) && disableOnScreenOff) {
                    int result = WirelessTools.enableRemoteTethering(context, false);
                    SwitchNotification.Check(context, result != WirelessTools.TETHERING_ENABLED);
                    TetherRemoteWidget.updateWidgets(context, result);
                }
            }
        }).start();
    }
}
