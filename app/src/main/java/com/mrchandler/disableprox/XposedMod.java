package com.mrchandler.disableprox;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.util.SparseArray;

import com.mrchandler.disableprox.util.Constants;

import java.util.Iterator;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
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
        int enabledMethods = getMethodsUsedForDisabling();
        if ((enabledMethods & Constants.ENABLE_METHOD_1) == Constants.ENABLE_METHOD_1) {
            disableSystemSensorManager(lpparam);
        }
        //noinspection StatementWithEmptyBody
        if ((enabledMethods & Constants.ENABLE_METHOD_2) == Constants.ENABLE_METHOD_2) {
            //disableSensorEventListeners(lpparam);
        }
        if ((enabledMethods & Constants.ENABLE_METHOD_3) == Constants.ENABLE_METHOD_3) {
            removeProximitySensor(lpparam);
        }
    }

    /**
     * Disable by changing the data that the SensorManager gives listeners.
     **/
    void disableSystemSensorManager(LoadPackageParam lpparam) {
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
                            //False == Turn Off, True == Leave On
                            boolean proximitySensorStatus = getProximitySensorStatus();
                            // This method receives the sensor that needs to be changed directly, making this code simpler than the JB MR2+ code.
                            if (!proximitySensorStatus) { //This seems kinda weird but the proximity_enabled says whether the proximity sensor should be allowed to be enabled or not.
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
                    }
            );
        } else {
            XC_MethodHook oldProximityHook = new XC_MethodHook() {
                @SuppressWarnings("unchecked")
                @Override
                protected void beforeHookedMethod(MethodHookParam param)
                        throws Throwable {
                    boolean proximitySensorStatus = getProximitySensorStatus();

                    if (!proximitySensorStatus) {
                        // This pulls the 'Handle to Sensor' array straight from the SystemSensorManager class, so it should always pull the appropriate sensor.
                        SparseArray<Sensor> sensors;
                        //Marshmallow converted our field into a module level one, so we have different code based on that. Otherwise, the same.
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                            sensors = (SparseArray<Sensor>) XposedHelpers.getStaticObjectField(systemSensorManager, "sHandleToSensor");
                        } else {
                            Object systemSensorManager = XposedHelpers.getObjectField(param.thisObject, "mManager");
                            sensors = (SparseArray<Sensor>) XposedHelpers.getObjectField(systemSensorManager, "mHandleToSensor");
                        }

                        // params.args[] is an array that holds the arguments that dispatchSensorEvent received, which are a handle pointing to a sensor
                        // in sHandleToSensor and a float[] of values that should be applied to that sensor.
                        int handle = (Integer) (param.args[0]); // This tells us which sensor was currently called.
                        Sensor sensor = sensors.get(handle);
                        if (sensor.getType() == Sensor.TYPE_PROXIMITY) {
                            float[] values = (float[]) param.args[1];
                            for (int i = 0; i < values.length; i++) {
                                //Even though only values[0] is checked, we do this just in case.
                                //I worry that sensor.getMaximumRange() might not always be set, but I've seen no instance of that case.
                                values[i] = sensor.getMaximumRange();
                            }
                            param.args[1] = values;
                        }
                    }
                }
            };
            XposedHelpers.findAndHookMethod("android.hardware.SystemSensorManager$SensorEventQueue", lpparam.classLoader, "dispatchSensorEvent", int.class, float[].class, int.class, long.class, oldProximityHook);
        }
    }

    /**
     * Method 2: Disable by changing the values that Listeners receive via delegating to the original listener.
     * [Probably the worst way to go about it.]
     **/
    void disableSensorEventListeners(LoadPackageParam lpparam) {

        XC_MethodHook method2RegisterHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Sensor sensor = (Sensor) param.args[1];
                if (!getProximitySensorStatus() && sensor.getType() == Sensor.TYPE_PROXIMITY) {
                    final SensorEventListener oldListener = (SensorEventListener) param.args[0];
                    if (oldListener instanceof InjectedSensorEventListener) {
                        return;
                    }
                    InjectedSensorEventListener injectedListener = new InjectedSensorEventListener(oldListener);
                    param.args[0] = injectedListener;
                }
            }
        };

        XC_MethodHook method2UnregisterHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                //TODO The problem with this method is it ruins the unregister. It could be possible
                //if there was an easier way to know if the listener received here had been shadowed
                //by an InjectedSensorEventListener. I leave it in as a possibility, but this is not
                //used.
            }
        };
        XposedHelpers.findAndHookMethod("android.hardware.SystemSensorManager", lpparam.classLoader, "registerListenerImpl", SensorEventListener.class, Sensor.class, int.class, Handler.class, int.class, int.class, method2RegisterHook);
        XposedHelpers.findAndHookMethod("android.hardware.SystemSensorManager", lpparam.classLoader, "unregisterListenerImpl", SensorEventListener.class, Sensor.class, method2UnregisterHook);
    }

    /**
     * Disable by removing the sensor data from the SensorManager. Apps will think the sensor does not exist.
     **/
    void removeProximitySensor(LoadPackageParam lpparam) {
        //This is the base method that gets called whenever the sensors are queryed. All roads lead back to getFullSensorList!
        XposedHelpers.findAndHookMethod("android.hardware.SystemSensorManager", lpparam.classLoader, "getFullSensorList", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (!getProximitySensorStatus()) {
                    List<Sensor> fullSensorList = (List<Sensor>) param.getResult();
                    Iterator<Sensor> iterator = fullSensorList.iterator();
                    while (iterator.hasNext()) {
                        Sensor sensor = iterator.next();
                        if (sensor.getType() == Sensor.TYPE_PROXIMITY) {
                            iterator.remove();
                        }
                    }
                    param.setResult(fullSensorList);
                }
            }
        });
    }

    boolean getProximitySensorStatus() {
        //False == Turn Off, True == Leave On
        //Always assume that the user wants it disabled. They can disable the app if we fail somehow.
        XposedHelpers.setStaticBooleanField(Environment.class, "sUserRequired", false);
        XSharedPreferences sharedPreferences = new XSharedPreferences("com.mrchandler.disableprox");
        return sharedPreferences.getBoolean(Constants.PREFS_KEY_PROX_SENSOR, false);

    }

    //TODO Implement a visual system for this so users can pick which method.
    int getMethodsUsedForDisabling() {
        return Constants.ENABLE_METHOD_1;
    }

    /**
     * Used for delegating a false Sensor value to registered sensor listeners.
     **/
    class InjectedSensorEventListener implements SensorEventListener {

        SensorEventListener oldListener;

        public InjectedSensorEventListener(SensorEventListener oldListener) {
            this.oldListener = oldListener;
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            event.values[0] = event.sensor.getMaximumRange();
            oldListener.onSensorChanged(event);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            oldListener.onAccuracyChanged(sensor, accuracy);
        }
    }
}