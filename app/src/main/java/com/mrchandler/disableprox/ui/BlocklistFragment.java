package com.mrchandler.disableprox.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.mrchandler.disableprox.R;
import com.mrchandler.disableprox.util.Constants;
import com.mrchandler.disableprox.util.SensorUtil;

import java.util.ArrayList;

/**
 * @author Wardell
 */
public class BlocklistFragment extends Fragment {

    private static final String APP_PACKAGE = "appPackage";
    private static final String APP_LABEL = "appLabel";
    private static final String WHITELIST = "whitelist";

    private ImageView appIcon;
    private ArrayList<CheckableSensor> checkableSensors;
    private RecyclerView.Adapter<SensorViewHolder> sensorAdapter;
    private String packageName;
    private boolean whitelisted;


    public static BlocklistFragment newInstance(String appPackage, String appLabel, boolean whitelist) {
        Bundle arguments = new Bundle(2);
        arguments.putString(APP_PACKAGE, appPackage);
        arguments.putString(APP_LABEL, appLabel);
        arguments.putBoolean(WHITELIST, whitelist);
        BlocklistFragment blocklistFragment = new BlocklistFragment();
        blocklistFragment.setArguments(arguments);
        return blocklistFragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.blacklist_dialog_fragment_layout, container, false);
        final RecyclerView sensorListView = (RecyclerView) rootView.findViewById(R.id.sensor_recycler_view);
        sensorListView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        sensorListView.addItemDecoration(new SimpleDividerItemDecoration(getActivity()));
        appIcon = (ImageView) rootView.findViewById(R.id.app_icon);
        TextView appLabel = (TextView) rootView.findViewById(R.id.app_label);
        CheckBox checkAll = (CheckBox) rootView.findViewById(R.id.blacklist_check_all);

        if (getArguments() != null) {
            Bundle arguments = getArguments();
            packageName = arguments.getString(APP_PACKAGE);
            setAppIcon(packageName);
            appLabel.setText(arguments.getString(APP_LABEL));
            whitelisted = arguments.getBoolean(WHITELIST);
            if (whitelisted) {
                getActivity().setTitle(R.string.whitelist_title);
            } else {
                getActivity().setTitle(R.string.blacklist_title);
            }
        }
        SensorManager sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        checkableSensors = new ArrayList<>();
        for (Sensor s : sensorManager.getSensorList(Sensor.TYPE_ALL)) {
            checkableSensors.add(new CheckableSensor(s, getStatus(s)));
        }
        sensorListView.setAdapter(sensorAdapter = new RecyclerView.Adapter<SensorViewHolder>() {
            @Override
            public SensorViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.single_blacklist_item_layout, parent, false);
                return new SensorViewHolder(view);
            }

            @Override
            public void onBindViewHolder(SensorViewHolder holder, int position) {
                holder.checkBox.setChecked(checkableSensors.get(position).isChecked);
                String label = SensorUtil.getHumanStringType(checkableSensors.get(position).sensor);
                if (label == null) {
                    label = checkableSensors.get(position).sensor.getName();
                }

                holder.checkBox.setText(label);
                holder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        int currentPosition = sensorListView.getChildAdapterPosition(buttonView);
                        checkableSensors.get(currentPosition).isChecked = isChecked;
                        saveStatus(isChecked, currentPosition);
                    }
                });
            }

            @Override
            public int getItemCount() {
                return checkableSensors.size();
            }
        });

        checkAll.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                for (CheckableSensor s : checkableSensors) {
                    s.isChecked = isChecked;
                }
                sensorAdapter.notifyDataSetChanged();

            }
        });

        return rootView;
    }

    private boolean getStatus(Sensor sensor) {
        SharedPreferences prefs = getActivity().getSharedPreferences(Constants.PREFS_FILE_NAME, Context.MODE_WORLD_READABLE);
        String key = SensorUtil.generateUniqueSensorPackageBasedKey(sensor, packageName, whitelisted);
        return prefs.getBoolean(key, false);
    }

    private void saveStatus(boolean newStatus, int position) {
        SharedPreferences prefs = getActivity().getSharedPreferences(Constants.PREFS_FILE_NAME, Context.MODE_WORLD_READABLE);
        CheckableSensor checkableSensor = checkableSensors.get(position);
        String key = SensorUtil.generateUniqueSensorPackageBasedKey(checkableSensor.sensor, packageName, whitelisted);
        prefs.edit().putBoolean(key, newStatus).apply();
    }

    private void setAppIcon(final String appPackage) {
        Context context = getActivity();
        new AsyncTask<Context, Void, Drawable>() {

            @Override
            protected Drawable doInBackground(Context... params) {
                if (params[0] != null) {
                    Context context = params[0];
                    PackageManager packageManager = context.getPackageManager();
                    try {
                        return packageManager.getApplicationIcon(appPackage);
                    } catch (PackageManager.NameNotFoundException e) {
                        return null;
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(Drawable drawable) {
                if (appIcon != null) {
                    appIcon.setImageDrawable(drawable);
                }
            }
        }.execute(context);
    }

    private static class SensorViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkBox;

        public SensorViewHolder(View itemView) {
            super(itemView);
            checkBox = (CheckBox) itemView;
        }
    }

    private static class CheckableSensor {
        Sensor sensor;
        boolean isChecked;

        public CheckableSensor(Sensor sensor, boolean isChecked) {
            this.sensor = sensor;
            this.isChecked = isChecked;
        }
    }
}
