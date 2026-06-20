package com.example.vvgreenhouse.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.example.vvgreenhouse.R;
import com.example.vvgreenhouse.database.GreenhouseDBHelper;
import com.example.vvgreenhouse.hardware.MockHardwareClient;
import com.example.vvgreenhouse.model.SensorData;
import com.example.vvgreenhouse.model.ThresholdConfig;
import com.google.android.material.tabs.TabLayout;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 环境监测模块 — Phase 2 完整版
 *
 * 功能：
 * - TabLayout 8个大棚切换
 * - 2列 8卡片网格展示传感器数据（正常绿/超标红）
 * - ScheduledExecutorService 每5秒轮询
 * - 历史查询 + 阈值设置 子面板
 */
public class EnvironmentFragment extends Fragment {

    // ========== 核心组件 ==========
    private MockHardwareClient hardwareClient;
    private GreenhouseDBHelper dbHelper;
    private ThresholdConfig thresholdConfig;
    private Handler mainHandler;
    private ScheduledExecutorService scheduler;

    // ========== UI 组件 ==========
    private TextView tvTitle, tvSubtitle, tvLastUpdate;
    private TabLayout tabGreenhouse;
    private GridLayout gridCards;
    private FrameLayout panelMonitor, panelHistory;

    // ========== 8张传感器卡片子视图 ==========
    // 顺序: temp, humidity, co2, light, soil_temp, soil_humidity, ph, ec
    private final View[] cardViews = new View[8];
    private final TextView[] cardIcons = new TextView[8];
    private final TextView[] cardNames = new TextView[8];
    private final TextView[] cardValues = new TextView[8];
    private final TextView[] cardUnits = new TextView[8];
    private final ImageView[] cardIndicators = new ImageView[8];
    private final TextView[] cardStatuses = new TextView[8];

    // ========== 状态 ==========
    private int currentGhId = 1;  // 1~8
    private static final int REFRESH_INTERVAL_SEC = 5;

    // ========== 工厂方法 ==========

    public static EnvironmentFragment newInstance(MockHardwareClient client) {
        EnvironmentFragment f = new EnvironmentFragment();
        f.hardwareClient = client;
        return f;
    }

    // ========== 生命周期 ==========

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_environment_v2, container, false);

        dbHelper = GreenhouseDBHelper.getInstance(getContext());
        thresholdConfig = ThresholdConfig.load(getContext());
        mainHandler = new Handler(Looper.getMainLooper());

        initViews(view);
        setupTabs();
        setupButtons(view);
        inflateSensorCards();
        startAutoRefresh();
        refreshData();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopAutoRefresh();
    }

    // ========== 初始化 ==========

    private void initViews(View view) {
        tvTitle = view.findViewById(R.id.tv_title);
        tvSubtitle = view.findViewById(R.id.tv_subtitle);
        tvLastUpdate = view.findViewById(R.id.tv_last_update);
        tabGreenhouse = view.findViewById(R.id.tab_greenhouse);
        gridCards = view.findViewById(R.id.grid_cards);
        panelMonitor = view.findViewById(R.id.panel_monitor);
        panelHistory = view.findViewById(R.id.panel_history);
    }

    private void setupTabs() {
        for (int i = 1; i <= 8; i++) {
            TabLayout.Tab tab = tabGreenhouse.newTab();
            tab.setText("大棚" + i);
            tabGreenhouse.addTab(tab);
        }
        // 默认选中大棚1
        TabLayout.Tab firstTab = tabGreenhouse.getTabAt(0);
        if (firstTab != null) firstTab.select();

        tabGreenhouse.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                currentGhId = tab.getPosition() + 1;
                updateTitle();
                refreshData();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupButtons(View view) {
        // 立即刷新
        view.findViewById(R.id.btn_refresh).setOnClickListener(v -> refreshData());

        // 异常模式下拉
        Button btnAbnormal = view.findViewById(R.id.btn_abnormal);
        btnAbnormal.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(getContext(), v, Gravity.BOTTOM);
            popup.getMenu().add("高温告警 (35~40°C)");
            popup.getMenu().add("低湿告警 (20~35%)");
            popup.getMenu().add("高CO₂告警 (1200~1500ppm)");
            popup.getMenu().add("全指标异常");
            popup.getMenu().add("关闭异常模式");
            popup.setOnMenuItemClickListener(item -> {
                String title = item.getTitle().toString();
                if (title.startsWith("关闭")) {
                    hardwareClient.setAbnormalMode(false, "");
                    tvSubtitle.setText("模拟数据每5秒自动刷新");
                    btnAbnormal.setText("异常模式 ▾");
                    btnAbnormal.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(0xFFF57C00));
                } else {
                    String type;
                    if (title.startsWith("高温")) type = "high_temp";
                    else if (title.startsWith("低湿")) type = "low_humidity";
                    else if (title.startsWith("高CO₂")) type = "high_co2";
                    else type = "";
                    hardwareClient.setAbnormalMode(true, type);
                    tvSubtitle.setText("⚠ 异常模式: " + title);
                    btnAbnormal.setText("异常模式 ▾ (开)");
                    btnAbnormal.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(0xFFD32F2F));
                }
                refreshData();
                return true;
            });
            popup.show();
        });

        // 历史数据
        view.findViewById(R.id.btn_history).setOnClickListener(v -> openHistoryPanel());

        // 阈值设置
        view.findViewById(R.id.btn_threshold).setOnClickListener(v -> {
            ThresholdDialogFragment dialog = ThresholdDialogFragment.newInstance(thresholdConfig);
            dialog.setOnThresholdSavedListener(cfg -> {
                thresholdConfig = cfg;
                // 刷新卡片状态
                if (gridCards.getChildCount() > 0) {
                    refreshData();
                }
            });
            dialog.show(getParentFragmentManager(), "threshold_dialog");
        });
    }

    /** 动态创建 8张传感器卡片 */
    private void inflateSensorCards() {
        for (int i = 0; i < SensorData.SENSOR_TYPES.length; i++) {
            View card = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_sensor_card, gridCards, false);

            cardViews[i] = card;
            cardIcons[i] = card.findViewById(R.id.tv_card_icon);
            cardNames[i] = card.findViewById(R.id.tv_card_name);
            cardValues[i] = card.findViewById(R.id.tv_card_value);
            cardUnits[i] = card.findViewById(R.id.tv_card_unit);
            cardIndicators[i] = card.findViewById(R.id.iv_card_indicator);
            cardStatuses[i] = card.findViewById(R.id.tv_card_status);

            // 卡片点击 → Toast 详情
            final int idx = i;
            card.setOnClickListener(v -> {
                // 可以后续扩展为弹窗详细数据
            });

            // 设置静态内容
            String type = SensorData.SENSOR_TYPES[i];
            cardIcons[i].setText(SensorData.getIcon(type));
            cardNames[i].setText(SensorData.getChineseName(type));
            cardUnits[i].setText(SensorData.getUnit(type));

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(i % 2, 1f);
            params.rowSpec = GridLayout.spec(i / 2);
            params.setMargins(4, 4, 4, 4);
            gridCards.addView(card, params);
        }
    }

    // ========== 数据刷新 ==========

    private void startAutoRefresh() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleWithFixedDelay(() -> {
            // 后台线程读取 + 存储
            SensorData data = hardwareClient.readSensors(currentGhId);
            dbHelper.saveSensorData(data);
            // 主线程更新UI
            mainHandler.post(() -> updateCardUI(data));
        }, 0, REFRESH_INTERVAL_SEC, TimeUnit.SECONDS);
    }

    private void stopAutoRefresh() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
    }

    private void refreshData() {
        new Thread(() -> {
            SensorData data = hardwareClient.readSensors(currentGhId);
            long id = dbHelper.saveSensorData(data);
            mainHandler.post(() -> {
                updateCardUI(data);
                tvLastUpdate.setText("上次更新: " + data.getRecordTime() + "  (id=" + id + ")");
            });
        }).start();
    }

    /** 用 SensorData 更新所有卡片 */
    private void updateCardUI(SensorData data) {
        for (int i = 0; i < SensorData.SENSOR_TYPES.length; i++) {
            String type = SensorData.SENSOR_TYPES[i];
            float value = data.getValueByName(type);
            boolean outOfRange = thresholdConfig.isOutOfRange(type, value);

            cardValues[i].setText(formatValue(value, type));
            cardValues[i].setTextColor(outOfRange
                    ? getResources().getColor(R.color.error_red)
                    : getResources().getColor(R.color.text_primary));
            cardIndicators[i].setImageResource(outOfRange
                    ? R.drawable.indicator_red : R.drawable.indicator_green);
            cardStatuses[i].setText(outOfRange ? "超标" : "正常");
            cardStatuses[i].setTextColor(outOfRange
                    ? getResources().getColor(R.color.error_red)
                    : getResources().getColor(R.color.success_green));

            // 超标时卡片边框变红
            if (cardViews[i] instanceof com.google.android.material.card.MaterialCardView) {
                com.google.android.material.card.MaterialCardView mcv =
                        (com.google.android.material.card.MaterialCardView) cardViews[i];
                mcv.setStrokeWidth(outOfRange ? 2 : 0);
                mcv.setStrokeColor(outOfRange
                        ? getResources().getColor(R.color.error_red)
                        : 0);
            }
        }
        tvLastUpdate.setText("上次更新: " + data.getRecordTime());
    }

    private String formatValue(float value, String type) {
        if ("light".equals(type)) return String.format("%.0f", value);
        else if ("ph".equals(type) || "ec".equals(type)) return String.format("%.2f", value);
        else return String.format("%.1f", value);
    }

    private void updateTitle() {
        tvTitle.setText("环境监测 — GH-" + String.format("%03d", currentGhId));
    }

    // ========== 历史查询面板 ==========

    private void openHistoryPanel() {
        panelMonitor.setVisibility(View.GONE);
        panelHistory.setVisibility(View.VISIBLE);

        HistoryFragment histFragment = new HistoryFragment();
        getChildFragmentManager().beginTransaction()
                .replace(R.id.panel_history, histFragment)
                .addToBackStack(null)
                .commit();

        // 监听返回栈
        getChildFragmentManager().addOnBackStackChangedListener(
                new androidx.fragment.app.FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                if (getChildFragmentManager().getBackStackEntryCount() == 0) {
                    panelHistory.setVisibility(View.GONE);
                    panelMonitor.setVisibility(View.VISIBLE);
                    getChildFragmentManager().removeOnBackStackChangedListener(this);
                }
            }
        });
    }
}
