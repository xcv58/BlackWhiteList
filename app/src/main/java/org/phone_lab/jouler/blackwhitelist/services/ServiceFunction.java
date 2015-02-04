package org.phone_lab.jouler.blackwhitelist.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.Settings;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.widget.Toast;

import org.phone_lab.jouler.blackwhitelist.activities.App;
import org.phone_lab.jouler.blackwhitelist.utils.Utils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by xcv58 on 1/27/15.
 */
public class ServiceFunction {
    private static final String LIST_MAP_LOCATION = "list_storage";
    private static final String TARGET_MAP_LOCATION = "target_map_storage";
    private static final String DEFAULT_LIST = Utils.NORMALLIST_TAB;
    private BlackWhiteListService service;
    private HashMap<String, String> listMap;
    private String target;
    private HashMap<String, HashSet<String>> targetSetMap;

    private int globalPriority = 10;
    private boolean isBrightnessSet;
    private int previousBrightness;
    private int previousBrightnessMode;

    private BroadcastReceiver activityResumePauseReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            StringBuilder sb = new StringBuilder();

            String packageName = bundle.getString(Utils.PACKAGE);
            sb.append(Utils.PACKAGE);
            sb.append(": ");
            sb.append(packageName);
            sb.append("; ");
            int uid = bundle.getInt(Utils.USERID);
            sb.append(Utils.USERID);
            sb.append(": ");
            sb.append(uid);
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_RESUME_ACTIVITY)) {
                if (isTargetApp(packageName, Utils.BLACKLIST_TAB)) {
                    saveMode(uid, packageName);
                }
            } else if (action.equals(Intent.ACTION_PAUSE_ACTIVITY)) {
                if (isTargetApp(packageName, Utils.BLACKLIST_TAB)) {
                    leaveMode(uid, packageName);
                }
            }
//            Log.d(TAG, intent.getAction() + "," + System.currentTimeMillis() + ", " + sb.toString() + ", Energy usage: " + getEnergy(uid));
        }
    };

    public ServiceFunction(BlackWhiteListService service) {
        this.service = service;
        this.target = DEFAULT_LIST;
        isBrightnessSet = false;
        this.registerBunchReceiver();
    }

    public HashMap<String, String> readListMap() {
//        Log.d(TAG, "read map from file: " + listMapLocation);
        try {
            FileInputStream fis = service.openFileInput(LIST_MAP_LOCATION);
            ObjectInputStream is = new ObjectInputStream(fis);
            HashMap<String, String> map = (HashMap<String, String>) is.readObject();
            is.close();
            if (map != null) {
                return map;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new HashMap<String, String>();
    }

    public boolean flush() {
        try {
            FileOutputStream fos = service.openFileOutput(LIST_MAP_LOCATION, service.MODE_PRIVATE);
            ObjectOutputStream os = new ObjectOutputStream(fos);
            os.writeObject(listMap);
            os.close();

            Log.d(Utils.TAG, "Write Target MAP");
            fos = service.openFileOutput(TARGET_MAP_LOCATION, service.MODE_PRIVATE);
            os = new ObjectOutputStream(fos);
            os.writeObject(targetSetMap);
            os.close();
        } catch (IOException e) {
            Log.d(Utils.TAG, "Write Target MAP ERROR");
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean isTargetApp(String packageName, String target) {
        this.initListMap();
        String value = this.getListName(packageName);
        Log.d(Utils.TAG, "PackageName: " + packageName + "; value: " + value + "; target: " + target);
        return value.equals(target);
    }

    private String getListName(String packageName) {
        String value = listMap.get(packageName);
        if (value == null) {
            value = DEFAULT_LIST;
            listMap.put(packageName, value);
        }
        return value;
    }

    private void initListMap() {
        if (listMap == null) {
            this.listMap = this.readListMap();
            Log.d(Utils.TAG, "ListMap: " + listMap.toString());
        }
    }

    private void initTargetMap() {
        if (this.targetSetMap == null) {
            Log.d(Utils.TAG, "targetSetMap is NULL");
            this.targetSetMap = this.readTargetMap();
        }
    }

    private HashMap<String, HashSet<String>> readTargetMap() {
        try {
            FileInputStream fis = service.openFileInput(TARGET_MAP_LOCATION);
            ObjectInputStream is = new ObjectInputStream(fis);
            HashMap<String, HashSet<String>> map = (HashMap<String, HashSet<String>>) is.readObject();
            is.close();
            if (map != null) {
                Log.d(Utils.TAG, "read TargetMap successful");
                return map;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d(Utils.TAG, "read TargetMap fail return new Map");
        return new HashMap<String, HashSet<String>>();
    }

    public void select(App app) {
        String packageName = app.getPackageName();
        HashSet<String> selectedSet = this.getSelectSet();
        if (selectedSet.contains(packageName)) {
            selectedSet.remove(packageName);
        } else {
            selectedSet.add(packageName);
        }
    }

    public boolean isSelected(App app) {
        String packageName = app.getPackageName();
        HashSet<String> selectedSet = this.getSelectSet();
        return selectedSet.contains(packageName);
    }

    public HashSet<String> getSelectSet() {
        this.initTargetMap();
        HashSet<String> set = this.targetSetMap.get(this.target);
        if (set == null) {
            set = new HashSet<String>();
            targetSetMap.put(this.target, set);
        }
        return set;
    }

    public void clearSelectSet() {
        this.initTargetMap();
        HashSet<String> set = this.targetSetMap.get(this.target);
        if (set != null) {
            set.clear();
        }
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getTarget() {
        return this.target;
    }

    public int moveTo(String target) {
        this.initListMap();
        Set<String> set = this.getSelectSet();
        int result = set.size();
        for (String packageName : set) {
            Log.d(Utils.TAG, "Move to " + packageName + ", " + target);
            listMap.put(packageName, target);
        }
        Log.d(Utils.TAG, "ListMap: " + listMap.toString());
        this.clearSelectSet();
        return result;
    }

    private void registerBunchReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_RESUME_ACTIVITY);
        intentFilter.addAction(Intent.ACTION_PAUSE_ACTIVITY);
        service.registerReceiver(activityResumePauseReceiver, intentFilter);

//        IntentFilter batteryChangeIntentFilter = new IntentFilter();
//        batteryChangeIntentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
//        service.registerReceiver(onBatteryChange, batteryChangeIntentFilter);
//
//        IntentFilter screenOnOffIntentFilter = new IntentFilter();
//        screenOnOffIntentFilter.addAction(Intent.ACTION_SCREEN_ON);
//        screenOnOffIntentFilter.addAction(Intent.ACTION_SCREEN_OFF);
//        service.registerReceiver(screenReceiver, screenOnOffIntentFilter);
    }

    private void unregisterBunchReceiver(BlackWhiteListService service) {
        service.unregisterReceiver(activityResumePauseReceiver);
//        service.unregisterReceiver(screenReceiver);
//        service.unregisterReceiver(onBatteryChange);
    }

    private void saveMode(int uid, String packageName) {
//        makeNotification(ENTER_SAVE_MODE, packagename);
        Log.d(Utils.TAG, "Enable saveMode for: " + packageName);

        try {
            if (service.iJoulerBaseServiceBound) {
                service.iJoulerBaseService.lowBrightness();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
//        setBrightness();
//        setPriority(uid, packagename);
    }

    private void leaveMode(int uid, String packageName) {
//        if (metaData.isRateLimited()) {
//            if (!metaData.alreadySetRateLimit(packagename)) {
//                joulerPolicy.rateLimitForUid(uid);
//                metaData.setRateLimit(packagename);
//            }
//        }
        try {
            if (service.iJoulerBaseServiceBound) {
                service.iJoulerBaseService.resetBrightness();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
//        if (metaData.isForegroundPriority(uid)) {
//            joulerPolicy.resetPriority(uid, metaData.getGlobalPriority());
//        }
    }
}
