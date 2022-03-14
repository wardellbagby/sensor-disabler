package com.wardellbagby.sensordisabler.xposed.sensormodifications.remove;

import android.app.AndroidAppHelper;
import android.content.Context;
import android.hardware.Sensor;
import com.wardellbagby.sensordisabler.BuildConfig;
import com.wardellbagby.sensordisabler.util.Constants;
import com.wardellbagby.sensordisabler.xposed.sensormodifications.SensorModificationMethod;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * A modification method for all API levels. Removes the sensor by modifying what is returned by
 * SystemSensorManager.getFullSensorList.
 *
 * @author Wardell Bagby
 */

public class RemoveSensorModificationMethod extends SensorModificationMethod {
  @Override
  public void modifySensor(final XC_LoadPackage.LoadPackageParam lpparam) {
    XposedHelpers.findAndHookMethod("android.hardware.SystemSensorManager", lpparam.classLoader,
        "getFullSensorList", new XC_MethodHook() {
          @Override
          protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            //Without this, you'd never be able to edit the values for a removed sensor! Aaah!
            if (!lpparam.packageName.equals(BuildConfig.APPLICATION_ID)) {
              //Create a new list so we don't modify the original list.
              @SuppressWarnings("unchecked") List<Sensor> fullSensorList =
                  new ArrayList<>((Collection<? extends Sensor>) param.getResult());
              Iterator<Sensor> iterator = fullSensorList.iterator();
              Context context = AndroidAppHelper.currentApplication();
              while (iterator.hasNext()) {
                Sensor sensor = iterator.next();
                if (shouldHideTrueSensor(lpparam.processName, sensor, context)
                    && getSensorStatus(sensor, context) == Constants.SENSOR_STATUS_REMOVE_SENSOR) {
                  iterator.remove();
                }
              }
              param.setResult(fullSensorList);
            }
          }
        });
  }
}
