package com.example.vvgreenhouse.model;

/**
 * 人员信息实体
 * 对应 personnel 表
 */
public class Personnel {
    private int id;
    private String employeeNo;     // 工号
    private String name;           // 姓名
    private String gender;         // 性别
    private String position;       // 岗位
    private String phone;          // 电话
    private int greenhouseId;      // 负责大棚 (0=全部)
    private int status;            // 1=在岗 0=离职
    private String createTime;     // 入职时间

    public Personnel() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getEmployeeNo() { return employeeNo; }
    public void setEmployeeNo(String employeeNo) { this.employeeNo = employeeNo; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public int getGreenhouseId() { return greenhouseId; }
    public void setGreenhouseId(int greenhouseId) { this.greenhouseId = greenhouseId; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }

    /** 岗位预设列表 */
    public static final String[] POSITIONS = {
        "园艺师", "技术员", "管理员", "采摘工", "巡检员", "其他"
    };
}
