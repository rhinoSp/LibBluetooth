package com.rhino.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.text.TextUtils;

import com.rhino.log.LogUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * @author rhino
 * @since Create on 2020/08/29.
 **/
public class BLEClientClassic {

    /**
     * 蓝牙适配器
     */
    private BluetoothAdapter bluetoothAdapter;
    /**
     * 回调事件
     */
    private BLECallback callback;

    /**
     * 连接成功的蓝牙设备
     */
    private BluetoothDevice bluetoothDeviceConnected;
    /**
     * Socket用于消息收发
     */
    private BluetoothSocket bluetoothSocket;

    /**
     * 连接线程
     */
    private ConnectThread connectThread;
    /**
     * 读取数据线程
     */
    private ReadThread readThread;

    /**
     * 是否销毁
     */
    private boolean onDestroy = false;


    public BLEClientClassic(BluetoothAdapter bluetoothAdapter, BLECallback callback) {
        this.bluetoothAdapter = bluetoothAdapter;
        this.callback = callback;
    }

    /**
     * 连接蓝牙
     */
    public void connect(BluetoothDevice bluetoothDevice) {
        closeSocket();
        startConnectThread(bluetoothDevice, null);
    }

    /**
     * 发送数据,未连接会服务器自动连接
     */
    public void write(BluetoothDevice bluetoothDevice, String msg) {
        if (bluetoothSocket != null && bluetoothSocket.isConnected() && bluetoothDevice.equals(bluetoothDeviceConnected)) {
            // 已连接该设备服务器,直接发送
            notifyEvent(BLEEvent.CONNECTED, "已连接服务器！");
            doWrite(msg);
        } else {
            // 未连接该设备，关闭已连接设备
            closeSocket();
            startConnectThread(bluetoothDevice, msg);
        }
    }

    /**
     * 发送数据
     */
    private void doWrite(String msg) {
        OutputStream outputStream = null;
        try {
            outputStream = bluetoothSocket.getOutputStream();
            outputStream.write(msg.getBytes());
            notifyEvent(BLEEvent.WRITE_SUCCESS, "发送数据成功！");
            LogUtils.i("发送成功：" + msg);
        } catch (Exception e) {
            if (!onDestroy && bluetoothDeviceConnected != null) {
                // 重新连接
                BluetoothDevice bluetoothDevice = bluetoothDeviceConnected;
                disconnect();
                write(bluetoothDevice, msg);
            }
            notifyEvent(BLEEvent.WRITE_FAILED, "发送数据失败！" + e.toString());
            LogUtils.e("发送数据失败！" + e.toString());
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (Exception e1) {
                LogUtils.e("关闭发送数据流失败！" + e.toString());
            }
        }
    }

    /**
     * 开启连接线程
     */
    private void startConnectThread(BluetoothDevice bluetoothDevice, String msg) {
        stopConnectThread();
        connectThread = new ConnectThread(bluetoothDevice, msg);
        connectThread.start();
    }

    /**
     * 停止连接线程
     */
    private void stopConnectThread() {
        if (connectThread != null) {
            connectThread.interrupt();
            connectThread = null;
        }
    }

    /**
     * 开启读线程
     */
    private void startReadThread() {
        stopReadThread();
        readThread = new ReadThread();
        readThread.start();
    }

    /**
     * 停止读线程
     */
    private void stopReadThread() {
        if (readThread != null) {
            readThread.interrupt();
            readThread = null;
        }
    }

    /**
     * 关闭套接字连接
     */
    private void closeSocket() {
        try {
            bluetoothDeviceConnected = null;
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
            }
        } catch (IOException e) {
            LogUtils.e("断开连接失败！", e);
        }
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        closeSocket();
        stopConnectThread();
        stopReadThread();
    }

    /**
     * 不再使用时调用
     */
    public void onDestroy() {
        onDestroy = true;
        disconnect();
    }

    /**
     * 通知
     */
    private void notifyEvent(BLEEvent event, Object obj) {
        this.callback.onBLEEvent(event, obj);
    }

    /**
     * 客户端连接线程
     */
    private class ConnectThread extends Thread {

        private BluetoothDevice bluetoothDevice;
        private String msg;

        public ConnectThread(BluetoothDevice bluetoothDevice, String msg) {
            this.bluetoothDevice = bluetoothDevice;
            this.msg = msg;
        }

        @Override
        public void run() {
            try {
                bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(UUID.fromString(BLEUtils.BLE_CLASSIC_UUID));
                notifyEvent(BLEEvent.CONNECTING, "连接服务器中！");
                bluetoothSocket.connect();
                notifyEvent(BLEEvent.CONNECT_SUCCESS, "连接服务器成功！");

                // 保存连接成功的设备
                bluetoothDeviceConnected = bluetoothDevice;
                // 开启读线程
                startReadThread();
                // 发送数据
                if (!TextUtils.isEmpty(msg)) {
                    doWrite(msg);
                }
            } catch (Exception e) {
                notifyEvent(BLEEvent.READ_FAILED, "连接服务器失败！" + e.toString());
                LogUtils.e("连接服务器失败！", e);
            }
        }
    }

    /**
     * 读取数据线程
     */
    private class ReadThread extends Thread {
        @Override
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;
            InputStream inputStream = null;
            try {
                inputStream = bluetoothSocket.getInputStream();
                while (!isInterrupted() && (bytes = inputStream.read(buffer)) > 0) {
                    // 循环读取
                    byte[] buf_data = new byte[bytes];
                    for (int i = 0; i < bytes; i++) {
                        buf_data[i] = buffer[i];
                    }
                    String msg = new String(buf_data);
                    notifyEvent(BLEEvent.READ_SUCCESS, msg);
                    LogUtils.i("已经接收到数据：" + msg);
                }
            } catch (Exception e) {
//                if (!onDestroy && bluetoothDeviceConnected != null) {
//                    BluetoothDevice bluetoothDevice = bluetoothDeviceConnected;
//                    disconnect();
//                    write(bluetoothDevice, "1");
//                }
                notifyEvent(BLEEvent.READ_FAILED, "读取数据失败！" + e.toString());
                LogUtils.e("读取数据失败！", e);
                try {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                } catch (Exception e1) {
                    LogUtils.e("关闭读取数据流失败！", e);
                }
            }
        }
    }
}
