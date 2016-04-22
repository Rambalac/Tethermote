package com.azi.tethermote;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;
import android.widget.Toast;

/**
 * Implementation of App Widget functionality.
 */
public class TetherRemoteWidget extends AppWidgetProvider {
    public static final String SWITCH_ACTION = "com.azi.tethermote.TetherRemoteWidget.SWITCH_ACTION";
    public static final String SWITCH_STATE = "com.azi.tethermote.TetherRemoteWidget.SWITCH_STATE";
    public static final String SWITCH_WIDGET_ID = "com.azi.tethermote.TetherRemoteWidget.SWITCH_WIDGET_ID";
    static final int[] imageIds = new int[]{
            R.drawable.widget_off_sel,
            R.drawable.widget_on_sel,
            R.drawable.widget_bet
    };

    public static void updateAppWidget(final Context context, final AppWidgetManager appWidgetManager,
                                       final int appWidgetId, final int state) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {

                // Construct the RemoteViews object
                RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.tether_remote_widget);

                Intent switchIntent = new Intent(context, TetherRemoteWidget.class);
                switchIntent.setAction(SWITCH_ACTION);
                switchIntent.putExtra(SWITCH_STATE, state == 0 ? 1 : 0);
                switchIntent.putExtra(SWITCH_WIDGET_ID, appWidgetId);

                PendingIntent switchPendingIntent = PendingIntent.getBroadcast(context, 0, switchIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);
                views.setOnClickPendingIntent(R.id.imageButton, switchPendingIntent);

                views.setInt(R.id.imageButton, "setImageResource", imageIds[state]);

                // Instruct the widget_off manager to update the widget_off

                appWidgetManager.updateAppWidget(appWidgetId, views);

                Toast.makeText(context, "Current state: " + state, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(context);
        if (intent.getAction().equals(SWITCH_ACTION)) {
            String address = PreferenceManager.getDefaultSharedPreferences(context)
                    .getString("remote_device", "");
            if (address.isEmpty()) {
                Intent settingsIntent = new Intent(context, SettingsActivity.class);
                settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(settingsIntent);
            } else {

                final int state = intent.getIntExtra(SWITCH_STATE, 0);
                final int widgetId = intent.getIntExtra(SWITCH_WIDGET_ID, 0);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        updateAppWidget(context, AppWidgetManager.getInstance(context), widgetId, 2);
                        int newstate = BluetoothService.sendRemoteTetherState(context, state);
                        updateAppWidget(context, AppWidgetManager.getInstance(context), widgetId, newstate);
                    }
                }).start();
            }
        }
        super.onReceive(context, intent);
    }

    @Override
    public void onUpdate(final Context context, final AppWidgetManager appWidgetManager, final int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, 2);
        }

        new Thread(new Runnable() {
            @Override
            public void run() {

                int state = BluetoothService.getRemoteTetherState(context);
                // There may be multiple widgets active, so update all of them
                for (int appWidgetId : appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, appWidgetId, state);
                }
            }
        }).start();
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget_off is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget_off is disabled
    }
}

