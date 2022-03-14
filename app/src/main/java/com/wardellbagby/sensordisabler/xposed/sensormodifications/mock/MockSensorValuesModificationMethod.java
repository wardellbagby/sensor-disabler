package com.wardellbagby.sensordisabler.xposed.sensormodifications.mock;

import android.app.AndroidAppHelper;
import android.content.Context;
import android.hardware.Sensor;
import com.wardellbagby.sensordisabler.util.Constants;
import com.wardellbagby.sensordisabler.xposed.sensormodifications.SensorModificationMethod;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * A modification method for API levels 1 - 17. Mocks the values by changing
 * what is returned by ListenerDelegate.onSensorChangedLocked.
 *
 * @author Wardell Bagby
 */

public class MockSensorValuesModificationMethod extends SensorModificationMethod {

  @Override
  public void modifySensor(final XC_LoadPackage.LoadPackageParam lpparam) {
    XposedHelpers.findAndHookMethod(
        "android.hardware.SystemSensorManager$ListenerDelegate",
        lpparam.classLoader, "onSensorChangedLocked", Sensor.class,
        float[].class, long[].class, int.class, new XC_MethodHook() {
          @Override
          protected void beforeHookedMethod(MethodHookParam param) {
            Sensor sensor = (Sensor) param.args[0];
            Context context = AndroidAppHelper.currentApplication();
            //Use processName here always. Not packageName.
            if (shouldHideTrueSensor(lpparam.processName, sensor, context)
                && getSensorStatus(sensor, context) == Constants.SENSOR_STATUS_MOCK_VALUES) {
              // Get the mock values from the settings.
              float[] values = getSensorValues(sensor, context);

              //noinspection SuspiciousSystemArraycopy
              System.arraycopy(values, 0, param.args[1], 0, values.length);
            }
          }
        }
    );
  }
}
