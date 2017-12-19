package com.mrchandler.disableprox.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
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
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.sensor_setting_fragment_layout, container, false);
        LinearLayout linearLayout = (LinearLayout) rootView.findViewById(R.id.content_view);
        if (!SensorUtil.isDangerousSensor(sensor)) {
            rootView.findViewById(R.id.dangerous_sensor_text_view).setVisibility(View.GONE);
        }
        radioGroup = (RadioGroup) LayoutInflater.from(getContext()).inflate(R.layout.single_sensor_value_setting_header, linearLayout, false);
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
        linearLayout.addView(radioGroup);
        if (labels.length == 0) {
            radioGroup.findViewById(R.id.mock_sensor_values_radio_button).setEnabled(false);
        }
        valueSettings = new ArrayList<>();

        int accentColor = getResources().getColor(R.color.primary_accent);
        int[][] states = new int[][]{
                new int[]{android.R.attr.state_enabled},
                new int[]{}
        };
        int[] colors = new int[]{
                accentColor,
                Color.GRAY
        };
        ColorStateList stateList = new ColorStateList(states, colors);
        for (String label : labels) {
            RelativeLayout layout = (RelativeLayout) LayoutInflater.from(getContext()).inflate(R.layout.single_sensor_value_setting, linearLayout, false);
            TextView textView = (TextView) layout.findViewById(R.id.label);
            textView.setText(label);
            /*There was an issue before where all seekbars would have the same value after rotation.
            I originally fixed this by assigning a random ID to all seekbars but that brought in more issues
            and ended up being more hassle than it was worth.
             */
            DiscreteSeekBar seekBar = (DiscreteSeekBar) layout.findViewById(R.id.value);
            seekBar.setScrubberColor(stateList);
            seekBar.setThumbColor(stateList, accentColor);
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
            linearLayout.addView(layout);
        }
        loadValues();
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
            case R.id.info:
                showInfoDialog();
                break;
            case R.id.reset:
                loadValues();
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
                        + "Range: " + sensor.getMaximumRange() + "\n\n"
                        + SensorUtil.getDescription(sensor) + "\n")
                .show();
    }

    void saveSettings() {
        if (getContext() != null) {
            String enabledStatusKey = SensorUtil.generateUniqueSensorKey(sensor);
            int enabledStatusValue = getSensorStatus();
            String mockedValuesPrefsKey = SensorUtil.generateUniqueSensorMockValuesKey(sensor);
            StringBuilder mockValuesPrefsValue = new StringBuilder();
            float[] currentValues = getValues();
            if (currentValues.length > 0) {
                for (float value : getValues()) {
                    mockValuesPrefsValue.append(value).append(":");
                }
            }
            SharedPreferences prefs = getContext().getSharedPreferences(Constants.PREFS_FILE_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit().putInt(enabledStatusKey, enabledStatusValue);
            if (mockValuesPrefsValue.length() > 0)
                editor.putString(mockedValuesPrefsKey, mockValuesPrefsValue.toString());
            editor.apply();

            Toast.makeText(getContext(), "Settings saved for sensor \"" + sensor.getName() + "\".", Toast.LENGTH_SHORT).show();
        }
    }

    protected void loadValues() {
        if (getContext() != null) {
            SharedPreferences prefs = getContext().getSharedPreferences(Constants.PREFS_FILE_NAME, Context.MODE_PRIVATE);
            String enabledStatusKey = SensorUtil.generateUniqueSensorKey(sensor);
            String mockedValuesKey = SensorUtil.generateUniqueSensorMockValuesKey(sensor);
            String[] mockValuesStringArray;
            int enabledStatus = prefs.getInt(enabledStatusKey, Constants.SENSOR_STATUS_DO_NOTHING);

            if (prefs.contains(mockedValuesKey)) {
                String prefsMockedValues = prefs.getString(mockedValuesKey, "");
                if (prefsMockedValues.isEmpty()) {
                    mockValuesStringArray = getDefaultMockedValues();
                } else {
                    mockValuesStringArray = prefsMockedValues.split(":", 0);
                }
            } else {
                mockValuesStringArray = getDefaultMockedValues();
            }

            //TODO This could be better.
            float[] mockValues = new float[mockValuesStringArray.length];
            for (int i = 0; i < mockValuesStringArray.length; i++) {
                mockValues[i] = Float.parseFloat(mockValuesStringArray[i]);
            }
            switch (enabledStatus) {
                default:
                case Constants.SENSOR_STATUS_DO_NOTHING:
                    radioGroup.check(R.id.do_nothing_radio_button);
                    break;
                case Constants.SENSOR_STATUS_REMOVE_SENSOR:
                    radioGroup.check(R.id.remove_sensor_radio_button);
                    break;
                case Constants.SENSOR_STATUS_MOCK_VALUES:
                    radioGroup.check(R.id.mock_sensor_values_radio_button);
                    break;
            }
            setValues(mockValues);
        }
    }

    private String[] getDefaultMockedValues() {
        String[] defaultValues = new String[valueSettings.size()];
        for (int i = 0; i < valueSettings.size(); i++) {
            defaultValues[i] = "" + (valueSettings.get(i).getMin() / 10.0f);
        }
        return defaultValues;
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

    @Override
    public void onResume() {
        super.onResume();
        setHasOptionsMenu(true);
    }

    public void setValues(float[] values) {
        for (int i = 0; i < Math.min(values.length, valueSettings.size()); i++) {
            valueSettings.get(i).setProgress((int) (values[i] * 10));
        }
    }

    public int getSensorStatus() {
        switch (radioGroup.getCheckedRadioButtonId()) {
            default:
            case R.id.do_nothing_radio_button:
                return Constants.SENSOR_STATUS_DO_NOTHING;
            case R.id.remove_sensor_radio_button:
                return Constants.SENSOR_STATUS_REMOVE_SENSOR;
            case R.id.mock_sensor_values_radio_button:
                return Constants.SENSOR_STATUS_MOCK_VALUES;
        }
    }

    public void setSensorStatus(int status) {
        if (radioGroup != null) {
            switch (status) {
                default:
                case Constants.SENSOR_STATUS_DO_NOTHING:
                    radioGroup.check(R.id.do_nothing_radio_button);
                    break;
                case Constants.SENSOR_STATUS_REMOVE_SENSOR:
                    radioGroup.check(R.id.remove_sensor_radio_button);
                    break;
                case Constants.SENSOR_STATUS_MOCK_VALUES:
                    radioGroup.check(R.id.mock_sensor_values_radio_button);
                    break;
            }
        }
    }
}
