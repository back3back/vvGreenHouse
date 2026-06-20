package com.example.vvgreenhouse.model;

/**
 * 病虫害防治记录实体
 * 对应 pest_control 表
 */
public class PestControl {
    private int id;
    private int greenhouseId;        // 大棚ID
    private String findDate;         // 发现日期
    private String pestType;         // 病虫害类型
    private String affectedArea;     // 受影响区域
    private String symptoms;         // 症状描述
    private String controlMeasures;  // 防治措施
    private String pesticideName;    // 农药名称
    private String pesticideAmount;  // 农药用量
    private String controlEffect;    // 防治效果
    private String recorder;         // 记录人
    private String photoPath;        // 图片路径
    private String createTime;       // 创建时间

    public PestControl() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getGreenhouseId() { return greenhouseId; }
    public void setGreenhouseId(int greenhouseId) { this.greenhouseId = greenhouseId; }

    public String getFindDate() { return findDate; }
    public void setFindDate(String findDate) { this.findDate = findDate; }

    public String getPestType() { return pestType; }
    public void setPestType(String pestType) { this.pestType = pestType; }

    public String getAffectedArea() { return affectedArea; }
    public void setAffectedArea(String affectedArea) { this.affectedArea = affectedArea; }

    public String getSymptoms() { return symptoms; }
    public void setSymptoms(String symptoms) { this.symptoms = symptoms; }

    public String getControlMeasures() { return controlMeasures; }
    public void setControlMeasures(String controlMeasures) { this.controlMeasures = controlMeasures; }

    public String getPesticideName() { return pesticideName; }
    public void setPesticideName(String pesticideName) { this.pesticideName = pesticideName; }

    public String getPesticideAmount() { return pesticideAmount; }
    public void setPesticideAmount(String pesticideAmount) { this.pesticideAmount = pesticideAmount; }

    public String getControlEffect() { return controlEffect; }
    public void setControlEffect(String controlEffect) { this.controlEffect = controlEffect; }

    public String getRecorder() { return recorder; }
    public void setRecorder(String recorder) { this.recorder = recorder; }

    public String getPhotoPath() { return photoPath; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }

    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }
}
