package com.example.vvgreenhouse.hardware;

import android.util.Log;

import com.example.vvgreenhouse.model.SensorData;

import java.io.ByteArrayOutputStream;
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
 * 真实硬件客户端 — TCP Socket 二进制协议
 *
 * 帧格式:
 *   帧头(2B): 0xAA 0x55
 *   设备地址(1B)
 *   命令类型(1B): 0x01=读传感器 0x02=控制 0x05=心跳
 *   数据长度(1B): N
 *   数据区(N B)
 *   校验和(1B): 帧头~数据区累加和低8位
 *   帧尾(2B): 0x0D 0x0A
 *
 * 响应:
 *   0x81=传感器数据  0x82=控制确认  0x83=错误
 */
public class RealHardwareClient implements IHardwareClient {

    private static final String TAG = "RealHWClient";
    private static final int CONNECT_TIMEOUT = 5000;
    private static final int READ_TIMEOUT = 10000;
    private static final int MAX_RETRY = 3;

    // ====== 帧常量 ======
    private static final byte H1 = (byte) 0xAA;
    private static final byte H2 = (byte) 0x55;
    private static final byte T1 = (byte) 0x0D;
    private static final byte T2 = (byte) 0x0A;
    private static final byte CMD_READ = 0x01;
    private static final byte CMD_CTRL = 0x02;
    private static final byte CMD_HB   = 0x05;
    private static final byte RESP_DATA = (byte) 0x81;
    private static final byte RESP_ACK  = (byte) 0x82;
    private static final byte RESP_ERR  = (byte) 0x83;

    // ====== 大棚ID → IP 映射 ======
    private static final Map<Integer, String> GH_IP = new HashMap<>();
    static {
        GH_IP.put(1, "192.168.1.101");
        GH_IP.put(2, "192.168.1.102");
        GH_IP.put(3, "192.168.1.103");
        GH_IP.put(4, "192.168.1.104");
        GH_IP.put(5, "192.168.1.105");
        GH_IP.put(6, "192.168.1.106");
        GH_IP.put(7, "192.168.1.107");
        GH_IP.put(8, "192.168.1.108");
    }

    // ====== 设备类型 → 硬件地址 ======
    private static final Map<String, Byte> DEV_ADDR = new HashMap<>();
    static {
        DEV_ADDR.put("ventilation_window", (byte) 0x10);
        DEV_ADDR.put("wet_curtain_fan",   (byte) 0x11);
        DEV_ADDR.put("heating_device",    (byte) 0x12);
        DEV_ADDR.put("circulation_fan_temp", (byte) 0x13);
        DEV_ADDR.put("circulation_fan_co2",  (byte) 0x14);
        DEV_ADDR.put("fill_light",        (byte) 0x20);
        DEV_ADDR.put("outer_shade",       (byte) 0x21);
        DEV_ADDR.put("inner_shade",       (byte) 0x22);
        DEV_ADDR.put("co2_generator",     (byte) 0x30);
        DEV_ADDR.put("high_pressure_spray", (byte) 0x40);
        DEV_ADDR.put("dehumidifier",      (byte) 0x41);
    }

    // ====== 连接状态 ======
    private Socket socket;
    private OutputStream out;
    private InputStream in;
    private volatile boolean connected;
    private final Object lock = new Object();

    private String serverIp;
    private int serverPort;

    // ====== 设备状态缓存 (key: "ghId_type") ======
    private final Map<String, Boolean> stateCache = new HashMap<>();

    @Override
    public boolean connect(String ip, int port) {
        this.serverIp = ip;
        this.serverPort = port;
        synchronized (lock) {
            try {
                closeInternal();
                socket = new Socket();
                socket.connect(new InetSocketAddress(ip, port), CONNECT_TIMEOUT);
                socket.setSoTimeout(READ_TIMEOUT);
                socket.setKeepAlive(true);
                out = socket.getOutputStream();
                in = socket.getInputStream();
                connected = true;
                Log.i(TAG, "Connected to " + ip + ":" + port);
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
        synchronized (lock) {
            closeInternal();
            connected = false;
        }
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

    // ========================================================
    // 读取传感器
    // ========================================================

    @Override
    public SensorData readSensors(int greenhouseId) {
        SensorData data = new SensorData();
        data.setGreenhouseId(greenhouseId);
        data.setRecordTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date()));

        long delay = 500;
        for (int retry = 0; retry < MAX_RETRY; retry++) {
            try {
                ensureConnected(greenhouseId);
                byte[] frame = buildFrame((byte) 0x01, CMD_READ, new byte[]{(byte) greenhouseId});
                send(frame);
                byte[] resp = recv(16);  // 8 sensors × 2 bytes each
                if (resp == null) throw new IOException("timeout");

                if (resp[3] == RESP_DATA) {
                    int off = 5;
                    data.setTemp(rU16(resp, off) / 10.0f);         off += 2;
                    data.setHumidity(rU16(resp, off) / 10.0f);     off += 2;
                    data.setCo2(rU16(resp, off));                  off += 2;
                    data.setLight(rU16(resp, off));                off += 2;
                    data.setSoilTemp(rU16(resp, off) / 10.0f);     off += 2;
                    data.setSoilHumidity(rU16(resp, off) / 10.0f); off += 2;
                    data.setPh(rU16(resp, off) / 10.0f);           off += 2;
                    data.setEc(rU16(resp, off) / 10.0f);
                    return data;
                }
                if (resp[3] == RESP_ERR) Log.w(TAG, "Sensor read: device error");
            } catch (IOException e) {
                Log.w(TAG, "Read sensors retry " + (retry + 1) + ": " + e.getMessage());
                disconnect();
                sleep(delay);
                delay *= 2;
            }
        }
        data.setTemp(-999); // marker for failure
        return data;
    }

    // ========================================================
    // 控制设备
    // ========================================================

    @Override
    public boolean controlDevice(int greenhouseId, String deviceType, String action) {
        byte addr = lookupAddr(deviceType);
        byte actCode;
        switch (action) {
            case "open":   actCode = 0x01; break;
            case "close":  actCode = 0x00; break;
            case "toggle":
                boolean cur = getDeviceState(greenhouseId, deviceType);
                actCode = cur ? (byte) 0x00 : (byte) 0x01;
                break;
            default:       actCode = 0x01;
        }

        long delay = 500;
        for (int retry = 0; retry < MAX_RETRY; retry++) {
            try {
                ensureConnected(greenhouseId);
                byte[] frame = buildFrame(addr, CMD_CTRL,
                        new byte[]{(byte) greenhouseId, actCode});
                send(frame);
                byte[] resp = recv(4);
                if (resp == null) throw new IOException("timeout");
                if (resp[3] == RESP_ACK) {
                    boolean newState = actCode != 0x00;
                    stateCache.put(key(greenhouseId, deviceType), newState);
                    return true;
                }
                if (resp[3] == RESP_ERR) { Log.w(TAG, "Device ctrl error"); return false; }
            } catch (IOException e) {
                Log.w(TAG, "Control device retry " + (retry + 1) + ": " + e.getMessage());
                disconnect();
                sleep(delay);
                delay *= 2;
            }
        }
        return false;
    }

    /** 查询设备当前状态（从缓存，初始 false） */
    public boolean getDeviceState(int greenhouseId, String deviceType) {
        return Boolean.TRUE.equals(stateCache.get(key(greenhouseId, deviceType)));
    }

    /** 批量控制 — 单次连接后逐条发送，不做sleep */
    public void controlDevicesBatch(int greenhouseId, String[] deviceTypes, String action) {
        ensureConnected(greenhouseId);
        for (String dt : deviceTypes) {
            controlDevice(greenhouseId, dt, action);
        }
    }

    // ========================================================
    // 心跳（包级可见，供 HeartbeatService 调用）
    // ========================================================

    void sendHeartbeat() throws IOException {
        byte[] hb = new byte[9];
        hb[0] = H1; hb[1] = H2;
        hb[2] = 0x01;
        hb[3] = CMD_HB;
        hb[4] = 0x01;
        hb[5] = 0x00;
        hb[6] = checksum(hb, 0, 6);
        hb[7] = T1; hb[8] = T2;
        send(hb);
    }

    // ========================================================
    // 帧构建
    // ========================================================

    private byte[] buildFrame(byte addr, byte cmd, byte[] data) {
        int total = 7 + data.length;  // H1+H2+addr+cmd+len + data + cksum + T1+T2
        byte[] f = new byte[total];
        int i = 0;
        f[i++] = H1;
        f[i++] = H2;
        f[i++] = addr;
        f[i++] = cmd;
        f[i++] = (byte) data.length;
        System.arraycopy(data, 0, f, i, data.length);
        i += data.length;
        f[i++] = checksum(f, 0, i);
        f[i++] = T1;
        f[i]   = T2;
        return f;
    }

    private byte checksum(byte[] buf, int start, int end) {
        int sum = 0;
        for (int i = start; i < end; i++) sum += (buf[i] & 0xFF);
        return (byte) (sum & 0xFF);
    }

    // ========================================================
    // 发送 / 接收
    // ========================================================

    void send(byte[] frame) throws IOException {
        synchronized (lock) {
            if (out == null) throw new IOException("OutputStream null");
            out.write(frame);
            out.flush();
        }
    }

    /** 接收一帧，返回 payload 部分（从H1到T2的完整帧），minDataLen=期望数据区最小长度 */
    private byte[] recv(int minDataLen) throws IOException {
        synchronized (lock) {
            if (in == null) throw new IOException("InputStream null");
            int b;
            // scan for H1
            while ((b = in.read()) != -1) {
                if ((byte) b != H1) continue;
                // peek H2
                int b2 = in.read();
                if (b2 == -1) return null;
                if ((byte) b2 != H2) continue;

                // read header: addr,cmd,len
                int addr = in.read(), cmd = in.read(), len = in.read();
                if (addr < 0 || cmd < 0 || len < 0) return null;

                // read data + checksum + tail
                int remaining = len + 1 + 2; // data + cksum + T1T2
                byte[] tail = new byte[remaining];
                int rd = 0;
                while (rd < remaining) {
                    int n = in.read(tail, rd, remaining - rd);
                    if (n == -1) return null;
                    rd += n;
                }

                // reconstruct full frame
                int flen = 6 + len + 2; // H1+H2+addr+cmd+len + data + cksum + T1+T2
                byte[] frame = new byte[flen];
                frame[0] = H1; frame[1] = H2;
                frame[2] = (byte) addr; frame[3] = (byte) cmd; frame[4] = (byte) len;
                System.arraycopy(tail, 0, frame, 5, remaining);

                // verify checksum
                int cksIdx = 5 + len;
                if (frame[cksIdx] != checksum(frame, 0, cksIdx)) {
                    Log.w(TAG, "Checksum mismatch");
                    continue;
                }
                // verify tail
                if (frame[flen - 2] != T1 || frame[flen - 1] != T2) {
                    Log.w(TAG, "Tail marker mismatch");
                    continue;
                }
                return frame;
            }
            return null;
        }
    }

    // ========================================================
    // 辅助
    // ========================================================

    private void ensureConnected(int greenhouseId) {
        String ip = GH_IP.containsKey(greenhouseId)
                ? GH_IP.get(greenhouseId) : serverIp;
        int port = serverPort > 0 ? serverPort : 8080;
        if (!isConnected()) {
            connect(ip, port);
        }
    }

    private int rU16(byte[] buf, int off) {
        return ((buf[off] & 0xFF) << 8) | (buf[off + 1] & 0xFF);
    }

    private byte lookupAddr(String deviceType) {
        Byte b = DEV_ADDR.get(deviceType);
        return b != null ? b : (byte) 0xFF;
    }

    private String key(int ghId, String type) { return ghId + "_" + type; }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    // ====== IHardwareClient 补充方法 ======

    @Override
    public void setAbnormalMode(boolean enabled, String type) {
        // Real client doesn't support mock abnormal mode
    }

    @Override
    public String executeAutoControl(int greenhouseId, SensorData data) {
        // Real client: auto-control logic runs on server side
        // Sends a SET_PARAM frame for auto thresholds, server handles the rest
        return "";
    }
}
