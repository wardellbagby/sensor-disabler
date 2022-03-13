package com.wardellbagby.sensordisabler.xposed.sensormodifications.mock;

import android.app.AndroidAppHelper;
import android.content.Context;
import android.hardware.Sensor;
import android.util.SparseArray;
import com.wardellbagby.sensordisabler.util.Constants;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * A modification method for API levels 18 - 22. Mocks the values by changing what is returned by
 * SensorEventQueue.dispatchSensorEvent. Gets the sensors from checking the static field
 * sHandleToSensor
 *
 * @author Wardell Bagby
 */

public class MockSensorValuesModificationMethodApi18 extends MockSensorValuesModificationMethod {
  @Override
  public void modifySensor(final XC_LoadPackage.LoadPackageParam lpparam) {
    XC_MethodHook mockSensorHook = new XC_MethodHook() {
      @SuppressWarnings("unchecked")
      @Override
      protected void beforeHookedMethod(MethodHookParam param)
          throws Throwable {

        Object systemSensorManager = XposedHelpers.getObjectField(param.thisObject, "mManager");
        SparseArray<Sensor> sensors = getSensors(systemSensorManager);

        // params.args[] is an array that holds the arguments that dispatchSensorEvent received, which are a handle pointing to a sensor
        // in sHandleToSensor and a float[] of values that should be applied to that sensor.
        int handle = (Integer) (param.args[0]); // This tells us which sensor was currently called.
        Sensor sensor = sensors.get(handle);
        Context context = AndroidAppHelper.currentApplication();
        if (shouldHideTrueSensor(lpparam.processName, sensor, context)
            && getSensorStatus(sensor, context) == Constants.SENSOR_STATUS_MOCK_VALUES) {
          float[] values = getSensorValues(sensor, context);
                        /*The SystemSensorManager compares the array it gets with the array from the a SensorEvent,
                        and some sensors (looking at you, Proximity) only use one index in the array
                        but still send along a length 3 array, so we copy here instead of replacing it
                        outright. */

          //noinspection SuspiciousSystemArraycopy
          System.arraycopy(values, 0, param.args[1], 0, values.length);
        }
      }
    };
    XposedHelpers.findAndHookMethod("android.hardware.SystemSensorManager$SensorEventQueue",
        lpparam.classLoader, "dispatchSensorEvent", int.class, float[].class, int.class, long.class,
        mockSensorHook);
  }

  @SuppressWarnings("unchecked")
  protected SparseArray<Sensor> getSensors(Object systemSensorManager) {
    return (SparseArray<Sensor>) XposedHelpers.getStaticObjectField(systemSensorManager.getClass(),
        "sHandleToSensor");
  }
}

