package com.mrchandler.disableprox.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.mrchandler.disableprox.R;
import com.mrchandler.disableprox.util.IabHelper;

public class SettingsPreferenceActivity extends Activity {

    private IabHelper helper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        helper = new IabHelper(this, getString(R.string.google_billing_public_key));
        setContentView(R.layout.activity_settings);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!helper.handleActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    protected IabHelper getIabHelper() {
        return helper;
    }
}
