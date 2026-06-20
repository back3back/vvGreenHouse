package com.example.vvgreenhouse.model;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 阈值配置实体 —— 读写 SharedPreferences
 */
public class ThresholdConfig {

    // ========== 空气环境 ==========
    public float tempMin = 18.0f, tempMax = 30.0f;
    public float humidityMin = 50.0f, humidityMax = 80.0f;
    public float co2Min = 300.0f, co2Max = 1000.0f;
    public float lightMin = 5000.0f, lightMax = 50000.0f;

    // ========== 土壤环境 ==========
    public float soilTempMin = 15.0f, soilTempMax = 30.0f;
    public float soilHumidityMin = 40.0f, soilHumidityMax = 70.0f;
    public float phMin = 5.5f, phMax = 7.5f;
    public float ecMin = 0.8f, ecMax = 2.5f;

    // ========== SharedPreferences 键名 ==========
    private static final String SP_NAME = "threshold_prefs";
    private static final String KEY_TEMP_MIN = "temp_min";
    private static final String KEY_TEMP_MAX = "temp_max";
    private static final String KEY_HUMIDITY_MIN = "humidity_min";
    private static final String KEY_HUMIDITY_MAX = "humidity_max";
    private static final String KEY_CO2_MIN = "co2_min";
    private static final String KEY_CO2_MAX = "co2_max";
    private static final String KEY_LIGHT_MIN = "light_min";
    private static final String KEY_LIGHT_MAX = "light_max";
    private static final String KEY_STEMP_MIN = "soil_temp_min";
    private static final String KEY_STEMP_MAX = "soil_temp_max";
    private static final String KEY_SHUM_MIN = "soil_humidity_min";
    private static final String KEY_SHUM_MAX = "soil_humidity_max";
    private static final String KEY_PH_MIN = "ph_min";
    private static final String KEY_PH_MAX = "ph_max";
    private static final String KEY_EC_MIN = "ec_min";
    private static final String KEY_EC_MAX = "ec_max";

    /** 创建默认阈值配置 */
    public static ThresholdConfig defaults() {
        return new ThresholdConfig();
    }

    /** 从 SharedPreferences 加载 */
    public static ThresholdConfig load(Context ctx) {
        ThresholdConfig cfg = new ThresholdConfig();
        SharedPreferences sp = ctx.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        cfg.tempMin = sp.getFloat(KEY_TEMP_MIN, cfg.tempMin);
        cfg.tempMax = sp.getFloat(KEY_TEMP_MAX, cfg.tempMax);
        cfg.humidityMin = sp.getFloat(KEY_HUMIDITY_MIN, cfg.humidityMin);
        cfg.humidityMax = sp.getFloat(KEY_HUMIDITY_MAX, cfg.humidityMax);
        cfg.co2Min = sp.getFloat(KEY_CO2_MIN, cfg.co2Min);
        cfg.co2Max = sp.getFloat(KEY_CO2_MAX, cfg.co2Max);
        cfg.lightMin = sp.getFloat(KEY_LIGHT_MIN, cfg.lightMin);
        cfg.lightMax = sp.getFloat(KEY_LIGHT_MAX, cfg.lightMax);
        cfg.soilTempMin = sp.getFloat(KEY_STEMP_MIN, cfg.soilTempMin);
        cfg.soilTempMax = sp.getFloat(KEY_STEMP_MAX, cfg.soilTempMax);
        cfg.soilHumidityMin = sp.getFloat(KEY_SHUM_MIN, cfg.soilHumidityMin);
        cfg.soilHumidityMax = sp.getFloat(KEY_SHUM_MAX, cfg.soilHumidityMax);
        cfg.phMin = sp.getFloat(KEY_PH_MIN, cfg.phMin);
        cfg.phMax = sp.getFloat(KEY_PH_MAX, cfg.phMax);
        cfg.ecMin = sp.getFloat(KEY_EC_MIN, cfg.ecMin);
        cfg.ecMax = sp.getFloat(KEY_EC_MAX, cfg.ecMax);
        return cfg;
    }

    /** 保存到 SharedPreferences */
    public void save(Context ctx) {
        SharedPreferences.Editor ed = ctx.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE).edit();
        ed.putFloat(KEY_TEMP_MIN, tempMin);
        ed.putFloat(KEY_TEMP_MAX, tempMax);
        ed.putFloat(KEY_HUMIDITY_MIN, humidityMin);
        ed.putFloat(KEY_HUMIDITY_MAX, humidityMax);
        ed.putFloat(KEY_CO2_MIN, co2Min);
        ed.putFloat(KEY_CO2_MAX, co2Max);
        ed.putFloat(KEY_LIGHT_MIN, lightMin);
        ed.putFloat(KEY_LIGHT_MAX, lightMax);
        ed.putFloat(KEY_STEMP_MIN, soilTempMin);
        ed.putFloat(KEY_STEMP_MAX, soilTempMax);
        ed.putFloat(KEY_SHUM_MIN, soilHumidityMin);
        ed.putFloat(KEY_SHUM_MAX, soilHumidityMax);
        ed.putFloat(KEY_PH_MIN, phMin);
        ed.putFloat(KEY_PH_MAX, phMax);
        ed.putFloat(KEY_EC_MIN, ecMin);
        ed.putFloat(KEY_EC_MAX, ecMax);
        ed.apply();
    }

    /**
     * 获取指定传感器的阈值范围
     * @return [min, max]，null 表示没有配置
     */
    public float[] getRange(String sensorType) {
        switch (sensorType) {
            case "temp": return new float[]{tempMin, tempMax};
            case "humidity": return new float[]{humidityMin, humidityMax};
            case "co2": return new float[]{co2Min, co2Max};
            case "light": return new float[]{lightMin, lightMax};
            case "soil_temp": return new float[]{soilTempMin, soilTempMax};
            case "soil_humidity": return new float[]{soilHumidityMin, soilHumidityMax};
            case "ph": return new float[]{phMin, phMax};
            case "ec": return new float[]{ecMin, ecMax};
            default: return null;
        }
    }

    /** 检查某个传感器的值是否超标 */
    public boolean isOutOfRange(String sensorType, float value) {
        float[] range = getRange(sensorType);
        if (range == null) return false;
        return value < range[0] || value > range[1];
    }
}
