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

package com.mrchandler.disableprox.receiver;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.mrchandler.disableprox.bundle.BundleScrubber;
import com.mrchandler.disableprox.bundle.PluginBundleManager;
import com.mrchandler.disableprox.util.Constants;

import java.util.Locale;

/**
 * This is the "fire" BroadcastReceiver for a Locale Plug-in setting.
 *
 * @see com.twofortyfouram.locale.Intent#ACTION_FIRE_SETTING
 * @see com.twofortyfouram.locale.Intent#EXTRA_BUNDLE
 */
public final class FireReceiver extends BroadcastReceiver {

    /**
     * @param context {@inheritDoc}.
     * @param intent  the incoming {@link com.twofortyfouram.locale.Intent#ACTION_FIRE_SETTING} Intent. This
     *                should contain the {@link com.twofortyfouram.locale.Intent#EXTRA_BUNDLE} that was saved by
     *                {@link com.mrchandler.disableprox.ui.EditActivity} and later broadcast by Locale.
     */
    @Override
    public void onReceive(final Context context, final Intent intent) {
        /*
         * Always be strict on input parameters! A malicious third-party app could send a malformed Intent.
         */

        if (!com.twofortyfouram.locale.Intent.ACTION_FIRE_SETTING.equals(intent.getAction())) {
            if (Constants.IS_LOGGABLE) {
                Log.e(Constants.LOG_TAG,
                        String.format(Locale.US, "Received unexpected Intent action %s", intent.getAction())); //$NON-NLS-1$
            }
            return;
        }

        BundleScrubber.scrub(intent);

        final Bundle bundle = intent.getBundleExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE);
        BundleScrubber.scrub(bundle);

        if (PluginBundleManager.isBundleValid(bundle)) {
            final boolean setting = bundle.getBoolean(PluginBundleManager.BUNDLE_EXTRA_BOOLEAN_SETTING);
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                //Can't save without permissions and Broadcast Receivers can't use that nice Permission dialog. :(
                Log.e(Constants.LOG_TAG, "The Tasker Receiver does not have permission to write to storage.");

            } else {
                SharedPreferences prefs = context.getSharedPreferences(Constants.PREFS_FILE_NAME, Context.MODE_WORLD_READABLE);
                prefs.edit().putBoolean(Constants.PREFS_KEY_PROX_SENSOR, setting).apply();
            }
        }
    }
}