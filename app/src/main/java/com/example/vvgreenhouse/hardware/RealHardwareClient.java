package com.example.vvgreenhouse.hardware;

import com.example.vvgreenhouse.model.SensorData;

/**
 * 真实硬件客户端 —— 预留给后续WiFi Socket通信实现
 *
 * TODO: 后续通过 WiFi Socket 与真实硬件通信
 * protocol: TCP, 端口 8899, 数据格式 JSON
 */
public class RealHardwareClient implements IHardwareClient {

    @Override
    public boolean connect(String ip, int port) {
        // TODO: 实现Socket连接
        return false;
    }

    @Override
    public void disconnect() {
        // TODO: 关闭Socket
    }

    @Override
    public SensorData readSensors(int greenhouseId) {
        // TODO: 发送读取指令，接收并解析JSON
        return new SensorData();
    }

    @Override
    public boolean controlDevice(int greenhouseId, String deviceType, String action) {
        // TODO: 发送控制指令
        return false;
    }
}
