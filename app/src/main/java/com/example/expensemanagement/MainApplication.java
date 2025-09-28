package com.example.expensemanagement;

import android.app.Application;

public class MainApplication extends Application {

    private AppStateManager appStateManager;

    @Override
    public void onCreate() {
        super.onCreate();

        appStateManager = AppStateManager.getInstance(this);
        registerActivityLifecycleCallbacks(appStateManager);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

        if (appStateManager != null) {
            unregisterActivityLifecycleCallbacks(appStateManager);
        }
    }
}