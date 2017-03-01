package com.azi.tethermote;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Arrays;
import java.util.HashSet;

public class SystemReceiver extends BroadcastReceiver {
    private static final HashSet<String> allowedActions = new HashSet<>(Arrays.asList(
            "android.bluetooth.device.action.ACL_CONNECTED",
            "android.intent.action.MY_PACKAGE_REPLACED",
            "android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED",
            "android.intent.action.BOOT_COMPLETED"
    ));

    public SystemReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(getClass().getSimpleName(), "Starting service");
        try {
            String action = intent.getAction();
            if (!allowedActions.contains(action)) {
                Log.e(getClass().getSimpleName(), "Wrong action " + action);
            }

            Boolean enableTetheringService = PreferenceManager.getDefaultSharedPreferences(context)
                    .getBoolean("enable_tethering", false);

            if (enableTetheringService) {

                Intent startServiceIntent = new Intent(context, BluetoothService.class);
                context.startService(startServiceIntent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
