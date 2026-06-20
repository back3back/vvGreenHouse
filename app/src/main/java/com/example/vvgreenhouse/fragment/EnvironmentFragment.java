package com.example.vvgreenhouse.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.vvgreenhouse.R;
import com.example.vvgreenhouse.database.GreenhouseDBHelper;
import com.example.vvgreenhouse.hardware.MockHardwareClient;
import com.example.vvgreenhouse.model.SensorData;

/**
 * 环境监测模块 Fragment
 *
 * 核心功能：
 * - 每5秒自动采集模拟传感器数据
 * - 数据展示 + 数据库持久化
 * - 异常模式切换：模拟温度超标、湿度过低等告警
 * - 大棚切换：轮询 GH-1 ~ GH-8
 */
public class EnvironmentFragment extends Fragment {

    private static final String ARG_HARDWARE = "hardware_client";

    private MockHardwareClient hardwareClient;
    private GreenhouseDBHelper dbHelper;

    private TextView tvTitle, tvSubtitle, tvLastUpdate, tvSensorData;
    private Button btnRefresh, btnAbnormal, btnSwitchGh;

    private int currentGhId = 1;           // 当前大棚ID (1~8)

    private Handler handler;
    private Runnable autoRefreshTask;

    /** 每5秒自动刷新 */
    private static final int REFRESH_INTERVAL_MS = 5000;

    /**
     * 工厂方法：传入模拟硬件客户端
     */
    public static EnvironmentFragment newInstance(MockHardwareClient client) {
        EnvironmentFragment f = new EnvironmentFragment();
        Bundle args = new Bundle();
        // Fragment 传参简化处理：通过 MainActivity 的引用已在外部设置
        f.hardwareClient = client;
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_environment, container, false);

        tvTitle = view.findViewById(R.id.tv_title);
        tvSubtitle = view.findViewById(R.id.tv_subtitle);
        tvLastUpdate = view.findViewById(R.id.tv_last_update);
        tvSensorData = view.findViewById(R.id.tv_sensor_data);
        btnRefresh = view.findViewById(R.id.btn_refresh);
        btnAbnormal = view.findViewById(R.id.btn_abnormal_mode);
        btnSwitchGh = view.findViewById(R.id.btn_switch_gh);

        dbHelper = GreenhouseDBHelper.getInstance(getContext());
        handler = new Handler(Looper.getMainLooper());

        // 更新标题
        updateTitle();

        // ===== 按钮事件 =====

        btnRefresh.setOnClickListener(v -> refreshData());

        btnAbnormal.setOnClickListener(v -> {
            boolean current = hardwareClient.isAbnormalMode();
            hardwareClient.setAbnormalMode(!current);
            btnAbnormal.setText(!current ? "异常模式(开)" : "异常模式");
            tvSubtitle.setText(!current ? "⚠ 异常模式开启 - 数据将超出阈值" : "模拟数据每5秒自动刷新");
            refreshData();
        });

        btnSwitchGh.setOnClickListener(v -> {
            currentGhId = (currentGhId % 8) + 1;  // 1→2→…→8→1
            updateTitle();
            refreshData();
        });

        // 首次立即刷新 + 启动定时任务
        refreshData();
        startAutoRefresh();

        return view;
    }

    /** 启动每5秒自动刷新 */
    private void startAutoRefresh() {
        autoRefreshTask = new Runnable() {
            @Override
            public void run() {
                refreshData();
                handler.postDelayed(this, REFRESH_INTERVAL_MS);
            }
        };
        handler.postDelayed(autoRefreshTask, REFRESH_INTERVAL_MS);
    }

    /** 读取数据 + 更新UI + 存入数据库 */
    private void refreshData() {
        new Thread(() -> {
            // 从模拟硬件读取数据
            SensorData data = hardwareClient.readSensors(currentGhId);
            // 持久化到数据库
            long savedId = dbHelper.saveSensorData(data);

            // 回到主线程更新UI
            new Handler(Looper.getMainLooper()).post(() -> {
                tvSensorData.setText(data.toDisplayString());
                tvLastUpdate.setText("上次更新: " + data.getRecordTime()
                        + "  (已存入DB, id=" + savedId + ")");
            });
        }).start();
    }

    private void updateTitle() {
        tvTitle.setText("环境监测 — GH-00" + currentGhId);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 停止定时刷新
        if (handler != null && autoRefreshTask != null) {
            handler.removeCallbacks(autoRefreshTask);
        }
    }
}
