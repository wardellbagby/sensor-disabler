package com.wardellbagby.sensordisabler.xposed.sensormodifications;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.os.Environment;
import com.wardellbagby.sensordisabler.util.AppFiltersKt;
import com.wardellbagby.sensordisabler.util.Constants;
import com.wardellbagby.sensordisabler.util.FilterType;
import com.wardellbagby.sensordisabler.util.SensorPreferencesKt;
import com.wardellbagby.sensordisabler.util.SensorUtil;
import com.wardellbagby.sensordisabler.util.remotepreferences.SensorDisablerPreferenceFactory;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * An abstract class that classes looking to modify values that a sensor will return or modify
 * a sensor itself should extend.
 *
 * @author Wardell Bagby
 */

public abstract class SensorModificationMethod {

  public abstract void modifySensor(final XC_LoadPackage.LoadPackageParam lpparam);

  protected int getSensorStatus(Sensor sensor, Context context) {
    //Always assume that the user wants the app to do nothing, since this accesses every sensor.
    XposedHelpers.setStaticBooleanField(Environment.class, "sUserRequired", false);
    String enabledStatusKey = SensorUtil.generateUniqueSensorKey(sensor);
    SharedPreferences sharedPreferences = getSharedPreferences(context);
    Integer status =
        SensorPreferencesKt.getLegacySensorModificationType(sharedPreferences, enabledStatusKey);

    if (status == null) {
      return Constants.SENSOR_STATUS_DO_NOTHING;
    } else {
      return status;
    }
  }

  protected float[] getSensorValues(Sensor sensor, Context context) {
    XposedHelpers.setStaticBooleanField(Environment.class, "sUserRequired", false);
    String mockValuesKey = SensorUtil.generateUniqueSensorMockValuesKey(sensor);

    SharedPreferences sharedPreferences = getSharedPreferences(context);
    float[] values = SensorPreferencesKt.getSensorMockedValues(sharedPreferences, mockValuesKey);
    if (values == null) {
      return new float[0];
    } else {
      return values;
    }
  }

  private SharedPreferences getSharedPreferences(Context context) {
    return SensorDisablerPreferenceFactory.getInstance(context);
  }

  private boolean isAllowFilteringEnabled(Context context) {
    SharedPreferences sharedPreferences = getSharedPreferences(context);
    return AppFiltersKt.getFilterType(sharedPreferences) == FilterType.Allow;
  }

  private boolean isDenyFilteringEnabled(Context context) {
    SharedPreferences sharedPreferences = getSharedPreferences(context);
    return AppFiltersKt.getFilterType(sharedPreferences) == FilterType.Deny;
  }

  private boolean isAppInDenyList(String packageName, Sensor sensor, Context context) {
    SharedPreferences sharedPreferences = getSharedPreferences(context);
    return sharedPreferences.getBoolean(SensorUtil.generateUniqueSensorPackageBasedKey(sensor,
        packageName,
        FilterType.Deny),
        false);
  }

  private boolean isAppInAllowList(String packageName, Sensor sensor, Context context) {
    SharedPreferences sharedPreferences = getSharedPreferences(context);

    return sharedPreferences.getBoolean(SensorUtil.generateUniqueSensorPackageBasedKey(sensor,
        packageName,
        FilterType.Allow),
        false);
  }

  /**
   * Is the package referenced by packageName allowed to see what the true sensor and its value are?
   *
   * If this is true, Sensor Disabler will potentially modify this sensor. If it isn't true, then
   * Sensor Disabler will do nothing to this sensor.
   *
   * @param packageName The package name to check.
   * @param sensor The sensor to check against.
   * @param context A context that can b e us
   * @return whether or not the package should be allowed to see the true sensor and its values.
   */
  protected boolean shouldHideTrueSensor(String packageName, Sensor sensor,
      Context context) {
    if (isAllowFilteringEnabled(context)) {
      return !isAppInAllowList(packageName, sensor, context);
    } else {
      if (isDenyFilteringEnabled(context)) {
        if (isAppInDenyList(packageName, sensor, context)) {
          return true;
        }
      }
    }
    return true;
  }
}
