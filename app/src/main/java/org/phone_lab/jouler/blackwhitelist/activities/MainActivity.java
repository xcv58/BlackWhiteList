package org.phone_lab.jouler.blackwhitelist.activities;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.drm.DrmStore;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import org.phone_lab.jouler.blackwhitelist.R;
import org.phone_lab.jouler.blackwhitelist.services.BlackWhiteListService;
import org.phone_lab.jouler.blackwhitelist.utils.Utils;
import org.phone_lab.jouler.joulerbase.IJoulerBaseService;

import java.util.Collections;
import java.util.FormatFlagsConversionMismatchException;
import java.util.List;
import java.util.Set;


public class MainActivity extends Activity {
    protected IJoulerBaseService iJoulerBaseService;
    private boolean iJoulerBaseServiceBound;
    protected BlackWhiteListService mService;
    protected boolean mBound;
    private ListTabListener listTabListener;
    private AppListFragment appListFragment;

    protected String leftTarget;
    protected String rightTarget;

    private class ListTabListener implements ActionBar.TabListener {
        @Override
        public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
            Log.d(Utils.TAG, "onTabSelected " + tab.getText());
            String target = tab.getText().toString();

            setButtons(target);

            if (mService == null) {
                Log.d(Utils.TAG, "mService is NULL");
            } else {
                mService.setTarget(target);
                initAppListFragment();
                appListFragment.setTarget(mService, target);
            }
        }

        @Override
        public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
            Log.d(Utils.TAG, "onTabUnselected " + tab.getText());
        }

        @Override
        public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
            Log.d(Utils.TAG, "onTabReSelected " + tab.getText());
            initAppListFragment();
        }
    }

    private ServiceConnection joulerBaseConnection = new ServiceConnection() {
        // Called when the connection with the service is established
        public void onServiceConnected(ComponentName className, IBinder service) {
            // Following the example above for an AIDL interface,
            // this gets an instance of the IRemoteInterface, which we can use to call on the service
            iJoulerBaseService = IJoulerBaseService.Stub.asInterface(service);
            Log.d(Utils.TAG, "Bound from another app");
            iJoulerBaseServiceBound = true;
            try {
                if (!iJoulerBaseService.checkPermission()) {
                    // ask to get permission;
                    startJoulerBase();
                }
//                iJoulerBaseService.test("From another app", "hh");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Called when the connection with the service disconnects unexpectedly
        public void onServiceDisconnected(ComponentName className) {
            iJoulerBaseService = null;
        }
    };

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.d(Utils.TAG, "Get mService");
            BlackWhiteListService.LocalBinder binder = (BlackWhiteListService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            initAppListFragment();
            Collections.sort(appListFragment.appList);
            appListFragment.appAdapter.notifyDataSetChanged();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBound = false;
            mService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(Utils.TAG, "Client app onCreate");

        if (!isPackageExisted(getResources().getString(R.string.jouler_base_packagename))) {
            Log.d(Utils.TAG, "Client return by no Jouler Base");
            return;
        }

        setContentView(R.layout.activity_main);

        initTabs();
        mBound = false;

//        if (savedInstanceState == null) {
//            getFragmentManager().beginTransaction()
//                    .add(R.id.container, new PlaceholderFragment())
//                    .commit();
//        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!mBound) {
            Log.d(Utils.TAG, "bind mService");
            Intent blackWhiteListServiceIntent  = new Intent(this, BlackWhiteListService.class);
            bindService(blackWhiteListServiceIntent, mConnection, this.BIND_AUTO_CREATE);
            Log.d(Utils.TAG, "bind mService end");
        }

        if (!iJoulerBaseServiceBound) {
            Log.d(Utils.TAG, "bind JoulerBaseService");
            Intent intent = new Intent(IJoulerBaseService.class.getName());
            bindService(intent, joulerBaseConnection, this.BIND_AUTO_CREATE);
            Log.d(Utils.TAG, "bind JoulerBaseService end");
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mBound) {
            mService.flush();
            mBound = false;
            unbindService(mConnection);
        }

        if (iJoulerBaseServiceBound) {
            iJoulerBaseServiceBound = false;
            unbindService(joulerBaseConnection);
        }
    }

    public void openJoulerBase(View view) {
        startJoulerBase();
    }

    public void moveButtonOnClick(View view) {
        Button button = (Button) view;
        int id = button.getId();

        switch (id) {
            case R.id.move_to_left_list:
                Log.d(Utils.TAG, "Click move to left list button");
                this.mService.moveTo(leftTarget);
                break;
            case R.id.move_to_right_list:
                Log.d(Utils.TAG, "Click move to right list button");
                this.mService.moveTo(rightTarget);
                break;
            default:
                Log.d(Utils.TAG, "Click UNKNOWN button");
        }
        initAppListFragment();
        appListFragment.setTarget(mService, mService.getTarget());
    }

    private void setButtons(String target) {
        Button leftButton = (Button) findViewById(R.id.move_to_left_list);
        Button rightButton = (Button) findViewById(R.id.move_to_right_list);
        int index = 0;
        int length = Utils.TABS_ARRAY.length;
        for (; index < length; index++) {
            if (Utils.TABS_ARRAY[index].equals(target)) {
                break;
            }
        }
        int leftIndex = ((index + length) - 1) % length;
        int rightIndex = ((index + length) + 1) % length;
        leftTarget = Utils.TABS_ARRAY[leftIndex];
        rightTarget = Utils.TABS_ARRAY[rightIndex];
        String prefix = getString(R.string.button_desc_prefix);
        leftButton.setText(prefix + " " + leftTarget);
        rightButton.setText(prefix + " " + rightTarget);
    }

    public void test(View view) {
        try {
            Log.d(Utils.TAG, "Click test button");
            if (!iJoulerBaseService.checkPermission()) {
                Toast.makeText(this, "No Permission", Toast.LENGTH_LONG).show();
                return;
            }
            iJoulerBaseService.test("Test", "from button");
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void rateLimit(View view) {
        Log.d(Utils.TAG, "add Rate limited");
        Set<String> set = mService.getSelectedApp();
        final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<PackageInfo> list = getPackageManager().getInstalledPackages(PackageManager.GET_ACTIVITIES);
        for (PackageInfo packageInfo : list) {
            if (set.contains(packageInfo.packageName)) {
                try {
                    if (iJoulerBaseService.checkPermission()) {
                        Log.d(Utils.TAG, "Has permission, do rate limit");
                        switch (view.getId()) {
                            case R.id.add_rate_limit:
                                iJoulerBaseService.addRateLimitRule(packageInfo.applicationInfo.uid);
                                break;
                            case R.id.del_rate_limit:
                                iJoulerBaseService.delRateLimitRule(packageInfo.applicationInfo.uid);
                                break;
                            default:
                                break;
                        }
                    } else {
                        Log.d(Utils.TAG, "Has no permission, do nothing");
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }
    }

    public void startJoulerBase() {
        Intent LaunchIntent = getPackageManager().getLaunchIntentForPackage(getResources().getString(R.string.jouler_base_packagename));
        LaunchIntent.putExtra("SOURCE", getApplicationInfo().packageName);
        startActivity(LaunchIntent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.buttons_main, container, false);
            return rootView;
        }
    }

    private void initTabs() {
        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        this.listTabListener = new ListTabListener();


        for (String tabName : Utils.TABS_ARRAY) {
            ActionBar.Tab tab = actionBar.newTab();
            tab.setText(tabName);
            tab.setTabListener(listTabListener);
            actionBar.addTab(tab, tabName.equals(Utils.NORMALLIST_TAB));
        }
    }

    private void initAppListFragment() {
        if (appListFragment == null) {
            appListFragment = (AppListFragment) getFragmentManager().findFragmentById(R.id.client_list);
        }
    }
    private boolean isPackageExisted(String targetPackage) {
        PackageManager pm=getPackageManager();
        try {
            PackageInfo info=pm.getPackageInfo(targetPackage,PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
        return true;
    }
}
