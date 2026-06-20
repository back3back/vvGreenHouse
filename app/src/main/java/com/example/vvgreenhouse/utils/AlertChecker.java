package com.example.vvgreenhouse.utils;

import com.example.vvgreenhouse.model.AlertRecord;
import com.example.vvgreenhouse.model.SensorData;
import com.example.vvgreenhouse.model.ThresholdConfig;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 预警检测工具类
 *
 * 根据传感器数据和阈值配置，检测所有超标指标，
 * 按偏离程度自动计算预警等级。
 *
 * 等级判定规则（偏离程度 = |value - nearestBound| / nearestBound）：
 *   1 = 轻度 — 偏离 < 10%
 *   2 = 中度 — 偏离 10% ~ 30%
 *   3 = 重度 — 偏离 > 30%
 */
public class AlertChecker {

    /**
     * 检查所有传感器指标，返回超标预警列表。
     *
     * @param data    传感器数据
     * @param config  阈值配置
     * @return 超标项目列表（空列表表示全部正常）
     */
    public static List<AlertRecord> check(SensorData data, ThresholdConfig config) {
        List<AlertRecord> alerts = new ArrayList<>();
        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date());

        for (String type : SensorData.SENSOR_TYPES) {
            float value = data.getValueByName(type);
            float[] range = config.getRange(type);
            if (range == null) continue;

            float min = range[0];
            float max = range[1];

            // 判断是否超标
            if (value >= min && value <= max) continue;

            // 计算偏离程度
            float deviation;
            if (value > max) {
                deviation = (value - max) / max;
            } else {
                // value < min — 防止 min=0 除零
                deviation = (min - value) / Math.max(min, 0.001f);
            }

            int level = calcLevel(deviation);

            AlertRecord alert = new AlertRecord();
            alert.setGreenhouseId(data.getGreenhouseId());
            alert.setSensorType(type);
            alert.setSensorName(SensorData.getChineseName(type));
            alert.setValue(value);
            alert.setThresholdMin(min);
            alert.setThresholdMax(max);
            alert.setLevel(level);
            alert.setStatus(0);  // 未处理
            alert.setRecordTime(now);
            alerts.add(alert);
        }
        return alerts;
    }

    /**
     * 根据偏离程度计算预警等级
     */
    public static int calcLevel(float deviation) {
        if (deviation <= 0.10f) return 1;   // 轻度
        if (deviation <= 0.30f) return 2;   // 中度
        return 3;                            // 重度
    }
}
