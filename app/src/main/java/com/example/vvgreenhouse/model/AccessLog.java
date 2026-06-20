package com.example.vvgreenhouse.model;

/**
 * 门锁/人体感应记录实体
 * 对应 access_logs 表
 */
public class AccessLog {
    private int id;
    private int greenhouseId;       // 大棚ID
    private String eventType;       // 事件类型: door_open/door_close/pir_motion/pir_clear
    private String eventName;       // 事件中文名
    private String operator;        // 操作人 (手动门锁) 或 "系统" (PIR自动)
    private String recordTime;      // 记录时间
    private String remarks;         // 备注

    public AccessLog() {}

    // ========== Getters & Setters ==========
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getGreenhouseId() { return greenhouseId; }
    public void setGreenhouseId(int greenhouseId) { this.greenhouseId = greenhouseId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }

    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }

    public String getRecordTime() { return recordTime; }
    public void setRecordTime(String recordTime) { this.recordTime = recordTime; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    /** 事件类型 → 中文名 */
    public static String typeToName(String eventType) {
        if (eventType == null) return "未知";
        switch (eventType) {
            case "door_open": return "门锁开启";
            case "door_close": return "门锁关闭";
            case "pir_motion": return "人体感应—有人";
            case "pir_clear": return "人体感应—无人";
            default: return eventType;
        }
    }
}
