package com.example.vvgreenhouse.hardware;

import com.example.vvgreenhouse.model.SensorData;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * 模拟硬件客户端 —— 开发阶段使用
 *
 * 特性：
 * - 传感器数据模拟（合理范围波动 + 异常模式）
 * - 设备状态模拟（Map存储开关状态）
 * - 自动控制联动逻辑
 */
public class MockHardwareClient implements IHardwareClient {

    private final Random random = new Random();
    private boolean connected = false;

    // ========== 异常模式 ==========
    private boolean abnormalMode = false;
    private String abnormalType = "";

    // ========== 基准值 ==========
    private float baseTemp = 25.0f;
    private float baseHumidity = 65.0f;
    private float baseCo2 = 450.0f;
    private float baseLight = 15000.0f;
    private float baseSoilTemp = 22.0f;
    private float baseSoilHumidity = 55.0f;
    private float basePh = 6.5f;
    private float baseEc = 1.4f;

    // ========== 设备状态存储 ==========
    // key: "ghId_deviceType"
    private final Map<String, Boolean> deviceStates = new HashMap<>();
    // key: "ghId_controlMode" → "manual" / "auto"
    private final Map<String, String> deviceModes = new HashMap<>();

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

    public void setAbnormalMode(boolean enabled) {
        this.abnormalMode = enabled;
        this.abnormalType = enabled ? "high_temp" : "";
    }

    public void setAbnormalMode(boolean enabled, String type) {
        this.abnormalMode = enabled;
        this.abnormalType = (type != null) ? type : "";
    }

    // ========== 设备状态 API ==========

    /** 获取设备当前状态 */
    public boolean getDeviceState(int greenhouseId, String deviceType) {
        String key = greenhouseId + "_" + deviceType;
        return Boolean.TRUE.equals(deviceStates.get(key));
    }

    /** 获取控制模式 */
    public String getControlMode(int greenhouseId) {
        return deviceModes.getOrDefault(greenhouseId + "_mode", "manual");
    }

    /** 设置控制模式 */
    public void setControlMode(int greenhouseId, String mode) {
        deviceModes.put(greenhouseId + "_mode", mode);
    }

    // ========== 传感器数据 ==========

    @Override
    public SensorData readSensors(int greenhouseId) {
        SensorData data = new SensorData();
        data.setGreenhouseId(greenhouseId);
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
        generateNormalData(data, offset);
        switch (abnormalType) {
            case "high_temp":
                data.setTemp(35.0f + random.nextFloat() * 5);
                data.setSoilTemp(32.0f + random.nextFloat() * 4);
                break;
            case "low_humidity":
                data.setHumidity(20.0f + random.nextFloat() * 15);
                data.setSoilHumidity(20.0f + random.nextFloat() * 15);
                break;
            case "high_co2":
                data.setCo2(1200.0f + random.nextFloat() * 300);
                break;
            default:
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

    // ========== 设备控制 ==========

    @Override
    public boolean controlDevice(int greenhouseId, String deviceType, String action) {
        String key = greenhouseId + "_" + deviceType;

        switch (action) {
            case "open":
                deviceStates.put(key, true);
                break;
            case "close":
                deviceStates.put(key, false);
                break;
            case "toggle":
                boolean cur = getDeviceState(greenhouseId, deviceType);
                deviceStates.put(key, !cur);
                break;
            case "auto":
                deviceModes.put(key, "auto");
                break;
        }

        // 模拟网络延迟
        try { Thread.sleep(300 + random.nextInt(700)); } catch (InterruptedException ignored) {}
        return true;
    }

    /**
     * 自动控制联动逻辑
     * 根据传感器数据自动调节设备状态
     * @return 本次联动操作描述
     */
    public String executeAutoControl(int greenhouseId, SensorData data) {
        StringBuilder log = new StringBuilder();

        // ===== 温控联动 =====
        if (data.getTemp() > 30.0f) {
            controlDevice(greenhouseId, "ventilation_window", "open");
            controlDevice(greenhouseId, "wet_curtain_fan", "open");
            controlDevice(greenhouseId, "circulation_fan_temp", "open");
            log.append("高温→开通风窗+湿帘风机+环流风机; ");
        } else if (data.getTemp() < 18.0f) {
            controlDevice(greenhouseId, "heating_device", "open");
            controlDevice(greenhouseId, "ventilation_window", "close");
            controlDevice(greenhouseId, "wet_curtain_fan", "close");
            log.append("低温→开热风供暖+关通风窗; ");
        } else {
            // 温度正常，关闭强制温控设备
            controlDevice(greenhouseId, "heating_device", "close");
            controlDevice(greenhouseId, "ventilation_window", "close");
            controlDevice(greenhouseId, "wet_curtain_fan", "close");
        }

        // ===== 光控联动 =====
        if (data.getLight() < 5000) {
            controlDevice(greenhouseId, "fill_light", "open");
            controlDevice(greenhouseId, "outer_shade", "close");
            controlDevice(greenhouseId, "inner_shade", "close");
            log.append("弱光→开补光灯+关遮阳; ");
        } else if (data.getLight() > 50000) {
            controlDevice(greenhouseId, "fill_light", "close");
            controlDevice(greenhouseId, "outer_shade", "open");
            controlDevice(greenhouseId, "inner_shade", "open");
            log.append("强光→开外遮阳+内遮阳; ");
        } else {
            controlDevice(greenhouseId, "fill_light", "close");
        }

        // ===== CO₂联动 =====
        if (data.getCo2() < 350) {
            controlDevice(greenhouseId, "co2_generator", "open");
            log.append("CO₂偏低→开补气; ");
        } else if (data.getCo2() > 1000) {
            controlDevice(greenhouseId, "co2_generator", "close");
            controlDevice(greenhouseId, "circulation_fan_co2", "open");
            log.append("CO₂偏高→关补气+开环流; ");
        }

        // ===== 湿度联动 =====
        if (data.getHumidity() < 50) {
            controlDevice(greenhouseId, "high_pressure_spray", "open");
            log.append("低湿→开高压喷雾; ");
        } else if (data.getHumidity() > 80) {
            controlDevice(greenhouseId, "dehumidifier", "open");
            log.append("高湿→开除湿; ");
        }

        if (log.length() == 0) {
            log.append("环境正常，无联动操作");
        }
        return log.toString();
    }
}
