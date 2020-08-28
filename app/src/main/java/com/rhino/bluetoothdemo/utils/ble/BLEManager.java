package com.rhino.bluetoothdemo.utils.ble;

import com.inuker.bluetooth.library.BluetoothClient;
import com.rhino.bluetoothdemo.App;

/**
 * @author rhino
 * @since Create on 2020/08/29.
 **/
public class BLEManager {

    private static BluetoothClient mClient;

    public static BluetoothClient getClient() {
        if (mClient == null) {
            synchronized (BLEManager.class) {
                if (mClient == null) {
                    mClient = new BluetoothClient(App.getInstance());
                }
            }
        }
        return mClient;
    }

}