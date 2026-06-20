package com.example.vvgreenhouse.hardware;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * 硬件客户端工厂
 *
 * 根据 SharedPreferences 配置决定返回 Mock / Real / Fro 客户端。
 * 默认使用 Mock（模拟模式），用户可在设置界面切换。
 */
public class HardwareClientFactory {

    private static final String KEY_USE_MOCK = "use_mock_hardware";
    private static final String KEY_USE_FRO  = "use_fro_library";
    private static final String KEY_SERVER_IP = "server_ip";
    private static final String KEY_SERVER_PORT = "server_port";
    private static final String DEFAULT_IP = "192.168.1.100";
    private static final int DEFAULT_PORT = 8080;

    public static IHardwareClient create(Context ctx) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        boolean mock = sp.getBoolean(KEY_USE_MOCK, true);

        if (mock) {
            return new MockHardwareClient();
        }

        String ip = sp.getString(KEY_SERVER_IP, DEFAULT_IP);
        int port = sp.getInt(KEY_SERVER_PORT, DEFAULT_PORT);

        boolean fro = sp.getBoolean(KEY_USE_FRO, false);
        if (fro) {
            // FroHardwareClient stub — can be added when JAR is available
            IHardwareClient fallback = new RealHardwareClient();
            fallback.connect(ip, port);
            return fallback;
        }

        IHardwareClient real = new RealHardwareClient();
        real.connect(ip, port);
        return real;
    }

    /** 保存硬件模式 */
    public static void setMockMode(Context ctx, boolean mock, boolean fro) {
        SharedPreferences.Editor ed = PreferenceManager.getDefaultSharedPreferences(ctx).edit();
        ed.putBoolean(KEY_USE_MOCK, mock);
        ed.putBoolean(KEY_USE_FRO, fro);
        ed.apply();
    }

    /** 保存服务器配置 */
    public static void setServerConfig(Context ctx, String ip, int port) {
        SharedPreferences.Editor ed = PreferenceManager.getDefaultSharedPreferences(ctx).edit();
        ed.putString(KEY_SERVER_IP, ip);
        ed.putInt(KEY_SERVER_PORT, port);
        ed.apply();
    }

    public static String getServerIp(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx)
                .getString(KEY_SERVER_IP, DEFAULT_IP);
    }

    public static int getServerPort(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx)
                .getInt(KEY_SERVER_PORT, DEFAULT_PORT);
    }

    public static boolean isMockMode(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx)
                .getBoolean(KEY_USE_MOCK, true);
    }
}
