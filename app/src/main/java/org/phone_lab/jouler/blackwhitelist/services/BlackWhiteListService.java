package org.phone_lab.jouler.blackwhitelist.services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

import org.phone_lab.jouler.blackwhitelist.activities.App;

import java.io.FileDescriptor;
import java.util.Random;

/**
 * Created by xcv58 on 1/23/15.
 */
public class BlackWhiteListService extends Service {
    private ServiceFunction serviceFunction;

    public void setTarget(String target) {
        initServiceFunction();
        serviceFunction.setTarget(target);
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

    public boolean isTargetApp(String packageName, String target) {
        this.initServiceFunction();
        return serviceFunction.isTargetApp(packageName, target);
    }

    public void select(App app) {
        initServiceFunction();
        serviceFunction.select(app);
    }

    public void flush() {
        initServiceFunction();
        serviceFunction.flush();
    }

    private void initServiceFunction() {
        if (serviceFunction == null) {
            serviceFunction = new ServiceFunction(this);
        }
    }
}
