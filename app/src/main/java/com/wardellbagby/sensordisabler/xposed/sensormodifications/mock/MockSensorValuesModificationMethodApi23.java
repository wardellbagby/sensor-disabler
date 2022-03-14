package com.wardellbagby.sensordisabler.xposed.sensormodifications.mock;

import android.hardware.Sensor;
import android.util.SparseArray;
import de.robv.android.xposed.XposedHelpers;

/**
 * A modification method for API levels 23. Mocks the values by using the same method as
 * {@link MockSensorValuesModificationMethodApi18} but gets the sensors from the field
 * mHandleToSensor.
 *
 * @author Wardell Bagby
 */

public class MockSensorValuesModificationMethodApi23
    extends MockSensorValuesModificationMethodApi18 {

  @SuppressWarnings("unchecked")
  @Override
  protected SparseArray<Sensor> getSensors(Object systemSensorManager) {
    return (SparseArray<Sensor>) XposedHelpers.getObjectField(systemSensorManager,
        "mHandleToSensor");
  }
}
