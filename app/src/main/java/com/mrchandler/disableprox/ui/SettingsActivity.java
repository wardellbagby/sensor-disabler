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


import android.app.ActionBar;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.mrchandler.disableprox.R;
import com.mrchandler.disableprox.util.Constants;
import com.mrchandler.disableprox.util.IabHelper;
import com.mrchandler.disableprox.util.IabResult;
import com.mrchandler.disableprox.util.Inventory;

import java.util.ArrayList;
import java.util.List;

public final class SettingsActivity extends FragmentActivity implements SensorListFragment.OnSensorClickedListener {

    private static final String TAG = SettingsActivity.class.getSimpleName();
    private static final int PERMISSION_RESULT_CODE = 192;
    private static final String CURRENT_FRAGMENT = "currentFragment";
    private static final String SELECTED_ITEM_POSITION = "selectedItemPosition";

    IabHelper helper;
    SharedPreferences prefs;
    List<Sensor> fullSensorList = new ArrayList<>();
    Spinner spinner;


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sensor_setting_layout);
        SensorManager manager = (SensorManager) getSystemService(SENSOR_SERVICE);
        fullSensorList = manager.getSensorList(Sensor.TYPE_ALL);
        initActionBar();

        if (savedInstanceState != null) {
            if (spinner != null) {
                spinner.setSelection(savedInstanceState.getInt(SELECTED_ITEM_POSITION));
            }
            Fragment fragment = getSupportFragmentManager().getFragment(savedInstanceState, CURRENT_FRAGMENT);
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment, CURRENT_FRAGMENT).commit();
        }

        //Has to be done to access with XSharedPreferences.
        prefs = getSharedPreferences(Constants.PREFS_FILE_NAME, MODE_WORLD_READABLE);

        //freeloadTextView = (TextView) findViewById(R.id.freeload);
        if (prefs.contains(Constants.PREFS_KEY_FREELOAD) && prefs.getBoolean(Constants.PREFS_KEY_FREELOAD, false)) {
            //         freeloadTextView.setVisibility(View.VISIBLE);
        } else if (!prefs.getBoolean(Constants.PREFS_KEY_TASKER, false)) {
            //          freeloadTextView.setVisibility(View.GONE);
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
    }

    private void initActionBar() {
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayShowCustomEnabled(true);
            actionBar.setCustomView(R.layout.actionbar_layout);
            if (actionBar.getCustomView() instanceof Spinner) {
                spinner = (Spinner) actionBar.getCustomView();
            }
            if (spinner != null) {
                final ArrayAdapter<Sensor> adapter = new ArrayAdapter<Sensor>(this, android.R.layout.simple_spinner_item, fullSensorList) {

                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        TextView sensorView;
                        if (convertView == null) {
                            sensorView = (TextView) getLayoutInflater().inflate(android.R.layout.simple_spinner_item, parent, false);
                            sensorView.setTextAppearance(getContext(), android.R.style.TextAppearance_DeviceDefault_Widget_ActionBar_Title);
                        } else {
                            sensorView = (TextView) convertView;
                        }
                        sensorView.setText(getItem(position).getName());
                        return sensorView;
                    }

                    @Override
                    public View getDropDownView(int position, View convertView, ViewGroup parent) {
                        TextView sensorDropDownView;
                        if (convertView == null) {
                            sensorDropDownView = (TextView) getLayoutInflater().inflate(android.R.layout.simple_spinner_dropdown_item, parent, false);
                        } else {
                            sensorDropDownView = (TextView) convertView;
                        }
                        sensorDropDownView.setText(getItem(position).getName());
                        return sensorDropDownView;
                    }
                };

                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinner.setAdapter(adapter);
                spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        Sensor sensor = adapter.getItem(position);
                        onSensorClicked(sensor);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                });
            } else {
                actionBar.setDisplayShowTitleEnabled(true);
                actionBar.setDisplayShowCustomEnabled(false);
                //TODO Change title appearance in tablet mode.
                //To make sure in tablet mode we start selected on a sensor.
                onSensorClicked(fullSensorList.get(0));
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(CURRENT_FRAGMENT);
        getSupportFragmentManager().putFragment(outState, CURRENT_FRAGMENT, fragment);
        if (spinner != null) {
            outState.putInt(SELECTED_ITEM_POSITION, spinner.getSelectedItemPosition());
        }
    }

    @Override
    public void onSensorClicked(Sensor sensor) {
        SensorSettingsFragment fragment = SensorSettingsFragment.newInstance(sensor);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment, CURRENT_FRAGMENT)
                .commit();
        setTitle(sensor.getName());
    }
}