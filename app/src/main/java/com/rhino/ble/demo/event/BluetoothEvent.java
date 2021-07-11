package com.rhino.ble.demo.event;

import com.rhino.ble.BLEEvent;

/**
 * @author rhino
 * @since Create on 2021/1/9.
 **/
public class BluetoothEvent {

    public BLEEvent event;
    public Object obj;

    private BluetoothEvent() {
    }

    public static BluetoothEvent create(BLEEvent event, Object obj) {
        BluetoothEvent bluetoothEvent = new BluetoothEvent();
        bluetoothEvent.event = event;
        bluetoothEvent.obj = obj;
        return bluetoothEvent;
    }
}
