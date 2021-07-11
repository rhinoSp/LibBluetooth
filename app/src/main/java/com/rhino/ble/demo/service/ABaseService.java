package com.rhino.ble.demo.service;

import android.app.Service;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;

import java.lang.ref.WeakReference;

/**
 * @author LuoLin
 * @since Create on 2020/08/29.
 **/
abstract public class ABaseService extends Service {

    public ABaseService mService;
    public MyHandler mHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        mService = this;
        mHandler = new MyHandler(this);
    }

    public void handleOsMessage(@NonNull Message data) {

    }

    public static class MyHandler extends Handler {
        private WeakReference<ABaseService> reference;

        public MyHandler(ABaseService o) {
            this.reference = new WeakReference(o);
        }

        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            ABaseService o = (ABaseService) this.reference.get();
            if (null != o) {
                o.handleOsMessage(msg);
            }
        }
    }

}
