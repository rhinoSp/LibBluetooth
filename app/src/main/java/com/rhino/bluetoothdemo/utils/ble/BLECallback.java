package com.rhino.bluetoothdemo.utils.ble;

/**
 * @author rhino
 * @since Create on 2020/08/29.
 **/
public interface BLECallback {
    void onBLEEvent(BLEEvent event, Object obj);
}
