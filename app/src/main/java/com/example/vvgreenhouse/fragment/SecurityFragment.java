package com.example.vvgreenhouse.fragment;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.vvgreenhouse.R;
import com.example.vvgreenhouse.database.GreenhouseDBHelper;
import com.example.vvgreenhouse.model.AlertRecord;
import com.example.vvgreenhouse.model.AccessLog;
import com.example.vvgreenhouse.model.SensorData;
import com.google.android.material.tabs.TabLayout;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 安防预警模块 Fragment
 *
 * 功能：
 * - 预警统计卡片（今日/本周/历史总数）
 * - TabLayout 切换：预警列表 / 门锁控制 / 监控画面
 * - 预警列表：按类型/等级筛选，点击查看详情
 * - 门锁控制：ToggleButton + 二次确认
 * - 人体感应：PIR实时状态 + 历史记录
 * - 监控画面：WebView + URL 输入
 */
public class SecurityFragment extends Fragment {

    private GreenhouseDBHelper dbHelper;
    private Handler mainHandler;

    // ========== 主布局组件 ==========
    private TextView tvAlertToday, tvAlertWeek, tvAlertTotal;
    private TabLayout tabSecurity;
    private FrameLayout containerSecurity;

    // ========== 子面板 ==========
    private View panelAlertList, panelDoorLock, panelMonitoring;

    // ========== 预警列表面板 ==========
    private RecyclerView rvAlertList;
    private Spinner spFilterType, spFilterLevel;
    private TextView tvAlertEmpty;

    // ========== 门锁面板 ==========
    private TabLayout tabLockGh;
    private TextView tvDoorStatus;
    private ToggleButton toggleDoor;
    private View indicatorPir;
    private TextView tvPirStatus, tvPirLastTime;
    private LinearLayout containerPirLogs;

    // ========== 监控面板 ==========
    private WebView webViewMonitor;
    private EditText etVideoUrl;
    private TextView tvVideoHint;

    // ========== PIR 定时器 ==========
    private ScheduledExecutorService pirScheduler;
    private int currentGhId = 1;
    private boolean pirMotion = false;  // 模拟PIR状态

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHelper = GreenhouseDBHelper.getInstance(getContext());
        mainHandler = new Handler(Looper.getMainLooper());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_security, container, false);

        initMainViews(view);
        inflateSubPanels();
        setupTabs();
        refreshStats();
        startPirSimulation();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopPirSimulation();
    }

    // ========== 主视图初始化 ==========

    private void initMainViews(View view) {
        tvAlertToday = view.findViewById(R.id.tv_alert_today);
        tvAlertWeek = view.findViewById(R.id.tv_alert_week);
        tvAlertTotal = view.findViewById(R.id.tv_alert_total);
        tabSecurity = view.findViewById(R.id.tab_security);
        containerSecurity = view.findViewById(R.id.container_security);
    }

    private void layoutStatsVisible(boolean visible) {
        View statsLayout = getView() != null ? getView().findViewById(R.id.layout_stats) : null;
        if (statsLayout != null) statsLayout.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    // ========== 子面板初始化 ==========

    private void inflateSubPanels() {
        LayoutInflater inflater = LayoutInflater.from(getContext());

        // ---- 预警列表面板 ----
        panelAlertList = inflater.inflate(R.layout.panel_alert_list, containerSecurity, false);
        rvAlertList = panelAlertList.findViewById(R.id.rv_alert_list);
        spFilterType = panelAlertList.findViewById(R.id.sp_filter_type);
        spFilterLevel = panelAlertList.findViewById(R.id.sp_filter_level);
        tvAlertEmpty = panelAlertList.findViewById(R.id.tv_alert_empty);

        rvAlertList.setLayoutManager(new LinearLayoutManager(getContext()));

        // 类型筛选 Spinner
        String[] typeItems = {"全部类型", "空气温度", "空气湿度", "CO₂浓度", "光照强度",
                "土壤温度", "土壤湿度", "土壤pH值", "土壤EC值"};
        final String[] typeValues = {"", "temp", "humidity", "co2", "light",
                "soil_temp", "soil_humidity", "ph", "ec"};
        spFilterType.setAdapter(new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item, typeItems));

        // 级别筛选 Spinner
        String[] levelItems = {"全部级别", "轻度", "中度", "重度"};
        final int[] levelValues = {0, 1, 2, 3};
        spFilterLevel.setAdapter(new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item, levelItems));

        panelAlertList.findViewById(R.id.btn_filter).setOnClickListener(v -> {
            int typeIdx = spFilterType.getSelectedItemPosition();
            int levelIdx = spFilterLevel.getSelectedItemPosition();
            String sensorType = typeIdx >= 0 && typeIdx < typeValues.length ? typeValues[typeIdx] : "";
            int level = levelIdx >= 0 && levelIdx < levelValues.length ? levelValues[levelIdx] : 0;
            loadAlertList(sensorType, level);
        });

        containerSecurity.addView(panelAlertList);

        // ---- 门锁面板 ----
        panelDoorLock = inflater.inflate(R.layout.panel_door_lock, containerSecurity, false);
        tabLockGh = panelDoorLock.findViewById(R.id.tab_lock_gh);
        tvDoorStatus = panelDoorLock.findViewById(R.id.tv_door_status);
        toggleDoor = panelDoorLock.findViewById(R.id.toggle_door);
        indicatorPir = panelDoorLock.findViewById(R.id.indicator_pir);
        tvPirStatus = panelDoorLock.findViewById(R.id.tv_pir_status);
        tvPirLastTime = panelDoorLock.findViewById(R.id.tv_pir_last_time);
        containerPirLogs = panelDoorLock.findViewById(R.id.container_pir_logs);

        setupLockGhTabs();
        setupDoorToggle();
        containerSecurity.addView(panelDoorLock);

        // ---- 监控面板 ----
        panelMonitoring = inflater.inflate(R.layout.panel_monitoring, containerSecurity, false);
        webViewMonitor = panelMonitoring.findViewById(R.id.webview_monitor);
        etVideoUrl = panelMonitoring.findViewById(R.id.et_video_url);
        tvVideoHint = panelMonitoring.findViewById(R.id.tv_video_hint);

        // WebView 设置
        WebSettings ws = webViewMonitor.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setLoadWithOverviewMode(true);
        ws.setUseWideViewPort(true);
        ws.setBuiltInZoomControls(true);
        webViewMonitor.setWebViewClient(new WebViewClient());
        webViewMonitor.setVisibility(View.GONE);

        panelMonitoring.findViewById(R.id.btn_load_video).setOnClickListener(v -> {
            String url = etVideoUrl.getText().toString().trim();
            if (url.isEmpty()) {
                Toast.makeText(getContext(), "请输入视频流URL", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "http://" + url;
            }
            webViewMonitor.setVisibility(View.VISIBLE);
            tvVideoHint.setVisibility(View.GONE);
            webViewMonitor.loadUrl(url);
            Toast.makeText(getContext(), "正在加载: " + url, Toast.LENGTH_SHORT).show();
        });

        containerSecurity.addView(panelMonitoring);

        // 默认显示预警列表
        showPanel(panelAlertList);
    }

    // ========== 主 TabLayout ==========

    private void setupTabs() {
        tabSecurity.addTab(tabSecurity.newTab().setText("预警列表"));
        tabSecurity.addTab(tabSecurity.newTab().setText("门锁控制"));
        tabSecurity.addTab(tabSecurity.newTab().setText("监控画面"));

        tabSecurity.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                int pos = tab.getPosition();
                switch (pos) {
                    case 0:
                        showPanel(panelAlertList);
                        layoutStatsVisible(true);
                        loadAlertList("", 0);
                        refreshStats();
                        break;
                    case 1:
                        showPanel(panelDoorLock);
                        layoutStatsVisible(false);
                        break;
                    case 2:
                        showPanel(panelMonitoring);
                        layoutStatsVisible(false);
                        break;
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void showPanel(View panel) {
        if (!isAdded()) return;
        for (int i = 0; i < containerSecurity.getChildCount(); i++) {
            containerSecurity.getChildAt(i).setVisibility(View.GONE);
        }
        panel.setVisibility(View.VISIBLE);
    }

    // ========== 预警统计 ==========

    private void refreshStats() {
        new Thread(() -> {
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(new Date());
            // Count today
            List<AlertRecord> all = dbHelper.getAlertRecords(0, null, 0, 9999);
            int todayCount = 0, weekCount = 0;
            Calendar calWeek = Calendar.getInstance();
            calWeek.add(Calendar.DAY_OF_MONTH, -7);
            String weekStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(calWeek.getTime());
            for (AlertRecord a : all) {
                String rt = a.getRecordTime();
                if (rt != null) {
                    if (rt.startsWith(today)) todayCount++;
                    if (rt.compareTo(weekStr) >= 0) weekCount++;
                }
            }
            final int t = todayCount;
            final int w = weekCount;
            final int total = all.size();
            mainHandler.post(() -> {
                if (!isAdded()) return;
                tvAlertToday.setText(String.valueOf(t));
                tvAlertWeek.setText(String.valueOf(w));
                tvAlertTotal.setText(String.valueOf(total));
            });
        }).start();
    }

    // ========== 预警列表 ==========

    private void loadAlertList(String sensorType, int level) {
        if (!isAdded()) return;
        new Thread(() -> {
            List<AlertRecord> alerts = dbHelper.getAlertRecords(0, sensorType, level, 500);
            mainHandler.post(() -> {
                if (!isAdded()) return;
                if (alerts.isEmpty()) {
                    rvAlertList.setVisibility(View.GONE);
                    tvAlertEmpty.setVisibility(View.VISIBLE);
                } else {
                    tvAlertEmpty.setVisibility(View.GONE);
                    rvAlertList.setVisibility(View.VISIBLE);
                    rvAlertList.setAdapter(new AlertListAdapter(alerts));
                }
            });
        }).start();
    }

    // ========== 预警列表 Adapter ==========

    private class AlertListAdapter extends RecyclerView.Adapter<AlertListAdapter.VH> {
        private final List<AlertRecord> items;

        AlertListAdapter(List<AlertRecord> items) { this.items = items; }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_alert_record, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            AlertRecord a = items.get(pos);
            h.sensorName.setText(a.getSensorName());
            h.value.setText(formatAlertValue(a.getValue(), a.getSensorType())
                    + SensorData.getUnit(a.getSensorType()));
            h.range.setText("(范围: " + formatAlertValue(a.getThresholdMin(), a.getSensorType())
                    + "~" + formatAlertValue(a.getThresholdMax(), a.getSensorType())
                    + SensorData.getUnit(a.getSensorType()) + ")");
            h.time.setText(a.getRecordTime());
            h.levelBar.setBackgroundColor(AlertRecord.levelToColor(a.getLevel()));
            h.level.setText(a.getLevelName());
            int levelBg = a.getLevel() >= 3 ? 0xFFD32F2F :
                    a.getLevel() >= 2 ? 0xFFFF9800 : 0xFFF57C00;
            h.level.setBackgroundColor(levelBg);
            h.level.setTextColor(0xFFFFFFFF);
            if (a.getStatus() == 0) {
                h.status.setText("未处理");
                h.status.setTextColor(0xFFD32F2F);
            } else {
                h.status.setText("已处理");
                h.status.setTextColor(0xFF388E3C);
            }
            h.itemView.setOnClickListener(v -> showAlertDetail(a));
        }

        @Override public int getItemCount() { return items.size(); }

        class VH extends RecyclerView.ViewHolder {
            View levelBar;
            TextView sensorName, level, value, range, time, status;
            VH(@NonNull View v) {
                super(v);
                levelBar = v.findViewById(R.id.v_level_bar);
                sensorName = v.findViewById(R.id.tv_alert_sensor_name);
                level = v.findViewById(R.id.tv_alert_level);
                value = v.findViewById(R.id.tv_alert_value);
                range = v.findViewById(R.id.tv_alert_range);
                time = v.findViewById(R.id.tv_alert_time);
                status = v.findViewById(R.id.tv_alert_status);
            }
        }
    }

    private String formatAlertValue(float val, String type) {
        if ("light".equals(type)) return String.format("%.0f", val);
        else if ("ph".equals(type) || "ec".equals(type)) return String.format("%.2f", val);
        else return String.format("%.1f", val);
    }

    // ========== 预警详情弹窗 ==========

    private void showAlertDetail(AlertRecord alert) {
        View detailView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_alert_detail, null);

        TextView tvIcon = detailView.findViewById(R.id.tv_detail_icon);
        TextView tvSensorName = detailView.findViewById(R.id.tv_detail_sensor_name);
        TextView tvLevel = detailView.findViewById(R.id.tv_detail_level);
        TextView tvGh = detailView.findViewById(R.id.tv_detail_gh);
        TextView tvValue = detailView.findViewById(R.id.tv_detail_value);
        TextView tvRange = detailView.findViewById(R.id.tv_detail_range);
        TextView tvDirection = detailView.findViewById(R.id.tv_detail_direction);
        TextView tvTime = detailView.findViewById(R.id.tv_detail_time);

        tvIcon.setText(SensorData.getIcon(alert.getSensorType()));
        tvSensorName.setText(alert.getSensorName());
        tvLevel.setText(alert.getLevelName() + "预警");
        tvLevel.setTextColor(AlertRecord.levelToColor(alert.getLevel()));
        tvGh.setText("GH-" + String.format("%03d", alert.getGreenhouseId()));
        tvValue.setText(formatAlertValue(alert.getValue(), alert.getSensorType())
                + " " + SensorData.getUnit(alert.getSensorType()));
        tvRange.setText(formatAlertValue(alert.getThresholdMin(), alert.getSensorType())
                + " ~ " + formatAlertValue(alert.getThresholdMax(), alert.getSensorType())
                + " " + SensorData.getUnit(alert.getSensorType()));
        tvDirection.setText(alert.getDirection());

        int dirColor = alert.isHigh() ? 0xFFD32F2F : 0xFF1976D2;
        tvDirection.setTextColor(dirColor);
        tvTime.setText(alert.getRecordTime());

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
                .setView(detailView)
                .setPositiveButton("标记已处理", (dialog, which) -> {
                    new Thread(() -> {
                        dbHelper.markAlertHandled(alert.getId(), "admin");
                        mainHandler.post(() -> {
                            Toast.makeText(getContext(), "已标记为已处理", Toast.LENGTH_SHORT).show();
                            refreshStats();
                            loadAlertList("", 0);
                        });
                    }).start();
                })
                .setNegativeButton("关闭", null);

        if (alert.getStatus() == 1) {
            builder.setPositiveButton(null, null);  // 已处理的不能再标记
            builder.setNeutralButton("已处理 ✓", null);
        }

        builder.show();
    }

    // ========== 门锁面板 ==========

    private void setupLockGhTabs() {
        for (int i = 1; i <= 8; i++) {
            TabLayout.Tab tab = tabLockGh.newTab();
            tab.setText("大棚" + i);
            tabLockGh.addTab(tab);
        }
        TabLayout.Tab first = tabLockGh.getTabAt(0);
        if (first != null) first.select();

        tabLockGh.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                currentGhId = tab.getPosition() + 1;
                refreshDoorLockUI();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupDoorToggle() {
        toggleDoor.setOnCheckedChangeListener(null);
        toggleDoor.setChecked(false);
        tvDoorStatus.setText("已锁定");
        tvDoorStatus.setTextColor(0xFF388E3C);

        toggleDoor.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // 二次确认
            String action = isChecked ? "解锁" : "锁定";
            String eventType = isChecked ? "door_open" : "door_close";
            new AlertDialog.Builder(getContext())
                    .setTitle("确认操作")
                    .setMessage("确定要" + action + " GH-" + String.format("%03d", currentGhId) + " 的门锁吗？")
                    .setPositiveButton("确定", (dialog, which) -> {
                        new Thread(() -> {
                            AccessLog log = new AccessLog();
                            log.setGreenhouseId(currentGhId);
                            log.setEventType(eventType);
                            log.setEventName(AccessLog.typeToName(eventType));
                            log.setOperator("admin");
                            log.setRemarks(action + "操作");
                            dbHelper.saveAccessLog(log);
                            mainHandler.post(() -> {
                                if (!isAdded()) return;
                                tvDoorStatus.setText(isChecked ? "已解锁" : "已锁定");
                                tvDoorStatus.setTextColor(isChecked ? 0xFFD32F2F : 0xFF388E3C);
                                Toast.makeText(getContext(), "门锁已" + action, Toast.LENGTH_SHORT).show();
                            });
                        }).start();
                    })
                    .setNegativeButton("取消", (dialog, which) -> {
                        // 恢复状态
                        toggleDoor.setOnCheckedChangeListener(null);
                        toggleDoor.setChecked(!isChecked);
                        setupDoorToggle();
                    })
                    .show();
        });
    }

    private void refreshDoorLockUI() {
        if (!isAdded()) return;
        toggleDoor.setOnCheckedChangeListener(null);
        toggleDoor.setChecked(false);
        tvDoorStatus.setText("已锁定");
        tvDoorStatus.setTextColor(0xFF388E3C);
        setupDoorToggle();
        refreshPirLogs();
    }

    // ========== PIR 人体感应模拟 ==========

    private void startPirSimulation() {
        pirScheduler = Executors.newSingleThreadScheduledExecutor();
        pirScheduler.scheduleWithFixedDelay(() -> {
            // 模拟 PIR：随机切换（实际应读硬件）
            pirMotion = Math.random() < 0.3;  // 30%概率检测到人
            String eventType = pirMotion ? "pir_motion" : "pir_clear";
            AccessLog log = new AccessLog();
            log.setGreenhouseId(currentGhId);
            log.setEventType(eventType);
            log.setEventName(AccessLog.typeToName(eventType));
            log.setOperator("系统");
            log.setRemarks(pirMotion ? "PIR检测到人体活动" : "PIR无人体活动");
            dbHelper.saveAccessLog(log);

            mainHandler.post(() -> {
                if (!isAdded()) return;
                updatePirUI();
                refreshPirLogs();
            });
        }, 0, 10, TimeUnit.SECONDS);
    }

    private void stopPirSimulation() {
        if (pirScheduler != null && !pirScheduler.isShutdown()) {
            pirScheduler.shutdownNow();
        }
    }

    private void updatePirUI() {
        if (pirMotion) {
            indicatorPir.setBackgroundResource(R.drawable.indicator_red);
            tvPirStatus.setText("有人");
            tvPirStatus.setTextColor(0xFFD32F2F);
        } else {
            indicatorPir.setBackgroundResource(R.drawable.indicator_green);
            tvPirStatus.setText("无人");
            tvPirStatus.setTextColor(0xFF388E3C);
        }
        tvPirLastTime.setText("最近检测: " + new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
    }

    private void refreshPirLogs() {
        new Thread(() -> {
            List<AccessLog> logs = dbHelper.getAccessLogs(currentGhId, 10);
            mainHandler.post(() -> {
                if (!isAdded()) return;
                containerPirLogs.removeAllViews();
                for (AccessLog log : logs) {
                    TextView tv = new TextView(getContext());
                    String icon = log.getEventType().contains("motion") ? "🚶" : "🚪";
                    int color = log.getEventType().contains("motion") ? 0xFFF57C00 : 0xFF388E3C;
                    tv.setText(icon + " " + log.getRecordTime() + "  " + log.getEventName()
                            + " (" + log.getOperator() + ")");
                    tv.setTextColor(color);
                    tv.setTextSize(12f);
                    tv.setPadding(0, 4, 0, 4);
                    containerPirLogs.addView(tv);
                }
            });
        }).start();
    }
}
