package com.example.expensemanagement;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "MoneyMasterPrefs";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_REMEMBER_ME = "rememberMe";
    private static final String KEY_USER_ID = "userId";

    private static final int ACTION_TOGGLE_SERVICE = 1001;
    private static final int ACTION_TEST_NOTIFICATION = 1002;
    private static final int ACTION_DEBUG_SERVICE = 1003;

    private ServiceManager serviceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ========= LOG ĐẦU TIÊN - QUAN TRỌNG NHẤT =========
        Log.d(TAG, "====== MAINACTIVITY onCreate() STARTED ======");
        System.out.println("MainActivity onCreate() - Console Log");

        try {
            Log.d(TAG, "Setting content view...");
            setContentView(R.layout.activity_main);
            Log.d(TAG, "Content view set successfully");
        } catch (Exception e) {
            Log.e(TAG, "ERROR setting content view: " + e.getMessage());
            e.printStackTrace();
            // Fallback - tạo UI đơn giản
            createFallbackUI();
        }

        try {
            Log.d(TAG, "Initializing SharedPreferences...");
            sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            Log.d(TAG, "SharedPreferences initialized");
        } catch (Exception e) {
            Log.e(TAG, "ERROR with SharedPreferences: " + e.getMessage());
            e.printStackTrace();
        }

        // Check if user is logged in
        if (!isUserLoggedIn()) {
            Log.d(TAG, "User not logged in, navigating to login");
            navigateToLogin();
            return;
        }

        Log.d(TAG, "User is logged in, proceeding with setup");

        try {
            setupUI();
            setupBackPressHandler();
            checkAndRequestNotificationPermission();

            // ✅ THÊM: Test service ngay lập tức để debug
            testServiceImmediately();

            Log.d(TAG, "====== MAINACTIVITY onCreate() COMPLETED ======");
        } catch (Exception e) {
            Log.e(TAG, "ERROR in MainActivity setup: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ✅ THÊM: Method test service ngay lập tức
    private void testServiceImmediately() {
        Log.d(TAG, "=== TESTING SERVICE IMMEDIATELY ===");

        try {
            // Delay 3 giây để UI load xong
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                Log.d(TAG, "Starting immediate service test...");

                if (serviceManager != null) {
                    String status = serviceManager.getServiceStatus();
                    Log.d(TAG, "Current service status: " + status);

                    // Force start service để test
                    Log.d(TAG, "Force starting service for test...");
                    boolean started = serviceManager.forceStartService();
                    Log.d(TAG, "Force start result: " + started);

                    Toast.makeText(this, "Service test started - Check Logcat", Toast.LENGTH_LONG).show();
                } else {
                    Log.e(TAG, "ServiceManager is null in immediate test");
                }
            }, 3000);

        } catch (Exception e) {
            Log.e(TAG, "Error in immediate service test: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createFallbackUI() {
        Log.d(TAG, "Creating fallback UI");
        TextView textView = new TextView(this);
        textView.setText("MainActivity Fallback Mode\n\nLayout file có thể bị lỗi.\nApp vẫn chạy được.");
        textView.setTextSize(16);
        textView.setPadding(50, 50, 50, 50);
        setContentView(textView);

        Toast.makeText(this, "MainActivity loaded in fallback mode", Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "MainActivity onStart()");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "MainActivity onResume()");

        // ✅ THÊM: Refresh service status khi resume
        if (serviceManager != null) {
            Log.d(TAG, "Refreshing service status on resume");
            String status = serviceManager.getServiceStatus();
            Log.d(TAG, "Service status on resume: " + status.replace("\n", " | "));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "MainActivity onPause()");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onRequestPermissionsResult called with requestCode: " + requestCode);

        if (requestCode == NotificationPermissionHelper.NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Notification permission granted");
                Toast.makeText(this, "Đã cấp quyền thông báo", Toast.LENGTH_SHORT).show();
                testNotification();

                // ✅ THÊM: Tự động start service khi được cấp quyền
                if (serviceManager != null && !serviceManager.isServiceEnabled()) {
                    Log.d(TAG, "Permission granted, auto-starting service");
                    serviceManager.startBackgroundService();
                }
            } else {
                Log.d(TAG, "Notification permission denied");
                Toast.makeText(this, "Chưa cấp quyền thông báo", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void checkAndRequestNotificationPermission() {
        Log.d(TAG, "Checking notification permission...");

        try {
            if (!NotificationPermissionHelper.hasNotificationPermission(this)) {
                Log.d(TAG, "No notification permission, showing dialog");
                showNotificationPermissionDialog();
            } else {
                Log.d(TAG, "Already have notification permission");
                testNotification();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking notification permission: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showNotificationPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Cần quyền thông báo")
                .setMessage("Ứng dụng cần quyền thông báo để:\n\n" +
                        "• Thông báo khi thêm giao dịch thành công\n" +
                        "• Cảnh báo khi số dư thấp\n" +
                        "• Dịch vụ chạy nền hoạt động\n\n" +
                        "Bạn có muốn cấp quyền không?")
                .setPositiveButton("Đồng ý", (dialog, which) -> {
                    Log.d(TAG, "User agreed to grant permission");
                    NotificationPermissionHelper.requestNotificationPermission(this);
                })
                .setNegativeButton("Bỏ qua", (dialog, which) -> {
                    Log.d(TAG, "User declined notification permission");
                    Toast.makeText(this, "Thông báo sẽ không hoạt động", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .setCancelable(false)
                .show();
    }

    private void testNotification() {
        try {
            Log.d(TAG, "Creating NotificationHelper...");
            NotificationHelper notificationHelper = new NotificationHelper(this);
            notificationHelper.showTestNotification();
            Log.d(TAG, "Test notification sent successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error sending test notification: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupUI() {
        Log.d(TAG, "Setting up UI...");

        try {
            // Setup toolbar
            Toolbar toolbar = findViewById(R.id.toolbar);
            if (toolbar != null) {
                setSupportActionBar(toolbar);
                Log.d(TAG, "Toolbar set successfully");
            } else {
                Log.w(TAG, "Toolbar not found in layout");
            }

            // Welcome message
            TextView tvWelcome = findViewById(R.id.tv_welcome);
            if (tvWelcome != null) {
                String username = sharedPreferences.getString(KEY_USERNAME, "User");
                tvWelcome.setText("Chào mừng, " + username + "!");
                Log.d(TAG, "Welcome message set for user: " + username);
            } else {
                Log.w(TAG, "tv_welcome not found in layout");
            }

            Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();

            setupBackgroundService();
            Log.d(TAG, "UI setup completed");

        } catch (Exception e) {
            Log.e(TAG, "Error in setupUI: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupBackgroundService() {
        Log.d(TAG, "Setting up background service...");

        try {
            serviceManager = new ServiceManager(this);
            Log.d(TAG, "ServiceManager created");

            // ✅ SỬA: Luôn log service status
            String initialStatus = serviceManager.getServiceStatus();
            Log.d(TAG, "Initial service status: " + initialStatus.replace("\n", " | "));

            // Kiểm tra quyền trước khi start service
            if (serviceManager.hasRequiredPermissions()) {
                Log.d(TAG, "Has required permissions");

                // Auto-start service nếu user đã enable trước đó
                if (serviceManager.isServiceEnabled()) {
                    Log.d(TAG, "Service was enabled, restarting...");
                    boolean restarted = serviceManager.restartService();
                    Log.d(TAG, "Background service restart result: " + restarted);
                } else {
                    Log.d(TAG, "Service not enabled, showing dialog");
                    // Lần đầu sử dụng, hỏi user có muốn enable service không
                    showEnableServiceDialog();
                }
            } else {
                Log.d(TAG, "No permission for background service, will request permission");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up background service: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showEnableServiceDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Dịch vụ chạy nền")
                .setMessage("Bạn có muốn bật dịch vụ chạy nền để:\n\n" +
                        "• Nhắc nhở ghi chép chi tiêu hàng ngày\n" +
                        "• Cảnh báo khi vượt ngân sách\n" +
                        "• Thông báo số dư thấp\n" +
                        "• Tóm tắt chi tiêu hàng tuần\n\n" +
                        "Dịch vụ sẽ chạy ngầm và tiêu tốn ít pin.")
                .setPositiveButton("Bật dịch vụ", (dialog, which) -> {
                    Log.d(TAG, "User wants to enable service");
                    boolean started = serviceManager.startBackgroundService();
                    if (started) {
                        Toast.makeText(this, "Đã bật dịch vụ chạy nền", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Background service enabled by user: SUCCESS");
                    } else {
                        Toast.makeText(this, "Không thể bật dịch vụ", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Failed to start background service");
                    }
                })
                .setNegativeButton("Bỏ qua", (dialog, which) -> {
                    Toast.makeText(this, "Bạn có thể bật dịch vụ trong menu", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "User declined service");
                    dialog.dismiss();
                })
                .setCancelable(true)
                .show();
    }

    private void setupBackPressHandler() {
        // Handle back button press with new API
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Show exit confirmation
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Thoát ứng dụng")
                        .setMessage("Bạn có muốn thoát Money Master Pro?")
                        .setPositiveButton("Thoát", (dialog, which) -> {
                            Log.d(TAG, "User exiting app");
                            finish();
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
            }
        });
    }

    private boolean isUserLoggedIn() {
        boolean isLoggedIn = sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
        Log.d(TAG, "User logged in status: " + isLoggedIn);
        return isLoggedIn;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "Creating options menu...");

        try {
            getMenuInflater().inflate(R.menu.main_menu, menu);

            // Thêm service toggle menu với ID constants
            menu.add(0, ACTION_TOGGLE_SERVICE, 0,
                    serviceManager != null && serviceManager.isServiceEnabled() ?
                            "Tắt dịch vụ nền" : "Bật dịch vụ nền");

            // Thêm test notification menu
            menu.add(0, ACTION_TEST_NOTIFICATION, 0, "Test thông báo");

            // Thêm debug service menu
            menu.add(0, ACTION_DEBUG_SERVICE, 0, "Debug Service");

            Log.d(TAG, "Options menu created successfully");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error creating options menu: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        Log.d(TAG, "Menu item selected: " + id);

        if (id == R.id.action_logout) {
            showLogoutDialog();
            return true;
        }

        // Xử lý toggle service với constant ID
        if (id == ACTION_TOGGLE_SERVICE) {
            toggleBackgroundService();
            return true;
        }

        // Test notification
        if (id == ACTION_TEST_NOTIFICATION) {
            testNotification();
            return true;
        }

        // Debug service
        if (id == ACTION_DEBUG_SERVICE) {
            openDebugService();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void openDebugService() {
        Log.d(TAG, "Opening debug service activity");
        try {
            Intent intent = new Intent(this, DebugServiceActivity.class);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening debug service: " + e.getMessage());
            e.printStackTrace();

            // ✅ THÊM: Fallback nếu không tìm thấy DebugServiceActivity
            Toast.makeText(this, "DebugServiceActivity chưa có, sẽ test service trực tiếp", Toast.LENGTH_LONG).show();
            testServiceDirectly();
        }
    }

    // ✅ THÊM: Method test service trực tiếp từ MainActivity
    private void testServiceDirectly() {
        Log.d(TAG, "=== TESTING SERVICE DIRECTLY FROM MAIN ===");

        try {
            if (serviceManager == null) {
                serviceManager = new ServiceManager(this);
                Log.d(TAG, "Created new ServiceManager for direct test");
            }

            String status = serviceManager.getServiceStatus();
            Log.d(TAG, "Direct test - Service status: " + status);

            // Test force start
            Log.d(TAG, "Direct test - Force starting service...");
            boolean started = serviceManager.forceStartService();
            Log.d(TAG, "Direct test - Force start result: " + started);

            Toast.makeText(this, "Direct service test completed - Check Logcat", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Log.e(TAG, "Error in direct service test: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void toggleBackgroundService() {
        Log.d(TAG, "Toggle background service requested");

        if (serviceManager == null) {
            Log.e(TAG, "ServiceManager is null");
            Toast.makeText(this, "Service manager chưa được khởi tạo", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!serviceManager.hasRequiredPermissions()) {
            Log.d(TAG, "No required permissions");
            Toast.makeText(this, "Cần cấp quyền thông báo trước", Toast.LENGTH_SHORT).show();
            checkAndRequestNotificationPermission();
            return;
        }

        Log.d(TAG, "Attempting to toggle service...");
        boolean success = serviceManager.toggleService();
        String message;

        if (serviceManager.isServiceEnabled()) {
            message = success ? "Đã bật dịch vụ chạy nền" : "Không thể bật dịch vụ";
            Log.d(TAG, "Service now ENABLED, success: " + success);
        } else {
            message = success ? "Đã tắt dịch vụ chạy nền" : "Không thể tắt dịch vụ";
            Log.d(TAG, "Service now DISABLED, success: " + success);
        }

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

        // Refresh menu để update text
        invalidateOptionsMenu();

        // ✅ THÊM: Log chi tiết sau khi toggle
        String newStatus = serviceManager.getServiceStatus();
        Log.d(TAG, "Service status after toggle: " + newStatus.replace("\n", " | "));
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc chắn muốn đăng xuất?")
                .setPositiveButton("Đăng xuất", (dialog, which) -> logout())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void logout() {
        Log.d(TAG, "User logging out");

        // Dừng background service khi logout
        if (serviceManager != null && serviceManager.isServiceEnabled()) {
            serviceManager.stopBackgroundService();
            Log.d(TAG, "Background service stopped on logout");
        }

        // Clear login state
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, false);
        editor.remove(KEY_USER_ID);

        boolean rememberMe = sharedPreferences.getBoolean(KEY_REMEMBER_ME, false);
        if (!rememberMe) {
            editor.remove(KEY_USERNAME);
        }

        editor.apply();

        // Navigate to login
        navigateToLogin();
        Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();
    }

    private void navigateToLogin() {
        Log.d(TAG, "Navigating to login");
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "MainActivity destroyed, service continues running");
    }
}