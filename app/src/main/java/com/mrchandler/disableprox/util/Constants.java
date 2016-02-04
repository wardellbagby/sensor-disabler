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

package com.mrchandler.disableprox.util;

import android.content.Context;

import com.mrchandler.disableprox.BuildConfig;

public final class Constants {

    //TODO If this file ends up being unwieldy, refactor into different files.

    public static final String LOG_TAG = "Disable Prox Sensor"; //$NON-NLS-1$
    public static final boolean IS_LOGGABLE = BuildConfig.DEBUG;

    public static final String PACKAGE_NAME = "com.mrchandler.disableprox";

    public static final String SKU_TASKER = "tasker_purchase";
    public static final String SKU_DONATION_1 = "donation_1";
    public static final String SKU_DONATION_5 = "donation_5";
    public static final String SKU_DONATION_10 = "donation_10";

    public static final String PREFS_FILE_NAME = "com.mrchandler.disableprox_preferences";
    public static final String PREFS_KEY_TASKER = "prefs_key_tasker";
    public static final String PREFS_KEY_FREELOAD = "prefs_key_freeload";
    public static final String PREFS_KEY_BLOCKLIST = "enabled_blocking_list";
    public static final String PREFS_KEY_NEVER_SHOW_XPOSED_INSTALLED = "never_show_xposed_installed";

    public static final int SENSOR_STATUS_DO_NOTHING = 0;
    public static final int SENSOR_STATUS_REMOVE_SENSOR = 1;
    public static final int SENSOR_STATUS_MOCK_VALUES = 2;

    public static final String INTENT_APP_PACKAGE = "appPackage";
    public static final String INTENT_APP_LABEL = "appLabel";

    public static final String BLACKLIST = "blacklist";
    public static final String WHITELIST = "whitelist";


    /**
     * Determines the "versionCode" in the {@code AndroidManifest}.
     *
     * @param context to read the versionCode.
     * @return versionCode of the app.
     */
    public static int getVersionCode(final Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch (final UnsupportedOperationException e) {
            /*
             * This exception is thrown by test contexts
             */

            return 1;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Constants() {
        throw new UnsupportedOperationException("This class is non-instantiable"); //$NON-NLS-1$
    }
}