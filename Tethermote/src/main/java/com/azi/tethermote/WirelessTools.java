package com.azi.tethermote;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

class WirelessTools {
    public static final UUID SERVICE_UUID = UUID.fromString("5dc6ece2-3e0d-4425-ac00-e444be6b56cb");

    public final static String tethermotePackageName = "com.azi.tethermote";

    public final static int TETHERING_ERROR = 3;
    public final static int TETHERING_DISABLED = 0;
    public final static int TETHERING_ENABLED = 1;
    public final static int TETHERING_STATE = 2;

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
        for (int tryout = 3; tryout > 0; tryout--)
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

    public static boolean enableLocalTethering(Context context, boolean enable) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        Method[] methods = wifiManager.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (method.getName().equals("setWifiApEnabled")) {
                try {
                    method.invoke(wifiManager, null, enable);
                    return true;
                } catch (Exception e) {
                    if (e.getCause() instanceof SecurityException) {
                        showToast(context, context.getString(R.string.write_settings_error), Toast.LENGTH_LONG);
                    } else {
                        //showToast(context, "enableLocalTethering Error " + e.getMessage(), Toast.LENGTH_SHORT);
                        ((TethermoteApp) context.getApplicationContext()).sendException(e);
                    }
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    public static boolean getLocalTetheringState(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        Method[] wmMethods = wifiManager.getClass().getDeclaredMethods();
        for (Method method : wmMethods) {
            if (method.getName().equals("isWifiApEnabled")) {
                try {
                    return (boolean) method.invoke(wifiManager);
                } catch (Exception e) {
                    //showToast(context, "getLocalTetheringState Error " + e.getMessage(), Toast.LENGTH_SHORT);
                    ((TethermoteApp) context.getApplicationContext()).sendException(e);
                    e.printStackTrace();
                }
            }
        }
        return false;
    }
}
