package com.example.expensemanagement;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

public class AppStateManager implements Application.ActivityLifecycleCallbacks {

    private static AppStateManager instance;
    private Context context;
    private boolean isAppInForeground = false;
    private NotificationHelper notificationHelper;

    private static final String PREFS_NAME = "AppStatePrefs";
    private static final String KEY_PENDING_NOTIFICATION_TYPE = "pendingNotificationType";
    private static final String KEY_PENDING_NOTIFICATION_AMOUNT = "pendingNotificationAmount";
    private static final String KEY_PENDING_NOTIFICATION_CATEGORY = "pendingNotificationCategory";
    private static final String KEY_HAS_PENDING_NOTIFICATION = "hasPendingNotification";

    private AppStateManager(Context context) {
        this.context = context.getApplicationContext();
        this.notificationHelper = new NotificationHelper(this.context);
    }

    public static synchronized AppStateManager getInstance(Context context) {
        if (instance == null) {
            instance = new AppStateManager(context);
        }
        return instance;
    }

    public boolean isAppInForeground() {
        return isAppInForeground;
    }

    public void savePendingNotification(String transactionType, double amount, String categoryName) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putBoolean(KEY_HAS_PENDING_NOTIFICATION, true)
                .putString(KEY_PENDING_NOTIFICATION_TYPE, transactionType)
                .putString(KEY_PENDING_NOTIFICATION_AMOUNT, String.valueOf(amount))
                .putString(KEY_PENDING_NOTIFICATION_CATEGORY, categoryName)
                .apply();
    }

    public void handleTransactionNotification(String transactionType, double amount, String categoryName) {
        if (isAppInForeground) {
            notificationHelper.showTransactionSuccessNotification(transactionType, amount, categoryName);
        } else {
            savePendingNotification(transactionType, amount, categoryName);
            notificationHelper.showDelayedTransactionNotification(transactionType, amount, categoryName);
        }
    }

    private void processPendingNotifications() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        if (prefs.getBoolean(KEY_HAS_PENDING_NOTIFICATION, false)) {
            String transactionType = prefs.getString(KEY_PENDING_NOTIFICATION_TYPE, "");
            String amountStr = prefs.getString(KEY_PENDING_NOTIFICATION_AMOUNT, "0");
            String categoryName = prefs.getString(KEY_PENDING_NOTIFICATION_CATEGORY, "");

            try {
                double amount = Double.parseDouble(amountStr);
                notificationHelper.showDelayedTransactionNotification(transactionType, amount, categoryName);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }

            prefs.edit().putBoolean(KEY_HAS_PENDING_NOTIFICATION, false).apply();
        }
    }

    public void showLowBalanceWarning(double balance) {
        notificationHelper.showLowBalanceWarning(balance);
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}

    @Override
    public void onActivityStarted(Activity activity) {}

    @Override
    public void onActivityResumed(Activity activity) {
        if (!isAppInForeground) {
            isAppInForeground = true;
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {}

    @Override
    public void onActivityStopped(Activity activity) {
        if (isAppInForeground) {
            isAppInForeground = false;
            processPendingNotifications();
        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

    @Override
    public void onActivityDestroyed(Activity activity) {}
}