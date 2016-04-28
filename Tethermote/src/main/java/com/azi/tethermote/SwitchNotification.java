package com.azi.tethermote;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

/**
 * Helper class for showing and canceling new message
 * notifications.
 * <p/>
 * This class makes heavy use of the {@link NotificationCompat.Builder} helper
 * class to create notifications in a backward-compatible way.
 */
class SwitchNotification {
    /**
     * The unique identifier for this type of notification.
     */
    private static final String NOTIFICATION_TAG = "TetheringSwitchNotification";

    public static void Check(Context context) {
        boolean toEnable = true;
        try {
            toEnable = WirelessTools.getRemoteTetherState(context) != WirelessTools.TETHERING_ENABLED;
        } catch (Exception e) {
            e.printStackTrace();
        }
        Check(context, toEnable);
    }

    public static void Check(Context context, boolean toEnable) {
        try {
            boolean enabled = PreferenceManager.getDefaultSharedPreferences(context)
                    .getBoolean("show_switch_notification", false);

            if (enabled) {
                SwitchNotification.notify(context, toEnable);
            } else {
                SwitchNotification.cancel(context);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Shows the notification, or updates a previously shown notification of
     * this type, with the given parameters.
     * <p/>
     * TODO: Customize this method's arguments to present relevant content in
     * the notification.
     * <p/>
     * TODO: Customize the contents of this method to tweak the behavior and
     * presentation of new message notifications. Make
     * sure to follow the
     * <a href="https://developer.android.com/design/patterns/notifications.html">
     * Notification design guidelines</a> when doing so.
     *
     * @see #cancel(Context)
     */
    private static void notify(final Context context, boolean forEnable) {
        final Resources res = context.getResources();

        // This image is used as the notification's large icon (thumbnail).
        // TODO: Remove this if your notification has no relevant thumbnail.
        final Bitmap picture = BitmapFactory.decodeResource(res, forEnable ? R.drawable.widget_off : R.drawable.widget_on);


        final String title = context.getString(R.string.app_name);
        final String text = res.getString(forEnable ?
                R.string.tether_enable_notification :
                R.string.tether_disable_notification);

        Intent switchIntent = new Intent(context, TetherRemoteWidget.class);
        switchIntent.setAction(TetherRemoteWidget.SWITCH_ACTION);
        switchIntent.putExtra(TetherRemoteWidget.SWITCH_STATE, forEnable ? WirelessTools.TETHERING_ENABLED : WirelessTools.TETHERING_DISABLED);

        PendingIntent switchPendingIntent = PendingIntent.getBroadcast(context, 0, switchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);


        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context)

                // Set appropriate defaults for the notification light, sound,
                // and vibration.
                .setOngoing(true)
                // Set required fields, including the small icon, the
                // notification title, and text.
                .setSmallIcon(R.drawable.ic_stat_new_message)
                .setContentTitle(title)
                .setContentText(text)
                // All fields below this line are optional.
                .setWhen(0)
                // Use a default priority (recognized on devices running Android
                // 4.1 or later)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)

                // Provide a large icon, shown with the notification in the
                // notification drawer on devices running Android 3.0 or later.
                .setLargeIcon(picture)

                // If this notification relates to a past or upcoming event, you
                // should set the relevant time information using the setWhen
                // method below. If this call is omitted, the notification's
                // timestamp will by set to the time at which it was shown.
                // TODO: Call setWhen if this notification relates to a past or
                // upcoming event. The sole argument to this method should be
                // the notification timestamp in milliseconds.
                //.setWhen(...)

                // Set the pending intent to be initiated when the user touches
                // the notification.
                .setContentIntent(switchPendingIntent)
                .setAutoCancel(false);

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

    /**
     * Cancels any notifications of this type previously shown using
     * {@link #notify(Context, boolean)}.
     */
    @TargetApi(Build.VERSION_CODES.ECLAIR)
    private static void cancel(final Context context) {
        final NotificationManager nm = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR) {
            nm.cancel(NOTIFICATION_TAG, 0);
        } else {
            nm.cancel(NOTIFICATION_TAG.hashCode());
        }
    }
}
