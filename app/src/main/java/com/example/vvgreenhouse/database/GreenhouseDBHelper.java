package com.example.vvgreenhouse.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.example.vvgreenhouse.model.SensorData;
import com.example.vvgreenhouse.model.AlertRecord;
import com.example.vvgreenhouse.model.AccessLog;
import com.example.vvgreenhouse.model.Personnel;
import com.example.vvgreenhouse.model.ManagementLog;
import com.example.vvgreenhouse.model.PestControl;
import com.example.vvgreenhouse.model.SalesRecord;
import com.example.vvgreenhouse.model.User;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据库管理类 —— 管理全部SQLite表的创建与CRUD操作
 *
 * 初始化时自动创建：
 * - users 表 + 默认管理员账号
 * - greenhouse_info 表 + 8个大棚
 * - sensor_data 表 + 联合索引
 */
public class GreenhouseDBHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "greenhouse.db";
    private static final int DB_VERSION = 4;

    // 单例
    private static GreenhouseDBHelper instance;

    // ========== 表名 ==========
    public static final String TABLE_USERS = "users";
    public static final String TABLE_GREENHOUSE = "greenhouse_info";
    public static final String TABLE_SENSOR = "sensor_data";
    public static final String TABLE_DEVICE_LOGS = "device_logs";
    public static final String TABLE_ALERT_RECORDS = "alert_records";
    public static final String TABLE_ACCESS_LOGS = "access_logs";
    public static final String TABLE_PERSONNEL = "personnel";
    public static final String TABLE_FARMING_LOGS = "farming_logs";
    public static final String TABLE_WATER_FERT_LOGS = "water_fertilizer_logs";
    public static final String TABLE_PEST_CONTROL = "pest_control";
    public static final String TABLE_MAINTENANCE_LOGS = "maintenance_logs";
    public static final String TABLE_SALES_RECORDS = "sales_records";

    private GreenhouseDBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    public static synchronized GreenhouseDBHelper getInstance(Context context) {
        if (instance == null) {
            instance = new GreenhouseDBHelper(context.getApplicationContext());
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // ===== 用户表 =====
        db.execSQL("CREATE TABLE " + TABLE_USERS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "username VARCHAR(50) NOT NULL UNIQUE, "
                + "password VARCHAR(64) NOT NULL, "
                + "real_name VARCHAR(50), "
                + "role VARCHAR(20) NOT NULL DEFAULT 'gardener', "
                + "phone VARCHAR(20), "
                + "status INTEGER NOT NULL DEFAULT 1, "
                + "create_time DATETIME DEFAULT CURRENT_TIMESTAMP, "
                + "update_time DATETIME DEFAULT CURRENT_TIMESTAMP)");

        // ===== 大棚信息表 =====
        db.execSQL("CREATE TABLE " + TABLE_GREENHOUSE + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "gh_no VARCHAR(20) NOT NULL UNIQUE, "
                + "gh_name VARCHAR(100) NOT NULL, "
                + "area FLOAT, "
                + "crop_type VARCHAR(100), "
                + "location VARCHAR(200), "
                + "status INTEGER NOT NULL DEFAULT 1, "
                + "create_time DATETIME DEFAULT CURRENT_TIMESTAMP)");

        // ===== 传感器数据表 =====
        db.execSQL("CREATE TABLE " + TABLE_SENSOR + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "greenhouse_id INTEGER NOT NULL, "
                + "temp FLOAT, humidity FLOAT, co2 FLOAT, light FLOAT, "
                + "soil_temp FLOAT, soil_humidity FLOAT, ph FLOAT, ec FLOAT, "
                + "record_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                + "FOREIGN KEY (greenhouse_id) REFERENCES greenhouse_info(id))");

        // 联合索引：加速按大棚+时间的历史查询
        db.execSQL("CREATE INDEX idx_sensor_gh_time ON "
                + TABLE_SENSOR + "(greenhouse_id, record_time)");

        // ===== 初始化数据 =====

        // 默认管理员账号 (密码: admin123, MD5: 0192023a7bbd73250516f069df18b500)
        db.execSQL("INSERT INTO " + TABLE_USERS
                + " (username, password, real_name, role) "
                + "VALUES ('admin', '0192023a7bbd73250516f069df18b500', '系统管理员', 'admin')");

        // 初始化8个大棚
        for (int i = 1; i <= 8; i++) {
            String ghNo = String.format("GH-%03d", i);
            String ghName = "花卉大棚" + i;
            ContentValues cv = new ContentValues();
            cv.put("gh_no", ghNo);
            cv.put("gh_name", ghName);
            cv.put("area", 500.0);
            cv.put("crop_type", "玫瑰");
            cv.put("location", "A区-" + i + "号");
            db.insert(TABLE_GREENHOUSE, null, cv);
        }

        // ===== 设备操作日志表 =====
        db.execSQL("CREATE TABLE " + TABLE_DEVICE_LOGS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "greenhouse_id INTEGER NOT NULL, "
                + "device_type VARCHAR(50) NOT NULL, "
                + "device_name VARCHAR(100) NOT NULL, "
                + "action VARCHAR(50) NOT NULL, "
                + "mode VARCHAR(20) NOT NULL DEFAULT 'manual', "
                + "operator VARCHAR(50) NOT NULL, "
                + "operate_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                + "result VARCHAR(20) NOT NULL DEFAULT 'success', "
                + "remarks TEXT)");
        db.execSQL("CREATE INDEX idx_devlog_gh_time ON "
                + TABLE_DEVICE_LOGS + "(greenhouse_id, operate_time)");

        // ===== 预警记录表 =====
        db.execSQL("CREATE TABLE " + TABLE_ALERT_RECORDS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "greenhouse_id INTEGER NOT NULL, "
                + "sensor_type VARCHAR(30) NOT NULL, "
                + "sensor_name VARCHAR(50) NOT NULL, "
                + "value FLOAT NOT NULL, "
                + "threshold_min FLOAT NOT NULL, "
                + "threshold_max FLOAT NOT NULL, "
                + "level INTEGER NOT NULL DEFAULT 1, "
                + "level_name VARCHAR(10), "
                + "status INTEGER NOT NULL DEFAULT 0, "
                + "record_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                + "handle_time DATETIME, "
                + "handler VARCHAR(50))");
        db.execSQL("CREATE INDEX idx_alert_gh_time ON "
                + TABLE_ALERT_RECORDS + "(greenhouse_id, record_time)");

        // ===== 门锁/人体感应记录表 =====
        db.execSQL("CREATE TABLE " + TABLE_ACCESS_LOGS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "greenhouse_id INTEGER NOT NULL, "
                + "event_type VARCHAR(30) NOT NULL, "
                + "event_name VARCHAR(50) NOT NULL, "
                + "operator VARCHAR(50) NOT NULL DEFAULT '系统', "
                + "record_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                + "remarks TEXT)");
        db.execSQL("CREATE INDEX idx_access_gh_time ON "
                + TABLE_ACCESS_LOGS + "(greenhouse_id, record_time)");

        // ===== 人员信息表 =====
        db.execSQL("CREATE TABLE " + TABLE_PERSONNEL + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "employee_no VARCHAR(20) NOT NULL UNIQUE, "
                + "name VARCHAR(50) NOT NULL, "
                + "gender VARCHAR(10), "
                + "position VARCHAR(50) NOT NULL, "
                + "phone VARCHAR(20), "
                + "greenhouse_id INTEGER, "
                + "status INTEGER NOT NULL DEFAULT 1, "
                + "create_time DATETIME DEFAULT CURRENT_TIMESTAMP)");

        // ===== 农事记录表 =====
        db.execSQL("CREATE TABLE " + TABLE_FARMING_LOGS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "greenhouse_id INTEGER NOT NULL, "
                + "log_type VARCHAR(50) NOT NULL, "
                + "operator VARCHAR(50) NOT NULL, "
                + "operate_time DATETIME NOT NULL, "
                + "content TEXT, "
                + "remarks TEXT, "
                + "create_time DATETIME DEFAULT CURRENT_TIMESTAMP)");
        db.execSQL("CREATE INDEX idx_farm_gh_time ON "
                + TABLE_FARMING_LOGS + "(greenhouse_id, operate_time)");

        // ===== 水肥管理表 =====
        db.execSQL("CREATE TABLE " + TABLE_WATER_FERT_LOGS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "greenhouse_id INTEGER NOT NULL, "
                + "log_type VARCHAR(50) NOT NULL, "
                + "operator VARCHAR(50) NOT NULL, "
                + "operate_time DATETIME NOT NULL, "
                + "fertilizer_name VARCHAR(200), "
                + "fertilizer_amount VARCHAR(100), "
                + "irrigation_amount VARCHAR(100), "
                + "duration_min INTEGER DEFAULT 0, "
                + "content TEXT, "
                + "remarks TEXT, "
                + "create_time DATETIME DEFAULT CURRENT_TIMESTAMP)");
        db.execSQL("CREATE INDEX idx_wf_gh_time ON "
                + TABLE_WATER_FERT_LOGS + "(greenhouse_id, operate_time)");

        // ===== 病虫害防治表 =====
        db.execSQL("CREATE TABLE " + TABLE_PEST_CONTROL + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "greenhouse_id INTEGER NOT NULL, "
                + "find_date DATE NOT NULL, "
                + "pest_type VARCHAR(100) NOT NULL, "
                + "affected_area VARCHAR(200), "
                + "symptoms TEXT, "
                + "control_measures TEXT, "
                + "pesticide_name VARCHAR(200), "
                + "pesticide_amount VARCHAR(100), "
                + "control_effect VARCHAR(200), "
                + "recorder VARCHAR(50) NOT NULL, "
                + "photo_path VARCHAR(500), "
                + "create_time DATETIME DEFAULT CURRENT_TIMESTAMP)");
        db.execSQL("CREATE INDEX idx_pest_gh_date ON "
                + TABLE_PEST_CONTROL + "(greenhouse_id, find_date)");

        // ===== 设备运维表 =====
        db.execSQL("CREATE TABLE " + TABLE_MAINTENANCE_LOGS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "greenhouse_id INTEGER NOT NULL, "
                + "device_name VARCHAR(100) NOT NULL, "
                + "maintenance_type VARCHAR(30) NOT NULL, "
                + "operator VARCHAR(50) NOT NULL, "
                + "operate_time DATETIME NOT NULL, "
                + "content TEXT, "
                + "cost VARCHAR(50), "
                + "remarks TEXT, "
                + "create_time DATETIME DEFAULT CURRENT_TIMESTAMP)");
        db.execSQL("CREATE INDEX idx_maint_gh_time ON "
                + TABLE_MAINTENANCE_LOGS + "(greenhouse_id, operate_time)");

        // ===== 产销台账表 =====
        db.execSQL("CREATE TABLE " + TABLE_SALES_RECORDS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "batch_no VARCHAR(50) NOT NULL UNIQUE, "
                + "greenhouse_id INTEGER NOT NULL, "
                + "variety VARCHAR(100) NOT NULL, "
                + "harvest_date DATE NOT NULL, "
                + "quantity INTEGER NOT NULL, "
                + "quality_grade VARCHAR(20), "
                + "sale_channel VARCHAR(100), "
                + "unit_price DECIMAL(10,2), "
                + "total_amount DECIMAL(10,2), "
                + "customer VARCHAR(200), "
                + "recorder VARCHAR(50) NOT NULL, "
                + "create_time DATETIME DEFAULT CURRENT_TIMESTAMP)");
        db.execSQL("CREATE INDEX idx_sales_gh_date ON "
                + TABLE_SALES_RECORDS + "(greenhouse_id, harvest_date)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE " + TABLE_DEVICE_LOGS + " ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "greenhouse_id INTEGER NOT NULL, "
                    + "device_type VARCHAR(50) NOT NULL, "
                    + "device_name VARCHAR(100) NOT NULL, "
                    + "action VARCHAR(50) NOT NULL, "
                    + "mode VARCHAR(20) NOT NULL DEFAULT 'manual', "
                    + "operator VARCHAR(50) NOT NULL, "
                    + "operate_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "result VARCHAR(20) NOT NULL DEFAULT 'success', "
                    + "remarks TEXT)");
            db.execSQL("CREATE INDEX idx_devlog_gh_time ON "
                    + TABLE_DEVICE_LOGS + "(greenhouse_id, operate_time)");
        }
        if (oldVersion < 3) {
            db.execSQL("CREATE TABLE " + TABLE_ALERT_RECORDS + " ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "greenhouse_id INTEGER NOT NULL, "
                    + "sensor_type VARCHAR(30) NOT NULL, "
                    + "sensor_name VARCHAR(50) NOT NULL, "
                    + "value FLOAT NOT NULL, "
                    + "threshold_min FLOAT NOT NULL, "
                    + "threshold_max FLOAT NOT NULL, "
                    + "level INTEGER NOT NULL DEFAULT 1, "
                    + "level_name VARCHAR(10), "
                    + "status INTEGER NOT NULL DEFAULT 0, "
                    + "record_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "handle_time DATETIME, "
                    + "handler VARCHAR(50))");
            db.execSQL("CREATE INDEX idx_alert_gh_time ON "
                    + TABLE_ALERT_RECORDS + "(greenhouse_id, record_time)");
            db.execSQL("CREATE TABLE " + TABLE_ACCESS_LOGS + " ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "greenhouse_id INTEGER NOT NULL, "
                    + "event_type VARCHAR(30) NOT NULL, "
                    + "event_name VARCHAR(50) NOT NULL, "
                    + "operator VARCHAR(50) NOT NULL DEFAULT '系统', "
                    + "record_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "remarks TEXT)");
            db.execSQL("CREATE INDEX idx_access_gh_time ON "
                    + TABLE_ACCESS_LOGS + "(greenhouse_id, record_time)");
        }
        if (oldVersion < 4) {
            // 人员
            db.execSQL("CREATE TABLE " + TABLE_PERSONNEL + " ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "employee_no VARCHAR(20) NOT NULL UNIQUE, "
                    + "name VARCHAR(50) NOT NULL, "
                    + "gender VARCHAR(10), "
                    + "position VARCHAR(50) NOT NULL, "
                    + "phone VARCHAR(20), "
                    + "greenhouse_id INTEGER, "
                    + "status INTEGER NOT NULL DEFAULT 1, "
                    + "create_time DATETIME DEFAULT CURRENT_TIMESTAMP)");
            // 农事
            db.execSQL("CREATE TABLE " + TABLE_FARMING_LOGS + " ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "greenhouse_id INTEGER NOT NULL, "
                    + "log_type VARCHAR(50) NOT NULL, "
                    + "operator VARCHAR(50) NOT NULL, "
                    + "operate_time DATETIME NOT NULL, "
                    + "content TEXT, "
                    + "remarks TEXT, "
                    + "create_time DATETIME DEFAULT CURRENT_TIMESTAMP)");
            db.execSQL("CREATE INDEX idx_farm_gh_time ON "
                    + TABLE_FARMING_LOGS + "(greenhouse_id, operate_time)");
            // 水肥
            db.execSQL("CREATE TABLE " + TABLE_WATER_FERT_LOGS + " ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "greenhouse_id INTEGER NOT NULL, "
                    + "log_type VARCHAR(50) NOT NULL, "
                    + "operator VARCHAR(50) NOT NULL, "
                    + "operate_time DATETIME NOT NULL, "
                    + "fertilizer_name VARCHAR(200), "
                    + "fertilizer_amount VARCHAR(100), "
                    + "irrigation_amount VARCHAR(100), "
                    + "duration_min INTEGER DEFAULT 0, "
                    + "content TEXT, "
                    + "remarks TEXT, "
                    + "create_time DATETIME DEFAULT CURRENT_TIMESTAMP)");
            db.execSQL("CREATE INDEX idx_wf_gh_time ON "
                    + TABLE_WATER_FERT_LOGS + "(greenhouse_id, operate_time)");
            // 病虫害
            db.execSQL("CREATE TABLE " + TABLE_PEST_CONTROL + " ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "greenhouse_id INTEGER NOT NULL, "
                    + "find_date DATE NOT NULL, "
                    + "pest_type VARCHAR(100) NOT NULL, "
                    + "affected_area VARCHAR(200), "
                    + "symptoms TEXT, "
                    + "control_measures TEXT, "
                    + "pesticide_name VARCHAR(200), "
                    + "pesticide_amount VARCHAR(100), "
                    + "control_effect VARCHAR(200), "
                    + "recorder VARCHAR(50) NOT NULL, "
                    + "photo_path VARCHAR(500), "
                    + "create_time DATETIME DEFAULT CURRENT_TIMESTAMP)");
            db.execSQL("CREATE INDEX idx_pest_gh_date ON "
                    + TABLE_PEST_CONTROL + "(greenhouse_id, find_date)");
            // 运维
            db.execSQL("CREATE TABLE " + TABLE_MAINTENANCE_LOGS + " ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "greenhouse_id INTEGER NOT NULL, "
                    + "device_name VARCHAR(100) NOT NULL, "
                    + "maintenance_type VARCHAR(30) NOT NULL, "
                    + "operator VARCHAR(50) NOT NULL, "
                    + "operate_time DATETIME NOT NULL, "
                    + "content TEXT, "
                    + "cost VARCHAR(50), "
                    + "remarks TEXT, "
                    + "create_time DATETIME DEFAULT CURRENT_TIMESTAMP)");
            db.execSQL("CREATE INDEX idx_maint_gh_time ON "
                    + TABLE_MAINTENANCE_LOGS + "(greenhouse_id, operate_time)");
            // 产销
            db.execSQL("CREATE TABLE " + TABLE_SALES_RECORDS + " ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "batch_no VARCHAR(50) NOT NULL UNIQUE, "
                    + "greenhouse_id INTEGER NOT NULL, "
                    + "variety VARCHAR(100) NOT NULL, "
                    + "harvest_date DATE NOT NULL, "
                    + "quantity INTEGER NOT NULL, "
                    + "quality_grade VARCHAR(20), "
                    + "sale_channel VARCHAR(100), "
                    + "unit_price DECIMAL(10,2), "
                    + "total_amount DECIMAL(10,2), "
                    + "customer VARCHAR(200), "
                    + "recorder VARCHAR(50) NOT NULL, "
                    + "create_time DATETIME DEFAULT CURRENT_TIMESTAMP)");
            db.execSQL("CREATE INDEX idx_sales_gh_date ON "
                    + TABLE_SALES_RECORDS + "(greenhouse_id, harvest_date)");
        }
    }

    // ==================== 用户相关操作 ====================

    /**
     * 验证登录
     * @return User对象；验证失败返回null
     */
    public User login(String username, String passwordMD5) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT id, username, real_name, role FROM " + TABLE_USERS
                        + " WHERE username=? AND password=? AND status=1",
                new String[]{username, passwordMD5});
        User user = null;
        if (cursor.moveToFirst()) {
            user = new User();
            user.setId(cursor.getInt(0));
            user.setUsername(cursor.getString(1));
            user.setRealName(cursor.getString(2));
            user.setRole(cursor.getString(3));
        }
        cursor.close();
        return user;
    }

    // ==================== 传感器数据操作 ====================

    /**
     * 保存一条传感器数据
     */
    public long saveSensorData(SensorData data) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("greenhouse_id", data.getGreenhouseId());
        cv.put("temp", data.getTemp());
        cv.put("humidity", data.getHumidity());
        cv.put("co2", data.getCo2());
        cv.put("light", data.getLight());
        cv.put("soil_temp", data.getSoilTemp());
        cv.put("soil_humidity", data.getSoilHumidity());
        cv.put("ph", data.getPh());
        cv.put("ec", data.getEc());
        cv.put("record_time", data.getRecordTime());
        return db.insert(TABLE_SENSOR, null, cv);
    }

    /**
     * 获取某个大棚的最新一条传感器数据
     */
    public SensorData getLatestSensorData(int greenhouseId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_SENSOR
                        + " WHERE greenhouse_id=? ORDER BY record_time DESC LIMIT 1",
                new String[]{String.valueOf(greenhouseId)});
        SensorData data = null;
        if (cursor.moveToFirst()) {
            data = cursorToSensorData(cursor);
        }
        cursor.close();
        return data;
    }

    /** Cursor → SensorData */
    private SensorData cursorToSensorData(Cursor c) {
        SensorData d = new SensorData();
        d.setId(c.getInt(c.getColumnIndexOrThrow("id")));
        d.setGreenhouseId(c.getInt(c.getColumnIndexOrThrow("greenhouse_id")));
        d.setTemp(c.getFloat(c.getColumnIndexOrThrow("temp")));
        d.setHumidity(c.getFloat(c.getColumnIndexOrThrow("humidity")));
        d.setCo2(c.getFloat(c.getColumnIndexOrThrow("co2")));
        d.setLight(c.getFloat(c.getColumnIndexOrThrow("light")));
        d.setSoilTemp(c.getFloat(c.getColumnIndexOrThrow("soil_temp")));
        d.setSoilHumidity(c.getFloat(c.getColumnIndexOrThrow("soil_humidity")));
        d.setPh(c.getFloat(c.getColumnIndexOrThrow("ph")));
        d.setEc(c.getFloat(c.getColumnIndexOrThrow("ec")));
        d.setRecordTime(c.getString(c.getColumnIndexOrThrow("record_time")));
        return d;
    }

    // ==================== 历史查询 ====================

    /**
     * 查询历史传感器数据
     *
     * @param greenhouseId 大棚ID
     * @param startTime    起始时间 "yyyy-MM-dd HH:mm:ss"
     * @param endTime      结束时间 "yyyy-MM-dd HH:mm:ss"
     * @param limit        最大条数，0=不限制
     * @return 按时间升序排列的数据列表
     */
    public List<SensorData> getHistoryData(int greenhouseId, String startTime, String endTime, int limit) {
        List<SensorData> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String sql = "SELECT * FROM " + TABLE_SENSOR
                + " WHERE greenhouse_id=? AND record_time BETWEEN ? AND ?"
                + " ORDER BY record_time ASC";
        if (limit > 0) {
            sql += " LIMIT " + limit;
        }
        Cursor cursor = db.rawQuery(sql, new String[]{
                String.valueOf(greenhouseId), startTime, endTime});
        while (cursor.moveToNext()) {
            list.add(cursorToSensorData(cursor));
        }
        cursor.close();
        return list;
    }

    /**
     * 查询某个大棚最新的 N 条数据
     */
    public List<SensorData> getRecentData(int greenhouseId, int count) {
        List<SensorData> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_SENSOR
                        + " WHERE greenhouse_id=? ORDER BY record_time DESC LIMIT ?",
                new String[]{String.valueOf(greenhouseId), String.valueOf(count)});
        while (cursor.moveToNext()) {
            list.add(cursorToSensorData(cursor));
        }
        cursor.close();
        return list;
    }

    // ==================== 设备日志操作 ====================

    /**
     * 保存设备操作日志
     */
    public void saveDeviceLog(int ghId, String deviceType, String deviceName,
                               String action, String mode, String operator) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("greenhouse_id", ghId);
        cv.put("device_type", deviceType);
        cv.put("device_name", deviceName);
        cv.put("action", action);
        cv.put("mode", mode);
        cv.put("operator", operator);
        cv.put("operate_time",
                new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                        .format(new java.util.Date()));
        db.insert(TABLE_DEVICE_LOGS, null, cv);
    }

    /**
     * 获取最近的操作日志（格式化文本，供列表展示）
     */
    public List<String> getDeviceLogs(int limit) {
        List<String> logs = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT operate_time, device_name, action, mode, operator FROM " + TABLE_DEVICE_LOGS
                        + " ORDER BY operate_time DESC LIMIT ?",
                new String[]{String.valueOf(limit)});
        while (cursor.moveToNext()) {
            logs.add(cursor.getString(0) + " | "
                    + cursor.getString(1) + " | "
                    + cursor.getString(2) + " | "
                    + cursor.getString(3) + " | "
                    + cursor.getString(4));
        }
        cursor.close();
        return logs;
    }

    // ==================== 预警记录操作 ====================

    /** 保存预警记录 */
    public long saveAlertRecord(AlertRecord alert) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("greenhouse_id", alert.getGreenhouseId());
        cv.put("sensor_type", alert.getSensorType());
        cv.put("sensor_name", alert.getSensorName());
        cv.put("value", alert.getValue());
        cv.put("threshold_min", alert.getThresholdMin());
        cv.put("threshold_max", alert.getThresholdMax());
        cv.put("level", alert.getLevel());
        cv.put("level_name", alert.getLevelName());
        cv.put("status", alert.getStatus());
        cv.put("record_time", alert.getRecordTime());
        return db.insert(TABLE_ALERT_RECORDS, null, cv);
    }

    /** 获取预警记录列表，支持筛选 */
    public List<AlertRecord> getAlertRecords(int greenhouseId, String sensorType, int level, int limit) {
        List<AlertRecord> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        StringBuilder sql = new StringBuilder("SELECT * FROM " + TABLE_ALERT_RECORDS + " WHERE 1=1");
        List<String> args = new ArrayList<>();
        if (greenhouseId > 0) {
            sql.append(" AND greenhouse_id=?");
            args.add(String.valueOf(greenhouseId));
        }
        if (sensorType != null && !sensorType.isEmpty()) {
            sql.append(" AND sensor_type=?");
            args.add(sensorType);
        }
        if (level > 0) {
            sql.append(" AND level=?");
            args.add(String.valueOf(level));
        }
        sql.append(" ORDER BY record_time DESC LIMIT ?");
        args.add(String.valueOf(limit));
        Cursor cursor = db.rawQuery(sql.toString(), args.toArray(new String[0]));
        while (cursor.moveToNext()) {
            list.add(cursorToAlertRecord(cursor));
        }
        cursor.close();
        return list;
    }

    private AlertRecord cursorToAlertRecord(Cursor c) {
        AlertRecord a = new AlertRecord();
        a.setId(c.getInt(c.getColumnIndexOrThrow("id")));
        a.setGreenhouseId(c.getInt(c.getColumnIndexOrThrow("greenhouse_id")));
        a.setSensorType(c.getString(c.getColumnIndexOrThrow("sensor_type")));
        a.setSensorName(c.getString(c.getColumnIndexOrThrow("sensor_name")));
        a.setValue(c.getFloat(c.getColumnIndexOrThrow("value")));
        a.setThresholdMin(c.getFloat(c.getColumnIndexOrThrow("threshold_min")));
        a.setThresholdMax(c.getFloat(c.getColumnIndexOrThrow("threshold_max")));
        a.setLevel(c.getInt(c.getColumnIndexOrThrow("level")));
        a.setLevelName(c.getString(c.getColumnIndexOrThrow("level_name")));
        a.setStatus(c.getInt(c.getColumnIndexOrThrow("status")));
        a.setRecordTime(c.getString(c.getColumnIndexOrThrow("record_time")));
        int hIdx = c.getColumnIndex("handle_time");
        a.setHandleTime(c.isNull(hIdx) ? null : c.getString(hIdx));
        int hdIdx = c.getColumnIndex("handler");
        a.setHandler(c.isNull(hdIdx) ? null : c.getString(hdIdx));
        return a;
    }

    /** 标记预警为已处理 */
    public void markAlertHandled(int alertId, String handler) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("status", 1);
        cv.put("handler", handler);
        cv.put("handle_time", new java.text.SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date()));
        db.update(TABLE_ALERT_RECORDS, cv, "id=?", new String[]{String.valueOf(alertId)});
    }

    /** 统计预警数量 */
    public long countAlertRecords(int greenhouseId, int status) {
        SQLiteDatabase db = getReadableDatabase();
        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) FROM " + TABLE_ALERT_RECORDS + " WHERE 1=1");
        List<String> args = new ArrayList<>();
        if (greenhouseId > 0) {
            sql.append(" AND greenhouse_id=?");
            args.add(String.valueOf(greenhouseId));
        }
        if (status >= 0) {
            sql.append(" AND status=?");
            args.add(String.valueOf(status));
        }
        Cursor cursor = db.rawQuery(sql.toString(), args.toArray(new String[0]));
        long count = 0;
        if (cursor.moveToFirst()) count = cursor.getLong(0);
        cursor.close();
        return count;
    }

    // ==================== 门锁/人体感应记录操作 ====================

    /** 保存门锁/人体感应记录 */
    public long saveAccessLog(AccessLog log) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("greenhouse_id", log.getGreenhouseId());
        cv.put("event_type", log.getEventType());
        cv.put("event_name", log.getEventName());
        cv.put("operator", log.getOperator());
        cv.put("record_time", new java.text.SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date()));
        cv.put("remarks", log.getRemarks());
        return db.insert(TABLE_ACCESS_LOGS, null, cv);
    }

    /** 获取门锁/人体感应记录 */
    public List<AccessLog> getAccessLogs(int greenhouseId, int limit) {
        List<AccessLog> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        StringBuilder sql = new StringBuilder(
                "SELECT * FROM " + TABLE_ACCESS_LOGS + " WHERE 1=1");
        List<String> args = new ArrayList<>();
        if (greenhouseId > 0) {
            sql.append(" AND greenhouse_id=?");
            args.add(String.valueOf(greenhouseId));
        }
        sql.append(" ORDER BY record_time DESC LIMIT ?");
        args.add(String.valueOf(limit));
        Cursor cursor = db.rawQuery(sql.toString(), args.toArray(new String[0]));
        while (cursor.moveToNext()) {
            AccessLog log = new AccessLog();
            log.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
            log.setGreenhouseId(cursor.getInt(cursor.getColumnIndexOrThrow("greenhouse_id")));
            log.setEventType(cursor.getString(cursor.getColumnIndexOrThrow("event_type")));
            log.setEventName(cursor.getString(cursor.getColumnIndexOrThrow("event_name")));
            log.setOperator(cursor.getString(cursor.getColumnIndexOrThrow("operator")));
            log.setRecordTime(cursor.getString(cursor.getColumnIndexOrThrow("record_time")));
            int rIdx = cursor.getColumnIndex("remarks");
            log.setRemarks(cursor.isNull(rIdx) ? "" : cursor.getString(rIdx));
            list.add(log);
        }
        cursor.close();
        return list;
    }

    // ==================== 人员管理 CRUD ====================

    public long savePersonnel(Personnel p) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("employee_no", p.getEmployeeNo());
        cv.put("name", p.getName());
        cv.put("gender", p.getGender());
        cv.put("position", p.getPosition());
        cv.put("phone", p.getPhone());
        cv.put("greenhouse_id", p.getGreenhouseId());
        cv.put("status", p.getStatus());
        cv.put("create_time", new java.text.SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date()));
        return db.insert(TABLE_PERSONNEL, null, cv);
    }

    public void updatePersonnel(Personnel p) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", p.getName());
        cv.put("gender", p.getGender());
        cv.put("position", p.getPosition());
        cv.put("phone", p.getPhone());
        cv.put("greenhouse_id", p.getGreenhouseId());
        cv.put("status", p.getStatus());
        db.update(TABLE_PERSONNEL, cv, "id=?", new String[]{String.valueOf(p.getId())});
    }

    public void deletePersonnel(int id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_PERSONNEL, "id=?", new String[]{String.valueOf(id)});
    }

    public List<Personnel> getAllPersonnel() {
        List<Personnel> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_PERSONNEL + " ORDER BY id DESC", null);
        while (c.moveToNext()) {
            Personnel p = new Personnel();
            p.setId(c.getInt(c.getColumnIndexOrThrow("id")));
            p.setEmployeeNo(c.getString(c.getColumnIndexOrThrow("employee_no")));
            p.setName(c.getString(c.getColumnIndexOrThrow("name")));
            p.setGender(c.getString(c.getColumnIndexOrThrow("gender")));
            p.setPosition(c.getString(c.getColumnIndexOrThrow("position")));
            p.setPhone(c.getString(c.getColumnIndexOrThrow("phone")));
            p.setGreenhouseId(c.getInt(c.getColumnIndexOrThrow("greenhouse_id")));
            p.setStatus(c.getInt(c.getColumnIndexOrThrow("status")));
            p.setCreateTime(c.getString(c.getColumnIndexOrThrow("create_time")));
            list.add(p);
        }
        c.close();
        return list;
    }

    // ==================== 农事日志 CRUD ====================

    public long saveFarmingLog(ManagementLog m) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("greenhouse_id", m.getGreenhouseId());
        cv.put("log_type", m.getLogType());
        cv.put("operator", m.getOperator());
        cv.put("operate_time", m.getOperateTime());
        cv.put("content", m.getContent());
        cv.put("remarks", m.getRemarks());
        cv.put("create_time", new java.text.SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date()));
        return db.insert(TABLE_FARMING_LOGS, null, cv);
    }

    public void deleteFarmingLog(int id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_FARMING_LOGS, "id=?", new String[]{String.valueOf(id)});
    }

    public List<ManagementLog> getFarmingLogs(int greenhouseId, int limit) {
        return getManagementLogs(TABLE_FARMING_LOGS, greenhouseId, limit, false);
    }

    // ==================== 水肥日志 CRUD ====================

    public long saveWaterFertLog(ManagementLog m) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("greenhouse_id", m.getGreenhouseId());
        cv.put("log_type", m.getLogType());
        cv.put("operator", m.getOperator());
        cv.put("operate_time", m.getOperateTime());
        cv.put("fertilizer_name", m.getFertilizerName());
        cv.put("fertilizer_amount", m.getFertilizerAmount());
        cv.put("irrigation_amount", m.getIrrigationAmount());
        cv.put("duration_min", m.getDurationMin());
        cv.put("content", m.getContent());
        cv.put("remarks", m.getRemarks());
        cv.put("create_time", new java.text.SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date()));
        return db.insert(TABLE_WATER_FERT_LOGS, null, cv);
    }

    public void deleteWaterFertLog(int id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_WATER_FERT_LOGS, "id=?", new String[]{String.valueOf(id)});
    }

    public List<ManagementLog> getWaterFertLogs(int greenhouseId, int limit) {
        return getManagementLogsExt(TABLE_WATER_FERT_LOGS, greenhouseId, limit);
    }

    // ==================== 运维日志 CRUD ====================

    public long saveMaintenanceLog(ManagementLog m) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("greenhouse_id", m.getGreenhouseId());
        cv.put("device_name", m.getDeviceName());
        cv.put("maintenance_type", m.getMaintenanceType());
        cv.put("operator", m.getOperator());
        cv.put("operate_time", m.getOperateTime());
        cv.put("content", m.getContent());
        cv.put("cost", m.getCost());
        cv.put("remarks", m.getRemarks());
        cv.put("create_time", new java.text.SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date()));
        return db.insert(TABLE_MAINTENANCE_LOGS, null, cv);
    }

    public void deleteMaintenanceLog(int id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_MAINTENANCE_LOGS, "id=?", new String[]{String.valueOf(id)});
    }

    public List<ManagementLog> getMaintenanceLogs(int greenhouseId, int limit) {
        List<ManagementLog> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        StringBuilder sql = new StringBuilder(
                "SELECT * FROM " + TABLE_MAINTENANCE_LOGS + " WHERE 1=1");
        List<String> args = new ArrayList<>();
        if (greenhouseId > 0) { sql.append(" AND greenhouse_id=?"); args.add(String.valueOf(greenhouseId)); }
        sql.append(" ORDER BY operate_time DESC LIMIT ?");
        args.add(String.valueOf(limit));
        Cursor c = db.rawQuery(sql.toString(), args.toArray(new String[0]));
        while (c.moveToNext()) {
            ManagementLog m = new ManagementLog();
            m.setId(c.getInt(c.getColumnIndexOrThrow("id")));
            m.setGreenhouseId(c.getInt(c.getColumnIndexOrThrow("greenhouse_id")));
            m.setDeviceName(c.getString(c.getColumnIndexOrThrow("device_name")));
            m.setMaintenanceType(c.getString(c.getColumnIndexOrThrow("maintenance_type")));
            m.setOperator(c.getString(c.getColumnIndexOrThrow("operator")));
            m.setOperateTime(c.getString(c.getColumnIndexOrThrow("operate_time")));
            m.setContent(c.getString(c.getColumnIndexOrThrow("content")));
            int ci = c.getColumnIndex("cost");
            m.setCost(c.isNull(ci) ? "" : c.getString(ci));
            int ri = c.getColumnIndex("remarks");
            m.setRemarks(c.isNull(ri) ? "" : c.getString(ri));
            m.setCategory("maintenance");
            list.add(m);
        }
        c.close();
        return list;
    }

    // ==================== 病虫害 CRUD ====================

    public long savePestControl(PestControl pc) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("greenhouse_id", pc.getGreenhouseId());
        cv.put("find_date", pc.getFindDate());
        cv.put("pest_type", pc.getPestType());
        cv.put("affected_area", pc.getAffectedArea());
        cv.put("symptoms", pc.getSymptoms());
        cv.put("control_measures", pc.getControlMeasures());
        cv.put("pesticide_name", pc.getPesticideName());
        cv.put("pesticide_amount", pc.getPesticideAmount());
        cv.put("control_effect", pc.getControlEffect());
        cv.put("recorder", pc.getRecorder());
        cv.put("photo_path", pc.getPhotoPath());
        cv.put("create_time", new java.text.SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date()));
        return db.insert(TABLE_PEST_CONTROL, null, cv);
    }

    public void deletePestControl(int id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_PEST_CONTROL, "id=?", new String[]{String.valueOf(id)});
    }

    public List<PestControl> getPestControls(int greenhouseId, int limit) {
        List<PestControl> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        StringBuilder sql = new StringBuilder(
                "SELECT * FROM " + TABLE_PEST_CONTROL + " WHERE 1=1");
        List<String> args = new ArrayList<>();
        if (greenhouseId > 0) { sql.append(" AND greenhouse_id=?"); args.add(String.valueOf(greenhouseId)); }
        sql.append(" ORDER BY find_date DESC LIMIT ?");
        args.add(String.valueOf(limit));
        Cursor c = db.rawQuery(sql.toString(), args.toArray(new String[0]));
        while (c.moveToNext()) {
            PestControl pc = new PestControl();
            pc.setId(c.getInt(c.getColumnIndexOrThrow("id")));
            pc.setGreenhouseId(c.getInt(c.getColumnIndexOrThrow("greenhouse_id")));
            pc.setFindDate(c.getString(c.getColumnIndexOrThrow("find_date")));
            pc.setPestType(c.getString(c.getColumnIndexOrThrow("pest_type")));
            pc.setAffectedArea(c.getString(c.getColumnIndexOrThrow("affected_area")));
            pc.setSymptoms(c.getString(c.getColumnIndexOrThrow("symptoms")));
            pc.setControlMeasures(c.getString(c.getColumnIndexOrThrow("control_measures")));
            pc.setPesticideName(c.getString(c.getColumnIndexOrThrow("pesticide_name")));
            pc.setPesticideAmount(c.getString(c.getColumnIndexOrThrow("pesticide_amount")));
            pc.setControlEffect(c.getString(c.getColumnIndexOrThrow("control_effect")));
            pc.setRecorder(c.getString(c.getColumnIndexOrThrow("recorder")));
            int pi = c.getColumnIndex("photo_path");
            pc.setPhotoPath(c.isNull(pi) ? "" : c.getString(pi));
            pc.setCreateTime(c.getString(c.getColumnIndexOrThrow("create_time")));
            list.add(pc);
        }
        c.close();
        return list;
    }

    // ==================== 产销台账 CRUD ====================

    public long saveSalesRecord(SalesRecord sr) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("batch_no", sr.getBatchNo());
        cv.put("greenhouse_id", sr.getGreenhouseId());
        cv.put("variety", sr.getVariety());
        cv.put("harvest_date", sr.getHarvestDate());
        cv.put("quantity", sr.getQuantity());
        cv.put("quality_grade", sr.getQualityGrade());
        cv.put("sale_channel", sr.getSaleChannel());
        cv.put("unit_price", sr.getUnitPrice());
        cv.put("total_amount", sr.getTotalAmount());
        cv.put("customer", sr.getCustomer());
        cv.put("recorder", sr.getRecorder());
        cv.put("create_time", new java.text.SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date()));
        return db.insert(TABLE_SALES_RECORDS, null, cv);
    }

    public void deleteSalesRecord(int id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_SALES_RECORDS, "id=?", new String[]{String.valueOf(id)});
    }

    public List<SalesRecord> getSalesRecords(int greenhouseId, int limit) {
        List<SalesRecord> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        StringBuilder sql = new StringBuilder(
                "SELECT * FROM " + TABLE_SALES_RECORDS + " WHERE 1=1");
        List<String> args = new ArrayList<>();
        if (greenhouseId > 0) { sql.append(" AND greenhouse_id=?"); args.add(String.valueOf(greenhouseId)); }
        sql.append(" ORDER BY harvest_date DESC LIMIT ?");
        args.add(String.valueOf(limit));
        Cursor c = db.rawQuery(sql.toString(), args.toArray(new String[0]));
        while (c.moveToNext()) {
            SalesRecord sr = new SalesRecord();
            sr.setId(c.getInt(c.getColumnIndexOrThrow("id")));
            sr.setBatchNo(c.getString(c.getColumnIndexOrThrow("batch_no")));
            sr.setGreenhouseId(c.getInt(c.getColumnIndexOrThrow("greenhouse_id")));
            sr.setVariety(c.getString(c.getColumnIndexOrThrow("variety")));
            sr.setHarvestDate(c.getString(c.getColumnIndexOrThrow("harvest_date")));
            sr.setQuantity(c.getInt(c.getColumnIndexOrThrow("quantity")));
            sr.setQualityGrade(c.getString(c.getColumnIndexOrThrow("quality_grade")));
            sr.setSaleChannel(c.getString(c.getColumnIndexOrThrow("sale_channel")));
            sr.setUnitPrice(c.getDouble(c.getColumnIndexOrThrow("unit_price")));
            sr.setTotalAmount(c.getDouble(c.getColumnIndexOrThrow("total_amount")));
            sr.setCustomer(c.getString(c.getColumnIndexOrThrow("customer")));
            sr.setRecorder(c.getString(c.getColumnIndexOrThrow("recorder")));
            sr.setCreateTime(c.getString(c.getColumnIndexOrThrow("create_time")));
            list.add(sr);
        }
        c.close();
        return list;
    }

    /** 产销按月统计：返回 {month, total_qty, total_amount} 文本列表 */
    public List<String> getSalesMonthlyStats() {
        List<String> stats = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT substr(harvest_date,1,7) AS mon, SUM(quantity), SUM(total_amount) "
                        + "FROM " + TABLE_SALES_RECORDS
                        + " GROUP BY mon ORDER BY mon DESC LIMIT 12", null);
        while (c.moveToNext()) {
            stats.add(c.getString(0) + " | 总量:" + c.getInt(1)
                    + " | 金额:¥" + String.format("%.2f", c.getDouble(2)));
        }
        c.close();
        return stats;
    }

    // ==================== 农事/水肥 通用读取辅助 ====================

    private List<ManagementLog> getManagementLogs(String table, int greenhouseId, int limit, boolean extended) {
        List<ManagementLog> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        StringBuilder sql = new StringBuilder("SELECT * FROM " + table + " WHERE 1=1");
        List<String> args = new ArrayList<>();
        if (greenhouseId > 0) { sql.append(" AND greenhouse_id=?"); args.add(String.valueOf(greenhouseId)); }
        sql.append(" ORDER BY operate_time DESC LIMIT ?");
        args.add(String.valueOf(limit));
        Cursor c = db.rawQuery(sql.toString(), args.toArray(new String[0]));
        while (c.moveToNext()) {
            ManagementLog m = new ManagementLog();
            m.setId(c.getInt(c.getColumnIndexOrThrow("id")));
            m.setGreenhouseId(c.getInt(c.getColumnIndexOrThrow("greenhouse_id")));
            m.setLogType(c.getString(c.getColumnIndexOrThrow("log_type")));
            m.setOperator(c.getString(c.getColumnIndexOrThrow("operator")));
            m.setOperateTime(c.getString(c.getColumnIndexOrThrow("operate_time")));
            m.setContent(c.getString(c.getColumnIndexOrThrow("content")));
            int ri = c.getColumnIndex("remarks");
            m.setRemarks(c.isNull(ri) ? "" : c.getString(ri));
            if (extended) {
                int fi = c.getColumnIndex("fertilizer_name");
                m.setFertilizerName(c.isNull(fi) ? "" : c.getString(fi));
                int ai = c.getColumnIndex("fertilizer_amount");
                m.setFertilizerAmount(c.isNull(ai) ? "" : c.getString(ai));
                int ii = c.getColumnIndex("irrigation_amount");
                m.setIrrigationAmount(c.isNull(ii) ? "" : c.getString(ii));
                int di = c.getColumnIndex("duration_min");
                m.setDurationMin(c.isNull(di) ? 0 : c.getInt(di));
            }
            list.add(m);
        }
        c.close();
        return list;
    }

    private List<ManagementLog> getManagementLogsExt(String table, int greenhouseId, int limit) {
        return getManagementLogs(table, greenhouseId, limit, true);
    }
}
