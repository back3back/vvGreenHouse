package com.example.vvgreenhouse.hardware;

import com.example.vvgreenhouse.model.SensorData;

/**
 * 硬件通信接口
 * 定义与大棚硬件设备交互的标准协议
 */
public interface IHardwareClient {

    /**
     * 连接硬件设备
     * @param ip   设备IP地址
     * @param port 设备端口
     * @return 连接是否成功
     */
    boolean connect(String ip, int port);

    /** 断开连接 */
    void disconnect();

    /**
     * 读取传感器数据
     * @param greenhouseId 大棚ID
     * @return 传感器数据对象
     */
    SensorData readSensors(int greenhouseId);

    /**
     * 控制设备
     * @param greenhouseId 大棚ID
     * @param deviceType   设备类型 (fan/pump/shade/light/heater)
     * @param action       动作 (on/off)
     * @return 控制是否成功
     */
    boolean controlDevice(int greenhouseId, String deviceType, String action);
}
