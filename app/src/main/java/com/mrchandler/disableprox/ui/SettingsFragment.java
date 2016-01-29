package com.mrchandler.disableprox.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;

import com.mrchandler.disableprox.R;
import com.mrchandler.disableprox.util.Constants;
import com.mrchandler.disableprox.util.ProUtil;

public class SettingsFragment extends PreferenceFragment {

    public SettingsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final SharedPreferences prefs = getActivity().getSharedPreferences(Constants.PREFS_FILE_NAME, Context.MODE_WORLD_READABLE);
        getPreferenceManager().setSharedPreferencesName(Constants.PREFS_FILE_NAME);
        addPreferencesFromResource(R.xml.pro_settings_preferences);
        addPreferencesFromResource(R.xml.blocklist_settings);
        final PreferenceCategory blockSettings = (PreferenceCategory) findPreference("block_settings");
        Preference freeload = findPreference(Constants.PREFS_KEY_FREELOAD);
        if (ProUtil.isProNotFreeloaded(getActivity())) {
            freeload.setEnabled(false);
            freeload.setSummary("Pro Features have already been purchased!");
        } else {
            freeload.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    ProUtil.setFreeloadStatus(getActivity(), (Boolean) newValue);
                    blockSettings.setEnabled((Boolean) newValue || ProUtil.isProNotFreeloaded(getActivity()));
                    return true;
                }
            });
        }


        blockSettings.setEnabled(ProUtil.isPro(getActivity()));

        Preference blocklistPreference = findPreference(Constants.PREFS_KEY_BLOCKLIST);
        updateBlocklistSummary(blocklistPreference, prefs);
        blocklistPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                updateBlocklistSummary(preference, prefs);
                return true;
            }
        });
    }

    private static void updateBlocklistSummary(Preference blocklistPreference, SharedPreferences prefs) {
        switch (prefs.getString(Constants.PREFS_KEY_BLOCKLIST, "")) {
            case Constants.WHITELIST:
                blocklistPreference.setSummary("Blacklisted apps will ignore the settings set in Sensor Disabler, and will use the true Sensor values. Non-blacklisted apps will follow the settings set in Sensor Disabler.");
                break;
            case Constants.BLACKLIST:
                blocklistPreference.setSummary("Whitelisted apps will use the settings set in Sensor Disabler. Non-whitelisted apps will ignore the settings set in Sensor Disabler, and will use the true Sensor values.");
                break;
            default:
                blocklistPreference.setSummary("Choose whether to use a blacklist or whitelist approach in blocking apps from being affected by the Sensor Disabler.");
        }
    }
}
