package com.mrchandler.disableprox.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import com.mrchandler.disableprox.R;
import com.mrchandler.disableprox.util.Constants;

/**
 * A placeholder fragment containing a simple view.
 */
public class SettingsFragment extends PreferenceFragment {

    public SettingsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final SharedPreferences prefs = getActivity().getSharedPreferences(Constants.PREFS_FILE_NAME, Context.MODE_WORLD_READABLE);
        addPreferencesFromResource(R.xml.pro_settings_preferences);
        addPreferencesFromResource(R.xml.blacklist_settings);
        findPreference(Constants.PREFS_KEY_FREELOAD).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                prefs.edit().putBoolean(Constants.PREFS_KEY_TASKER, (Boolean) newValue).apply();
                return true;
            }
        });
    }
}
