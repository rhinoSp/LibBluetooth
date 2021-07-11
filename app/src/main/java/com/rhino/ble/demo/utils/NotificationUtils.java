package com.rhino.ble.demo.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;

/**
 * @author rhino
 * @since Create on 2019/2/9.
 **/
public class NotificationUtils {

    public static Notification.Builder builder(Context context,
                                               Class<?> activityClass,
                                               String channelId,
                                               String channelName,
                                               int largeIconResId,
                                               int smallIconResId,
                                               String contentTitle,
                                               String contentText) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(channelId,
                    channelName, NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setShowBadge(true);
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(notificationChannel);
        }
        Notification.Builder builder = new Notification.Builder(context);
        Intent notificationIntent = new Intent(context, activityClass);
        // 设置PendingIntent
        builder.setContentIntent(PendingIntent.getActivity(context, 0, notificationIntent, 0))
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), largeIconResId))  // 设置下拉列表中的图标(大图标)
                .setContentTitle(contentTitle) // 设置下拉列表里的标题
                .setSmallIcon(smallIconResId) // 设置状态栏内的小图标
                .setContentText(contentText) // 设置上下文内容
                .setWhen(System.currentTimeMillis()); // 设置该通知发生的时间
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(channelId);
        }
        return builder;
    }

}
