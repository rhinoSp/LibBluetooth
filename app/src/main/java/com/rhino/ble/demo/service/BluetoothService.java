package com.rhino.ble.demo.service;

import android.app.Notification;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.text.TextUtils;

import com.inuker.bluetooth.library.search.SearchResult;
import com.rhino.ble.BLECallback;
import com.rhino.ble.BLEEvent;
import com.rhino.ble.BLEUtils;
import com.rhino.ble.demo.R;
import com.rhino.ble.demo.event.BluetoothEvent;
import com.rhino.ble.demo.page.MainActivity;
import com.rhino.ble.demo.utils.NotificationUtils;
import com.rhino.ble.demo.utils.SharedPreferencesUtils;
import com.rhino.log.LogUtils;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author rhino
 * @since Create on 2021/1/9.
 **/
public class BluetoothService extends Service implements BLECallback {

    /**
     * 搜索成功，默认连接蓝牙mac地址
     */
    private static String autoConnectBluetoothMac;
    /**
     * 设备编号
     */
    private static String deviceNo;
    /**
     * 是否正在搜索
     */
    private static boolean isSearching;

    public static final int NOTICE_ID = 100;
    /**
     * 蓝牙集合
     */
    private static List<BluetoothDevice> bluetoothDeviceList = new ArrayList<>();

    /**
     * 开启服务
     */
    public static void startService(Context context, boolean startSearch) {
        Intent intent = new Intent(context, BluetoothService.class);
        intent.putExtra("startSearch", startSearch);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    /**
     * 停止服务
     */
    public static void stopService(Context context) {
        Intent intent = new Intent(context, BluetoothService.class);
        context.stopService(intent);
    }

    /**
     * 获取正在连接蓝牙
     */
    public static BluetoothDevice getBluetoothDeviceConnecting() {
        return BLEUtils.getInstance().getBluetoothDeviceConnected();
    }

    /**
     * 当前已连接的蓝牙
     */
    public static BluetoothDevice getBluetoothDeviceConnected() {
        return BLEUtils.getInstance().getBluetoothDeviceConnected();
    }

    /**
     * 获取设备编号
     */
    public static String getDeviceNo() {
        return deviceNo;
    }

    /**
     * 设置设备编号
     */
    public static void setDeviceNo(String deviceNo) {
        BluetoothService.deviceNo = deviceNo;
    }

    /**
     * 是否正在搜索
     */
    public static boolean isSearching() {
        return isSearching;
    }

    /**
     * 设置正在搜索
     */
    public static void setSearching(boolean isSearching) {
        BluetoothService.isSearching = isSearching;
    }

    /**
     * 获取已经配对的设备
     */
    public static List<BluetoothDevice> getBondedDevices() {
        return BLEUtils.getInstance().getBondedDevices();
    }

    /**
     * 是否已绑定
     */
    public static boolean isBonded(BluetoothDevice bluetoothDevice) {
        List<BluetoothDevice> bondedDevices = BLEUtils.getInstance().getBondedDevices();
        for (BluetoothDevice bondedDevice : bondedDevices) {
            if (Objects.equals(bondedDevice, bluetoothDevice)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 是否连接中
     */
    public static boolean isConnecting(BluetoothDevice bluetoothDevice) {
        return Objects.equals(getBluetoothDeviceConnecting(), bluetoothDevice);
    }

    /**
     * 是否已连接
     */
    public static boolean isConnected(BluetoothDevice bluetoothDevice) {
        return Objects.equals(getBluetoothDeviceConnected(), bluetoothDevice);
    }

    /**
     * 获取已经扫描到的设备
     */
    public static List<BluetoothDevice> getSearchedDevices() {
        return bluetoothDeviceList;
    }

    /**
     * 与设备配对
     */
    public static void createBond(BluetoothDevice device) {
        BLEUtils.getInstance().createBond(device);
    }

    /**
     * 设置蓝牙可被发现
     */
    public static void setDiscoverable(Context context, int second) {
        BLEUtils.getInstance().setDiscoverable(context, second);
    }

    /**
     * 打开蓝牙
     */
    public static void open() {
        BLEUtils.getInstance().open();
    }

    /**
     * 关闭蓝牙
     */
    public static void close() {
        BLEUtils.getInstance().close();
    }

    /**
     * 打开蓝牙开始搜索
     */
    public static void openAndStartSearch() {
        if (isBluetoothOpened()) {
            startSearch();
        } else {
            open();
        }
    }

    /**
     * 开始搜索
     */
    public static void startSearch() {
        if (isSearching()) {
            return;
        }
        if (!isBluetoothOpened()) {
            return;
        }
        setSearching(true);
        BLEUtils.getInstance().startSearch();
    }

    /**
     * 停止搜索
     */
    public static void stopSearch() {
        setSearching(false);
        BLEUtils.getInstance().stopSearch();
    }

    /**
     * 蓝牙是否打开
     */
    public static boolean isBluetoothOpened() {
        return BLEUtils.getInstance().isBluetoothOpened();
    }

    /**
     * 服务端-发送数据
     */
    public static void serverWrite(String msg) {
        BLEUtils.getInstance().serverWrite(msg);
    }

    /**
     * 服务端-开启等待连接线程
     */
    public static void startNextAcceptConnectThread() {
        BLEUtils.getInstance().startNextAcceptConnectThread();
    }

    /**
     * 服务端-开启等待连接线程
     */
    public static void serverStartAcceptConnectThread() {
        BLEUtils.getInstance().serverStartAcceptConnectThread();
    }

    /**
     * 客户端-断开连接
     */
    public static void disconnect() {
        BLEUtils.getInstance().clientDisconnect();
    }

    /**
     * 客户端-连接
     */
    public static void clientConnect(BluetoothDevice bluetoothDevice) {
        LogUtils.d("开始连接：" + bluetoothDevice.getName() + ", " + bluetoothDevice.getAddress());
        clearConnectedData();
        BLEUtils.getInstance().clientConnect(bluetoothDevice);
    }

    /**
     * 客户端-发送数据
     */
    public static void clientWrite(BluetoothDevice bluetoothDevice, String msg) {
        BLEUtils.getInstance().clientWrite(bluetoothDevice, msg);
    }

    /**
     * 客户端-发送数据
     */
    public static void clientWrite(String msg) {
        BLEUtils.getInstance().clientWrite(getBluetoothDeviceConnected(), msg);
    }

    /**
     * 获取当前蓝牙名称
     */
    public static String getName() {
        return BLEUtils.getName();
    }

    /**
     * 设置蓝牙名称
     */
    public static void setName(String name) {
        BLEUtils.setName(name);
    }

    /**
     * 获取当前蓝牙地址
     */
    public static String getAddress() {
        return BLEUtils.getAddress();
    }

    /**
     * 清除当前连接数据
     */
    public static void clearConnectedData() {
        LogUtils.d("清除连接信息");
        deviceNo = null;
        isSearching = false;
    }

    /**
     * 保存连接成功的蓝牙
     */
    public static void saveAutoConnectBluetoothMac(String mac) {
        autoConnectBluetoothMac = mac;
        SharedPreferencesUtils.getInstance().putString("mac", mac);
    }

    /**
     * 获取连接成功的蓝牙
     */
    public static String getAutoConnectBluetoothMac() {
        if (TextUtils.isEmpty(autoConnectBluetoothMac)) {
            autoConnectBluetoothMac = SharedPreferencesUtils.getInstance().getString("mac");
        }
        return autoConnectBluetoothMac;
    }

    /**
     * 在activity.onCreate()中调用
     */
    private void init(Context context, BLECallback callBack) {
        // 传统蓝牙之间通信的uuid
        BLEUtils.BLE_CLASSIC_UUID = "00001101-0000-1000-8000-00805F9B34FB";
        // 低功耗蓝牙的特征值uuid，发送
        BLEUtils.BLE_lE_UUID_SERVICE_EIGENVALUE_SEND = "0000ffe1-0000-1000-8000-00805f9b34fb";
        // 低功耗蓝牙的特征值uuid，接收
        BLEUtils.BLE_LE_UUID_SERVICE_EIGENVALUE_READ = "00002902-0000-1000-8000-00805f9b34fb";
        BLEUtils.getInstance().onCreate(context, callBack);
    }

    /**
     * 在activity.onDestroy()中调用，回收资源
     */
    public void releaseAll() {
        BLEUtils.getInstance().onDestroy();
    }


    /**
     * 当服务被创建时调用.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        LogUtils.d("onCreate()");
        init(this, this);
        Notification.Builder builder = NotificationUtils.builder(this,
                MainActivity.class,
                "channel_id",
                "channelName",
                R.mipmap.ic_rhino_launcher,
                R.mipmap.ic_rhino_launcher,
                getString(R.string.app_name) + "正在运行",
                "点击查看详情");
        Notification notification = builder.build(); // 获取构建好的Notification
        notification.defaults = Notification.DEFAULT_SOUND; // 设置为默认的声音
        startForeground(NOTICE_ID, notification);
    }

    /**
     * 服务不再有用且将要被销毁时调用
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        LogUtils.d("onDestroy()");
        clearConnectedData();
        releaseAll();
    }

    /**
     * 调用startService()启动服务时回调
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtils.d("onStartCommand()");
        boolean startSearch = intent != null && intent.getBooleanExtra("startSearch", false);
        if (isBluetoothOpened()) {
            //蓝牙已开启，自动开启搜索
            if (startSearch) {
                setDiscoverable(this, 60);
                startSearch();
            }
            startNextAcceptConnectThread();
        } else {
            if (startSearch) {
                open();
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onBLEEvent(BLEEvent event, Object obj) {
        LogUtils.d("event = " + event + ", obj = " + obj);
        switch (event) {
            case BLE_OPEN:
                //蓝牙已开启，自动开启搜索
                setDiscoverable(this, 60);
                startSearch();
                startNextAcceptConnectThread();
                break;
            case BLE_CLOSE:
                //蓝牙已关闭
                bluetoothDeviceList.clear();
                clearConnectedData();
                break;
            case ACCEPT_CONNECTING:
                //正在等待客户端连接
                break;
            case ACCEPT_CONNECT_SUCCESS:
                //客户端已连接
                break;
            case ACCEPT_CONNECT_FAILED:
                //等待连接失败
                break;
            case CONNECTING:
                //正在连接服务器
                break;
            case CONNECTED:
                //已经连接过服务器，发送数据中
            case CONNECT_SUCCESS:
                //已连接服务器
                if (getBluetoothDeviceConnected() != null) {
                    saveAutoConnectBluetoothMac(getBluetoothDeviceConnected().getAddress());
                }
                break;
            case CONNECT_FAILED:
                //连接失败
                clearConnectedData();
                break;
            case DISCONNECTED:
                //断开连接
                clearConnectedData();
                break;
            case READ_SUCCESS:
                //收到消息
                break;
            case READ_FAILED:
                //读数据失败
                clearConnectedData();
                break;
            case WRITE_SUCCESS:
                //写数据成功
                break;
            case WRITE_FAILED:
                //写数据失败
                clearConnectedData();
                break;
            case SEARCH_START:
                //开始搜索
                setSearching(true);
                break;
            case SEARCH_FOUND_DEVICE:
                //搜索到设备
                SearchResult searchResult = (SearchResult) obj;
                if (!bluetoothDeviceList.contains(searchResult.device)) {
                    //搜索到设备
                    LogUtils.d("搜索到蓝牙：" + searchResult.device.getName() + ", mac：" + searchResult.device.getAddress());
                    bluetoothDeviceList.add(searchResult.device);
                }
                checkAutoConnect();
                break;
            case SEARCH_CANCEL:
                //取消搜索
                setSearching(false);
                break;
            case SEARCH_STOP:
                //搜索完成
                setSearching(false);
                break;
            default:
                break;
        }
        EventBus.getDefault().post(BluetoothEvent.create(event, obj));
    }

    /**
     * 检查自动连接
     */
    public static boolean checkAutoConnect() {
        if (!isBluetoothOpened()) {
            return false;
        }
        for (BluetoothDevice bluetoothDevice : bluetoothDeviceList) {
            if (TextUtils.equals(getAutoConnectBluetoothMac(), bluetoothDevice.getAddress())) {
                LogUtils.d("开始自动连接：" + bluetoothDevice.getName() + "， mac：" + bluetoothDevice.getAddress());
                //createBond(searchResult.searchResult);
                clientConnect(bluetoothDevice);
                //clientWrite(searchResult.searchResult, "020202");
                return true;
            }
        }
        return false;
    }

}
