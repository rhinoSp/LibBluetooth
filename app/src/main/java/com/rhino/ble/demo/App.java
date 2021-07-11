package com.rhino.ble.demo;

import android.app.Application;

import com.rhino.ble.demo.service.AppService;
import com.rhino.ble.demo.service.BluetoothService;
import com.rhino.ble.demo.utils.SharedPreferencesUtils;
import com.rhino.log.LogUtils;
import com.rhino.log.crash.CrashHandlerUtils;
import com.rhino.log.crash.DefaultCrashHandler;

/**
 * @author LuoLin
 * @since Create on 2020/08/29.
 **/
public class App extends Application {

    private static App instance = null;

    public static App getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        LogUtils.init(this, BuildConfig.DEBUG, false);
        CrashHandlerUtils.getInstance().init(this, new DefaultCrashHandler());
        instance = this;

        SharedPreferencesUtils.getInstance().init(this);
        AppService.startService(this);
        BluetoothService.startService(this, false);
    }
}
