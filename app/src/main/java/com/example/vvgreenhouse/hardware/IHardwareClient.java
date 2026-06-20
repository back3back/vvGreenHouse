package com.example.vvgreenhouse.hardware;

import com.example.vvgreenhouse.model.SensorData;

/**
 * 硬件通信接口
 * 定义与大棚硬件设备交互的标准协议
 */
public interface IHardwareClient {

    /** 连接硬件设备 */
    boolean connect(String ip, int port);

    /** 断开连接 */
    void disconnect();

    /** 读取传感器数据 */
    SensorData readSensors(int greenhouseId);

    /** 控制单个设备 */
    boolean controlDevice(int greenhouseId, String deviceType, String action);

    /** 获取设备当前状态 */
    boolean getDeviceState(int greenhouseId, String deviceType);

    /** 批量控制设备（一次连接多次指令） */
    void controlDevicesBatch(int greenhouseId, String[] deviceTypes, String action);

    /** 设置异常模拟模式（Mock实现有效，Real实现忽略） */
    void setAbnormalMode(boolean enabled, String type);

    /** 自动控制联动逻辑（Mock实现有效，Real实现返回空字符串） */
    String executeAutoControl(int greenhouseId, SensorData data);
}
