package com.example.expensemanagement;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import database.UserDAO;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import database.DatabaseHelper;

public class DashboardActivity extends AppCompatActivity {

    // Database
    private DatabaseHelper dbHelper;
    private SQLiteDatabase database;

    // Shared Preferences
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "MoneyMasterPrefs";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_DARK_MODE = "DarkMode";
    private static final String KEY_REMEMBER_ME = "rememberMe";

    // UI Components
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView tvUserName;
    private TextView tvTotalBalance;
    private TextView tvBalanceChange;
    private TextView tvTotalIncome;
    private TextView tvIncomeChange;
    private TextView tvTotalExpense;
    private TextView tvExpenseChange;
    private TextView tvFinancialScore;
    private TextView tvSavingsRate;
    private TextView tvTransactionsSummary;
    private TextView tvVisibilityIcon;
    private ImageView ivProfileAvatar;

    private ProgressBar pbSavingsRate;

    private MaterialCardView btnThemeToggle;
    private MaterialCardView btnNotifications;
    private MaterialCardView btnToggleBalanceVisibility;
    private MaterialCardView profileCard;
    private MaterialCardView cardIncome;
    private MaterialCardView cardExpense;
    private MaterialCardView actionAddIncome;
    private MaterialCardView actionAddExpense;

    private MaterialButton btnViewAllInsights;
    private MaterialButton btnViewAllTransactions;
    private MaterialButton btnAddFirstTransaction;

    private ExtendedFloatingActionButton fabAddTransaction;

    private RecyclerView rvAiInsights;
    private RecyclerView rvRecentTransactions;

    private View layoutEmptyTransactions;
    private View cardDefaultInsight;
    private View notificationBadge;

    // Data
    private boolean isBalanceVisible = true;
    private double totalBalance = 0.0;
    private double totalIncome = 0.0;
    private double totalExpense = 0.0;
    private double previousMonthBalance = 0.0;
    private double previousMonthIncome = 0.0;
    private double previousMonthExpense = 0.0;
    private boolean isDarkMode = false;
    private int transactionCount = 0;

    // Formatters
    private DecimalFormat currencyFormat;
    private SimpleDateFormat dateFormat;

    private BottomNavigationView bottomNavigation;
    private PopupWindow profilePopup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set up immersive status bar
        setupStatusBar();

        setContentView(R.layout.activity_dashboard);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        isDarkMode = sharedPreferences.getBoolean(KEY_DARK_MODE, false);

        // Apply theme
        applyTheme();

        // Check if user is logged in
        if (!isUserLoggedIn()) {
            navigateToLogin();
            return;
        }

        initializeComponents();
        setupDatabase();
        setupFormatters();
        setupEventListeners();
        setupBackPressHandler();
        debugDatabaseUsers();
        debugDashboardState();
        debugDatabaseState();
        loadUserData();
        loadFinancialData();
        setupRecyclerViews();
        startEntranceAnimations();

        // THÊM VÀO ĐẦU onCreate() của DashboardActivity
        Log.d("DashboardActivity", "=== TESTING SERVICE FROM DASHBOARD ===");

        try {
            ServiceManager serviceManager = new ServiceManager(this);
            Log.d("DashboardActivity", "ServiceManager created");

            boolean started = serviceManager.forceStartService();
            Log.d("DashboardActivity", "Force start service result: " + started);

            // Test notification
            NotificationHelper notificationHelper = new NotificationHelper(this);
            notificationHelper.showTestNotification();
            Log.d("DashboardActivity", "Test notification sent");

        } catch (Exception e) {
            Log.e("DashboardActivity", "Service test error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshData();
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_dashboard);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.d("Dashboard", "🔄 onDestroy - Cleaning up resources");

        // Close database connection
        if (database != null && database.isOpen()) {
            database.close();
            Log.d("Dashboard", "✅ Database closed successfully");
        }

        // Dismiss popup if showing
        if (profilePopup != null && profilePopup.isShowing()) {
            profilePopup.dismiss();
        }

        Log.d("Dashboard", "✅ All resources cleaned up");
    }

    private void debugDatabaseState() {
        Log.d("Dashboard", "=== DATABASE DEBUG ===");

        try {
            // Check if database is open
            if (database == null || !database.isOpen()) {
                Log.e("Dashboard", "❌ Database is null or closed!");
                return;
            }

            // Check current user
            int userId = sharedPreferences.getInt(KEY_USER_ID, -1);
            String username = sharedPreferences.getString(KEY_USERNAME, "unknown");
            Log.d("Dashboard", "Current user - ID: " + userId + ", Username: " + username);

            // Check if user exists in database
            String userCheckQuery = "SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_USERS +
                    " WHERE " + DatabaseHelper.COLUMN_ID + " = ?";
            Cursor userCursor = database.rawQuery(userCheckQuery, new String[]{String.valueOf(userId)});
            boolean userExists = false;
            if (userCursor.moveToFirst()) {
                userExists = userCursor.getInt(0) > 0;
            }
            userCursor.close();
            Log.d("Dashboard", "User exists in database: " + userExists);

            // Check transactions count for this user
            String transactionCheckQuery = "SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_TRANSACTIONS +
                    " WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ?";
            Cursor transactionCursor = database.rawQuery(transactionCheckQuery, new String[]{String.valueOf(userId)});
            int transactionCount = 0;
            if (transactionCursor.moveToFirst()) {
                transactionCount = transactionCursor.getInt(0);
            }
            transactionCursor.close();
            Log.d("Dashboard", "Transactions count for user: " + transactionCount);

            // List all transactions for this user
            String allTransactionsQuery = "SELECT " + DatabaseHelper.COLUMN_ID + ", " +
                    DatabaseHelper.COLUMN_TYPE + ", " +
                    DatabaseHelper.COLUMN_AMOUNT + ", " +
                    DatabaseHelper.COLUMN_DATE + " FROM " +
                    DatabaseHelper.TABLE_TRANSACTIONS +
                    " WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ?";
            Cursor allCursor = database.rawQuery(allTransactionsQuery, new String[]{String.valueOf(userId)});

            Log.d("Dashboard", "=== USER TRANSACTIONS ===");
            if (allCursor.moveToFirst()) {
                do {
                    Log.d("Dashboard", "Transaction - ID: " + allCursor.getLong(0) +
                            ", Type: " + allCursor.getString(1) +
                            ", Amount: " + allCursor.getDouble(2) +
                            ", Date: " + allCursor.getString(3));
                } while (allCursor.moveToNext());
            } else {
                Log.d("Dashboard", "No transactions found for this user");
            }
            allCursor.close();

        } catch (Exception e) {
            Log.e("Dashboard", "❌ Error in debugDatabaseState: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupStatusBar() {
        // Make status bar transparent
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );
    }

    private boolean isUserLoggedIn() {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    private void initializeComponents() {
        // Initialize all UI components
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        tvUserName = findViewById(R.id.tv_user_name);
        tvTotalBalance = findViewById(R.id.tv_total_balance);
        tvBalanceChange = findViewById(R.id.tv_balance_change);
        tvTotalIncome = findViewById(R.id.tv_total_income);
        tvIncomeChange = findViewById(R.id.tv_income_change);
        tvTotalExpense = findViewById(R.id.tv_total_expense);
        tvExpenseChange = findViewById(R.id.tv_expense_change);
        tvFinancialScore = findViewById(R.id.tv_financial_score);
        tvSavingsRate = findViewById(R.id.tv_savings_rate);
        tvTransactionsSummary = findViewById(R.id.tv_transactions_summary);
        tvVisibilityIcon = findViewById(R.id.tv_visibility_icon);
        ivProfileAvatar = findViewById(R.id.iv_profile_avatar);

        pbSavingsRate = findViewById(R.id.pb_savings_rate);

        btnThemeToggle = findViewById(R.id.btn_theme_toggle);
        btnNotifications = findViewById(R.id.btn_notifications);
        btnToggleBalanceVisibility = findViewById(R.id.btn_toggle_balance_visibility);
        profileCard = findViewById(R.id.profile_card);
        cardIncome = findViewById(R.id.card_income);
        cardExpense = findViewById(R.id.card_expense);
        actionAddIncome = findViewById(R.id.action_add_income);
        actionAddExpense = findViewById(R.id.action_add_expense);

        btnViewAllInsights = findViewById(R.id.btn_view_all_insights);
        btnViewAllTransactions = findViewById(R.id.btn_view_all_transactions);
        btnAddFirstTransaction = findViewById(R.id.btn_add_first_transaction);

        fabAddTransaction = findViewById(R.id.fab_add_transaction);

        rvAiInsights = findViewById(R.id.rv_ai_insights);
        rvRecentTransactions = findViewById(R.id.rv_recent_transactions);

        layoutEmptyTransactions = findViewById(R.id.layout_empty_transactions);
        cardDefaultInsight = findViewById(R.id.card_default_insight);
        bottomNavigation = findViewById(R.id.bottom_navigation);
        notificationBadge = findViewById(R.id.notification_badge);

        // Debug từng bước
        Log.d("Dashboard", "=== DEBUGGING BOTTOM NAVIGATION ===");

        bottomNavigation = findViewById(R.id.bottom_navigation);

        if (bottomNavigation == null) {
            Log.e("Dashboard", "❌ bottomNavigation is NULL - ID không tìm thấy!");
        } else {
            Log.d("Dashboard", "✅ bottomNavigation found");
            Log.d("Dashboard", "Width: " + bottomNavigation.getWidth() + ", Height: " + bottomNavigation.getHeight());
            Log.d("Dashboard", "LayoutParams: " + bottomNavigation.getLayoutParams());

            // Force visible
            bottomNavigation.setVisibility(View.VISIBLE);
            bottomNavigation.bringToFront();

            // Kiểm tra menu
            if (bottomNavigation.getMenu() != null) {
                Log.d("Dashboard", "Menu size: " + bottomNavigation.getMenu().size());
            } else {
                Log.e("Dashboard", "❌ Menu is NULL!");
            }
        }
    }

    private void setupDatabase() {
        try {
            dbHelper = new DatabaseHelper(this);
            database = dbHelper.getReadableDatabase(); // ✅ Mở database 1 lần

            Log.d("Dashboard", "✅ Database opened successfully");

            // ✅ Verify database is ready
            if (dbHelper.isDatabaseReady()) {
                Log.d("Dashboard", "✅ Database is ready with default categories");
            } else {
                Log.e("Dashboard", "❌ Database is not properly initialized");
            }

        } catch (Exception e) {
            Log.e("Dashboard", "❌ Error setting up database: " + e.getMessage());
            e.printStackTrace();

            // Show error to user
            showToast("Lỗi khởi tạo cơ sở dữ liệu. Vui lòng thử lại.");
            finish();
        }
    }

    private void setupFormatters() {
        currencyFormat = new DecimalFormat("#,###,### đ");
        dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    }

    private void setupRecyclerViews() {
        // Setup AI Insights RecyclerView
        if (rvAiInsights != null) {
            rvAiInsights.setLayoutManager(new LinearLayoutManager(this));
            rvAiInsights.setNestedScrollingEnabled(false);
        }

        // Setup Recent Transactions RecyclerView
        if (rvRecentTransactions != null) {
            rvRecentTransactions.setLayoutManager(new LinearLayoutManager(this));
            rvRecentTransactions.setNestedScrollingEnabled(false);
        }
    }

    private void setupEventListeners() {
        // Swipe refresh
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(this::refreshData);
            swipeRefreshLayout.setColorSchemeResources(
                    R.color.primary,
                    R.color.secondary,
                    R.color.accent
            );
        }

        // Balance visibility toggle
        if (btnToggleBalanceVisibility != null) {
            btnToggleBalanceVisibility.setOnClickListener(v -> toggleBalanceVisibility());
        }

        // Theme toggle
        if (btnThemeToggle != null) {
            btnThemeToggle.setOnClickListener(v -> toggleTheme());
        }

        // Profile card click - show enhanced popup
        if (profileCard != null) {
            profileCard.setOnClickListener(v -> showEnhancedProfileMenu());
        }

        // Notifications
        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(v -> openNotifications());
        }

        // Card clicks
        if (cardIncome != null) {
            cardIncome.setOnClickListener(v -> openIncomeDetails());
        }

        if (cardExpense != null) {
            cardExpense.setOnClickListener(v -> openExpenseDetails());
        }

        // Action buttons
        if (actionAddIncome != null) {
            actionAddIncome.setOnClickListener(v -> openAddTransaction("income"));
        }

        if (actionAddExpense != null) {
            actionAddExpense.setOnClickListener(v -> openAddTransaction("expense"));
        }

        if (btnAddFirstTransaction != null) {
            btnAddFirstTransaction.setOnClickListener(v -> openAddTransaction());
        }

        // View all buttons
        if (btnViewAllInsights != null) {
            btnViewAllInsights.setOnClickListener(v -> openAllInsights());
        }

        if (btnViewAllTransactions != null) {
            btnViewAllTransactions.setOnClickListener(v -> openAllTransactions());
        }

        // Floating action button
        if (fabAddTransaction != null) {
            fabAddTransaction.setOnClickListener(v -> openAddTransaction());
        }

        // Setup bottom navigation
        if (bottomNavigation != null) {
            bottomNavigation.setOnItemSelectedListener(this::onNavigationItemSelected);
            bottomNavigation.setSelectedItemId(R.id.nav_dashboard);
        }
    }

    private void loadUserData() {
        String username = sharedPreferences.getString(KEY_USERNAME, "User");
        if (tvUserName != null) {
            tvUserName.setText(username);
        }

        // Load profile avatar if available
        // TODO: Implement profile picture loading from storage or server

        // Update greeting based on time
        updateGreeting();
    }

    private void updateGreeting() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        String greeting;
        if (hour < 12) {
            greeting = "Chào buổi sáng! ☀️";
        } else if (hour < 18) {
            greeting = "Chào buổi chiều! 🌤️";
        } else {
            greeting = "Chào buổi tối! 🌙";
        }

        // Find greeting TextView in header and update it
        // This assumes you have a greeting TextView in your layout
    }

    private void loadFinancialData() {
        Log.d("Dashboard", "🔄 Starting loadFinancialData...");

        // ✅ KIỂM TRA database trước khi sử dụng
        if (database == null || !database.isOpen()) {
            Log.e("Dashboard", "❌ Database is null or closed!");

            // Thử mở lại database
            try {
                database = dbHelper.getReadableDatabase();
                Log.d("Dashboard", "✅ Database reopened successfully");
            } catch (Exception e) {
                Log.e("Dashboard", "❌ Failed to reopen database: " + e.getMessage());
                setDefaultFinancialValues();
                return;
            }
        }

        try {
            int userId = sharedPreferences.getInt(KEY_USER_ID, -1);
            String username = sharedPreferences.getString(KEY_USERNAME, "");

            Log.d("Dashboard", "🔍 Loading data - UserID: " + userId + ", Username: " + username);

            // ✅ FIX: Nếu userId = -1, thử lấy lại từ username
            if (userId == -1 && !username.isEmpty()) {
                Log.w("Dashboard", "⚠️ userId is -1, trying to get from username...");
                userId = getUserIdFromUsername(username);

                if (userId != -1) {
                    // Save lại userId vào SharedPreferences
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putInt(KEY_USER_ID, userId);
                    editor.apply();
                    Log.d("Dashboard", "✅ Recovered userId: " + userId);
                }
            }

            if (userId == -1) {
                Log.e("Dashboard", "❌ Invalid userId: " + userId);
                showToast("Lỗi: Phiên đăng nhập không hợp lệ");
                navigateToLogin();
                return;
            }

            // ✅ Reset values trước khi load
            resetFinancialValues();

            // ✅ Verify user exists TRƯỚC KHI load data
            if (!verifyUserExists(userId)) {
                Log.e("Dashboard", "❌ User does not exist in database!");
                showToast("Lỗi: Tài khoản không tồn tại");
                navigateToLogin();
                return;
            }

            // Load data với error handling
            boolean dataLoaded = loadCurrentMonthDataSafe(userId);
            if (dataLoaded) {
                loadPreviousMonthDataSafe(userId);
                calculateFinancialMetrics();
                updateFinancialUI();
                loadRecentTransactionsFixed(userId);
                checkNotificationsFixed(userId);

                Log.d("Dashboard", "✅ Successfully loaded all financial data");
            } else {
                Log.w("Dashboard", "⚠️ Failed to load data, using defaults");
                setDefaultFinancialValues();
            }

        } catch (Exception e) {
            Log.e("Dashboard", "❌ Error loading financial data: " + e.getMessage());
            e.printStackTrace();
            setDefaultFinancialValues();
            showToast("Lỗi tải dữ liệu: " + e.getMessage());
        }
    }

    // ✅ 4. THÊM HELPER METHODS
    private int getUserIdFromUsername(String username) {
        if (username == null || username.isEmpty()) {
            Log.e("Dashboard", "❌ Username is null or empty");
            return -1;
        }

        try {
            // ✅ SỬ DỤNG database instance hiện tại thay vì tạo mới
            if (database == null || !database.isOpen()) {
                Log.e("Dashboard", "❌ Database is not open");
                return -1;
            }

            String query = "SELECT " + DatabaseHelper.COLUMN_ID + " FROM " + DatabaseHelper.TABLE_USERS +
                    " WHERE " + DatabaseHelper.COLUMN_USERNAME + " = ?";

            Cursor cursor = database.rawQuery(query, new String[]{username});
            int userId = -1;

            if (cursor.moveToFirst()) {
                userId = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID));
            }
            cursor.close();

            Log.d("Dashboard", "✅ getUserIdFromUsername - Username: " + username + ", UserID: " + userId);
            return userId;

        } catch (Exception e) {
            Log.e("Dashboard", "❌ Error getting userId from username: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }

    private void resetFinancialValues() {
        totalIncome = 0.0;
        totalExpense = 0.0;
        totalBalance = 0.0;
        previousMonthIncome = 0.0;
        previousMonthExpense = 0.0;
        previousMonthBalance = 0.0;
        transactionCount = 0;
    }

    private boolean loadCurrentMonthDataSafe(int userId) {
        Log.d("Dashboard", "=== Loading All Time Data (Safe) ===");

        try {
            // Kiểm tra database trước khi sử dụng
            if (database == null || !database.isOpen()) {
                Log.e("Dashboard", "❌ Database is not available");
                return false;
            }

            // ✅ BỎ filter theo ngày tháng - lấy tất cả
            Log.d("Dashboard", "📅 Query all transactions for user: " + userId);

            // Query tổng thu nhập tích lũy
            String incomeQuery = "SELECT COALESCE(SUM(" + DatabaseHelper.COLUMN_AMOUNT + "), 0) FROM " +
                    DatabaseHelper.TABLE_TRANSACTIONS +
                    " WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ? AND " +
                    DatabaseHelper.COLUMN_TYPE + " = 'income'";

            Cursor incomeCursor = database.rawQuery(incomeQuery, new String[]{String.valueOf(userId)});

            if (incomeCursor != null && incomeCursor.moveToFirst()) {
                totalIncome = incomeCursor.getDouble(0);
                Log.d("Dashboard", "✅ Total cumulative income: " + totalIncome);
            }
            if (incomeCursor != null) incomeCursor.close();

            // Query tổng chi phí tích lũy
            String expenseQuery = "SELECT COALESCE(SUM(" + DatabaseHelper.COLUMN_AMOUNT + "), 0) FROM " +
                    DatabaseHelper.TABLE_TRANSACTIONS +
                    " WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ? AND " +
                    DatabaseHelper.COLUMN_TYPE + " = 'expense'";

            Cursor expenseCursor = database.rawQuery(expenseQuery, new String[]{String.valueOf(userId)});

            if (expenseCursor != null && expenseCursor.moveToNext()) {
                totalExpense = expenseCursor.getDouble(0);
                Log.d("Dashboard", "✅ Total cumulative expense: " + totalExpense);
            }
            if (expenseCursor != null) expenseCursor.close();

            // Tính số dư tích lũy
            totalBalance = totalIncome - totalExpense;
            Log.d("Dashboard", "✅ Calculated cumulative balance: " + totalBalance);

            return true;

        } catch (Exception e) {
            Log.e("Dashboard", "❌ Error in loadCurrentMonthDataSafe: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }


    private boolean verifyUserExists(int userId) {
        try {
            if (database == null || !database.isOpen()) {
                Log.e("Dashboard", "❌ Database is not available for user verification");
                return false;
            }

            String query = "SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_USERS +
                    " WHERE " + DatabaseHelper.COLUMN_ID + " = ?";
            Cursor cursor = database.rawQuery(query, new String[]{String.valueOf(userId)});

            boolean exists = false;
            if (cursor != null && cursor.moveToFirst()) {
                exists = cursor.getInt(0) > 0;
            }
            if (cursor != null) cursor.close();

            Log.d("Dashboard", "✅ verifyUserExists - UserID: " + userId + ", Exists: " + exists);
            return exists;

        } catch (Exception e) {
            Log.e("Dashboard", "❌ Error verifying user exists: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

//    private boolean loadPreviousMonthDataSafe(int userId) {
//        Log.d("Dashboard", "=== Loading Previous Month Data (Safe) ===");
//
//        try {
//            SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
//
//            // Get previous month dates
//            Calendar calendar = Calendar.getInstance();
//            calendar.add(Calendar.MONTH, -1);
//            calendar.set(Calendar.DAY_OF_MONTH, 1);
//            String prevMonthStart = dbDateFormat.format(calendar.getTime());
//
//            calendar.add(Calendar.MONTH, 1);
//            calendar.add(Calendar.DAY_OF_MONTH, -1);
//            String prevMonthEnd = dbDateFormat.format(calendar.getTime());
//
//            // Similar queries for previous month...
//            String incomeQuery = "SELECT COALESCE(SUM(" + DatabaseHelper.COLUMN_AMOUNT + "), 0) FROM " +
//                    DatabaseHelper.TABLE_TRANSACTIONS +
//                    " WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ? AND " +
//                    DatabaseHelper.COLUMN_TYPE + " = 'income' AND " +
//                    "date(" + DatabaseHelper.COLUMN_DATE + ") BETWEEN date(?) AND date(?)";
//
//            Cursor incomeCursor = database.rawQuery(incomeQuery, new String[]{
//                    String.valueOf(userId), prevMonthStart, prevMonthEnd
//            });
//
//            if (incomeCursor != null && incomeCursor.moveToFirst()) {
//                previousMonthIncome = incomeCursor.getDouble(0);
//            }
//            if (incomeCursor != null) incomeCursor.close();
//
//            String expenseQuery = "SELECT COALESCE(SUM(" + DatabaseHelper.COLUMN_AMOUNT + "), 0) FROM " +
//                    DatabaseHelper.TABLE_TRANSACTIONS +
//                    " WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ? AND " +
//                    DatabaseHelper.COLUMN_TYPE + " = 'expense' AND " +
//                    "date(" + DatabaseHelper.COLUMN_DATE + ") BETWEEN date(?) AND date(?)";
//
//            Cursor expenseCursor = database.rawQuery(expenseQuery, new String[]{
//                    String.valueOf(userId), prevMonthStart, prevMonthEnd
//            });
//
//            if (expenseCursor != null && expenseCursor.moveToFirst()) {
//                previousMonthExpense = expenseCursor.getDouble(0);
//            }
//            if (expenseCursor != null) expenseCursor.close();
//
//            previousMonthBalance = previousMonthIncome - previousMonthExpense;
//            Log.d("Dashboard", "✅ Previous month data loaded");
//
//            return true;
//
//        } catch (Exception e) {
//            Log.e("Dashboard", "❌ Error in loadPreviousMonthDataSafe: " + e.getMessage());
//            return false;
//        }
//    }

    private boolean loadPreviousMonthDataSafe(int userId) {
        // Không cần tính tháng trước nữa vì đã tính tích lũy
        previousMonthIncome = 0.0;
        previousMonthExpense = 0.0;
        previousMonthBalance = 0.0;

        Log.d("Dashboard", "✅ Skipped previous month data (using cumulative)");
        return true;
    }

    private void loadCurrentMonthDataFixed(int userId) {
        Log.d("Dashboard", "=== Loading Current Month Data ===");

        try {
            // Sử dụng format ngày chuẩn SQLite
            SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

            // Get current month dates
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            String monthStart = dbDateFormat.format(calendar.getTime());

            calendar.add(Calendar.MONTH, 1);
            calendar.add(Calendar.DAY_OF_MONTH, -1);
            String monthEnd = dbDateFormat.format(calendar.getTime());

            Log.d("Dashboard", "Query date range: " + monthStart + " to " + monthEnd);

            // ✅ Query income với COALESCE để handle NULL
            String incomeQuery = "SELECT COALESCE(SUM(" + DatabaseHelper.COLUMN_AMOUNT + "), 0) FROM " +
                    DatabaseHelper.TABLE_TRANSACTIONS +
                    " WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ? AND " +
                    DatabaseHelper.COLUMN_TYPE + " = 'income' AND " +
                    "date(" + DatabaseHelper.COLUMN_DATE + ") BETWEEN date(?) AND date(?)";

            Log.d("Dashboard", "Executing income query: " + incomeQuery);
            Log.d("Dashboard", "Query params: userId=" + userId + ", start=" + monthStart + ", end=" + monthEnd);

            Cursor incomeCursor = database.rawQuery(incomeQuery, new String[]{
                    String.valueOf(userId), monthStart, monthEnd
            });

            if (incomeCursor.moveToFirst()) {
                totalIncome = incomeCursor.getDouble(0);
                Log.d("Dashboard", "✅ Total income loaded: " + totalIncome);
            } else {
                Log.w("Dashboard", "⚠️ No income data found");
            }
            incomeCursor.close();

            // ✅ Query expenses với COALESCE
            String expenseQuery = "SELECT COALESCE(SUM(" + DatabaseHelper.COLUMN_AMOUNT + "), 0) FROM " +
                    DatabaseHelper.TABLE_TRANSACTIONS +
                    " WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ? AND " +
                    DatabaseHelper.COLUMN_TYPE + " = 'expense' AND " +
                    "date(" + DatabaseHelper.COLUMN_DATE + ") BETWEEN date(?) AND date(?)";

            Log.d("Dashboard", "Executing expense query: " + expenseQuery);

            Cursor expenseCursor = database.rawQuery(expenseQuery, new String[]{
                    String.valueOf(userId), monthStart, monthEnd
            });

            if (expenseCursor.moveToFirst()) {
                totalExpense = expenseCursor.getDouble(0);
                Log.d("Dashboard", "✅ Total expense loaded: " + totalExpense);
            } else {
                Log.w("Dashboard", "⚠️ No expense data found");
            }
            expenseCursor.close();

            // Calculate total balance
            totalBalance = totalIncome - totalExpense;
            Log.d("Dashboard", "✅ Calculated balance: " + totalBalance);

            // ✅ Count transactions in last 7 days
            Calendar recentDate = Calendar.getInstance();
            recentDate.add(Calendar.DAY_OF_MONTH, -7);
            String recentDateStr = dbDateFormat.format(recentDate.getTime());

            String countQuery = "SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_TRANSACTIONS +
                    " WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ? AND " +
                    "date(" + DatabaseHelper.COLUMN_DATE + ") >= date(?)";

            Cursor countCursor = database.rawQuery(countQuery, new String[]{
                    String.valueOf(userId), recentDateStr
            });

            if (countCursor.moveToFirst()) {
                transactionCount = countCursor.getInt(0);
                Log.d("Dashboard", "✅ Transaction count (7 days): " + transactionCount);
            }
            countCursor.close();

        } catch (Exception e) {
            Log.e("Dashboard", "❌ Error in loadCurrentMonthDataFixed: " + e.getMessage());
            e.printStackTrace();
            throw e; // Re-throw để parent method handle
        }
    }

    private void loadPreviousMonthDataFixed(int userId) {
        Log.d("Dashboard", "=== Loading Previous Month Data ===");

        try {
            SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

            // Get previous month dates
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MONTH, -1);
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            String prevMonthStart = dbDateFormat.format(calendar.getTime());

            calendar.add(Calendar.MONTH, 1);
            calendar.add(Calendar.DAY_OF_MONTH, -1);
            String prevMonthEnd = dbDateFormat.format(calendar.getTime());

            Log.d("Dashboard", "Previous month range: " + prevMonthStart + " to " + prevMonthEnd);

            // Query previous month income với COALESCE
            String incomeQuery = "SELECT COALESCE(SUM(" + DatabaseHelper.COLUMN_AMOUNT + "), 0) FROM " +
                    DatabaseHelper.TABLE_TRANSACTIONS +
                    " WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ? AND " +
                    DatabaseHelper.COLUMN_TYPE + " = 'income' AND " +
                    "date(" + DatabaseHelper.COLUMN_DATE + ") BETWEEN date(?) AND date(?)";

            Cursor incomeCursor = database.rawQuery(incomeQuery, new String[]{
                    String.valueOf(userId), prevMonthStart, prevMonthEnd
            });

            if (incomeCursor.moveToFirst()) {
                previousMonthIncome = incomeCursor.getDouble(0);
                Log.d("Dashboard", "✅ Previous month income: " + previousMonthIncome);
            }
            incomeCursor.close();

            // Query previous month expenses với COALESCE
            String expenseQuery = "SELECT COALESCE(SUM(" + DatabaseHelper.COLUMN_AMOUNT + "), 0) FROM " +
                    DatabaseHelper.TABLE_TRANSACTIONS +
                    " WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ? AND " +
                    DatabaseHelper.COLUMN_TYPE + " = 'expense' AND " +
                    "date(" + DatabaseHelper.COLUMN_DATE + ") BETWEEN date(?) AND date(?)";

            Cursor expenseCursor = database.rawQuery(expenseQuery, new String[]{
                    String.valueOf(userId), prevMonthStart, prevMonthEnd
            });

            if (expenseCursor.moveToFirst()) {
                previousMonthExpense = expenseCursor.getDouble(0);
                Log.d("Dashboard", "✅ Previous month expense: " + previousMonthExpense);
            }
            expenseCursor.close();

            previousMonthBalance = previousMonthIncome - previousMonthExpense;
            Log.d("Dashboard", "✅ Previous month balance: " + previousMonthBalance);

        } catch (Exception e) {
            Log.e("Dashboard", "❌ Error in loadPreviousMonthDataFixed: " + e.getMessage());
            e.printStackTrace();
            // Don't re-throw, just log - previous month data is not critical
        }
    }

//    private void loadRecentTransactionsFixed(int userId) {
//        Log.d("Dashboard", "=== Counting Recent Transactions (24h) ===");
//
//        try {
//            // ✅ FIX 1: Sử dụng format date thay vì datetime
//            SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
//            Calendar calendar = Calendar.getInstance();
//            calendar.add(Calendar.DAY_OF_MONTH, -1); // ✅ FIX 2: 1 ngày trước (24h)
//            String yesterday = dbDateFormat.format(calendar.getTime());
//
//            Log.d("Dashboard", "📅 Counting transactions after: " + yesterday);
//
//            // ✅ FIX 3: Sử dụng COLUMN_DATE thay vì COLUMN_CREATED_AT
//            String countQuery = "SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_TRANSACTIONS +
//                    " WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ? AND " +
//                    "date(" + DatabaseHelper.COLUMN_DATE + ") >= date(?)";
//
//            Log.d("Dashboard", "🔍 Query: " + countQuery);
//            Log.d("Dashboard", "🔍 Params: userId=" + userId + ", date>=" + yesterday);
//
//            Cursor cursor = database.rawQuery(countQuery, new String[]{
//                    String.valueOf(userId), yesterday
//            });
//
//            int transactionCount = 0;
//            if (cursor.moveToFirst()) {
//                transactionCount = cursor.getInt(0);
//            }
//            cursor.close();
//
//            Log.d("Dashboard", "📊 Found " + transactionCount + " transactions in last 24h for user " + userId);
//
//            // ✅ Cập nhật UI với số lượng thực tế
//            final int finalCount = transactionCount;
//            runOnUiThread(() -> {
//                updateTransactionCountUI(finalCount);
//            });
//
//            // ✅ THÊM: Debug chi tiết các giao dịch
//            debugUserTransactions(userId);
//
//        } catch (Exception e) {
//            Log.e("Dashboard", "❌ Error counting recent transactions: " + e.getMessage());
//            e.printStackTrace();
//
//            // Fallback - hiển thị 0
//            runOnUiThread(() -> {
//                updateTransactionCountUI(0);
//            });
//        }
//    }
private void loadRecentTransactionsFixed(int userId) {
    Log.d("Dashboard", "=== Counting All Transactions ===");

    try {
        // Đếm tất cả giao dịch
        String countQuery = "SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_TRANSACTIONS +
                " WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ?";

        Log.d("Dashboard", "🔍 Query: " + countQuery);

        Cursor cursor = database.rawQuery(countQuery, new String[]{String.valueOf(userId)});

        int transactionCount = 0;
        if (cursor.moveToFirst()) {
            transactionCount = cursor.getInt(0);
        }
        cursor.close();

        Log.d("Dashboard", "📊 Found " + transactionCount + " total transactions for user " + userId);

        // Cập nhật UI
        final int finalCount = transactionCount;
        runOnUiThread(() -> {
            if (tvTransactionsSummary != null) {
                tvTransactionsSummary.setText(finalCount + " tổng số giao dịch");
            }
        });

    } catch (Exception e) {
        Log.e("Dashboard", "❌ Error counting transactions: " + e.getMessage());
        e.printStackTrace();
    }
}
    private void debugUserTransactions(int userId) {
        try {
            String debugQuery = "SELECT " + DatabaseHelper.COLUMN_ID + ", " +
                    DatabaseHelper.COLUMN_TYPE + ", " +
                    DatabaseHelper.COLUMN_AMOUNT + ", " +
                    DatabaseHelper.COLUMN_DATE + ", " +
                    DatabaseHelper.COLUMN_CREATED_AT + " FROM " +
                    DatabaseHelper.TABLE_TRANSACTIONS +
                    " WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ? " +
                    "ORDER BY " + DatabaseHelper.COLUMN_DATE + " DESC LIMIT 5";

            Cursor cursor = database.rawQuery(debugQuery, new String[]{String.valueOf(userId)});

            Log.d("Dashboard", "=== DEBUG USER TRANSACTIONS ===");
            Log.d("Dashboard", "Total transactions for user " + userId + ": " + cursor.getCount());

            if (cursor.moveToFirst()) {
                do {
                    long id = cursor.getLong(0);
                    String type = cursor.getString(1);
                    double amount = cursor.getDouble(2);
                    String date = cursor.getString(3);
                    String createdAt = cursor.getString(4);

                    Log.d("Dashboard", "Transaction ID:" + id + " | Type:" + type +
                            " | Amount:" + amount + " | Date:" + date + " | Created:" + createdAt);
                } while (cursor.moveToNext());
            } else {
                Log.w("Dashboard", "⚠️ No transactions found for user " + userId);
            }
            cursor.close();

        } catch (Exception e) {
            Log.e("Dashboard", "❌ Error in debugUserTransactions: " + e.getMessage());
        }
    }


    private void updateTransactionCountUI(int count) {
        try {
            // ✅ Cập nhật text với số lượng thực tế
            if (tvTransactionsSummary != null) {
                String summaryText = count + " giao dịch trong 24h qua";
                tvTransactionsSummary.setText(summaryText);
                Log.d("Dashboard", "✅ Updated transaction summary: " + summaryText);
            }

            // ✅ Hiển thị empty state nếu không có giao dịch
            boolean hasTransactions = count > 0;

            if (layoutEmptyTransactions != null) {
                layoutEmptyTransactions.setVisibility(hasTransactions ? View.GONE : View.VISIBLE);
            }

            if (rvRecentTransactions != null) {
                // Nếu bạn có RecyclerView cho recent transactions thì ẩn đi
                // vì bạn chỉ muốn hiển thị số lượng
                rvRecentTransactions.setVisibility(View.GONE);
            }

        } catch (Exception e) {
            Log.e("Dashboard", "❌ Error updating transaction count UI: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void checkNotificationsFixed(int userId) {
        Log.d("Dashboard", "=== Checking Notifications ===");

        try {
            // Check if notifications table exists first
            String checkTableQuery = "SELECT name FROM sqlite_master WHERE type='table' AND name='notifications'";
            Cursor tableCheck = database.rawQuery(checkTableQuery, null);
            boolean notificationsTableExists = tableCheck.moveToFirst();
            tableCheck.close();

            if (!notificationsTableExists) {
                Log.d("Dashboard", "⚠️ Notifications table doesn't exist yet");
                if (notificationBadge != null) {
                    notificationBadge.setVisibility(View.GONE);
                }
                return;
            }

            String query = "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = 0";
            Cursor cursor = database.rawQuery(query, new String[]{String.valueOf(userId)});

            int unreadCount = 0;
            if (cursor.moveToFirst()) {
                unreadCount = cursor.getInt(0);
            }
            cursor.close();

            Log.d("Dashboard", "✅ Unread notifications: " + unreadCount);

            if (notificationBadge != null) {
                notificationBadge.setVisibility(unreadCount > 0 ? View.VISIBLE : View.GONE);
            }

        } catch (Exception e) {
            Log.e("Dashboard", "❌ Error checking notifications: " + e.getMessage());
            // Hide badge on error
            if (notificationBadge != null) {
                notificationBadge.setVisibility(View.GONE);
            }
        }
    }

    private void setDefaultFinancialValues() {
        Log.d("Dashboard", "Setting default financial values");

        totalIncome = 0.0;
        totalExpense = 0.0;
        totalBalance = 0.0;
        previousMonthIncome = 0.0;
        previousMonthExpense = 0.0;
        previousMonthBalance = 0.0;
        transactionCount = 0;

        // Update UI với default values
        runOnUiThread(() -> {
            updateFinancialUI();

            if (layoutEmptyTransactions != null) {
                layoutEmptyTransactions.setVisibility(View.VISIBLE);
            }
            if (rvRecentTransactions != null) {
                rvRecentTransactions.setVisibility(View.GONE);
            }
        });
    }
    private void loadCurrentMonthData(int userId) {
        // ✅ FIX: Sử dụng format ngày chuẩn cho query
        SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        // Get current month start and end dates
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        String monthStart = dbDateFormat.format(calendar.getTime());

        calendar.add(Calendar.MONTH, 1);
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        String monthEnd = dbDateFormat.format(calendar.getTime());

        // Query income với format ngày đúng
        String incomeQuery = "SELECT SUM(" + DatabaseHelper.COLUMN_AMOUNT + ") FROM " +
                DatabaseHelper.TABLE_TRANSACTIONS +
                " WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ? AND " +
                DatabaseHelper.COLUMN_TYPE + " = 'income' AND " +
                "date(" + DatabaseHelper.COLUMN_DATE + ") BETWEEN date(?) AND date(?)";

        Cursor incomeCursor = database.rawQuery(incomeQuery, new String[]{
                String.valueOf(userId), monthStart, monthEnd
        });

        if (incomeCursor.moveToFirst()) {
            totalIncome = incomeCursor.getDouble(0);
        }
        incomeCursor.close();

        // Query expenses với format ngày đúng
        String expenseQuery = "SELECT SUM(" + DatabaseHelper.COLUMN_AMOUNT + ") FROM " +
                DatabaseHelper.TABLE_TRANSACTIONS +
                " WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ? AND " +
                DatabaseHelper.COLUMN_TYPE + " = 'expense' AND " +
                "date(" + DatabaseHelper.COLUMN_DATE + ") BETWEEN date(?) AND date(?)";

        Cursor expenseCursor = database.rawQuery(expenseQuery, new String[]{
                String.valueOf(userId), monthStart, monthEnd
        });

        if (expenseCursor.moveToFirst()) {
            totalExpense = expenseCursor.getDouble(0);
        }
        expenseCursor.close();

        // Calculate total balance
        totalBalance = totalIncome - totalExpense;

        // Count recent transactions (last 7 days)
        Calendar recentDate = Calendar.getInstance();
        recentDate.add(Calendar.DAY_OF_MONTH, -7);
        String recentDateStr = dbDateFormat.format(recentDate.getTime());

        String countQuery = "SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_TRANSACTIONS +
                " WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ? AND " +
                "date(" + DatabaseHelper.COLUMN_DATE + ") >= date(?)";

        Cursor countCursor = database.rawQuery(countQuery, new String[]{
                String.valueOf(userId), recentDateStr
        });

        if (countCursor.moveToFirst()) {
            transactionCount = countCursor.getInt(0);
        }
        countCursor.close();

        Log.d("Dashboard", "Loaded data - Income: " + totalIncome + ", Expense: " + totalExpense + ", Balance: " + totalBalance);
    }

    private void loadPreviousMonthData(int userId) {
        SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        // Get previous month dates
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -1);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        String prevMonthStart = dbDateFormat.format(calendar.getTime());

        calendar.add(Calendar.MONTH, 1);
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        String prevMonthEnd = dbDateFormat.format(calendar.getTime());

        // Query previous month income
        String incomeQuery = "SELECT SUM(" + DatabaseHelper.COLUMN_AMOUNT + ") FROM " +
                DatabaseHelper.TABLE_TRANSACTIONS +
                " WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ? AND " +
                DatabaseHelper.COLUMN_TYPE + " = 'income' AND " +
                "date(" + DatabaseHelper.COLUMN_DATE + ") BETWEEN date(?) AND date(?)";

        Cursor incomeCursor = database.rawQuery(incomeQuery, new String[]{
                String.valueOf(userId), prevMonthStart, prevMonthEnd
        });

        if (incomeCursor.moveToFirst()) {
            previousMonthIncome = incomeCursor.getDouble(0);
        }
        incomeCursor.close();

        // Query previous month expenses
        String expenseQuery = "SELECT SUM(" + DatabaseHelper.COLUMN_AMOUNT + ") FROM " +
                DatabaseHelper.TABLE_TRANSACTIONS +
                " WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ? AND " +
                DatabaseHelper.COLUMN_TYPE + " = 'expense' AND " +
                "date(" + DatabaseHelper.COLUMN_DATE + ") BETWEEN date(?) AND date(?)";

        Cursor expenseCursor = database.rawQuery(expenseQuery, new String[]{
                String.valueOf(userId), prevMonthStart, prevMonthEnd
        });

        if (expenseCursor.moveToFirst()) {
            previousMonthExpense = expenseCursor.getDouble(0);
        }
        expenseCursor.close();

        previousMonthBalance = previousMonthIncome - previousMonthExpense;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            boolean transactionAdded = data.getBooleanExtra("transaction_added", false);
            if (transactionAdded) {
                Log.d("Dashboard", "Transaction added, refreshing data...");
                refreshData();
            }
        }
    }

    private void calculateFinancialMetrics() {
        // Calculate financial health score (0-100)
        int healthScore = 50; // Base score

        // Add points for positive balance
        if (totalBalance > 0) {
            healthScore += 20;
        }

        // Add points for high savings rate
        double savingsRate = totalIncome > 0 ? ((totalIncome - totalExpense) / totalIncome) * 100 : 0;
        if (savingsRate > 20) {
            healthScore += 15;
        }

        // Add points for consistent income growth
        if (totalIncome > previousMonthIncome) {
            healthScore += 15;
        }

        // Ensure score is within bounds
        healthScore = Math.min(100, Math.max(0, healthScore));

        // Update UI elements would be called from updateFinancialUI()
    }

    private void updateFinancialUI() {
        runOnUiThread(() -> {
            // Update balance
            if (tvTotalBalance != null) {
                String balanceText = isBalanceVisible ? currencyFormat.format(totalBalance) : "••••••• đ";
                tvTotalBalance.setText(balanceText);
            }

            // Update income
            if (tvTotalIncome != null) {
                String incomeText = isBalanceVisible ? currencyFormat.format(totalIncome) : "••••••• đ";
                tvTotalIncome.setText(incomeText);
            }

            // Update expense
            if (tvTotalExpense != null) {
                String expenseText = isBalanceVisible ? currencyFormat.format(totalExpense) : "••••••• đ";
                tvTotalExpense.setText(expenseText);
            }

            // Update balance change
//            if (tvBalanceChange != null && previousMonthBalance > 0) {
//                double changePercent = ((totalBalance - previousMonthBalance) / previousMonthBalance) * 100;
//                String changeText = String.format("%.1f%% so với tháng trước", Math.abs(changePercent));
//                if (changePercent >= 0) {
//                    changeText = "📈 +" + changeText;
//                } else {
//                    changeText = "📉 -" + changeText;
//                }
//                tvBalanceChange.setText(changeText);
//            }
            if (tvBalanceChange != null) {
                // Thay vì so sánh tháng trước, hiển thị thông tin khác
                String changeText = "Tổng số dư tích lũy";
                tvBalanceChange.setText(changeText);
            }

            // Update income change
//            if (tvIncomeChange != null && previousMonthIncome > 0) {
//                double changePercent = ((totalIncome - previousMonthIncome) / previousMonthIncome) * 100;
//                String changeText = String.format("↗ %+.1f%%", changePercent);
//                tvIncomeChange.setText(changeText);
//            }

            if (tvIncomeChange != null) {
                String changeText = "Tổng thu nhập";
                tvIncomeChange.setText(changeText);
            }


            // Update expense change
//            if (tvExpenseChange != null && previousMonthExpense > 0) {
//                double changePercent = ((totalExpense - previousMonthExpense) / previousMonthExpense) * 100;
//                String changeText = String.format("↗ %+.1f%%", changePercent);
//                tvExpenseChange.setText(changeText);
//            }
            if (tvExpenseChange != null) {
                String changeText = "Tổng chi tiêu";
                tvExpenseChange.setText(changeText);
            }

            // Update financial score
            int score = calculateHealthScore();
            if (tvFinancialScore != null) {
                tvFinancialScore.setText(score + "/100");
            }

            // Update savings rate
            double savingsRate = totalIncome > 0 ? ((totalIncome - totalExpense) / totalIncome) * 100 : 0;
            if (tvSavingsRate != null) {
                tvSavingsRate.setText(String.format("%.1f%%", savingsRate));
            }
            if (pbSavingsRate != null) {
                pbSavingsRate.setProgress((int) savingsRate);
            }

            // Update transactions summary
            if (tvTransactionsSummary != null) {
                tvTransactionsSummary.setText(transactionCount + " giao dịch trong 24h qua");
            }
        });
    }

//    private int calculateHealthScore() {
//        int score = 30; // Điểm cơ bản thấp hơn
//
//        // Điểm cho số dư
//        if (totalBalance > 0) {
//            score += totalBalance > (totalIncome * 3) ? 25 : 15; // Dự trữ khẩn cấp
//        }
//
//        // Điểm cho tỷ lệ tiết kiệm (phân cấp chi tiết hơn)
//        double savingsRate = totalIncome > 0 ? ((totalIncome - totalExpense) / totalIncome) * 100 : 0;
//        if (savingsRate >= 10 && savingsRate <= 30) score += 25;      // Lý tưởng
//        else if (savingsRate > 30 && savingsRate <= 50) score += 15;  // Hơi cao
//        else if (savingsRate > 50) score += 5;                        // Quá cao, không bền vững
//        else if (savingsRate > 0) score += 10;                        // Có tiết kiệm
//
//        // Điểm cho tăng trưởng thu nhập
//        if (totalIncome > previousMonthIncome) {
//            double growthRate = ((totalIncome - previousMonthIncome) / previousMonthIncome) * 100;
//            if (growthRate <= 20) score += 15;      // Tăng trưởng bền vững
//            else score += 10;                       // Tăng trưởng quá cao
//        }
//
//        // Điểm cho tính ổn định chi tiêu
//        if (previousMonthExpense > 0) {
//            double expenseChange = Math.abs((totalExpense - previousMonthExpense) / previousMonthExpense);
//            if (expenseChange < 0.1) score += 15;  // Chi tiêu ổn định
//        }
//
//        return Math.min(100, Math.max(0, score));
//    }
private int calculateHealthScore() {
    int score = 0;

    Log.d("Dashboard", "=== Calculating Health Score ===");
    Log.d("Dashboard", "Income: " + totalIncome + ", Expense: " + totalExpense + ", Balance: " + totalBalance);

    // 1. KIỂM TRA SỐ DƯ (50 điểm - quan trọng nhất)
    if (totalBalance < 0) {
        // Số dư âm = nguy hiểm tài chính
        score = 0; // Bắt đầu từ 0 nếu âm tiền
        Log.d("Dashboard", "Negative balance - CRITICAL: 0 points");
    } else if (totalBalance == 0) {
        score += 10; // Không âm nhưng không có dự phòng
    } else {
        // Số dư dương - tính theo khả năng chi trả
        if (totalExpense > 0) {
            double monthsCovered = totalBalance / (totalExpense / 12); // Ước tính số tháng có thể chi trả
            if (monthsCovered >= 6) {
                score += 50; // Dự trữ 6+ tháng = tuyệt vời
            } else if (monthsCovered >= 3) {
                score += 40; // 3-6 tháng = tốt
            } else if (monthsCovered >= 1) {
                score += 25; // 1-3 tháng = khá
            } else {
                score += 15; // Dưới 1 tháng = yếu
            }
        } else {
            score += 30; // Có tiền nhưng chưa có chi tiêu để đánh giá
        }
    }

    // 2. TỶ LỆ TIẾT KIỆM (30 điểm)
    if (totalIncome > 0) {
        double savingsRate = ((totalIncome - totalExpense) / totalIncome) * 100;

        if (savingsRate < 0) {
            score += 0; // Chi nhiều hơn thu = 0 điểm
        } else if (savingsRate >= 20) {
            score += 30; // Tiết kiệm 20%+ = xuất sắc
        } else if (savingsRate >= 10) {
            score += 25; // 10-20% = tốt
        } else if (savingsRate >= 5) {
            score += 15; // 5-10% = khá
        } else if (savingsRate > 0) {
            score += 10; // 0-5% = yếu nhưng có tiết kiệm
        }

        Log.d("Dashboard", "Savings rate: " + savingsRate + "%");
    }

    // 3. KIỂM SOÁT CHI TIÊU (20 điểm)
    if (totalIncome > 0 && totalExpense >= 0) {
        double expenseRatio = (totalExpense / totalIncome) * 100;

        if (expenseRatio <= 50) {
            score += 20; // Chi tiêu <= 50% thu nhập = tuyệt vời
        } else if (expenseRatio <= 70) {
            score += 15; // 50-70% = tốt
        } else if (expenseRatio <= 80) {
            score += 10; // 70-80% = khá
        } else if (expenseRatio <= 100) {
            score += 5; // 80-100% = yếu
        } else {
            score += 0; // Chi > thu = 0 điểm
        }
    }

    // PENALTY: Trừ điểm nghiêm trọng nếu tình hình tồi tệ
    if (totalBalance < 0 && totalIncome > 0) {
        double debtToIncomeRatio = Math.abs(totalBalance) / totalIncome;
        if (debtToIncomeRatio > 1.0) {
            score -= 20; // Nợ > thu nhập cả năm
        } else if (debtToIncomeRatio > 0.5) {
            score -= 10; // Nợ > 50% thu nhập năm
        }
    }

    // Đảm bảo không âm và không quá 100
    score = Math.min(100, Math.max(0, score));

    Log.d("Dashboard", "=== Final Health Score: " + score + " ===");
    return score;
}

    private void loadRecentTransactions(int userId) {
        // Load recent transactions and update RecyclerView
        // This would typically involve creating an adapter and setting it to the RecyclerView
        try {
            String query = "SELECT * FROM transactions WHERE user_id = ? ORDER BY date DESC LIMIT 5";
            Cursor cursor = database.rawQuery(query, new String[]{String.valueOf(userId)});

            boolean hasTransactions = cursor.getCount() > 0;

            if (layoutEmptyTransactions != null) {
                layoutEmptyTransactions.setVisibility(hasTransactions ? View.GONE : View.VISIBLE);
            }

            if (rvRecentTransactions != null) {
                rvRecentTransactions.setVisibility(hasTransactions ? View.VISIBLE : View.GONE);
            }

            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkNotifications(int userId) {
        // Check for pending notifications and update badge
        try {
            String query = "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = 0";
            Cursor cursor = database.rawQuery(query, new String[]{String.valueOf(userId)});

            int unreadCount = 0;
            if (cursor.moveToFirst()) {
                unreadCount = cursor.getInt(0);
            }
            cursor.close();

            if (notificationBadge != null) {
                notificationBadge.setVisibility(unreadCount > 0 ? View.VISIBLE : View.GONE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void refreshData() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(true);
        }

        // Simulate network delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            loadFinancialData();
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setRefreshing(false);
            }
            showToast("Dữ liệu đã được cập nhật");
        }, 1000);
    }

    private void toggleBalanceVisibility() {
        isBalanceVisible = !isBalanceVisible;

        // Update visibility icon
        if (tvVisibilityIcon != null) {
            tvVisibilityIcon.setText(isBalanceVisible ? "👁️" : "🙈");
        }

        // Update financial UI
        updateFinancialUI();

        // Add animation
        animateBalanceToggle();
    }

    private void animateBalanceToggle() {
        List<View> views = new ArrayList<>();
        if (tvTotalBalance != null) views.add(tvTotalBalance);
        if (tvTotalIncome != null) views.add(tvTotalIncome);
        if (tvTotalExpense != null) views.add(tvTotalExpense);

        for (View view : views) {
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.8f, 1f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.8f, 1f);
            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playTogether(scaleX, scaleY);
            animatorSet.setDuration(300);
            animatorSet.start();
        }
    }

    private void toggleTheme() {
        isDarkMode = !isDarkMode;

        // Save preference
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_DARK_MODE, isDarkMode);
        editor.apply();

        // Update theme icon
        if (btnThemeToggle != null) {
            TextView themeIcon = btnThemeToggle.findViewById(android.R.id.text1);
            if (themeIcon == null) {
                // Find the TextView in the MaterialCardView
                for (int i = 0; i < btnThemeToggle.getChildCount(); i++) {
                    View child = btnThemeToggle.getChildAt(i);
                    if (child instanceof TextView) {
                        themeIcon = (TextView) child;
                        break;
                    }
                }
            }
            if (themeIcon != null) {
                themeIcon.setText(isDarkMode ? "☀️" : "🌙");
            }
        }

        // Apply theme
        applyTheme();

        showToast(isDarkMode ? "Đã chuyển sang chế độ tối" : "Đã chuyển sang chế độ sáng");
    }

    private void applyTheme() {
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    private void showEnhancedProfileMenu() {
        // Create custom popup layout
        View popupView = LayoutInflater.from(this).inflate(R.layout.popup_profile_menu, null);

        // Create PopupWindow
        profilePopup = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );

        // Set popup background and animation
        profilePopup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        profilePopup.setElevation(16f);
        profilePopup.setAnimationStyle(R.style.PopupAnimation);

        // Get popup elements
        MaterialCardView profileOption = popupView.findViewById(R.id.profile_option);
        MaterialCardView logoutOption = popupView.findViewById(R.id.logout_option);
        TextView usernameText = popupView.findViewById(R.id.popup_username);
        TextView emailText = popupView.findViewById(R.id.popup_email);

        // Set user data
        String username = sharedPreferences.getString(KEY_USERNAME, "User");
        if (usernameText != null) {
            usernameText.setText(username);
        }
        if (emailText != null) {
            emailText.setText(username.toLowerCase() + "@moneymaster.com");
        }

        // Set click listeners
        if (profileOption != null) {
            profileOption.setOnClickListener(v -> {
                profilePopup.dismiss();
                openProfile();
            });
        }

        if (logoutOption != null) {
            logoutOption.setOnClickListener(v -> {
                profilePopup.dismiss();
                showLogoutDialog();
            });
        }

        // Show popup at anchor view (profile card)
        int[] location = new int[2];
        profileCard.getLocationOnScreen(location);

        profilePopup.showAtLocation(
                profileCard,
                Gravity.NO_GRAVITY,
                location[0] + profileCard.getWidth() / 2 - 100,
                location[1] + profileCard.getHeight() + 20
        );

        // Add dim background effect
        dimBackground(true);
        profilePopup.setOnDismissListener(() -> dimBackground(false));
    }

    private void dimBackground(boolean dim) {
        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        layoutParams.alpha = dim ? 0.7f : 1.0f;
        getWindow().setAttributes(layoutParams);
    }

    private boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_dashboard) {
            return true; // Already on dashboard
        } else if (id == R.id.nav_transactions) {
            openTransactions();
            return true;
        } else if (id == R.id.nav_analytics) {
            openAnalytics();
            return true;
        } else if (id == R.id.nav_budget) {
            openBudgetManager();
            return true;
        } else if (id == R.id.nav_goals) {
            openGoalsManager();
            return true;
        }

        return false;
    }

    private void setupBackPressHandler() {
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (profilePopup != null && profilePopup.isShowing()) {
                    profilePopup.dismiss();
                    return;
                }

                new AlertDialog.Builder(DashboardActivity.this)
                        .setTitle("Thoát ứng dụng")
                        .setMessage("Bạn có muốn thoát Money Master Pro?")
                        .setPositiveButton("Thoát", (dialog, which) -> finish())
                        .setNegativeButton("Hủy", null)
                        .show();
            }
        });
    }

    private void startEntranceAnimations() {
        // Animate cards with staggered entrance
        List<View> animatedViews = new ArrayList<>();

        // Add all major UI components for animation
        if (profileCard != null) animatedViews.add(profileCard);
        if (cardIncome != null) animatedViews.add(cardIncome);
        if (cardExpense != null) animatedViews.add(cardExpense);
        if (actionAddIncome != null) animatedViews.add(actionAddIncome);
        if (actionAddExpense != null) animatedViews.add(actionAddExpense);
        if (fabAddTransaction != null) animatedViews.add(fabAddTransaction);

        for (int i = 0; i < animatedViews.size(); i++) {
            View view = animatedViews.get(i);
            view.setAlpha(0f);
            view.setTranslationY(100f);

            view.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(500)
                    .setStartDelay(i * 100)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
        }
    }

    // Navigation methods
    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void openProfile() {
        Intent intent = new Intent(this, ProfileActivity.class);
        startActivity(intent);
    }

    private void openNotifications() {
        Intent intent = new Intent(this, NotificationsActivity.class);
        startActivity(intent);
    }

    private void openTransactions() {
        Intent intent = new Intent(this, TransactionsActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void openAnalytics() {
        Intent intent = new Intent(this, AnalyticsActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void openBudgetManager() {
        try {
            // ✅ FIX: Lấy currentUserId từ SharedPreferences
            int userId = sharedPreferences.getInt(KEY_USER_ID, -1);

            if (userId == -1) {
                Log.e("Dashboard", "❌ Cannot open Budget: Invalid user ID");
                showToast("Lỗi: Không xác định được người dùng");
                return;
            }

            Intent intent = new Intent(this, BudgetListActivity.class);
            // ✅ QUAN TRỌNG: Chuyển int thành String để SmartBudgetActivity có thể nhận
            intent.putExtra("user_id", String.valueOf(userId));
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);

            Log.d("Dashboard", "✅ Opening Budget with user_id: " + userId + " (as String)");

        } catch (Exception e) {
            Log.e("Dashboard", "❌ Error opening Budget: " + e.getMessage());
            showToast("Lỗi mở màn hình ngân sách: " + e.getMessage());
        }
    }

    private void openGoalsManager() {
        try {
            // ✅ FIX: Lấy currentUserId từ SharedPreferences
            int userId = sharedPreferences.getInt(KEY_USER_ID, -1);

            if (userId == -1) {
                Log.e("Dashboard", "❌ Cannot open Goals: Invalid user ID");
                showToast("Lỗi: Không xác định được người dùng");
                return;
            }

            Intent intent = new Intent(this, GoalsActivity.class);
            intent.putExtra("user_id", userId); // ✅ QUAN TRỌNG: Truyền user_id
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);

            Log.d("Dashboard", "✅ Opening Goals with user_id: " + userId);

        } catch (Exception e) {
            Log.e("Dashboard", "❌ Error opening Goals: " + e.getMessage());
            showToast("Lỗi mở màn hình mục tiêu: " + e.getMessage());
        }
    }

//    private void openAddTransaction() {
//        Intent intent = new Intent(this, AddTransactionActivity.class);
//        startActivity(intent);
//        overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_down);
//    }

    private void openAddTransaction(String type) {
        try {
            Intent intent = new Intent(this, AddTransactionActivity.class);
            if (type != null) {
                intent.putExtra("transaction_type", type);
            }
            startActivityForResult(intent, 100); // Sử dụng startActivityForResult
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi mở giao diện thêm giao dịch", Toast.LENGTH_SHORT).show();
        }
    }

    private void debugDatabaseUsers() {
        android.util.Log.d("Dashboard", "=== DEBUG DATABASE USERS ===");

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // 1. Kiểm tra tất cả users
        Cursor usersCursor = db.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_USERS, null);
        android.util.Log.d("Dashboard", "Total users in database: " + usersCursor.getCount());

        if (usersCursor.moveToFirst()) {
            do {
                int id = usersCursor.getInt(usersCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID));
                String username = usersCursor.getString(usersCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USERNAME));
                android.util.Log.d("Dashboard", "User ID: " + id + ", Username: " + username);
            } while (usersCursor.moveToNext());
        }
        usersCursor.close();

        // 2. Kiểm tra tất cả transactions theo từng user
        Cursor transactionsCursor = db.rawQuery(
                "SELECT " + DatabaseHelper.COLUMN_USER_ID + ", COUNT(*) as count " +
                        "FROM " + DatabaseHelper.TABLE_TRANSACTIONS +
                        " GROUP BY " + DatabaseHelper.COLUMN_USER_ID, null);

        android.util.Log.d("Dashboard", "Transactions by user:");
        if (transactionsCursor.moveToFirst()) {
            do {
                int userId = transactionsCursor.getInt(0);
                int count = transactionsCursor.getInt(1);
                android.util.Log.d("Dashboard", "User ID " + userId + ": " + count + " transactions");
            } while (transactionsCursor.moveToNext());
        }
        transactionsCursor.close();

        // 3. Kiểm tra current user
        int currentUserId = sharedPreferences.getInt(KEY_USER_ID, -1);
        String currentUsername = sharedPreferences.getString(KEY_USERNAME, "unknown");
        android.util.Log.d("Dashboard", "Current logged in - UserID: " + currentUserId + ", Username: " + currentUsername);

        db.close();
    }

    private void debugDashboardState() {
        Log.d("Dashboard", "=== DASHBOARD DEBUG STATE ===");

        // 1. Check SharedPreferences
        boolean isLoggedIn = sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
        int userId = sharedPreferences.getInt(KEY_USER_ID, -1);
        String username = sharedPreferences.getString(KEY_USERNAME, "");
        boolean rememberMe = sharedPreferences.getBoolean(KEY_REMEMBER_ME, false);

        Log.d("Dashboard", "SharedPreferences:");
        Log.d("Dashboard", "  IsLoggedIn: " + isLoggedIn);
        Log.d("Dashboard", "  UserID: " + userId);
        Log.d("Dashboard", "  Username: " + username);
        Log.d("Dashboard", "  RememberMe: " + rememberMe);

        // 2. Check database connection
        if (database == null) {
            Log.e("Dashboard", "❌ Database is NULL!");
            return;
        } else {
            Log.d("Dashboard", "✅ Database connected: " + database.isOpen());
        }

        // 3. Check if user exists in database
        if (userId != -1) {
            try {
                String userQuery = "SELECT * FROM " + DatabaseHelper.TABLE_USERS +
                        " WHERE " + DatabaseHelper.COLUMN_ID + " = ?";
                Cursor userCursor = database.rawQuery(userQuery, new String[]{String.valueOf(userId)});

                if (userCursor.moveToFirst()) {
                    String dbUsername = userCursor.getString(userCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USERNAME));
                    String dbEmail = userCursor.getString(userCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EMAIL));
                    Log.d("Dashboard", "✅ User found in DB - Username: " + dbUsername + ", Email: " + dbEmail);
                } else {
                    Log.e("Dashboard", "❌ User " + userId + " NOT found in database!");
                }
                userCursor.close();

                // 4. Check user's transactions
                String transQuery = "SELECT COUNT(*), " +
                        "COALESCE(SUM(CASE WHEN " + DatabaseHelper.COLUMN_TYPE + " = 'income' THEN " + DatabaseHelper.COLUMN_AMOUNT + " ELSE 0 END), 0) as income, " +
                        "COALESCE(SUM(CASE WHEN " + DatabaseHelper.COLUMN_TYPE + " = 'expense' THEN " + DatabaseHelper.COLUMN_AMOUNT + " ELSE 0 END), 0) as expense " +
                        "FROM " + DatabaseHelper.TABLE_TRANSACTIONS +
                        " WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ?";

                Cursor transCursor = database.rawQuery(transQuery, new String[]{String.valueOf(userId)});
                if (transCursor.moveToFirst()) {
                    int count = transCursor.getInt(0);
                    double income = transCursor.getDouble(1);
                    double expense = transCursor.getDouble(2);
                    Log.d("Dashboard", "User transactions - Count: " + count + ", Income: " + income + ", Expense: " + expense);
                }
                transCursor.close();

            } catch (Exception e) {
                Log.e("Dashboard", "❌ Error checking user in database: " + e.getMessage());
                e.printStackTrace();
            }
        }

        Log.d("Dashboard", "=== END DEBUG ===");
    }

    private void openAddTransaction() {
        openAddTransaction(null);
    }

    private void openIncomeDetails() {
        Intent intent = new Intent(this, IncomeDetailsActivity.class);
        intent.putExtra("total_income", totalIncome);
        intent.putExtra("income_change", totalIncome - previousMonthIncome);
        startActivity(intent);
    }

    private void openExpenseDetails() {
        Intent intent = new Intent(this, ExpenseDetailsActivity.class);
        intent.putExtra("total_expense", totalExpense);
        intent.putExtra("expense_change", totalExpense - previousMonthExpense);
        startActivity(intent);
    }

    private void openAllInsights() {
        Intent intent = new Intent(this, InsightsActivity.class);
        startActivity(intent);
    }

    private void openAllTransactions() {
        openTransactions();
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc chắn muốn đăng xuất khỏi tài khoản?")
                .setPositiveButton("Đăng xuất", (dialog, which) -> {
                    performLogout();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void performLogout() {
        Log.d("Dashboard", "🚪 Performing logout...");

        // Clear shared preferences
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, false);

        // ✅ FIX: Chỉ xóa những gì cần thiết
        boolean rememberMe = sharedPreferences.getBoolean(KEY_REMEMBER_ME, false);
        if (!rememberMe) {
            editor.remove(KEY_USERNAME);
        }
        // KHÔNG xóa KEY_USER_ID - có thể cần cho cleanup
        // editor.remove(KEY_USER_ID); // ❌ KHÔNG làm thế này

        editor.apply();

        Log.d("Dashboard", "✅ Logout completed");

        // Navigate to login
        navigateToLogin();
        showToast("Đã đăng xuất thành công");
    }

    // Utility methods
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private String formatCurrency(double amount) {
        return currencyFormat.format(amount);
    }

    private String formatDate(long timestamp) {
        return dateFormat.format(timestamp);
    }

    // Database utility methods
    private void executeQuery(String query, String[] params) {
        try {
            database.execSQL(query, params);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Cursor getReadOnlyQuery(String query, String[] params) {
        try {
            return database.rawQuery(query, params);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Financial calculation utilities
    private double calculatePercentageChange(double current, double previous) {
        if (previous == 0) return 0;
        return ((current - previous) / previous) * 100;
    }

    private String getChangeIcon(double change) {
        if (change > 0) return "📈";
        else if (change < 0) return "📉";
        else return "➡️";
    }

    private String getChangeText(double changePercent, boolean isPositive) {
        String arrow = isPositive ? "↗" : "↘";
        return String.format("%s %+.1f%%", arrow, changePercent);
    }

    // UI Animation utilities
    private void animateValue(TextView textView, String newValue) {
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(textView, "alpha", 1f, 0f);
        fadeOut.setDuration(150);
        fadeOut.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                textView.setText(newValue);
                ObjectAnimator fadeIn = ObjectAnimator.ofFloat(textView, "alpha", 0f, 1f);
                fadeIn.setDuration(150);
                fadeIn.start();
            }
        });
        fadeOut.start();
    }

    private void animateProgressBar(ProgressBar progressBar, int newProgress) {
        ObjectAnimator animator = ObjectAnimator.ofInt(progressBar, "progress", progressBar.getProgress(), newProgress);
        animator.setDuration(1000);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.start();
    }

    // Color utilities for dynamic theming
    private int getColorForAmount(double amount) {
        if (amount > 0) {
            return getResources().getColor(R.color.income_green, getTheme());
        } else if (amount < 0) {
            return getResources().getColor(R.color.expense_red, getTheme());
        } else {
            return getResources().getColor(R.color.text_secondary, getTheme());
        }
    }

    // Data validation utilities
    private boolean isValidAmount(double amount) {
        return amount >= 0 && !Double.isNaN(amount) && !Double.isInfinite(amount);
    }

    private boolean isValidDate(long timestamp) {
        return timestamp > 0 && timestamp <= System.currentTimeMillis();
    }

    // Network and sync utilities (for future implementation)
    private void syncDataWithServer() {
        // TODO: Implement server synchronization
        // This would handle uploading local changes to server
        // and downloading remote changes
    }

    private void scheduleBackgroundSync() {
        // TODO: Schedule background sync using WorkManager
        // This would ensure data stays synchronized even when app is closed
    }

    // Notification utilities
    private void scheduleFinancialReminders() {
        // TODO: Schedule daily/weekly financial reminders
        // Such as budget warnings, savings goals progress, etc.
    }

    private void showFinancialAlert(String title, String message, int iconResource) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setIcon(iconResource)
                .setPositiveButton("Đã hiểu", null)
                .show();
    }

    // Data export utilities
    private void exportDataToCSV() {
        // TODO: Implement CSV export functionality
        // This would allow users to export their financial data
    }

    private void shareFinancialSummary() {
        // TODO: Create shareable summary of financial status
        // This could generate a nice image or PDF to share
    }

    // Security utilities
    private void enableBiometricLock() {
        // TODO: Implement biometric authentication
        // This would add extra security layer for sensitive financial data
    }

    private void backupDataSecurely() {
        // TODO: Implement secure data backup
        // This would encrypt and backup user data to cloud storage
    }

    // Accessibility utilities
    private void setupAccessibility() {
        // TODO: Add proper content descriptions for all UI elements
        // This would make the app more accessible to users with disabilities
    }

    // Performance optimization
    private void optimizeMemoryUsage() {
        // Clear unused resources
        System.gc();
    }

    private void preloadCriticalData() {
        // TODO: Preload frequently accessed data in background
        // This would improve app responsiveness
    }
}