package com.example.expensemanagement;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.text.DecimalFormat;

public class NotificationHelper {

    private static final String CHANNEL_ID = "expense_management_channel";
    private static final String CHANNEL_HIGH_ID = "expense_high_priority_channel";
    private static final String CHANNEL_NAME = "Quản lý chi tiêu";
    private static final String CHANNEL_HIGH_NAME = "Thông báo quan trọng";
    private static final String CHANNEL_DESCRIPTION = "Thông báo về giao dịch thu chi";
    private static final int NOTIFICATION_ID_TRANSACTION = 1001;
    private static final int NOTIFICATION_ID_LOW_BALANCE = 1002;
    private static final String TAG = "NotificationHelper";

    private Context context;
    private NotificationManagerCompat notificationManager;
    private DecimalFormat currencyFormat;

    public NotificationHelper(Context context) {
        this.context = context;
        this.notificationManager = NotificationManagerCompat.from(context);
        this.currencyFormat = new DecimalFormat("#,###,### đ");
        createNotificationChannels();
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);

            // Channel bình thường
            NotificationChannel normalChannel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            normalChannel.setDescription(CHANNEL_DESCRIPTION);
            normalChannel.enableLights(true);
            normalChannel.enableVibration(true);
            normalChannel.setVibrationPattern(new long[]{0, 250, 250, 250});

            // Channel có priority cao - để có heads-up notification
            NotificationChannel highPriorityChannel = new NotificationChannel(
                    CHANNEL_HIGH_ID,
                    CHANNEL_HIGH_NAME,
                    NotificationManager.IMPORTANCE_HIGH // ✅ HIGH IMPORTANCE
            );
            highPriorityChannel.setDescription("Thông báo giao dịch và cảnh báo quan trọng");
            highPriorityChannel.enableLights(true);
            highPriorityChannel.enableVibration(true);
            highPriorityChannel.setVibrationPattern(new long[]{0, 300, 150, 300});
            highPriorityChannel.setShowBadge(true);

            if (manager != null) {
                manager.createNotificationChannel(normalChannel);
                manager.createNotificationChannel(highPriorityChannel);
                Log.d(TAG, "Notification channels created successfully");
            }
        }
    }

    private boolean hasNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    // ✅ HEADS-UP NOTIFICATION CHO GIAO DỊCH THÀNH CÔNG
    public void showTransactionSuccessNotification(String transactionType, double amount, String categoryName) {
        if (!hasNotificationPermission()) {
            Log.w(TAG, "No notification permission, cannot show notification");
            return;
        }

        String title = "income".equals(transactionType) ?
                "✅ Thêm thu nhập thành công!" :
                "✅ Thêm chi tiêu thành công!";

        String message = String.format("%s: %s\nDanh mục: %s",
                "income".equals(transactionType) ? "Thu nhập" : "Chi tiêu",
                currencyFormat.format(amount),
                categoryName);

        Log.d(TAG, "Showing transaction notification: " + title);
        showHeadsUpNotification(NOTIFICATION_ID_TRANSACTION, title, message, true);
    }

    // ✅ HEADS-UP NOTIFICATION CHO CẢNH BÁO SỐ DƯ THẤP
    public void showLowBalanceWarning(double currentBalance) {
        if (!hasNotificationPermission()) {
            Log.w(TAG, "No notification permission, cannot show low balance warning");
            return;
        }

        String title = "⚠️ Cảnh báo số dư thấp";
        String message = String.format("Số dư hiện tại: %s\nHãy cân nhắc chi tiêu!",
                currencyFormat.format(currentBalance));

        Log.d(TAG, "Showing low balance warning: " + title);
        showHeadsUpNotification(NOTIFICATION_ID_LOW_BALANCE, title, message, true);
    }

    // ✅ METHOD CHÍNH ĐỂ HIỂN THỊ HEADS-UP NOTIFICATION
    private void showHeadsUpNotification(int notificationId, String title, String message, boolean isHeadsUp) {
        try {
            if (!hasNotificationPermission()) {
                Log.w(TAG, "Cannot show notification - no permission");
                return;
            }

            Intent intent = new Intent(context, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context,
                    notificationId,
                    intent,
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ?
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE :
                            PendingIntent.FLAG_UPDATE_CURRENT
            );

            // ✅ SỬ DỤNG CHANNEL HIGH PRIORITY VÀ CÁC THIẾT LẬP CHO HEADS-UP
            String channelId = isHeadsUp ? CHANNEL_HIGH_ID : CHANNEL_ID;
            int priority = isHeadsUp ? NotificationCompat.PRIORITY_HIGH : NotificationCompat.PRIORITY_DEFAULT;

            // ✅ THÊM SOUND ĐỂ THU HÚT SỰ CHÚ Ý
            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                    .setPriority(priority) // ✅ HIGH PRIORITY
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setDefaults(NotificationCompat.DEFAULT_ALL) // ✅ DEFAULT ALL
                    .setSound(defaultSoundUri) // ✅ SOUND
                    .setVibrate(new long[]{0, 300, 150, 300}) // ✅ VIBRATION
                    .setLights(0xFF00FF00, 3000, 3000); // ✅ LED LIGHTS

            // ✅ CHO ANDROID 21+ ĐỂ CÓ HEADS-UP
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && isHeadsUp) {
                builder.setCategory(NotificationCompat.CATEGORY_MESSAGE)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .setFullScreenIntent(pendingIntent, false); // ✅ FULL SCREEN INTENT
            }

            Log.d(TAG, "About to show heads-up notification with ID: " + notificationId);
            Log.d(TAG, "Title: " + title);
            Log.d(TAG, "Message: " + message);
            Log.d(TAG, "Channel: " + channelId + ", Priority: " + priority);

            notificationManager.notify(notificationId, builder.build());
            Log.d(TAG, "Heads-up notification sent successfully");

        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException when showing notification: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            Log.e(TAG, "Exception when showing notification: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ✅ NOTIFICATION DELAYED (KHÔNG HEADS-UP)
    public void showDelayedTransactionNotification(String transactionType, double amount, String categoryName) {
        if (!hasNotificationPermission()) {
            Log.w(TAG, "No notification permission, cannot show delayed notification");
            return;
        }

        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(() -> {
            String title = "income".equals(transactionType) ?
                    "💰 Thu nhập mới đã được thêm" :
                    "💸 Chi tiêu mới đã được thêm";

            String message = String.format("Bạn vừa %s %s cho %s",
                    "income".equals(transactionType) ? "thu được" : "chi tiêu",
                    currencyFormat.format(amount),
                    categoryName);

            Log.d(TAG, "Showing delayed transaction notification: " + title);
            showHeadsUpNotification(NOTIFICATION_ID_TRANSACTION + 100, title, message, false);
        }, 2000);
    }

    // ✅ TEST NOTIFICATION VỚI HEADS-UP
    public void showTestNotification() {
        if (hasNotificationPermission()) {
            showHeadsUpNotification(9999, "🧪 Test Notification",
                    "Đây là thông báo test với heads-up display!", true);
            Log.d(TAG, "Test heads-up notification sent");
        } else {
            Log.w(TAG, "Cannot send test notification - no permission");
        }
    }
}