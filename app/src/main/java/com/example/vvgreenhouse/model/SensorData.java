package com.example.vvgreenhouse.model;

/**
 * 传感器数据实体类
 * 对应 sensor_data 数据表，存储一次采集的所有环境参数
 */
public class SensorData {
    private int id;
    private int greenhouseId;       // 大棚ID
    private float temp;             // 空气温度 (°C)
    private float humidity;         // 空气湿度 (%)
    private float co2;              // CO₂浓度 (ppm)
    private float light;            // 光照强度 (lux)
    private float soilTemp;         // 土壤温度 (°C)
    private float soilHumidity;     // 土壤湿度 (%)
    private float ph;               // 土壤pH值
    private float ec;               // 土壤电导率 (mS/cm)
    private String recordTime;      // 采集时间

    public SensorData() {}

    // ========== Getters & Setters ==========
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getGreenhouseId() { return greenhouseId; }
    public void setGreenhouseId(int greenhouseId) { this.greenhouseId = greenhouseId; }

    public float getTemp() { return temp; }
    public void setTemp(float temp) { this.temp = temp; }

    public float getHumidity() { return humidity; }
    public void setHumidity(float humidity) { this.humidity = humidity; }

    public float getCo2() { return co2; }
    public void setCo2(float co2) { this.co2 = co2; }

    public float getLight() { return light; }
    public void setLight(float light) { this.light = light; }

    public float getSoilTemp() { return soilTemp; }
    public void setSoilTemp(float soilTemp) { this.soilTemp = soilTemp; }

    public float getSoilHumidity() { return soilHumidity; }
    public void setSoilHumidity(float soilHumidity) { this.soilHumidity = soilHumidity; }

    public float getPh() { return ph; }
    public void setPh(float ph) { this.ph = ph; }

    public float getEc() { return ec; }
    public void setEc(float ec) { this.ec = ec; }

    public String getRecordTime() { return recordTime; }
    public void setRecordTime(String recordTime) { this.recordTime = recordTime; }

    /**
     * 格式化为完整展示字符串
     */
    public String toDisplayString() {
        return         "============================\n"
                + "  大棚ID: " + greenhouseId + "\n"
                + "  采集时间: " + recordTime + "\n"
                + "----------------------------\n"
                + "  空气温度: " + String.format("%.1f", temp) + " °C\n"
                + "  空气湿度: " + String.format("%.1f", humidity) + " %\n"
                + "  CO₂浓度: " + String.format("%.1f", co2) + " ppm\n"
                + "  光照强度: " + String.format("%.0f", light) + " lux\n"
                + "----------------------------\n"
                + "  土壤温度: " + String.format("%.1f", soilTemp) + " °C\n"
                + "  土壤湿度: " + String.format("%.1f", soilHumidity) + " %\n"
                + "  土壤pH值: " + String.format("%.2f", ph) + "\n"
                + "  土壤EC值: " + String.format("%.2f", ec) + " mS/cm\n"
                + "============================\n";
    }

    /**
     * 简洁展示（仅空气数据）
     */
    public String toSimpleString() {
        return "GH-" + greenhouseId
                + "  温度: " + String.format("%.1f", temp) + "°C"
                + "  湿度: " + String.format("%.1f", humidity) + "%"
                + "  CO₂: " + String.format("%.0f", co2) + "ppm";
    }
}
