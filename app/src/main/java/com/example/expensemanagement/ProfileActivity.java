package com.example.expensemanagement;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

import database.DatabaseHelper;

public class ProfileActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private SQLiteDatabase database;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "MoneyMasterPrefs";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_USER_ID = "userId";

    // UI Components
    private Toolbar toolbar;
    private TextView tvUsername;
    private TextView tvEmail;
    private TextView tvMemberSince;
    private TextView tvTotalTransactions;
    private TextInputEditText etFullName;
    private TextInputEditText etEmail;
    private MaterialButton btnSaveChanges;
    private MaterialButton btnChangePassword;
    private MaterialCardView cardStats;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        initializeComponents();
        setupDatabase();
        setupToolbar();
        setupEventListeners();
        loadUserData();
    }

    private void initializeComponents() {
        toolbar = findViewById(R.id.toolbar);
        tvUsername = findViewById(R.id.tv_username);
        tvEmail = findViewById(R.id.tv_email);
        tvMemberSince = findViewById(R.id.tv_member_since);
        tvTotalTransactions = findViewById(R.id.tv_total_transactions);
        etFullName = findViewById(R.id.et_full_name);
        etEmail = findViewById(R.id.et_email);
        btnSaveChanges = findViewById(R.id.btn_save_changes);
        btnChangePassword = findViewById(R.id.btn_change_password);
        cardStats = findViewById(R.id.card_stats);
    }

    private void setupDatabase() {
        dbHelper = new DatabaseHelper(this);
        database = dbHelper.getReadableDatabase();
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Thông tin cá nhân");
        }
    }

    private void setupEventListeners() {
        if (btnSaveChanges != null) {
            btnSaveChanges.setOnClickListener(v -> saveChanges());
        }

        if (btnChangePassword != null) {
            btnChangePassword.setOnClickListener(v -> changePassword());
        }
    }

    private void loadUserData() {
        try {
            int userId = sharedPreferences.getInt(KEY_USER_ID, -1);
            if (userId == -1) return;

            String query = "SELECT * FROM users WHERE _id = ?";
            Cursor cursor = database.rawQuery(query, new String[]{String.valueOf(userId)});

            if (cursor.moveToFirst()) {
                String username = cursor.getString(cursor.getColumnIndexOrThrow("username"));
                String email = cursor.getString(cursor.getColumnIndexOrThrow("email"));
                String fullName = cursor.getString(cursor.getColumnIndexOrThrow("full_name"));
                long createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"));

                if (tvUsername != null) tvUsername.setText(username);
                if (tvEmail != null) tvEmail.setText(email != null ? email : "Chưa cập nhật");
                if (etFullName != null) etFullName.setText(fullName);
                if (etEmail != null) etEmail.setText(email);

                // Format member since date
                if (tvMemberSince != null) {
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
                    tvMemberSince.setText("Thành viên từ: " + sdf.format(new java.util.Date(createdAt)));
                }
            }
            cursor.close();

            // Load transaction count
            loadTransactionStats(userId);

        } catch (Exception e) {
            e.printStackTrace();
            showToast("Lỗi khi tải thông tin người dùng");
        }
    }

    private void loadTransactionStats(int userId) {
        try {
            String query = "SELECT COUNT(*) FROM transactions WHERE user_id = ?";
            Cursor cursor = database.rawQuery(query, new String[]{String.valueOf(userId)});

            if (cursor.moveToFirst()) {
                int totalTransactions = cursor.getInt(0);
                if (tvTotalTransactions != null) {
                    tvTotalTransactions.setText("Tổng giao dịch: " + totalTransactions);
                }
            }
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveChanges() {
        // Implementation for saving profile changes
        showToast("Chức năng lưu thay đổi đang được phát triển");
    }

    private void changePassword() {
        // Implementation for changing password
        showToast("Chức năng đổi mật khẩu đang được phát triển");
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (database != null && database.isOpen()) {
            database.close();
        }
    }
}