package com.mrchandler.disableprox.ui;

import android.hardware.Sensor;
import android.os.Bundle;
import android.widget.Toast;

import com.mrchandler.disableprox.util.SensorUtil;

/**
 * @author Wardell
 */
public class TaskerSensorSettingsFragment extends SensorSettingsFragment {

    public static final String ENABLED_STATUS = "enabledStatus";
    public static final String MOCK_VALUES = "mockValues";

    public static TaskerSensorSettingsFragment newInstance(String sensorKey, int enabledStatus, float[] mockValues) {
        TaskerSensorSettingsFragment fragment = new TaskerSensorSettingsFragment();
        Bundle bundle = new Bundle(1);
        bundle.putString(UNIQUE_SENSOR_KEY, sensorKey);
        bundle.putInt(ENABLED_STATUS, enabledStatus);
        bundle.putFloatArray(MOCK_VALUES, mockValues);
        fragment.setArguments(bundle);
        return fragment;
    }

    public static TaskerSensorSettingsFragment newInstance(Sensor sensor) {
        TaskerSensorSettingsFragment fragment = new TaskerSensorSettingsFragment();
        Bundle bundle = new Bundle(1);
        //Sensors aren't parcelable or serializable, so we have to do this. I hate it.
        bundle.putString(UNIQUE_SENSOR_KEY, SensorUtil.generateUniqueSensorKey(sensor));
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void loadDefaultValues() {
        Bundle settings = getArguments();
        if (settings != null && settings.containsKey(ENABLED_STATUS) && settings.containsKey(MOCK_VALUES)) {
            setSensorStatus(settings.getInt(ENABLED_STATUS));
            setValues(settings.getFloatArray(MOCK_VALUES));
        } else {
            super.loadDefaultValues();
        }
    }

    @Override
    void saveSettings() {
        Toast.makeText(getContext(), "Settings for Tasker saved successfully.", Toast.LENGTH_SHORT).show();
        getActivity().finish();
    }
}
