package com.example.vvgreenhouse.model;

/**
 * 产销记录实体
 * 对应 sales_records 表
 */
public class SalesRecord {
    private int id;
    private String batchNo;        // 批次号
    private int greenhouseId;      // 大棚ID
    private String variety;        // 品种
    private String harvestDate;    // 采收日期
    private int quantity;          // 数量（枝/公斤）
    private String qualityGrade;   // 质量等级 A/B/C
    private String saleChannel;    // 销售渠道
    private double unitPrice;      // 单价
    private double totalAmount;    // 总金额
    private String customer;       // 客户
    private String recorder;       // 记录人
    private String createTime;     // 创建时间

    public SalesRecord() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }

    public int getGreenhouseId() { return greenhouseId; }
    public void setGreenhouseId(int greenhouseId) { this.greenhouseId = greenhouseId; }

    public String getVariety() { return variety; }
    public void setVariety(String variety) { this.variety = variety; }

    public String getHarvestDate() { return harvestDate; }
    public void setHarvestDate(String harvestDate) { this.harvestDate = harvestDate; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getQualityGrade() { return qualityGrade; }
    public void setQualityGrade(String qualityGrade) { this.qualityGrade = qualityGrade; }

    public String getSaleChannel() { return saleChannel; }
    public void setSaleChannel(String saleChannel) { this.saleChannel = saleChannel; }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public String getCustomer() { return customer; }
    public void setCustomer(String customer) { this.customer = customer; }

    public String getRecorder() { return recorder; }
    public void setRecorder(String recorder) { this.recorder = recorder; }

    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }
}
