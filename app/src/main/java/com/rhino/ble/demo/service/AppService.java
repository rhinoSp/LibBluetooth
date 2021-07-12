package com.rhino.ble.demo.service;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.rhino.ble.demo.page.MainActivity;
import com.rhino.ble.demo.utils.NotificationUtils;
import com.rhino.ble.demo.R;
import com.rhino.ble.demo.event.TimerEvent;
import com.rhino.ble.demo.utils.TimeUtils;
import com.rhino.log.LogUtils;

/**
 * @author LuoLin
 * @since Create on 2020/08/29.
 **/
public class AppService extends ABaseService {

    public static final int NOTICE_ID = 100;

    public static void startService(Context context) {
        Intent intent = new Intent(context, AppService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        LogUtils.i("onCreate");
        Notification.Builder builder = NotificationUtils.builder(this,
                MainActivity.class,
                "channel_id",
                "channelName",
                R.mipmap.ic_rhino_launcher,
                R.mipmap.ic_rhino_launcher,
                getString(R.string.app_name) + "正在运行",
                "点击查看详情");
        Notification notification = builder.build(); // 获取构建好的Notification
        notification.defaults = Notification.DEFAULT_SOUND; // 设置为默认的声音
        startForeground(NOTICE_ID, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtils.i("onStartCommand");
        postDelayTimer(0);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogUtils.i("onDestroy");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        LogUtils.i("onBind");
        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        LogUtils.i("onUnbind");
        return super.onUnbind(intent);
    }

    /**
     * 检测蓝牙连接
     */
    private void checkBluetoothConnect() {
        if (!TextUtils.isEmpty(BluetoothService.getAutoConnectBluetoothMac())
                && BluetoothService.getBluetoothDeviceConnected() == null
                && !BluetoothService.isSearching()
                && System.currentTimeMillis() / 1000 % 5 == 0) {
            // 连接过，且未连接开启搜索自动连接
            LogUtils.w("连接过，且未连接开启搜索自动连接");
            if (!BluetoothService.checkAutoConnect()) {
                if (BluetoothService.isBluetoothOpened()) {
                    BluetoothService.startSearch();
                } else {
                    BluetoothService.open();
                }
            }
        }
    }

    /**
     * 定时器事件，一秒触发一次
     */
    private void onTimerEvent(TimerEvent timerEvent) {
        // 检测蓝牙连接
        checkBluetoothConnect();
    }

    /**
     * 开启定时器
     */
    private void postDelayTimer(long delayMillis) {
        removeTimer();
        mHandler.postDelayed(timerRunnable, delayMillis);
    }

    /**
     * 关闭定时器
     */
    private void removeTimer() {
        mHandler.removeCallbacks(timerRunnable);
    }

    /**
     * 定时器
     */
    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            postDelayTimer(1000);
            onTimerEvent(TimerEvent.currentTimerEvent());
        }
    };


}
