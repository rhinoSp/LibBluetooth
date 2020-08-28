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

    private WeakReference<? extends BLECallback> callBack;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothClient bluetoothClient;
    private static BLEUtils instance;

    private BLEServer bleServer;
    private BLEClient bleClient;

    /**
     * 监听蓝牙状态改变
     */
    private BluetoothStateListener bluetoothStateListener = new BluetoothStateListener() {

        @Override
        public void onBluetoothStateChanged(boolean openOrClosed) {
            if (openOrClosed) {
                callBack.get().onBLEStatusChanged(BLEStatus.BLE_OPEN, "蓝牙开启");
            } else {
                if (bluetoothClient != null) {
                    bluetoothClient.unregisterBluetoothStateListener(bluetoothStateListener);
                }
                stopSearch();
                bleClient.disconnect();
                bleServer.disconnect();
                callBack.get().onBLEStatusChanged(BLEStatus.BLE_CLOSE, "蓝牙关闭");
            }
        }
    };

    /**
     * 搜索监听
     */
    private SearchResponse searchResponse = new SearchResponse() {
        @Override
        public void onSearchStarted() {
            callBack.get().onBLEStatusChanged(BLEStatus.SEARCH_START, "开始搜索");
        }

        @Override
        public void onDeviceFounded(SearchResult device) {
            callBack.get().onBLEStatusChanged(BLEStatus.SEARCH_FOUND_DEVICE, device);
        }

        @Override
        public void onSearchStopped() {
            callBack.get().onBLEStatusChanged(BLEStatus.SEARCH_STOP, "停止搜索");
        }

        @Override
        public void onSearchCanceled() {
            callBack.get().onBLEStatusChanged(BLEStatus.SEARCH_CANCEL, "取消搜索");
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
        this.bleClient = new BLEClient(callBack);
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

    public String getName() {
        return bluetoothAdapter.getName();
    }

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
     * 打开蓝牙
     */
    public void openBluetooth() {
        this.bluetoothClient.registerBluetoothStateListener(bluetoothStateListener);
        this.bluetoothClient.openBluetooth();
    }

    /**
     * 关闭蓝牙
     */
    public void closeBluetooth() {
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
     * 发送数据
     */
    public void serverWrite(String msg) {
        bleServer.doWrite(msg);
    }

    /**
     * 开启等待连接线程
     */
    public void serverStartAcceptConnectThread() {
        bleServer.startAcceptConnectThread();
    }

    /**
     * 发送数据
     */
    public void clientWrite(BluetoothDevice bluetoothDevice, String msg) {
        bleClient.write(bluetoothDevice, msg);
    }

}
