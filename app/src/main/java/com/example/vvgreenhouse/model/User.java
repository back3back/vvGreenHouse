package com.example.vvgreenhouse.model;

/**
 * 用户实体类
 */
public class User {
    private int id;
    private String username;
    private String password;      // MD5存储
    private String realName;
    private String role;          // boss / admin / gardener
    private String phone;
    private int status;           // 1=正常 0=禁用
    private String createTime;
    private String updateTime;

    public User() {}

    public User(int id, String username, String realName, String role) {
        this.id = id;
        this.username = username;
        this.realName = realName;
        this.role = role;
    }

    // ========== Getters & Setters ==========
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }

    public String getUpdateTime() { return updateTime; }
    public void setUpdateTime(String updateTime) { this.updateTime = updateTime; }

    /** 获取角色中文名 */
    public String getRoleDisplayName() {
        switch (role) {
            case "boss": return "老板";
            case "admin": return "管理员";
            case "gardener": return "园艺师";
            default: return role;
        }
    }
}
