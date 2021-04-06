package com.rhino.ble;

/**
 * @author rhino
 * @since Create on 2020/08/29.
 **/
public enum BLEEvent {

    /**
     * 蓝牙开启
     */
    BLE_OPEN,
    /**
     * 蓝牙关闭
     */
    BLE_CLOSE,
    /**
     * 服务端等待连接
     */
    ACCEPT_CONNECTING,
    /**
     * 服务端等待成功
     */
    ACCEPT_CONNECT_SUCCESS,
    /**
     * 服务端等待失败
     */
    ACCEPT_CONNECT_FAILED,

    /**
     * 客户端正在连接主机
     */
    CONNECTING,
    /**
     * 客户端连接主机成功
     */
    CONNECT_SUCCESS,
    /**
     * 客户端已连接主机
     */
    CONNECTED,
    /**
     * 客户端连接主机连接失败
     */
    CONNECT_FAILED,
    /**
     * 断开链接
     */
    DISCONNECTED,

    /**
     * 读取成功
     */
    READ_SUCCESS,
    /**
     * 读取失败
     */
    READ_FAILED,

    /**
     * 发送成功
     */
    WRITE_SUCCESS,
    /**
     * 发送失败
     */
    WRITE_FAILED,


    /**
     * 开始搜索
     */
    SEARCH_START,
    /**
     * 搜
     */
    SEARCH_FOUND_DEVICE,
    /**
     * 取消搜索
     */
    SEARCH_CANCEL,
    /**
     * 停止搜索
     */
    SEARCH_STOP,

    /**
     * 未知
     */
    UNKNOWN;

}
