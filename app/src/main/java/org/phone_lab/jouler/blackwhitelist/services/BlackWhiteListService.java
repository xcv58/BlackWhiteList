package org.phone_lab.jouler.blackwhitelist.services;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import org.phone_lab.jouler.blackwhitelist.R;
import org.phone_lab.jouler.blackwhitelist.activities.App;
import org.phone_lab.jouler.blackwhitelist.utils.Utils;
import org.phone_lab.jouler.joulerbase.IJoulerBaseService;

import android.support.v4.app.NotificationCompat;

import java.util.Set;

/**
 * Created by xcv58 on 1/23/15.
 */
public class BlackWhiteListService extends Service {
    private ServiceFunction serviceFunction;
    
    protected boolean iJoulerBaseServiceBound;
    protected IJoulerBaseService iJoulerBaseService;

    private NotificationCompat.Builder notificationBuilder;
    private static final int NOTIFICATION_ID = 001;

    private ServiceConnection joulerBaseConnection = new ServiceConnection() {
        // Called when the connection with the service is established
        public void onServiceConnected(ComponentName className, IBinder service) {
            // Following the example above for an AIDL interface,
            // this gets an instance of the IRemoteInterface, which we can use to call on the service
            iJoulerBaseService = IJoulerBaseService.Stub.asInterface(service);
            iJoulerBaseServiceBound = true;
            try {
                if (!iJoulerBaseService.checkPermission()) {
                    // ask to get permission;
//                    startJoulerBase();
//                } else {
//                    iJoulerBaseService.test("Demo", "app");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Called when the connection with the service disconnects unexpectedly
        public void onServiceDisconnected(ComponentName className) {
            iJoulerBaseService = null;
        }
    };

    public void setTarget(String target) {
        initServiceFunction();
        serviceFunction.setTarget(target);
    }

    public String getTarget() {
        initServiceFunction();
        return serviceFunction.getTarget();
    }

    public boolean isSelected(App app) {
        initServiceFunction();
        return serviceFunction.isSelected(app);
    }

    public class LocalBinder extends Binder {
        public BlackWhiteListService getService() {
            return BlackWhiteListService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new LocalBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(Utils.TAG, Utils.TAG + " onCreate");
        foreground();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.d(Utils.TAG, Utils.TAG + " onStartCommand");
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(Utils.TAG, "onDestroy");
        // should reset everything.
        if (iJoulerBaseServiceBound) {
            unbindService(joulerBaseConnection);
        }
        stopForeground();
    }

    private void stopForeground() {
        stopForeground(true);
    }

    private void foreground() {
        notificationBuilder = new NotificationCompat.
                Builder(getBaseContext())
                .setContentTitle(Utils.TAG)
                .setContentText(Utils.TAG)
                .setSmallIcon(R.drawable.ic_launcher);
        startForeground(NOTIFICATION_ID, notificationBuilder.build());
        return;
    }

    public boolean isTargetApp(String packageName, String target) {
        this.initServiceFunction();
        return serviceFunction.isTargetApp(packageName, target);
    }

    public void select(App app) {
        initServiceFunction();
        serviceFunction.select(app);
    }

    public void moveTo(String target) {
        initServiceFunction();
        int size = serviceFunction.moveTo(target);
        if (size == 0) {
            Toast.makeText(this, "No app selected, please select at least one app", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, size + " app" + (size == 1 ? "" : "s") + " have successfully moved to " + target, Toast.LENGTH_SHORT).show();
        }
    }

    public void flush() {
        initServiceFunction();
        serviceFunction.flush();
    }

    public Set<String> getSelectedApp() {
        initServiceFunction();
        return serviceFunction.getSelectSet();
    }

    private void initServiceFunction() {
        if (serviceFunction == null) {
            serviceFunction = new ServiceFunction(this);
        }
        if (!iJoulerBaseServiceBound) {
            Intent intent = new Intent(IJoulerBaseService.class.getName());
            bindService(intent, joulerBaseConnection, BIND_AUTO_CREATE);
        }
    }
}
