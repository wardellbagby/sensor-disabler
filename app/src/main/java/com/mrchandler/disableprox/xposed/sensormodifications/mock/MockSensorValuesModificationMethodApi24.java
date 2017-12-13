package com.mrchandler.disableprox.xposed.sensormodifications.mock;

import android.hardware.Sensor;
import android.util.SparseArray;

import java.util.HashMap;

import de.robv.android.xposed.XposedHelpers;

/**
 * A modification method for API levels 24. Mocks the values by using the same method as
 * {@link MockSensorValuesModificationMethodApi18} but gets the sensors from the field mHandleToSensor,
 * which is now a HashMap.
 *
 * @author Wardell Bagby
 * @author Wardell Bagby
 */

public class MockSensorValuesModificationMethodApi24 extends MockSensorValuesModificationMethodApi18 {

    @Override
    protected SparseArray<Sensor> getSensors(Object systemSensorManager) {
        @SuppressWarnings("unchecked")
        HashMap<Integer, Sensor> map = (HashMap<Integer, Sensor>) XposedHelpers.getObjectField(systemSensorManager, "mHandleToSensor");

        SparseArray<Sensor> sensors = new SparseArray<>(map.size());
        for (Integer i : map.keySet()) {
            sensors.append(i, map.get(i));
        }
        return sensors;
    }
}
