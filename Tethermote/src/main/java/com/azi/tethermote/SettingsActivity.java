package com.azi.tethermote;


import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.view.View;

import java.util.ArrayList;
import java.util.Set;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity {
    private static final int BLUETOOTH_PAIR_REQUEST_CODE = 123;
    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static final Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);
            }
            return true;
        }
    };
    private final SharedPreferences.OnSharedPreferenceChangeListener sharedPreferecesListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    SwitchNotification.Check(SettingsActivity.this);
                }
            }).start();
        }
    };

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    private static void enableBluetooth(Activity context) {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        enableBtIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivityForResult(enableBtIntent, BLUETOOTH_PAIR_REQUEST_CODE);
    }

    private static void setListOfDevices(Activity act, ListPreference lp) {
        ArrayList<CharSequence> entriesList = new ArrayList<>();
        ArrayList<CharSequence> entryValuesList = new ArrayList<>();
        entriesList.add("None");
        entryValuesList.add("");
        entriesList.add("Self");
        entryValuesList.add(".");

        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter != null) {
            if (!btAdapter.isEnabled()) {
                enableBluetooth(act);
            }

            Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();

            for (BluetoothDevice device : pairedDevices) {
                entriesList.add(device.getName());
                entryValuesList.add(device.getAddress());
            }

        }

        lp.setEntries(entriesList.toArray(new CharSequence[entriesList.size()]));
        lp.setEntryValues(entryValuesList.toArray(new CharSequence[entryValuesList.size()]));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        GeneralPreferenceFragment fragment = new GeneralPreferenceFragment();
        // Display the fragment as the main content.
        getFragmentManager().beginTransaction().replace(android.R.id.content,
                fragment, "general").commit();

        IntentFilter stateFilter = new IntentFilter();
        stateFilter.addAction(Intent.ACTION_SCREEN_ON);
        stateFilter.addAction(Intent.ACTION_SCREEN_OFF);

        registerReceiver(new StateReceiver(), stateFilter);

        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(sharedPreferecesListener);

        SwitchNotification.Check(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || GeneralPreferenceFragment.class.getName().equals(fragmentName);
    }

    public void openWifi(View view) {
        Intent intentOpenBluetoothSettings = new Intent();
        intentOpenBluetoothSettings.setAction(Settings.ACTION_WIFI_SETTINGS);
        startActivity(intentOpenBluetoothSettings);
    }

    public void openBluetooth(View view) {
        Intent intentOpenBluetoothSettings = new Intent();
        intentOpenBluetoothSettings.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
        startActivityForResult(intentOpenBluetoothSettings, BLUETOOTH_PAIR_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == BLUETOOTH_PAIR_REQUEST_CODE) {
            PreferenceFragment fragment = (PreferenceFragment) getFragmentManager().findFragmentByTag("general");
            final ListPreference listPreference = (ListPreference) fragment.findPreference("remote_device");
            setListOfDevices(this, listPreference);
        }
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment {

        private final Preference.OnPreferenceClickListener OnRemoteDevicePreferenceClickListener = new Preference.OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                setListOfDevices(GeneralPreferenceFragment.this.getActivity(), (ListPreference) preference);
                return true;
            }
        };
        private Preference.OnPreferenceChangeListener OnEnableTetheringPreferenceClickListener = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object val) {
                Boolean value = (Boolean) val;

                Activity act = GeneralPreferenceFragment.this.getActivity();
                Intent serviceIntent = new Intent(act, BluetoothService.class);
                if (value) {
                    act.getApplicationContext().startService(serviceIntent);
                    WirelessTools.checkWriteSettingsPermission(act);

                    WirelessTools.checkPowerSave(act);
                } else {
                    act.getApplicationContext().stopService(serviceIntent);
                }

                return true;
            }
        };

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);
            setHasOptionsMenu(true);


            final ListPreference listPreference = (ListPreference) findPreference("remote_device");
            setListOfDevices(GeneralPreferenceFragment.this.getActivity(), listPreference);
            listPreference.setOnPreferenceClickListener(OnRemoteDevicePreferenceClickListener);

            final SwitchPreference enableTetheringPreference = (SwitchPreference) findPreference("enable_tethering");
            enableTetheringPreference.setOnPreferenceChangeListener(OnEnableTetheringPreferenceClickListener);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(listPreference);
//            bindPreferenceSummaryToValue(findPreference("disable_on_screen_off"));
//            bindPreferenceSummaryToValue(findPreference("enable_on_screen_on"));
        }
    }
}
