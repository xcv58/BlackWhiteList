package org.phone_lab.jouler.blackwhitelist.activities;

import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.util.Log;

import org.phone_lab.jouler.blackwhitelist.utils.Utils;

import java.util.Comparator;

/**
 * Created by xcv58 on 1/23/15.
 */
public class App implements Comparable<App> {
    protected ResolveInfo resolveInfo;
    private String appName;
    private String description;
    private String packageName;
    private Drawable icon;
    private AppListFragment appListFragment;
    private final static String NO_DESCRIPTION = "no description";

    public App(ResolveInfo resolveInfo, AppListFragment appListFragment) {
        this.resolveInfo = resolveInfo;
        this.appListFragment = appListFragment;

        PackageManager pm = appListFragment.getActivity().getPackageManager();
        appName = resolveInfo.activityInfo.applicationInfo.loadLabel(pm).toString();

        packageName = resolveInfo.activityInfo.packageName;
        CharSequence charSequence = resolveInfo.activityInfo.applicationInfo.loadDescription(pm);
        description = charSequence == null ? NO_DESCRIPTION : charSequence.toString();
        icon = resolveInfo.activityInfo.applicationInfo.loadIcon(pm);
    }

    public String getAppName() {
        return appName;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getDescription() {
        return description;
    }

    public Drawable getIcon() {
        return icon;
    }

    public boolean isSelected() {
        MainActivity mainActivity = (MainActivity) appListFragment.getActivity();
        if (mainActivity.mService != null) {
            return mainActivity.mService.isSelected(this);
        }
        return false;
    }

    @Override
    public int compareTo(App app) {
        boolean selectedA = this.isSelected();
        boolean selectedB = app.isSelected();
        if (selectedA ^ selectedB) {
            return selectedB ? 1 : -1;
        } else {
            return this.appName.compareTo(app.appName);
        }
    }
}
