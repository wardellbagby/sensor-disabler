package com.mrchandler.disableprox;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.util.SparseArray;

import com.mrchandler.disableprox.util.Constants;
import com.mrchandler.disableprox.util.SensorUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import static de.robv.android.xposed.XposedHelpers.findClass;

public class XposedMod implements IXposedHookLoadPackage, IXposedHookZygoteInit {
    private static XSharedPreferences sharedPreferences;

    /**
     * Shout out to abusalimov for his Light Sensor fix that inspired this app.
     */

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        sharedPreferences = new XSharedPreferences(Constants.PREFS_PACKAGE);
        sharedPreferences.makeWorldReadable();
    }

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam)
            throws Throwable {
        disableSystemSensorManager(lpparam);
        //disableSensorEventListeners(lpparam);
        removeSensors(lpparam);
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
                            Sensor sensor = (Sensor) param.args[0];
                            if (getSensorStatus(sensor) == Constants.MOCK_VALUES) {
                                // Get the mock values from the settings.
                                float[] values = getSensorValues(sensor);

                                //noinspection SuspiciousSystemArraycopy
                                System.arraycopy(values, 0, param.args[1], 0, values.length);
                            }
                        }
                    }
            );
        } else {
            XC_MethodHook mockSensorHook = new XC_MethodHook() {
                @SuppressWarnings("unchecked")
                @Override
                protected void beforeHookedMethod(MethodHookParam param)
                        throws Throwable {

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
                    if (getSensorStatus(sensor) == Constants.MOCK_VALUES) {
                        float[] values = getSensorValues(sensor);
                        /*The SystemSensorManager compares the array it gets with the array from the a SensorEvent,
                        and some sensors (looking at you, Proximity) only use one index in the array
                        but still send along a length 3 array, so we copy here instead of replacing it
                        outright. */

                        //noinspection SuspiciousSystemArraycopy
                        System.arraycopy(values, 0, param.args[1], 0, values.length);
                    }
                }
            };
            XposedHelpers.findAndHookMethod("android.hardware.SystemSensorManager$SensorEventQueue", lpparam.classLoader, "dispatchSensorEvent", int.class, float[].class, int.class, long.class, mockSensorHook);
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
                if (getSensorStatus(sensor) == Constants.MOCK_VALUES) {
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
    void removeSensors(final LoadPackageParam lpparam) {
        //This is the base method that gets called whenever the sensors are queried. All roads lead back to getFullSensorList!
        XposedHelpers.findAndHookMethod("android.hardware.SystemSensorManager", lpparam.classLoader, "getFullSensorList", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                //Without this, you'd never be able to edit the values for a removed sensor! Aaah!
                if (!lpparam.packageName.equals(Constants.PREFS_PACKAGE)) {
                    //Create a new list so we don't modify the original list.
                    @SuppressWarnings("unchecked") List<Sensor> fullSensorList = new ArrayList<>((Collection<? extends Sensor>) param.getResult());
                    Iterator<Sensor> iterator = fullSensorList.iterator();
                    while (iterator.hasNext()) {
                        Sensor sensor = iterator.next();
                        if (getSensorStatus(sensor) == Constants.REMOVE_SENSOR) {
                            iterator.remove();
                        }
                    }
                    param.setResult(fullSensorList);
                }
            }
        });
    }

    int getSensorStatus(Sensor sensor) {
        //Always assume that the user wants the app to do nothing, since this accesses every sensor.
        XposedHelpers.setStaticBooleanField(Environment.class, "sUserRequired", false);
        String enabledStatusKey = SensorUtil.generateUniqueSensorKey(sensor);
        sharedPreferences.reload();
        return sharedPreferences.getInt(enabledStatusKey, Constants.DO_NOTHING);
    }

    float[] getSensorValues(Sensor sensor) {
        XposedHelpers.setStaticBooleanField(Environment.class, "sUserRequired", false);
        String mockValuesKey = SensorUtil.generateUniqueSensorKey(sensor) + "_values";
        String[] mockValuesStrings;
        sharedPreferences.reload();
        if (sharedPreferences.contains(mockValuesKey)) {
            mockValuesStrings = sharedPreferences.getString(mockValuesKey, "").split(":", 0);
        } else {
            return new float[0];
        }

        float[] mockValuesFloats = new float[mockValuesStrings.length];
        for (int i = 0; i < mockValuesStrings.length; i++) {
            mockValuesFloats[i] = Float.parseFloat(mockValuesStrings[i]);
        }
        return mockValuesFloats;
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