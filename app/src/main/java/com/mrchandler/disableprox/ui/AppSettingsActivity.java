package com.mrchandler.disableprox.ui;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.mrchandler.disableprox.R;

/**
 * @author Wardell
 */
public class AppSettingsActivity extends FragmentActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_settings_activity_layout);
    }
}
