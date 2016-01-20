package com.mrchandler.disableprox.ui;

import android.animation.Animator;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.util.Pair;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.github.jlmd.animatedcircleloadingview.AnimatedCircleLoadingView;
import com.mrchandler.disableprox.R;
import com.mrchandler.disableprox.util.Constants;

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
        appListView = (ListView) rootView.findViewById(R.id.app_list_view);
        appListView.setFastScrollEnabled(true);
        progressBar = (AnimatedCircleLoadingView) rootView.findViewById(R.id.progress_bar);
        progressBar.setAnimationListener(new AnimatedCircleLoadingView.AnimationListener() {
            @Override
            public void onAnimationEnd() {
                appListView.animate().alpha(1f).setDuration(500).setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        appListView.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                }).start();
                progressBar.animate().alpha(0f).setDuration(500).setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        progressBar.setVisibility(View.GONE);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                }).start();
            }
        });
        createInstalledAppsList();
        return rootView;
    }

    private void createInstalledAppsList() {
        new AsyncTask<Void, Pair<Integer, ApplicationInfoWrapper>, Void>() {

            List<ApplicationInfo> applicationInfos = packageManager.getInstalledApplications(0);

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                appListView.setVisibility(View.INVISIBLE);
                progressBar.setVisibility(View.VISIBLE);
                progressBar.startDeterminate();
            }

            @SafeVarargs
            @Override
            protected final void onProgressUpdate(Pair<Integer, ApplicationInfoWrapper>... values) {
                progressBar.setPercent((int) (((values[0].first * 1.0f) / (applicationInfos.size() - 2)) * 100));
                installedApps.add(values[0].second);
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
            }

            @SuppressWarnings("unchecked")
            @Override
            protected Void doInBackground(Void... params) {

                for (int i = 0; i < applicationInfos.size(); i++) {
                    ApplicationInfo info = applicationInfos.get(i);
                    Drawable icon = info.loadIcon(packageManager);
                    CharSequence label = info.loadLabel(packageManager);
                    publishProgress(Pair.create(i, new ApplicationInfoWrapper(icon, label, info)));
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                Collections.sort(installedApps);
                setAdapter();
                progressBar.setPercent(100);
            }
        }.execute();
    }


    private void setAdapter() {
        adapter = new AppViewHolderAdapter(getContext(), android.R.layout.simple_list_item_1, installedApps, true);
        appListView.setAdapter(adapter);
        appListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ApplicationInfoWrapper appInfoWrapper = adapter.getItem(position);
                Intent blacklistIntent = new Intent(getActivity(), BlocklistActivity.class);
                blacklistIntent.putExtra(Constants.INTENT_APP_PACKAGE, appInfoWrapper.applicationInfo.packageName);
                blacklistIntent.putExtra(Constants.INTENT_APP_LABEL, appInfoWrapper.label.toString());
                startActivity(blacklistIntent);
            }
        });
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
        boolean showListedStatus;

        public AppViewHolderAdapter(Context context, int resource, List<ApplicationInfoWrapper> objects, boolean showListedStatus) {
            super(context, resource, objects);
            this.showListedStatus = showListedStatus;
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
