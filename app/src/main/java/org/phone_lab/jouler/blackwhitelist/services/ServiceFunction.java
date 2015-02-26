package org.phone_lab.jouler.blackwhitelist.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.phone_lab.jouler.blackwhitelist.activities.App;
import org.phone_lab.jouler.blackwhitelist.utils.Mechanism;
import org.phone_lab.jouler.blackwhitelist.utils.Utils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by xcv58 on 1/27/15.
 */
public class ServiceFunction {
    private static final int MAX_CPU_FREQUENCY_HIGH = 2265600;
    private static final int MAX_CPU_FREQUENCY_LOW = 300000;
    // 300000
    // 422400
    // 652800
    // 729600
    // 883200
    // 960000
    // 1036800
    // 1190400
    // 1267200
    // 1497600
    // 1574400
    // 1728000
    // 1958400
    // 2265600

    private static final String LIST_MAP_LOCATION = "list_storage";
    private static final String TARGET_MAP_LOCATION = "target_map_storage";
    private static final String DEFAULT_LIST = Utils.NORMALLIST_TAB;
    private BlackWhiteListService service;
    private HashMap<String, String> listMap;
    private String target;
    private HashMap<String, HashSet<String>> targetSetMap;

    private HashSet<Integer> rateLimitPackageSet;
    private HashSet<Integer> cpuControlSet;
    private HashMap<Integer, Integer> priorityChangedPackageMap;

    private int globalPriority = 10;
    private boolean isBrightnessSet;
    private int batteryLevel;

    private static final float BLACK_THRESHOLD_PUNISH = 0.2f;
    private static final float BLACK_THRESHOLD_FORGIVE = 0.01f;
    private static final float NORMAL_THRESHOLD_PUNISH = 0.5f;
    private static final float NORMAL_THRESHOLD_FORGIVE = 0.3f;

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
            if (!service.iJoulerBaseServiceBound) {
                return;
            }
            if ("com.google.android.googlequicksearchbox".equals(packageName)) {
                return;
            }
            if (action.equals(Intent.ACTION_RESUME_ACTIVITY)) {
                if (isTargetApp(packageName, Utils.BLACKLIST_TAB) || isTargetApp(packageName, Utils.NORMALLIST_TAB)) {
                    if (!cpuControlSet.contains(uid)) {
                        try {
                            Utils.log(Utils.CONTROL_MAX_CPU_FRE_LOW + " by RESUME: ", packageName);
                            cpuControlSet.add(uid);
                            batteryLevelChanged();
                            service.iJoulerBaseService.controlCpuMaxFrequency(MAX_CPU_FREQUENCY_LOW);
                        } catch (RemoteException e) {
                            Utils.log(Utils.TAG, e.toString());
                            e.printStackTrace();
                        }
                    }
                }
            } else if (action.equals(Intent.ACTION_PAUSE_ACTIVITY)) {
                if (isTargetApp(packageName, Utils.BLACKLIST_TAB) || isTargetApp(packageName, Utils.NORMALLIST_TAB)) {
                    if (cpuControlSet.contains(uid)) {
                        try {
                            Utils.log(Utils.CONTROL_MAX_CPU_FRE_HIGH + " by Pause: ", packageName);
                            cpuControlSet.remove(uid);
                            service.iJoulerBaseService.controlCpuMaxFrequency(MAX_CPU_FREQUENCY_HIGH);
                        } catch (RemoteException e) {
                            Utils.log(Utils.TAG, e.toString());
                            e.printStackTrace();
                        }
                    }
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
        cpuControlSet = new HashSet<Integer>();
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
            Utils.log(Utils.LIST_DETAILS, this.getListDetails().toString());
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
        // DONE: This should output all app's information.
        Utils.log(Utils.LIST_DETAILS, this.getListDetails().toString());
        this.clearSelectSet();
//        batteryLevelChanged();
        return result;
    }

    private JSONObject getListDetails() {
        JSONObject jsonObject = new JSONObject();
        try {
            for (Map.Entry<String, String> entry : listMap.entrySet()) {
                String packageName = entry.getKey();
                String belongList = entry.getValue();
                jsonObject.put(packageName, belongList);
            }
        } catch (JSONException e) {
            Log.d(Utils.TAG, e.toString());
        }
        return jsonObject;
    }

    private void registerBunchReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_RESUME_ACTIVITY);
        intentFilter.addAction(Intent.ACTION_PAUSE_ACTIVITY);
        service.registerReceiver(activityResumePauseReceiver, intentFilter);

        IntentFilter batteryChangeIntentFilter = new IntentFilter();
        batteryChangeIntentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        service.registerReceiver(onBatteryChange, batteryChangeIntentFilter);

//        IntentFilter screenOnOffIntentFilter = new IntentFilter();
//        screenOnOffIntentFilter.addAction(Intent.ACTION_SCREEN_ON);
//        screenOnOffIntentFilter.addAction(Intent.ACTION_SCREEN_OFF);
//        service.registerReceiver(screenReceiver, screenOnOffIntentFilter);
    }

    private void unregisterBunchReceiver() {
        service.unregisterReceiver(activityResumePauseReceiver);
//        service.unregisterReceiver(screenReceiver);
        service.unregisterReceiver(onBatteryChange);
    }

    private void saveMode(int uid, String packageName) {
        Log.d(Utils.TAG, "Enable saveMode for: " + packageName);
        Utils.log(Utils.ENABLE_SAVEMODE, packageName);

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

    public void batteryLevelChanged() {
        // do some computation to get threshold
        // do punish or forgive based on the threshold
        Log.d(Utils.TAG, "batteryLevelChanged");
        if (!service.iJoulerBaseServiceBound) {
            Log.d(Utils.TAG, "batteryLevelChanged no bound service");
            return;
        }
        try {
            // TODO: statistics different app usage information.
            // TODO: and depend on this information to decide next action.
            String src = service.iJoulerBaseService.getStatistics();
            EnergyDetails energyDetails = new EnergyDetails(listMap, src);
            JSONObject ratioJSONObject = this.getRatioJSONObject(energyDetails);
            double blackRatio = ratioJSONObject.getDouble(Utils.JSON_BlackRatio);
            double whiteRatio = ratioJSONObject.getDouble(Utils.JSON_WhiteRatio);
            double normalRatio = ratioJSONObject.getDouble(Utils.JSON_NormalRatio);

            boolean blackPunish = blackRatio > BLACK_THRESHOLD_PUNISH;
            boolean blackForgive = blackRatio < BLACK_THRESHOLD_FORGIVE;

            boolean normalPunish = normalRatio > NORMAL_THRESHOLD_PUNISH;
            boolean normalForgive = normalRatio < NORMAL_THRESHOLD_FORGIVE;

//            if (blackPunish || normalPunish) {
//                Utils.log(Utils.PUNISH, ratioJSONObject.toString());
//                punish(energyDetails);
//            } else if (blackForgive && normalForgive) {
//                Utils.log(Utils.FORGIVE, ratioJSONObject.toString());
//                forgive(energyDetails);
//            } else {
            Utils.log(Utils.DONOTHING, ratioJSONObject.toString());
//            }
        } catch (RemoteException e) {
            Log.d(Utils.TAG, e.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private JSONObject getRatioJSONObject(EnergyDetails energyDetails) {
        JSONObject jsonObject = new JSONObject();
        EnergyDetails.ListEnergy blackListEnergy = energyDetails.getListEnergy(Utils.BLACKLIST_TAB);
        EnergyDetails.ListEnergy normalListEnergy = energyDetails.getListEnergy(Utils.NORMALLIST_TAB);
        EnergyDetails.ListEnergy whiteListEnergy = energyDetails.getListEnergy(Utils.WHITELIST_TAB);
        double blackListTotal = blackListEnergy.fgEnergy + blackListEnergy.bgEnergy;
        double whiteListTotal = whiteListEnergy.fgEnergy + whiteListEnergy.bgEnergy;
        double normalListTotal = normalListEnergy.fgEnergy + normalListEnergy.bgEnergy;
        double total = blackListTotal + whiteListTotal + normalListTotal;
        double blackRatio = blackListTotal / total;
        double whiteRatio = whiteListTotal / total;
        double normalRatio = normalListTotal / total;
        try {
            jsonObject.put(Utils.GLOBAL_PRIORITY, globalPriority);
            jsonObject.put(Utils.BATTERY_LEVEL, batteryLevel);
            jsonObject.put(Utils.JSON_BlackTotal, blackListTotal);
            jsonObject.put(Utils.JSON_NormalTotal, normalListTotal);
            jsonObject.put(Utils.JSON_WhiteTotal, whiteListTotal);
            jsonObject.put(Utils.JSON_BlackRatio, blackRatio);
            jsonObject.put(Utils.JSON_NormalRatio, normalRatio);
            jsonObject.put(Utils.JSON_WhiteRatio, whiteRatio);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    private void leaveMode(int uid, String packageName) {
        Utils.log(Utils.LEAVE_SAVEMODE, packageName);
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
        Utils.log(Utils.RESET_EVERYTHING, "");
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
            Log.d(Utils.TAG, e.toString());
        }
        this.unregisterBunchReceiver();
    }
}
