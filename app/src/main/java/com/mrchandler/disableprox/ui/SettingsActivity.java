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

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

import com.mrchandler.disableprox.R;
import com.mrchandler.disableprox.util.Constants;
import com.mrchandler.disableprox.util.FileUtil;
import com.mrchandler.disableprox.util.IabHelper;
import com.mrchandler.disableprox.util.IabResult;
import com.mrchandler.disableprox.util.Inventory;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public final class SettingsActivity extends Activity {

    private static final String TAG = SettingsActivity.class.getSimpleName();
    private static final int PERMISSION_RESULT_CODE = 192;

    IabHelper helper;
    SharedPreferences prefs;
    TextView freeloadTextView;

    boolean saveToFile = true;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        if (null == savedInstanceState) {
            try {
                Scanner scanner = new Scanner(FileUtil.getEnabledSettingsFile());
                if (scanner.hasNextBoolean()) {
                    ((Switch) findViewById(android.R.id.checkbox)).setChecked(scanner.nextBoolean());
                }
            } catch (FileNotFoundException e) {
                //Just don't set the checkbox.
                Log.e(TAG, "Unable to get the settings.", e);
            }
        }

        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                new AlertDialog.Builder(this)
                        .setMessage("In order to allow the ability to change the proximity sensor setting without restarting, this app needs to save the settings to a file. \n\nWill you allow this app to save data on this phone's storage?")
                        .setPositiveButton("Yes, that's fine.", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions(SettingsActivity.this,
                                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                        192);
                            }
                        })
                        .setNegativeButton("No, I'd rather not.", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .setCancelable(false)
                        .create().show();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSION_RESULT_CODE);
            }
        }
        prefs = PreferenceManager.getDefaultSharedPreferences(SettingsActivity.this);


        freeloadTextView = (TextView) findViewById(R.id.freeload);
        if (prefs.contains(Constants.PREFS_KEY_FREELOAD) && prefs.getBoolean(Constants.PREFS_KEY_FREELOAD, false)) {
            freeloadTextView.setVisibility(View.VISIBLE);
        } else if (!prefs.getBoolean(Constants.PREFS_KEY_TASKER, false)) {
            freeloadTextView.setVisibility(View.GONE);
        }

        helper = new IabHelper(this, getString(R.string.google_billing_public_key));
        //Has the user purchased the Tasker IAP?
        if (!prefs.getBoolean(Constants.PREFS_KEY_TASKER, false)) {
            helper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
                @Override
                public void onIabSetupFinished(IabResult result) {
                    if (!result.isSuccess()) {
                        Log.d(TAG, "Unable to get up In-App Billing. Oh well.");
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
                            }
                        }
                    });
                }
            });
        }
        findViewById(android.R.id.checkbox).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (prefs.contains(Constants.PREFS_KEY_FREELOAD) && prefs.getBoolean(Constants.PREFS_KEY_FREELOAD, false)) {
                    prefs.edit().remove(Constants.PREFS_KEY_FREELOAD).apply();
                    freeloadTextView.setVisibility(View.GONE);
                } else if (!prefs.getBoolean(Constants.PREFS_KEY_TASKER, false)) {
                    freeloadTextView.setVisibility(View.VISIBLE);
                    prefs.edit().putBoolean(Constants.PREFS_KEY_FREELOAD, true).apply();
                }
                return true;
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case PERMISSION_RESULT_CODE:
                if (resultCode != RESULT_OK) {
                    //Try to avoid an exception when saving by checking the result for the permission.
                    saveToFile = false;
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void finish() {
        if (saveToFile) {
            try {
                FileWriter writer = new FileWriter(FileUtil.getEnabledSettingsFile());
                writer.write(String.valueOf(((Switch) findViewById(android.R.id.checkbox)).isChecked()));
                writer.flush();
                writer.close();
            } catch (IOException | SecurityException e) {
                //Finish gracefully.
                Log.e(TAG, "Unable to save settings to file.", e);
            }
        }
        super.finish();
    }
}