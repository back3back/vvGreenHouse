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
 * - 支持分类"异常模式"：高温 / 低湿 / 高CO₂
 * - 模拟数据格式与真实硬件一致
 */
public class MockHardwareClient implements IHardwareClient {

    private final Random random = new Random();
    private boolean connected = false;

    // ========== 异常模式 ==========
    private boolean abnormalMode = false;
    private String abnormalType = "";  // "high_temp", "low_humidity", "high_co2"

    // ========== 基准值 ==========
    private float baseTemp = 25.0f;
    private float baseHumidity = 65.0f;
    private float baseCo2 = 450.0f;
    private float baseLight = 15000.0f;
    private float baseSoilTemp = 22.0f;
    private float baseSoilHumidity = 55.0f;
    private float basePh = 6.5f;
    private float baseEc = 1.4f;

    @Override
    public boolean connect(String ip, int port) {
        connected = true;
        return true;
    }

    @Override
    public void disconnect() {
        connected = false;
    }

    // ========== 异常模式 API ==========

    public boolean isAbnormalMode() { return abnormalMode; }
    public String getAbnormalType() { return abnormalType; }

    /** 旧版兼容：不带类型的开关 */
    public void setAbnormalMode(boolean enabled) {
        this.abnormalMode = enabled;
        this.abnormalType = enabled ? "high_temp" : "";
    }

    /**
     * 新版：指定异常类型
     * @param enabled 是否启用
     * @param type    异常类型: "high_temp" / "low_humidity" / "high_co2" / "" (全指标异常)
     */
    public void setAbnormalMode(boolean enabled, String type) {
        this.abnormalMode = enabled;
        this.abnormalType = (type != null) ? type : "";
    }

    /**
     * 读取传感器数据（模拟）
     */
    @Override
    public SensorData readSensors(int greenhouseId) {
        SensorData data = new SensorData();
        data.setGreenhouseId(greenhouseId);
        // 加入大棚偏移，使不同大棚数据略有差异
        float ghOffset = greenhouseId * 0.3f;

        if (abnormalMode) {
            generateAbnormalData(data, ghOffset);
        } else {
            generateNormalData(data, ghOffset);
        }

        data.setRecordTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
        return data;
    }

    private void generateNormalData(SensorData data, float offset) {
        data.setTemp(baseTemp + offset + random.nextFloat() * 4 - 2);
        data.setHumidity(baseHumidity + random.nextFloat() * 10 - 5);
        data.setCo2(baseCo2 + offset * 10 + random.nextFloat() * 100 - 50);
        data.setLight(baseLight + random.nextFloat() * 10000 - 5000);
        data.setSoilTemp(baseSoilTemp + offset + random.nextFloat() * 3 - 1.5f);
        data.setSoilHumidity(baseSoilHumidity + random.nextFloat() * 10 - 5);
        data.setPh(basePh + random.nextFloat() * 0.6f - 0.3f);
        data.setEc(baseEc + random.nextFloat() * 0.4f - 0.2f);
    }

    private void generateAbnormalData(SensorData data, float offset) {
        // 先按正常生成
        generateNormalData(data, offset);

        // 然后覆盖指定异常指标
        switch (abnormalType) {
            case "high_temp":
                data.setTemp(35.0f + random.nextFloat() * 5);         // 35~40°C
                data.setSoilTemp(32.0f + random.nextFloat() * 4);     // 32~36°C
                break;
            case "low_humidity":
                data.setHumidity(20.0f + random.nextFloat() * 15);    // 20~35%
                data.setSoilHumidity(20.0f + random.nextFloat() * 15);// 20~35%
                break;
            case "high_co2":
                data.setCo2(1200.0f + random.nextFloat() * 300);      // 1200~1500ppm
                break;
            default:
                // 全指标异常
                data.setTemp(35.0f + random.nextFloat() * 5);
                data.setHumidity(20.0f + random.nextFloat() * 15);
                data.setCo2(1200.0f + random.nextFloat() * 300);
                data.setLight(50000 + random.nextFloat() * 10000);
                data.setSoilTemp(32.0f + random.nextFloat() * 4);
                data.setSoilHumidity(20.0f + random.nextFloat() * 15);
                data.setPh(8.0f + random.nextFloat() * 1.5f);
                data.setEc(3.0f + random.nextFloat() * 1.0f);
                break;
        }
    }

    @Override
    public boolean controlDevice(int greenhouseId, String deviceType, String action) {
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        return true;
    }
}
