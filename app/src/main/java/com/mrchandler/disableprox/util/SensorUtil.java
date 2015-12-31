package com.mrchandler.disableprox.util;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;

import com.mrchandler.disableprox.R;

/**
 * @author Wardell
 */
public final class SensorUtil {
    private SensorUtil() {
    }

    public static String[] getLabelsForSensor(Context context, Sensor sensor) {
        String[] labels;
        switch (sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                labels = context.getResources().getStringArray(R.array.accelerometer_values);
                break;
            case Sensor.TYPE_AMBIENT_TEMPERATURE:
                labels = context.getResources().getStringArray(R.array.ambient_temperature_values);
                break;
            case Sensor.TYPE_GAME_ROTATION_VECTOR:
                labels = context.getResources().getStringArray(R.array.game_rotation_vector_values);
                break;
            case Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR:
                labels = context.getResources().getStringArray(R.array.rotation_vector_values);
                break;
            case Sensor.TYPE_GRAVITY:
                labels = context.getResources().getStringArray(R.array.gravity_values);
                break;
            case Sensor.TYPE_GYROSCOPE:
                labels = context.getResources().getStringArray(R.array.gyroscore_values);
                break;
            case Sensor.TYPE_GYROSCOPE_UNCALIBRATED:
                labels = context.getResources().getStringArray(R.array.gyroscore_uncalibrated_values);
                break;
            case Sensor.TYPE_HEART_RATE:
                labels = context.getResources().getStringArray(R.array.heart_rate_values);
                break;
            case Sensor.TYPE_LIGHT:
                labels = context.getResources().getStringArray(R.array.light_values);
                break;
            case Sensor.TYPE_LINEAR_ACCELERATION:
                labels = context.getResources().getStringArray(R.array.linear_acceleration_values);
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                labels = context.getResources().getStringArray(R.array.magnetic_values);
                break;
            case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
                labels = context.getResources().getStringArray(R.array.magnetic_field_uncalibrated_values);
                break;
            case Sensor.TYPE_PRESSURE:
                labels = context.getResources().getStringArray(R.array.pressure_values);
                break;
            case Sensor.TYPE_PROXIMITY:
                labels = context.getResources().getStringArray(R.array.proximity_values);
                break;
            case Sensor.TYPE_RELATIVE_HUMIDITY:
                labels = context.getResources().getStringArray(R.array.relative_humidity_values);
                break;
            case Sensor.TYPE_ROTATION_VECTOR:
                labels = context.getResources().getStringArray(R.array.rotation_vector_values);
                break;
            case Sensor.TYPE_STEP_COUNTER:
                labels = context.getResources().getStringArray(R.array.step_counter_values);
                break;
            default:
                labels = new String[]{};
        }
        return labels;
    }

    //All are sorta guesstimations and what I know about some sensors.
    public static float getMinimumValueForSensor(Sensor sensor) {
        float minimumValue;
        switch (sensor.getType()) {
            case Sensor.TYPE_HEART_RATE:
            case Sensor.TYPE_LIGHT:
            case Sensor.TYPE_PROXIMITY:
            case Sensor.TYPE_STEP_COUNTER:
            case Sensor.TYPE_PRESSURE:
                minimumValue = 0;
                break;
            default:
                minimumValue = -sensor.getMaximumRange();
        }
        return minimumValue;
    }

    public static String generateUniqueSensorKey(Sensor sensor) {
        return sensor.getName()
                + "|"
                + sensor.getVendor()
                + "|"
                + sensor.getVersion()
                + "|"
                + sensor.getType();
    }

    public static String generateUniqueSensorMockValuesKey(Sensor sensor) {
        return generateUniqueSensorKey(sensor) + "_values";
    }

    public static String generateUniqueSensorPackageBasedKey(Sensor sensor, String packageName, boolean whitelisted) {
        return generateUniqueSensorKey(sensor) + '_' + packageName + ((whitelisted) ? "_whitelisted" : "_blacklisted");
    }

    public static Sensor getSensorFromUniqueSensorKey(Context context, String key) {
        String[] data = key.split("\\|"); //Regex, man. Regex.
        if (data.length >= 4) {
            try {
                String name = data[0];
                String vendor = data[1];
                int version = Integer.parseInt(data[2]);
                int type = Integer.parseInt(data[3]);
                SensorManager manager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
                for (Sensor sensor : manager.getSensorList(type)) {
                    if (sensor.getName().equals(name)
                            && sensor.getVendor().equals(vendor)
                            && sensor.getVersion() == version) {
                        return sensor;
                    }
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Unable to get unique sensor from key.");
            }
        }
        throw new IllegalArgumentException("Unable to get unique sensor from key.");
    }

    public static String getHumanStringType(Sensor sensor) {
        switch (sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                return "Accelerometer";

            case Sensor.TYPE_AMBIENT_TEMPERATURE:
                return "Ambient Temperature";

            case Sensor.TYPE_GAME_ROTATION_VECTOR:
                return "Game Rotation Vector";

            case Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR:
                return "Geomagnetic Rotation Vector";

            case Sensor.TYPE_GRAVITY:
                return "Gravity";

            case Sensor.TYPE_GYROSCOPE:
                return "Gyroscope";

            case Sensor.TYPE_GYROSCOPE_UNCALIBRATED:
                return "Gyroscope (Uncalibrated)";

            case Sensor.TYPE_HEART_RATE:
                return "Heart Rate";

            case Sensor.TYPE_LIGHT:
                return "Light";

            case Sensor.TYPE_LINEAR_ACCELERATION:
                return "Linear Acceleration";

            case Sensor.TYPE_MAGNETIC_FIELD:
                return "Magnetic Field";

            case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
                return "Magnetic Field (Uncalibrated)";

            case Sensor.TYPE_PRESSURE:
                return "Pressure";

            case Sensor.TYPE_PROXIMITY:
                return "Proximity";

            case Sensor.TYPE_RELATIVE_HUMIDITY:
                return "Relative Humidity";

            case Sensor.TYPE_ROTATION_VECTOR:
                return "Rotation Vector";

            case Sensor.TYPE_SIGNIFICANT_MOTION:
                return "Significant Motion";

            case Sensor.TYPE_STEP_COUNTER:
                return "Step Counter";

            case Sensor.TYPE_STEP_DETECTOR:
                return "Step Detector";

            case Sensor.TYPE_ORIENTATION:
                return "Orientation";

            case Sensor.TYPE_TEMPERATURE:
                return "Temperature";
        }
        return null;
    }

    public static boolean isDangerousSensor(Sensor sensor) {
        switch (sensor.getType()) {
            case Sensor.TYPE_LIGHT:
            case Sensor.TYPE_ACCELEROMETER:
            case Sensor.TYPE_ORIENTATION:
            case Sensor.TYPE_GYROSCOPE:
            case Sensor.TYPE_GYROSCOPE_UNCALIBRATED:
                return true;
            default:
                //Don't have a human string type? Safer to say that it's possibly dangerous.
                return getHumanStringType(sensor) == null;
        }
    }
}
