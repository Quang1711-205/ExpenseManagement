package com.example.expensemanagement;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import database.DatabaseHelper;
import database.UserDAO;
import models.User;

public class RegisterActivity extends AppCompatActivity {

    // UI components
    private TextInputLayout tilFullName, tilUsername, tilEmail, tilPassword, tilConfirmPassword;
    private TextInputEditText etFullName, etUsername, etEmail, etPassword, etConfirmPassword;
    private Button btnRegister;
    private TextView tvBackToLogin;
    private ProgressBar progressBar;

    // Database
    private UserDAO userDAO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize views
        initViews();

        // Initialize components
        initializeComponents();

        // Set up click listeners
        setupClickListeners();
    }

    private void initViews() {
        tilFullName = findViewById(R.id.til_full_name);
        tilUsername = findViewById(R.id.til_username);
        tilEmail = findViewById(R.id.til_email);
        tilPassword = findViewById(R.id.til_password);
        tilConfirmPassword = findViewById(R.id.til_confirm_password);

        etFullName = findViewById(R.id.et_full_name);
        etUsername = findViewById(R.id.et_username);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);

        btnRegister = findViewById(R.id.btn_register);
        tvBackToLogin = findViewById(R.id.tv_back_to_login);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void initializeComponents() {
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        userDAO = new UserDAO(dbHelper);
    }

    private void setupClickListeners() {
        btnRegister.setOnClickListener(v -> performRegister());

        tvBackToLogin.setOnClickListener(v -> {
            finish(); // Quay lại màn hình login
        });
    }

    private void performRegister() {
        String fullName = etFullName.getText().toString().trim();
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // Clear previous errors
        clearErrors();

        // Validate input
        if (!validateInput(fullName, username, email, password, confirmPassword)) {
            return;
        }

        // Show loading
        showLoading(true);

        // Perform registration in background thread
        new Thread(() -> {
            try {
                // Simulate network delay
                Thread.sleep(1000);

                // Check if username already exists
                boolean usernameExists = userDAO.isUsernameExists(username);

                runOnUiThread(() -> {
                    showLoading(false);

                    if (usernameExists) {
                        tilUsername.setError("Tên đăng nhập đã tồn tại");
                        etUsername.requestFocus();
                    } else {
                        // Create new user
                        User newUser = new User(username, password, email, fullName);
                        long result = userDAO.insertUser(newUser);

                        if (result != -1) {
                            handleRegisterSuccess();
                        } else {
                            handleRegisterFailure();
                        }
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

    private boolean validateInput(String fullName, String username, String email, String password, String confirmPassword) {
        boolean isValid = true;

        // Validate full name
        if (TextUtils.isEmpty(fullName)) {
            tilFullName.setError("Vui lòng nhập họ tên");
            if (isValid) etFullName.requestFocus();
            isValid = false;
        } else if (fullName.length() < 2) {
            tilFullName.setError("Họ tên phải có ít nhất 2 ký tự");
            if (isValid) etFullName.requestFocus();
            isValid = false;
        }

        // Validate username
        if (TextUtils.isEmpty(username)) {
            tilUsername.setError("Vui lòng nhập tên đăng nhập");
            if (isValid) etUsername.requestFocus();
            isValid = false;
        } else if (username.length() < 3) {
            tilUsername.setError("Tên đăng nhập phải có ít nhất 3 ký tự");
            if (isValid) etUsername.requestFocus();
            isValid = false;
        } else if (!username.matches("^[a-zA-Z0-9_]+$")) {
            tilUsername.setError("Tên đăng nhập chỉ được chứa chữ, số và dấu gạch dưới");
            if (isValid) etUsername.requestFocus();
            isValid = false;
        }

        // Validate email
        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("Vui lòng nhập email");
            if (isValid) etEmail.requestFocus();
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Email không hợp lệ");
            if (isValid) etEmail.requestFocus();
            isValid = false;
        }

        // Validate password
        if (TextUtils.isEmpty(password)) {
            tilPassword.setError("Vui lòng nhập mật khẩu");
            if (isValid) etPassword.requestFocus();
            isValid = false;
        } else if (password.length() < 6) {
            tilPassword.setError("Mật khẩu phải có ít nhất 6 ký tự");
            if (isValid) etPassword.requestFocus();
            isValid = false;
        }

        // Validate confirm password
        if (TextUtils.isEmpty(confirmPassword)) {
            tilConfirmPassword.setError("Vui lòng xác nhận mật khẩu");
            if (isValid) etConfirmPassword.requestFocus();
            isValid = false;
        } else if (!password.equals(confirmPassword)) {
            tilConfirmPassword.setError("Mật khẩu xác nhận không khớp");
            if (isValid) etConfirmPassword.requestFocus();
            isValid = false;
        }

        return isValid;
    }

    private void clearErrors() {
        tilFullName.setError(null);
        tilUsername.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!show);
        btnRegister.setText(show ? "Đang đăng ký..." : "Đăng ký");

        // Disable input fields during registration
        etFullName.setEnabled(!show);
        etUsername.setEnabled(!show);
        etEmail.setEnabled(!show);
        etPassword.setEnabled(!show);
        etConfirmPassword.setEnabled(!show);
    }

    private void handleRegisterSuccess() {
        Toast.makeText(this, "Đăng ký thành công! Vui lòng đăng nhập.", Toast.LENGTH_LONG).show();

        // Navigate back to login with the username filled
        Intent intent = new Intent();
        intent.putExtra("registered_username", etUsername.getText().toString().trim());
        setResult(RESULT_OK, intent);
        finish();
    }

    private void handleRegisterFailure() {
        Toast.makeText(this, "Đăng ký thất bại. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}