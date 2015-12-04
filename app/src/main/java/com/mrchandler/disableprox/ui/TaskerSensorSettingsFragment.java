package com.mrchandler.disableprox.ui;

import android.hardware.Sensor;
import android.os.Bundle;
import android.widget.Toast;

import com.mrchandler.disableprox.util.SensorUtil;

/**
 * @author Wardell
 */
public class TaskerSensorSettingsFragment extends SensorSettingsFragment {


    public static TaskerSensorSettingsFragment newInstance(Sensor sensor) {
        TaskerSensorSettingsFragment fragment = new TaskerSensorSettingsFragment();
        Bundle bundle = new Bundle(1);
        //Sensors aren't parcelable or serializable, so we have to do this. I hate it.
        bundle.putString(UNIQUE_SENSOR_KEY, SensorUtil.generateUniqueSensorKey(sensor));
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    void saveSettings() {
        Toast.makeText(getContext(), "Settings for Tasker saved successfully.", Toast.LENGTH_SHORT).show();
        getActivity().finish();
    }
}
