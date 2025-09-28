package com.example.expensemanagement;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class SimpleBackgroundService extends Service {

    private static final String TAG = "SimpleBackgroundService";
    private static final String CHANNEL_ID = "simple_background_channel";
    private static final String CHANNEL_ID_ALERTS = "simple_alerts_channel";
    private static final int FOREGROUND_NOTIFICATION_ID = 1001;

    // Interval cho các check khác nhau
    private static final long DAILY_CHECK_INTERVAL = 24 * 60 * 60 * 1000; // 24 giờ
    private static final long BUDGET_CHECK_INTERVAL = 60 * 60 * 1000; // 1 giờ
    private static final long TEST_INTERVAL = 30 * 1000; // 30 giây cho test

    private Handler handler;
    private Runnable dailyReminderRunnable;
    private Runnable budgetCheckRunnable;
    private SharedPreferences sharedPreferences;
    private int testCounter = 0;
    private boolean isServiceRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "=== SimpleBackgroundService onCreate() ===");

        try {
            // Khởi tạo SharedPreferences
            sharedPreferences = getSharedPreferences("MoneyMasterPrefs", MODE_PRIVATE);

            // Tạo notification channels
            createNotificationChannels();

            // Khởi tạo handler
            handler = new Handler(Looper.getMainLooper());

            // Tạo các runnable
            setupRunnables();

            Log.d(TAG, "Service created successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "=== SimpleBackgroundService onStartCommand() ===");

        try {
            // Bắt buộc start foreground trước
            startForeground(FOREGROUND_NOTIFICATION_ID, createForegroundNotification());
            Log.d(TAG, "Started as foreground service");

            // Đánh dấu service đang chạy
            isServiceRunning = true;

            // Gửi notification khởi động
            sendStartupNotification();

            // Bắt đầu các task định kỳ
            startPeriodicTasks();

            Log.d(TAG, "Service started successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error in onStartCommand: " + e.getMessage());
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
        Log.d(TAG, "=== SimpleBackgroundService onDestroy() ===");

        try {
            isServiceRunning = false;

            // Dọn dẹp handler
            if (handler != null) {
                handler.removeCallbacks(dailyReminderRunnable);
                handler.removeCallbacks(budgetCheckRunnable);
                Log.d(TAG, "All callbacks removed");
            }

            // Gửi notification dừng
            sendShutdownNotification();

        } catch (Exception e) {
            Log.e(TAG, "Error in onDestroy: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);

            if (manager != null) {
                // Channel cho foreground service (priority thấp)
                NotificationChannel serviceChannel = new NotificationChannel(
                        CHANNEL_ID,
                        "Dịch vụ quản lý chi tiêu",
                        NotificationManager.IMPORTANCE_LOW
                );
                serviceChannel.setDescription("Dịch vụ chạy ngầm theo dõi chi tiêu");
                serviceChannel.setShowBadge(false);
                manager.createNotificationChannel(serviceChannel);

                // 🔥 Channel cho thông báo (IMPORTANCE_HIGH để có heads-up)
                NotificationChannel alertChannel = new NotificationChannel(
                        CHANNEL_ID_ALERTS,
                        "Thông báo chi tiêu",
                        NotificationManager.IMPORTANCE_HIGH // 🔥 HIGH IMPORTANCE
                );
                alertChannel.setDescription("Nhắc nhở và cảnh báo về chi tiêu");
                alertChannel.setShowBadge(true);
                alertChannel.enableVibration(true);
                alertChannel.setVibrationPattern(new long[]{0, 500, 200, 500}); // 🔥 VIBRATION PATTERN
                alertChannel.enableLights(true);
                alertChannel.setLightColor(0xFF00FF00); // 🔥 LIGHT COLOR
                alertChannel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC); // 🔥 HIỆN TRÊN LOCK SCREEN
                manager.createNotificationChannel(alertChannel);

                Log.d(TAG, "✅ Notification channels created with HIGH priority");
            }
        }
    }

    private Notification createForegroundNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ?
                        PendingIntent.FLAG_IMMUTABLE : 0);

        String currentTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Money Master Pro")
                .setContentText("Dịch vụ đang hoạt động - " + currentTime)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setShowWhen(false)
                .build();
    }

    private void setupRunnables() {
        // Runnable cho nhắc nhở hàng ngày
        dailyReminderRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isServiceRunning) return;

                Log.d(TAG, "Daily reminder check");
                sendDailyReminder();

                // Lên lịch cho lần tiếp theo
                handler.postDelayed(this, DAILY_CHECK_INTERVAL);
            }
        };

        // Runnable cho kiểm tra ngân sách
        budgetCheckRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isServiceRunning) return;

                testCounter++;
                Log.d(TAG, "Budget check #" + testCounter);

                // Thực hiện các kiểm tra
                performBudgetCheck();

                // Lên lịch cho lần tiếp theo (dùng TEST_INTERVAL cho demo)
                handler.postDelayed(this, TEST_INTERVAL);
            }
        };
    }

    private void startPeriodicTasks() {
        Log.d(TAG, "Starting periodic tasks");

        // Bắt đầu daily reminder (delay 5 giây)
        handler.postDelayed(dailyReminderRunnable, 5000);

        // Bắt đầu budget check (delay 10 giây)
        handler.postDelayed(budgetCheckRunnable, 10000);

        Log.d(TAG, "Periodic tasks scheduled");
    }

    private void sendStartupNotification() {
        String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        sendNotification(
                "Dịch vụ đã khởi động",
                "Money Master Pro bắt đầu theo dõi chi tiêu lúc " + currentTime,
                NotificationCompat.PRIORITY_DEFAULT
        );
    }

    private void sendShutdownNotification() {
        String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        sendNotification(
                "Dịch vụ đã dừng",
                "Money Master Pro ngừng theo dõi lúc " + currentTime,
                NotificationCompat.PRIORITY_DEFAULT
        );
    }

    private void sendDailyReminder() {
        Log.d(TAG, "Sending daily reminder");

        String username = sharedPreferences.getString("username", "Bạn");

        sendNotification(
                "Nhắc nhở hàng ngày",
                "Chào " + username + "! Đã ghi chép chi tiêu hôm nay chưa?",
                NotificationCompat.PRIORITY_DEFAULT
        );
    }

    private void performBudgetCheck() {
        Log.d(TAG, "Performing budget check #" + testCounter);

        try {
            // Giả lập kiểm tra ngân sách
            Random random = new Random();
            boolean shouldAlert = random.nextBoolean();

            if (shouldAlert && testCounter % 3 == 0) {
                // Giả lập cảnh báo vượt ngân sách
                sendBudgetWarning();
            } else if (testCounter % 5 == 0) {
                // Gửi báo cáo định kỳ
                sendPeriodicReport();
            } else {
                // Log bình thường
                Log.d(TAG, "Budget check completed - no alerts needed");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in budget check: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void sendBudgetWarning() {
        Log.d(TAG, "Sending budget warning");

        sendNotification(
                "Cảnh báo ngân sách",
                "Chi tiêu tháng này đã vượt 80% ngân sách dự kiến!",
                NotificationCompat.PRIORITY_HIGH
        );
    }

    private void sendPeriodicReport() {
        Log.d(TAG, "Sending periodic report");

        String currentDate = new SimpleDateFormat("dd/MM", Locale.getDefault()).format(new Date());

        sendNotification(
                "Báo cáo chi tiêu",
                "Tính đến " + currentDate + ": Bạn đã chi tiêu 1,250,000đ trong tuần này",
                NotificationCompat.PRIORITY_DEFAULT
        );
    }

    private void sendNotification(String title, String message, int priority) {
        try {
            // Kiểm tra quyền notification
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                        != getPackageManager().PERMISSION_GRANTED) {
                    Log.w(TAG, "No notification permission");
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

            // 🔥 CẤU HÌNH ĐỂ CÓ HEADS-UP NOTIFICATION
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID_ALERTS)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                    .setPriority(NotificationCompat.PRIORITY_HIGH) // 🔥 BẮT BUỘC HIGH PRIORITY
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setVibrate(new long[]{0, 500, 200, 500}) // 🔥 VIBRATION MẠNH
                    .setLights(0xFF00FF00, 1000, 1000) // 🔥 LED LIGHTS
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE) // 🔥 CATEGORY MESSAGE
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // 🔥 HIỆN CÔNG KHAI
                    .setFullScreenIntent(pendingIntent, false); // 🔥 FULL SCREEN INTENT

            // 🔥 ĐẶC BIỆT QUAN TRỌNG: Thêm sound URI
            android.net.Uri defaultSoundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION);
            builder.setSound(defaultSoundUri);

            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            if (notificationManager != null) {
                int notificationId = (int) System.currentTimeMillis();
                notificationManager.notify(notificationId, builder.build());
                Log.d(TAG, "🔥 HEADS-UP Notification sent: " + title + " (ID: " + notificationId + ")");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error sending heads-up notification: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Method để check trạng thái service từ bên ngoài
    public static boolean isServiceRunning() {
        // Đây là cách đơn giản, có thể implement phức tạp hơn nếu cần
        return true;
    }
}