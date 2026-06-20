package com.example.vvgreenhouse.hardware;

import com.example.vvgreenhouse.model.SensorData;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

/**
 * 模拟硬件客户端 —— 开发阶段使用
 *
 * 特性：
 * - 在合理范围内生成伪随机传感器数据
 * - 支持"异常模式"：模拟温度超标、湿度过低等告警场景
 * - 模拟数据格式与真实硬件一致
 */
public class MockHardwareClient implements IHardwareClient {

    private final Random random = new Random();
    private boolean connected = false;

    /** 异常模式开关：开启后数据将超出正常阈值 */
    private boolean abnormalMode = false;

    /** 基准值（作为波动中心，而不是全随机） */
    private float baseTemp = 25.0f;
    private float baseHumidity = 65.0f;
    private float baseCo2 = 450.0f;
    private float baseLight = 15000.0f;
    private float baseSoilTemp = 22.0f;
    private float baseSoilHumidity = 55.0f;
    private float basePh = 6.5f;
    private float baseEc = 1.4f;

    // ========== 异常模式基准值 ==========
    private static final float ABNORMAL_TEMP = 35.0f;
    private static final float ABNORMAL_HUMIDITY = 30.0f;
    private static final float ABNORMAL_CO2 = 1200.0f;
    private static final float ABNORMAL_SOIL_HUMIDITY = 25.0f;
    private static final float ABNORMAL_PH = 8.5f;
    private static final float ABNORMAL_EC = 3.5f;

    @Override
    public boolean connect(String ip, int port) {
        connected = true;
        return true; // 模拟连接成功
    }

    @Override
    public void disconnect() {
        connected = false;
    }

    /** 是否处于异常模式 */
    public boolean isAbnormalMode() {
        return abnormalMode;
    }

    /** 切换异常模式 */
    public void setAbnormalMode(boolean abnormalMode) {
        this.abnormalMode = abnormalMode;
    }

    /**
     * 读取传感器数据（模拟）
     * 正常模式：数据在合理范围内围绕基准值波动
     * 异常模式：数据趋向告警阈值
     */
    @Override
    public SensorData readSensors(int greenhouseId) {
        SensorData data = new SensorData();
        data.setGreenhouseId(greenhouseId);

        if (abnormalMode) {
            // 异常模式：数据趋向危险区间
            data.setTemp(ABNORMAL_TEMP + random.nextFloat() * 3 - 1.5f);       // 33.5~36.5
            data.setHumidity(ABNORMAL_HUMIDITY + random.nextFloat() * 10);     // 30~40
            data.setCo2(ABNORMAL_CO2 + random.nextFloat() * 200);               // 1200~1400
            data.setLight(50000 + random.nextFloat() * 10000);                  // 50000~60000
            data.setSoilTemp(35.0f + random.nextFloat() * 3);                    // 35~38
            data.setSoilHumidity(ABNORMAL_SOIL_HUMIDITY + random.nextFloat() * 10); // 25~35
            data.setPh(ABNORMAL_PH + random.nextFloat() * 0.8f - 0.4f);         // 8.1~8.9
            data.setEc(ABNORMAL_EC + random.nextFloat() * 0.5f);                 // 3.5~4.0
        } else {
            // 正常模式：基准值附近小范围波动（±波动幅度）
            data.setTemp(baseTemp + random.nextFloat() * 4 - 2);          // 23~27
            data.setHumidity(baseHumidity + random.nextFloat() * 10 - 5); // 60~70
            data.setCo2(baseCo2 + random.nextFloat() * 100 - 50);         // 400~500
            data.setLight(baseLight + random.nextFloat() * 10000 - 5000); // 10000~20000
            data.setSoilTemp(baseSoilTemp + random.nextFloat() * 3 - 1.5f);        // 20.5~23.5
            data.setSoilHumidity(baseSoilHumidity + random.nextFloat() * 10 - 5);   // 50~60
            data.setPh(basePh + random.nextFloat() * 0.6f - 0.3f);                  // 6.2~6.8
            data.setEc(baseEc + random.nextFloat() * 0.4f - 0.2f);                  // 1.2~1.6
        }

        // 采集时间
        data.setRecordTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
        return data;
    }

    @Override
    public boolean controlDevice(int greenhouseId, String deviceType, String action) {
        // 模拟延时（真实设备响应时间）
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        return true; // 模拟控制成功
    }
}
