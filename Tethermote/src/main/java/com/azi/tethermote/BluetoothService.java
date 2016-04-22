package com.azi.tethermote;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

public class BluetoothService extends Service {
    public static final UUID SERVICE_UUID = UUID.fromString("5dc6ece2-3e0d-4425-ac00-e444be6b56cb");
    static boolean running;
    private final BluetoothAdapter btAdapter;
    private final String appName = "Tethermote";
    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Handle reciever
            String mAction = intent.getAction();

            boolean enableOnScreenOn = PreferenceManager.getDefaultSharedPreferences(context)
                    .getBoolean("enable_on_screen_on", false);
            boolean disableOnScreenOff = PreferenceManager.getDefaultSharedPreferences(context)
                    .getBoolean("disable_on_screen_off", false);

            int action = -1;
            if (mAction.equals(Intent.ACTION_SCREEN_ON) && enableOnScreenOn) {
                android.net.wifi.WifiManager m = (WifiManager) getSystemService(WIFI_SERVICE);
                android.net.wifi.SupplicantState s = m.getConnectionInfo().getSupplicantState();
                NetworkInfo.DetailedState state = WifiInfo.getDetailedStateOf(s);
                if (state == NetworkInfo.DetailedState.CONNECTED) {
                    action = 1;
                }
            } else if (mAction.equals(Intent.ACTION_SCREEN_OFF) && disableOnScreenOff) {
                action = 0;
            }
            if (action != -1) {
                int newstate = sendRemoteTetherState(context, action);
                updateWidgets(context, newstate);
            }
        }
    };

    public BluetoothService() {
        btAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public static void showToast(final Context context, final String s, final int length) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, s, length).show();
            }
        });


    }

    public static void startSocketTimeout(final BluetoothSocket socket, final int time) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(time);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static int sendRemoteTetherState(final Context context, int state) {
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        btAdapter.cancelDiscovery();
//        if (!btAdapter.isEnabled()) {
//            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
//        }

        String address = PreferenceManager.getDefaultSharedPreferences(context)
                .getString("remote_device", "");
        if (address.isEmpty()) return 2;

        BluetoothDevice device = btAdapter.getRemoteDevice(address);
        try {
            BluetoothSocket clientSocket = device.createRfcommSocketToServiceRecord(BluetoothService.SERVICE_UUID);
            startSocketTimeout(clientSocket, 5000);
            clientSocket.connect();
            try {
                OutputStream outstream = clientSocket.getOutputStream();
                if (outstream == null) {
                    return 2;
                }
                outstream.write(state);
                outstream.flush();

                InputStream instream = clientSocket.getInputStream();
                int result = instream.read();

                if (result == 1) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            for (int i = 0; i < 5; i++) {
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                WiFiRescan(context);
                            }
                        }
                    }).start();
                }
                return result;
            } finally {
                clientSocket.close();
            }
        } catch (Exception e) {
            BluetoothService.showToast(context, "Send failed: " + e.getMessage(), Toast.LENGTH_LONG);
            e.printStackTrace();
            return 2;
        }
    }

    private static void WiFiRescan(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(WIFI_SERVICE);
        wifiManager.startScan();
    }

    public static int getRemoteTetherState(Context context) {
        return sendRemoteTetherState(context, 2);
    }

    private void updateWidgets(Context context, int newstate) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        final int[] appWidgetIds = manager.getAppWidgetIds(new ComponentName(context, TetherRemoteWidget.class));

        for (int i = 0; i < appWidgetIds.length; ++i) {
            TetherRemoteWidget.updateAppWidget(context, manager, appWidgetIds[i], newstate);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        IntentFilter screenStateFilter = new IntentFilter();
        screenStateFilter.addAction(Intent.ACTION_SCREEN_ON);
        screenStateFilter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mIntentReceiver, screenStateFilter);

        if (running) return START_STICKY;
        showToast("Started", Toast.LENGTH_SHORT);
        running = true;
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        try {
                            BluetoothServerSocket myServerSocket = btAdapter
                                    .listenUsingRfcommWithServiceRecord(appName, SERVICE_UUID);
                            while (true) {
                                try {
                                    BluetoothSocket socket = myServerSocket.accept();

                                    try {
                                        startSocketTimeout(socket, 5000);
                                        showToast("Got clientSocket " + socket.getRemoteDevice().getName(), Toast.LENGTH_SHORT);
                                        InputStream instream = socket.getInputStream();
                                        OutputStream outstream = socket.getOutputStream();

                                        int b = instream.read();
                                        if (b == 0) {
                                            enableTethering(false);
                                            outstream.write(0);
                                        } else if (b == 1) {
                                            boolean success = enableTethering(true);
                                            outstream.write(success ? 1 : 0);
                                        } else if (b == 2) {
                                            boolean state = getTetheringState();
                                            outstream.write(state ? 1 : 0);
                                        }
                                        outstream.flush();

                                    } finally {
                                        socket.close();
                                    }
                                } catch (Exception e) {
                                    showToast("Error " + e.getMessage(), Toast.LENGTH_SHORT);
                                    e.printStackTrace();
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                } finally {
                    running = false;
                }
            }
        });
        thread.start();

        return START_STICKY;
    }

    private void showToast(final String s, final int length) {
        BluetoothService.showToast(getApplicationContext(), s, length);
    }

    private boolean getTetheringState() {
        showToast("Getting state ", Toast.LENGTH_SHORT);

        WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        Method[] wmMethods = wifiManager.getClass().getDeclaredMethods();
        for (Method method : wmMethods) {
            if (method.getName().equals("isWifiApEnabled")) {
                try {
                    return (boolean) method.invoke(wifiManager);
                } catch (Exception e) {
                    showToast("getTetheringState Error " + e.getMessage(), Toast.LENGTH_SHORT);
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    private boolean enableTethering(boolean enable) {
        showToast("Enabling " + enable, Toast.LENGTH_SHORT);

        WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);

        Method[] methods = wifiManager.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (method.getName().equals("setWifiApEnabled")) {
                try {
                    method.invoke(wifiManager, null, enable);
                    return true;
                } catch (Exception e) {
                    showToast("enableTethering Error " + e.getMessage(), Toast.LENGTH_SHORT);
                    e.printStackTrace();
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
