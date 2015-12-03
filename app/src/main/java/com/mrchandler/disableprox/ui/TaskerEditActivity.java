/*
 * Copyright 2013 two forty four a.m. LLC <http://www.twofortyfouram.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * <http://www.apache.org/licenses/LICENSE-2.0>
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package com.mrchandler.disableprox.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.mrchandler.disableprox.R;
import com.mrchandler.disableprox.bundle.BundleScrubber;
import com.mrchandler.disableprox.bundle.PluginBundleManager;
import com.mrchandler.disableprox.util.Constants;
import com.mrchandler.disableprox.util.IabHelper;
import com.mrchandler.disableprox.util.IabResult;
import com.mrchandler.disableprox.util.Inventory;
import com.mrchandler.disableprox.util.Purchase;
import com.mrchandler.disableprox.util.SensorUtil;

import java.util.Arrays;

/**
 * This is the "Edit" activity for a Locale Plug-in.
 * <p/>
 * This Activity can be started in one of two states:
 * <ul>
 * <li>New plug-in instance: The Activity's Intent will not contain
 * {@link com.twofortyfouram.locale.Intent#EXTRA_BUNDLE}.</li>
 * <li>Old plug-in instance: The Activity's Intent will contain
 * {@link com.twofortyfouram.locale.Intent#EXTRA_BUNDLE} from a previously saved plug-in instance that the
 * user is editing.</li>
 * </ul>
 *
 * @see com.twofortyfouram.locale.Intent#ACTION_EDIT_SETTING
 * @see com.twofortyfouram.locale.Intent#EXTRA_BUNDLE
 */
public final class TaskerEditActivity extends SettingsActivity {

    private static final String TAG = TaskerEditActivity.class.getSimpleName();
    private static final int PURCHASE_RESULT_CODE = 191;

    private boolean doNotSave = false;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BundleScrubber.scrub(getIntent());
        final Bundle localeBundle = getIntent().getBundleExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE);
        BundleScrubber.scrub(localeBundle);

        if (null == savedInstanceState) {
            if (PluginBundleManager.isBundleValid(localeBundle)) {
                //Init state
            }
        }
        final IabHelper helper = new IabHelper(this, getString(R.string.google_billing_public_key));
        //Has the user purchased the Tasker IAP?
        if (!prefs.getBoolean(Constants.PREFS_KEY_TASKER, false)) {
            helper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
                @Override
                public void onIabSetupFinished(IabResult result) {
                    if (result.isFailure()) {
                        Log.d(TAG, "Unable to get up In-App Billing. Oh well.");
                        return;
                    }
                    helper.queryInventoryAsync(new IabHelper.QueryInventoryFinishedListener() {
                        @Override
                        public void onQueryInventoryFinished(IabResult result, Inventory inv) {
                            if (result.isFailure()) {
                                prefs.edit().putBoolean(Constants.PREFS_KEY_TASKER, false).apply();
                                return;
                            }
                            if (inv.hasPurchase(Constants.SKU_TASKER)) {
                                prefs.edit().putBoolean(Constants.PREFS_KEY_TASKER, true).apply();
                                prefs.edit().remove(Constants.PREFS_KEY_FREELOAD).apply();
                            } else {
                                prefs.edit().putBoolean(Constants.PREFS_KEY_TASKER, false).apply();
                                if (!(prefs.contains(Constants.PREFS_KEY_FREELOAD) && prefs.getBoolean(Constants.PREFS_KEY_FREELOAD, false))) {
                                    AlertDialog dialog = new AlertDialog.Builder(TaskerEditActivity.this)
                                            .setTitle(R.string.iap_dialog_title)
                                            .setMessage(R.string.iap_dialog_message)
                                            .setNegativeButton("Not Right Now", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    doNotSave = true;
                                                    finish();
                                                }
                                            })
                                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    helper.launchPurchaseFlow(TaskerEditActivity.this, Constants.SKU_TASKER, PURCHASE_RESULT_CODE, new IabHelper.OnIabPurchaseFinishedListener() {
                                                        @Override
                                                        public void onIabPurchaseFinished(IabResult result, Purchase info) {
                                                            if (result.isFailure()) {
                                                                Toast.makeText(TaskerEditActivity.this, "Error getting purchase details.", Toast.LENGTH_SHORT).show();
                                                                doNotSave = true;
                                                                finish();
                                                                return;
                                                            }
                                                            if (info.getSku().equals(Constants.SKU_TASKER)) {
                                                                prefs.edit().putBoolean(Constants.PREFS_KEY_TASKER, true).apply();
                                                            }
                                                        }
                                                    });
                                                }
                                            })
                                            .setNeutralButton("More Information", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    AlertDialog oldDialog = (AlertDialog) dialog;
                                                    oldDialog.dismiss();
                                                    AlertDialog newDialog = new AlertDialog.Builder(TaskerEditActivity.this)
                                                            .setTitle(getString(R.string.iap_dialog_title))
                                                            .setMessage(getString(R.string.iap_dialog_more_information))
                                                            .setOnCancelListener(new DialogInterface.OnCancelListener() {
                                                                @Override
                                                                public void onCancel(DialogInterface dialog) {
                                                                    doNotSave = true;
                                                                    finish();
                                                                }
                                                            })
                                                            .setCancelable(true)
                                                            .create();
                                                    newDialog.setCanceledOnTouchOutside(true);
                                                    newDialog.show();
                                                }
                                            })
                                            .setOnCancelListener(new DialogInterface.OnCancelListener() {
                                                @Override
                                                public void onCancel(DialogInterface dialog) {
                                                    doNotSave = true;
                                                    finish();
                                                }
                                            })
                                            .setCancelable(true)
                                            .create();
                                    dialog.setCanceledOnTouchOutside(true);
                                    dialog.show();
                                }
                            }
                        }
                    });
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case PURCHASE_RESULT_CODE:
                if (resultCode != RESULT_OK) {
                    Toast.makeText(this, "Error making purchase.", Toast.LENGTH_SHORT).show();
                    doNotSave = true;
                    finish();
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    @Override
    public void finish() {
        if (!doNotSave) {
            final Intent resultIntent = new Intent();
            final Bundle resultBundle =
                    PluginBundleManager.generateBundle(getApplicationContext(),
                            SensorUtil.generateUniqueSensorKey(getCurrentShowingSensor()),
                            getCurrentSensorStatus(),
                            SensorUtil.generateUniqueSensorMockValuesKey(getCurrentShowingSensor()),
                            getCurrentMockValues());
            resultIntent.putExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE, resultBundle);
            final String blurb = generateBlurb();
            resultIntent.putExtra(com.twofortyfouram.locale.Intent.EXTRA_STRING_BLURB, blurb);
            setResult(RESULT_OK, resultIntent);
        }
        super.finish();
    }

    String generateBlurb() {
        StringBuilder builder = new StringBuilder(getCurrentShowingSensor().getName());
        builder.append(" is set to ");
        switch (getCurrentSensorStatus()) {
            case Constants.SENSOR_STATUS_DO_NOTHING:
                builder.append("do nothing.");
                break;
            case Constants.SENSOR_STATUS_REMOVE_SENSOR:
                builder.append("be removed.");
                break;
            case Constants.SENSOR_STATUS_MOCK_VALUES:
                builder.append("mock values. New values are: ");
                builder.append(Arrays.toString(getCurrentMockValues()));
                break;
            default:
                builder.append("an unknown state. State = ");
                builder.append(getCurrentSensorStatus());
                break;
        }
        return builder.toString();
    }
}