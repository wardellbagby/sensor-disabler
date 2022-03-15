package com.wardellbagby.sensordisabler.util;

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