package com.mrchandler.disableprox.util;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.mrchandler.disableprox.R;

import java.util.regex.Pattern;

/**
 * @author Wardell
 */
public final class SensorUtil {

    private static final String SENSOR_SEPARATOR = "|*^&SensorDisabler&^*|";

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
        //TODO Maybe use Sensor.toString?
        return sensor.getName()
                + SENSOR_SEPARATOR
                + sensor.getVendor()
                + SENSOR_SEPARATOR
                + sensor.getVersion()
                + SENSOR_SEPARATOR
                + sensor.getType();
    }

    public static String generateUniqueSensorMockValuesKey(Sensor sensor) {
        return generateUniqueSensorKey(sensor) + "_values";
    }

    public static String generateUniqueSensorPackageBasedKey(Sensor sensor, String packageName, BlocklistType type) {
        //TODO Expand this out if I ever come up with some sort of extra kind of blocking list.
        return generateUniqueSensorKey(sensor) + '_' + packageName + ((type == BlocklistType.WHITELIST) ? "_whitelisted" : "_blacklisted");
    }

    public static Sensor getSensorFromUniqueSensorKey(Context context, String key) {
        String[] data = key.split(Pattern.quote(SENSOR_SEPARATOR)); //Regex, man. Regex.
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
        } else {
            Log.e("SensorUtil", "Unable to parse \"" + key + "\" as Sensor data.");
        }
        return null;
    }

    @Nullable
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

    public static String getDescription(Sensor sensor) {
        switch (sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                return "Measures the acceleration force in m/s² that is applied to a device on all three physical axes (x, y, and z), including the force of gravity.";

            case Sensor.TYPE_AMBIENT_TEMPERATURE:
                return "Measures the ambient room temperature in degrees Celsius (°C).";

            case Sensor.TYPE_GRAVITY:
                return "Measures the force of gravity in m/s² that is applied to a device on all three physical axes (x, y, z).";

            case Sensor.TYPE_GYROSCOPE:
                return "Measures a device's rate of rotation in rad/s around each of the three physical axes (x, y, and z).";

            case Sensor.TYPE_HEART_RATE:
                return "Measures heart rate.";

            case Sensor.TYPE_LIGHT:
                return "Measures the ambient light level (illumination) in lx.";

            case Sensor.TYPE_LINEAR_ACCELERATION:
                return "Measures the acceleration force in m/s² that is applied to a device on all three physical axes (x, y, and z), excluding the force of gravity.";

            case Sensor.TYPE_MAGNETIC_FIELD:
                return "Measures the ambient geomagnetic field for all three physical axes (x, y, z) in μT.";

            case Sensor.TYPE_PRESSURE:
                return "Measures the ambient air pressure in hPa or mbar.";

            case Sensor.TYPE_PROXIMITY:
                return "Measures the proximity of an object in cm relative to the view screen of a device. This sensor is typically used to determine whether a handset is being held up to a person's ear.";

            case Sensor.TYPE_RELATIVE_HUMIDITY:
                return "Measures the relative ambient humidity in percent (%).";

            case Sensor.TYPE_ROTATION_VECTOR:
                return "Measures the orientation of a device by providing the three elements of the device's rotation vector.";

            case Sensor.TYPE_ORIENTATION:
                return "Measures degrees of rotation that a device makes around all three physical axes (x, y, z). ";

            case Sensor.TYPE_TEMPERATURE:
                return "Measures the temperature of the device in degrees Celsius (°C). ";

            default:
                return "Information about this sensor is unavailable.";
        }
    }
}
