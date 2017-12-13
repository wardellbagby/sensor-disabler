package com.mrchandler.disableprox.xposed.sensormodifications;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.os.Environment;

import com.crossbowffs.remotepreferences.RemotePreferences;
import com.mrchandler.disableprox.BuildConfig;
import com.mrchandler.disableprox.util.BlocklistType;
import com.mrchandler.disableprox.util.Constants;
import com.mrchandler.disableprox.util.SensorUtil;

import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * An abstract class that classes looking to modify values that a sensor will return or modify
 * a sensor itself should extend.
 *
 * @author Wardell Bagby
 */

public abstract class SensorModificationMethod {

    public abstract void modifySensor(final XC_LoadPackage.LoadPackageParam lpparam);


    protected int getSensorStatus(Sensor sensor, Context context) {
        //Always assume that the user wants the app to do nothing, since this accesses every sensor.
        XposedHelpers.setStaticBooleanField(Environment.class, "sUserRequired", false);
        String enabledStatusKey = SensorUtil.generateUniqueSensorKey(sensor);
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        return sharedPreferences.getInt(enabledStatusKey, Constants.SENSOR_STATUS_DO_NOTHING);
    }

    protected float[] getSensorValues(Sensor sensor, Context context) {
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

    /**
     * Is the package referenced by packageName allowed to see what the true sensor and its value are?
     *
     * @param packageName The package name to check.
     * @param sensor      The sensor to check against.
     * @param context     A context that can b e us
     * @return whether or not the package should be allowed to see the true sensor and its values.
     */
    protected boolean isPackageAllowedToSeeTrueSensor(String packageName, Sensor sensor, Context context) {
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
