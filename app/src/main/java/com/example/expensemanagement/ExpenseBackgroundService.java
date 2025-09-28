package com.example.expensemanagement;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import database.DatabaseHelper;

public class ExpenseBackgroundService extends Service {

    private static final String TAG = "ExpenseBackgroundService";
    private static final String CHANNEL_ID = "expense_background_service";
    private static final String CHANNEL_ID_ALERTS = "expense_alerts";
    private static final int NOTIFICATION_ID = 2001;
    private static final long CHECK_INTERVAL = 10 * 1000; // 10 seconds for testing

    private Handler handler;
    private Runnable checkRunnable;
    private DatabaseHelper dbHelper;
    private SQLiteDatabase database;
    private SharedPreferences sharedPreferences;
    private DecimalFormat currencyFormat;
    private int testCounter = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        // 🚨 THÊM LOG ĐẦU TIÊN NGAY LẬP TỨC
        Log.d(TAG, "=== SERVICE onCreate() CALLED ===");
        System.out.println("ExpenseBackgroundService onCreate() called!");

        try {
            Log.d(TAG, "Initializing service components...");

            // Khởi tạo components
            dbHelper = new DatabaseHelper(this);
            database = dbHelper.getReadableDatabase();
            sharedPreferences = getSharedPreferences("MoneyMasterPrefs", MODE_PRIVATE);
            currencyFormat = new DecimalFormat("#,###,### đ");

            // Tạo notification channels
            createNotificationChannels();

            // Khởi tạo handler
            handler = new Handler(Looper.getMainLooper());

            // Tạo runnable
            checkRunnable = new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "🔄 Runnable executing...");
                    performBackgroundChecks();
                    handler.postDelayed(this, CHECK_INTERVAL);
                }
            };

            Log.d(TAG, "✅ Service initialization completed successfully");

        } catch (Exception e) {
            Log.e(TAG, "❌ Error in onCreate: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "=== SERVICE onStartCommand() CALLED ===");
        System.out.println("ExpenseBackgroundService onStartCommand() called!");

        try {
            // Bắt buộc phải start foreground service trước
            Notification notification = createForegroundNotification();
            startForeground(NOTIFICATION_ID, notification);
            Log.d(TAG, "✅ Started as foreground service with notification ID: " + NOTIFICATION_ID);

            // Bắt đầu periodic checks
            if (handler != null && checkRunnable != null) {
                handler.post(checkRunnable);
                Log.d(TAG, "✅ Periodic checks scheduled");
            } else {
                Log.e(TAG, "❌ Handler or checkRunnable is null!");
            }

            // Test notification ngay lập tức
            sendImmediateTestNotification();

        } catch (Exception e) {
            Log.e(TAG, "❌ Error in onStartCommand: " + e.getMessage());
            e.printStackTrace();
        }

        return START_STICKY; // Service sẽ được restart nếu bị kill
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "=== SERVICE onDestroy() CALLED ===");
        System.out.println("ExpenseBackgroundService onDestroy() called!");

        try {
            if (handler != null && checkRunnable != null) {
                handler.removeCallbacks(checkRunnable);
                Log.d(TAG, "✅ Handler callbacks removed");
            }

            if (database != null && database.isOpen()) {
                database.close();
                Log.d(TAG, "✅ Database closed");
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error in onDestroy: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);

            if (manager != null) {
                // Channel cho foreground service
                NotificationChannel serviceChannel = new NotificationChannel(
                        CHANNEL_ID,
                        "Dịch vụ quản lý chi tiêu",
                        NotificationManager.IMPORTANCE_LOW
                );
                serviceChannel.setDescription("Theo dõi chi tiêu và gửi nhắc nhở");
                serviceChannel.setShowBadge(false);
                manager.createNotificationChannel(serviceChannel);

                // Channel cho alerts
                NotificationChannel alertChannel = new NotificationChannel(
                        CHANNEL_ID_ALERTS,
                        "Thông báo chi tiêu",
                        NotificationManager.IMPORTANCE_HIGH
                );
                alertChannel.setDescription("Thông báo nhắc nhở và cảnh báo chi tiêu");
                alertChannel.setShowBadge(true);
                alertChannel.enableVibration(true);
                alertChannel.enableLights(true);
                manager.createNotificationChannel(alertChannel);

                Log.d(TAG, "✅ Notification channels created successfully");
            } else {
                Log.e(TAG, "❌ NotificationManager is null");
            }
        }
    }

    private Notification createForegroundNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ?
                        PendingIntent.FLAG_IMMUTABLE : 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Money Master Pro")
                .setContentText("Đang theo dõi chi tiêu của bạn...")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE);
        }

        return builder.build();
    }

    private void sendImmediateTestNotification() {
        Log.d(TAG, "🧪 Sending immediate test notification...");

        String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        sendNotificationReminder(
                "🚀 Service Started!",
                "ExpenseBackgroundService đã khởi động thành công lúc " + currentTime
        );
    }

    private void performBackgroundChecks() {
        testCounter++;
        Log.d(TAG, "🔍 Performing background checks... #" + testCounter);
        System.out.println("Background check #" + testCounter);

        try {
            String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
            String testMessage = String.format(Locale.getDefault(),
                    "Service hoạt động bình thường!\nLần test: %d\nThời gian: %s",
                    testCounter, currentTime
            );

            Log.d(TAG, "📤 Sending test notification: " + testMessage);

            sendNotificationReminder(
                    "✅ Test Service #" + testCounter,
                    testMessage
            );

            // Chạy các kiểm tra khác sau 3 lần test
            if (testCounter > 3) {
                runExtendedChecks();
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Error in performBackgroundChecks: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void runExtendedChecks() {
        try {
            Log.d(TAG, "🔄 Running extended checks...");

            int userId = sharedPreferences.getInt("userId", 1);
            Log.d(TAG, "👤 User ID: " + userId);

            checkDailySpendingReminder(userId);
            checkBudgetOverflow(userId);
            checkLowBalance(userId);
            checkWeeklySummary(userId);

            Log.d(TAG, "✅ Extended checks completed");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error in extended checks: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void checkDailySpendingReminder(int userId) {
        try {
            Log.d(TAG, "📊 Checking daily spending reminder for user: " + userId);
            // Implementation here...
        } catch (Exception e) {
            Log.e(TAG, "❌ Error checking daily spending: " + e.getMessage());
        }
    }

    private void checkBudgetOverflow(int userId) {
        try {
            Log.d(TAG, "💰 Checking budget overflow for user: " + userId);
            // Implementation here...
        } catch (Exception e) {
            Log.e(TAG, "❌ Error checking budget overflow: " + e.getMessage());
        }
    }

    private void checkLowBalance(int userId) {
        try {
            Log.d(TAG, "💳 Checking low balance for user: " + userId);
            // Implementation here...
        } catch (Exception e) {
            Log.e(TAG, "❌ Error checking low balance: " + e.getMessage());
        }
    }

    private void checkWeeklySummary(int userId) {
        try {
            Log.d(TAG, "📈 Checking weekly summary for user: " + userId);
            // Implementation here...
        } catch (Exception e) {
            Log.e(TAG, "❌ Error checking weekly summary: " + e.getMessage());
        }
    }

    private void sendNotificationReminder(String title, String message) {
        sendNotification(title, message, NotificationCompat.PRIORITY_DEFAULT);
    }

    private void sendNotificationWarning(String title, String message) {
        sendNotification(title, message, NotificationCompat.PRIORITY_HIGH);
    }

    private void sendNotification(String title, String message, int priority) {
        try {
            Log.d(TAG, "📤 Attempting to send notification: " + title);

            // Kiểm tra permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                        != getPackageManager().PERMISSION_GRANTED) {
                    Log.e(TAG, "❌ No notification permission!");
                    return;
                }
            }

            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this,
                    (int) System.currentTimeMillis(),
                    intent,
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ?
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE :
                            PendingIntent.FLAG_UPDATE_CURRENT
            );

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID_ALERTS)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                    .setPriority(priority)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            if (notificationManager != null) {
                int notificationId = (int) System.currentTimeMillis();
                notificationManager.notify(notificationId, builder.build());
                Log.d(TAG, "✅ Notification sent successfully: " + title + " (ID: " + notificationId + ")");
            } else {
                Log.e(TAG, "❌ NotificationManager is null!");
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Error sending notification: " + e.getMessage());
            e.printStackTrace();
        }
    }
}