package com.rhino.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;

import com.rhino.log.LogUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author rhino
 * @since Create on 2020/08/29.
 **/
public class BLEServer {

    /**
     * 蓝牙适配器
     */
    private BluetoothAdapter bluetoothAdapter;
    /**
     * 回调事件
     */
    private BLECallback callback;

    /**
     * Socket用于消息收发
     */
    private BluetoothServerSocket bluetoothServerSocket;
    /**
     * Socket用于消息收发
     */
    private BluetoothSocket bluetoothSocket;

    /**
     * Socket用于消息收发
     */
    private BluetoothServerSocket bluetoothServerSocketLast;
    /**
     * Socket用于消息收发
     */
    private BluetoothSocket bluetoothSocketLast;
    /**
     * 等待连接线程
     */
    private AcceptConnectThread acceptConnectThread;
    /**
     * 读取数据线程
     */
    private ReadThread readThread;

    /**
     * 是否销毁
     */
    private boolean onDestroy = false;

    public BLEServer(BluetoothAdapter bluetoothAdapter, BLECallback callback) {
        this.bluetoothAdapter = bluetoothAdapter;
        this.callback = callback;
    }

    /**
     * 开启下一次等待连接
     */
    private void startNextAcceptConnectThread() {
        if (bluetoothServerSocketLast != null && bluetoothSocketLast != null) {
            try {
                bluetoothServerSocketLast.close();
                bluetoothSocketLast.close();
            } catch (IOException e) {
                LogUtils.e("断开连接失败！", e);
            }
        }
        bluetoothServerSocketLast = bluetoothServerSocket;
        bluetoothSocketLast = bluetoothSocket;
        startAcceptConnectThread();
    }

    /**
     * 发送数据
     */
    public void doWrite(String msg) {
        OutputStream mmOutStream = null;
        try {
            mmOutStream = bluetoothSocket.getOutputStream();
            mmOutStream.write(msg.getBytes());
            LogUtils.i("发送数据成功：" + msg);
            notifyEvent(BLEEvent.WRITE_SUCCESS, "发送数据成功！");
        } catch (Exception e) {
            LogUtils.e("发送数据失败！", e);
            notifyEvent(BLEEvent.WRITE_FAILED, "发送数据失败！" + e.toString());
            try {
                mmOutStream.close();
            } catch (Exception e1) {
                LogUtils.e("发送数据失败！", e);
            }
        }
    }

    /**
     * 开启被连接服务
     */
    public void startAcceptConnectThread() {
        stopAcceptConnectThread();
        acceptConnectThread = new AcceptConnectThread();
        acceptConnectThread.start();
    }

    /**
     * 停止等待连接线程
     */
    public void stopAcceptConnectThread() {
        if (acceptConnectThread != null) {
            acceptConnectThread.interrupt();
            acceptConnectThread = null;
        }
    }

    /**
     * 开启读线程
     */
    public void startReadThread() {
        stopReadThread();
        readThread = new ReadThread();
        readThread.start();
    }

    /**
     * 停止读线程
     */
    public void stopReadThread() {
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
            if (bluetoothServerSocket != null) {
                bluetoothServerSocket.close();
            }
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
        stopAcceptConnectThread();
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
        if (!onDestroy) {
            this.callback.onBLEEvent(event, obj);
        }
    }

    /**
     * 服务器端等待连接线程
     */
    private class AcceptConnectThread extends Thread {

        @Override
        public void run() {
            try {
                bluetoothServerSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(BLEUtils.NAME,
                        BLEUtils.BLE_UUID);
                notifyEvent(BLEEvent.ACCEPT_CONNECTING, "等待客户端连接中！");
                // 等待客户端连接
                BluetoothSocket socket = bluetoothServerSocket.accept();
                bluetoothSocket = socket;
                notifyEvent(BLEEvent.ACCEPT_CONNECT_SUCCESS, "客户端已连接！");
                stopReadThread();
                startReadThread();

                // 等待下一个连接
                startNextAcceptConnectThread();
            } catch (Exception e) {
//                startNextAcceptConnectThread();
                LogUtils.e("等待客户端连接失败！", e);
                notifyEvent(BLEEvent.ACCEPT_CONNECT_FAILED, "等待客户端连接失败！" + e.toString());
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
                    byte[] buf_data = new byte[bytes];
                    for (int i = 0; i < bytes; i++) {
                        buf_data[i] = buffer[i];
                    }
                    String msg = new String(buf_data);
//                    if ("disconnect".equals(msg)) {
//                        stopAcceptConnectThread();
//                        stopReadThread();
//                        disconnect();
//
//                        startReadThread();
//                        continue;
//                    }
                    LogUtils.i("读取数据成功：" + msg);
                    notifyEvent(BLEEvent.READ_SUCCESS, msg);
                }
            } catch (Exception e) {
                if (!onDestroy && bluetoothSocket != null && !bluetoothSocket.isConnected()) {
                    stopAcceptConnectThread();
                    stopReadThread();
                    closeSocket();
                    startNextAcceptConnectThread();
                }

                LogUtils.e("读取数据失败！", e);
                notifyEvent(BLEEvent.READ_FAILED, "读取数据失败！" + e.toString());
                try {
                    inputStream.close();
                } catch (Exception e1) {
                    LogUtils.e(e.toString());
                }
            }
        }
    }
}
