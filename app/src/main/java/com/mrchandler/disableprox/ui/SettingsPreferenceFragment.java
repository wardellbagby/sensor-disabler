package com.mrchandler.disableprox.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.widget.Toast;

import com.mrchandler.disableprox.R;
import com.mrchandler.disableprox.util.Constants;
import com.mrchandler.disableprox.util.IabHelper;
import com.mrchandler.disableprox.util.IabResult;
import com.mrchandler.disableprox.util.ProUtil;
import com.mrchandler.disableprox.util.Purchase;

public class SettingsPreferenceFragment extends PreferenceFragment {

    private static final int PURCHASE_REQUEST_CODE = 534;

    public SettingsPreferenceFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final SharedPreferences prefs = getActivity().getSharedPreferences(Constants.PREFS_FILE_NAME, Context.MODE_WORLD_READABLE);
        getPreferenceManager().setSharedPreferencesName(Constants.PREFS_FILE_NAME);
        addPreferencesFromResource(R.xml.pro_settings_preferences);
        addPreferencesFromResource(R.xml.blocklist_settings);
        addPreferencesFromResource(R.xml.donation_preferences);

        //Pro and Freeload
        final PreferenceCategory blockSettingsPreferenceCategory = (PreferenceCategory) findPreference("block_settings");
        final CheckBoxPreference freeload = (CheckBoxPreference) findPreference(Constants.PREFS_KEY_FREELOAD);
        final Preference proPurchase = findPreference(Constants.SKU_TASKER);
        if (ProUtil.isProNotFreeloaded(getActivity())) {
            freeload.setEnabled(false);
            freeload.setSummary("Pro Features have already been purchased!");
            proPurchase.setEnabled(false);
        } else {
            freeload.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    ProUtil.setFreeloadStatus(getActivity(), (Boolean) newValue);
                    blockSettingsPreferenceCategory.setEnabled((Boolean) newValue || ProUtil.isProNotFreeloaded(getActivity()));
                    return true;
                }
            });
        }

        //Blocklist
        blockSettingsPreferenceCategory.setEnabled(ProUtil.isPro(getActivity()));
        Preference blocklistPreference = findPreference(Constants.PREFS_KEY_BLOCKLIST);
        final Preference blocklistSettingsPreference = findPreference("blocking_list_settings");
        blocklistSettingsPreference.setEnabled(prefs.contains(Constants.PREFS_KEY_BLOCKLIST)
                && !(prefs.getString(Constants.PREFS_KEY_BLOCKLIST, "none").equals("none")));
        updateBlocklistSummary(blocklistPreference, prefs.getString(Constants.PREFS_KEY_BLOCKLIST, "none"));
        blocklistPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                updateBlocklistSummary(preference, (String) newValue);
                blocklistSettingsPreference.setEnabled(!newValue.equals("none"));
                return true;
            }
        });

        //Donations
        final PreferenceCategory donationCategory = (PreferenceCategory) findPreference("donation_category");
        final IabHelper helper = ((SettingsPreferenceActivity) getActivity()).getIabHelper();
        helper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            @Override
            public void onIabSetupFinished(IabResult result) {
                donationCategory.setEnabled(result.isSuccess());
                proPurchase.setEnabled(result.isSuccess());
                if (result.isFailure()) {
                    Toast.makeText(getActivity(), "Can't connect to Google Play Services. Paying options disabled.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        Preference donation1 = findPreference("donation_1");
        Preference donation5 = findPreference("donation_5");
        Preference donation10 = findPreference("donation_10");
        Preference.OnPreferenceClickListener preferenceClickListener = new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                helper.launchPurchaseFlow(getActivity(), preference.getKey(), PURCHASE_REQUEST_CODE, new IabHelper.OnIabPurchaseFinishedListener() {
                    @Override
                    public void onIabPurchaseFinished(IabResult result, Purchase info) {
                        //Don't consume Pro/Tasker Iab purchases. That'd be a jerk move.
                        if (result.isSuccess() && info != null && !info.getSku().equals(Constants.SKU_TASKER)) {
                            helper.consumeAsync(info, new IabHelper.OnConsumeFinishedListener() {
                                @Override
                                public void onConsumeFinished(Purchase purchase, IabResult result) {
                                    Log.e(SettingsPreferenceFragment.class.getSimpleName(), "Unable to consume donation: " + result.getMessage());
                                }
                            });
                        } else if (result.isSuccess() && info != null && info.getSku().equals(Constants.SKU_TASKER)) {
                            freeload.setEnabled(false);
                            freeload.setChecked(false);
                            freeload.setSummary("Pro Features have already been purchased!");
                            proPurchase.setEnabled(false);
                            ProUtil.setProStatus(getActivity(), true);
                        } else if (result.isFailure()) {
                            Log.e(SettingsPreferenceFragment.class.getSimpleName(), "Unable to complete purchase.");
                            if (info != null && info.getSku().equals(Constants.SKU_TASKER)) {
                                Toast.makeText(SettingsPreferenceFragment.this.getActivity(), "Unable to complete Pro purchase at this time. Please try again later.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
                return true;
            }
        };
        donation1.setOnPreferenceClickListener(preferenceClickListener);
        donation5.setOnPreferenceClickListener(preferenceClickListener);
        donation10.setOnPreferenceClickListener(preferenceClickListener);

        proPurchase.setOnPreferenceClickListener(preferenceClickListener);
    }

    private static void updateBlocklistSummary(Preference blocklistPreference, String value) {
        switch (value) {
            case Constants.BLACKLIST:
                blocklistPreference.setSummary("Blacklisted apps will ignore the settings set in Sensor Disabler, and will use the true Sensor values. Non-blacklisted apps will follow the settings set in Sensor Disabler.");
                break;
            case Constants.WHITELIST:
                blocklistPreference.setSummary("Whitelisted apps will use the settings set in Sensor Disabler. Non-whitelisted apps will ignore the settings set in Sensor Disabler, and will use the true Sensor values.");
                break;
            default:
                blocklistPreference.setSummary("Choose whether to use a blacklist or whitelist approach in blocking apps from being affected by the Sensor Disabler.");
        }
    }
}
