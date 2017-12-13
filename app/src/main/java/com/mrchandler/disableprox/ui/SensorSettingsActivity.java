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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;

import com.melnykov.fab.FloatingActionButton;
import com.melnykov.fab.ObservableScrollView;
import com.mrchandler.disableprox.BuildConfig;
import com.mrchandler.disableprox.R;
import com.mrchandler.disableprox.util.Constants;
import com.mrchandler.disableprox.util.IabHelper;
import com.mrchandler.disableprox.util.IabResult;
import com.mrchandler.disableprox.util.Inventory;
import com.mrchandler.disableprox.util.ProUtil;
import com.mrchandler.disableprox.util.SensorUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SensorSettingsActivity extends FragmentActivity implements SensorListFragment.OnSensorClickedListener {

    private static final String TAG = SensorSettingsActivity.class.getSimpleName();
    private static final String XPOSED_PACKAGE = "de.robv.android.xposed.installer";
    static final String CURRENT_FRAGMENT = "currentFragment";
    static final String SETTINGS_FRAGMENT = "settingFragment";

    SensorSettingsFragment currentFragment;
    IabHelper helper;
    SharedPreferences prefs;
    List<Sensor> fullSensorList = new ArrayList<>();
    ActionBarDrawerToggle toggle;
    DrawerLayout drawer;
    FloatingActionButton fab;


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sensor_setting_activity_layout);
        SensorManager manager = (SensorManager) getSystemService(SENSOR_SERVICE);
        //getSensorList returns an unmodifiable list
        fullSensorList = new ArrayList<>(manager.getSensorList(Sensor.TYPE_ALL));

        Collections.sort(fullSensorList, new Comparator<Sensor>() {
            @Override
            public int compare(Sensor lhs, Sensor rhs) {
                String lhsName = SensorUtil.getHumanStringType(lhs);
                String rhsName = SensorUtil.getHumanStringType(rhs);
                if (lhsName == null) {
                    lhsName = lhs.getName();
                }
                if (rhsName == null) {
                    rhsName = rhs.getName();
                }
                return lhsName.compareTo(rhsName);
            }
        });

        //Set sorted sensor list for the SensorListFragment
        ((SensorListFragment) getSupportFragmentManager()
                .findFragmentById(R.id.sensor_list_fragment))
                .setSensors(fullSensorList);

        if (findViewById(R.id.drawer_layout) != null) {
            drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            toggle = new ActionBarDrawerToggle(this,
                    (DrawerLayout) findViewById(R.id.drawer_layout),
                    android.R.string.yes,
                    android.R.string.no);
            drawer.setDrawerListener(toggle);
            drawer.setScrimColor(getResources().getColor(android.R.color.background_dark));

            if (getActionBar() != null) {
                getActionBar().setDisplayHomeAsUpEnabled(true);
                getActionBar().setHomeButtonEnabled(true);
                getActionBar().setDisplayShowHomeEnabled(false);
            }
            toggle.syncState();
        }
        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentFragment != null) {
                    currentFragment.saveSettings();
                }
            }
        });
        ObservableScrollView scrollView = (ObservableScrollView) findViewById(R.id.scrollView);
        fab.attachToScrollView(scrollView);
        fab.hide(false);

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

        prefs = getSharedPreferences(Constants.PREFS_FILE_NAME, MODE_PRIVATE);

        helper = new IabHelper(this, getString(R.string.google_billing_public_key));
        //Has the user purchased the Pro IAP?
        if (!ProUtil.isPro(this)) {
            helper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
                @Override
                public void onIabSetupFinished(IabResult result) {
                    if (!result.isSuccess()) {
                        Log.d(TAG, "Unable to get up In-App Billing. Oh well.");
                        ProUtil.setFreeloadStatus(SensorSettingsActivity.this, true);
                        return;
                    }
                    helper.queryInventoryAsync(new IabHelper.QueryInventoryFinishedListener() {
                        @Override
                        public void onQueryInventoryFinished(IabResult result, Inventory inv) {
                            if (result.isFailure()) {
                                ProUtil.setProStatus(SensorSettingsActivity.this, false);
                                return;
                            }
                            if (inv.hasPurchase(Constants.SKU_TASKER)) {
                                ProUtil.setProStatus(SensorSettingsActivity.this, true);
                            } else {
                                ProUtil.setProStatus(SensorSettingsActivity.this, false);
                            }
                        }
                    });
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isPackageInstalled(XPOSED_PACKAGE, this) && !prefs.getBoolean(Constants.PREFS_KEY_NEVER_SHOW_XPOSED_INSTALLED, false)) {
            final View view = LayoutInflater.from(this).inflate(R.layout.alert_dialog_xposed_not_installed_layout, null, false);
            new AlertDialog.Builder(this)
                    .setTitle("Xposed Not Installed")
                    .setView(view)
                    .setMessage(R.string.xposed_not_installed)
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            CheckBox box = (CheckBox) view.findViewById(R.id.never_show_again_checkbox);
                            prefs.edit().putBoolean(Constants.PREFS_KEY_NEVER_SHOW_XPOSED_INSTALLED, box.isChecked()).apply();
                        }
                    })
                    .setNegativeButton("Uninstall", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Uri packageUri = Uri.parse("package:" + BuildConfig.APPLICATION_ID);
                            startActivity(new Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageUri));
                            finish();
                        }
                    })
                    .show();
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        showDefaultSensorFragment();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = new MenuInflater(this);
        inflater.inflate(R.menu.settings_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (toggle != null && toggle.onOptionsItemSelected(item)) {
            return true;
        }
        switch (item.getItemId()) {
            case R.id.settings:
                startActivity(new Intent(this, SettingsPreferenceActivity.class));
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
        if (currentFragment == null) {
            fab.hide();
        } else {
            fab.show();
        }
    }

    public void showDefaultSensorFragment() {
        if (!fullSensorList.isEmpty()) {
            onSensorClicked(fullSensorList.get(0));
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

    private static boolean isPackageInstalled(String packagename, Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(packagename, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}