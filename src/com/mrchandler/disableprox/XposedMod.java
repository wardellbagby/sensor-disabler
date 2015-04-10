package com.mrchandler.disableprox;

import static de.robv.android.xposed.XposedHelpers.findClass;

import android.hardware.Sensor;
import android.util.Log;
import android.util.SparseArray;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class XposedMod implements IXposedHookLoadPackage {

    /**
     * Shout out to abusalimov for his Light Sensor fix that inspired this app.
     */
    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam)
            throws Throwable {
        // Alright, so we start by creating a reference to the class that
        // handles sensors.
        final Class<?> systemSensorManager = findClass(
                "android.hardware.SystemSensorManager", lpparam.classLoader);
        // Here, we grab the method that actually dispatches sensor events to
        // tweak what it receives. Since the API seems to have changed in
        // Kitkat, we use two different method hooks depending on the API.
        if (android.os.Build.VERSION.SDK_INT < 18)
            //This seems to work fine, but there might be a better method to override.
            XposedHelpers.findAndHookMethod(
                    "android.hardware.SystemSensorManager$ListenerDelegate",
                    lpparam.classLoader, "onSensorChangedLocked", Sensor.class,
                    float[].class, long[].class, int.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            // This method receives the sensor that needs to be changed directly,
                            // making this code simpler than the Kitkat code.
                            Sensor s = (Sensor) param.args[0];
                            if (s.getType() != Sensor.TYPE_PROXIMITY) {
                                // Maybe this should be done the reverse way. Sue me. At least I'm
                                // consistent.
                                return;
                            }
                            // So we grab that float[], set the maximum for the Proximity Sensor,
                            // and call it a day.
                            float[] values = (float[]) param.args[1];
                            values[0] = s.getMaximumRange();
                            param.args[1] = values;
                        }
                    }
            );
        else
            XposedHelpers.findAndHookMethod(
                    "android.hardware.SystemSensorManager$SensorEventQueue",
                    lpparam.classLoader, "dispatchSensorEvent", int.class,
                    float[].class, int.class, long.class, new XC_MethodHook() {
                        @SuppressWarnings("unchecked")
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param)
                                throws Throwable {
                            // This pulls the 'Handle to Sensor' array straight from the
                            // SystemSensorManager class, so it should always pull the appropriate
                            // sensor. I'm not certain of the performance hits this causes, though.
                            SparseArray<Sensor> sensors = (SparseArray<Sensor>) XposedHelpers
                                    .getStaticObjectField(systemSensorManager,
                                            "sHandleToSensor");
                            // params.args[] is an array that holds the arguments that
                            // dispatchSensorEvent received, which are a handle pointing to a sensor
                            // in sHandleToSensor and a float[] of values that should be applied to
                            // that sensor.
                            int handle = (Integer) (param.args[0]); // This tells me which sensor
                            // was currently called.
                            Sensor s = sensors.get(handle);
                            if (s.getType() != Sensor.TYPE_PROXIMITY) // This could be expanded to
                            // disable ANY sensor.
                            {
                                return;
                            }

                            float[] values = (float[]) param.args[1];
                            Log.e("HANDLE", handle + " : v - " + values[0]);
                            values[0] = s.getMaximumRange(); // Sets the value
                            // to
                            // the maximum
                            // possible. You
                            // could also
                            // set it
                            // to any value
                            // about like 5f
                            // and
                            // it should
                            // work
                            // fine.

                            param.args[1] = values;
                        }
                    }
            );

    }
}