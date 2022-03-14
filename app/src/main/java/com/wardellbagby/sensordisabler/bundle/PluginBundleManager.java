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

package com.wardellbagby.sensordisabler.bundle;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import com.wardellbagby.sensordisabler.util.Constants;

/**
 * Class for managing the {@link com.twofortyfouram.locale.Intent#EXTRA_BUNDLE} for this plug-in.
 */
public final class PluginBundleManager {

  public static final String BUNDLE_EXTRA_SENSOR_STATUS_KEY =
      "com.wardellbagby.sensordisabler.extra.SENSOR_STATUS_KEY";
  public static final String BUNDLE_EXTRA_SENSOR_STATUS_VALUE =
      "com.wardellbagby.sensordisabler.extra.SENSOR_STATUS_VALUE";
  public static final String BUNDLE_EXTRA_SENSOR_MOCK_VALUES_KEY =
      "com.wardellbagby.sensordisabler.extra.SENSOR_VALUE_KEY";
  public static final String BUNDLE_EXTRA_SENSOR_MOCK_VALUES_VALUES =
      "com.wardellbagby.sensordisabler.extra.SENSOR_VALUE_VALUE";

  /*
   * This extra is not strictly required, however it makes backward and forward compatibility significantly
   * easier. For example, suppose a bug is found in how some version of the plug-in stored its Bundle. By
   * having the version, the plug-in can better detect when such bugs occur.
   */
  public static final String BUNDLE_EXTRA_INT_VERSION_CODE =
      "com.wardellbagby.sensordisabler.extra.INT_VERSION_CODE";

  /**
   * Private constructor prevents instantiation
   *
   * @throws UnsupportedOperationException because this class cannot be instantiated.
   */
  private PluginBundleManager() {
    throw new UnsupportedOperationException("This class is non-instantiable"); //$NON-NLS-1$
  }

  public static boolean isBundleValid(final Bundle bundle) {
    if (null == bundle) {
      return false;
    }

    /*
     * Make sure the expected extras exist
     */
    if (!bundle.containsKey(BUNDLE_EXTRA_SENSOR_STATUS_KEY)) {
      if (Constants.IS_LOGGABLE) {
        Log.e(Constants.LOG_TAG,
            String.format("bundle must contain extra %s",
                BUNDLE_EXTRA_SENSOR_STATUS_KEY)); //$NON-NLS-1$
      }
      return false;
    }
    if (!bundle.containsKey(BUNDLE_EXTRA_SENSOR_STATUS_VALUE)) {
      if (Constants.IS_LOGGABLE) {
        Log.e(Constants.LOG_TAG,
            String.format("bundle must contain extra %s",
                BUNDLE_EXTRA_SENSOR_STATUS_VALUE)); //$NON-NLS-1$
      }
      return false;
    }
    if (!bundle.containsKey(BUNDLE_EXTRA_SENSOR_MOCK_VALUES_KEY)) {
      if (Constants.IS_LOGGABLE) {
        Log.e(Constants.LOG_TAG,
            String.format("bundle must contain extra %s",
                BUNDLE_EXTRA_SENSOR_MOCK_VALUES_KEY)); //$NON-NLS-1$
      }
      return false;
    }
    if (!bundle.containsKey(BUNDLE_EXTRA_SENSOR_MOCK_VALUES_VALUES)) {
      if (Constants.IS_LOGGABLE) {
        Log.e(Constants.LOG_TAG,
            String.format("bundle must contain extra %s",
                BUNDLE_EXTRA_SENSOR_MOCK_VALUES_VALUES)); //$NON-NLS-1$
      }
      return false;
    }
    if (!bundle.containsKey(BUNDLE_EXTRA_INT_VERSION_CODE)) {
      if (Constants.IS_LOGGABLE) {
        Log.e(Constants.LOG_TAG,
            String.format("bundle must contain extra %s",
                BUNDLE_EXTRA_INT_VERSION_CODE)); //$NON-NLS-1$
      }
      return false;
    }

    /*
     * Make sure the correct number of extras exist. Run this test after checking for specific Bundle
     * extras above so that the error message is more useful. (E.g. the caller will see what extras are
     * missing, rather than just a message that there is the wrong number).
     */
    if (5 != bundle.keySet().size()) {
      if (Constants.IS_LOGGABLE) {
        Log.e(Constants.LOG_TAG,
            String.format("bundle must contain 5 keys, but currently contains %d keys: %s",
                bundle.keySet().size(), bundle.keySet())); //$NON-NLS-1$
      }
      return false;
    }

    //If version checking is ever implemented, this could be useful.
    //        if (bundle.getInt(BUNDLE_EXTRA_INT_VERSION_CODE, 0) != bundle.getInt(BUNDLE_EXTRA_INT_VERSION_CODE, 1)) {
    //            if (Constants.IS_LOGGABLE) {
    //                Log.e(Constants.LOG_TAG,
    //                        String.format("bundle extra %s appears to be the wrong type.  It must be an int", BUNDLE_EXTRA_INT_VERSION_CODE)); //$NON-NLS-1$
    //            }
    //
    //            return false;
    //        }

    return true;
  }

  public static Bundle generateBundle(final Context context, String sensorKey, int sensorValue,
      String sensorValueKey, float[] sensorMockValues) {
    final Bundle result = new Bundle();
    result.putInt(BUNDLE_EXTRA_INT_VERSION_CODE, Constants.getVersionCode(context));
    result.putString(BUNDLE_EXTRA_SENSOR_STATUS_KEY, sensorKey);
    result.putInt(BUNDLE_EXTRA_SENSOR_STATUS_VALUE, sensorValue);
    result.putString(BUNDLE_EXTRA_SENSOR_MOCK_VALUES_KEY, sensorValueKey);
    result.putFloatArray(BUNDLE_EXTRA_SENSOR_MOCK_VALUES_VALUES, sensorMockValues);
    return result;
  }
}