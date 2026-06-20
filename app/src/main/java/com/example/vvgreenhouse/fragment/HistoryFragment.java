package com.example.vvgreenhouse.fragment;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.vvgreenhouse.R;
import com.example.vvgreenhouse.database.GreenhouseDBHelper;
import com.example.vvgreenhouse.model.SensorData;
import com.example.vvgreenhouse.widget.SimpleLineChart;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 历史数据查询 Fragment
 *
 * 功能：按大棚 + 传感器类型 + 日期范围查询，支持列表 / 图表两种展示模式
 */
public class HistoryFragment extends Fragment {

    private Spinner spGh, spSensor;
    private EditText etStartDate, etEndDate;
    private RecyclerView rvData;
    private SimpleLineChart chartView;
    private TextView tvEmptyHint;
    private GreenhouseDBHelper dbHelper;
    private Handler mainHandler;

    private List<SensorData> currentData = new ArrayList<>();
    private SimpleDateFormat sdfFull = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    private SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);
        dbHelper = GreenhouseDBHelper.getInstance(getContext());
        mainHandler = new Handler(Looper.getMainLooper());

        spGh = view.findViewById(R.id.sp_history_gh);
        spSensor = view.findViewById(R.id.sp_history_sensor);
        etStartDate = view.findViewById(R.id.et_start_date);
        etEndDate = view.findViewById(R.id.et_end_date);
        rvData = view.findViewById(R.id.rv_history_data);
        chartView = view.findViewById(R.id.chart_view);
        tvEmptyHint = view.findViewById(R.id.tv_empty_hint);

        // 返回按钮
        view.findViewById(R.id.btn_back_to_monitor).setOnClickListener(v -> {
            getParentFragmentManager().popBackStack();
        });

        // 设置日期默认值（今天往前7天）
        Calendar cal = Calendar.getInstance();
        etEndDate.setText(sdfDate.format(cal.getTime()));
        cal.add(Calendar.DAY_OF_MONTH, -7);
        etStartDate.setText(sdfDate.format(cal.getTime()));

        // 日期选择器
        etStartDate.setOnClickListener(v -> showDatePicker(etStartDate));
        etEndDate.setOnClickListener(v -> showDatePicker(etEndDate));

        // 初始化 Spinner
        initSpinners();

        // RecyclerView
        rvData.setLayoutManager(new LinearLayoutManager(getContext()));

        // 按钮
        view.findViewById(R.id.btn_query).setOnClickListener(v -> queryData());
        view.findViewById(R.id.btn_list_view).setOnClickListener(v -> showListView());
        view.findViewById(R.id.btn_chart_view).setOnClickListener(v -> showChartView());

        return view;
    }

    private void initSpinners() {
        // 大棚编号
        String[] ghItems = new String[8];
        for (int i = 0; i < 8; i++) {
            ghItems[i] = "GH-" + String.format("%03d", i + 1);
        }
        spGh.setAdapter(new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item, ghItems));

        // 传感器类型
        String[] sensorItems = new String[8];
        for (int i = 0; i < SensorData.SENSOR_TYPES.length; i++) {
            sensorItems[i] = SensorData.getChineseName(SensorData.SENSOR_TYPES[i]);
        }
        spSensor.setAdapter(new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item, sensorItems));
    }

    private void showDatePicker(EditText target) {
        Calendar cal = Calendar.getInstance();
        int y = cal.get(Calendar.YEAR);
        int m = cal.get(Calendar.MONTH);
        int d = cal.get(Calendar.DAY_OF_MONTH);
        new DatePickerDialog(getContext(), (view, year, month, day) -> {
            target.setText(String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, day));
        }, y, m, d).show();
    }

    private void queryData() {
        tvEmptyHint.setVisibility(View.GONE);
        int ghIndex = spGh.getSelectedItemPosition();  // 0~7
        int sensorIndex = spSensor.getSelectedItemPosition();  // 0~7

        String startStr = etStartDate.getText().toString() + " 00:00:00";
        String endStr = etEndDate.getText().toString() + " 23:59:59";

        new Thread(() -> {
            List<SensorData> data = dbHelper.getHistoryData(ghIndex + 1, startStr, endStr, 200);
            currentData = data;
            mainHandler.post(() -> {
                if (data.isEmpty()) {
                    tvEmptyHint.setText("无数据，请先运行实时监测采集数据");
                    tvEmptyHint.setVisibility(View.VISIBLE);
                    rvData.setVisibility(View.GONE);
                    chartView.setVisibility(View.GONE);
                } else {
                    tvEmptyHint.setVisibility(View.GONE);
                    updateListView(sensorIndex);
                    updateChartView(sensorIndex);
                    showListView(); // 默认列表视图
                }
            });
        }).start();
    }

    private void updateListView(int sensorIndex) {
        String sensorType = SensorData.SENSOR_TYPES[sensorIndex];
        String unit = SensorData.getUnit(sensorType);
        List<String> items = new ArrayList<>();
        for (SensorData d : currentData) {
            float val = d.getValueByName(sensorType);
            items.add(d.getRecordTime() + "  " + String.format("%.1f", val) + " " + unit);
        }
        rvData.setAdapter(new HistoryListAdapter(items));
    }

    private void updateChartView(int sensorIndex) {
        String sensorType = SensorData.SENSOR_TYPES[sensorIndex];
        List<Float> values = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (SensorData d : currentData) {
            values.add(d.getValueByName(sensorType));
            labels.add(d.getRecordTime());
        }
        chartView.addSeries(values, labels, 0xFF2E7D32, SensorData.getChineseName(sensorType));
    }

    private void showListView() {
        rvData.setVisibility(View.VISIBLE);
        chartView.setVisibility(View.GONE);
        tvEmptyHint.setVisibility(View.GONE);
    }

    private void showChartView() {
        rvData.setVisibility(View.GONE);
        chartView.setVisibility(View.VISIBLE);
        tvEmptyHint.setVisibility(View.GONE);
    }

    // ==================== 简单 RecyclerView Adapter ====================

    private static class HistoryListAdapter extends RecyclerView.Adapter<HistoryListAdapter.VH> {
        private final List<String> items;

        HistoryListAdapter(List<String> items) { this.items = items; }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView tv = new TextView(parent.getContext());
            tv.setPadding(32, 16, 32, 16);
            tv.setTextSize(14f);
            tv.setTextColor(0xFF212121);
            return new VH(tv);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            holder.textView.setText(items.get(position));
        }

        @Override public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            final TextView textView;
            VH(@NonNull View itemView) { super(itemView); textView = (TextView) itemView; }
        }
    }
}
