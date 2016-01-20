package com.mrchandler.disableprox.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

import com.mrchandler.disableprox.R;
import com.mrchandler.disableprox.util.BlocklistType;
import com.mrchandler.disableprox.util.Constants;

/**
 * @author Wardell
 */
public class BlocklistActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.blocklist_activity_layout);
        Bundle extras = getIntent().getExtras();
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_FILE_NAME, MODE_WORLD_READABLE);
        String blocklistStatus = prefs.getString(Constants.PREFS_KEY_BLOCKLIST, Constants.BLACKLIST);
        boolean whitelist = blocklistStatus.equalsIgnoreCase(Constants.WHITELIST);
        BlocklistType type = (whitelist) ? BlocklistType.WHITELIST : BlocklistType.BLACKLIST;
        Fragment fragment = BlocklistFragment.newInstance(extras.getString(Constants.INTENT_APP_PACKAGE), extras.getString(Constants.INTENT_APP_LABEL), type);
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, fragment)
                .commit();
    }
}
