package org.phone_lab.jouler.blackwhitelist.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.DropBoxManager;
import android.os.RemoteException;
import android.provider.Settings;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.phone_lab.jouler.blackwhitelist.activities.App;
import org.phone_lab.jouler.blackwhitelist.utils.Utils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
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

    private HashSet<Integer> rateLimitPackageSet;
    private HashMap<Integer, Integer> priorityChangedPackageMap;

    private int globalPriority = 10;
    private boolean isBrightnessSet;

    private int batteryLevel;

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
        }
    };

    BroadcastReceiver onBatteryChange = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL;

            if (!isLevelChanged(level)) {
                return;
            }
            batteryLevelChanged();

//            JSONObject detail = getJsonDetail();
//            try {
//                double totalInList = detail.getDouble(TOTAL_CONSUMPTION_IN_LIST);
//                double totalNotInList = detail.getDouble(TOTAL_CONSUMPTION_NOT_IN_LIST);
//                int numInList = detail.getInt(NUM_IN_LIST);
//                int numNotINList = detail.getInt(NUM_NOT_IN_LIST);
//                double meanInList = 0.0;
//                double meanNotInList = 0.0;
//                if (numInList != 0 && totalInList > 0.0) {
//                    meanInList = totalInList / numInList;
//                }
//                if (numNotINList != 0 && totalNotInList > 0.0) {
//                    meanNotInList = totalNotInList / numNotINList;
//                }
//                double ratio = meanInList / meanNotInList;
////                makeNotification("Battery Level change", "Level: " + level + ", ratio: " + ratio);
//                if (!isBlackList()) {
//                    ratio = meanNotInList / meanInList;
//                }
//                if (ratio >= MAX_THRESHOLD) {
//                    punish();
//                }
//                if (ratio <= MIN_THRESHOLD) {
//                    forgive();
//                }
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
        }
    };

    public ServiceFunction(BlackWhiteListService service) {
        this.service = service;
        this.target = DEFAULT_LIST;
        isBrightnessSet = false;
        batteryLevel = -1;
        rateLimitPackageSet = new HashSet<Integer>();
        priorityChangedPackageMap = new HashMap<Integer, Integer>();
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
//        Log.d(Utils.TAG, "PackageName: " + packageName + "; value: " + value + "; target: " + target);
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
//        Log.d(Utils.TAG, "ListMap: " + listMap.toString());
        this.clearSelectSet();
        batteryLevelChanged();
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
        Log.d(Utils.TAG, "Enable saveMode for: " + packageName);

        try {
            if (service.iJoulerBaseServiceBound) {
                if (!isBrightnessSet) {
                    service.iJoulerBaseService.lowBrightness();
                    isBrightnessSet = true;
                }

                if (rateLimitPackageSet.contains(uid)) {
                    service.iJoulerBaseService.delRateLimitRule(uid);
                }

                Integer originalPriority = priorityChangedPackageMap.get(uid);
                if (originalPriority != null) {
                    // have already change the priority of package
                    // so need put it back because it becomes foreground.
                    service.iJoulerBaseService.resetPriority(uid, originalPriority);
                    priorityChangedPackageMap.remove(uid);
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
//        setBrightness();
//        setPriority(uid, packagename);
    }

    private boolean isLevelChanged(int newLevel) {
        if (batteryLevel == -1) {
            batteryLevel = newLevel;
        }
        if (newLevel == batteryLevel) {
            return false;
        }
        batteryLevel = newLevel;
        return true;
    }

    private void batteryLevelChanged() {
        // do some computation to get threshold
        // do punish or forgive based on the threshold
        Log.d(Utils.TAG, "batteryLevelChanged");
        if (!service.iJoulerBaseServiceBound) {
            Log.d(Utils.TAG, "batteryLevelChanged no bound service");
            return;
        }
        try {
            String src = service.iJoulerBaseService.getStatistics();
            JSONObject json = new JSONObject(src);
            Iterator<String> e = json.keys();
            while (e.hasNext()) {
                String packageName = e.next();
                String whichList = listMap.get(packageName);
                if (whichList == null) {
                    Log.d(Utils.TAG, packageName + " not in list.");
                    continue;
                }
                Log.d(Utils.TAG, packageName + ":");
                JSONObject uidStats = json.getJSONObject(packageName);
//                Iterator<String> uidStatsE = uidStats.keys();
//                json.put("FgEnergy", u.getFgEnergy());
//                json.put("BgEnergy", u.getBgEnergy());
                Object fgEnergy = uidStats.get(Utils.JSON_FgEnergy);
                Log.d(Utils.TAG, "Package name: " + packageName + "; fgEnergy: " + fgEnergy);
//                Double.parseDouble(uidStats.get(Utils.JSON_FgEnergy).toString());

//                while (uidStatsE.hasNext()) {
//                    String attribute = uidStatsE.next();
//                    Log.d(Utils.TAG, "Package name: " + key + "; " + attribute + ": " + uidStats.get(attribute));
//                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
//        initListMap();
//        for (Map.Entry<String, String> entry : listMap.entrySet()) {
//            String packageName = entry.getKey();
//            String whichList = entry.getValue();
//            if (whichList.equals(Utils.BLACKLIST_TAB)) {
//                Log.d(Utils.TAG, packageName + " is in " + whichList);
//            } else if (whichList.equals(Utils.NORMALLIST_TAB)) {
//                Log.d(Utils.TAG, packageName + " is in " + whichList);
//            } else if (whichList.equals(Utils.WHITELIST_TAB)) {
//                Log.d(Utils.TAG, packageName + " is in " + whichList);
//            } else {
//                Log.d(Utils.TAG, "ERROR: " + packageName + " is in " + whichList + ", which doesn't exist!");
//            }
//        }
    }

    private void leaveMode(int uid, String packageName) {
        try {
            if (service.iJoulerBaseServiceBound) {
                if (isBrightnessSet) {
                    service.iJoulerBaseService.resetBrightness();
                    isBrightnessSet = false;
                }

                if (rateLimitPackageSet.contains(uid)) {
                    service.iJoulerBaseService.addRateLimitRule(uid);
                }

                Integer originalPriority = priorityChangedPackageMap.get(uid);
                if (originalPriority == null) {
                    // haven't touch this package
                    service.iJoulerBaseService.getPriority(uid);
                    priorityChangedPackageMap.put(uid, originalPriority);
                    service.iJoulerBaseService.resetPriority(uid, globalPriority);
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void reset() {
        // should reset everything.
        try {
            for (Integer uid : rateLimitPackageSet) {
                service.iJoulerBaseService.delRateLimitRule(uid);
            }
            rateLimitPackageSet.clear();
            for (Map.Entry<Integer, Integer> entry : priorityChangedPackageMap.entrySet()) {
                int uid = entry.getKey();
                int priority = entry.getValue();
                service.iJoulerBaseService.resetPriority(uid, priority);
            }
            priorityChangedPackageMap.clear();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
