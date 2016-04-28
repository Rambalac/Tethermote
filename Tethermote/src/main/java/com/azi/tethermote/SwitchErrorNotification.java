package com.azi.tethermote;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

/**
 * Helper class for showing and canceling new message
 * notifications.
 * <p/>
 * This class makes heavy use of the {@link NotificationCompat.Builder} helper
 * class to create notifications in a backward-compatible way.
 */
class SwitchErrorNotification {
    /**
     * The unique identifier for this type of notification.
     */
    private static final String NOTIFICATION_TAG = "TetheringSwitchErrorNotification";

    public static void notify(final Context context, String deviceName) {
        final Resources res = context.getResources();
        final String title = context.getString(R.string.app_name);
        final String bigtext = res.getString(R.string.bluetooth_device_not_accessible_notification);
        final String text = context.getString(R.string.bluetooth_device_not_accessible, deviceName);

        final NotificationCompat.BigTextStyle style = new NotificationCompat.BigTextStyle()
                .bigText(text + "\r\n" + bigtext);

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setStyle(style)
                .setTicker(context.getString(R.string.bluetooth_error))
                .setOngoing(false)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(title)
                .setContentText(context.getString(R.string.bluetooth_device_not_accessible, ""))
                .setSubText("...")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setAutoCancel(true);
        notify(context, builder.build());
    }

    @TargetApi(Build.VERSION_CODES.ECLAIR)
    private static void notify(final Context context, final Notification notification) {
        final NotificationManager nm = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR) {
            nm.notify(NOTIFICATION_TAG, 0, notification);
        } else {
            nm.notify(NOTIFICATION_TAG.hashCode(), notification);
        }
    }
}
