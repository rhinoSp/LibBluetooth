package com.rhino.bluetoothdemo.utils.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;

import com.inuker.bluetooth.library.BluetoothClient;
import com.inuker.bluetooth.library.connect.listener.BluetoothStateListener;
import com.inuker.bluetooth.library.search.SearchRequest;
import com.inuker.bluetooth.library.search.SearchResult;
import com.inuker.bluetooth.library.search.response.SearchResponse;
import com.rhino.bluetoothdemo.App;
import com.rhino.log.LogUtils;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * @author rhino
 * @since Create on 2020/08/29.
 **/
public class BLEUtils {

    /**
     * 用于蓝牙之间通信的uuid
     */
    public final static UUID BLE_UUID = UUID.fromString("00002a05-0000-1000-8000-00805f9b34fb");
    public static final String NAME = "BLE";

    /**
     * 单例对象
     */
    private static BLEUtils instance;
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
    private BLEClient bleClient;

    /**
     * 监听蓝牙状态改变
     */
    private BluetoothStateListener bluetoothStateListener = new BluetoothStateListener() {

        @Override
        public void onBluetoothStateChanged(boolean openOrClosed) {
            if (openOrClosed) {
                callBack.get().onBLEEvent(BLEEvent.BLE_OPEN, "蓝牙开启");
            } else {
                if (bluetoothClient != null) {
                    bluetoothClient.unregisterBluetoothStateListener(bluetoothStateListener);
                }
                stopSearch();
                bleClient.disconnect();
                bleServer.disconnect();
                callBack.get().onBLEEvent(BLEEvent.BLE_CLOSE, "蓝牙关闭");
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
        if (instance == null) {
            instance = new BLEUtils();
        }
        return instance;
    }

    private BLEUtils() {
    }

    /**
     * 在activity.onCreate()中调用
     */
    public void onCreate(BLECallback callBack) {
        this.callBack = new WeakReference<>(callBack);
        if (this.bluetoothClient == null) {
            this.bluetoothClient = BLEManager.getClient();
        }
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.bleServer = new BLEServer(bluetoothAdapter, callBack);
        this.bleClient = new BLEClient(bluetoothAdapter, callBack);
    }

    /**
     * 在activity.onDestroy()中调用，回收资源
     */
    public void onDestroy() {
        if (bluetoothClient != null) {
            bluetoothClient.unregisterBluetoothStateListener(bluetoothStateListener);
        }
        callBack.clear();
        stopSearch();
        bleServer.onDestroy();
        bleClient.onDestroy();
        bluetoothClient = null;
        instance = null;
    }

    /**
     * 获取当前蓝牙名称
     */
    public String getName() {
        return bluetoothAdapter.getName();
    }

    /**
     * 获取当前蓝牙地址
     */
    public String getAddress() {
        return bluetoothAdapter.getAddress();
    }

    /**
     * 获取已经配对的设备
     */
    public List<BluetoothDevice> getBondedDevices() {
        List<BluetoothDevice> devices = new ArrayList<>();
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
    public void setDiscoverable(int second) {
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, second);
        discoverableIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        App.getInstance().startActivity(discoverableIntent);
    }

    /**
     * 打开蓝牙
     */
    public void open() {
        this.bluetoothClient.registerBluetoothStateListener(bluetoothStateListener);
        this.bluetoothClient.openBluetooth();
    }

    /**
     * 关闭蓝牙
     */
    public void close() {
        if (bluetoothClient != null) {
            bluetoothClient.unregisterBluetoothStateListener(bluetoothStateListener);
        }
        stopSearch();
        bleServer.disconnect();
        bleClient.disconnect();
        this.bluetoothClient.registerBluetoothStateListener(bluetoothStateListener);
        this.bluetoothClient.closeBluetooth();
    }

    /**
     * 开始搜索
     */
    public void startSearch() {
        SearchRequest request = new SearchRequest.Builder()
                .searchBluetoothLeDevice(3000, 3)   // 先扫BLE设备3次，每次3s
                .searchBluetoothClassicDevice(5000) // 再扫经典蓝牙5s
                .searchBluetoothLeDevice(2000)      // 再扫BLE设备2s
                .build();
        bluetoothClient.search(request, searchResponse);
    }

    /**
     * 停止搜索
     */
    public void stopSearch() {
        if (bluetoothClient != null) {
            bluetoothClient.stopSearch();
        }
    }

    /**
     * 蓝牙是否打开
     */
    public boolean isBluetoothOpened() {
        return bluetoothClient.isBluetoothOpened();
    }

    /**
     * 服务端-发送数据
     */
    public void serverWrite(String msg) {
        bleServer.doWrite(msg);
    }

    /**
     * 服务端-开启等待连接线程
     */
    public void serverStartAcceptConnectThread() {
        bleServer.startAcceptConnectThread();
    }

    /**
     * 客户端-发送数据
     */
    public void clientWrite(BluetoothDevice bluetoothDevice, String msg) {
        bleClient.write(bluetoothDevice, msg);
    }

}
