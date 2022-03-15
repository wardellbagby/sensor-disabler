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

package com.wardellbagby.sensordisabler.util;

import android.content.Context;

public final class Constants {

  public static final String SKU_TASKER = "tasker_purchase";
  public static final String SKU_DONATION_1 = "donation_1";
  public static final String SKU_DONATION_5 = "donation_5";
  public static final String SKU_DONATION_10 = "donation_10";

  public static final String PREFS_FILE_NAME = "com.wardellbagby.sensordisabler_preferences";
  public static final String PREFS_KEY_TASKER = "prefs_key_tasker";
  public static final String PREFS_KEY_FREELOAD = "prefs_key_freeload";
  public static final String PREFS_KEY_BLOCKLIST = "enabled_blocking_list";

  public static final int SENSOR_STATUS_DO_NOTHING = 0;
  public static final int SENSOR_STATUS_REMOVE_SENSOR = 1;
  public static final int SENSOR_STATUS_MOCK_VALUES = 2;

  private Constants() {
    throw new UnsupportedOperationException("This class is non-instantiable"); //$NON-NLS-1$
  }
}