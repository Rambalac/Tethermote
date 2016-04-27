package com.azi.tethermote;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class BluetoothThread extends Thread {
    private static final String appName = "Tethermote";
    private final BluetoothService context;
    private BluetoothServerSocket myServerSocket;


    public BluetoothThread(BluetoothService context) {
        this.context = context;
    }

    @Override
    public void run() {
        //WirelessTools.showToast(context, "Service starting", Toast.LENGTH_LONG);
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        try {
            if (btAdapter == null || !btAdapter.isEnabled()) {
                return;
            }
            if (myServerSocket != null) {
                try {
                    myServerSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            myServerSocket = btAdapter
                    .listenUsingRfcommWithServiceRecord(appName, WirelessTools.SERVICE_UUID);
            while (true) {
                BluetoothSocket socket = myServerSocket.accept();
                String deviceName = socket.getRemoteDevice().getName();
                try {
                    WirelessTools.startSocketTimeout(socket, 5000);
                    InputStream inStream = socket.getInputStream();
                    OutputStream outStream = socket.getOutputStream();

                    int b = inStream.read();
                    if (b == WirelessTools.TETHERING_DISABLED) {
                        WirelessTools.showToast(context, context.getString(R.string.remote_tethering_disabling) + deviceName, Toast.LENGTH_SHORT);
                        WirelessTools.enableLocalTethering(context, false);
                        outStream.write(WirelessTools.TETHERING_DISABLED);
                    } else if (b == WirelessTools.TETHERING_ENABLED) {
                        WirelessTools.showToast(context, context.getString(R.string.remote_tethering_enabling) + deviceName, Toast.LENGTH_SHORT);
                        boolean success = WirelessTools.enableLocalTethering(context, true);
                        outStream.write(success ? WirelessTools.TETHERING_ENABLED : WirelessTools.TETHERING_DISABLED);
                    } else if (b == WirelessTools.TETHERING_STATE) {
                        boolean state = WirelessTools.geteLocalTetheringState(context);
                        outStream.write(state ? WirelessTools.TETHERING_ENABLED : WirelessTools.TETHERING_DISABLED);
                    }
                    outStream.flush();

                } catch (Exception e) {
                    //WirelessTools.showToast(context, "Error " + e.getMessage(), Toast.LENGTH_SHORT);
                    e.printStackTrace();
                    sendException(e);
                } finally {
                    socket.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                myServerSocket.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            sendException(e);
        } finally {
            //WirelessTools.showToast(context, "Service stopping", Toast.LENGTH_LONG);
            context.onThreadStopped();
        }

    }

    private void sendException(Exception e) {
        ((TethermoteApp) context.getApplication()).sendException(e);
    }

    public void cancel() {
        if (myServerSocket != null) {
            try {
                myServerSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}