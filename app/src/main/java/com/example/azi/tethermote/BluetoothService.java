package com.example.azi.tethermote;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.IBinder;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.UUID;

public class BluetoothService extends Service {
    private final BluetoothAdapter btAdapter;
    private final String appName = getResources().getString(R.string.app_name);
    private final UUID serviceUuid = UUID.fromString(getResources().getString(R.string.service_uuid));

    public BluetoothService() {
        btAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        try {
            BluetoothServerSocket myServerSocket = btAdapter
                    .listenUsingRfcommWithServiceRecord(appName, serviceUuid);
            while (true) {
                BluetoothSocket socket = myServerSocket.accept();
                try {
                    InputStream stream = socket.getInputStream();
                    try {
                        int b = stream.read();
                        if (b == 0) {
                            EnableTethering(false);
                        } else if (b == 1) {
                            EnableTethering(true);
                        }
                    } finally {
                        stream.close();
                    }
                } finally {
                    socket.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return START_STICKY;
    }

    private boolean EnableTethering(boolean enable) {
        WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);

        Method[] methods = wifiManager.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (method.getName().equals("setWifiApEnabled")) {
                try {
                    method.invoke(wifiManager, null, enable);
                    return true;
                } catch (Exception ex) {
                }
            }
        }
        return false;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
