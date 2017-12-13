package com.mrchandler.disableprox;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.os.Build;
import android.os.Environment;
import android.util.SparseArray;

import com.crossbowffs.remotepreferences.RemotePreferences;
import com.mrchandler.disableprox.util.BlocklistType;
import com.mrchandler.disableprox.util.Constants;
import com.mrchandler.disableprox.util.SensorUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import static de.robv.android.xposed.XposedHelpers.findClass;

//todo Refactor this class into separate components.
public class XposedMod implements IXposedHookLoadPackage, IXposedHookZygoteInit {

    /**
     * Shout out to abusalimov for his Light Sensor fix that inspired this app.
     */

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {

    }

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam)
            throws Throwable {
        mockSensorValues(lpparam);
        removeSensors(lpparam);
    }

    /**
     * Disable by changing the data that the SensorManager gives listeners.
     **/
    private void mockSensorValues(final LoadPackageParam lpparam) {
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
                            Context context = (Context) XposedHelpers.getObjectField(XposedHelpers.getSurroundingThis(param.thisObject), "mContext");
                            //Use processName here always. Not packageName.
                            if (!isPackageAllowedToSeeTrueSensor(lpparam.processName, sensor, context) && getSensorStatus(sensor, context) == Constants.SENSOR_STATUS_MOCK_VALUES) {
                                // Get the mock values from the settings.
                                float[] values = getSensorValues(sensor, context);

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

                    Object systemSensorManager = XposedHelpers.getObjectField(param.thisObject, "mManager");
                    // This pulls the 'Handle to Sensor' array straight from the SystemSensorManager class, so it should always pull the appropriate sensor.
                    SparseArray<Sensor> sensors;
                    //Marshmallow converted our field into a module level one, so we have different code based on that. Otherwise, the same.
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                        final Class<?> systemSensorManagerClass = findClass("android.hardware.SystemSensorManager", lpparam.classLoader);
                        sensors = (SparseArray<Sensor>) XposedHelpers.getStaticObjectField(systemSensorManagerClass, "sHandleToSensor");
                    } else {
                        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.M) {
                            sensors = (SparseArray<Sensor>) XposedHelpers.getObjectField(systemSensorManager, "mHandleToSensor");
                        } else {
                            //From N there is a HashMap. Checked until O(27).
                            HashMap<Integer, Sensor> map = (HashMap<Integer, Sensor>) XposedHelpers.getObjectField(systemSensorManager, "mHandleToSensor");

                            sensors = new SparseArray<>(map.size());
                            for (Integer i : map.keySet()) {
                                sensors.append(i, map.get(i));
                            }
                        }
                    }

                    // params.args[] is an array that holds the arguments that dispatchSensorEvent received, which are a handle pointing to a sensor
                    // in sHandleToSensor and a float[] of values that should be applied to that sensor.
                    int handle = (Integer) (param.args[0]); // This tells us which sensor was currently called.
                    Sensor sensor = sensors.get(handle);
                    Context context = (Context) XposedHelpers.getObjectField(systemSensorManager, "mContext");
                    if (!isPackageAllowedToSeeTrueSensor(lpparam.processName, sensor, context) && getSensorStatus(sensor, context) == Constants.SENSOR_STATUS_MOCK_VALUES) {
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
            XposedHelpers.findAndHookMethod("android.hardware.SystemSensorManager$SensorEventQueue", lpparam.classLoader, "dispatchSensorEvent", int.class, float[].class, int.class, long.class, mockSensorHook);
        }
    }

    /**
     * Disable by removing the sensor data from the SensorManager. Apps will think the sensor does not exist.
     **/
    private void removeSensors(final LoadPackageParam lpparam) {
        //This is the base method that gets called whenever the sensors are queried. All roads lead back to getFullSensorList!
        XposedHelpers.findAndHookMethod("android.hardware.SystemSensorManager", lpparam.classLoader, "getFullSensorList", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                //Without this, you'd never be able to edit the values for a removed sensor! Aaah!
                if (!lpparam.packageName.equals(BuildConfig.APPLICATION_ID)) {
                    //Create a new list so we don't modify the original list.
                    @SuppressWarnings("unchecked") List<Sensor> fullSensorList = new ArrayList<>((Collection<? extends Sensor>) param.getResult());
                    Iterator<Sensor> iterator = fullSensorList.iterator();
                    Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                    while (iterator.hasNext()) {
                        Sensor sensor = iterator.next();
                        if (!isPackageAllowedToSeeTrueSensor(lpparam.processName, sensor, context) && getSensorStatus(sensor, context) == Constants.SENSOR_STATUS_REMOVE_SENSOR) {
                            iterator.remove();
                        }
                    }
                    param.setResult(fullSensorList);
                }
            }
        });
    }

    private int getSensorStatus(Sensor sensor, Context context) {
        //Always assume that the user wants the app to do nothing, since this accesses every sensor.
        XposedHelpers.setStaticBooleanField(Environment.class, "sUserRequired", false);
        String enabledStatusKey = SensorUtil.generateUniqueSensorKey(sensor);
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        return sharedPreferences.getInt(enabledStatusKey, Constants.SENSOR_STATUS_DO_NOTHING);
    }

    private float[] getSensorValues(Sensor sensor, Context context) {
        XposedHelpers.setStaticBooleanField(Environment.class, "sUserRequired", false);
        String mockValuesKey = SensorUtil.generateUniqueSensorMockValuesKey(sensor);
        String[] mockValuesStrings;
        SharedPreferences sharedPreferences = getSharedPreferences(context);
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

    private SharedPreferences getSharedPreferences(Context context) {
        return new RemotePreferences(context, BuildConfig.APPLICATION_ID, Constants.PREFS_FILE_NAME);
    }

    private boolean isWhitelistEnabled(Context context) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        return sharedPreferences.getString(Constants.PREFS_KEY_BLOCKLIST, BlocklistType.NONE.getValue()).equalsIgnoreCase(BlocklistType.WHITELIST.getValue());
    }

    private boolean isBlacklistEnabled(Context context) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        return sharedPreferences.getString(Constants.PREFS_KEY_BLOCKLIST, BlocklistType.NONE.getValue()).equalsIgnoreCase(BlocklistType.BLACKLIST.getValue());
    }

    private boolean isAppBlacklisted(String packageName, Sensor sensor, Context context) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        return sharedPreferences.getBoolean(SensorUtil.generateUniqueSensorPackageBasedKey(sensor,
                packageName,
                BlocklistType.BLACKLIST),
                false);
    }

    private boolean isAppWhitelisted(String packageName, Sensor sensor, Context context) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);

        return sharedPreferences.getBoolean(SensorUtil.generateUniqueSensorPackageBasedKey(sensor,
                packageName,
                BlocklistType.WHITELIST),
                false);
    }

    private boolean isPackageAllowedToSeeTrueSensor(String packageName, Sensor sensor, Context context) {
        if (isWhitelistEnabled(context)) {
            if (isAppWhitelisted(packageName, sensor, context)) {
                return true;
            }
        } else {
            if (isBlacklistEnabled(context)) {
                if (isAppBlacklisted(packageName, sensor, context)) {
                    return false;
                }
            }
        }
        return false;
    }
}