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


import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.mrchandler.disableprox.R;
import com.mrchandler.disableprox.util.Constants;
import com.mrchandler.disableprox.util.IabHelper;
import com.mrchandler.disableprox.util.IabResult;
import com.mrchandler.disableprox.util.Inventory;
import com.mrchandler.disableprox.util.SensorUtil;

import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends FragmentActivity implements SensorListFragment.OnSensorClickedListener {

    private static final String TAG = SettingsActivity.class.getSimpleName();
    private static final String CURRENT_FRAGMENT = "currentFragment";
    private static final String SELECTED_ITEM_POSITION = "selectedItemPosition";

    private SensorSettingsFragment currentFragment;
    IabHelper helper;
    SharedPreferences prefs;
    List<Sensor> fullSensorList = new ArrayList<>();
    ActionBarDrawerToggle toggle;
    DrawerLayout drawer;


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sensor_setting_layout);
        SensorManager manager = (SensorManager) getSystemService(SENSOR_SERVICE);
        fullSensorList = manager.getSensorList(Sensor.TYPE_ALL);
        if (findViewById(R.id.drawer_layout) != null) {
            drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            toggle = new ActionBarDrawerToggle(this,
                    (DrawerLayout) findViewById(R.id.drawer_layout),
                    android.R.string.yes,
                    android.R.string.no);
            drawer.setDrawerListener(toggle);
            drawer.setScrimColor(getResources().getColor(android.R.color.background_dark));
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setHomeButtonEnabled(true);
            getActionBar().setDisplayShowHomeEnabled(false);
            toggle.syncState();
        }
        if (savedInstanceState != null) {
            SensorSettingsFragment fragment = (SensorSettingsFragment) getSupportFragmentManager().getFragment(savedInstanceState, CURRENT_FRAGMENT);
            if (fragment != null) {
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment, CURRENT_FRAGMENT).commit();
                String sensorTitle = SensorUtil.getHumanStringType(fragment.sensor);
                if (sensorTitle == null) {
                    sensorTitle = fragment.sensor.getName();
                }
                setTitle(sensorTitle);
            }
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = new MenuInflater(this);
        inflater.inflate(R.menu.settings_menu, menu);
        MenuItem freeload = menu.findItem(R.id.freeload);
        if (prefs.contains(Constants.PREFS_KEY_FREELOAD)) {
            freeload.setChecked(true);
        } else {
            freeload.setChecked(false);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (toggle != null && toggle.onOptionsItemSelected(item)) {
            return true;
        }
        switch (item.getItemId()) {
            case R.id.freeload:
                item.setChecked(!item.isChecked());
                if (item.isChecked()) {
                    prefs.edit().putBoolean(Constants.PREFS_KEY_FREELOAD, true).apply();
                } else {
                    prefs.edit().remove(Constants.PREFS_KEY_FREELOAD).apply();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(CURRENT_FRAGMENT);
        if (fragment != null) {
            getSupportFragmentManager().putFragment(outState, CURRENT_FRAGMENT, fragment);
        }
    }

    @Override
    public void onSensorClicked(Sensor sensor) {
        SensorSettingsFragment fragment = SensorSettingsFragment.newInstance(sensor);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment, CURRENT_FRAGMENT)
                .commit();
        String sensorTitle = SensorUtil.getHumanStringType(sensor);
        if (sensorTitle == null) {
            sensorTitle = sensor.getName();
        }
        setTitle(sensorTitle);
        currentFragment = fragment;
        if (drawer != null) {
            drawer.closeDrawer(GravityCompat.START);
        }
    }

    Sensor getCurrentShowingSensor() {
        return currentFragment.sensor;
    }

    int getCurrentSensorStatus() {
        return currentFragment.getSensorStatus();
    }

    float[] getCurrentMockValues() {
        return currentFragment.getValues();
    }
}