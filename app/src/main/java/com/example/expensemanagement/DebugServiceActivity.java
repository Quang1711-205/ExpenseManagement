package com.example.expensemanagement;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class DebugServiceActivity extends AppCompatActivity {

    private static final String TAG = "DebugServiceActivity";

    private ServiceManager serviceManager;
    private TextView tvStatus;
    private Button btnStart, btnStop, btnToggle, btnCheck, btnTest;
    private ScrollView scrollView;
    private Handler handler;
    private Runnable statusUpdateRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "DebugServiceActivity created");

        createUI();
        setupServiceManager();
        setupAutoRefresh();
    }

    private void createUI() {
        // Tạo UI programmatically để tránh lỗi layout
        ScrollView scrollView = new ScrollView(this);

        // Container chính
        android.widget.LinearLayout mainLayout = new android.widget.LinearLayout(this);
        mainLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
        mainLayout.setPadding(50, 50, 50, 50);

        // Title
        TextView title = new TextView(this);
        title.setText("Debug Service Manager");
        title.setTextSize(20);
        title.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
        title.setPadding(0, 0, 0, 30);
        mainLayout.addView(title);

        // Status TextView
        tvStatus = new TextView(this);
        tvStatus.setText("Đang tải trạng thái...");
        tvStatus.setTextSize(14);
        tvStatus.setPadding(20, 20, 20, 20);
        tvStatus.setBackgroundColor(0xFFEEEEEE);
        mainLayout.addView(tvStatus);

        // Buttons
        btnStart = new Button(this);
        btnStart.setText("Start Service");
        btnStart.setOnClickListener(v -> startService());
        mainLayout.addView(btnStart);

        btnStop = new Button(this);
        btnStop.setText("Stop Service");
        btnStop.setOnClickListener(v -> stopService());
        mainLayout.addView(btnStop);

        btnToggle = new Button(this);
        btnToggle.setText("Toggle Service");
        btnToggle.setOnClickListener(v -> toggleService());
        mainLayout.addView(btnToggle);

        btnCheck = new Button(this);
        btnCheck.setText("Check Status");
        btnCheck.setOnClickListener(v -> updateStatus());
        mainLayout.addView(btnCheck);

        btnTest = new Button(this);
        btnTest.setText("Test Notification");
        btnTest.setOnClickListener(v -> testNotification());
        mainLayout.addView(btnTest);

        // Force buttons
        Button btnForceStart = new Button(this);
        btnForceStart.setText("Force Start");
        btnForceStart.setOnClickListener(v -> forceStart());
        mainLayout.addView(btnForceStart);

        Button btnForceStop = new Button(this);
        btnForceStop.setText("Force Stop");
        btnForceStop.setOnClickListener(v -> forceStop());
        mainLayout.addView(btnForceStop);

        scrollView.addView(mainLayout);
        setContentView(scrollView);

        Log.d(TAG, "UI created successfully");
    }

    private void setupServiceManager() {
        try {
            serviceManager = new ServiceManager(this);
            Log.d(TAG, "ServiceManager initialized");
            updateStatus();
        } catch (Exception e) {
            Log.e(TAG, "Error setting up ServiceManager: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupAutoRefresh() {
        handler = new Handler(Looper.getMainLooper());
        statusUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                updateStatus();
                handler.postDelayed(this, 5000); // Update mỗi 5 giây
            }
        };

        // Bắt đầu auto refresh
        handler.postDelayed(statusUpdateRunnable, 2000);
    }

    private void startService() {
        Log.d(TAG, "Manual start service requested");
        try {
            boolean success = serviceManager.startBackgroundService();
            String message = success ? "Service started successfully" : "Failed to start service";
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Start service result: " + success);
            updateStatus();
        } catch (Exception e) {
            Log.e(TAG, "Error starting service: " + e.getMessage());
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void stopService() {
        Log.d(TAG, "Manual stop service requested");
        try {
            boolean success = serviceManager.stopBackgroundService();
            String message = success ? "Service stopped successfully" : "Failed to stop service";
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Stop service result: " + success);
            updateStatus();
        } catch (Exception e) {
            Log.e(TAG, "Error stopping service: " + e.getMessage());
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleService() {
        Log.d(TAG, "Toggle service requested");
        try {
            boolean success = serviceManager.toggleService();
            String status = serviceManager.isServiceEnabled() ? "enabled" : "disabled";
            String message = "Service " + status + ". Result: " + success;
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Toggle service result: " + message);
            updateStatus();
        } catch (Exception e) {
            Log.e(TAG, "Error toggling service: " + e.getMessage());
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void forceStart() {
        Log.d(TAG, "Force start requested");
        try {
            boolean success = serviceManager.forceStartService();
            String message = success ? "Force start successful" : "Force start failed";
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            updateStatus();
        } catch (Exception e) {
            Log.e(TAG, "Error in force start: " + e.getMessage());
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void forceStop() {
        Log.d(TAG, "Force stop requested");
        try {
            boolean success = serviceManager.forceStopService();
            String message = success ? "Force stop successful" : "Force stop failed";
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            updateStatus();
        } catch (Exception e) {
            Log.e(TAG, "Error in force stop: " + e.getMessage());
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void testNotification() {
        Log.d(TAG, "Test notification requested");
        try {
            NotificationHelper notificationHelper = new NotificationHelper(this);
            notificationHelper.showTestNotification();
            Toast.makeText(this, "Test notification sent", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error sending test notification: " + e.getMessage());
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateStatus() {
        try {
            String statusText = serviceManager.getServiceStatus();
            String timestamp = java.text.DateFormat.getTimeInstance().format(new java.util.Date());
            String fullStatus = "Last Updated: " + timestamp + "\n\n" + statusText;

            tvStatus.setText(fullStatus);
            Log.d(TAG, "Status updated: " + statusText.replace("\n", " | "));
        } catch (Exception e) {
            Log.e(TAG, "Error updating status: " + e.getMessage());
            tvStatus.setText("Error updating status: " + e.getMessage());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null && statusUpdateRunnable != null) {
            handler.removeCallbacks(statusUpdateRunnable);
        }
        Log.d(TAG, "DebugServiceActivity destroyed");
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
        Log.d(TAG, "DebugServiceActivity resumed");
    }
}