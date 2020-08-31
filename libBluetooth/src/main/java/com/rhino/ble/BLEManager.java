package com.rhino.ble;

import android.content.Context;

import com.inuker.bluetooth.library.BluetoothClient;

/**
 * @author rhino
 * @since Create on 2020/08/29.
 **/
public class BLEManager {

    private static BluetoothClient mClient;

    public static BluetoothClient getClient(Context context) {
        if (mClient == null) {
            synchronized (BLEManager.class) {
                if (mClient == null) {
                    mClient = new BluetoothClient(context);
                }
            }
        }
        return mClient;
    }

}