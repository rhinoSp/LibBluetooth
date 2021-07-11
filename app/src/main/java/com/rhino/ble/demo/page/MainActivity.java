package com.rhino.ble.demo.page;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.inuker.bluetooth.library.search.SearchResult;

import com.rhino.ble.BLEUtils;
import com.rhino.ble.demo.R;
import com.rhino.ble.demo.event.BluetoothEvent;
import com.rhino.ble.demo.service.BluetoothService;
import com.rhino.ble.demo.utils.TimeUtils;
import com.rhino.ble.demo.utils.PermissionsUtils;
import com.rhino.ble.BLEEvent;
import com.rhino.log.LogUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

/**
 * @author LuoLin
 * @since Create on 2020/08/29.
 **/
public class MainActivity extends AppCompatActivity {

    public static final int REQUEST_CODE_PERMISSIONS = 1;
    public static final String[] PERMISSIONS = {
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH
    };

    /**
     * 蓝牙集合
     */
    private List<BluetoothDevice> bluetoothDeviceList = new ArrayList<>();

    /**
     * 蓝牙列表
     */
    private List<String> bluetoothNameList = new ArrayList<>();
    private ArrayAdapter<String> adapterBluetoothList;
    /**
     * 日志列表
     */
    private ListView listViewLog;
    private List<String> logList = new ArrayList<>();
    private ArrayAdapter<String> adapterLogList;
    /**
     * 消息列表
     */
    private TextView textViewMsg;
    private ListView listViewMsg;
    private List<String> msgList = new ArrayList<>();
    private ArrayAdapter<String> adapterMsgList;
    /**
     * 消息编辑框
     */
    private EditText editTextMsg;
    /**
     * 连接的设备
     */
    private BluetoothDevice connectBluetoothDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
        setContentView(R.layout.activity_main);
        if (PermissionsUtils.checkSelfPermission(this, PERMISSIONS)) {
            onGetAllRequest();
        } else {
            PermissionsUtils.requestPermissions(this, PERMISSIONS);
        }
        adapterBluetoothList = new ArrayAdapter<>(this, R.layout.simple_list_item_1, bluetoothNameList);
        ListView listView1 = findViewById(R.id.list_view1);
        listView1.setAdapter(adapterBluetoothList);
        listView1.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                onBleClick(bluetoothDeviceList.get(position));
            }
        });
        adapterLogList = new ArrayAdapter<>(this, R.layout.simple_list_item_1, logList);
        listViewLog = findViewById(R.id.list_view2);
        listViewLog.setAdapter(adapterLogList);

        adapterMsgList = new ArrayAdapter<>(this, R.layout.simple_list_item_1, msgList);
        listViewMsg = findViewById(R.id.list_view3);
        listViewMsg.setAdapter(adapterMsgList);

        textViewMsg = findViewById(R.id.tv_tips3);

        editTextMsg = findViewById(R.id.et_msg);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        BluetoothService.stopService(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (PermissionsUtils.checkHasAllPermission(permissions, grantResults)) {
                onGetAllRequest();
            } else {
                showToast("缺少必要权限，请到权限设置开启本应用需要的权限。");
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(BluetoothEvent bluetoothEvent) {
        LogUtils.d("event = " + bluetoothEvent.event + ", obj = " + bluetoothEvent.obj);
        if (!isFinishing()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    dealBLEEvent(bluetoothEvent.event, bluetoothEvent.obj);
                }
            });
        }
    }

    /**
     * 获得所有权限
     */
    private void onGetAllRequest() {
        BluetoothService.open();
        if (BluetoothService.isBluetoothOpened()) {
            // 如果蓝牙是开着的，直接开始搜索
            BluetoothService.startService(this, true);
        }
        setTitle("您的蓝牙名称：" + BluetoothService.getName());
    }

    /**
     * 开启蓝牙
     */
    public void onOpenBleClick(View view) {
        BluetoothService.open();
    }

    /**
     * 关闭蓝牙
     */
    public void onCloseBleClick(View view) {
        BluetoothService.close();
    }

    /**
     * 发送消息
     */
    public void onSendClick(View view) {
        String msg = editTextMsg.getText().toString();
        if (TextUtils.isEmpty(msg)) {
            return;
        }
        if (connectBluetoothDevice != null) {
            // 不等于null，说明当前是客户端
            BluetoothService.clientWrite(connectBluetoothDevice, msg);
        } else {
            BluetoothService.serverWrite(msg);
        }
        notifyMsgList("[我]" + msg);
    }

    /**
     * 选中某个蓝牙
     */
    public void onBleClick(BluetoothDevice device) {
        if (connectBluetoothDevice != device) {
            msgList.clear();
            adapterMsgList.notifyDataSetChanged();
        }

        String msg = createHelloMsg(device);
        BluetoothService.clientWrite(device, msg);
//
        notifyMsgList("[我]" + msg);

        connectBluetoothDevice = device;
        textViewMsg.setText("消息列表【" + device.getName() + "】");
    }

    /**
     * 处理蓝牙状态改变
     */
    private void dealBLEEvent(BLEEvent event, Object obj) {
        switch (event) {
            case BLE_OPEN:
                notifyLogList("蓝牙已开启");
                notifyBleList();
                break;
            case BLE_CLOSE:
                notifyLogList("蓝牙已关闭");
                bluetoothDeviceList.clear();
                notifyBleList();
                break;
            case ACCEPT_CONNECTING:
                notifyLogList("正在等待客户端连接");
                break;
            case ACCEPT_CONNECT_SUCCESS:
                notifyLogList("客户端已连接");
                notifyBleList();
                break;
            case ACCEPT_CONNECT_FAILED:
                notifyLogList((String) obj);
                break;
            case CONNECTING:
                notifyLogList("正在连接服务器");
                break;
            case CONNECTED:
                notifyLogList("已经连接过服务器");
                break;
            case CONNECT_SUCCESS:
                notifyLogList("已连接服务器");
                notifyBleList();
                break;
            case CONNECT_FAILED:
                notifyLogList((String) obj);
                break;
            case READ_SUCCESS:
                notifyLogList("收到消息：" + obj);
                notifyMsgList("[对方]" + obj);
                if (((String) obj).startsWith("Hello")) {
                    String msg = createHiMsg();
                    BluetoothService.serverWrite(msg);
                    notifyLogList("发送消息" + msg);
                    notifyMsgList("[我]" + msg);
                }
                break;
            case READ_FAILED:
                notifyLogList((String) obj);
                break;
            case WRITE_SUCCESS:
                notifyLogList((String) obj);
                break;
            case WRITE_FAILED:
                notifyLogList((String) obj);
                break;
            case SEARCH_START:
                notifyLogList("开始搜索");
                break;
            case SEARCH_FOUND_DEVICE:
                SearchResult searchResult = (SearchResult) obj;
                if (!bluetoothDeviceList.contains(searchResult.device)) {
                    // 去重
                    notifyLogList("搜索到设备：" + (TextUtils.isEmpty(searchResult.device.getName()) ? "" : searchResult.device.getName()));
                    bluetoothDeviceList.add(searchResult.device);
                    notifyBleList();
                }
                break;
            case SEARCH_CANCEL:
                notifyLogList("取消搜索");
                break;
            case SEARCH_STOP:
                notifyLogList("搜索完成");
                break;
            default:
                break;
        }
    }

    /**
     * 刷新蓝牙列表
     */
    private void notifyBleList() {
        bluetoothNameList.clear();
        if (BluetoothService.isBluetoothOpened()) {
            List<BluetoothDevice> bondedBluetoothDeviceList = BluetoothService.getBondedDevices();
            for (BluetoothDevice device : bondedBluetoothDeviceList) {
                if (!bluetoothDeviceList.contains(device)) {
                    bluetoothDeviceList.add(device);
                }
            }
            for (BluetoothDevice device : bluetoothDeviceList) {
                String status = "可用";
                if (bondedBluetoothDeviceList.contains(device)) {
                    status = "已配对";
                }
                String name = TextUtils.isEmpty(device.getName()) ? "N/A" : device.getName();
                String ble = BLEUtils.isBLE(device) ? "[BLE]" : "";
                bluetoothNameList.add(name + "（" + status + "）" + ble);
            }
        }
        adapterBluetoothList.notifyDataSetChanged();
    }

    /**
     * 刷新日志列表
     */
    private void notifyLogList(String msg) {
        logList.add(currentTime() + "\n" + msg);
        adapterLogList.notifyDataSetChanged();
        listViewLog.setSelection(listViewLog.getBottom());
    }

    /**
     * 刷新消息列表
     */
    private void notifyMsgList(String msg) {
        msgList.add(currentTime() + "\n" + msg);
        adapterMsgList.notifyDataSetChanged();
        listViewMsg.setSelection(listViewMsg.getBottom());
    }

    /**
     * 构建hello消息
     */
    private String createHelloMsg(BluetoothDevice device) {
        return "Hello " + device.getName() + ", I am " + BluetoothService.getName();
    }

    /**
     * 构建hi消息
     */
    private String createHiMsg() {
        return "Hi " + ", I am " + BluetoothService.getName();
    }

    /**
     * 当前时间
     */
    private String currentTime() {
        return TimeUtils.formatTime(TimeUtils.getUtcMilliseconds(), "yyyy-MM-dd HH:mm:ss");
    }

    /**
     * 显示toast消息
     */
    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }


}
