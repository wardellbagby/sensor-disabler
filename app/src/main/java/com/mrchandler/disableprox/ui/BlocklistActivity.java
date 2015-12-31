package com.mrchandler.disableprox.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

import com.mrchandler.disableprox.R;
import com.mrchandler.disableprox.util.Constants;

/**
 * @author Wardell
 */
public class BlocklistActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.blacklist_activity_layout);
        Bundle extras = getIntent().getExtras();
        //TODO Fix this. Make the keys statics.
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_FILE_NAME, MODE_WORLD_READABLE);
        boolean whitelist = false;
        String blocklistStatus = prefs.getString("enabled_blocking_list", "blacklist");
        whitelist = blocklistStatus.equalsIgnoreCase("whitelist");
        Fragment fragment = BlocklistFragment.newInstance(extras.getString("appPackage"), extras.getString("appLabel"), whitelist);
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, fragment)
                .commit();
    }
}
