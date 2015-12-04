package com.mrchandler.disableprox.ui;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;


public class SensorListFragment extends ListFragment {

    private OnSensorClickedListener mListener;

    public static SensorListFragment newInstance() {
        return new SensorListFragment();
    }

    public SensorListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SensorManager manager = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
        //TODO This list adapter should share its sensor list with its Activity.
        setListAdapter(new ArrayAdapter<Sensor>(getContext(),
                android.R.layout.simple_list_item_1, android.R.id.text1, manager.getSensorList(Sensor.TYPE_ALL)) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view;
                if (convertView != null) {
                    view = (TextView) convertView;
                } else {
                    view = (TextView) LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
                }
                view.setText(getItem(position).getName());
                return view;
            }
        });
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
