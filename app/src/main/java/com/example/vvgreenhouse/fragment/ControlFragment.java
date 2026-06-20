package com.example.vvgreenhouse.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.vvgreenhouse.R;
import com.example.vvgreenhouse.database.GreenhouseDBHelper;
import com.example.vvgreenhouse.hardware.IHardwareClient;
import com.example.vvgreenhouse.model.DeviceDef;
import com.example.vvgreenhouse.model.SensorData;
import com.google.android.material.tabs.TabLayout;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 智能控制模块 Fragment
 *
 * 功能：
 * - 温控/光控/气调/湿控 四大系统设备卡片
 * - 手动/自动/定时三种控制模式
 * - ToggleButton 开关设备
 * - 自动模式：定时读取传感器数据联动控制
 */
public class ControlFragment extends Fragment {

    private IHardwareClient hardwareClient;
    private GreenhouseDBHelper dbHelper;
    private Handler mainHandler;

    private TabLayout tabGreenhouse;
    private RadioGroup rgMode;
    private TextView tvAutoStatus;
    private int currentGhId = 1;

    private static final int REFRESH_SEC = 10;  // 自动模式轮询间隔

    // 设备容器
    private LinearLayout containerTemp, containerLight, containerCo2, containerHumidity;

    // 所有设备行视图 (key: deviceType)
    private final Map<String, DeviceRow> deviceRows = new LinkedHashMap<>();

    // 定时器
    private ScheduledExecutorService autoScheduler;

    public static ControlFragment newInstance(IHardwareClient client) {
        ControlFragment f = new ControlFragment();
        f.hardwareClient = client;
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_control, container, false);

        dbHelper = GreenhouseDBHelper.getInstance(getContext());
        mainHandler = new Handler(Looper.getMainLooper());

        tabGreenhouse = view.findViewById(R.id.tab_greenhouse);
        rgMode = view.findViewById(R.id.rg_control_mode);
        tvAutoStatus = view.findViewById(R.id.tv_auto_status);
        containerTemp = view.findViewById(R.id.container_temp_devices);
        containerLight = view.findViewById(R.id.container_light_devices);
        containerCo2 = view.findViewById(R.id.container_co2_devices);
        containerHumidity = view.findViewById(R.id.container_humidity_devices);

        setupTabs();
        setupModeSwitch(view);
        inflateAllDeviceRows(view);
        refreshAllDeviceStates();

        // 操作日志
        view.findViewById(R.id.btn_operation_log).setOnClickListener(v -> {
            OperationLogFragment logFrag = new OperationLogFragment();
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, logFrag)
                    .addToBackStack(null)
                    .commit();
        });

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopAutoControl();
    }

    // ========== Tabs ==========

    private void setupTabs() {
        for (int i = 1; i <= 8; i++) {
            TabLayout.Tab tab = tabGreenhouse.newTab();
            tab.setText("大棚" + i);
            tabGreenhouse.addTab(tab);
        }
        TabLayout.Tab first = tabGreenhouse.getTabAt(0);
        if (first != null) first.select();

        tabGreenhouse.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                currentGhId = tab.getPosition() + 1;
                refreshAllDeviceStates();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    // ========== 控制模式 ==========

    private void setupModeSwitch(View view) {
        rgMode.setOnCheckedChangeListener((group, checkedId) -> {
            stopAutoControl();
            if (checkedId == R.id.rb_auto) {
                tvAutoStatus.setVisibility(View.VISIBLE);
                tvAutoStatus.setText("● 自动运行中 (10s)");
                startAutoControl();
                // 自动模式下禁用手动按钮
                setAllToggleButtonsEnabled(false);
            } else {
                tvAutoStatus.setVisibility(View.GONE);
                setAllToggleButtonsEnabled(true);
            }
        });

        // 紧急停止
        view.findViewById(R.id.btn_emergency_stop).setOnClickListener(v -> {
            new Thread(() -> {
                String[] types = new String[DeviceDef.ALL_DEVICES.length];
                for (int i = 0; i < types.length; i++) types[i] = DeviceDef.ALL_DEVICES[i].type;
                hardwareClient.controlDevicesBatch(currentGhId, types, "close");
                mainHandler.post(() -> {
                    if (!isAdded()) return;
                    refreshAllDeviceStates();
                    writeLog("ALL", "紧急停止", "手动");
                    Toast.makeText(getContext(), "所有设备已停止", Toast.LENGTH_SHORT).show();
                });
            }).start();
        });

        // 一键全开
        view.findViewById(R.id.btn_all_open).setOnClickListener(v -> {
            new Thread(() -> {
                String[] types = new String[DeviceDef.ALL_DEVICES.length];
                for (int i = 0; i < types.length; i++) types[i] = DeviceDef.ALL_DEVICES[i].type;
                hardwareClient.controlDevicesBatch(currentGhId, types, "open");
                mainHandler.post(() -> {
                    if (!isAdded()) return;
                    refreshAllDeviceStates();
                    writeLog("ALL", "一键全开", "手动");
                    Toast.makeText(getContext(), "所有设备已开启", Toast.LENGTH_SHORT).show();
                });
            }).start();
        });
    }

    // ========== 设备行 ==========

    private void inflateAllDeviceRows(View root) {
        for (DeviceDef dd : DeviceDef.ALL_DEVICES) {
            View row = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_device_control, null);
            DeviceRow dr = new DeviceRow();
            dr.icon = row.findViewById(R.id.tv_device_icon);
            dr.name = row.findViewById(R.id.tv_device_name);
            dr.statusDot = row.findViewById(R.id.v_status_dot);
            dr.statusText = row.findViewById(R.id.tv_device_status);
            dr.toggleBtn = row.findViewById(R.id.btn_toggle);

            dr.icon.setText(dd.icon);
            dr.name.setText(dd.name);
            dr.def = dd;

            dr.toggleBtn.setOnClickListener(v -> {
                boolean isOn = hardwareClient.getDeviceState(currentGhId, dd.type);
                String action = isOn ? "close" : "open";
                String actionLabel = isOn ? "关闭" : "开启";
                new Thread(() -> {
                    hardwareClient.controlDevice(currentGhId, dd.type, action);
                    mainHandler.post(() -> {
                        if (!isAdded()) return;
                        updateDeviceRowUI(dr, !isOn);
                        writeLog(dd.type, actionLabel, "手动");
                    });
                }).start();
            });

            // 分配到对应系统容器
            LinearLayout target = null;
            for (String t : DeviceDef.TEMP_DEVICES) {
                if (t.equals(dd.type)) { target = containerTemp; break; }
            }
            if (target == null) {
                for (String t : DeviceDef.LIGHT_DEVICES) {
                    if (t.equals(dd.type)) { target = containerLight; break; }
                }
            }
            if (target == null) {
                for (String t : DeviceDef.CO2_DEVICES) {
                    if (t.equals(dd.type)) { target = containerCo2; break; }
                }
            }
            if (target == null) target = containerHumidity;

            target.addView(row);
            deviceRows.put(dd.type, dr);
        }
    }

    private void refreshAllDeviceStates() {
        for (Map.Entry<String, DeviceRow> e : deviceRows.entrySet()) {
            boolean state = hardwareClient.getDeviceState(currentGhId, e.getKey());
            updateDeviceRowUI(e.getValue(), state);
        }
    }

    private void updateDeviceRowUI(DeviceRow dr, boolean isOn) {
        if (isOn) {
            dr.statusDot.setBackgroundResource(R.drawable.indicator_green);
            dr.statusText.setText("运行中");
            dr.statusText.setTextColor(0xFF388E3C);
            dr.toggleBtn.setText("关闭");
            dr.toggleBtn.setTextColor(0xFFD32F2F);
        } else {
            dr.statusDot.setBackgroundResource(R.drawable.indicator_red);
            dr.statusText.setText("已停止");
            dr.statusText.setTextColor(0xFFD32F2F);
            dr.toggleBtn.setText("开启");
            dr.toggleBtn.setTextColor(0xFF2E7D32);
        }
    }

    private void setAllToggleButtonsEnabled(boolean enabled) {
        for (DeviceRow dr : deviceRows.values()) {
            dr.toggleBtn.setEnabled(enabled);
            dr.toggleBtn.setAlpha(enabled ? 1.0f : 0.5f);
        }
    }

    // ========== 自动控制 ==========

    private void startAutoControl() {
        autoScheduler = Executors.newSingleThreadScheduledExecutor();
        autoScheduler.scheduleWithFixedDelay(() -> {
            SensorData data = hardwareClient.readSensors(currentGhId);
            String log = hardwareClient.executeAutoControl(currentGhId, data);
            mainHandler.post(() -> {
                if (!isAdded()) return;
                refreshAllDeviceStates();
                tvAutoStatus.setText("● 自动运行中 (10s) " + data.getRecordTime());
                writeLog("AUTO", log, "自动");
            });
        }, 0, REFRESH_SEC, TimeUnit.SECONDS);
    }

    private void stopAutoControl() {
        if (autoScheduler != null && !autoScheduler.isShutdown()) {
            autoScheduler.shutdownNow();
        }
    }

    // ========== 日志 ==========

    private void writeLog(String deviceType, String action, String mode) {
        final String name;
        if ("ALL".equals(deviceType) || "AUTO".equals(deviceType)) {
            name = deviceType;
        } else {
            DeviceDef dd = DeviceDef.findByType(deviceType);
            name = dd.name;
        }
        new Thread(() -> {
            dbHelper.saveDeviceLog(currentGhId, deviceType,
                    name, action, mode, "admin");
        }).start();
    }

    // ========== View Holder ==========

    private static class DeviceRow {
        TextView icon, name, statusText;
        View statusDot;
        Button toggleBtn;
        DeviceDef def;
    }
}
