package com.mrchandler.disableprox.ui;

import android.view.MenuItem;

import com.mrchandler.disableprox.R;

/**
 * @author Wardell
 */
public class TaskerSensorSettingsFragment extends SensorSettingsFragment {
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.save) {
            getActivity().finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
