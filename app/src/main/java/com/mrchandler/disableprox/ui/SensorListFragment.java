package com.mrchandler.disableprox.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.mrchandler.disableprox.R;
import com.mrchandler.disableprox.util.SensorUtil;

import java.util.List;


public class SensorListFragment extends ListFragment {

    private OnSensorClickedListener mListener;

    public static SensorListFragment newInstance() {
        return new SensorListFragment();
    }

    public SensorListFragment() {
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (OnSensorClickedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.getClass().getSimpleName()
                    + " must implement OnSensorClickedListener");
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setEmptyText("No sensors available on your device.");
        ListView listView = getListView();
        listView.setDivider(null);
        listView.setScrollbarFadingEnabled(false);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void setSensors(List<Sensor> sensorList) {
        setListAdapter(new ArrayAdapter<Sensor>(getContext(),
                R.layout.sensor_list_item, sensorList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView textView;
                if (convertView != null) {
                    textView = (TextView) convertView;
                } else {
                    textView = (TextView) LayoutInflater.from(parent.getContext()).inflate(R.layout.sensor_list_item, parent, false);
                }

                Sensor sensor = getItem(position);
                String title = SensorUtil.getHumanStringType(sensor);
                if (title == null) {
                    title = sensor.getName();
                }
                textView.setText(title);
                if (SensorUtil.isDangerousSensor(sensor)) {
                    Drawable drawable = getResources().getDrawable(R.drawable.ic_error_outline_white_24dp);
                    if (drawable != null) {
                        drawable.setColorFilter(Color.RED, PorterDuff.Mode.SRC_ATOP);
                    }
                    textView.setCompoundDrawablesWithIntrinsicBounds(null, null, drawable, null);
                } else {
                    textView.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
                }
                return textView;
            }
        });
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        if (null != mListener) {
            mListener.onSensorClicked((Sensor) l.getAdapter().getItem(position));
        }
    }

    public interface OnSensorClickedListener {
        void onSensorClicked(Sensor sensor);
    }

}
