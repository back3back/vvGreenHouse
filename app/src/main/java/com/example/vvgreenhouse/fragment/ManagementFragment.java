package com.example.vvgreenhouse.fragment;

import android.app.AlertDialog;
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
import com.example.vvgreenhouse.model.ManagementLog;
import com.example.vvgreenhouse.model.Personnel;
import com.example.vvgreenhouse.model.PestControl;
import com.example.vvgreenhouse.model.SalesRecord;
import com.example.vvgreenhouse.utils.CsvExporter;
import com.google.android.material.tabs.TabLayout;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 日常管理模块 Fragment
 *
 * 6个Tab：人员管理 / 农事记录 / 病虫害 / 水肥管理 / 设备运维 / 产销台账
 * 每个 Tab 下 RecyclerView 列表 + FAB 添加 + 长按删除 + 导出CSV
 */
public class ManagementFragment extends Fragment {

    private GreenhouseDBHelper dbHelper;
    private Handler mainHandler;
    private SimpleDateFormat sdfFull = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    private SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private TabLayout tabMgmt;
    private RecyclerView rvList;
    private TextView tvEmptyHint;
    private View fabAdd;
    private Button btnExport, btnStats;

    private int currentTab = 0;  // 0=人员,1=农事,2=病虫害,3=水肥,4=运维,5=产销

    // 列表数据缓存
    private List<Personnel> personnelList = new ArrayList<>();
    private List<ManagementLog> farmingList = new ArrayList<>();
    private List<PestControl> pestList = new ArrayList<>();
    private List<ManagementLog> waterFertList = new ArrayList<>();
    private List<ManagementLog> maintList = new ArrayList<>();
    private List<SalesRecord> salesList = new ArrayList<>();

    private static final String[] TAB_TITLES = {"人员", "农事", "病虫害", "水肥", "运维", "产销"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_management, container, false);
        dbHelper = GreenhouseDBHelper.getInstance(getContext());
        mainHandler = new Handler(Looper.getMainLooper());

        tabMgmt = view.findViewById(R.id.tab_mgmt);
        rvList = view.findViewById(R.id.rv_list);
        tvEmptyHint = view.findViewById(R.id.tv_empty_hint);
        fabAdd = view.findViewById(R.id.fab_add);
        btnExport = view.findViewById(R.id.btn_export_csv);
        btnStats = view.findViewById(R.id.btn_stats);

        rvList.setLayoutManager(new LinearLayoutManager(getContext()));

        setupTabs();
        fabAdd.setOnClickListener(v -> onFabClick());
        btnExport.setOnClickListener(v -> exportCurrentTab());

        loadTab(0);

        return view;
    }

    private void setupTabs() {
        for (String title : TAB_TITLES) {
            tabMgmt.addTab(tabMgmt.newTab().setText(title));
        }
        tabMgmt.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                btnStats.setVisibility(currentTab == 5 ? View.VISIBLE : View.GONE);
                loadTab(currentTab);
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    // ========== 数据加载 ==========

    private void loadTab(int tab) {
        if (!isAdded()) return;
        new Thread(() -> {
            switch (tab) {
                case 0: // 人员
                    personnelList = dbHelper.getAllPersonnel();
                    mainHandler.post(() -> showPersonnelList());
                    break;
                case 1: // 农事
                    farmingList = dbHelper.getFarmingLogs(0, 200);
                    mainHandler.post(() -> showFarmingList());
                    break;
                case 2: // 病虫害
                    pestList = dbHelper.getPestControls(0, 200);
                    mainHandler.post(() -> showPestList());
                    break;
                case 3: // 水肥
                    waterFertList = dbHelper.getWaterFertLogs(0, 200);
                    mainHandler.post(() -> showWaterFertList());
                    break;
                case 4: // 运维
                    maintList = dbHelper.getMaintenanceLogs(0, 200);
                    mainHandler.post(() -> showMaintList());
                    break;
                case 5: // 产销
                    salesList = dbHelper.getSalesRecords(0, 200);
                    mainHandler.post(() -> showSalesList());
                    break;
            }
        }).start();
    }

    // ========== 列表呈现（通用 Adapter） ==========

    private void showGenericList(GenericAdapter adapter, List<?> list) {
        if (!isAdded()) return;
        if (list.isEmpty()) {
            rvList.setAdapter(null);
            tvEmptyHint.setVisibility(View.VISIBLE);
        } else {
            tvEmptyHint.setVisibility(View.GONE);
            rvList.setAdapter(adapter);
        }
    }

    // ---------- 人员 ----------
    private void showPersonnelList() {
        List<String> items = new ArrayList<>();
        for (Personnel p : personnelList) {
            items.add(p.getName() + " | " + p.getPosition()
                    + " | 工号:" + p.getEmployeeNo()
                    + " | " + (p.getStatus() == 1 ? "在岗" : "离职"));
        }
        List<String> subs = new ArrayList<>();
        for (Personnel p : personnelList) {
            subs.add("电话:" + (p.getPhone() != null ? p.getPhone() : "-")
                    + "  负责大棚:" + (p.getGreenhouseId() > 0 ? "GH-" + p.getGreenhouseId() : "全部"));
        }
        List<String> times = new ArrayList<>();
        for (Personnel p : personnelList) {
            times.add("入职:" + p.getCreateTime());
        }
        List<String> tags = new ArrayList<>();
        for (Personnel p : personnelList) {
            tags.add(p.getPosition());
        }
        showGenericList(new GenericAdapter(items, subs, times, tags) {
            @Override void onClick(int pos) { editPersonnel(pos); }
            @Override void onLongClick(int pos) { deletePersonnel(pos); }
        }, personnelList);
    }

    // ---------- 农事 ----------
    private void showFarmingList() {
        List<String> items = new ArrayList<>();
        List<String> subs = new ArrayList<>();
        List<String> times = new ArrayList<>();
        List<String> tags = new ArrayList<>();
        for (ManagementLog m : farmingList) {
            items.add("🌾 " + m.getLogType() + " — GH-" + String.format("%03d", m.getGreenhouseId()));
            subs.add(m.getContent() != null ? m.getContent() : "");
            times.add(m.getOperateTime() + " | " + m.getOperator());
            tags.add(m.getLogType());
        }
        showGenericList(new GenericAdapter(items, subs, times, tags) {
            @Override void onClick(int pos) { /* detail popup */ }
            @Override void onLongClick(int pos) { deleteFarming(pos); }
        }, farmingList);
    }

    // ---------- 病虫害 ----------
    private void showPestList() {
        List<String> items = new ArrayList<>();
        List<String> subs = new ArrayList<>();
        List<String> times = new ArrayList<>();
        List<String> tags = new ArrayList<>();
        for (PestControl pc : pestList) {
            items.add("🐛 " + pc.getPestType() + " — GH-" + String.format("%03d", pc.getGreenhouseId()));
            subs.add("措施:" + (pc.getControlMeasures() != null ? pc.getControlMeasures() : "-")
                    + "  农药:" + (pc.getPesticideName() != null ? pc.getPesticideName() : "-"));
            times.add("发现:" + pc.getFindDate() + " | 记录:" + pc.getRecorder());
            tags.add(pc.getControlEffect() != null ? pc.getControlEffect() : "");
        }
        showGenericList(new GenericAdapter(items, subs, times, tags) {
            @Override void onClick(int pos) { /* detail popup */ }
            @Override void onLongClick(int pos) { deletePest(pos); }
        }, pestList);
    }

    // ---------- 水肥 ----------
    private void showWaterFertList() {
        List<String> items = new ArrayList<>();
        List<String> subs = new ArrayList<>();
        List<String> times = new ArrayList<>();
        List<String> tags = new ArrayList<>();
        for (ManagementLog m : waterFertList) {
            items.add("💧 " + m.getLogType() + " — GH-" + String.format("%03d", m.getGreenhouseId()));
            StringBuilder sb = new StringBuilder();
            if (m.getFertilizerName() != null && !m.getFertilizerName().isEmpty())
                sb.append("肥料:").append(m.getFertilizerName()).append(" ");
            if (m.getFertilizerAmount() != null && !m.getFertilizerAmount().isEmpty())
                sb.append("用量:").append(m.getFertilizerAmount()).append(" ");
            if (m.getIrrigationAmount() != null && !m.getIrrigationAmount().isEmpty())
                sb.append("灌溉:").append(m.getIrrigationAmount());
            subs.add(sb.toString());
            times.add(m.getOperateTime() + " | " + m.getOperator()
                    + (m.getDurationMin() > 0 ? " | 耗时" + m.getDurationMin() + "分" : ""));
            tags.add(m.getLogType());
        }
        showGenericList(new GenericAdapter(items, subs, times, tags) {
            @Override void onClick(int pos) { }
            @Override void onLongClick(int pos) { deleteWaterFert(pos); }
        }, waterFertList);
    }

    // ---------- 运维 ----------
    private void showMaintList() {
        List<String> items = new ArrayList<>();
        List<String> subs = new ArrayList<>();
        List<String> times = new ArrayList<>();
        List<String> tags = new ArrayList<>();
        for (ManagementLog m : maintList) {
            items.add("🔧 " + m.getDeviceName() + " — GH-" + String.format("%03d", m.getGreenhouseId()));
            subs.add("类型:" + m.getMaintenanceType()
                    + (m.getCost() != null && !m.getCost().isEmpty() ? " | 费用:¥" + m.getCost() : ""));
            times.add(m.getOperateTime() + " | " + m.getOperator());
            tags.add(m.getMaintenanceType());
        }
        showGenericList(new GenericAdapter(items, subs, times, tags) {
            @Override void onClick(int pos) { }
            @Override void onLongClick(int pos) { deleteMaint(pos); }
        }, maintList);
    }

    // ---------- 产销 ----------
    private void showSalesList() {
        List<String> items = new ArrayList<>();
        List<String> subs = new ArrayList<>();
        List<String> times = new ArrayList<>();
        List<String> tags = new ArrayList<>();
        for (SalesRecord sr : salesList) {
            items.add("💰 " + sr.getVariety() + " ×" + sr.getQuantity()
                    + " — GH-" + String.format("%03d", sr.getGreenhouseId()));
            subs.add("批次:" + sr.getBatchNo()
                    + " | 单价:¥" + String.format("%.2f", sr.getUnitPrice())
                    + " | 总价:¥" + String.format("%.2f", sr.getTotalAmount())
                    + " | " + (sr.getCustomer() != null ? sr.getCustomer() : ""));
            times.add("采收:" + sr.getHarvestDate() + " | 记录:" + sr.getRecorder());
            tags.add(sr.getQualityGrade() != null ? sr.getQualityGrade() + "级" : "");
        }
        showGenericList(new GenericAdapter(items, subs, times, tags) {
            @Override void onClick(int pos) { editSales(pos); }
            @Override void onLongClick(int pos) { deleteSales(pos); }
        }, salesList);
    }

    // ========== 通用列表 Adapter ==========

    private abstract class GenericAdapter extends RecyclerView.Adapter<GenericAdapter.VH> {
        final List<String> titles, subtitles, times, tags;
        GenericAdapter(List<String> t, List<String> s, List<String> tm, List<String> tg) {
            titles = t; subtitles = s; times = tm; tags = tg;
        }
        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_mgmt_log, parent, false);
            return new VH(v);
        }
        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            h.title.setText(titles.get(pos));
            h.subtitle.setText(subtitles.get(pos));
            h.time.setText(times.get(pos));
            String tag = tags.get(pos);
            if (tag != null && !tag.isEmpty()) {
                h.tag.setVisibility(View.VISIBLE);
                h.tag.setText(tag);
            } else {
                h.tag.setVisibility(View.GONE);
            }
            h.itemView.setOnClickListener(v -> onClick(pos));
            h.itemView.setOnLongClickListener(v -> { onLongClick(pos); return true; });
        }
        @Override public int getItemCount() { return titles.size(); }
        abstract void onClick(int pos);
        abstract void onLongClick(int pos);
        class VH extends RecyclerView.ViewHolder {
            TextView title, subtitle, time, tag;
            VH(@NonNull View v) {
                super(v);
                title = v.findViewById(R.id.tv_item_title);
                subtitle = v.findViewById(R.id.tv_item_subtitle);
                time = v.findViewById(R.id.tv_item_time);
                tag = v.findViewById(R.id.tv_item_tag);
            }
        }
    }

    // ========== FAB 添加 ==========

    private void onFabClick() {
        switch (currentTab) {
            case 0: showPersonnelForm(); break;
            case 1: showLogForm("farming"); break;
            case 2: showPestForm(); break;
            case 3: showLogForm("water_fert"); break;
            case 4: showLogForm("maintenance"); break;
            case 5: showSalesForm(); break;
        }
    }

    // ---- 人员表单 ----
    private void showPersonnelForm() {
        View form = LayoutInflater.from(getContext()).inflate(R.layout.dialog_personnel_form, null);
        EditText etNo = form.findViewById(R.id.et_employee_no);
        EditText etName = form.findViewById(R.id.et_name);
        Spinner spGender = form.findViewById(R.id.sp_gender);
        Spinner spPosition = form.findViewById(R.id.sp_position);
        EditText etPhone = form.findViewById(R.id.et_phone);
        Spinner spGh = form.findViewById(R.id.sp_greenhouse);

        spGender.setAdapter(new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"男", "女"}));
        spPosition.setAdapter(new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item,
                Personnel.POSITIONS));

        String[] ghItems = new String[9];
        ghItems[0] = "全部(0)";
        for (int i = 1; i <= 8; i++) ghItems[i] = "GH-" + String.format("%03d", i);
        spGh.setAdapter(new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item, ghItems));

        new AlertDialog.Builder(getContext())
                .setTitle("添加人员")
                .setView(form)
                .setPositiveButton("保存", (d, w) -> {
                    Personnel p = new Personnel();
                    p.setEmployeeNo(etNo.getText().toString().trim());
                    p.setName(etName.getText().toString().trim());
                    p.setGender(spGender.getSelectedItem().toString());
                    p.setPosition(spPosition.getSelectedItem().toString());
                    p.setPhone(etPhone.getText().toString().trim());
                    p.setGreenhouseId(spGh.getSelectedItemPosition());
                    p.setStatus(1);
                    new Thread(() -> {
                        dbHelper.savePersonnel(p);
                        mainHandler.post(() -> loadTab(0));
                    }).start();
                })
                .setNegativeButton("取消", null).show();
    }

    private void editPersonnel(int pos) {
        if (pos < 0 || pos >= personnelList.size()) return;
        // Simple detail + delete popup for now
    }

    private void deletePersonnel(int pos) {
        if (pos < 0 || pos >= personnelList.size()) return;
        Personnel p = personnelList.get(pos);
        new AlertDialog.Builder(getContext())
                .setTitle("确认删除")
                .setMessage("确定删除人员 " + p.getName() + " (工号:" + p.getEmployeeNo() + ") 吗？")
                .setPositiveButton("删除", (d, w) -> new Thread(() -> {
                    dbHelper.deletePersonnel(p.getId());
                    mainHandler.post(() -> loadTab(0));
                }).start())
                .setNegativeButton("取消", null).show();
    }

    // ---- 通用日志表单（农事/水肥/运维） ----
    private void showLogForm(String category) {
        View form = LayoutInflater.from(getContext()).inflate(R.layout.dialog_log_form, null);
        TextView tvTitle = form.findViewById(R.id.tv_form_title);
        Spinner spGh = form.findViewById(R.id.sp_gh);
        Spinner spType = form.findViewById(R.id.sp_type);
        EditText etOp = form.findViewById(R.id.et_operator);
        EditText etTime = form.findViewById(R.id.et_time);
        EditText etContent = form.findViewById(R.id.et_content);
        EditText etRemarks = form.findViewById(R.id.et_remarks);
        TextView tvLabelType = form.findViewById(R.id.tv_label_type);
        LinearLayout layoutWf = form.findViewById(R.id.layout_water_fert_fields);
        LinearLayout layoutMaint = form.findViewById(R.id.layout_maintenance_fields);

        // 默认时间
        etTime.setText(sdfFull.format(new Date()));
        etTime.setOnClickListener(v -> showDateTimePicker(etTime));

        // 大棚 Spinner
        String[] ghItems = new String[8];
        for (int i = 0; i < 8; i++) ghItems[i] = "GH-" + String.format("%03d", i + 1);
        spGh.setAdapter(new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item, ghItems));

        switch (category) {
            case "farming":
                tvTitle.setText("添加农事记录");
                tvLabelType.setText("农事类型");
                spType.setAdapter(new ArrayAdapter<>(getContext(),
                        android.R.layout.simple_spinner_dropdown_item,
                        new String[]{"播种", "移栽", "修剪", "除草", "施肥", "采收", "翻耕", "其他"}));
                break;
            case "water_fert":
                tvTitle.setText("添加水肥记录");
                tvLabelType.setText("水肥类型");
                spType.setAdapter(new ArrayAdapter<>(getContext(),
                        android.R.layout.simple_spinner_dropdown_item,
                        new String[]{"灌溉", "施肥", "滴灌施肥", "叶面喷施", "冲洗", "其他"}));
                layoutWf.setVisibility(View.VISIBLE);
                break;
            case "maintenance":
                tvTitle.setText("添加运维记录");
                tvLabelType.setText("维护类型");
                spType.setAdapter(new ArrayAdapter<>(getContext(),
                        android.R.layout.simple_spinner_dropdown_item,
                        new String[]{"例行检查", "保养", "维修", "更换"}));
                layoutMaint.setVisibility(View.VISIBLE);
                break;
        }

        new AlertDialog.Builder(getContext())
                .setView(form)
                .setPositiveButton("保存", (d, w) -> {
                    ManagementLog m = new ManagementLog();
                    m.setGreenhouseId(spGh.getSelectedItemPosition() + 1);
                    m.setLogType(spType.getSelectedItem().toString());
                    m.setOperator(etOp.getText().toString().trim());
                    m.setOperateTime(etTime.getText().toString().trim());
                    m.setContent(etContent.getText().toString().trim());
                    m.setRemarks(etRemarks.getText().toString().trim());
                    m.setCategory(category);

                    new Thread(() -> {
                        switch (category) {
                            case "farming":
                                dbHelper.saveFarmingLog(m); break;
                            case "water_fert": {
                                EditText etFert = form.findViewById(R.id.et_fertilizer_name);
                                EditText etFertAmt = form.findViewById(R.id.et_fertilizer_amount);
                                EditText etIrri = form.findViewById(R.id.et_irrigation);
                                EditText etDur = form.findViewById(R.id.et_duration);
                                m.setFertilizerName(etFert.getText().toString().trim());
                                m.setFertilizerAmount(etFertAmt.getText().toString().trim());
                                m.setIrrigationAmount(etIrri.getText().toString().trim());
                                try { m.setDurationMin(Integer.parseInt(etDur.getText().toString().trim())); }
                                catch (NumberFormatException ignored) {}
                                dbHelper.saveWaterFertLog(m); break;
                            }
                            case "maintenance": {
                                EditText etDev = form.findViewById(R.id.et_device_name);
                                Spinner spMtType = form.findViewById(R.id.sp_maintenance_type);
                                EditText etCost = form.findViewById(R.id.et_cost);
                                m.setDeviceName(etDev.getText().toString().trim());
                                m.setMaintenanceType(spMtType.getSelectedItem().toString());
                                m.setCost(etCost.getText().toString().trim());
                                dbHelper.saveMaintenanceLog(m); break;
                            }
                        }
                        mainHandler.post(() -> loadTab(currentTab));
                    }).start();
                })
                .setNegativeButton("取消", null).show();

        // 运维表单额外初始化
        if ("maintenance".equals(category)) {
            Spinner spMtType = form.findViewById(R.id.sp_maintenance_type);
            spMtType.setAdapter(new ArrayAdapter<>(getContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    new String[]{"例行检查", "保养", "维修", "更换"}));
        }
    }

    // ---- 病虫害表单 ----
    private void showPestForm() {
        View form = LayoutInflater.from(getContext()).inflate(R.layout.dialog_pest_form, null);
        Spinner spGh = form.findViewById(R.id.sp_gh);
        EditText etFindDate = form.findViewById(R.id.et_find_date);
        Spinner spPestType = form.findViewById(R.id.sp_pest_type);
        EditText etArea = form.findViewById(R.id.et_affected_area);
        EditText etSymptoms = form.findViewById(R.id.et_symptoms);
        EditText etMeasures = form.findViewById(R.id.et_measures);
        EditText etPestName = form.findViewById(R.id.et_pesticide_name);
        EditText etPestAmt = form.findViewById(R.id.et_pesticide_amount);
        Spinner spEffect = form.findViewById(R.id.sp_effect);
        EditText etPhoto = form.findViewById(R.id.et_photo_path);
        EditText etRecorder = form.findViewById(R.id.et_recorder);

        etFindDate.setText(sdfDate.format(new Date()));
        etFindDate.setOnClickListener(v -> showDatePickerOnly(etFindDate));

        String[] ghItems = new String[8];
        for (int i = 0; i < 8; i++) ghItems[i] = "GH-" + String.format("%03d", i + 1);
        spGh.setAdapter(new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item, ghItems));
        spPestType.setAdapter(new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"蚜虫", "红蜘蛛", "白粉虱", "霜霉病", "灰霉病", "白粉病", "根腐病", "其他"}));
        spEffect.setAdapter(new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"显著", "有效", "部分有效", "无效"}));

        new AlertDialog.Builder(getContext())
                .setView(form)
                .setPositiveButton("保存", (d, w) -> {
                    PestControl pc = new PestControl();
                    pc.setGreenhouseId(spGh.getSelectedItemPosition() + 1);
                    pc.setFindDate(etFindDate.getText().toString().trim());
                    pc.setPestType(spPestType.getSelectedItem().toString());
                    pc.setAffectedArea(etArea.getText().toString().trim());
                    pc.setSymptoms(etSymptoms.getText().toString().trim());
                    pc.setControlMeasures(etMeasures.getText().toString().trim());
                    pc.setPesticideName(etPestName.getText().toString().trim());
                    pc.setPesticideAmount(etPestAmt.getText().toString().trim());
                    pc.setControlEffect(spEffect.getSelectedItem().toString());
                    pc.setPhotoPath(etPhoto.getText().toString().trim());
                    pc.setRecorder(etRecorder.getText().toString().trim());
                    new Thread(() -> {
                        dbHelper.savePestControl(pc);
                        mainHandler.post(() -> loadTab(2));
                    }).start();
                })
                .setNegativeButton("取消", null).show();
    }

    // ---- 产销表单 ----
    private void showSalesForm() {
        View form = LayoutInflater.from(getContext()).inflate(R.layout.dialog_sales_form, null);
        EditText etBatch = form.findViewById(R.id.et_batch_no);
        Spinner spGh = form.findViewById(R.id.sp_gh);
        EditText etVariety = form.findViewById(R.id.et_variety);
        EditText etHarvest = form.findViewById(R.id.et_harvest_date);
        EditText etQty = form.findViewById(R.id.et_quantity);
        Spinner spQuality = form.findViewById(R.id.sp_quality);
        Spinner spChannel = form.findViewById(R.id.sp_channel);
        EditText etPrice = form.findViewById(R.id.et_price);
        EditText etTotal = form.findViewById(R.id.et_total);
        EditText etCustomer = form.findViewById(R.id.et_customer);
        EditText etRecorder = form.findViewById(R.id.et_recorder);

        etBatch.setText("BAT-" + System.currentTimeMillis() % 1000000);
        etHarvest.setText(sdfDate.format(new Date()));
        etHarvest.setOnClickListener(v -> showDatePickerOnly(etHarvest));

        String[] ghItems = new String[8];
        for (int i = 0; i < 8; i++) ghItems[i] = "GH-" + String.format("%03d", i + 1);
        spGh.setAdapter(new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item, ghItems));
        spQuality.setAdapter(new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item, new String[]{"A", "B", "C"}));
        spChannel.setAdapter(new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"批发市场", "花卉门店", "线上商城", "出口", "其他"}));

        new AlertDialog.Builder(getContext())
                .setView(form)
                .setPositiveButton("保存", (d, w) -> {
                    SalesRecord sr = new SalesRecord();
                    sr.setBatchNo(etBatch.getText().toString().trim());
                    sr.setGreenhouseId(spGh.getSelectedItemPosition() + 1);
                    sr.setVariety(etVariety.getText().toString().trim());
                    sr.setHarvestDate(etHarvest.getText().toString().trim());
                    try { sr.setQuantity(Integer.parseInt(etQty.getText().toString().trim())); }
                    catch (NumberFormatException exc) { sr.setQuantity(0); }
                    sr.setQualityGrade(spQuality.getSelectedItem().toString());
                    sr.setSaleChannel(spChannel.getSelectedItem().toString());
                    try { sr.setUnitPrice(Double.parseDouble(etPrice.getText().toString().trim())); }
                    catch (NumberFormatException exc) { sr.setUnitPrice(0); }
                    String totalStr = etTotal.getText().toString().trim();
                    if (!totalStr.isEmpty()) {
                        try { sr.setTotalAmount(Double.parseDouble(totalStr)); }
                        catch (NumberFormatException exc) { sr.setTotalAmount(sr.getUnitPrice() * sr.getQuantity()); }
                    } else {
                        sr.setTotalAmount(sr.getUnitPrice() * sr.getQuantity());
                    }
                    sr.setCustomer(etCustomer.getText().toString().trim());
                    sr.setRecorder(etRecorder.getText().toString().trim());
                    new Thread(() -> {
                        dbHelper.saveSalesRecord(sr);
                        mainHandler.post(() -> loadTab(5));
                    }).start();
                })
                .setNegativeButton("取消", null).show();
    }

    // ========== 删除操作 ==========

    private void deleteFarming(int pos) {
        if (pos < 0 || pos >= farmingList.size()) return;
        new AlertDialog.Builder(getContext())
                .setTitle("确认删除")
                .setMessage("确定删除这条农事记录吗？")
                .setPositiveButton("删除", (d, w) -> new Thread(() -> {
                    dbHelper.deleteFarmingLog(farmingList.get(pos).getId());
                    mainHandler.post(() -> loadTab(1));
                }).start())
                .setNegativeButton("取消", null).show();
    }

    private void deletePest(int pos) {
        if (pos < 0 || pos >= pestList.size()) return;
        new AlertDialog.Builder(getContext())
                .setTitle("确认删除")
                .setMessage("确定删除这条病虫害记录吗？")
                .setPositiveButton("删除", (d, w) -> new Thread(() -> {
                    dbHelper.deletePestControl(pestList.get(pos).getId());
                    mainHandler.post(() -> loadTab(2));
                }).start())
                .setNegativeButton("取消", null).show();
    }

    private void deleteWaterFert(int pos) {
        if (pos < 0 || pos >= waterFertList.size()) return;
        new AlertDialog.Builder(getContext())
                .setTitle("确认删除")
                .setMessage("确定删除这条水肥记录吗？")
                .setPositiveButton("删除", (d, w) -> new Thread(() -> {
                    dbHelper.deleteWaterFertLog(waterFertList.get(pos).getId());
                    mainHandler.post(() -> loadTab(3));
                }).start())
                .setNegativeButton("取消", null).show();
    }

    private void deleteMaint(int pos) {
        if (pos < 0 || pos >= maintList.size()) return;
        new AlertDialog.Builder(getContext())
                .setTitle("确认删除")
                .setMessage("确定删除这条运维记录吗？")
                .setPositiveButton("删除", (d, w) -> new Thread(() -> {
                    dbHelper.deleteMaintenanceLog(maintList.get(pos).getId());
                    mainHandler.post(() -> loadTab(4));
                }).start())
                .setNegativeButton("取消", null).show();
    }

    private void deleteSales(int pos) {
        if (pos < 0 || pos >= salesList.size()) return;
        new AlertDialog.Builder(getContext())
                .setTitle("确认删除")
                .setMessage("确定删除这条产销记录吗？")
                .setPositiveButton("删除", (d, w) -> new Thread(() -> {
                    dbHelper.deleteSalesRecord(salesList.get(pos).getId());
                    mainHandler.post(() -> loadTab(5));
                }).start())
                .setNegativeButton("取消", null).show();
    }

    private void editSales(int pos) {
        // Future: edit existing sales record
    }

    // ========== 导出 ==========

    private void exportCurrentTab() {
        new Thread(() -> {
            String[] headers;
            List<String[]> rows = new ArrayList<>();
            String prefix;
            switch (currentTab) {
                case 0: {
                    headers = new String[]{"工号","姓名","性别","岗位","电话","负责大棚","状态","入职时间"};
                    final List<Personnel> list = dbHelper.getAllPersonnel();
                    for (Personnel p : list) {
                        rows.add(new String[]{p.getEmployeeNo(), p.getName(), p.getGender(),
                                p.getPosition(), p.getPhone(), String.valueOf(p.getGreenhouseId()),
                                p.getStatus() == 1 ? "在岗":"离职", p.getCreateTime()});
                    }
                    prefix = "personnel"; break;
                }
                case 1: {
                    headers = new String[]{"大棚ID","类型","操作人","操作时间","内容","备注"};
                    final List<ManagementLog> list = dbHelper.getFarmingLogs(0, 9999);
                    for (ManagementLog m : list) {
                        rows.add(new String[]{String.valueOf(m.getGreenhouseId()), m.getLogType(),
                                m.getOperator(), m.getOperateTime(), m.getContent(), m.getRemarks()});
                    }
                    prefix = "farming"; break;
                }
                case 2: {
                    headers = new String[]{"大棚ID","发现日期","病虫害类型","区域","症状","措施","农药","用量","效果","记录人"};
                    final List<PestControl> list = dbHelper.getPestControls(0, 9999);
                    for (PestControl pc : list) {
                        rows.add(new String[]{String.valueOf(pc.getGreenhouseId()), pc.getFindDate(),
                                pc.getPestType(), pc.getAffectedArea(), pc.getSymptoms(),
                                pc.getControlMeasures(), pc.getPesticideName(), pc.getPesticideAmount(),
                                pc.getControlEffect(), pc.getRecorder()});
                    }
                    prefix = "pest_control"; break;
                }
                case 3: {
                    headers = new String[]{"大棚ID","类型","操作人","时间","肥料","用量","灌溉量","时长(分)","内容","备注"};
                    final List<ManagementLog> list = dbHelper.getWaterFertLogs(0, 9999);
                    for (ManagementLog m : list) {
                        rows.add(new String[]{String.valueOf(m.getGreenhouseId()), m.getLogType(),
                                m.getOperator(), m.getOperateTime(), m.getFertilizerName(),
                                m.getFertilizerAmount(), m.getIrrigationAmount(),
                                String.valueOf(m.getDurationMin()), m.getContent(), m.getRemarks()});
                    }
                    prefix = "water_fertilizer"; break;
                }
                case 4: {
                    headers = new String[]{"大棚ID","设备","维护类型","操作人","时间","内容","费用","备注"};
                    final List<ManagementLog> list = dbHelper.getMaintenanceLogs(0, 9999);
                    for (ManagementLog m : list) {
                        rows.add(new String[]{String.valueOf(m.getGreenhouseId()), m.getDeviceName(),
                                m.getMaintenanceType(), m.getOperator(), m.getOperateTime(),
                                m.getContent(), m.getCost(), m.getRemarks()});
                    }
                    prefix = "maintenance"; break;
                }
                case 5: {
                    headers = new String[]{"批次号","大棚ID","品种","采收日期","数量","等级","渠道","单价","总价","客户","记录人"};
                    final List<SalesRecord> list = dbHelper.getSalesRecords(0, 9999);
                    for (SalesRecord sr : list) {
                        rows.add(new String[]{sr.getBatchNo(), String.valueOf(sr.getGreenhouseId()),
                                sr.getVariety(), sr.getHarvestDate(), String.valueOf(sr.getQuantity()),
                                sr.getQualityGrade(), sr.getSaleChannel(),
                                String.format("%.2f", sr.getUnitPrice()),
                                String.format("%.2f", sr.getTotalAmount()),
                                sr.getCustomer(), sr.getRecorder()});
                    }
                    prefix = "sales"; break;
                }
                default: return;
            }
            File file = CsvExporter.export("GreenHouse", prefix, headers, rows);
            if (file != null) {
                CsvExporter.share(getContext(), file, "text/csv");
            } else {
                mainHandler.post(() -> Toast.makeText(getContext(),
                        "导出失败，请检查存储权限", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // ========== 日期选择 ==========

    private void showDateTimePicker(EditText target) {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(getContext(), (view, y, m, d) -> {
            Calendar picked = Calendar.getInstance();
            picked.set(y, m, d);
            // keep time part
            target.setText(sdfFull.format(picked.getTime()));
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showDatePickerOnly(EditText target) {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(getContext(), (view, y, m, d) -> {
            target.setText(String.format(Locale.getDefault(), "%04d-%02d-%02d", y, m + 1, d));
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }
}
