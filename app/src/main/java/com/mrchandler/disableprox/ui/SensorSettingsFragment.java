package com.mrchandler.disableprox.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mrchandler.disableprox.R;
import com.mrchandler.disableprox.util.Constants;
import com.mrchandler.disableprox.util.SensorUtil;

import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Wardell
 */
public class SensorSettingsFragment extends Fragment {

    public static String UNIQUE_SENSOR_KEY = "sensorKey";

    List<DiscreteSeekBar> valueSettings = new ArrayList<>();
    float maximumValue;
    float minimumValue;
    Sensor sensor;
    String[] labels;
    RadioGroup radioGroup;


    public static SensorSettingsFragment newInstance(Sensor sensor) {
        SensorSettingsFragment fragment = new SensorSettingsFragment();
        Bundle bundle = new Bundle(1);
        //Sensors aren't parcelable or serializable, so we have to do this. I hate it.
        bundle.putString(UNIQUE_SENSOR_KEY, SensorUtil.generateUniqueSensorKey(sensor));
        fragment.setArguments(bundle);
        fragment.setHasOptionsMenu(true);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (sensor == null) {
            sensor = SensorUtil.getSensorFromUniqueSensorKey(context, getArguments().getString(UNIQUE_SENSOR_KEY));
        }
        maximumValue = sensor.getMaximumRange();
        minimumValue = SensorUtil.getMinimumValueForSensor(sensor);
        labels = SensorUtil.getLabelsForSensor(context, sensor);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //If this fragment is already created, don't recreate it unnecessarily.
        if (getView() != null) {
            return getView();
        }
        LinearLayout rootView = new LinearLayout(getContext());
        rootView.setOrientation(LinearLayout.VERTICAL);
        rootView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        radioGroup = (RadioGroup) LayoutInflater.from(getContext()).inflate(R.layout.single_sensor_value_setting_header, rootView, false);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.mock_sensor_values_radio_button) {
                    for (DiscreteSeekBar seekBar : valueSettings) {
                        seekBar.setEnabled(true);
                    }
                } else {
                    for (DiscreteSeekBar seekBar : valueSettings) {
                        seekBar.setEnabled(false);
                    }
                }
            }
        });
        rootView.addView(radioGroup);
        if (labels.length == 0) {
            radioGroup.findViewById(R.id.mock_sensor_values_radio_button).setEnabled(false);
        }
        valueSettings = new ArrayList<>();
        for (String label : labels) {
            RelativeLayout layout = (RelativeLayout) LayoutInflater.from(getContext()).inflate(R.layout.single_sensor_value_setting, rootView, false);
            TextView textView = (TextView) layout.findViewById(R.id.label);
            textView.setText(label);
            /*There was an issue before where all seekbars would have the same value after rotation.
            I originally fixed this by assigning a random ID to all seekbars but that brought in more issues
            and ended up being more hassle than it was worth.
             */
            DiscreteSeekBar seekBar = (DiscreteSeekBar) layout.findViewById(R.id.value);
            seekBar.setMin((int) minimumValue * 10);
            seekBar.setMax((int) maximumValue * 10);
            seekBar.setNumericTransformer(new DiscreteSeekBar.NumericTransformer() {
                @Override
                public int transform(int value) {
                    return value;
                }

                @Override
                public String transformToString(int value) {
                    return "" + (value / 10.0f);
                }

                @Override
                public boolean useStringTransform() {
                    return true;
                }
            });
            valueSettings.add(seekBar);
            rootView.addView(layout);
        }
        loadDefaultValues();
        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.sensor_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.save:
                saveToPrefs();
                break;
            case R.id.info:
                showInfoDialog();
                break;
            case R.id.reset:
                loadDefaultValues();
                break;
            default:
                super.onOptionsItemSelected(item);
        }
        return super.onOptionsItemSelected(item);
    }

    protected void showInfoDialog() {
        new AlertDialog.Builder(getContext())
                .setTitle("Sensor Information")
                .setMessage("Name: " + sensor.getName() + "\n"
                        + "Type: " + SensorUtil.getHumanStringType(sensor) + "\n"
                        + "Vendor: " + sensor.getVendor() + "\n"
                        + "Range: " + sensor.getMaximumRange() + "\n")
                .show();
    }

    protected void saveToPrefs() {
        if (getContext() != null) {
            String enabledStatusKey = SensorUtil.generateUniqueSensorKey(sensor);
            int enabledStatusValue;
            String mockValuesKey = SensorUtil.generateUniqueSensorKey(sensor) + "_values";
            String mockValuesValues = "";
            for (float value : getValues()) {
                mockValuesValues += value + ":";
            }
            switch (radioGroup.getCheckedRadioButtonId()) {
                default:
                case R.id.do_nothing_radio_button:
                    enabledStatusValue = Constants.DO_NOTHING;
                    break;
                case R.id.remove_sensor_radio_button:
                    enabledStatusValue = Constants.REMOVE_SENSOR;
                    break;
                case R.id.mock_sensor_values_radio_button:
                    enabledStatusValue = Constants.MOCK_VALUES;
                    break;
            }
            SharedPreferences prefs = getContext().getSharedPreferences(Constants.PREFS_FILE_NAME, Context.MODE_WORLD_READABLE);
            prefs.edit()
                    .putInt(enabledStatusKey, enabledStatusValue)
                    .putString(mockValuesKey, mockValuesValues)
                    .apply();
            Toast.makeText(getContext(), "Settings saved for sensor " + sensor.getName() + ".", Toast.LENGTH_SHORT).show();
        }
    }

    protected void loadDefaultValues() {
        if (getContext() != null) {
            SharedPreferences prefs = getContext().getSharedPreferences(Constants.PREFS_FILE_NAME, Context.MODE_WORLD_READABLE);
            String enabledStatusKey = SensorUtil.generateUniqueSensorKey(sensor);
            String mockValuesKey = SensorUtil.generateUniqueSensorKey(sensor) + "_values";
            int enabledStatus = prefs.getInt(enabledStatusKey, Constants.DO_NOTHING);
            //TODO Check for existence in prefs before creating a default.
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < valueSettings.size(); i++) {
                builder.append((valueSettings.get(i).getMin() / 10.0f));
                if (i != valueSettings.size() - 1) {
                    builder.append(':');
                }
            }
            String defaultValue = builder.toString();
            String[] mockValuesStringArray;
            if ("".equals(defaultValue)) {
                mockValuesStringArray = new String[0];
            } else {
                mockValuesStringArray = prefs.getString(mockValuesKey, defaultValue).split(":", 0);
            }
            float[] mockValues = new float[mockValuesStringArray.length];
            for (int i = 0; i < mockValuesStringArray.length; i++) {
                mockValues[i] = Float.parseFloat(mockValuesStringArray[i]);
            }
            switch (enabledStatus) {
                default:
                case Constants.DO_NOTHING:
                    radioGroup.check(R.id.do_nothing_radio_button);
                    break;
                case Constants.REMOVE_SENSOR:
                    radioGroup.check(R.id.remove_sensor_radio_button);
                    break;
                case Constants.MOCK_VALUES:
                    radioGroup.check(R.id.mock_sensor_values_radio_button);
                    break;
            }
            setValues(mockValues);
        }
    }

    public float[] getValues() {
        float[] values = new float[valueSettings.size()];
        for (int i = 0; i < valueSettings.size(); i++) {
            DiscreteSeekBar bar = valueSettings.get(i);
            //For exact matching where we might have lost precision.
            if (bar.getProgress() == bar.getMax()) {
                values[i] = maximumValue;
            } else {
                values[i] = bar.getProgress() / 10.0f;
            }
        }
        return values;
    }

    public void setValues(float[] values) {
        for (int i = 0; i < valueSettings.size(); i++) {
            valueSettings.get(i).setProgress((int) (values[i] * 10));
        }
    }
}
