package com.example.vvgreenhouse.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.example.vvgreenhouse.model.SensorData;
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
    private static final int DB_VERSION = 2;

    // 单例
    private static GreenhouseDBHelper instance;

    // ========== 表名 ==========
    public static final String TABLE_USERS = "users";
    public static final String TABLE_GREENHOUSE = "greenhouse_info";
    public static final String TABLE_SENSOR = "sensor_data";
    public static final String TABLE_DEVICE_LOGS = "device_logs";

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
}
