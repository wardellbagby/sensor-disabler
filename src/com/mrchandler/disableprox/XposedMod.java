package com.mrchandler.disableprox;

import android.hardware.Sensor;
import android.os.Build;
import android.util.SparseArray;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import static de.robv.android.xposed.XposedHelpers.findClass;

public class XposedMod implements IXposedHookLoadPackage {

    /**
     * Shout out to abusalimov for his Light Sensor fix that inspired this app.
     */
    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam)
            throws Throwable {

        if (lpparam.packageName.equals("android")) {

            // Alright, so we start by creating a reference to the class that handles sensors.
            final Class<?> systemSensorManager = findClass(
                    "android.hardware.SystemSensorManager", lpparam.classLoader);

            // Here, we grab the method that actually dispatches sensor events to tweak what it receives. Since the API seems to have changed in
            // Jelly Bean MR2, we use two different method hooks depending on the API.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {

                //This seems to work fine, but there might be a better method to override.
                XposedHelpers.findAndHookMethod(
                        "android.hardware.SystemSensorManager$ListenerDelegate",
                        lpparam.classLoader, "onSensorChangedLocked", Sensor.class,
                        float[].class, long[].class, int.class, new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) {
                                // This method receives the sensor that needs to be changed directly, making this code simpler than the JB MR2 + code.
                                Sensor s = (Sensor) param.args[0];
                                if (s.getType() != Sensor.TYPE_PROXIMITY) {
                                    return;
                                }
                                // So we grab that float[], set the maximum for the Proximity Sensor, and call it a day.
                                float[] values = (float[]) param.args[1];
                                values[0] = s.getMaximumRange();
                                param.args[1] = values;
                            }
                        }
                );
            } else {

                XposedHelpers.findAndHookMethod(
                        "android.hardware.SystemSensorManager$SensorEventQueue", lpparam.classLoader, "dispatchSensorEvent", int.class, float[].class, int.class, long.class, new XC_MethodHook() {
                            @SuppressWarnings("unchecked")
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param)
                                    throws Throwable {
                                // This pulls the 'Handle to Sensor' array straight from the SystemSensorManager class, so it should always pull the appropriate sensor.
                                SparseArray<Sensor> sensors = (SparseArray<Sensor>) XposedHelpers.getStaticObjectField(systemSensorManager, "sHandleToSensor");

                                // params.args[] is an array that holds the arguments that dispatchSensorEvent received, which are a handle pointing to a sensor
                                // in sHandleToSensor and a float[] of values that should be applied to that sensor.
                                int handle = (Integer) (param.args[0]); // This tells us which sensor was currently called.
                                Sensor s = sensors.get(handle);
                                if (s.getType() == Sensor.TYPE_PROXIMITY) {// This could be expanded to disable ANY sensor.

                                    float[] values = (float[]) param.args[1];
                                    values[0] = 100f;
                                    param.args[1] = values;
                                }
                            }
                        }
                );
            }
        }
    }
}