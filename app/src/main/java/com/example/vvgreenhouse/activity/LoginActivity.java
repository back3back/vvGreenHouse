package com.example.vvgreenhouse.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.example.vvgreenhouse.R;
import com.example.vvgreenhouse.database.GreenhouseDBHelper;
import com.example.vvgreenhouse.model.User;
import com.example.vvgreenhouse.utils.MD5Util;

/**
 * 登录界面
 *
 * 功能：
 * - 用户名 + 密码 + 角色选择
 * - MD5 加密比对
 * - 记住密码（SharedPreferences）
 * - 验证通过跳转 MainActivity
 */
public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private Spinner spinnerRole;
    private CheckBox cbRemember;
    private Button btnLogin;

    private GreenhouseDBHelper dbHelper;
    private SharedPreferences sp;

    // 角色列表（与 arrays.xml 的 role_values 一致）
    private static final String[] ROLE_VALUES = {"boss", "admin", "gardener"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        dbHelper = GreenhouseDBHelper.getInstance(this);
        sp = getSharedPreferences("login_prefs", MODE_PRIVATE);

        initViews();
        loadSavedCredentials();
        setupListeners();
    }

    private void initViews() {
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        spinnerRole = findViewById(R.id.spinner_role);
        cbRemember = findViewById(R.id.cb_remember);
        btnLogin = findViewById(R.id.btn_login);
    }

    /** 加载已保存的凭证 */
    private void loadSavedCredentials() {
        boolean remember = sp.getBoolean("remember", false);
        cbRemember.setChecked(remember);
        if (remember) {
            etUsername.setText(sp.getString("username", ""));
            etPassword.setText(sp.getString("password", ""));
            // 恢复上次选择的角色
            int roleIdx = sp.getInt("role_index", 1); // 默认 admin
            if (roleIdx < ROLE_VALUES.length) {
                spinnerRole.setSelection(roleIdx);
            }
        }
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> attemptLogin());
    }

    /** 执行登录验证 */
    private void attemptLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "请输入用户名和密码", Toast.LENGTH_SHORT).show();
            return;
        }

        // 禁用按钮防重复点击
        btnLogin.setEnabled(false);
        btnLogin.setText("登录中…");

        // 后台线程执行数据库操作
        new Thread(() -> {
            String md5Pwd = MD5Util.md5(password);
            User user = dbHelper.login(username, md5Pwd);

            new Handler(Looper.getMainLooper()).post(() -> {
                btnLogin.setEnabled(true);
                btnLogin.setText(R.string.login);

                if (user != null) {
                    // 保存凭证
                    saveCredentials(username, password);

                    Toast.makeText(LoginActivity.this,
                            "欢迎, " + user.getRealName() + "!", Toast.LENGTH_SHORT).show();

                    // 跳转主界面
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.putExtra("user_id", user.getId());
                    intent.putExtra("username", user.getUsername());
                    intent.putExtra("real_name", user.getRealName());
                    intent.putExtra("role", user.getRole());
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this,
                            "用户名或密码错误", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    /** 保存/清除记住密码 */
    private void saveCredentials(String username, String password) {
        SharedPreferences.Editor editor = sp.edit();
        if (cbRemember.isChecked()) {
            editor.putBoolean("remember", true);
            editor.putString("username", username);
            editor.putString("password", password);  // 明文保存仅用于"记住密码"，实际比对用MD5
            editor.putInt("role_index", spinnerRole.getSelectedItemPosition());
        } else {
            editor.clear();
        }
        editor.apply();
    }
}
