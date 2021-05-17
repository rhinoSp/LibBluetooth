package com.rhino.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;

import com.inuker.bluetooth.library.BluetoothClient;
import com.inuker.bluetooth.library.connect.listener.BluetoothStateListener;
import com.inuker.bluetooth.library.search.SearchRequest;
import com.inuker.bluetooth.library.search.SearchResult;
import com.inuker.bluetooth.library.search.response.SearchResponse;
import com.rhino.log.LogUtils;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author rhino
 * @since Create on 2020/08/29.
 **/
public class BLEUtils {

    /**
     * 传统蓝牙用于服务端蓝牙监听连接name
     */
    public static String NAME = "BLE";
    /**
     * 传统蓝牙之间通信的uuid
     */
    public static String BLE_CLASSIC_UUID = "00002a05-0000-1000-8000-00805f9b34fb";
    /**
     * 低功耗蓝牙的特征值uuid，发送
     */
    public static String BLE_lE_UUID_SERVICE_EIGENVALUE_SEND = "0000ffe1-0000-1000-8000-00805f9b34fb";
    /**
     * 低功耗蓝牙的特征值uuid，接收
     */
    public static String BLE_LE_UUID_SERVICE_EIGENVALUE_READ = "00002902-0000-1000-8000-00805f9b34fb";

    /**
     * 弱引用的回调
     */
    private WeakReference<? extends BLECallback> callBack;

    /**
     * 蓝牙适配器
     */
    private BluetoothAdapter bluetoothAdapter;
    /**
     * 蓝牙BluetoothClient实例
     */
    private BluetoothClient bluetoothClient;

    /**
     * 服务端
     */
    private BLEServer bleServer;
    /**
     * 客户端
     */
    private BLEClientClassic bleClientClassic;
    /**
     * 客户端
     */
    private BLEClientLe bleClientLe;

    /**
     * 监听蓝牙状态改变
     */
    private BluetoothStateListener bluetoothStateListener = new BluetoothStateListener() {

        @Override
        public void onBluetoothStateChanged(boolean openOrClosed) {
            if (openOrClosed) {
                LogUtils.d("蓝牙已开启");
                callBack.get().onBLEEvent(BLEEvent.BLE_OPEN, "蓝牙已开启");
            } else {
                LogUtils.d("蓝牙已关闭");
                if (bluetoothClient != null) {
                    bluetoothClient.unregisterBluetoothStateListener(bluetoothStateListener);
                }
                stopSearch();
                bleServer.disconnect();
                bleClientClassic.disconnect();
                bleClientLe.disconnect();
                callBack.get().onBLEEvent(BLEEvent.BLE_CLOSE, "蓝牙已关闭");
            }
        }
    };

    /**
     * 搜索监听
     */
    private SearchResponse searchResponse = new SearchResponse() {
        @Override
        public void onSearchStarted() {
            callBack.get().onBLEEvent(BLEEvent.SEARCH_START, "开始搜索");
        }

        @Override
        public void onDeviceFounded(SearchResult device) {
            callBack.get().onBLEEvent(BLEEvent.SEARCH_FOUND_DEVICE, device);
        }

        @Override
        public void onSearchStopped() {
            callBack.get().onBLEEvent(BLEEvent.SEARCH_STOP, "停止搜索");
        }

        @Override
        public void onSearchCanceled() {
            callBack.get().onBLEEvent(BLEEvent.SEARCH_CANCEL, "取消搜索");
        }
    };

    public static BLEUtils getInstance() {
        return Builder.instance;
    }

    public static class Builder {
        private static BLEUtils instance = new BLEUtils();
    }

    private BLEUtils() {
    }

    /**
     * 在activity.onCreate()中调用
     */
    public void onCreate(Context context, BLECallback callBack) {
        this.callBack = new WeakReference<>(callBack);
        if (this.bluetoothClient == null) {
            this.bluetoothClient = BLEManager.getClient(context);
        }
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.bleServer = new BLEServer(bluetoothAdapter, callBack);
        this.bleClientClassic = new BLEClientClassic(bluetoothAdapter, callBack);
        this.bleClientLe = new BLEClientLe(context, bluetoothAdapter, callBack);
    }

    /**
     * 在activity.onDestroy()中调用，回收资源
     */
    public void onDestroy() {
        if (bluetoothClient != null) {
            bluetoothClient.unregisterBluetoothStateListener(bluetoothStateListener);
        }
        if (callBack != null) {
            callBack.clear();
        }
        stopSearch();
        if (bleServer != null) {
            bleServer.onDestroy();
        }
        if (bleClientClassic != null) {
            bleClientClassic.onDestroy();
        }
        if (bleClientLe != null) {
            bleClientLe.onDestroy();
        }
        bluetoothClient = null;
    }

    /**
     * 获取已经配对的设备
     */
    public List<BluetoothDevice> getBondedDevices() {
        List<BluetoothDevice> devices = new ArrayList<>();
        if (bluetoothAdapter == null) {
            return devices;
        }
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        // 判断是否有配对过的设备
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                devices.add(device);
                LogUtils.d("Bonded bluetooth device: " + device.getName());
            }
        }
        return devices;
    }

    /**
     * 与设备配对
     */
    public void createBond(BluetoothDevice device) {
        try {
            Method createBondMethod = BluetoothDevice.class.getMethod("createBond");
            createBondMethod.invoke(device);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置蓝牙可被发现
     */
    public void setDiscoverable(Context context, int second) {
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, second);
        discoverableIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(discoverableIntent);
    }

    /**
     * 打开蓝牙
     */
    public void open() {
        if (bluetoothClient == null) {
            return;
        }
        bluetoothClient.registerBluetoothStateListener(bluetoothStateListener);
        bluetoothClient.openBluetooth();
    }

    /**
     * 关闭蓝牙
     */
    public void close() {
        if (bluetoothClient != null) {
            bluetoothClient.unregisterBluetoothStateListener(bluetoothStateListener);
        }
        stopSearch();
        if (bleServer != null) {
            bleServer.disconnect();
        }
        if (bleClientLe != null) {
            bleClientLe.disconnect();
        }
        if (bluetoothClient != null) {
            bluetoothClient.registerBluetoothStateListener(bluetoothStateListener);
            bluetoothClient.closeBluetooth();
        }
    }

    /**
     * 开始搜索
     */
    public void startSearch() {
        if (bluetoothClient == null) {
            return;
        }
        SearchRequest request = new SearchRequest.Builder()
                .searchBluetoothLeDevice(5000, 3) // 先扫BLE设备3次，每次3s
                .searchBluetoothClassicDevice(5000) // 再扫经典蓝牙5s
                .searchBluetoothLeDevice(2000) // 再扫BLE设备2s
                .build();
        startSearch(request);
    }

    /**
     * 开始搜索
     */
    public void startSearch(SearchRequest searchRequest) {
        if (bluetoothClient == null) {
            return;
        }
        bluetoothClient.search(searchRequest, searchResponse);
    }

    /**
     * 停止搜索
     */
    public void stopSearch() {
        if (bluetoothClient == null) {
            return;
        }
        bluetoothClient.stopSearch();
    }

    /**
     * 蓝牙是否打开
     */
    public boolean isBluetoothOpened() {
        if (bluetoothClient == null) {
            return false;
        }
        return bluetoothClient.isBluetoothOpened();
    }

    /**
     * 服务端-发送数据
     */
    public void serverWrite(String msg) {
        if (bleServer == null) {
            return;
        }
        bleServer.doWrite(msg);
    }

    /**
     * 服务端-开启等待连接线程
     */
    public void serverStartAcceptConnectThread() {
        if (bleServer == null) {
            return;
        }
        bleServer.startAcceptConnectThread();
    }

    /**
     * 客户端-断开连接
     */
    public void clientDisconnect() {
        if (bleClientLe != null) {
            bleClientLe.disconnect();
        }
        if (bleClientClassic != null) {
            bleClientClassic.disconnect();
        }
        if (callBack != null && callBack.get() != null) {
            callBack.get().onBLEEvent(BLEEvent.DISCONNECTED, "蓝牙断开连接");
        }
    }

    /**
     * 客户端-连接服务
     */
    public void clientConnect(BluetoothDevice bluetoothDevice) {
        if (isBLE(bluetoothDevice)) {
            if (bleClientLe != null) {
                bleClientLe.connect(bluetoothDevice);
            }
        } else {
            if (bleClientClassic != null) {
                bleClientClassic.connect(bluetoothDevice);
            }
        }
    }

    /**
     * 客户端-发送数据
     */
    public void clientWrite(BluetoothDevice bluetoothDevice, String msg) {
        if (isBLE(bluetoothDevice)) {
            if (bleClientLe != null) {
                bleClientLe.write(bluetoothDevice, msg);
            }
        } else {
            if (bleClientClassic != null) {
                bleClientClassic.write(bluetoothDevice, msg);
            }
        }
    }

    /**
     * 判断蓝牙是否ble蓝牙
     */
    public static boolean isBLE(BluetoothDevice bluetoothDevice) {
        return bluetoothDevice.getType() == BluetoothDevice.DEVICE_TYPE_LE;
    }

    /**
     * 获取当前蓝牙名称
     */
    public static String getName() {
        return BluetoothAdapter.getDefaultAdapter().getName();
    }

    /**
     * 设置蓝牙名称
     */
    public static void setName(String name) {
        BluetoothAdapter.getDefaultAdapter().setName(name);
    }

    /**
     * 获取当前蓝牙地址
     */
    public static String getAddress() {
        return BluetoothAdapter.getDefaultAdapter().getAddress();
    }

}
