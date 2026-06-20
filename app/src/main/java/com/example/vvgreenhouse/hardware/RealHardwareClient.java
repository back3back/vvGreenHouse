package com.example.vvgreenhouse.hardware;

import android.util.Log;

import com.example.vvgreenhouse.model.SensorData;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 真实硬件客户端 — MODBUS-RTU over TCP
 *
 * 基于 com.fro.util 工具包的协议分析：
 * - 端口 4001
 * - hex-string 命令（如 "01 03 00 14 00 02 XX XX"）
 * - CRC16 校验
 * - 传感器值 = uint16大端 / 10
 * - 设备控制 = "01 10 ..." 写保持寄存器命令
 */
public class RealHardwareClient implements IHardwareClient {

    private static final String TAG = "RealHWClient";
    private static final int CONNECT_TIMEOUT = 3000;
    private static final int READ_TIMEOUT = 5000;
    private static final int MAX_RETRY = 3;
    private static final int PORT = 4001;  // MODBUS TCP port

    private Socket socket;
    private OutputStream out;
    private InputStream in;
    private volatile boolean connected;
    private final Object lock = new Object();

    private String serverIp;

    private final Map<String, Boolean> stateCache = new HashMap<>();

    @Override
    public boolean connect(String ip, int port) {
        this.serverIp = ip;
        synchronized (lock) {
            try {
                closeInternal();
                socket = new Socket();
                int p = port > 0 ? port : PORT;
                socket.connect(new InetSocketAddress(ip, p), CONNECT_TIMEOUT);
                socket.setSoTimeout(READ_TIMEOUT);
                socket.setKeepAlive(true);
                out = socket.getOutputStream();
                in = socket.getInputStream();
                connected = true;
                Log.i(TAG, "MODBUS connected to " + ip + ":" + p);
                return true;
            } catch (IOException e) {
                Log.e(TAG, "Connect failed: " + e.getMessage());
                connected = false;
                return false;
            }
        }
    }

    @Override
    public void disconnect() {
        synchronized (lock) { closeInternal(); connected = false; }
    }

    private void closeInternal() {
        try { if (in != null) in.close(); } catch (IOException ignored) {}
        try { if (out != null) out.close(); } catch (IOException ignored) {}
        try { if (socket != null && !socket.isClosed()) socket.close(); } catch (IOException ignored) {}
        in = null; out = null; socket = null;
    }

    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }

    // ============================================================
    // 读取传感器 —— 分4次查询: 温湿度 + 光照 + 土壤 + CO₂
    // ============================================================

    @Override
    public SensorData readSensors(int greenhouseId) {
        SensorData d = new SensorData();
        d.setGreenhouseId(greenhouseId);
        d.setRecordTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));

        long delay = 400;
        for (int retry = 0; retry < MAX_RETRY; retry++) {
            try {
                ensureConnected(greenhouseId);

                // 1. 温湿度
                byte[] resp = sendRecv(FroConstants.TEMHUM_CMD_PREFIX, FroConstants.TEMHUM_LEN);
                if (resp != null && resp.length >= FroConstants.TEMHUM_LEN) {
                    d.setTemp(FroConstants.readU16Div10(resp, 3));
                    d.setHumidity(FroConstants.readU16Div10(resp, 5));
                }
                // 2. 光照
                resp = sendRecv(FroConstants.LIGHT_CMD_PREFIX, FroConstants.LIGHT_LEN);
                if (resp != null && resp.length >= FroConstants.LIGHT_LEN) {
                    d.setLight(FroConstants.readU16(resp, 3));
                }
                // 3. 土壤 (soilTemp / soilHum / pH / EC)
                resp = sendRecv(FroConstants.SOIL_CMD_PREFIX, FroConstants.SOIL_LEN);
                if (resp != null && resp.length >= FroConstants.SOIL_LEN) {
                    d.setSoilTemp(FroConstants.readU16Div10(resp, 3));
                    d.setSoilHumidity(FroConstants.readU16Div10(resp, 5));
                    d.setPh(FroConstants.readU16Div10(resp, 7));
                    d.setEc(FroConstants.readU16Div10(resp, 9));
                }
                // 4. CO₂
                resp = sendRecv(FroConstants.CO2_CMD_PREFIX, FroConstants.CO2_LEN);
                if (resp != null && resp.length >= FroConstants.CO2_LEN) {
                    d.setCo2(FroConstants.readU16(resp, 3));
                }
                return d;
            } catch (IOException e) {
                Log.w(TAG, "Read retry " + (retry + 1) + ": " + e.getMessage());
                disconnect();
                sleep(delay); delay *= 2;
            }
        }
        d.setTemp(-999); // marker: all retries failed
        return d;
    }

    // ============================================================
    // 控制设备
    // ============================================================

    @Override
    public boolean controlDevice(int greenhouseId, String deviceType, String action) {
        boolean on = "open".equals(action);
        String cmd = null;
        switch (deviceType) {
            case "ventilation_window":   cmd = on ? FroConstants.VENTILATION_ON  : FroConstants.VENTILATION_OFF;  break;
            case "wet_curtain_fan":
            case "circulation_fan_temp":
            case "circulation_fan_co2":   cmd = on ? FroConstants.CURTAIN_ON     : FroConstants.CURTAIN_OFF;     break;
            case "fill_light":            cmd = on ? FroConstants.LIGHT_ON       : FroConstants.LIGHT_OFF;       break;
            case "outer_shade":
            case "inner_shade":           cmd = on ? FroConstants.CURTAIN_ON     : FroConstants.CURTAIN_OFF;     break;
            case "co2_generator":         cmd = on ? FroConstants.CO2_SUPPLY_ON  : FroConstants.CO2_SUPPLY_OFF;  break;
            case "high_pressure_spray":   cmd = on ? FroConstants.SPRAY_ON       : FroConstants.SPRAY_OFF;       break;
            case "dehumidifier":          cmd = on ? FroConstants.SPRAY_OFF      : FroConstants.SPRAY_ON;        break;
            case "heating_device":        cmd = on ? FroConstants.CURTAIN_ON     : FroConstants.CURTAIN_OFF;     break; // 复用
            default: cmd = on ? FroConstants.CURTAIN_ON : FroConstants.CURTAIN_OFF;
        }
        if (cmd == null) return false;

        long delay = 300;
        for (int retry = 0; retry < MAX_RETRY; retry++) {
            try {
                ensureConnected(greenhouseId);
                byte[] tx = FroConstants.hexToBytes(cmd);
                send(tx);
                byte[] rx = recv(FroConstants.CTRL_LEN);
                boolean ok = rx != null && rx.length >= FroConstants.CTRL_LEN
                        && rx[0] == FroConstants.CTRL_NODE;
                if (ok) {
                    boolean newState = on;
                    stateCache.put(key(greenhouseId, deviceType), newState);
                    return true;
                }
            } catch (IOException e) {
                Log.w(TAG, "Ctrl retry " + (retry + 1) + ": " + e.getMessage());
                disconnect();
                sleep(delay); delay *= 2;
            }
        }
        return false;
    }

    @Override
    public boolean getDeviceState(int greenhouseId, String deviceType) {
        return Boolean.TRUE.equals(stateCache.get(key(greenhouseId, deviceType)));
    }

    @Override
    public void controlDevicesBatch(int greenhouseId, String[] deviceTypes, String action) {
        ensureConnected(greenhouseId);
        for (String dt : deviceTypes) {
            controlDevice(greenhouseId, dt, action);
        }
    }

    // ============================================================
    // 人体感应 (PIR) — public 给 SecurityFragment 用
    // ============================================================

    public Boolean readPir(int greenhouseId) {
        long delay = 300;
        for (int retry = 0; retry < MAX_RETRY; retry++) {
            try {
                ensureConnected(greenhouseId);
                byte[] resp = sendRecv(FroConstants.BODY_CHK, FroConstants.BODY_LEN);
                if (resp != null && resp.length >= FroConstants.BODY_LEN) {
                    // FROBody.getData logic: dataOffset=4, byte[1]
                    int raw = (resp[4] & 0xFF);
                    return raw != 0;
                }
            } catch (IOException e) {
                Log.w(TAG, "PIR retry " + (retry + 1) + ": " + e.getMessage());
                disconnect();
                sleep(delay); delay *= 2;
            }
        }
        return null; // all retries failed
    }

    // ============================================================
    // IHardwareClient 补充
    // ============================================================

    @Override
    public void setAbnormalMode(boolean enabled, String type) {
        // Real client: no mock mode
    }

    @Override
    public String executeAutoControl(int greenhouseId, SensorData data) {
        // Auto logic runs server-side or via ControlFragment threshold
        return "";
    }

    // ============================================================
    // 发送 / 接收 (MODBUS hex-string)
    // ============================================================

    private byte[] sendRecv(String cmdPrefix, int expectedLen) throws IOException {
        String fullCmd = FroConstants.buildCmdWithCRC(cmdPrefix);
        byte[] tx = FroConstants.hexToBytes(fullCmd);
        send(tx);
        return recv(expectedLen);
    }

    void send(byte[] frame) throws IOException {
        synchronized (lock) {
            if (out == null) throw new IOException("OutputStream null");
            out.write(frame);
            out.flush();
        }
    }

    private byte[] recv(int minLen) throws IOException {
        synchronized (lock) {
            if (in == null) throw new IOException("InputStream null");
            // wait for buffer to fill
            long deadline = System.currentTimeMillis() + READ_TIMEOUT;
            while (System.currentTimeMillis() < deadline) {
                int avail = in.available();
                if (avail >= minLen) {
                    byte[] buf = new byte[avail];
                    int n = in.read(buf);
                    if (n >= minLen) {
                        Log.d(TAG, "RECV[" + n + "]: " + bytesToHex(buf, n));
                        return buf;
                    }
                }
                sleep(50);
            }
            return null;
        }
    }

    // ============================================================
    // 辅助
    // ============================================================

    private void ensureConnected(int greenhouseId) {
        String ip = FroConstants.getIpForGreenhouse(greenhouseId);
        if (!isConnected()) {
            // 如果传入了自定义IP，用自定义的；否则用大棚映射IP
            String useIp = (serverIp != null && !serverIp.isEmpty() && !serverIp.equals("192.168.1.100"))
                    ? serverIp : ip;
            connect(useIp, PORT);
        }
    }

    private String key(int ghId, String type) { return ghId + "_" + type; }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    private String bytesToHex(byte[] b, int len) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(len, b.length); i++)
            sb.append(String.format("%02X ", b[i]));
        return sb.toString();
    }
}
