package com.rhino.ble.demo.utils;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;

/**
 * @author LuoLin
 * @since Create on 2019/6/9.
 **/
public class PermissionsUtils {

    public static final int REQUEST_CODE_PERMISSIONS = 1;

    /**
     * Requests permissions needed for recording video.
     */
    public static void requestPermissions(@NonNull Activity activity, @NonNull String[] permissions) {
        ActivityCompat.requestPermissions(activity, permissions, REQUEST_CODE_PERMISSIONS);
    }

    /**
     * Check permissions
     */
    public static boolean checkSelfPermission(@NonNull Context context, @NonNull String[] permissions) {
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check permission when onRequestPermissionsResult
     */
    public static boolean checkHasAllPermission(@NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length == permissions.length) {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        } else {
            return false;
        }
        return true;
    }

}
