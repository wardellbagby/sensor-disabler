package com.wardellbagby.sensordisabler.xposed;

import android.os.Build;
import com.wardellbagby.sensordisabler.xposed.sensormodifications.SensorModificationMethod;
import com.wardellbagby.sensordisabler.xposed.sensormodifications.mock.MockSensorValuesModificationMethod;
import com.wardellbagby.sensordisabler.xposed.sensormodifications.mock.MockSensorValuesModificationMethodApi18;
import com.wardellbagby.sensordisabler.xposed.sensormodifications.mock.MockSensorValuesModificationMethodApi23;
import com.wardellbagby.sensordisabler.xposed.sensormodifications.mock.MockSensorValuesModificationMethodApi24;
import com.wardellbagby.sensordisabler.xposed.sensormodifications.remove.RemoveSensorModificationMethod;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import static com.wardellbagby.sensordisabler.xposed.ManagerVisibilityKt.setXposedVisibilityForManager;

/**
 * Shout out to abusalimov for his Light Sensor fix that inspired this app.
 */
public class XposedMod implements IXposedHookLoadPackage {

  @Override
  public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
    mockSensorValues(lpparam);
    removeSensors(lpparam);
    setXposedVisibilityForManager(lpparam);
  }

  /**
   * Disable by changing the data that the SensorManager gives listeners.
   **/
  private void mockSensorValues(final LoadPackageParam lpparam) {
    SensorModificationMethod sensorModificationMethod;
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
      sensorModificationMethod = new MockSensorValuesModificationMethod();
    } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      sensorModificationMethod = new MockSensorValuesModificationMethodApi18();
    } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
      sensorModificationMethod = new MockSensorValuesModificationMethodApi23();
    } else {
      sensorModificationMethod = new MockSensorValuesModificationMethodApi24();
    }

    sensorModificationMethod.modifySensor(lpparam);
  }

  /**
   * Disable by removing the sensor data from the SensorManager. Apps will think the sensor does not
   * exist.
   **/
  private void removeSensors(final LoadPackageParam lpparam) {
    SensorModificationMethod removeSensor = new RemoveSensorModificationMethod();
    removeSensor.modifySensor(lpparam);
  }
}