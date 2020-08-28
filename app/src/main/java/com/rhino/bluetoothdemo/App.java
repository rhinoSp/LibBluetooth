package com.rhino.bluetoothdemo;

import android.app.Application;

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
    }
}
