package org.phone_lab.jouler.blackwhitelist.services;

import android.content.Entity;
import android.os.DropBoxManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.phone_lab.jouler.blackwhitelist.utils.Utils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by xcv58 on 2/23/15.
 */
public class EnergyDetails {
    public class ListEnergy {
        double fgEnergy;
        double bgEnergy;

        public ListEnergy() {
            fgEnergy = 0.0;
            bgEnergy = 0.0;
        }
    }

    private HashMap<String, ListEnergy> listPackageMap;
    // key: different list include Utils.UNKNOWN_TAB
    // value: JSONArray that include json object from different apps.

    private JSONObject energyDetailJSONObject;

    public EnergyDetails(HashMap<String, String> listMap, String jsonString) {
        listPackageMap = new HashMap<String, ListEnergy>();
        energyDetailJSONObject = new JSONObject();
        try {
            for (String target : Utils.TABS_ARRAY) {
                energyDetailJSONObject.put(target, new JSONArray());
                listPackageMap.put(target, new ListEnergy());
            }
            energyDetailJSONObject.put(Utils.UNKNOWN_TAB, new JSONArray());
            listPackageMap.put(Utils.UNKNOWN_TAB, new ListEnergy());

            // TODO: statistics different app usage information.
            // TODO: and depend on this information to decide next action.
            JSONObject json = new JSONObject(jsonString);
            energyDetailJSONObject.put("BluetoothEnergy", json.getDouble("BluetoothEnergy"));
            energyDetailJSONObject.put("CurrentBgDischargeRate", json.getDouble("CurrentBgDischargeRate"));
            energyDetailJSONObject.put("CurrentDischargeRate", json.getDouble("CurrentDischargeRate"));
            energyDetailJSONObject.put("CurrentFgDischargeRate", json.getDouble("CurrentFgDischargeRate"));
            energyDetailJSONObject.put("IdleEnergy", json.getDouble("IdleEnergy"));
            energyDetailJSONObject.put("PhoneEnergy", json.getDouble("PhoneEnergy"));
            energyDetailJSONObject.put("RadioEnergy", json.getDouble("RadioEnergy"));
            energyDetailJSONObject.put("ScreenEnergy", json.getDouble("ScreenEnergy"));
            energyDetailJSONObject.put("TotalScreenOffTime", json.getDouble("TotalScreenOffTime"));
            energyDetailJSONObject.put("TotalScreenOnTime", json.getDouble("TotalScreenOnTime"));
            energyDetailJSONObject.put("Uptime", json.getDouble("Uptime"));
            energyDetailJSONObject.put("WifiEnergy", json.getDouble("WifiEnergy"));

            JSONObject packageDetails = (JSONObject) json.get("packageDetails");
            Iterator<String> e = packageDetails.keys();
            while (e.hasNext()) {
                String packageName = e.next();
                String whichList = listMap.get(packageName);
                if (whichList == null) {
                    whichList = Utils.UNKNOWN_TAB;
                }
                JSONObject packageEnergyDetail = packageDetails.getJSONObject(packageName);
                JSONArray jsonArray = (JSONArray) energyDetailJSONObject.get(whichList);
                jsonArray.put(packageEnergyDetail);

                ListEnergy listEnergy = listPackageMap.get(whichList);
                Object fgEnergy = packageEnergyDetail.get(Utils.JSON_FgEnergy);
                Object bgEnergy = packageEnergyDetail.get(Utils.JSON_BgEnergy);
                listEnergy.fgEnergy += Double.parseDouble(fgEnergy.toString());
                listEnergy.bgEnergy += Double.parseDouble(bgEnergy.toString());

//                Log.d(Utils.TAG, "Package name: " + packageName + "; fgEnergy: " + fgEnergy + "; bgEnergy: " + bgEnergy);
            }
            for (Map.Entry<String, ListEnergy> entry : listPackageMap.entrySet()) {
                String key = entry.getKey();
                ListEnergy listEnergy = entry.getValue();
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("bgEnergy", listEnergy.bgEnergy);
                jsonObject.put("fgEnergy", listEnergy.fgEnergy);
                jsonObject.put("totalEnergy", listEnergy.bgEnergy + listEnergy.fgEnergy);
                energyDetailJSONObject.put(key + " TotalEnergy", jsonObject);
            }
            Utils.log(Utils.ENERGY_DETAILS, energyDetailJSONObject.toString());
        } catch (JSONException e) {
            Log.d(Utils.TAG, e.toString());
        }
    }

    public JSONObject getEnergyDetailJSONObject() {
        return energyDetailJSONObject;
    }

    public ListEnergy getListEnergy(String target) {
        ListEnergy listEnergy = listPackageMap.get(target);
        if (listEnergy == null) {
            Log.d(Utils.TAG, target + " is not valid list options");
        }
        return listEnergy;
    }
}
