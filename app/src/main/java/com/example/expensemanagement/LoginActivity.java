package com.example.expensemanagement;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;

import database.DatabaseHelper;
import database.UserDAO;

public class LoginActivity extends AppCompatActivity {

    // UI components
    private TextInputLayout tilUsername, tilPassword;
    private TextInputEditText etUsername, etPassword;
    private CheckBox cbRememberMe;
    private Button btnLogin;
    private TextView tvForgotPassword, tvCreateAccount;
    private ProgressBar progressBar;

    // Database and preferences
    private UserDAO userDAO;
    private SharedPreferences sharedPreferences;

    private static final String PREFS_NAME = "MoneyMasterPrefs";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_REMEMBER_ME = "rememberMe";

    // Activity Result Launcher for Register Activity
    private ActivityResultLauncher<Intent> registerActivityLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize activity result launcher
        initializeActivityLauncher();

        // Initialize views
        initViews();

        // Initialize components
        initializeComponents();

        // Check if user is already logged in
        if (isUserLoggedIn()) {
            navigateToDashboard();
            return;
        }

        // Load saved credentials if remember me was checked
        loadSavedCredentials();

        // Set up click listeners
        setupClickListeners();
    }

    private void initializeActivityLauncher() {
        registerActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        // Get the registered username from register activity
                        String registeredUsername = result.getData().getStringExtra("registered_username");
                        if (registeredUsername != null) {
                            etUsername.setText(registeredUsername);
                            etPassword.requestFocus();
                        }
                    }
                }
        );
    }

    private void initViews() {
        tilUsername = findViewById(R.id.til_username);
        tilPassword = findViewById(R.id.til_password);
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        cbRememberMe = findViewById(R.id.cb_remember_me);
        btnLogin = findViewById(R.id.btn_login);
        tvForgotPassword = findViewById(R.id.tv_forgot_password);
        tvCreateAccount = findViewById(R.id.tv_create_account);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void initializeComponents() {
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        userDAO = new UserDAO(dbHelper);
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
    }

    private boolean isUserLoggedIn() {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    private void loadSavedCredentials() {
        boolean rememberMe = sharedPreferences.getBoolean(KEY_REMEMBER_ME, false);
        if (rememberMe) {
            String savedUsername = sharedPreferences.getString(KEY_USERNAME, "");
            etUsername.setText(savedUsername);
            cbRememberMe.setChecked(true);
        }
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> performLogin());

        tvForgotPassword.setOnClickListener(v -> {
            // TODO: Implement forgot password functionality
            Toast.makeText(this, "Chức năng đang được phát triển", Toast.LENGTH_SHORT).show();
        });

        tvCreateAccount.setOnClickListener(v -> {
            // Navigate to registration screen
            Intent intent = new Intent(this, RegisterActivity.class);
            registerActivityLauncher.launch(intent);
        });
    }

    private void performLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Clear previous errors
        tilUsername.setError(null);
        tilPassword.setError(null);

        // Validate input
        if (TextUtils.isEmpty(username)) {
            tilUsername.setError("Vui lòng nhập tên đăng nhập");
            etUsername.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            tilPassword.setError("Vui lòng nhập mật khẩu");
            etPassword.requestFocus();
            return;
        }

        // Show loading
        showLoading(true);

        // Simulate login process (replace with actual authentication)
        new Thread(() -> {
            try {
                // Simulate network delay
                Thread.sleep(1500);

                // Check credentials
                boolean isValidUser = userDAO.validateUser(username, password);

                runOnUiThread(() -> {
                    showLoading(false);

                    if (isValidUser) {
                        handleLoginSuccess(username);
                    } else {
                        handleLoginFailure();
                    }
                });

            } catch (InterruptedException e) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(this, "Đã xảy ra lỗi", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!show);
        btnLogin.setText(show ? "Đang đăng nhập..." : "Đăng nhập");
    }

    private void handleLoginSuccess(String username) {
        Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();

        // ✅ FIX: Get user info TRƯỚC KHI save
        int userId = userDAO.getUserId(username);

        if (userId == -1) {
            Log.e("LoginActivity", "❌ Cannot get userId for username: " + username);
            Toast.makeText(this, "Lỗi: Không thể lấy thông tin user", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("LoginActivity", "✅ Login success - UserID: " + userId + ", Username: " + username);

        // Save login state với userId
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putInt(KEY_USER_ID, userId); // ✅ QUAN TRỌNG: Lưu userId
        editor.putString(KEY_USERNAME, username);
        editor.putBoolean(KEY_REMEMBER_ME, cbRememberMe.isChecked());
        editor.apply();

        // Navigate to dashboard
        navigateToDashboard();
    }

    private void handleLoginFailure() {
        tilPassword.setError("Tên đăng nhập hoặc mật khẩu không đúng");
        etPassword.setText("");
        etPassword.requestFocus();
    }

    private void navigateToDashboard() {
        Intent intent = new Intent(this, DashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}