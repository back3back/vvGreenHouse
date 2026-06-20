package com.example.vvgreenhouse.model;

/**
 * 预警记录实体
 * 对应 alert_records 表
 */
public class AlertRecord {
    private int id;
    private int greenhouseId;       // 大棚ID
    private String sensorType;      // 传感器类型: temp/humidity/co2/light/soil_temp/soil_humidity/ph/ec
    private String sensorName;      // 传感器中文名
    private float value;            // 实测值
    private float thresholdMin;     // 阈值下限
    private float thresholdMax;     // 阈值上限
    private int level;              // 预警等级 1=轻度 2=中度 3=重度
    private String levelName;       // 等级中文名
    private int status;             // 0=未处理 1=已处理
    private String recordTime;      // 预警时间
    private String handleTime;      // 处理时间 (nullable)
    private String handler;         // 处理人 (nullable)

    public AlertRecord() {}

    // ========== Getters & Setters ==========
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getGreenhouseId() { return greenhouseId; }
    public void setGreenhouseId(int greenhouseId) { this.greenhouseId = greenhouseId; }

    public String getSensorType() { return sensorType; }
    public void setSensorType(String sensorType) { this.sensorType = sensorType; }

    public String getSensorName() { return sensorName; }
    public void setSensorName(String sensorName) { this.sensorName = sensorName; }

    public float getValue() { return value; }
    public void setValue(float value) { this.value = value; }

    public float getThresholdMin() { return thresholdMin; }
    public void setThresholdMin(float thresholdMin) { this.thresholdMin = thresholdMin; }

    public float getThresholdMax() { return thresholdMax; }
    public void setThresholdMax(float thresholdMax) { this.thresholdMax = thresholdMax; }

    public int getLevel() { return level; }
    public void setLevel(int level) {
        this.level = level;
        this.levelName = levelToName(level);
    }

    public String getLevelName() { return levelName != null ? levelName : levelToName(level); }
    public void setLevelName(String levelName) { this.levelName = levelName; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getRecordTime() { return recordTime; }
    public void setRecordTime(String recordTime) { this.recordTime = recordTime; }

    public String getHandleTime() { return handleTime; }
    public void setHandleTime(String handleTime) { this.handleTime = handleTime; }

    public String getHandler() { return handler; }
    public void setHandler(String handler) { this.handler = handler; }

    // ========== 辅助方法 ==========

    public static String levelToName(int level) {
        switch (level) {
            case 1: return "轻度";
            case 2: return "中度";
            case 3: return "重度";
            default: return "未知";
        }
    }

    public static int levelToColor(int level) {
        switch (level) {
            case 1: return 0xFFF57C00;  // 橙色 warning
            case 2: return 0xFFFF9800;  // 深橙
            case 3: return 0xFFD32F2F;  // 红色 error
            default: return 0xFF757575;
        }
    }

    /** 超标方向：true=偏高(超出上限)，false=偏低(低于下限) */
    public boolean isHigh() {
        return value > thresholdMax;
    }

    public String getDirection() {
        if (value > thresholdMax) return "偏高↑";
        if (value < thresholdMin) return "偏低↓";
        return "异常";
    }
}
