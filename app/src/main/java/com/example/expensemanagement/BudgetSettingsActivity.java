package com.example.expensemanagement;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.*;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

public class BudgetSettingsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "BudgetSettings";

    // Alert Settings
    private SwitchCompat switchBudgetAlerts;
    private SwitchCompat switchOverspendingWarning;
    private SwitchCompat switchDailyReminders;
    private SeekBar seekWarningThreshold;
    private TextView tvWarningThreshold;

    // Notification Settings
    private SwitchCompat switchPushNotifications;
    private SwitchCompat switchEmailNotifications;
    private Spinner spinnerNotificationTime;

    // Budget Behavior
    private SwitchCompat switchAutoRollover;
    private SwitchCompat switchSmartSuggestions;
    private RadioGroup rgBudgetResetBehavior;

    // Display Settings
    private Spinner spinnerCurrency;
    private SwitchCompat switchShowPercentages;
    private SwitchCompat switchShowHealthScore;

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_budget_settings);

        initializeComponents();
        loadSettings();
        setupEventListeners();
        setupBackPressedCallback();
    }

    private void initializeComponents() {
        // Alert Settings
        switchBudgetAlerts = findViewById(R.id.switch_budget_alerts);
        switchOverspendingWarning = findViewById(R.id.switch_overspending_warning);
        switchDailyReminders = findViewById(R.id.switch_daily_reminders);
        seekWarningThreshold = findViewById(R.id.seek_warning_threshold);
        tvWarningThreshold = findViewById(R.id.tv_warning_threshold);

        // Notification Settings
        switchPushNotifications = findViewById(R.id.switch_push_notifications);
        switchEmailNotifications = findViewById(R.id.switch_email_notifications);
        spinnerNotificationTime = findViewById(R.id.spinner_notification_time);

        // Budget Behavior
        switchAutoRollover = findViewById(R.id.switch_auto_rollover);
        switchSmartSuggestions = findViewById(R.id.switch_smart_suggestions);
        rgBudgetResetBehavior = findViewById(R.id.rg_budget_reset_behavior);

        // Display Settings
        spinnerCurrency = findViewById(R.id.spinner_currency);
        switchShowPercentages = findViewById(R.id.switch_show_percentages);
        switchShowHealthScore = findViewById(R.id.switch_show_health_score);

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        setupSpinners();
    }

    private void setupSpinners() {
        // Notification Time Spinner - Tạo array trong code thay vì dùng resources
        String[] notificationTimes = {"8:00 AM", "12:00 PM", "6:00 PM", "9:00 PM"};
        ArrayAdapter<String> timeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, notificationTimes);
        timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerNotificationTime.setAdapter(timeAdapter);

        // Currency Spinner - Tạo array trong code thay vì dùng resources
        String[] currencies = {"USD ($)", "EUR (€)", "VND (₫)", "JPY (¥)", "GBP (£)"};
        ArrayAdapter<String> currencyAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, currencies);
        currencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCurrency.setAdapter(currencyAdapter);
    }

    private void loadSettings() {
        // Alert Settings
        switchBudgetAlerts.setChecked(sharedPreferences.getBoolean("budget_alerts", true));
        switchOverspendingWarning.setChecked(sharedPreferences.getBoolean("overspending_warning", true));
        switchDailyReminders.setChecked(sharedPreferences.getBoolean("daily_reminders", false));

        int warningThreshold = sharedPreferences.getInt("warning_threshold", 80);
        seekWarningThreshold.setProgress(warningThreshold);
        tvWarningThreshold.setText(warningThreshold + "%");

        // Notification Settings
        switchPushNotifications.setChecked(sharedPreferences.getBoolean("push_notifications", true));
        switchEmailNotifications.setChecked(sharedPreferences.getBoolean("email_notifications", false));

        int notificationTime = sharedPreferences.getInt("notification_time", 0);
        spinnerNotificationTime.setSelection(notificationTime);

        // Budget Behavior
        switchAutoRollover.setChecked(sharedPreferences.getBoolean("auto_rollover", false));
        switchSmartSuggestions.setChecked(sharedPreferences.getBoolean("smart_suggestions", true));

        String resetBehavior = sharedPreferences.getString("reset_behavior", "reset");
        if (resetBehavior.equals("reset")) {
            rgBudgetResetBehavior.check(R.id.rb_reset);
        } else if (resetBehavior.equals("rollover")) {
            rgBudgetResetBehavior.check(R.id.rb_rollover);
        } else {
            rgBudgetResetBehavior.check(R.id.rb_adjust);
        }

        // Display Settings
        int currencyIndex = sharedPreferences.getInt("currency_index", 0);
        spinnerCurrency.setSelection(currencyIndex);

        switchShowPercentages.setChecked(sharedPreferences.getBoolean("show_percentages", true));
        switchShowHealthScore.setChecked(sharedPreferences.getBoolean("show_health_score", true));
    }

    private void setupEventListeners() {
        // Warning Threshold SeekBar
        seekWarningThreshold.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvWarningThreshold.setText(progress + "%");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                saveSettings();
            }
        });

        // All switches
        switchBudgetAlerts.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveSettings();
            updateDependentSettings();
        });

        switchOverspendingWarning.setOnCheckedChangeListener((buttonView, isChecked) -> saveSettings());
        switchDailyReminders.setOnCheckedChangeListener((buttonView, isChecked) -> saveSettings());
        switchPushNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> saveSettings());
        switchEmailNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> saveSettings());
        switchAutoRollover.setOnCheckedChangeListener((buttonView, isChecked) -> saveSettings());
        switchSmartSuggestions.setOnCheckedChangeListener((buttonView, isChecked) -> saveSettings());
        switchShowPercentages.setOnCheckedChangeListener((buttonView, isChecked) -> saveSettings());
        switchShowHealthScore.setOnCheckedChangeListener((buttonView, isChecked) -> saveSettings());

        // Spinners
        spinnerNotificationTime.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                saveSettings();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerCurrency.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                saveSettings();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // RadioGroup
        rgBudgetResetBehavior.setOnCheckedChangeListener((group, checkedId) -> saveSettings());
    }

    private void setupBackPressedCallback() {
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                saveSettings();
                finish();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    private void updateDependentSettings() {
        boolean budgetAlertsEnabled = switchBudgetAlerts.isChecked();

        switchOverspendingWarning.setEnabled(budgetAlertsEnabled);
        switchDailyReminders.setEnabled(budgetAlertsEnabled);
        seekWarningThreshold.setEnabled(budgetAlertsEnabled);
    }

    private void saveSettings() {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Alert Settings
        editor.putBoolean("budget_alerts", switchBudgetAlerts.isChecked());
        editor.putBoolean("overspending_warning", switchOverspendingWarning.isChecked());
        editor.putBoolean("daily_reminders", switchDailyReminders.isChecked());
        editor.putInt("warning_threshold", seekWarningThreshold.getProgress());

        // Notification Settings
        editor.putBoolean("push_notifications", switchPushNotifications.isChecked());
        editor.putBoolean("email_notifications", switchEmailNotifications.isChecked());
        editor.putInt("notification_time", spinnerNotificationTime.getSelectedItemPosition());

        // Budget Behavior
        editor.putBoolean("auto_rollover", switchAutoRollover.isChecked());
        editor.putBoolean("smart_suggestions", switchSmartSuggestions.isChecked());

        String resetBehavior = "reset";
        int checkedId = rgBudgetResetBehavior.getCheckedRadioButtonId();
        if (checkedId == R.id.rb_rollover) {
            resetBehavior = "rollover";
        } else if (checkedId == R.id.rb_adjust) {
            resetBehavior = "adjust";
        }
        editor.putString("reset_behavior", resetBehavior);

        // Display Settings
        editor.putInt("currency_index", spinnerCurrency.getSelectedItemPosition());
        editor.putBoolean("show_percentages", switchShowPercentages.isChecked());
        editor.putBoolean("show_health_score", switchShowHealthScore.isChecked());

        editor.apply();
    }
}