package com.rhino.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.os.Build;

import com.rhino.log.LogUtils;

import java.util.List;
import java.util.UUID;

/**
 * @author rhino
 * @since Create on 2020/08/29.
 **/
public class BLEClientLe {
    /**
     * 上下文
     */
    private Context context;
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
     * 连接线程
     */
    private ConnectThread connectThread;
    /**
     * 扫描蓝牙服务线程
     */
    private DiscoverServicesThread discoverServicesThread;

    /**
     * 是否销毁
     */
    private boolean onDestroy = false;

    private BluetoothGatt bluetoothGatt;
    /**
     * 写数据特征
     */
    private BluetoothGattCharacteristic bluetoothGattCharacteristic;
    /**
     * 服务UUID
     */
    private String serviceUUID;
    /**
     * 读写UUID
     */
    private String readWriteUUID;


    public BLEClientLe(Context context, BluetoothAdapter bluetoothAdapter, BLECallback callback) {
        this.context = context;
        this.bluetoothAdapter = bluetoothAdapter;
        this.callback = callback;
    }

    /**
     * 连接服务器
     */
    public void connect(BluetoothDevice bluetoothDevice) {
        startConnectThread(bluetoothDevice);
    }

    /**
     * 发送数据,未连接会服务器自动连接
     */
    public void write(BluetoothDevice bluetoothDevice, String msg) {
        if (isConnected() && bluetoothDevice.equals(bluetoothDeviceConnected)) {
            // 已连接该设备服务器,直接发送
            doWrite(msg);
        } else {
            // 未连接该设备，关闭已连接设备
            LogUtils.w("未连接服务");
            notifyEvent(BLEEvent.WRITE_FAILED, "未连接服务");
        }
    }

    /**
     * 发送数据
     */
    private void doWrite(String msg) {
        LogUtils.d("发送数据:" + msg);
        bluetoothGattCharacteristic.setValue(msg.getBytes());
        bluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic);
    }

    /**
     * 开启连接线程
     */
    private void startConnectThread(BluetoothDevice bluetoothDevice) {
        LogUtils.d("开始蓝牙连接线程");
        stopConnectThread();
        connectThread = new ConnectThread(bluetoothDevice);
        connectThread.start();
    }

    /**
     * 开启扫描蓝牙服务线程
     */
    private void startDiscoverServicesThread() {
        LogUtils.d("开始扫描服务线程");
        stopDiscoverServicesThread();
        discoverServicesThread = new DiscoverServicesThread();
        discoverServicesThread.start();
    }

    /**
     * 停止连接线程
     */
    private void stopConnectThread() {
        LogUtils.d("停止蓝牙连接线程");
        if (connectThread != null) {
            connectThread.interrupt();
            connectThread = null;
        }
    }

    /**
     * 停止扫描蓝牙服务线程
     */
    private void stopDiscoverServicesThread() {
        LogUtils.d("停止扫描服务线程");
        if (discoverServicesThread != null) {
            discoverServicesThread.interrupt();
            discoverServicesThread = null;
        }
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        LogUtils.d("断开蓝牙连接");
        if (bluetoothGatt != null) {
            bluetoothGatt.disconnect();
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
        stopConnectThread();
        stopDiscoverServicesThread();
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

        public ConnectThread(BluetoothDevice bluetoothDevice) {
            this.bluetoothDevice = bluetoothDevice;
        }

        @Override
        public void run() {
            try {
                LogUtils.d("连接服务中，" + bluetoothDevice.getName());
                notifyEvent(BLEEvent.CONNECTING, "连接服务中");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    bluetoothGatt = bluetoothDevice.connectGatt(context, false, bluetoothGattCallback, BluetoothDevice.TRANSPORT_LE);
                } else {
                    bluetoothGatt = bluetoothDevice.connectGatt(context, false, bluetoothGattCallback);
                }
                bluetoothDeviceConnected = bluetoothDevice;
            } catch (Exception e) {
                notifyEvent(BLEEvent.CONNECT_FAILED, "连接服务器失败" + e.toString());
                LogUtils.e("连接服务器失败", e);
            }
        }
    }

    /**
     * 获取蓝牙服务
     */
    private class DiscoverServicesThread extends Thread {

        public boolean discovered;

        @Override
        public void run() {
            // 延迟扫描服务（设置延迟时间过短，很可能发现不了服务）
            LogUtils.d("延迟扫描服务");
            threadSleep(1500);
            if (detectionGattValid()) {
                return;
            }
            LogUtils.d("开始扫描服务");
            bluetoothGatt.discoverServices();

            // 扫描服务超时
            LogUtils.d("开启扫描服务超时监听");
            threadSleep(5000);
            if (discovered) {
                // 已扫描到服务
                LogUtils.d("已扫描到服务");
                return;
            }
            LogUtils.d("扫描服务超时");
            notifyEvent(BLEEvent.CONNECT_FAILED, "扫描服务超时");
        }

        public void setDiscovered(boolean discovered) {
            this.discovered = discovered;
        }
    }


    /**
     * 蓝牙连接完的所有回调
     */
    private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            // 当连接上设备或者失去连接时会回调该函数
            if (newState == 133) {
                LogUtils.d("出现133问题，需要扫描重连");
                notifyEvent(BLEEvent.UNKNOWN, "出现133问题，需要扫描重连");
            }
            switch (newState) {
                case BluetoothGatt.STATE_CONNECTED:
                    LogUtils.d("蓝牙连接成功，开始获取服务UUID");
                    if (detectionGattValid()) {
                        return;
                    }
                    startDiscoverServicesThread();
                    break;
                case BluetoothGatt.STATE_DISCONNECTED:
                    LogUtils.d("蓝牙断开");
                    notifyEvent(BLEEvent.DISCONNECTED, "蓝牙断开链接");
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            // 当向Characteristic写数据时会回调该函数
            if (status != BluetoothGatt.GATT_SUCCESS) {
                LogUtils.w("received: " + status);
                return;
            }
            notifyEvent(BLEEvent.WRITE_SUCCESS, "发送数据成功");
            LogUtils.i("发送成功：" + new String(characteristic.getValue()));
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            //当向设备Descriptor中写数据时，会回调该函数
            if (status != BluetoothGatt.GATT_SUCCESS) {
                LogUtils.w("received: " + status);
                return;
            }
            //mBluetoothGatt.writeDescriptor(descriptor);
            //来到这里，才算真正的建立连接
            LogUtils.d("设置监听成功，" + gatt.getDevice().getName() + ", " + gatt.getDevice().getAddress());
            notifyEvent(BLEEvent.CONNECTED, "设置监听成功");
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            // 当设备是否找到服务时，会回调该函数
            // （思想：获取该模块的所有服务，然后再轮询服务下面的所有特征的UUID，再与汇承的UUID比较
            // 　　　　找到汇承的UUID后，建立监听模块数据的回调才算完成真正的连接。）
            if (status != BluetoothGatt.GATT_SUCCESS) {
                LogUtils.w("received: " + status);
                return;
            }
            if (detectionGattValid()) {
                return;
            }
            if (discoverServicesThread != null) {
                discoverServicesThread.setDiscovered(true);
            }
            List<BluetoothGattService> bluetoothGattServices = bluetoothGatt.getServices();//获取模块的所有服务
            LogUtils.d("扫描到服务的个数:" + bluetoothGattServices.size());
            int i = 0;
            for (final BluetoothGattService bluetoothGattService : bluetoothGattServices) {
                ++i;
                LogUtils.d(i + "号服务的uuid: " + bluetoothGattService.getUuid().toString());
                List<BluetoothGattCharacteristic> bluetoothGattCharacteristics = bluetoothGattService.getCharacteristics();//获取单个服务下的所有特征
                int j = 0;
                for (BluetoothGattCharacteristic bluetoothGattCharacteristic : bluetoothGattCharacteristics) {
                    ++j;
                    if (bluetoothGattCharacteristic.getUuid().toString().equals(BLEUtils.BLE_lE_UUID_SERVICE_EIGENVALUE_SEND)) {
                        LogUtils.d(i + "号服务的第" + j + "个特征" + bluetoothGattCharacteristic.getUuid().toString());
                        serviceUUID = bluetoothGattService.getUuid().toString();
                        readWriteUUID = bluetoothGattCharacteristic.getUuid().toString();
                        BLEClientLe.this.bluetoothGattCharacteristic = bluetoothGattCharacteristic;
                        LogUtils.d("发送特征：" + BLEClientLe.this.bluetoothGattCharacteristic.getUuid().toString());
                        bluetoothGatt.setCharacteristicNotification(BLEClientLe.this.bluetoothGattCharacteristic, true);

                        BluetoothGattDescriptor clientConfig = BLEClientLe.this.bluetoothGattCharacteristic.getDescriptor(UUID.fromString(BLEUtils.BLE_LE_UUID_SERVICE_EIGENVALUE_READ));//这个收取数据的UUID
                        if (clientConfig != null) {
                            //BluetoothGatt.getService(service)
                            clientConfig.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);//设置接收模式
                            bluetoothGatt.writeDescriptor(clientConfig);//必须是设置这个才能监听模块数据
                        } else {
                            LogUtils.d("备用方法测试");
                            BluetoothGattService linkLossService = gatt.getService(bluetoothGattService.getUuid());
                            setNotification(bluetoothGatt, linkLossService.getCharacteristic(UUID.fromString(BLEUtils.BLE_LE_UUID_SERVICE_EIGENVALUE_READ)), true);
                        }
                        notifyEvent(BLEEvent.CONNECT_SUCCESS, "连接服务器成功");
                    } else {
                        LogUtils.d(i + "号服务的第" + j + "个特征" + bluetoothGattCharacteristic.getUuid().toString());
                    }
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            // 设备发出通知时会调用到该接口，蓝牙模块发送的所有数据都会回调到这里
            notifyEvent(BLEEvent.READ_SUCCESS, new String(characteristic.getValue()));
        }
    };

    private boolean isConnected() {
        return bluetoothGatt != null && bluetoothGattCharacteristic != null;
    }

    private boolean detectionGattValid() {
        if (bluetoothGatt == null) {
            LogUtils.d("出现未知错误，服务关闭，GATT is null");
            notifyEvent(BLEEvent.UNKNOWN, "未知错误");
            return false;
        }
        return true;
    }

    public void setNotification(BluetoothGatt bluetoothGatt, BluetoothGattCharacteristic characteristic, boolean enable) {
        if (bluetoothGatt == null || characteristic == null) {
            LogUtils.d("bluetoothGatt == null || characteristic == null");
            return;
        }
        boolean success = bluetoothGatt.setCharacteristicNotification(characteristic, enable);
        LogUtils.d("setNotification: " + success);
        if (success) {
            for (final BluetoothGattDescriptor bluetoothGattDescriptor : characteristic.getDescriptors()) {
                if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                    bluetoothGattDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    LogUtils.d("路线1");
                } else if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) {
                    bluetoothGattDescriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                    LogUtils.d("路线2");
                } else {
                    LogUtils.d("没有走");
                }
                bluetoothGatt.writeDescriptor(bluetoothGattDescriptor);
                LogUtils.d("监听的特征是: " + bluetoothGattDescriptor.getUuid().toString());
            }
        }
    }

    /**
     * sleep
     */
    private void threadSleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            LogUtils.e(e);
        }
    }
}
