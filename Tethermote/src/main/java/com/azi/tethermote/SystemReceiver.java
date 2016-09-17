package com.azi.tethermote;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.util.Log;

public class SystemReceiver extends BroadcastReceiver {
    public SystemReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(getClass().getSimpleName(), "Starting service");
        try {
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
