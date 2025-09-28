package com.example.expensemanagement;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import java.text.DecimalFormat;

public class InAppNotificationManager {

    private static final String TAG = "InAppNotification";
    private static final int NOTIFICATION_DURATION = 4000; // 4 seconds

    private Activity activity;
    private ViewGroup parentLayout;
    private DecimalFormat currencyFormat;

    public InAppNotificationManager(Activity activity) {
        this.activity = activity;
        this.currencyFormat = new DecimalFormat("#,###,### đ");

        // Tìm parent layout để thêm notification
        this.parentLayout = activity.findViewById(android.R.id.content);
    }

    public InAppNotificationManager(Activity activity, ViewGroup customParent) {
        this.activity = activity;
        this.parentLayout = customParent;
        this.currencyFormat = new DecimalFormat("#,###,### đ");
    }

    // Hiển thị notification thành công giao dịch
    public void showTransactionSuccess(String transactionType, double amount, String categoryName) {
        String title = "income".equals(transactionType) ?
                "✅ Thêm thu nhập thành công!" :
                "✅ Thêm chi tiêu thành công!";

        String message = String.format("%s: %s\nDanh mục: %s",
                "income".equals(transactionType) ? "Thu nhập" : "Chi tiêu",
                currencyFormat.format(amount),
                categoryName);

        int backgroundColor = "income".equals(transactionType) ?
                R.color.income_green : R.color.expense_red;

        showNotification(title, message, backgroundColor, android.R.drawable.ic_dialog_info);
    }

    // Hiển thị cảnh báo số dư thấp
    public void showLowBalanceWarning(double currentBalance) {
        String title = "⚠️ Cảnh báo số dư thấp";
        String message = String.format("Số dư hiện tại: %s\nHãy cân nhắc chi tiêu!",
                currencyFormat.format(currentBalance));

        showNotification(title, message, android.R.color.holo_orange_dark, android.R.drawable.ic_dialog_alert);
    }

    // Hiển thị notification tùy chỉnh
    public void showCustomNotification(String title, String message) {
        showNotification(title, message, R.color.primary_color, android.R.drawable.ic_dialog_info);
    }

    // Method chính để hiển thị notification
    private void showNotification(String title, String message, int backgroundColorRes, int iconRes) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            Log.w(TAG, "Activity is not available, cannot show in-app notification");
            return;
        }

        activity.runOnUiThread(() -> {
            try {
                // Tạo notification view từ layout
                LayoutInflater inflater = LayoutInflater.from(activity);
                View notificationView = inflater.inflate(R.layout.notification_popup, parentLayout, false);

                // Setup views
                CardView cardNotification = notificationView.findViewById(R.id.notification_popup);
                ImageView ivIcon = notificationView.findViewById(R.id.iv_notification_icon);
                TextView tvTitle = notificationView.findViewById(R.id.tv_notification_title);
                TextView tvMessage = notificationView.findViewById(R.id.tv_notification_message);
                ImageView ivClose = notificationView.findViewById(R.id.iv_close_notification);

                // Set content
                tvTitle.setText(title);
                tvMessage.setText(message);
                ivIcon.setImageResource(iconRes);

                // Set background color
                cardNotification.setCardBackgroundColor(ContextCompat.getColor(activity, backgroundColorRes));

                // Set close button click
                ivClose.setOnClickListener(v -> hideNotification(notificationView));

                // Thêm vào parent layout
                parentLayout.addView(notificationView);

                // Animation hiện lên
                showAnimation(notificationView);

                // Tự động ẩn sau NOTIFICATION_DURATION
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    hideNotification(notificationView);
                }, NOTIFICATION_DURATION);

                Log.d(TAG, "In-app notification shown: " + title);

            } catch (Exception e) {
                Log.e(TAG, "Error showing in-app notification: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    // Animation hiện notification
    private void showAnimation(View notificationView) {
        notificationView.setVisibility(View.VISIBLE);
        notificationView.setAlpha(0f);
        notificationView.setTranslationY(-100f);

        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(notificationView, "alpha", 0f, 1f);
        ObjectAnimator slideDown = ObjectAnimator.ofFloat(notificationView, "translationY", -100f, 0f);

        fadeIn.setDuration(300);
        slideDown.setDuration(300);

        fadeIn.start();
        slideDown.start();
    }

    // Animation ẩn notification
    private void hideNotification(View notificationView) {
        if (notificationView != null && notificationView.getParent() != null) {
            ObjectAnimator fadeOut = ObjectAnimator.ofFloat(notificationView, "alpha", 1f, 0f);
            ObjectAnimator slideUp = ObjectAnimator.ofFloat(notificationView, "translationY", 0f, -100f);

            fadeOut.setDuration(250);
            slideUp.setDuration(250);

            fadeOut.start();
            slideUp.start();

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                try {
                    if (parentLayout != null && notificationView.getParent() == parentLayout) {
                        parentLayout.removeView(notificationView);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error removing notification view: " + e.getMessage());
                }
            }, 250);

            Log.d(TAG, "In-app notification hidden");
        }
    }

    // Ẩn tất cả notifications
    public void hideAllNotifications() {
        if (parentLayout != null) {
            for (int i = parentLayout.getChildCount() - 1; i >= 0; i--) {
                View child = parentLayout.getChildAt(i);
                if (child.findViewById(R.id.notification_popup) != null) {
                    hideNotification(child);
                }
            }
        }
    }
}