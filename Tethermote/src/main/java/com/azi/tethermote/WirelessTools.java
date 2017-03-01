package com.azi.tethermote;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

class WirelessTools {
    public static final UUID SERVICE_UUID = UUID.fromString("5dc6ece2-3e0d-4425-ac00-e444be6b56cb");
    public final static int TETHERING_ERROR = 3;
    public final static int TETHERING_DISABLED = 0;
    public final static int TETHERING_ENABLED = 1;
    public final static int TETHERING_STATE = 2;
    private final static String tethermotePackageName = "com.azi.tethermote";
    private static Handler handler = new Handler(Looper.getMainLooper());

    public static int enableRemoteTethering(final Context context, boolean enable) {
        String address = getAddress(context);
        int result;
        if (address.isEmpty()) {
            result = TETHERING_STATE;
        } else if (address.equals(".")) {
            result = (enableLocalTethering(context, enable) && enable) ? TETHERING_ENABLED : TETHERING_DISABLED;
        } else {
            result = sendRemoteTetherState(context, address, enable ? TETHERING_ENABLED : TETHERING_DISABLED);
            if (result == TETHERING_ENABLED) {
                handler.postDelayed(new WiFiScanner(context, 15, 1000), 10);
            }
        }

        SwitchNotification.Check(context, result != TETHERING_ENABLED);

        return result;
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

    private static int sendRemoteTetherState(final Context context, String address, int state) {
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null) {
            return TETHERING_ERROR;
        }
        if (!btAdapter.isEnabled()) {
            enableBluetooth(context);
        }

        btAdapter.cancelDiscovery();

        BluetoothDevice device = btAdapter.getRemoteDevice(address);
        if (device == null) {
            //showToast(context, context.getString(R.string.bluetooth_device_not_found), Toast.LENGTH_LONG);
            return TETHERING_ERROR;
        }
        String deviceName = device.getName();
        //String deviceErrorMessage = context.getString(R.string.bluetooth_device_not_accessible, deviceName);
        for (int tryout = 10; tryout > 0; tryout--)
            try {
                BluetoothSocket clientSocket = device.createRfcommSocketToServiceRecord(SERVICE_UUID);
                if (clientSocket == null) {
                    //showToast(context, deviceErrorMessage, Toast.LENGTH_LONG);
                    return TETHERING_ERROR;
                }
                startSocketTimeout(clientSocket, 5000);
                clientSocket.connect();
                try {
                    OutputStream outStream = clientSocket.getOutputStream();
                    if (outStream == null) {
                        //showToast(context, deviceErrorMessage, Toast.LENGTH_LONG);
                        return TETHERING_ERROR;
                    }
                    outStream.write(state);
                    outStream.flush();

                    InputStream inStream = clientSocket.getInputStream();

                    return inStream.read();
                } finally {
                    clientSocket.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (tryout != 1) try {
                    Thread.sleep(500);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        return TETHERING_ERROR;
    }

    private static void enableBluetooth(Context context) {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        enableBtIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(enableBtIntent);
    }

    private static void WiFiRescan(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        wifiManager.startScan();
    }

    private static String getAddress(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString("remote_device", "");
    }

    private static boolean getWiFiDisablePref(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean("disable_wifi", true);
    }

    private static void setWiFiDisablePref(Context context, boolean val) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit().putBoolean("disable_wifi", val).commit();
    }

    public static int getRemoteTetherState(Context context) {
        String address = getAddress(context);
        if (address.isEmpty()) {
            return TETHERING_STATE;
        } else if (address.equals(".")) {
            return getLocalTetheringState(context) ? TETHERING_ENABLED : TETHERING_DISABLED;
        }

        return sendRemoteTetherState(context, address, TETHERING_STATE);
    }

    public static void checkWriteSettingsPermission(final Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(context)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AlertTheme);
                AlertDialog alert = builder.setMessage(R.string.need_write_settings)
                        .setPositiveButton(R.string.open_system_settings, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                @SuppressLint("InlinedApi") Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + tethermotePackageName));
                                context.startActivity(intent);
                            }
                        }).create();
//                alert.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                alert.show();
            }
        }
    }

    private static boolean WiFiConnected(Context context) {
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            for (Network net : connManager.getAllNetworks()) {
                NetworkInfo ni = connManager.getNetworkInfo(net);
                if (ni.getType() == ConnectivityManager.TYPE_WIFI) {
                    mWifi = ni;
                    break;
                }
            }
        } else {
            mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        }
        if (mWifi == null) return false;
        return mWifi.isConnected();
    }

    private static void setWifiApEnabled(Context context, boolean enable) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        final Method method = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
        method.invoke(wifiManager, null, enable);
    }

    public static boolean enableLocalTethering(final Context context, final boolean enableAp) {
        final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        try {
            boolean wifidisable = getWiFiDisablePref(context);
            if (enableAp && wifidisable) {
                wifiManager.setWifiEnabled(false);
            }

            setWifiApEnabled(context, enableAp);

            handler.removeCallbacksAndMessages(null);
            if (!enableAp && wifidisable) {
                wifiManager.setWifiEnabled(true);
            } else if (enableAp && !wifidisable) {
                handler.postDelayed(new StateChecker(context, 20, 500), 500);
            }
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        } catch (Exception e) {
            if (e.getCause() instanceof SecurityException) {
                showToast(context, context.getString(R.string.write_settings_error), Toast.LENGTH_LONG);
            } else {
                showToast(context, "enableLocalTethering Error " + e.getMessage(), Toast.LENGTH_SHORT);
                ((TethermoteApp) context.getApplicationContext()).sendException(e);
            }
            e.printStackTrace();
        }
        return false;
    }

    public static boolean getLocalTetheringState(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        try {
            Method method = wifiManager.getClass().getMethod("isWifiApEnabled");
            return (boolean) method.invoke(wifiManager);
        } catch (Exception e) {
            //showToast(context, "getLocalTetheringState Error " + e.getMessage(), Toast.LENGTH_SHORT);
            ((TethermoteApp) context.getApplicationContext()).sendException(e);
            e.printStackTrace();
        }
        return false;
    }

    static class WiFiScanner implements Runnable {
        final Context context;
        final int delay;
        int counter;

        WiFiScanner(Context context, int counter, int delay) {
            this.context = context;
            this.counter = counter;
            this.delay = delay;
        }

        @Override
        public void run() {
            boolean state = getLocalTetheringState(context);
            if (!state) {
                if (WiFiConnected(context)) return;

                WiFiRescan(context);

                counter--;
                if (counter > 0) {
                    handler.postDelayed(this, delay);
                }
            }
        }

    }

    static class StateChecker implements Runnable {
        final Context context;
        final int delay;
        int counter;

        StateChecker(Context context, int counter, int delay) {
            this.context = context;
            this.counter = counter;
            this.delay = delay;
        }

        @Override
        public void run() {
            boolean state = getLocalTetheringState(context);
            if (!state) {
                counter--;
                if (counter > 0) {
                    handler.postDelayed(this, delay);
                } else {
                    if (!WiFiConnected(context)) {
                        showToast(context, context.getString(R.string.wifi_need_disable), Toast.LENGTH_LONG);

                        final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                        wifiManager.setWifiEnabled(false);
                        try {
                            setWifiApEnabled(context, true);
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        } catch (InvocationTargetException e) {
                            e.printStackTrace();
                        } catch (NoSuchMethodException e) {
                            e.printStackTrace();
                        }
                        setWiFiDisablePref(context, true);
                    } else {
                        showToast(context, context.getString(R.string.wifi_suggest_disable), Toast.LENGTH_LONG);
                    }
                }
            }
        }
    }
}
