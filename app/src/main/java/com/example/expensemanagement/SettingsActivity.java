package com.example.expensemanagement;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class SettingsActivity extends AppCompatActivity {

    private ServiceManager serviceManager;
    private Switch switchBackgroundService;
    private Switch switchNotifications;
    private TextView tvServiceStatus;
    private TextView tvBatteryOptimization;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        initializeComponents();
        setupToolbar();
        setupServiceManager();
        setupEventListeners();
        updateUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }

    private void initializeComponents() {
        switchBackgroundService = findViewById(R.id.switch_background_service);
        switchNotifications = findViewById(R.id.switch_notifications);
        tvServiceStatus = findViewById(R.id.tv_service_status);
        tvBatteryOptimization = findViewById(R.id.tv_battery_optimization);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Cài đặt");
        }
    }

    private void setupServiceManager() {
        serviceManager = new ServiceManager(this);
    }

    private void setupEventListeners() {
        // Background service switch
        switchBackgroundService.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                enableBackgroundService();
            } else {
                disableBackgroundService();
            }
        });

        // Notifications switch
        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (!NotificationPermissionHelper.hasNotificationPermission(this)) {
                    // Request permission
                    NotificationPermissionHelper.requestNotificationPermission(this);
                } else {
                    updateUI();
                }
            } else {
                // Direct to settings to disable
                showNotificationSettingsDialog();
            }
        });

        // Battery optimization info
        tvBatteryOptimization.setOnClickListener(v -> showBatteryOptimizationDialog());
    }

    private void enableBackgroundService() {
        if (!serviceManager.hasRequiredPermissions()) {
            switchBackgroundService.setChecked(false);
            showPermissionRequiredDialog();
            return;
        }

        boolean success = serviceManager.startBackgroundService();
        if (success) {
            Toast.makeText(this, "Đã bật dịch vụ chạy nền", Toast.LENGTH_SHORT).show();
            updateUI();
        } else {
            switchBackgroundService.setChecked(false);
            Toast.makeText(this, "Không thể bật dịch vụ", Toast.LENGTH_SHORT).show();
        }
    }

    private void disableBackgroundService() {
        boolean success = serviceManager.stopBackgroundService();
        if (success) {
            Toast.makeText(this, "Đã tắt dịch vụ chạy nền", Toast.LENGTH_SHORT).show();
            updateUI();
        } else {
            switchBackgroundService.setChecked(true);
            Toast.makeText(this, "Không thể tắt dịch vụ", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateUI() {
        // Update service switch
        switchBackgroundService.setChecked(serviceManager.isServiceEnabled());

        // Update notification switch
        switchNotifications.setChecked(
                NotificationPermissionHelper.hasNotificationPermission(this));

        // Update service status
        if (serviceManager.isServiceEnabled()) {
            tvServiceStatus.setText("Dịch vụ đang chạy - Theo dõi chi tiêu tự động");
            tvServiceStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            tvServiceStatus.setText("Dịch vụ đã tắt - Không có nhắc nhở tự động");
            tvServiceStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }

        // Update battery optimization info
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            android.os.PowerManager pm = (android.os.PowerManager) getSystemService(POWER_SERVICE);
            if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                tvBatteryOptimization.setText("Tối ưu hóa pin: BẬT (có thể ảnh hưởng đến dịch vụ)");
                tvBatteryOptimization.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
            } else {
                tvBatteryOptimization.setText("Tối ưu hóa pin: TẮT (tốt cho dịch vụ)");
                tvBatteryOptimization.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            }
        }
    }

    private void showPermissionRequiredDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Cần quyền thông báo")
                .setMessage("Dịch vụ chạy nền cần quyền thông báo để gửi nhắc nhở.\n\n" +
                        "Vui lòng cấp quyền trước khi bật dịch vụ.")
                .setPositiveButton("Cấp quyền", (dialog, which) -> {
                    NotificationPermissionHelper.requestNotificationPermission(this);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showNotificationSettingsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Tắt thông báo")
                .setMessage("Để tắt thông báo, vui lòng vào Cài đặt hệ thống.")
                .setPositiveButton("Mở cài đặt", (dialog, which) -> {
                    NotificationPermissionHelper.openNotificationSettings(this);
                })
                .setNegativeButton("Hủy", (dialog, which) -> {
                    switchNotifications.setChecked(true);
                })
                .show();
    }

    private void showBatteryOptimizationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Tối ưu hóa pin")
                .setMessage("Để dịch vụ hoạt động ổn định, nên tắt tối ưu hóa pin cho app này.\n\n" +
                        "Điều này giúp app chạy nền mà không bị Android tự động tắt.")
                .setPositiveButton("Mở cài đặt pin", (dialog, which) -> {
                    openBatteryOptimizationSettings();
                })
                .setNegativeButton("Bỏ qua", null)
                .show();
    }

    private void openBatteryOptimizationSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } catch (Exception e) {
            // Fallback to general battery settings
            try {
                Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                startActivity(intent);
            } catch (Exception ex) {
                Toast.makeText(this, "Không thể mở cài đặt pin", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}