package com.example.vvgreenhouse.model;

/**
 * 管理日志实体 — 统一承载 农事/水肥/运维 三类日志
 * 对应 farming_logs / water_fertilizer_logs / maintenance_logs 三张表
 *
 * category 字段在代码中区分："farming" / "water_fert" / "maintenance"
 */
public class ManagementLog {
    private int id;
    private int greenhouseId;      // 大棚ID
    private String category;       // 日志分类（不存DB，用于代码路由）
    private String logType;        // 日志子类型
    private String operator;       // 操作人
    private String operateTime;    // 操作时间
    private String content;        // 详细内容
    private String remarks;        // 备注
    private String createTime;     // 创建时间

    // === 水肥专用字段 (water_fertilizer_logs) ===
    private String fertilizerName; // 肥料/药剂名称
    private String fertilizerAmount; // 用量
    private String irrigationAmount; // 灌溉量
    private int durationMin;       // 作业时长(分钟)

    // === 运维专用字段 (maintenance_logs) ===
    private String deviceName;     // 设备名称
    private String maintenanceType; // 维护类型：保养/维修/更换
    private String cost;           // 费用

    public ManagementLog() {}

    // ====== 通用字段 ======
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getGreenhouseId() { return greenhouseId; }
    public void setGreenhouseId(int greenhouseId) { this.greenhouseId = greenhouseId; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getLogType() { return logType; }
    public void setLogType(String logType) { this.logType = logType; }

    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }

    public String getOperateTime() { return operateTime; }
    public void setOperateTime(String operateTime) { this.operateTime = operateTime; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }

    // ====== 水肥专用 ======
    public String getFertilizerName() { return fertilizerName; }
    public void setFertilizerName(String fertilizerName) { this.fertilizerName = fertilizerName; }

    public String getFertilizerAmount() { return fertilizerAmount; }
    public void setFertilizerAmount(String fertilizerAmount) { this.fertilizerAmount = fertilizerAmount; }

    public String getIrrigationAmount() { return irrigationAmount; }
    public void setIrrigationAmount(String irrigationAmount) { this.irrigationAmount = irrigationAmount; }

    public int getDurationMin() { return durationMin; }
    public void setDurationMin(int durationMin) { this.durationMin = durationMin; }

    // ====== 运维专用 ======
    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }

    public String getMaintenanceType() { return maintenanceType; }
    public void setMaintenanceType(String maintenanceType) { this.maintenanceType = maintenanceType; }

    public String getCost() { return cost; }
    public void setCost(String cost) { this.cost = cost; }
}
