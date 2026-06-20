package com.example.vvgreenhouse.fragment;

import android.content.Context;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.vvgreenhouse.R;
import com.example.vvgreenhouse.database.GreenhouseDBHelper;
import com.example.vvgreenhouse.hardware.HardwareClientFactory;
import com.example.vvgreenhouse.utils.CsvExporter;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 系统设置 Fragment
 *
 * 功能：
 * - 硬件连接模式切换（模拟/真实/Fro）
 * - 服务器 IP/端口配置
 * - 8个大棚名称/品种/面积编辑
 * - 数据库备份到 Downloads
 * - 数据库从备份恢复
 * - 关于信息
 */
public class SettingsFragment extends Fragment {

    private GreenhouseDBHelper dbHelper;
    private Handler mainHandler;
    private SharedPreferences sp;

    // 硬件模式
    private RadioGroup rgHwMode;
    private RadioButton rbMock, rbReal, rbFro;
    // 服务器
    private EditText etIp, etPort;
    // 大棚列表
    private LinearLayout layoutGhList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        dbHelper = GreenhouseDBHelper.getInstance(getContext());
        mainHandler = new Handler(Looper.getMainLooper());
        sp = PreferenceManager.getDefaultSharedPreferences(getContext());

        // —— 硬件模式 ——
        rgHwMode = view.findViewById(R.id.rg_hw_mode);
        rbMock = view.findViewById(R.id.rb_mock);
        rbReal = view.findViewById(R.id.rb_real);
        rbFro   = view.findViewById(R.id.rb_fro);

        boolean isMock = HardwareClientFactory.isMockMode(getContext());
        boolean isFro  = sp.getBoolean("use_fro_library", false);
        if (isFro) {
            rbFro.setChecked(true);
        } else if (!isMock) {
            rbReal.setChecked(true);
        } else {
            rbMock.setChecked(true);
        }

        rgHwMode.setOnCheckedChangeListener((g, id) -> {
            if (id == R.id.rb_mock) {
                HardwareClientFactory.setMockMode(getContext(), true, false);
                Toast.makeText(getContext(), "已切换为模拟模式（重启生效）", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.rb_real) {
                HardwareClientFactory.setMockMode(getContext(), false, false);
                Toast.makeText(getContext(), "已切换为真实模式（重启生效）", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.rb_fro) {
                HardwareClientFactory.setMockMode(getContext(), false, true);
                Toast.makeText(getContext(), "已切换为Fro模式（重启生效）", Toast.LENGTH_SHORT).show();
            }
        });

        // —— 服务器配置 ——
        etIp   = view.findViewById(R.id.et_server_ip);
        etPort = view.findViewById(R.id.et_server_port);
        etIp.setText(HardwareClientFactory.getServerIp(getContext()));
        etPort.setText(String.valueOf(HardwareClientFactory.getServerPort(getContext())));

        view.findViewById(R.id.btn_save_server).setOnClickListener(v -> {
            String ip = etIp.getText().toString().trim();
            int port;
            try { port = Integer.parseInt(etPort.getText().toString().trim()); }
            catch (NumberFormatException e) { port = 8080; }
            HardwareClientFactory.setServerConfig(getContext(), ip, port);
            Toast.makeText(getContext(), "服务器配置已保存", Toast.LENGTH_SHORT).show();
        });

        // —— 大棚信息列表 ——
        layoutGhList = view.findViewById(R.id.layout_gh_list);
        loadGhEditRows();
        // 保存按钮靠 layout 自带

        // —— 数据备份 ——
        view.findViewById(R.id.btn_backup_db).setOnClickListener(v -> backupDatabase());
        view.findViewById(R.id.btn_restore_db).setOnClickListener(v -> restoreDatabase());

        // —— 关于 ——
        TextView tvAbout = view.findViewById(R.id.tv_about);
        tvAbout.setText("智能花卉大棚种植系统 v1.0\n\n" +
                "技术栈: Java / Android SDK 34\n" +
                "数据库: SQLite (GreenhouseDB v4)\n" +
                "通信: TCP Socket 二进制协议\n\n" +
                "© 2026 智能花卉大棚项目组");

        return view;
    }

    // ========== 大棚信息编辑 ==========

    private final EditText[] ghNameEdits = new EditText[8];
    private final EditText[] ghCropEdits = new EditText[8];
    private final EditText[] ghAreaEdits = new EditText[8];

    private void loadGhEditRows() {
        layoutGhList.removeAllViews();
        new Thread(() -> {
            // Read greenhouse names from DB
            android.database.sqlite.SQLiteDatabase db =
                    dbHelper.getReadableDatabase();
            android.database.Cursor c = db.rawQuery(
                    "SELECT gh_name, crop_type, area FROM greenhouse_info ORDER BY id", null);
            final String[] names = new String[8];
            final String[] crops = new String[8];
            final float[] areas = new float[8];
            int idx = 0;
            while (c.moveToNext() && idx < 8) {
                names[idx] = c.getString(0);
                crops[idx] = c.getString(1);
                areas[idx] = c.getFloat(2);
                idx++;
            }
            c.close();

            mainHandler.post(() -> {
                if (!isAdded()) return;
                for (int i = 0; i < 8; i++) {
                    View row = LayoutInflater.from(getContext())
                            .inflate(R.layout.item_gh_edit, layoutGhList, false);
                    TextView label = row.findViewById(R.id.tv_gh_label);
                    EditText etName = row.findViewById(R.id.et_gh_name);
                    EditText etCrop = row.findViewById(R.id.et_gh_crop);
                    EditText etArea = row.findViewById(R.id.et_gh_area);

                    label.setText("GH-" + String.format("%03d", i + 1));
                    etName.setText(names[i] != null ? names[i] : "花卉大棚" + (i + 1));
                    etCrop.setText(crops[i] != null ? crops[i] : "玫瑰");
                    etArea.setText(String.valueOf(areas[i] > 0 ? (int) areas[i] : 500));

                    ghNameEdits[i] = etName;
                    ghCropEdits[i] = etCrop;
                    ghAreaEdits[i] = etArea;
                    layoutGhList.addView(row);
                }
            });
        }).start();
    }

    private void saveGhEdits() {
        new Thread(() -> {
            android.database.sqlite.SQLiteDatabase db =
                    dbHelper.getWritableDatabase();
            for (int i = 0; i < 8; i++) {
                String name = ghNameEdits[i] != null
                        ? ghNameEdits[i].getText().toString().trim() : "";
                String crop = ghCropEdits[i] != null
                        ? ghCropEdits[i].getText().toString().trim() : "";
                float area = 500;
                if (ghAreaEdits[i] != null) {
                    try { area = Float.parseFloat(ghAreaEdits[i].getText().toString().trim()); }
                    catch (NumberFormatException ignored) {}
                }
                android.content.ContentValues cv = new android.content.ContentValues();
                cv.put("gh_name", name);
                cv.put("crop_type", crop);
                cv.put("area", area);
                db.update("greenhouse_info", cv, "id=?", new String[]{String.valueOf(i + 1)});
            }
            mainHandler.post(() -> {
                Toast.makeText(getContext(), "大棚信息已保存", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    // ========== 数据库备份 ==========

    private void backupDatabase() {
        new Thread(() -> {
            try {
                File dbFile = getContext().getDatabasePath("greenhouse.db");
                if (!dbFile.exists()) {
                    mainHandler.post(() -> Toast.makeText(getContext(),
                            "数据库文件不存在", Toast.LENGTH_SHORT).show());
                    return;
                }
                File dir = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS), "GreenHouse");
                if (!dir.exists()) dir.mkdirs();
                String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                        .format(new Date());
                File bak = new File(dir, "greenhouse_backup_" + ts + ".db");

                copyFile(dbFile, bak);

                mainHandler.post(() -> {
                    Toast.makeText(getContext(),
                            "备份成功: " + bak.getAbsolutePath(), Toast.LENGTH_LONG).show();
                    // share via FileProvider if needed
                });
            } catch (IOException e) {
                mainHandler.post(() -> Toast.makeText(getContext(),
                        "备份失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void restoreDatabase() {
        new AlertDialog.Builder(getContext())
                .setTitle("数据库恢复")
                .setMessage("将从 Downloads/GreenHouse/ 目录下选择最近的备份文件恢复。\n" +
                        "当前数据库将被覆盖。确定继续吗？")
                .setPositiveButton("确定", (d, w) -> {
                    new Thread(() -> {
                        try {
                            File dir = new File(Environment.getExternalStoragePublicDirectory(
                                    Environment.DIRECTORY_DOWNLOADS), "GreenHouse");
                            File[] files = dir.listFiles((f, n) -> n.endsWith(".db"));
                            if (files == null || files.length == 0) {
                                mainHandler.post(() -> Toast.makeText(getContext(),
                                        "未找到备份文件", Toast.LENGTH_SHORT).show());
                                return;
                            }
                            // 最近的
                            File latest = files[0];
                            for (File f : files) {
                                if (f.lastModified() > latest.lastModified()) latest = f;
                            }
                            File dbFile = getContext().getDatabasePath("greenhouse.db");
                            copyFile(latest, dbFile);
                            mainHandler.post(() -> Toast.makeText(getContext(),
                                    "恢复成功，请重启应用", Toast.LENGTH_SHORT).show());
                        } catch (IOException e) {
                            mainHandler.post(() -> Toast.makeText(getContext(),
                                    "恢复失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        }
                    }).start();
                })
                .setNegativeButton("取消", null).show();
    }

    private void copyFile(File src, File dst) throws IOException {
        try (FileInputStream fis = new FileInputStream(src);
             FileOutputStream fos = new FileOutputStream(dst)) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = fis.read(buf)) != -1) fos.write(buf, 0, n);
        }
    }
}
