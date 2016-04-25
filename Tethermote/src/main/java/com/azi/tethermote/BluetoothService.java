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
    public final static int TETHERING_DISABLED = 0;
    public final static int TETHERING_ENABLED = 1;
    public final static int TETHERING_STATE = 2;

    private static final UUID SERVICE_UUID = UUID.fromString("5dc6ece2-3e0d-4425-ac00-e444be6b56cb");
    private static boolean running;
    private static Thread mainThread;
    private final BluetoothAdapter btAdapter;
    private final String appName = "Tethermote";
    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Handle receiver
            String mAction = intent.getAction();

            boolean enableOnScreenOn = PreferenceManager.getDefaultSharedPreferences(context)
                    .getBoolean("enable_on_screen_on", false);
            boolean disableOnScreenOff = PreferenceManager.getDefaultSharedPreferences(context)
                    .getBoolean("disable_on_screen_off", false);

            if (mAction.equals(Intent.ACTION_SCREEN_ON) && enableOnScreenOn) {
                android.net.wifi.WifiManager m = (WifiManager) getSystemService(WIFI_SERVICE);
                android.net.wifi.SupplicantState s = m.getConnectionInfo().getSupplicantState();
                NetworkInfo.DetailedState state = WifiInfo.getDetailedStateOf(s);
                if (state == NetworkInfo.DetailedState.CONNECTED) {
                    EnableRemoteTethering(context, true);
                }
            } else if (mAction.equals(Intent.ACTION_SCREEN_OFF) && disableOnScreenOff) {
                EnableRemoteTethering(context, false);
            }
        }
    };

    public BluetoothService() {
        btAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public static int EnableRemoteTethering(Context context, boolean enable) {
        String address = getAddress(context);
        int result;
        if (address.isEmpty()) {
            result = TETHERING_STATE;
        } else if (address.equals(".")) {
            result = enableTethering(context, enable) ? TETHERING_ENABLED : TETHERING_DISABLED;
        } else {
            result = sendRemoteTetherState(context, address, enable ? TETHERING_ENABLED : TETHERING_DISABLED);
        }

        updateWidgets(context, result);
        return result;
    }

    private static void showToast(final Context context, final String s, final int length) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, s, length).show();
            }
        });


    }

    private static void startSocketTimeout(final BluetoothSocket socket, final int time) {
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

    private static int sendRemoteTetherState(final Context context, String address, int state) {
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null) {
            return TETHERING_STATE;
        }
        if (!btAdapter.isEnabled()) {
            enableBluetooth(context);
        }

        btAdapter.cancelDiscovery();

        BluetoothDevice device = btAdapter.getRemoteDevice(address);
        if (device == null) {
            showToast(context, context.getString(R.string.bluetooth_device_not_found), Toast.LENGTH_LONG);
            return TETHERING_STATE;
        }
        String deviceName = device.getName();

        try {
            BluetoothSocket clientSocket = device.createRfcommSocketToServiceRecord(BluetoothService.SERVICE_UUID);
            if (clientSocket == null) {
                showToast(context, context.getString(R.string.bluetooth_device_not_accessible) + deviceName, Toast.LENGTH_LONG);
                return TETHERING_STATE;
            }
            startSocketTimeout(clientSocket, 5000);
            clientSocket.connect();
            try {
                OutputStream outStream = clientSocket.getOutputStream();
                if (outStream == null) {
                    showToast(context, context.getString(R.string.bluetooth_device_not_accessible) + deviceName, Toast.LENGTH_LONG);
                    return TETHERING_STATE;
                }
                outStream.write(state);
                outStream.flush();

                InputStream inStream = clientSocket.getInputStream();
                int result = inStream.read();

                if (result == TETHERING_ENABLED) {
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
        } catch (IOException ex) {
            showToast(context, context.getString(R.string.bluetooth_device_not_accessible) + deviceName, Toast.LENGTH_LONG);
            ex.printStackTrace();
            return TETHERING_STATE;
        } catch (Exception e) {
            BluetoothService.showToast(context, "Send failed: " + e.getMessage(), Toast.LENGTH_LONG);
            e.printStackTrace();
            return TETHERING_STATE;
        }
    }

    private static void enableBluetooth(Context context) {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        enableBtIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(enableBtIntent);
    }

    private static void WiFiRescan(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(WIFI_SERVICE);
        wifiManager.startScan();
    }

    private static String getAddress(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString("remote_device", "");
    }

    public static int getRemoteTetherState(Context context) {
        String address = getAddress(context);
        if (address.isEmpty()) {
            return TETHERING_STATE;
        } else if (address.equals(".")) {
            return getTetheringState(context) ? TETHERING_ENABLED : TETHERING_DISABLED;
        }

        return sendRemoteTetherState(context, address, TETHERING_STATE);
    }

    private static void updateWidgets(Context context, int newState) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        final int[] appWidgetIds = manager.getAppWidgetIds(new ComponentName(context, TetherRemoteWidget.class));

        for (int appWidgetId : appWidgetIds) {
            TetherRemoteWidget.updateAppWidget(context, manager, appWidgetId, newState);
        }
    }

    private static boolean getTetheringState(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(WIFI_SERVICE);
        Method[] wmMethods = wifiManager.getClass().getDeclaredMethods();
        for (Method method : wmMethods) {
            if (method.getName().equals("isWifiApEnabled")) {
                try {
                    return (boolean) method.invoke(wifiManager);
                } catch (Exception e) {
                    showToast(context, "getTetheringState Error " + e.getMessage(), Toast.LENGTH_SHORT);
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    private static boolean enableTethering(Context context, boolean enable) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(WIFI_SERVICE);

        Method[] methods = wifiManager.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (method.getName().equals("setWifiApEnabled")) {
                try {
                    method.invoke(wifiManager, null, enable);
                    return true;
                } catch (Exception e) {
                    showToast(context, "enableTethering Error " + e.getMessage(), Toast.LENGTH_SHORT);
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (btAdapter == null) {
            return START_NOT_STICKY;
        }
        IntentFilter screenStateFilter = new IntentFilter();
        screenStateFilter.addAction(Intent.ACTION_SCREEN_ON);
        screenStateFilter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mIntentReceiver, screenStateFilter);

        if (running && mainThread != null && mainThread.isAlive()) {
            return START_STICKY;
        }

        running = true;
        mainThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        try {
                            if (!btAdapter.isEnabled()) {
                                try {
                                    Thread.sleep(10000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                continue;
                            }

                            BluetoothServerSocket myServerSocket = btAdapter
                                    .listenUsingRfcommWithServiceRecord(appName, SERVICE_UUID);
                            while (true) {
                                try {
                                    BluetoothSocket socket = myServerSocket.accept();

                                    try {
                                        startSocketTimeout(socket, 5000);
                                        InputStream inStream = socket.getInputStream();
                                        OutputStream outStream = socket.getOutputStream();

                                        int b = inStream.read();
                                        if (b == TETHERING_DISABLED) {
                                            enableTethering(BluetoothService.this, false);
                                            outStream.write(TETHERING_DISABLED);
                                        } else if (b == TETHERING_ENABLED) {
                                            boolean success = enableTethering(BluetoothService.this, true);
                                            outStream.write(success ? TETHERING_ENABLED : TETHERING_DISABLED);
                                        } else if (b == TETHERING_STATE) {
                                            boolean state = getTetheringState(BluetoothService.this);
                                            outStream.write(state ? TETHERING_ENABLED : TETHERING_DISABLED);
                                        }
                                        outStream.flush();

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
        mainThread.start();

        return START_STICKY;
    }

    private void showToast(final String s, final int length) {
        BluetoothService.showToast(getApplicationContext(), s, length);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
