package com.mrchandler.disableprox.ui;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.Spinner;
import android.widget.TextView;

import com.github.jlmd.animatedcircleloadingview.AnimatedCircleLoadingView;
import com.mrchandler.disableprox.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Wardell
 */
public class AppSettingsFragment extends Fragment {
    private List<ApplicationInfoWrapper> installedApps = new ArrayList<>();
    private PackageManager packageManager;
    private ArrayAdapter<ApplicationInfoWrapper> adapter;
    private ListView appListView;
    private AnimatedCircleLoadingView progressBar;

    public AppSettingsFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.app_settings_fragment_layout, container, false);
        packageManager = getContext().getPackageManager();
        appListView = (ListView) rootView.findViewById(R.id.app_recycler_view);
        appListView.setFastScrollEnabled(true);
        progressBar = (AnimatedCircleLoadingView) rootView.findViewById(R.id.progress_bar);
        Spinner spinner = (Spinner) rootView.findViewById(R.id.apps_spinner);
        spinner.setAdapter(new ArrayAdapter<String>(getContext(), android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.app_spinner)) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_spinner_item, parent, false);
                }
                TextView textView = (TextView) convertView;
                textView.setText(getItem(position));
                textView.setTextAppearance(getContext(), android.R.style.TextAppearance_DeviceDefault_Widget_ActionBar_Title);
                return textView;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_spinner_dropdown_item, parent, false);
                }
                ((TextView) convertView).setText(getItem(position));
                return convertView;
            }
        });
        createInstalledAppsList();
        return rootView;
    }

    private void createInstalledAppsList() {
        new AsyncTask<Void, Integer, Void>() {

            List<ApplicationInfo> applicationInfos = packageManager.getInstalledApplications(0);

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                appListView.setVisibility(View.INVISIBLE);
                progressBar.setVisibility(View.VISIBLE);
                progressBar.startDeterminate();
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                progressBar.setPercent((int) (((values[0] * 1.0f) / applicationInfos.size()) * 100));
            }

            @Override
            protected Void doInBackground(Void... params) {

                for (int i = 0; i < applicationInfos.size(); i++) {
                    ApplicationInfo info = applicationInfos.get(i);
                    Drawable icon = info.loadIcon(packageManager);
                    CharSequence label = info.loadLabel(packageManager);
                    installedApps.add(new ApplicationInfoWrapper(icon, label, info));
                    publishProgress(i);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                Collections.sort(installedApps);
                appListView.animate().alpha(100f).setDuration(250).start();
                progressBar.animate().alpha(0f).setDuration(250).start();
                appListView.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
                setAdapter();
            }
        }.execute();
    }

    private void setAdapter() {
        adapter = new AppViewHolderAdapter(getContext(), android.R.layout.simple_list_item_1, installedApps);
        appListView.setAdapter(adapter);
    }

    class ApplicationInfoWrapper implements Comparable<ApplicationInfoWrapper> {
        Drawable icon;
        CharSequence label;
        ApplicationInfo applicationInfo;

        public ApplicationInfoWrapper(Drawable icon, CharSequence label, ApplicationInfo applicationInfo) {
            this.icon = icon;
            if (label != null) {
                this.label = label;
            } else {
                this.label = "Unknown";
            }
            this.applicationInfo = applicationInfo;

        }


        @Override
        public int compareTo(@NonNull ApplicationInfoWrapper another) {
            return label.toString().compareTo(another.label.toString());
        }
    }

    class AppViewHolder extends RecyclerView.ViewHolder {
        TextView label;
        ImageView icon;

        public AppViewHolder(View itemView) {
            super(itemView);
            label = (TextView) itemView.findViewById(R.id.app_label);
            icon = (ImageView) itemView.findViewById(R.id.app_icon);
        }
    }

    class AppViewHolderAdapter extends ArrayAdapter<ApplicationInfoWrapper> implements SectionIndexer {

        Character[] sectionsToChar = new Character[0];

        public AppViewHolderAdapter(Context context, int resource, List<ApplicationInfoWrapper> objects) {
            super(context, resource, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            AppViewHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.single_app_layout, parent, false);
                holder = new AppViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (AppViewHolder) convertView.getTag();
            }
            holder.label.setText(getItem(position).label);
            holder.icon.setImageDrawable(getItem(position).icon);
            return convertView;
        }

        @Override
        public Object[] getSections() {
            ArrayList<Character> sections = new ArrayList<>();
            for (ApplicationInfoWrapper wrapper : installedApps) {
                Character firstCharacter = wrapper.label.toString().charAt(0);
                if (!sections.contains(firstCharacter)) {
                    sections.add(firstCharacter);
                }
            }
            return sectionsToChar = sections.toArray(sectionsToChar);
        }

        @Override
        public int getPositionForSection(int sectionIndex) {
            for (int position = 0; position < installedApps.size(); position++) {
                Character firstCharacter = getItem(position).label.toString().charAt(0);
                if (firstCharacter.equals(sectionsToChar[sectionIndex])) {
                    return position;
                }
            }
            return 0;
        }

        @Override
        public int getSectionForPosition(int position) {
            Character firstCharacter = getItem(position).label.toString().charAt(0);
            for (int section = 0; section < sectionsToChar.length; section++) {
                if (firstCharacter.equals(sectionsToChar[section])) {
                    return section;
                }
            }
            return 0;
        }
    }
}
