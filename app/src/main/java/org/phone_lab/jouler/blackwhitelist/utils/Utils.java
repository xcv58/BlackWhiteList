package org.phone_lab.jouler.blackwhitelist.utils;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by xcv58 on 1/23/15.
 */
public class Utils {
    public static final String TAG = "JoulerBlackWhiteList";

    public static final String BLACKLIST_TAB = "Black List";
    public static final String NORMALLIST_TAB = "Normal List";
    public static final String WHITELIST_TAB = "White List";
    public static final String UNKNOWN_TAB = "Not in List";

    public static final String[] TABS_ARRAY = {BLACKLIST_TAB, NORMALLIST_TAB, WHITELIST_TAB};
    public static final int NO_UID = -1;
    public static final String LIST_DETAILS = "List details";

    public static final String PACKAGE = "Package";
    public static final String USERID = "UserId";

    public static final String START_ACTION = "org.phone_lab.jouler.blackwhitelist.START";
    public static final String STOP_ACTION = "org.phone_lab.jouler.blackwhitelist.STOP";

    public static final String JSON_packageName = "packageName";
    public static final String JSON_uiActivity = "uiActivity";
    public static final String JSON_Uid = "Uid";
    public static final String JSON_FgEnergy = "FgEnergy";
    public static final String JSON_BgEnergy = "BgEnergy";
    public static final String JSON_CpuEnergy = "CpuEnergy";
    public static final String JSON_WakelockEnergy = "WakelockEnergy";
    public static final String JSON_WifiEnergy = "WifiEnergy";
    public static final String JSON_SensorEnergy = "SensorEnergy";
    public static final String JSON_MobileDataEnergy = "MobileDataEnergy";
    public static final String JSON_WifiDataEnergy = "WifiDataEnergy";
    public static final String JSON_VideoEnergy = "VideoEnergy";
    public static final String JSON_AudioEnergy = "AudioEnergy";
    public static final String JSON_Frame = "Frame";
    public static final String JSON_State = "State";
    public static final String JSON_Throttle = "Throttle";
    public static final String JSON_Count = "Count";
    public static final String JSON_UsageTime = "UsageTime";

    public static final String JSON_BlackTotal = "Black Total";
    public static final String JSON_NormalTotal = "Normal Total";
    public static final String JSON_WhiteTotal = "White Total";
    public static final String JSON_BlackRatio = "Black Ratio";
    public static final String JSON_NormalRatio = "Normal Ratio";
    public static final String JSON_WhiteRatio = "White Ratio";

    public static final String GLOBAL_PRIORITY = "Global Priority";
    public static final String BATTERY_LEVEL = "Battery Level";

    public static final String TIME_STAMP = "EpochTime";
    public static final String ENERGY_DETAILS = "Energy Details";
    public static final String ENABLE_SAVEMODE = "Enable saveMode";
    public static final String LEAVE_SAVEMODE = "Leave saveMode";

    public static final String PUNISH = "Punish";
    public static final String FORGIVE = "Forgive";
    public static final String DONOTHING = "Do nothing";

    public static final String ADD_RATE_LIMIT = "Add rate limit";
    public static final String REMOVE_RATE_LIMIT = "Remove rate limit";

    public static final String SET_PRIORITY_LOWEST = "Set priority to 19";
    public static final String SET_PRIORITY_HIGHEST = "Set priority to -20";
    public static final int LOWEST_PRIORITY = 19;
    public static final int HIGHEST_PRIORITY = -20;

    public static final String RESET_EVERYTHING = "Reset everything";

    public static final String RESUME = "Jouler BlackWhiteList resume";
    public static final String PAUSE = "Jouler BlackWhiteList pause";

    public static void log(String key, String value) {
        long timeStamp = System.currentTimeMillis();
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(TIME_STAMP, timeStamp);
            jsonObject.put(key, value);
            Log.d(TAG, jsonObject.toString());
        } catch (JSONException e) {
            Log.d(TAG, e.toString());
        }
    }
}

