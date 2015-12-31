package com.mrchandler.disableprox.ui;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.mrchandler.disableprox.R;

/**
 * A placeholder fragment containing a simple view.
 */
public class SettingsFragment extends PreferenceFragment {

    public SettingsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.pro_settings_preferences);
        addPreferencesFromResource(R.xml.blacklist_settings);
    }
}
