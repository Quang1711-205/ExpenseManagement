package com.example.expensemanagement;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import java.util.List;

public class ServiceManager {

    private static final String TAG = "ServiceManager";
    private static final String PREFS_NAME = "ServicePrefs";
    private static final String KEY_SERVICE_ENABLED = "serviceEnabled";

    private Context context;
    private SharedPreferences servicePrefs;

    public ServiceManager(Context context) {
        this.context = context;
        this.servicePrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Bắt đầu background service
     */
    public boolean startBackgroundService() {
        try {
            Log.d(TAG, "Starting SimpleBackgroundService...");

            // Dừng service cũ trước (nếu có)
            stopBackgroundService();

            Intent serviceIntent = new Intent(context, SimpleBackgroundService.class);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
                Log.d(TAG, "Started as foreground service");
            } else {
                context.startService(serviceIntent);
                Log.d(TAG, "Started as background service");
            }

            // Lưu trạng thái service
            servicePrefs.edit()
                    .putBoolean(KEY_SERVICE_ENABLED, true)
                    .apply();

            Log.d(TAG, "SimpleBackgroundService started successfully");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error starting SimpleBackgroundService: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Dừng background service
     */
    public boolean stopBackgroundService() {
        try {
            Log.d(TAG, "Stopping SimpleBackgroundService...");

            Intent serviceIntent = new Intent(context, SimpleBackgroundService.class);
            boolean stopped = context.stopService(serviceIntent);

            // Lưu trạng thái service
            servicePrefs.edit()
                    .putBoolean(KEY_SERVICE_ENABLED, false)
                    .apply();

            Log.d(TAG, "SimpleBackgroundService stopped: " + stopped);
            return true; // Trả về true vì đã save trạng thái

        } catch (Exception e) {
            Log.e(TAG, "Error stopping SimpleBackgroundService: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Kiểm tra service có được enable không (theo settings)
     */
    public boolean isServiceEnabled() {
        boolean enabled = servicePrefs.getBoolean(KEY_SERVICE_ENABLED, false);
        Log.d(TAG, "Service enabled status: " + enabled);
        return enabled;
    }

    /**
     * Kiểm tra service có thực sự đang chạy không
     */
    public boolean isServiceActuallyRunning() {
        try {
            ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (manager != null) {
                List<ActivityManager.RunningServiceInfo> services = manager.getRunningServices(Integer.MAX_VALUE);
                for (ActivityManager.RunningServiceInfo service : services) {
                    if (SimpleBackgroundService.class.getName().equals(service.service.getClassName())) {
                        Log.d(TAG, "Service is actually running");
                        return true;
                    }
                }
            }
            Log.d(TAG, "Service is not actually running");
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error checking if service is running: " + e.getMessage());
            return false;
        }
    }

    /**
     * Restart service (dùng khi app được update hoặc restart)
     */
    public boolean restartService() {
        Log.d(TAG, "Restarting SimpleBackgroundService...");

        if (isServiceEnabled()) {
            Log.d(TAG, "Service was enabled, restarting...");

            // Dừng service
            stopBackgroundService();

            // Delay một chút trước khi start lại
            try {
                Thread.sleep(2000); // 2 giây
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.w(TAG, "Restart delay interrupted");
            }

            // Start lại
            boolean started = startBackgroundService();
            Log.d(TAG, "Service restart result: " + started);
            return started;
        } else {
            Log.d(TAG, "Service was not enabled, skip restart");
            return false;
        }
    }

    /**
     * Toggle service on/off
     */
    public boolean toggleService() {
        Log.d(TAG, "Toggling service...");

        if (isServiceEnabled()) {
            Log.d(TAG, "Service is enabled, stopping...");
            return stopBackgroundService();
        } else {
            Log.d(TAG, "Service is disabled, starting...");
            return startBackgroundService();
        }
    }

    /**
     * Kiểm tra quyền cần thiết cho service
     */
    public boolean hasRequiredPermissions() {
        // Kiểm tra notification permission
        boolean hasNotificationPermission = NotificationPermissionHelper.hasNotificationPermission(context);
        Log.d(TAG, "Has notification permission: " + hasNotificationPermission);
        return hasNotificationPermission;
    }

    /**
     * Request permissions nếu cần
     */
    public void requestPermissionsIfNeeded() {
        if (!hasRequiredPermissions()) {
            Log.d(TAG, "Service needs notification permission");
            // Có thể show dialog hoặc redirect đến settings
        } else {
            Log.d(TAG, "Service has all required permissions");
        }
    }

    /**
     * Lấy trạng thái chi tiết của service
     */
    public String getServiceStatus() {
        boolean enabled = isServiceEnabled();
        boolean running = isServiceActuallyRunning();
        boolean hasPermissions = hasRequiredPermissions();

        StringBuilder status = new StringBuilder();
        status.append("Service Status:\n");
        status.append("- Enabled: ").append(enabled).append("\n");
        status.append("- Actually Running: ").append(running).append("\n");
        status.append("- Has Permissions: ").append(hasPermissions).append("\n");

        if (enabled && !running) {
            status.append("- Issue: Service enabled but not running!");
        } else if (!enabled && running) {
            status.append("- Issue: Service running but not enabled!");
        } else if (enabled && running && hasPermissions) {
            status.append("- Status: All good!");
        }

        String result = status.toString();
        Log.d(TAG, result);
        return result;
    }

    /**
     * Force start service (bỏ qua trạng thái enabled)
     */
    public boolean forceStartService() {
        Log.d(TAG, "Force starting service...");

        try {
            Intent serviceIntent = new Intent(context, SimpleBackgroundService.class);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }

            // Không save trạng thái enabled = true
            Log.d(TAG, "Force start completed");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error in force start: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Force stop service (bỏ qua trạng thái enabled)
     */
    public boolean forceStopService() {
        Log.d(TAG, "Force stopping service...");

        try {
            Intent serviceIntent = new Intent(context, SimpleBackgroundService.class);
            context.stopService(serviceIntent);

            // Không save trạng thái enabled = false
            Log.d(TAG, "Force stop completed");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error in force stop: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}