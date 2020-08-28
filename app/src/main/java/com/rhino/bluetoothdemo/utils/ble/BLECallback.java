package com.rhino.bluetoothdemo.utils.ble;

/**
 * @author rhino
 * @since Create on 2020/08/29.
 **/
public interface BLECallback {
    void onBLEStatusChanged(int bleStatus, Object obj);
}
