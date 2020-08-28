package com.rhino.bluetoothdemo;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.inuker.bluetooth.library.search.SearchResult;
import com.rhino.bluetoothdemo.utils.TimeUtils;
import com.rhino.bluetoothdemo.utils.ble.BLECallback;
import com.rhino.bluetoothdemo.utils.ble.BLEUtils;
import com.rhino.bluetoothdemo.utils.ble.BLEStatus;
import com.rhino.bluetoothdemo.utils.PermissionsUtils;
import com.rhino.log.LogUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author LuoLin
 * @since Create on 2020/08/29.
 **/
public class MainActivity extends AppCompatActivity implements BLECallback {

    public static final int REQUEST_CODE_PERMISSIONS = 1;
    public static final String[] PERMISSIONS = {
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH
    };

    private List<BluetoothDevice> bluetoothDeviceList = new ArrayList<>();
    private ArrayAdapter<String> adapterBluetoothList;
    private List<String> bluetoothNameList = new ArrayList<>();
    private ArrayAdapter<String> adapterLogList;
    private List<String> logList = new ArrayList<>();
    private TextView tvTips;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (PermissionsUtils.checkSelfPermission(this, PERMISSIONS)) {
            onGetAllRequest();
        } else {
            PermissionsUtils.requestPermissions(this, PERMISSIONS);
        }
        adapterBluetoothList = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, bluetoothNameList);
        tvTips = findViewById(R.id.tv_tips);
        ListView listView1 = findViewById(R.id.list_view1);
        listView1.setAdapter(adapterBluetoothList);
        listView1.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                onBleClick(bluetoothDeviceList.get(position));
            }
        });
        adapterLogList = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, logList);
        ListView listView2 = findViewById(R.id.list_view2);
        listView2.setAdapter(adapterLogList);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BLEUtils.getInstance().onDestroy();
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

    @Override
    public void onBLEStatusChanged(int bleStatus, Object obj) {
        LogUtils.d("bleStatus = " + bleStatus + ", obj = " + obj);
        if (!isFinishing()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    dealBLEStatusChanged(bleStatus, obj);
                }
            });
        }
    }

    private void onGetAllRequest() {
        BLEUtils.getInstance().onCreate(this);
        if (BLEUtils.getInstance().isBluetoothOpened()) {
            BLEUtils.getInstance().setDiscoverable(60);
            BLEUtils.getInstance().startSearch();
            BLEUtils.getInstance().serverStartAcceptConnectThread();
        }
        setTitle("您的蓝牙名称:" + BLEUtils.getInstance().getName());
    }

    public void onOpenBleClick(View view) {
        BLEUtils.getInstance().openBluetooth();
    }

    public void onCloseBleClick(View view) {
        BLEUtils.getInstance().closeBluetooth();
    }

    public void onBleClick(BluetoothDevice device) {
        BLEUtils.getInstance().clientWrite(device, "Hello " + device.getName() + ", I am " + BLEUtils.getInstance().getName());
    }

    private void dealBLEStatusChanged(int bleStatus, Object obj) {
        logList.add(0, TimeUtils.formatTime(TimeUtils.getUtcMilliseconds(), "yyyy-MM-dd HH:mm:ss") + "\n[bleStatus:" + bleStatus + ", obj:" + obj + "]");
        adapterLogList.notifyDataSetChanged();
        switch (bleStatus) {
            case BLEStatus.BLE_OPEN:
                showToast("蓝牙已开启！");
                refreshList();
                BLEUtils.getInstance().setDiscoverable(60);
                BLEUtils.getInstance().startSearch();
                BLEUtils.getInstance().serverStartAcceptConnectThread();
                break;
            case BLEStatus.BLE_CLOSE:
                showToast("蓝牙已关闭！");
                bluetoothDeviceList.clear();
                refreshList();
                break;
            case BLEStatus.ACCEPT_CONNECTING:
                showToast("正在等待客户端连接！");
                break;
            case BLEStatus.ACCEPT_CONNECT_SUCCESS:
                showToast("客户端已连接！");
                break;
            case BLEStatus.ACCEPT_CONNECT_FAILED:
                showToast((String) obj);
                break;
            case BLEStatus.CONNECTING:
                showToast("正在连接服务器！");
                break;
            case BLEStatus.CONNECTED:
                showToast("已经连接过,直接发送数据！");
                break;
            case BLEStatus.CONNECT_SUCCESS:
                showToast("已连接服务器！");
                break;
            case BLEStatus.CONNECT_FAILED:
                showToast((String) obj);
                showToast("连接中断！");
                break;
            case BLEStatus.READ_SUCCESS:
                showToast("收到消息：" + obj);
                if (((String) obj).startsWith("Hello")) {
                    BLEUtils.getInstance().serverWrite("Hi " + ", I am " + BLEUtils.getInstance().getName());
                }
                break;
            case BLEStatus.READ_FAILED:
                showToast((String) obj);
                break;
            case BLEStatus.WRITE_SUCCESS:
                break;
            case BLEStatus.WRITE_FAILED:
                showToast((String) obj);
                break;
            case BLEStatus.SEARCH_START:
                showToast("开始搜索！");
                break;
            case BLEStatus.SEARCH_FOUND_DEVICE:
                SearchResult device = (SearchResult) obj;
                if (!TextUtils.isEmpty(device.device.getName()) && !bluetoothDeviceList.contains(device.device)) {
                    bluetoothDeviceList.add(device.device);
                    refreshList();
                }
                break;
            case BLEStatus.SEARCH_CANCEL:
                showToast("取消搜索！");
                break;
            case BLEStatus.SEARCH_STOP:
                showToast("搜索完成！");
        }
    }

    private void refreshList() {
        bluetoothNameList.clear();
        if (BLEUtils.getInstance().isBluetoothOpened()) {
            List<BluetoothDevice> bondedBluetoothDeviceList = BLEUtils.getInstance().getBondedDevices();
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
                bluetoothNameList.add(device.getName() + "（" + status + "）");
            }
        }
        adapterBluetoothList.notifyDataSetChanged();
    }


    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        tvTips.setText(msg);
    }


}
