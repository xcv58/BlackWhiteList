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
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.phone_lab.jouler.blackwhitelist.R;
import org.phone_lab.jouler.blackwhitelist.services.BlackWhiteListService;
import org.phone_lab.jouler.blackwhitelist.utils.Utils;
import org.phone_lab.jouler.joulerbase.IJoulerBaseService;

import java.util.Collections;


public class MainActivity extends Activity {
    protected IJoulerBaseService iJoulerBaseService;
    protected BlackWhiteListService mService;
    private ListTabListener listTabListener;

    private class ListTabListener implements ActionBar.TabListener {
        @Override
        public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
            Log.d(Utils.TAG, "onTabSelected " + tab.getText());
            AppListFragment appListFragment = (AppListFragment) getFragmentManager().findFragmentById(R.id.client_list);
            String target = tab.getText().toString();

            if (mService == null) {
                Log.d(Utils.TAG, "mService is NULL");
            } else {
                mService.setTarget(target);
                appListFragment.setTarget(mService, target);
                Log.d(Utils.TAG, "mService is " + iJoulerBaseService.toString());
            }
        }

        @Override
        public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
            Log.d(Utils.TAG, "onTabUnselected " + tab.getText());
        }

        @Override
        public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
            Log.d(Utils.TAG, "onTabReSelected " + tab.getText());
            AppListFragment appListFragment = (AppListFragment) getFragmentManager().findFragmentById(R.id.client_list);
        }
    }

    private ServiceConnection joulerBaseConnection = new ServiceConnection() {
        // Called when the connection with the service is established
        public void onServiceConnected(ComponentName className, IBinder service) {
            // Following the example above for an AIDL interface,
            // this gets an instance of the IRemoteInterface, which we can use to call on the service
            iJoulerBaseService = IJoulerBaseService.Stub.asInterface(service);
            Log.d(Utils.TAG, "Bound from another app");
            try {
                if (!iJoulerBaseService.checkPermission()) {
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
            AppListFragment appListFragment = (AppListFragment) getFragmentManager().findFragmentById(R.id.client_list);
            Collections.sort(appListFragment.appList);
            appListFragment.appAdapter.notifyDataSetChanged();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
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

//        if (savedInstanceState == null) {
//            getFragmentManager().beginTransaction()
//                    .add(R.id.container, new PlaceholderFragment())
//                    .commit();
//        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mService == null) {
            Log.d(Utils.TAG, "bind mService");
            Intent blackWhiteListServiceIntent  = new Intent(this, BlackWhiteListService.class);
            bindService(blackWhiteListServiceIntent, mConnection, this.BIND_AUTO_CREATE);
            Log.d(Utils.TAG, "bind mService end");
        }

        if (iJoulerBaseService == null) {
            Log.d(Utils.TAG, "bind JoulerBaseService");
            Intent intent = new Intent(IJoulerBaseService.class.getName());
            bindService(intent, joulerBaseConnection, this.BIND_AUTO_CREATE);
            Log.d(Utils.TAG, "bind JoulerBaseService end");
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mService != null) {
            mService.flush();
            unbindService(mConnection);
        }

        if (iJoulerBaseService != null) {
            unbindService(joulerBaseConnection);
        }
    }

    public void openJoulerBase(View view) {
        startJoulerBase();
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
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
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
