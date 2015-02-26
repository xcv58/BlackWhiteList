package org.phone_lab.jouler.blackwhitelist.activities;

import android.app.ListFragment;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import org.phone_lab.jouler.blackwhitelist.R;
import org.phone_lab.jouler.blackwhitelist.services.BlackWhiteListService;
import org.phone_lab.jouler.blackwhitelist.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/**
 * Created by xcv58 on 1/23/15.
 */
public class AppListFragment extends ListFragment {
    protected AppAdapter appAdapter;
    public List<App> appList;
    public List<App> wholeAppList;
    private HashSet<String> duplicateSet;
    private ListView listView;
    private String target;

    public AppListFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(Utils.TAG, "onCreateView");
        View rootView = inflater.inflate(R.layout.app_list, container, false);
        appList = new ArrayList<App>();
        wholeAppList = new ArrayList<App>();
        getAppList(wholeAppList);

        appAdapter = new AppAdapter(getActivity(), appList);
        setListAdapter(appAdapter);

//        listView = (ListView) rootView.findViewWithTag(getString(R.string.list_view_tag));

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(Utils.TAG, "Fragment onResume");
    }

    @Override
    public void onPause() {
        super.onPause();
        // save option for persistence.
        Log.d(Utils.TAG, "AppListFragment onPause Run");
//        if (mBound) {
//            Log.d(Utils.TAG, "mBound is true");
////            iJoulerBaseService.flush();
//            getActivity().unbindService(mConnection);
//        }
    }

    @Override
    public void onListItemClick(android.widget.ListView l, android.view.View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        App app = appList.get(position);
        Log.d(Utils.TAG, "Select" + app.getAppName());
        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity.mBound) {
            Log.d(Utils.TAG, "run service.select");
            mainActivity.mService.select(app);
        }

//        Client client = clientList.get(position);
//        if (client.isSelected()) {
//            iJoulerBaseService.setChoosed(client);
//        } else {
//            iJoulerBaseService.setSelected(client);
//        }
        appAdapter.notifyDataSetChanged();
    }

    private void getAppList(List<App> list) {
        list.clear();
        PackageManager pm = getActivity().getPackageManager();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        getFilteredList(pm.queryIntentActivities(mainIntent, 0), list);
        return;
    }

    private void initDuplicateHashSet() {
        if (duplicateSet == null) {
            duplicateSet = new HashSet<String>();
        } else {
            duplicateSet.clear();
        }
        duplicateSet.add(getActivity().getPackageName());
        duplicateSet.add("com.google.android.launcher");
        duplicateSet.add("com.google.android.googlequicksearchbox");
        PackageManager pm = getActivity().getPackageManager();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_HOME);
        List<ResolveInfo> resolveInfoList = pm.queryIntentActivities(mainIntent, 0);
        for (ResolveInfo resolveInfo : resolveInfoList) {
            duplicateSet.add(resolveInfo.activityInfo.packageName);
        }
    }

    private void getFilteredList(List<ResolveInfo> list, List<App> descList) {
        initDuplicateHashSet();
        for (ResolveInfo resolveInfo : list) {
            String packageName = resolveInfo.activityInfo.packageName;
            if (!duplicateSet.contains(packageName)) {
                descList.add(new App(resolveInfo, this));
                duplicateSet.add(resolveInfo.activityInfo.packageName);
            }
        }
        return;
    }

    public void setTarget(BlackWhiteListService service, String target) {
        List<App> list = new ArrayList<App>();
        for (App app : wholeAppList) {
            if (service.isTargetApp(app.getPackageName(), target)) {
                list.add(app);
            }
        }
        appList.clear();
        appList.addAll(list);
        Collections.sort(appList);
        appAdapter.notifyDataSetChanged();
        Log.d(Utils.TAG, "setTarget " + target);
    }
}
