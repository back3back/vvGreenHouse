package com.example.vvgreenhouse.model;

/**
 * 设备定义 —— 描述一个大棚中可控设备的元数据
 */
public class DeviceDef {

    /** 设备类型 key */
    public String type;
    /** 中文名 */
    public String name;
    /** 所属系统: 温控/光控/气调/湿控 */
    public String system;
    /** emoji 图标 */
    public String icon;

    private DeviceDef(String type, String name, String system, String icon) {
        this.type = type;
        this.name = name;
        this.system = system;
        this.icon = icon;
    }

    // ========== 全部设备列表 ==========

    public static final DeviceDef[] ALL_DEVICES = {
            new DeviceDef("ventilation_window",   "通风窗",   "温控系统", "🪟"),
            new DeviceDef("wet_curtain_fan",     "湿帘风机",  "温控系统", "🌀"),
            new DeviceDef("heating_device",      "热风供暖",  "温控系统", "🔥"),
            new DeviceDef("circulation_fan_temp","环流风机",  "温控系统", "🌬"),
            new DeviceDef("fill_light",           "补光灯",   "光控系统", "💡"),
            new DeviceDef("outer_shade",          "外遮阳网",  "光控系统", "🏖"),
            new DeviceDef("inner_shade",          "内遮阳幕",  "光控系统", "🪟"),
            new DeviceDef("co2_generator",        "CO₂补气",  "气调系统", "🫧"),
            new DeviceDef("circulation_fan_co2",  "环流风机",  "气调系统", "💨"),
            new DeviceDef("high_pressure_spray",  "高压喷雾",  "湿控系统", "💦"),
            new DeviceDef("dehumidifier",         "除湿设备",  "湿控系统", "💨"),
    };

    /** 按系统分组：温控 */
    public static final String[] TEMP_DEVICES = {
            "ventilation_window", "wet_curtain_fan", "heating_device", "circulation_fan_temp"
    };
    /** 光控 */
    public static final String[] LIGHT_DEVICES = {
            "fill_light", "outer_shade", "inner_shade"
    };
    /** 气调 */
    public static final String[] CO2_DEVICES = {
            "co2_generator", "circulation_fan_co2"
    };
    /** 湿控 */
    public static final String[] HUMIDITY_DEVICES = {
            "high_pressure_spray", "dehumidifier"
    };

    /** 按类型查找 */
    public static DeviceDef findByType(String type) {
        for (DeviceDef d : ALL_DEVICES) {
            if (d.type.equals(type)) return d;
        }
        return new DeviceDef(type, type, "", "🔧");
    }
}
