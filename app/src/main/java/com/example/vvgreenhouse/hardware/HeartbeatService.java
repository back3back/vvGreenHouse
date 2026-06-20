package com.example.vvgreenhouse.hardware;

import android.util.Log;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 心跳保活服务
 *
 * 对已连接的 RealHardwareClient 每 30s 发送心跳帧，
 * 维持 TCP 长连接不被中间设备（路由器/NAT）断开。
 */
public class HeartbeatService {

    private static final String TAG = "HeartbeatService";
    private static final long INTERVAL_MS = 30_000;

    private ScheduledExecutorService scheduler;
    private volatile boolean running;

    public void start(RealHardwareClient client) {
        if (client == null) return;
        stop();
        running = true;
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleWithFixedDelay(() -> {
            if (!running || !client.isConnected()) return;
            try {
                client.sendHeartbeat();
            } catch (IOException e) {
                Log.w(TAG, "Heartbeat failed: " + e.getMessage());
            }
        }, INTERVAL_MS, INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        running = false;
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
        scheduler = null;
    }
}
