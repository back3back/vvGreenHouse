package com.example.vvgreenhouse.activity;

import android.os.Bundle;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.example.vvgreenhouse.R;
import com.example.vvgreenhouse.fragment.*;
import com.example.vvgreenhouse.hardware.MockHardwareClient;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * 主界面 —— 底部导航 + Fragment 容器
 */
public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private TextView tvUserInfo, tvConnectionStatus;

    private String username, realName, role;
    private MockHardwareClient hardwareClient;

    // 当前 Fragment 缓存
    private Fragment envFragment, ctrlFragment, mgmtFragment, secFragment, settingsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 接收登录传递的参数
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            username = extras.getString("username", "");
            realName = extras.getString("real_name", "");
            role = extras.getString("role", "");
        }

        initViews();
        initHardware();
        setupBottomNav();

        // 默认显示环境监测页
        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_environment);
        }
    }

    private void initViews() {
        bottomNav = findViewById(R.id.bottom_navigation);
        tvUserInfo = findViewById(R.id.tv_user_info);
        tvConnectionStatus = findViewById(R.id.tv_connection_status);

        // 用户信息显示
        String roleDisplay = "";
        switch (role) {
            case "boss": roleDisplay = "老板"; break;
            case "admin": roleDisplay = "管理员"; break;
            case "gardener": roleDisplay = "园艺师"; break;
        }
        tvUserInfo.setText(roleDisplay + ": " + realName);
    }

    /** 初始化模拟硬件连接 */
    private void initHardware() {
        hardwareClient = new MockHardwareClient();
        boolean ok = hardwareClient.connect("127.0.0.1", 8899);
        tvConnectionStatus.setText(ok ? "● 已连接(模拟)" : "○ 未连接");
    }

    private void setupBottomNav() {
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_environment) {
                showFragment(getEnvFragment());
                return true;
            } else if (id == R.id.nav_control) {
                showFragment(getControlFragment());
                return true;
            } else if (id == R.id.nav_management) {
                showFragment(getMgmtFragment());
                return true;
            } else if (id == R.id.nav_security) {
                showFragment(getSecFragment());
                return true;
            } else if (id == R.id.nav_settings) {
                showFragment(getSettingsFragment());
                return true;
            }
            return false;
        });
    }

    private void showFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    // ========== Fragment 懒加载 ==========

    private Fragment getEnvFragment() {
        if (envFragment == null) {
            envFragment = EnvironmentFragment.newInstance(hardwareClient);
        }
        return envFragment;
    }

    private Fragment getControlFragment() {
        if (ctrlFragment == null) {
            ctrlFragment = ControlFragment.newInstance(hardwareClient);
        }
        return ctrlFragment;
    }

    private Fragment getMgmtFragment() {
        if (mgmtFragment == null) {
            mgmtFragment = PlaceholderFragment.newInstance("日常管理");
        }
        return mgmtFragment;
    }

    private Fragment getSecFragment() {
        if (secFragment == null) {
            secFragment = PlaceholderFragment.newInstance("安防预警");
        }
        return secFragment;
    }

    private Fragment getSettingsFragment() {
        if (settingsFragment == null) {
            settingsFragment = PlaceholderFragment.newInstance("系统设置");
        }
        return settingsFragment;
    }
}
