package com.example.expensemanagement;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "=== BootReceiver triggered ===");
        Log.d(TAG, "Received broadcast: " + action);

        if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
                Intent.ACTION_MY_PACKAGE_REPLACED.equals(action) ||
                Intent.ACTION_PACKAGE_REPLACED.equals(action)) {

            Log.d(TAG, "Device boot completed or app updated");

            try {
                // Khởi động service nếu user đã enable trước đó
                ServiceManager serviceManager = new ServiceManager(context);
                Log.d(TAG, "ServiceManager created");

                if (serviceManager.isServiceEnabled()) {
                    Log.d(TAG, "Service was enabled, auto-starting...");

                    boolean started = serviceManager.startBackgroundService();
                    Log.d(TAG, "Auto-started SimpleBackgroundService after boot: " + started);

                    if (started) {
                        Log.d(TAG, "✅ Service started successfully on boot");
                    } else {
                        Log.e(TAG, "❌ Failed to start service on boot");
                    }
                } else {
                    Log.d(TAG, "Service not enabled, skipping auto-start");
                }

            } catch (Exception e) {
                Log.e(TAG, "❌ Error in BootReceiver: " + e.getMessage());
                e.printStackTrace();
            }

        } else {
            Log.d(TAG, "Irrelevant broadcast action: " + action);
        }

        Log.d(TAG, "=== BootReceiver completed ===");
    }
}