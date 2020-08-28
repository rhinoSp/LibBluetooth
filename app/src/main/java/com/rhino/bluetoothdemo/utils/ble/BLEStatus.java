package com.rhino.bluetoothdemo.utils.ble;

/**
 * @author rhino
 * @since Create on 2020/08/29.
 **/
public class BLEStatus {

    /**
     * 蓝牙开启
     */
    public static final int BLE_OPEN = -2;
    /**
     * 蓝牙关闭
     */
    public static final int BLE_CLOSE = -1;
    /**
     * 服务端等待连接
     */
    public static final int ACCEPT_CONNECTING = 1;
    /**
     * 服务端等待成功
     */
    public static final int ACCEPT_CONNECT_SUCCESS = 2;
    /**
     * 服务端等待失败
     */
    public static final int ACCEPT_CONNECT_FAILED = 3;

    /**
     * 客户端正在连接主机
     */
    public static final int CONNECTING = 4;
    /**
     * 客户端连接主机成功
     */
    public static final int CONNECT_SUCCESS = 5;
    /**
     * 客户端已连接主机
     */
    public static final int CONNECTED= 6;
    /**
     * 客户端连接主机连接失败
     */
    public static final int CONNECT_FAILED = 7;

    /**
     * 读取成功
     */
    public static final int READ_SUCCESS = 8;
    /**
     * 读取失败
     */
    public static final int READ_FAILED = 9;

    /**
     * 发送成功
     */
    public static final int WRITE_SUCCESS = 10;
    /**
     * 发送失败
     */
    public static final int WRITE_FAILED = 11;


    /**
     * 开始搜索
     */
    public static final int SEARCH_START = 12;
    /**
     * 搜
     */
    public static final int SEARCH_FOUND_DEVICE = 13;
    /**
     * 取消搜索
     */
    public static final int SEARCH_CANCEL = 14;
    /**
     * 停止搜索
     */
    public static final int SEARCH_STOP = 15;


}
