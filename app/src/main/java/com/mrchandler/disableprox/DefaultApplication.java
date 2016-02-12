package com.mrchandler.disableprox;

import android.app.Application;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;

import com.mrchandler.disableprox.util.Constants;
import com.mrchandler.disableprox.util.SensorUtil;

/**
 * @author Wardell
 */
public class DefaultApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_FILE_NAME, MODE_WORLD_READABLE);
        if (prefs.getBoolean("prefs_key_prox_sensor", false)) {
            //Updating from an older version. Let's make sure the app will still work as intended.
            SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            Sensor proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
            String sensorKey = SensorUtil.generateUniqueSensorKey(proximitySensor);
            String mockValuesKey = SensorUtil.generateUniqueSensorMockValuesKey(proximitySensor);
            prefs.edit()
                    .putInt(sensorKey, Constants.SENSOR_STATUS_MOCK_VALUES)
                    .putString(mockValuesKey, proximitySensor.getMaximumRange() + ":")
                    .remove("prefs_key_prox_sensor")
                    .apply();
        }
    }
}
