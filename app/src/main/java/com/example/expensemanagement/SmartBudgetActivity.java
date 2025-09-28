/// 💰 Enhanced SmartBudgetActivity.java - Complete Budget Management
package com.example.expensemanagement;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.expensemanagement.R;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

import database.DatabaseHelper;
import models.BudgetPlan;
import models.SmartInsight;
import adapters.BudgetCategoryAdapter;
import adapters.SmartInsightAdapter;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SmartBudgetActivity extends AppCompatActivity {

    private static final String TAG = "SmartBudgetActivity";
    private static final String PREFS_NAME = "ExpenseManagementPrefs";
    private static final String KEY_USER_ID = "user_id";

    // Request codes for activities
    private static final int REQUEST_CREATE_BUDGET = 1001;
    private static final int REQUEST_ADJUST_BUDGET = 1002;
    private static final int REQUEST_ADD_EXPENSE = 1003;
    private static final int REQUEST_SETTINGS = 1004;

    // Core Data
    private DatabaseHelper dbHelper;
    private String currentUserId;
    private BudgetPlan currentBudget;
    private List<BudgetPlan.CategoryBudget> budgetCategories;
    private List<SmartInsight> smartInsights;

    // UI Components - Header
    private TextView tvUserGreeting;
    private Spinner spinnerBudgetPeriod;
    private TextView tvDateRange;
    private ImageView ivNotifications, ivSettings;

    // UI Components - Overview
    private LinearLayout layoutBudgetOverview;
    private CardView cardBudgetHealth;
    private TextView tvTotalAllocated, tvTotalSpent, tvRemainingAmount, tvUsagePercentage;
    private TextView tvHealthScore, tvHealthStatus, tvHealthDescription;
    private TextView tvCategoriesOnTrack, tvOverspendWarning;
    private ProgressBar pbOverallProgress;
    private TextView tvDaysRemaining;

    // UI Components - Content
    private CardView cardBudgetCategories, cardSmartInsights, cardAnalytics;
    private RecyclerView rvBudgetCategories, rvSmartInsights;
    private PieChart pieChart;
    private SwipeRefreshLayout swipeRefreshLayout;

    // UI Components - Empty State & Actions
    private LinearLayout layoutNoData, layoutActionButtons;
    private Button btnStartBudgeting, btnCreateBudget, btnQuickAddExpense;
    private Button btnAdjustBudget, btnBudgetAnalysis, btnBudgetExport;

    // Utils
    private NumberFormat currencyFormatter;
    private ExecutorService executor;
    private Handler mainHandler;
    private BudgetCategoryAdapter categoryAdapter;
    private SmartInsightAdapter insightAdapter;
    // Add these fields to store budget-specific data
    private String selectedBudgetName;
    private String selectedBudgetId;
    private String budgetStartDate;
    private String budgetEndDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_smart_budget);

        Log.d(TAG, "🚀 SmartBudgetActivity onCreate started");

        // Get budget information from intent
        getBudgetInfoFromIntent();
        // Debug intent extras
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            Log.d(TAG, "🔍 DEBUG: Intent extras:");
            for (String key : extras.keySet()) {
                Log.d(TAG, "  " + key + " = " + extras.get(key));
            }
        } else {
            Log.d(TAG, "🔍 DEBUG: No intent extras");
        }

        ImageView backButton = findViewById(R.id.iv_back_button);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        initializeComponents();
        setupUI();
        loadBudgetData();
    }

    // NEW METHOD: Get budget info from BudgetListActivity
    private void getBudgetInfoFromIntent() {
        Intent intent = getIntent();
        selectedBudgetId = intent.getStringExtra("BUDGET_ID");
        selectedBudgetName = intent.getStringExtra("BUDGET_NAME");
        budgetStartDate = intent.getStringExtra("start_date");
        budgetEndDate = intent.getStringExtra("end_date");

        // Also get user_id from intent or SharedPreferences
        String userIdFromIntent = intent.getStringExtra("user_id");
        if (userIdFromIntent != null) {
            currentUserId = userIdFromIntent;
        }

        Log.d(TAG, "🔍 Budget Info from Intent:");
        Log.d(TAG, "  Budget ID: " + selectedBudgetId);
        Log.d(TAG, "  Budget Name: " + selectedBudgetName);
        Log.d(TAG, "  Start Date: " + budgetStartDate);
        Log.d(TAG, "  End Date: " + budgetEndDate);
        Log.d(TAG, "  User ID: " + currentUserId);
    }

    // ===== INITIALIZATION =====

    private void initializeComponents() {
        Log.d(TAG, "🚀 Initializing enhanced components...");

        // Core setup
        dbHelper = new DatabaseHelper(this);
        executor = Executors.newFixedThreadPool(3);
        mainHandler = new Handler(Looper.getMainLooper());
        currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

        // Get user ID with validation
        currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            redirectToLogin();
            return;
        }

        // Initialize data collections
        budgetCategories = new ArrayList<>();
        smartInsights = new ArrayList<>();

        Log.d(TAG, "✅ Enhanced components initialized for user: " + currentUserId);
    }

    private void setupUI() {
        Log.d(TAG, "🎨 Setting up enhanced UI...");

        findViews();
        setupActionBar();
        setupSwipeRefresh();
        setupSpinner();
        setupRecyclerViews();
        setupChart();
        setupEventListeners();
        updateUserGreeting();

        Log.d(TAG, "✅ Enhanced UI setup complete");
    }

    private void findViews() {
        // Swipe refresh
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);

        // Header
        tvUserGreeting = findViewById(R.id.tv_user_greeting);
        spinnerBudgetPeriod = findViewById(R.id.spinner_budget_period);
        tvDateRange = findViewById(R.id.tv_date_range);
        ivNotifications = findViewById(R.id.iv_notifications);
        ivSettings = findViewById(R.id.iv_settings);

        // Overview cards
        layoutBudgetOverview = findViewById(R.id.layout_budget_overview);
        cardBudgetHealth = findViewById(R.id.card_budget_health);
        tvTotalAllocated = findViewById(R.id.tv_total_allocated);
        tvTotalSpent = findViewById(R.id.tv_total_spent);
        tvRemainingAmount = findViewById(R.id.tv_remaining_amount);
        tvUsagePercentage = findViewById(R.id.tv_usage_percentage);
        tvHealthScore = findViewById(R.id.tv_health_score);
        tvHealthStatus = findViewById(R.id.tv_health_status);
        tvHealthDescription = findViewById(R.id.tv_health_description);
        tvCategoriesOnTrack = findViewById(R.id.tv_categories_on_track);
        tvOverspendWarning = findViewById(R.id.tv_overspend_warning);
        pbOverallProgress = findViewById(R.id.pb_overall_progress);
        tvDaysRemaining = findViewById(R.id.tv_days_remaining);

        // Content
        cardBudgetCategories = findViewById(R.id.card_budget_categories);
        cardSmartInsights = findViewById(R.id.card_smart_insights);
        cardAnalytics = findViewById(R.id.card_analytics);
        rvBudgetCategories = findViewById(R.id.rv_budget_categories);
        rvSmartInsights = findViewById(R.id.rv_smart_insights);
        pieChart = findViewById(R.id.pie_chart_budget_allocation);

        // Empty state & actions
        layoutNoData = findViewById(R.id.layout_no_data);
        layoutActionButtons = findViewById(R.id.layout_action_buttons);
        btnStartBudgeting = findViewById(R.id.btn_start_budgeting);
        btnCreateBudget = findViewById(R.id.btn_create_budget);
        btnQuickAddExpense = findViewById(R.id.btn_quick_add_expense);
        btnAdjustBudget = findViewById(R.id.btn_adjust_budget);
        btnBudgetAnalysis = findViewById(R.id.btn_budget_analysis);
        btnBudgetExport = findViewById(R.id.btn_budget_export);
    }

    private void setupSwipeRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setColorSchemeResources(
                    android.R.color.holo_blue_bright,
                    android.R.color.holo_green_light,
                    android.R.color.holo_orange_light,
                    android.R.color.holo_red_light
            );
            swipeRefreshLayout.setOnRefreshListener(this:: refreshData);
        }
    }

    private void setupSpinner() {
        String[] periods = {"Hàng tuần", "Hàng tháng", "Hàng quý", "Hàng năm"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, periods);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBudgetPeriod.setAdapter(adapter);
        spinnerBudgetPeriod.setSelection(0); // Default to weekly

        spinnerBudgetPeriod.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "🔄 Period changed to: " + periods[position]);
                loadBudgetData();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupRecyclerViews() {
        // Budget Categories with enhanced click handling
        categoryAdapter = new BudgetCategoryAdapter(budgetCategories, this);
        categoryAdapter.setOnBudgetCategoryClickListener(new BudgetCategoryAdapter.OnBudgetCategoryClickListener() {
            @Override
            public void onCategoryClick(BudgetPlan.CategoryBudget category, int position) {
                openCategoryDetails(category);
            }

            @Override
            public void onAdjustBudgetClick(BudgetPlan.CategoryBudget category, int position) {
                adjustSingleCategory(category, position);
            }

            @Override
            public void onViewDetailsClick(BudgetPlan.CategoryBudget category, int position) {
                viewCategoryTransactions(category);
            }

            @Override
            public void onQuickExpenseClick(BudgetPlan.CategoryBudget category, int position) {
                // xử lý khi bấm Quick Expense
            }

            @Override
            public void onDeleteCategoryClick(BudgetPlan.CategoryBudget category, int position) {
                // xử lý khi xóa Category
            }
        });
        rvBudgetCategories.setLayoutManager(new LinearLayoutManager(this));
        rvBudgetCategories.setAdapter(categoryAdapter);

        // Smart Insights with enhanced actions
        insightAdapter = new SmartInsightAdapter(smartInsights);
        insightAdapter.setOnInsightClickListener(new SmartInsightAdapter.OnInsightClickListener() {
            @Override
            public void onInsightClick(SmartInsight insight, int position) {
                showInsightDetails(insight);
            }

            @Override
            public void onActionClick(SmartInsight insight, int position) {
                executeInsightAction(insight);
            }
        });
        rvSmartInsights.setLayoutManager(new LinearLayoutManager(this));
        rvSmartInsights.setAdapter(insightAdapter);
    }

    private void setupChart() {
        pieChart.setUsePercentValues(true);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleRadius(40f);
        pieChart.setTransparentCircleRadius(45f);
        pieChart.setDescription(null);
        pieChart.getLegend().setEnabled(true);
        pieChart.getLegend().setWordWrapEnabled(true);
    }

    private void setupEventListeners() {
        // Header actions
        ivNotifications.setOnClickListener(v -> openNotifications());
        ivSettings.setOnClickListener(v -> openBudgetSettings());

        // Primary actions
        btnStartBudgeting.setOnClickListener(v -> startCreateBudgetActivity());
        btnCreateBudget.setOnClickListener(v -> startCreateBudgetActivity());
        btnQuickAddExpense.setOnClickListener(v -> startQuickExpenseActivity());

        // Secondary actions
        btnAdjustBudget.setOnClickListener(v -> adjustEntireBudget());
        btnBudgetAnalysis.setOnClickListener(v -> openBudgetAnalysis());
        btnBudgetExport.setOnClickListener(v -> exportBudgetData());

        // Sort and filter
        ImageView ivSortCategories = findViewById(R.id.iv_sort_categories);
        ImageView ivFilterCategories = findViewById(R.id.iv_filter_categories);

        if (ivSortCategories != null) {
            ivSortCategories.setOnClickListener(v -> showSortOptions());
        }

        if (ivFilterCategories != null) {
            ivFilterCategories.setOnClickListener(v -> showFilterOptions());
        }

        // Chart tab buttons
        Button btnChartAllocation = findViewById(R.id.btn_chart_allocation);
        Button btnChartTrend = findViewById(R.id.btn_chart_trend);
        Button btnChartComparison = findViewById(R.id.btn_chart_comparison);

        if (btnChartAllocation != null) {
            btnChartAllocation.setOnClickListener(v -> switchChartView("allocation"));
        }
        if (btnChartTrend != null) {
            btnChartTrend.setOnClickListener(v -> switchChartView("trend"));
        }
        if (btnChartComparison != null) {
            btnChartComparison.setOnClickListener(v -> switchChartView("comparison"));
        }
    }

    // ===== MENU =====

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_smart_budget, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.action_refresh) {
            refreshData();
            return true;
        } else if (id == R.id.action_settings) {
            openBudgetSettings();
            return true;
        } else if (id == R.id.action_export) {
            exportBudgetData();
            return true;
        } else if (id == R.id.action_create_new) {
            confirmCreateNewBudget();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // ===== ENHANCED DATA LOADING =====

    private void loadBudgetData() {
        Log.d(TAG, "📊 Loading enhanced budget data...");

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(true);
        }

        executor.execute(() -> {
            try {
                String selectedPeriod = getCurrentPeriod();
                currentBudget = loadBudgetFromDatabase(selectedPeriod);

                if (currentBudget != null) {
                    Log.d(TAG, "✅ Found budget: " + currentBudget.getName());
                    loadCategoriesData();
                    generateSmartInsights();
                    calculateTimeRemaining();
                    mainHandler.post(this::displayBudgetData);
                } else {
                    Log.d(TAG, "❌ No budget found for period: " + selectedPeriod);
                    mainHandler.post(this::showEmptyState);
                }

            } catch (Exception e) {
                Log.e(TAG, "❌ Error loading budget data: " + e.getMessage(), e);
                mainHandler.post(() -> showError("Không thể tải dữ liệu ngân sách"));
            } finally {
                mainHandler.post(() -> {
                    if (swipeRefreshLayout != null) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
            }
        });
    }

    private void refreshData() {
        Log.d(TAG, "🔄 Manual refresh triggered");
        loadBudgetData();
        Toast.makeText(this, "Đang cập nhật dữ liệu...", Toast.LENGTH_SHORT).show();
    }

    // ===== ENHANCED ACTIONS =====

    private void adjustSingleCategory(BudgetPlan.CategoryBudget category, int position) {
        // Create custom dialog for single category adjustment
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_adjust_single_category, null);

        EditText etNewAmount = dialogView.findViewById(R.id.et_new_amount);
        TextView tvCurrentAmount = dialogView.findViewById(R.id.tv_current_amount);
        TextView tvCategoryName = dialogView.findViewById(R.id.tv_category_name);

        tvCategoryName.setText(category.getName());
        tvCurrentAmount.setText("Hiện tại: " + currencyFormatter.format(category.getAllocatedAmount()));
        etNewAmount.setText(String.valueOf((int) category.getAllocatedAmount()));

        builder.setView(dialogView)
                .setTitle("Điều chỉnh ngân sách: " + category.getName())
                .setPositiveButton("Cập nhật", (dialog, which) -> {
                    try {
                        double newAmount = Double.parseDouble(etNewAmount.getText().toString());
                        updateCategoryBudget(category, newAmount, position);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Vui lòng nhập số tiền hợp lệ", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void adjustEntireBudget() {
        if (currentBudget == null) {
            Toast.makeText(this, "Không có ngân sách để điều chỉnh", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, AdjustBudgetActivity.class);
        intent.putExtra("budget_data", currentBudget);
        intent.putExtra("user_id", currentUserId);
        startActivityForResult(intent, REQUEST_ADJUST_BUDGET);
    }

    private void updateCategoryBudget(BudgetPlan.CategoryBudget category, double newAmount, int position) {
        executor.execute(() -> {
            try {
                SQLiteDatabase db = dbHelper.getWritableDatabase();

                android.content.ContentValues values = new android.content.ContentValues();
                values.put(DatabaseHelper.COLUMN_BUDGET_AMOUNT, newAmount);

                String whereClause = DatabaseHelper.COLUMN_CATEGORY_ID + " = ? AND " +
                        DatabaseHelper.COLUMN_USER_ID + " = ? AND " +
                        DatabaseHelper.COLUMN_BUDGET_PERIOD + " = ?";
                String[] whereArgs = {
                        String.valueOf(category.getCategoryId()),
                        currentUserId,
                        mapDisplayToPeriod(getCurrentPeriod())
                };

                int updated = db.update(DatabaseHelper.TABLE_BUDGETS, values, whereClause, whereArgs);

                if (updated > 0) {
                    category.setAllocatedAmount(newAmount);

                    mainHandler.post(() -> {
                        categoryAdapter.updateCategory(position, category);
                        updateBudgetOverview();
                        Toast.makeText(this, "✅ Đã cập nhật ngân sách cho " + category.getName(),
                                Toast.LENGTH_SHORT).show();
                    });
                } else {
                    mainHandler.post(() -> Toast.makeText(this, "❌ Không thể cập nhật ngân sách",
                            Toast.LENGTH_SHORT).show());
                }

            } catch (Exception e) {
                Log.e(TAG, "Error updating category budget: " + e.getMessage());
                mainHandler.post(() -> Toast.makeText(this, "Lỗi: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show());
            }
        });
    }

//    private void openCategoryDetails(BudgetPlan.CategoryBudget category) {
//        Intent intent = new Intent(this, CategoryDetailActivity.class);
//        intent.putExtra("category_id", category.getCategoryId());
//        intent.putExtra("category_name", category.getName());
//        intent.putExtra("user_id", currentUserId);
//        intent.putExtra("budget_amount", category.getAllocatedAmount());
//        intent.putExtra("spent_amount", category.getSpentAmount());
//        startActivity(intent);
//    }

    private void openCategoryDetails(BudgetPlan.CategoryBudget category) {
        // THAY ĐỔI: Chuyển hướng đến CategoryTransactionActivity thay vì CategoryDetailActivity
        Intent intent = new Intent(this, CategoryTransactionActivity.class);

        // Truyền thông tin category
        intent.putExtra("category_id", category.getCategoryId());
        intent.putExtra("category_name", category.getName());
        intent.putExtra("user_id", currentUserId);

        // Truyền thông tin budget để filter theo đúng thời gian
        intent.putExtra("budget_name", selectedBudgetName);
        intent.putExtra("start_date", currentBudget.getStartDate());
        intent.putExtra("end_date", currentBudget.getEndDate());

        // Truyền thông tin ngân sách của category này
        intent.putExtra("budget_amount", category.getAllocatedAmount());
        intent.putExtra("spent_amount", category.getSpentAmount());

        Log.d(TAG, "Opening CategoryTransactionActivity with:");
        Log.d(TAG, "  Category: " + category.getName() + " (ID: " + category.getCategoryId() + ")");
        Log.d(TAG, "  Budget: " + selectedBudgetName);
        Log.d(TAG, "  Date range: " + currentBudget.getStartDate() + " to " + currentBudget.getEndDate());
        Log.d(TAG, "  Budget amount: " + category.getAllocatedAmount());
        Log.d(TAG, "  Spent amount: " + category.getSpentAmount());

        startActivity(intent);
    }



    // MODIFIED METHOD: View transactions with proper date range
    private void viewCategoryTransactions(BudgetPlan.CategoryBudget category) {
        Intent intent = new Intent(this, TransactionDetailsActivity.class);
        intent.putExtra("category_id", category.getCategoryId());
        intent.putExtra("category_name", category.getName());
        intent.putExtra("user_id", currentUserId);
        intent.putExtra("filter_type", "category");
        // Add date range for filtering
        intent.putExtra("start_date", currentBudget.getStartDate());
        intent.putExtra("end_date", currentBudget.getEndDate());
        intent.putExtra("budget_name", selectedBudgetName);
        startActivity(intent);
    }

    // NEW METHOD: Convert date format for database queries
    private String convertToDbDate(String displayDate) {
        try {
            // If already in YYYY-MM-DD format, return as is
            if (displayDate.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return displayDate;
            }

            // Convert from DD/MM/YYYY to YYYY-MM-DD
            SimpleDateFormat displayFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = displayFormat.parse(displayDate);
            return dbFormat.format(date);
        } catch (Exception e) {
            Log.e(TAG, "Error converting date: " + displayDate, e);
            return displayDate; // Return original if conversion fails
        }
    }


    private void openNotifications() {
        Intent intent = new Intent(this, NotificationsActivity.class);
        intent.putExtra("user_id", currentUserId);
        startActivity(intent);
    }

    private void openBudgetSettings() {
        Intent intent = new Intent(this, BudgetSettingsActivity.class);
        intent.putExtra("user_id", currentUserId);
        startActivityForResult(intent, REQUEST_SETTINGS);
    }



    private void openBudgetAnalysis() {
        Intent intent = new Intent(this, BudgetAnalytics.class);
        intent.putExtra("user_id", currentUserId);
        intent.putExtra("budget_data", currentBudget);
        startActivity(intent);
    }

    private void exportBudgetData() {
        if (currentBudget == null) {
            Toast.makeText(this, "Không có dữ liệu để xuất", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show export options dialog
        String[] options = {"Xuất PDF", "Xuất Excel", "Chia sẻ văn bản"};

        new AlertDialog.Builder(this)
                .setTitle("Chọn định dạng xuất")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: exportToPDF(); break;
                        case 1: exportToExcel(); break;
                        case 2: shareAsText(); break;
                    }
                })
                .show();
    }

    private void confirmCreateNewBudget() {
        new AlertDialog.Builder(this)
                .setTitle("Tạo ngân sách mới?")
                .setMessage("Bạn có muốn tạo ngân sách mới không? Ngân sách hiện tại sẽ được giữ nguyên.")
                .setPositiveButton("Tạo mới", (dialog, which) -> startCreateBudgetActivity())
                .setNegativeButton("Hủy", null)
                .show();
    }

    // ===== CHART MANAGEMENT =====

    private void switchChartView(String chartType) {
        // Update chart visibility and data based on type
        switch (chartType) {
            case "allocation":
                updateAllocationChart();
                break;
            case "trend":
                updateTrendChart();
                break;
            case "comparison":
                updateComparisonChart();
                break;
        }

        // Update tab button states
        updateChartTabButtons(chartType);
    }

    private void updateChartTabButtons(String activeChart) {
        Button btnAllocation = findViewById(R.id.btn_chart_allocation);
        Button btnTrend = findViewById(R.id.btn_chart_trend);
        Button btnComparison = findViewById(R.id.btn_chart_comparison);

        // Reset all buttons to normal state
        if (btnAllocation != null) btnAllocation.setBackgroundResource(R.drawable.tab_button_normal);
        if (btnTrend != null) btnTrend.setBackgroundResource(R.drawable.tab_button_normal);
        if (btnComparison != null) btnComparison.setBackgroundResource(R.drawable.tab_button_normal);

        // Set active button
        switch (activeChart) {
            case "allocation":
                if (btnAllocation != null) btnAllocation.setBackgroundResource(R.drawable.tab_button_selected);
                break;
            case "trend":
                if (btnTrend != null) btnTrend.setBackgroundResource(R.drawable.tab_button_selected);
                break;
            case "comparison":
                if (btnComparison != null) btnComparison.setBackgroundResource(R.drawable.tab_button_selected);
                break;
        }
    }

    private void updateAllocationChart() {
        if (budgetCategories.isEmpty()) return;

        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        for (BudgetPlan.CategoryBudget category : budgetCategories) {
            if (category.getAllocatedAmount() > 0) {
                // Tạo PieEntry với tên danh mục để legend hiển thị đúng
                entries.add(new PieEntry((float) category.getAllocatedAmount(), category.getName()));

                try {
                    colors.add(Color.parseColor(category.getColor()));
                } catch (Exception e) {
                    colors.add(generateCategoryColor(category.getCategoryId()));
                }
            }
        }

        if (!entries.isEmpty()) {
            PieDataSet dataSet = new PieDataSet(entries, "");
            dataSet.setColors(colors);
            dataSet.setValueTextSize(12f);
            dataSet.setValueTextColor(Color.WHITE);
            dataSet.setSliceSpace(3f);
            dataSet.setSelectionShift(5f);

            // Hiển thị phần trăm trên biểu đồ
            dataSet.setDrawValues(true);

            PieData data = new PieData(dataSet);
            data.setValueFormatter(new com.github.mikephil.charting.formatter.PercentFormatter(pieChart));
            data.setValueTextSize(12f);
            data.setValueTextColor(Color.WHITE);

            // Cài đặt biểu đồ chính
            pieChart.setUsePercentValues(true);
            pieChart.setDrawEntryLabels(false); // Tắt label trên slice
            pieChart.setDrawHoleEnabled(true);
            pieChart.setHoleRadius(40f);
            pieChart.setTransparentCircleRadius(45f);
            pieChart.setCenterTextSize(16f);
            pieChart.setDescription(null);

            // Cài đặt legend (chú thích dưới biểu đồ)
            pieChart.getLegend().setEnabled(true);
            pieChart.getLegend().setVerticalAlignment(com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM);
            pieChart.getLegend().setHorizontalAlignment(com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER);
            pieChart.getLegend().setOrientation(com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL);
            pieChart.getLegend().setDrawInside(false);
            pieChart.getLegend().setWordWrapEnabled(true);
            pieChart.getLegend().setTextSize(10f);
            pieChart.getLegend().setFormSize(8f);
            pieChart.getLegend().setFormToTextSpace(4f);
            pieChart.getLegend().setXEntrySpace(8f);
            pieChart.getLegend().setYEntrySpace(2f);

            // Thiết lập rotation và touch
            pieChart.setRotationAngle(0);
            pieChart.setRotationEnabled(true);
            pieChart.setTouchEnabled(true);

            pieChart.setData(data);
            pieChart.animateY(1000);
            pieChart.invalidate();
        }
    }

    private void updateTrendChart() {
        // Implementation for trend chart (line chart showing spending over time)
        Toast.makeText(this, "Đang phát triển tính năng biểu đồ xu hướng", Toast.LENGTH_SHORT).show();
    }

    private void updateComparisonChart() {
        // Implementation for comparison chart (bar chart comparing planned vs actual)
        Toast.makeText(this, "Đang phát triển tính năng biểu đồ so sánh", Toast.LENGTH_SHORT).show();
    }

    // ===== SORTING & FILTERING =====

    private void showSortOptions() {
        String[] options = {
                "Tên A-Z", "Tên Z-A",
                "Ngân sách cao → thấp", "Ngân sách thấp → cao",
                "Chi tiêu cao → thấp", "Chi tiêu thấp → cao",
                "Phần trăm sử dụng cao → thấp", "Phần trăm sử dụng thấp → cao"
        };

        new AlertDialog.Builder(this)
                .setTitle("Sắp xếp danh mục")
                .setItems(options, (dialog, which) -> sortCategories(which))
                .show();
    }

    private void showFilterOptions() {
        String[] options = {
                "Tất cả danh mục",
                "Đang trong ngân sách (≤ 80%)",
                "Cần chú ý (80-95%)",
                "Vượt ngân sách (> 100%)",
                "Chưa sử dụng (0%)"
        };

        new AlertDialog.Builder(this)
                .setTitle("Lọc danh mục")
                .setItems(options, (dialog, which) -> filterCategories(which))
                .show();
    }

    private void sortCategories(int sortType) {
        List<BudgetPlan.CategoryBudget> sortedCategories = new ArrayList<>(budgetCategories);

        switch (sortType) {
            case 0: // Name A-Z
                sortedCategories.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
                break;
            case 1: // Name Z-A
                sortedCategories.sort((a, b) -> b.getName().compareToIgnoreCase(a.getName()));
                break;
            case 2: // Budget high to low
                sortedCategories.sort((a, b) -> Double.compare(b.getAllocatedAmount(), a.getAllocatedAmount()));
                break;
            case 3: // Budget low to high
                sortedCategories.sort((a, b) -> Double.compare(a.getAllocatedAmount(), b.getAllocatedAmount()));
                break;
            case 4: // Spent high to low
                sortedCategories.sort((a, b) -> Double.compare(b.getSpentAmount(), a.getSpentAmount()));
                break;
            case 5: // Spent low to high
                sortedCategories.sort((a, b) -> Double.compare(a.getSpentAmount(), b.getSpentAmount()));
                break;
            case 6: // Usage % high to low
                sortedCategories.sort((a, b) -> Double.compare(b.getPercentageUsed(), a.getPercentageUsed()));
                break;
            case 7: // Usage % low to high
                sortedCategories.sort((a, b) -> Double.compare(a.getPercentageUsed(), b.getPercentageUsed()));
                break;
        }

        categoryAdapter.updateData(sortedCategories);
        Toast.makeText(this, "Đã sắp xếp danh mục", Toast.LENGTH_SHORT).show();
    }

    private void filterCategories(int filterType) {
        List<BudgetPlan.CategoryBudget> filteredCategories = new ArrayList<>();

        for (BudgetPlan.CategoryBudget category : budgetCategories) {
            double percentage = category.getPercentageUsed();

            switch (filterType) {
                case 0: // All
                    filteredCategories.add(category);
                    break;
                case 1: // Within budget
                    if (percentage <= 80) filteredCategories.add(category);
                    break;
                case 2: // Need attention
                    if (percentage > 80 && percentage <= 95) filteredCategories.add(category);
                    break;
                case 3: // Over budget
                    if (percentage > 100) filteredCategories.add(category);
                    break;
                case 4: // Unused
                    if (percentage == 0) filteredCategories.add(category);
                    break;
            }
        }

        categoryAdapter.updateData(filteredCategories);
        String filterName = getFilterName(filterType);
        Toast.makeText(this, "Lọc: " + filterName + " (" + filteredCategories.size() + " mục)", Toast.LENGTH_SHORT).show();
    }

    private String getFilterName(int filterType) {
        String[] names = {"Tất cả", "Trong ngân sách", "Cần chú ý", "Vượt ngân sách", "Chưa sử dụng"};
        return names[filterType];
    }

    // ===== EXPORT FUNCTIONS =====

    private void exportToPDF() {
        Toast.makeText(this, "Đang phát triển tính năng xuất PDF", Toast.LENGTH_SHORT).show();
        // TODO: Implement PDF export using iText or similar library
    }

    private void exportToExcel() {
        Toast.makeText(this, "Đang phát triển tính năng xuất Excel", Toast.LENGTH_SHORT).show();
        // TODO: Implement Excel export using Apache POI or similar library
    }

    private void shareAsText() {
        if (currentBudget == null) return;

        StringBuilder shareText = new StringBuilder();
        shareText.append("📊 BÁO CÁO NGÂN SÁCH\n");
        shareText.append("============================\n\n");
        shareText.append("📅 Kỳ: ").append(currentBudget.getPeriodDisplayName()).append("\n");
        shareText.append("🏷️ Tên: ").append(currentBudget.getName()).append("\n");
        shareText.append("📆 Thời gian: ").append(currentBudget.getStartDate()).append(" - ").append(currentBudget.getEndDate()).append("\n\n");

        shareText.append("💰 TỔNG QUAN:\n");
        shareText.append("• Tổng phân bổ: ").append(currencyFormatter.format(currentBudget.getTotalAllocated())).append("\n");
        shareText.append("• Đã chi tiêu: ").append(currencyFormatter.format(currentBudget.getTotalSpent())).append("\n");
        shareText.append("• Còn lại: ").append(currencyFormatter.format(currentBudget.getOverallVariance())).append("\n");

        double percentage = currentBudget.getTotalAllocated() > 0 ?
                (currentBudget.getTotalSpent() / currentBudget.getTotalAllocated()) * 100 : 0;
        shareText.append("• Tỷ lệ sử dụng: ").append(String.format("%.1f%%", percentage)).append("\n\n");

        shareText.append("📂 CHI TIẾT DANH MỤC:\n");
        for (BudgetPlan.CategoryBudget category : budgetCategories) {
            shareText.append("• ").append(category.getName()).append("\n");
            shareText.append("  - Phân bổ: ").append(currencyFormatter.format(category.getAllocatedAmount())).append("\n");
            shareText.append("  - Đã chi: ").append(currencyFormatter.format(category.getSpentAmount())).append("\n");
            shareText.append("  - Sử dụng: ").append(String.format("%.1f%%", category.getPercentageUsed())).append("\n\n");
        }

        shareText.append("📱 Được tạo từ ứng dụng Quản Lý Chi Tiêu");

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText.toString());
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Báo cáo ngân sách - " + currentBudget.getName());

        startActivity(Intent.createChooser(shareIntent, "Chia sẻ báo cáo ngân sách"));
    }

    // ===== ENHANCED UI DISPLAY =====

    private void displayBudgetData() {
        Log.d(TAG, "🎨 Displaying enhanced budget data...");

        showDataViews();
        updateBudgetOverview();
        updateHealthDashboard();
        updateAllocationChart(); // Default to allocation chart
        updateDateRange();
        updateTimeProgress();

        categoryAdapter.notifyDataSetChanged();
        insightAdapter.notifyDataSetChanged();

        Log.d(TAG, "✅ Enhanced budget data displayed");
    }

    private void updateDateRange() {
        if (currentBudget != null && tvDateRange != null) {
            tvDateRange.setText(currentBudget.getStartDate() + " - " + currentBudget.getEndDate());
        }
    }

    private void updateTimeProgress() {
        if (currentBudget != null && tvDaysRemaining != null) {
            int daysRemaining = calculateDaysRemaining();
            if (daysRemaining > 0) {
                tvDaysRemaining.setText(daysRemaining + " ngày còn lại");
            } else if (daysRemaining == 0) {
                tvDaysRemaining.setText("Hôm nay là ngày cuối");
                tvDaysRemaining.setTextColor(Color.parseColor("#FF9800"));
            } else {
                tvDaysRemaining.setText("Đã hết hạn " + Math.abs(daysRemaining) + " ngày");
                tvDaysRemaining.setTextColor(Color.RED);
            }
        }
    }

    private void updateBudgetOverview() {
        if (currentBudget == null) return;

        tvTotalAllocated.setText(currencyFormatter.format(currentBudget.getTotalAllocated()));
        tvTotalSpent.setText(currencyFormatter.format(currentBudget.getTotalSpent()));
        tvRemainingAmount.setText(currencyFormatter.format(currentBudget.getOverallVariance()));

        double percentage = currentBudget.getTotalAllocated() > 0 ?
                (currentBudget.getTotalSpent() / currentBudget.getTotalAllocated()) * 100 : 0;
        tvUsagePercentage.setText(String.format("%.1f%%", percentage));

        // Update progress bar with color coding
        int progress = (int) Math.min(100, percentage);
        pbOverallProgress.setProgress(progress);

        // Color code progress bar
        if (percentage <= 70) {
            pbOverallProgress.getProgressDrawable().setColorFilter(Color.GREEN, android.graphics.PorterDuff.Mode.SRC_IN);
        } else if (percentage <= 85) {
            pbOverallProgress.getProgressDrawable().setColorFilter(Color.parseColor("#8BC34A"), android.graphics.PorterDuff.Mode.SRC_IN);
        } else if (percentage <= 95) {
            pbOverallProgress.getProgressDrawable().setColorFilter(Color.parseColor("#FF9800"), android.graphics.PorterDuff.Mode.SRC_IN);
        } else {
            pbOverallProgress.getProgressDrawable()
                    .setColorFilter(Color.RED, android.graphics.PorterDuff.Mode.SRC_IN);
        }
    }

    private void updateHealthDashboard() {
        if (currentBudget == null) return;

        int healthScore = calculateOverallHealthScore();
        tvHealthScore.setText(String.valueOf(healthScore));

        String healthStatus = getHealthStatusText(healthScore);
        tvHealthStatus.setText(healthStatus);

        // Update health description
        String description = getHealthDescription(healthScore);
        if (tvHealthDescription != null) {
            tvHealthDescription.setText(description);
        }

        // Update category metrics
        int totalCategories = budgetCategories.size();
        int onTrackCategories = 0;
        int overspendCategories = 0;

        for (BudgetPlan.CategoryBudget category : budgetCategories) {
            if (category.getPercentageUsed() <= 85) {
                onTrackCategories++;
            }
            if (category.getPercentageUsed() > 100) {
                overspendCategories++;
            }
        }

        if (tvCategoriesOnTrack != null) {
            tvCategoriesOnTrack.setText("📊 " + onTrackCategories + "/" + totalCategories + " đúng hạn");
        }

        if (tvOverspendWarning != null) {
            if (overspendCategories > 0) {
                tvOverspendWarning.setText("⚠️ " + overspendCategories + " danh mục vượt");
                tvOverspendWarning.setTextColor(Color.parseColor("#FF9800"));
            } else {
                tvOverspendWarning.setText("✅ Không có danh mục vượt");
                tvOverspendWarning.setTextColor(Color.parseColor("#4CAF50"));
            }
        }

        // Set colors based on health
        int color = getHealthColor(healthScore);
        tvHealthScore.setTextColor(color);
        tvHealthStatus.setTextColor(color);
    }

    private void showDataViews() {
        layoutNoData.setVisibility(View.GONE);
        layoutBudgetOverview.setVisibility(View.VISIBLE);
        cardBudgetHealth.setVisibility(View.VISIBLE);
        cardBudgetCategories.setVisibility(View.VISIBLE);
        cardSmartInsights.setVisibility(View.VISIBLE);
        cardAnalytics.setVisibility(View.VISIBLE);
        layoutActionButtons.setVisibility(View.VISIBLE);
    }

    private void showEmptyState() {
        Log.d(TAG, "📭 Showing empty state");
        Log.d(TAG, "🔍 DEBUG: Current user ID: " + currentUserId);
        Log.d(TAG, "🔍 DEBUG: Current budget: " + (currentBudget != null ? currentBudget.getName() : "null"));
        Log.d(TAG, "🔍 DEBUG: Categories count: " + budgetCategories.size());

        layoutNoData.setVisibility(View.VISIBLE);
        layoutBudgetOverview.setVisibility(View.GONE);
        cardBudgetHealth.setVisibility(View.GONE);
        cardBudgetCategories.setVisibility(View.GONE);
        cardSmartInsights.setVisibility(View.GONE);
        cardAnalytics.setVisibility(View.GONE);
        layoutActionButtons.setVisibility(View.GONE);

        // Show helpful message in empty state
        TextView emptyMessage = findViewById(R.id.tv_empty_message);
        if (emptyMessage != null) {
            if (currentUserId == null) {
                emptyMessage.setText("Lỗi: Không tìm thấy thông tin người dùng. Vui lòng đăng nhập lại.");
            } else {
                emptyMessage.setText("Chưa có ngân sách nào. Hãy tạo ngân sách đầu tiên!");
            }
        }
    }

    public void forceRefreshData() {
        Log.d(TAG, "🔄 Force refresh triggered by user");

        // Clear current data
        currentBudget = null;
        budgetCategories.clear();
        smartInsights.clear();

        // Force reload user ID
        currentUserId = getCurrentUserId();

        if (currentUserId != null) {
            loadBudgetData();
        } else {
            Log.e(TAG, "❌ Cannot refresh: No user ID");
            showEmptyState();
        }
    }

    // ===== ENHANCED UTILITY METHODS =====

    private String getHealthDescription(int score) {
        if (score >= 85) return "Bạn đang quản lý ngân sách rất tốt! Tiếp tục duy trì.";
        else if (score >= 70) return "Ngân sách ổn định. Một số danh mục cần chú ý thêm.";
        else if (score >= 50) return "Cần điều chỉnh một số danh mục để cải thiện hiệu quả.";
        else return "Ngân sách cần được xem xét và điều chỉnh nghiêm túc.";
    }

    private int calculateDaysRemaining() {
        if (currentBudget == null) return 0;

        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            java.util.Date endDate = sdf.parse(currentBudget.getEndDate());
            java.util.Date today = new java.util.Date();

            long diffInMillis = endDate.getTime() - today.getTime();
            return (int) (diffInMillis / (1000 * 60 * 60 * 24));
        } catch (Exception e) {
            return 0;
        }
    }

    private void calculateTimeRemaining() {
        // Enhanced time calculations for better insights
        // This method can be extended for more sophisticated time-based analysis
    }

    private int generateCategoryColor(int categoryId) {
        // Generate consistent colors for categories
        int[] colors = {
                Color.parseColor("#2196F3"), Color.parseColor("#4CAF50"), Color.parseColor("#FF9800"),
                Color.parseColor("#9C27B0"), Color.parseColor("#F44336"), Color.parseColor("#00BCD4"),
                Color.parseColor("#8BC34A"), Color.parseColor("#FFC107"), Color.parseColor("#673AB7"),
                Color.parseColor("#E91E63")
        };
        return colors[categoryId % colors.length];
    }

    // ===== EXISTING METHODS (Enhanced versions) =====

    private String getCurrentUserId() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String userIdFromIntent = getIntent().getStringExtra("user_id");

        Log.d(TAG, "🔍 DEBUG: User ID from Intent: " + userIdFromIntent);

        if (userIdFromIntent != null && !userIdFromIntent.trim().isEmpty()) {
            prefs.edit().putString(KEY_USER_ID, userIdFromIntent.trim()).apply();
            Log.d(TAG, "✅ DEBUG: Using Intent user ID: " + userIdFromIntent.trim());
            return userIdFromIntent.trim();
        }

        String storedUserId = prefs.getString(KEY_USER_ID, null);
        Log.d(TAG, "🔍 DEBUG: User ID from SharedPrefs: " + storedUserId);

        if (storedUserId == null || storedUserId.trim().isEmpty()) {
            Log.w(TAG, "❌ DEBUG: No valid user ID found!");

            // DEBUG: Check if we can extract from database
            String fallbackUserId = findAnyUserIdFromDatabase();
            if (fallbackUserId != null) {
                Log.d(TAG, "🔧 DEBUG: Using fallback user ID: " + fallbackUserId);
                prefs.edit().putString(KEY_USER_ID, fallbackUserId).apply();
                return fallbackUserId;
            }

            redirectToLogin();
            return null;
        }

        return storedUserId.trim();
    }

    private String findAnyUserIdFromDatabase() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try {
            String query = "SELECT DISTINCT " + DatabaseHelper.COLUMN_USER_ID + " " +
                    "FROM " + DatabaseHelper.TABLE_BUDGETS + " " +
                    "LIMIT 1";

            Cursor cursor = db.rawQuery(query, null);
            if (cursor.moveToFirst()) {
                String userId = cursor.getString(0);
                cursor.close();
                return userId;
            }
            cursor.close();
        } catch (Exception e) {
            Log.e(TAG, "Error finding fallback user ID: " + e.getMessage());
        }
        return null;
    }

    private String getCurrentPeriod() {
        String[] periods = {"weekly", "monthly", "quarterly", "yearly"};
        int selectedPosition = spinnerBudgetPeriod.getSelectedItemPosition();
        return selectedPosition >= 0 && selectedPosition < periods.length ?
                periods[selectedPosition] : "weekly";
    }

    private String mapDisplayToPeriod(String displayName) {
        String result;
        switch (displayName) {
            case "Hàng tuần": result = "weekly"; break;
            case "Hàng tháng": result = "monthly"; break;
            case "Hàng quý": result = "quarterly"; break;
            case "Hàng năm": result = "yearly"; break;
            default: result = "monthly"; break; // Changed default to monthly since your data shows monthly
        }

        Log.d(TAG, "🔍 DEBUG: Period mapping: '" + displayName + "' -> '" + result + "'");
        return result;
    }

    private BudgetPlan loadBudgetFromDatabase(String period) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Log.d(TAG, "🔍 DEBUG: Loading specific budget: " + selectedBudgetName + " for user: " + currentUserId);

        try {
            // Load specific budget by name instead of period
            String budgetQuery = "SELECT " + DatabaseHelper.COLUMN_BUDGET_NAME + ", " +
                    DatabaseHelper.COLUMN_BUDGET_PERIOD + ", " +
                    "MIN(" + DatabaseHelper.COLUMN_BUDGET_START_DATE + ") as start_date, " +
                    "MAX(" + DatabaseHelper.COLUMN_BUDGET_END_DATE + ") as end_date " +
                    "FROM " + DatabaseHelper.TABLE_BUDGETS + " " +
                    "WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ? " +
                    "AND " + DatabaseHelper.COLUMN_BUDGET_NAME + " = ? " +
                    "GROUP BY " + DatabaseHelper.COLUMN_BUDGET_NAME + " " +
                    "LIMIT 1";

            Cursor budgetCursor = db.rawQuery(budgetQuery, new String[]{currentUserId, selectedBudgetName});
            Log.d(TAG, "🔍 DEBUG: Budget query executed. Results: " + budgetCursor.getCount());

            BudgetPlan budget = null;
            if (budgetCursor.moveToFirst()) {
                budget = createBudgetFromCursor(budgetCursor, currentUserId);
                Log.d(TAG, "✅ DEBUG: Found budget: " + budget.getName());
            }
            budgetCursor.close();

            return budget;

        } catch (Exception e) {
            Log.e(TAG, "❌ ERROR: Exception in loadBudgetFromDatabase: " + e.getMessage(), e);
            return null;
        }
    }

    private void debugAllBudgetsInDatabase(SQLiteDatabase db) {
        try {
            String debugQuery = "SELECT " + DatabaseHelper.COLUMN_USER_ID + ", " +
                    DatabaseHelper.COLUMN_BUDGET_NAME + ", " +
                    DatabaseHelper.COLUMN_BUDGET_PERIOD + ", " +
                    DatabaseHelper.COLUMN_BUDGET_START_DATE + ", " +
                    DatabaseHelper.COLUMN_BUDGET_END_DATE + " " +
                    "FROM " + DatabaseHelper.TABLE_BUDGETS + " " +
                    "ORDER BY " + DatabaseHelper.COLUMN_CREATED_AT + " DESC";

            Cursor debugCursor = db.rawQuery(debugQuery, null);
            Log.d(TAG, "🔍 DEBUG: Total budgets in database: " + debugCursor.getCount());

            int count = 0;
            while (debugCursor.moveToNext() && count < 10) { // Only log first 10
                String userId = debugCursor.getString(0);
                String name = debugCursor.getString(1);
                String period = debugCursor.getString(2);
                String startDate = debugCursor.getString(3);
                String endDate = debugCursor.getString(4);

                Log.d(TAG, String.format("🔍 Budget %d: user=%s, name=%s, period=%s, dates=%s to %s",
                        count + 1, userId, name, period, startDate, endDate));
                count++;
            }
            debugCursor.close();

            // Also check current user ID
            Log.d(TAG, "🔍 DEBUG: Current user ID: '" + currentUserId + "'");
            Log.d(TAG, "🔍 DEBUG: Current user ID length: " + (currentUserId != null ? currentUserId.length() : "null"));

        } catch (Exception e) {
            Log.e(TAG, "Error in debug method: " + e.getMessage());
        }
    }

    private void debugTransactionData(SQLiteDatabase db, int categoryId, String userId) {
        // Query tất cả transactions cho category này và user này
        String debugQuery = "SELECT " + DatabaseHelper.COLUMN_ID + ", " +
                DatabaseHelper.COLUMN_TYPE + ", " +
                DatabaseHelper.COLUMN_AMOUNT + ", " +
                DatabaseHelper.COLUMN_DATE + ", " +
                DatabaseHelper.COLUMN_NOTE + " " +
                "FROM " + DatabaseHelper.TABLE_TRANSACTIONS + " " +
                "WHERE " + DatabaseHelper.COLUMN_CATEGORY_ID + " = ? " +
                "AND " + DatabaseHelper.COLUMN_USER_ID + " = ? " +
                "ORDER BY " + DatabaseHelper.COLUMN_DATE + " DESC";

        Cursor debugCursor = db.rawQuery(debugQuery, new String[]{String.valueOf(categoryId), userId});

        Log.d(TAG, String.format("🔍 DEBUG: All transactions for category %d, user %s:", categoryId, userId));
        Log.d(TAG, "🔍 Total found: " + debugCursor.getCount());

        int count = 0;
        while (debugCursor.moveToNext() && count < 5) { // Chỉ in 5 records đầu
            int id = debugCursor.getInt(0);
            String type = debugCursor.getString(1);
            double amount = debugCursor.getDouble(2);
            String date = debugCursor.getString(3);
            String note = debugCursor.getString(4);

            Log.d(TAG, String.format("  Transaction %d: id=%d, type=%s, amount=%.0f, date=%s, note=%s",
                    count + 1, id, type, amount, date, note));
            count++;
        }
        debugCursor.close();
    }


    private void loadCategoriesData() {
        if (currentBudget == null || selectedBudgetName == null) {
            Log.w(TAG, "❌ Cannot load categories: currentBudget or selectedBudgetName is null");
            return;
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        budgetCategories.clear();

        Log.d(TAG, "🔍 DEBUG: Loading categories for budget: " + selectedBudgetName);

        try {
            // Load ONLY categories that belong to this specific budget
            String budgetQuery = "SELECT b." + DatabaseHelper.COLUMN_CATEGORY_ID + ", " +
                    "b." + DatabaseHelper.COLUMN_BUDGET_AMOUNT + ", " +
                    "b." + DatabaseHelper.COLUMN_BUDGET_START_DATE + ", " +
                    "b." + DatabaseHelper.COLUMN_BUDGET_END_DATE + ", " +
                    "c." + DatabaseHelper.COLUMN_NAME + " as category_name, " +
                    "c." + DatabaseHelper.COLUMN_CATEGORY_ICON + ", " +
                    "c." + DatabaseHelper.COLUMN_CATEGORY_COLOR + " " +
                    "FROM " + DatabaseHelper.TABLE_BUDGETS + " b " +
                    "JOIN " + DatabaseHelper.TABLE_CATEGORIES + " c ON b." + DatabaseHelper.COLUMN_CATEGORY_ID + " = c." + DatabaseHelper.COLUMN_ID + " " +
                    "WHERE b." + DatabaseHelper.COLUMN_USER_ID + " = ? " +
                    "AND b." + DatabaseHelper.COLUMN_BUDGET_NAME + " = ?";

            String[] budgetArgs = {currentUserId, selectedBudgetName};
            Log.d(TAG, "🔍 DEBUG: Category query args: user=" + currentUserId + ", budget=" + selectedBudgetName);

            Cursor budgetCursor = db.rawQuery(budgetQuery, budgetArgs);
            Log.d(TAG, "🔍 DEBUG: Budget categories found: " + budgetCursor.getCount());

            double totalAllocated = 0;
            double totalSpent = 0;

            while (budgetCursor.moveToNext()) {
                BudgetPlan.CategoryBudget category = new BudgetPlan.CategoryBudget();

                // Get basic category info
                int categoryId = getColumnValue(budgetCursor, DatabaseHelper.COLUMN_CATEGORY_ID, 0);
                String name = getColumnValue(budgetCursor, "category_name", "");
                String icon = getColumnValue(budgetCursor, DatabaseHelper.COLUMN_CATEGORY_ICON, "");
                String color = getColumnValue(budgetCursor, DatabaseHelper.COLUMN_CATEGORY_COLOR, "");
                double allocated = getColumnValue(budgetCursor, DatabaseHelper.COLUMN_BUDGET_AMOUNT, 0.0);
                String startDate = getColumnValue(budgetCursor, DatabaseHelper.COLUMN_BUDGET_START_DATE, "");
                String endDate = getColumnValue(budgetCursor, DatabaseHelper.COLUMN_BUDGET_END_DATE, "");

                Log.d(TAG, String.format("🔍 Processing category: %s (id=%d) for period %s to %s",
                        name, categoryId, startDate, endDate));

                // Get spent amount for this specific category, user, and date range
                double spent = getSpentAmountForCategoryInDateRange(db, categoryId, currentUserId, startDate, endDate);

                // Get transaction count for this specific date range
                int recentTransactionCount = getRecentTransactionCountForCategoryInDateRange(db, categoryId, currentUserId, startDate, endDate);

                category.setCategoryId(categoryId);
                category.setName(name);
                category.setIcon(icon);
                category.setColor(color);
                category.setAllocatedAmount(allocated);
                category.setSpentAmount(spent);
                category.setTransactionCount(recentTransactionCount);

                double percentageUsed = allocated > 0 ? (spent / allocated) * 100 : 0;
                category.setPercentageUsed(percentageUsed);
                category.setVariance(allocated - spent);
                category.setHealthStatus(calculateHealthStatus(percentageUsed));

                budgetCategories.add(category);
                totalAllocated += allocated;
                totalSpent += spent;

                Log.d(TAG, String.format("✅ DEBUG: Category: %s, allocated=%.0f, spent=%.0f, transactions=%d",
                        name, allocated, spent, recentTransactionCount));
            }
            budgetCursor.close();

            currentBudget.setTotalAllocated(totalAllocated);
            currentBudget.setTotalSpent(totalSpent);
            currentBudget.setCategories(budgetCategories);

            Log.d(TAG, String.format("✅ DEBUG: Categories loaded for budget '%s'. Total: %d, Allocated: %.0f, Spent: %.0f",
                    selectedBudgetName, budgetCategories.size(), totalAllocated, totalSpent));

        } catch (Exception e) {
            Log.e(TAG, "❌ ERROR: Exception in loadCategoriesData: " + e.getMessage(), e);
        }
    }

    // NEW METHOD: Get spent amount for specific date range
    private double getSpentAmountForCategoryInDateRange(SQLiteDatabase db, int categoryId, String userId, String startDate, String endDate) {
        String dbStartDate = convertToDbDate(startDate);
        String dbEndDate = convertToDbDate(endDate);

        String spentQuery = "SELECT COALESCE(SUM(" + DatabaseHelper.COLUMN_AMOUNT + "), 0) as spent " +
                "FROM " + DatabaseHelper.TABLE_TRANSACTIONS + " " +
                "WHERE " + DatabaseHelper.COLUMN_CATEGORY_ID + " = ? " +
                "AND " + DatabaseHelper.COLUMN_USER_ID + " = ? " +
                "AND " + DatabaseHelper.COLUMN_TYPE + " = 'expense' " +
                "AND " + DatabaseHelper.COLUMN_DATE + " BETWEEN ? AND ?";

        String[] spentArgs = {String.valueOf(categoryId), userId, dbStartDate, dbEndDate};

        Log.d(TAG, String.format("🔍 Spent query for category %d (user %s): %s to %s",
                categoryId, userId, dbStartDate, dbEndDate));

        Cursor spentCursor = db.rawQuery(spentQuery, spentArgs);
        double spent = 0;
        if (spentCursor.moveToFirst()) {
            spent = spentCursor.getDouble(0);
        }
        spentCursor.close();

        Log.d(TAG, String.format("💰 Category %d spent for user %s in date range: %.0f", categoryId, userId, spent));
        return spent;
    }

    // NEW METHOD: Get transaction count for specific date range
    private int getRecentTransactionCountForCategoryInDateRange(SQLiteDatabase db, int categoryId, String userId, String startDate, String endDate) {
        String dbStartDate = convertToDbDate(startDate);
        String dbEndDate = convertToDbDate(endDate);

        String countQuery = "SELECT COUNT(*) as transaction_count " +
                "FROM " + DatabaseHelper.TABLE_TRANSACTIONS + " " +
                "WHERE " + DatabaseHelper.COLUMN_CATEGORY_ID + " = ? " +
                "AND " + DatabaseHelper.COLUMN_USER_ID + " = ? " +
                "AND " + DatabaseHelper.COLUMN_TYPE + " = 'expense' " +
                "AND " + DatabaseHelper.COLUMN_DATE + " BETWEEN ? AND ?";

        String[] countArgs = {String.valueOf(categoryId), userId, dbStartDate, dbEndDate};

        Log.d(TAG, String.format("🔍 Transaction count query: categoryId=%d, userId=%s, dateRange=%s to %s",
                categoryId, userId, dbStartDate, dbEndDate));

        Cursor countCursor = db.rawQuery(countQuery, countArgs);
        int count = 0;
        if (countCursor.moveToFirst()) {
            count = countCursor.getInt(0);
        }
        countCursor.close();

        Log.d(TAG, String.format("📊 Category %d transaction count for user %s in date range: %d", categoryId, userId, count));
        return count;
    }


    private void debugAllTransactions(SQLiteDatabase db) {
        String query = "SELECT " + DatabaseHelper.COLUMN_ID + ", " +
                DatabaseHelper.COLUMN_USER_ID + ", " +
                DatabaseHelper.COLUMN_CATEGORY_ID + ", " +
                DatabaseHelper.COLUMN_TYPE + ", " +
                DatabaseHelper.COLUMN_AMOUNT + ", " +
                DatabaseHelper.COLUMN_DATE + " " +
                "FROM " + DatabaseHelper.TABLE_TRANSACTIONS + " " +
                "ORDER BY " + DatabaseHelper.COLUMN_DATE + " DESC LIMIT 10";

        Cursor cursor = db.rawQuery(query, null);
        Log.d(TAG, "🔍 DEBUG: All transactions in database (top 10):");

        int count = 0;
        while (cursor.moveToNext()) {
            int id = cursor.getInt(0);
            String userId = cursor.getString(1);
            int categoryId = cursor.getInt(2);
            String type = cursor.getString(3);
            double amount = cursor.getDouble(4);
            String date = cursor.getString(5);

            Log.d(TAG, String.format("  %d: id=%d, user=%s, cat=%d, type=%s, amount=%.0f, date=%s",
                    count + 1, id, userId, categoryId, type, amount, date));
            count++;
        }
        cursor.close();
    }

    private double getSpentAmountForCategory(SQLiteDatabase db, int categoryId, String userId, String startDate, String endDate) {
        String spentQuery = "SELECT COALESCE(SUM(" + DatabaseHelper.COLUMN_AMOUNT + "), 0) as spent " +
                "FROM " + DatabaseHelper.TABLE_TRANSACTIONS + " " +
                "WHERE " + DatabaseHelper.COLUMN_CATEGORY_ID + " = ? " +
                "AND " + DatabaseHelper.COLUMN_USER_ID + " = ? " +
                "AND " + DatabaseHelper.COLUMN_TYPE + " = 'expense' " +
                "AND " + DatabaseHelper.COLUMN_DATE + " BETWEEN ? AND ?";

        String[] spentArgs = {String.valueOf(categoryId), userId, startDate, endDate};

        Log.d(TAG, String.format("🔍 Spent query for category %d (user %s): %s to %s",
                categoryId, userId, startDate, endDate));

        Cursor spentCursor = db.rawQuery(spentQuery, spentArgs);
        double spent = 0;
        if (spentCursor.moveToFirst()) {
            spent = spentCursor.getDouble(0);
        }
        spentCursor.close();

        Log.d(TAG, String.format("💰 Category %d spent for user %s: %.0f", categoryId, userId, spent));
        return spent;
    }

    private int getRecentTransactionCountForCategory(SQLiteDatabase db, int categoryId, String userId, String startDate, String endDate) {
        String countQuery = "SELECT COUNT(*) as transaction_count " +
                "FROM " + DatabaseHelper.TABLE_TRANSACTIONS + " " +
                "WHERE " + DatabaseHelper.COLUMN_CATEGORY_ID + " = ? " +
                "AND " + DatabaseHelper.COLUMN_USER_ID + " = ? " +
                "AND " + DatabaseHelper.COLUMN_TYPE + " = 'expense' " +
                "AND " + DatabaseHelper.COLUMN_DATE + " BETWEEN ? AND ?";

        String[] countArgs = {String.valueOf(categoryId), userId, startDate, endDate};

        Log.d(TAG, String.format("🔍 Transaction count query: categoryId=%d, userId=%s, startDate=%s, endDate=%s",
                categoryId, userId, startDate, endDate));

        // Debug: In ra câu query để kiểm tra
        Log.d(TAG, "🔍 Query: " + countQuery);
        Log.d(TAG, "🔍 Args: " + java.util.Arrays.toString(countArgs));

        Cursor countCursor = db.rawQuery(countQuery, countArgs);
        int count = 0;
        if (countCursor.moveToFirst()) {
            count = countCursor.getInt(0);
        }
        countCursor.close();

        Log.d(TAG, String.format("📊 Category %d transaction count for user %s: %d", categoryId, userId, count));
        return count;
    }


    private int getTransactionCountForCategory(SQLiteDatabase db, int categoryId, String startDate, String endDate) {
        // This method should also include user_id for proper filtering
        return getRecentTransactionCountForCategory(db, categoryId, currentUserId, startDate, endDate);
    }

    private void setupActionBar() {
        if (getSupportActionBar() != null) {
            String title = selectedBudgetName != null ?
                    "💰 " + selectedBudgetName :
                    "💰 Ngân Sách Thông Minh";
            getSupportActionBar().setTitle(title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private String getCategoryNameById(int categoryId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try {
            String query = "SELECT " + DatabaseHelper.COLUMN_NAME + " " +
                    "FROM " + DatabaseHelper.TABLE_CATEGORIES + " " +
                    "WHERE " + DatabaseHelper.COLUMN_ID + " = ?";

            Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(categoryId)});

            if (cursor.moveToFirst()) {
                String categoryName = cursor.getString(0);
                cursor.close();
                return categoryName;
            }
            cursor.close();

        } catch (Exception e) {
            Log.e(TAG, "Error getting category name for ID: " + categoryId, e);
        }

        return "Unknown Category";
    }

    private boolean validateTransactionOwnership(int transactionId, String userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try {
            String query = "SELECT COUNT(*) " +
                    "FROM " + DatabaseHelper.TABLE_TRANSACTIONS + " " +
                    "WHERE " + DatabaseHelper.COLUMN_ID + " = ? " +
                    "AND " + DatabaseHelper.COLUMN_USER_ID + " = ?";

            Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(transactionId), userId});

            boolean isValid = false;
            if (cursor.moveToFirst()) {
                isValid = cursor.getInt(0) > 0;
            }
            cursor.close();

            return isValid;

        } catch (Exception e) {
            Log.e(TAG, "Error validating transaction ownership", e);
            return false;
        }
    }

    private List<TransactionSummary> getTransactionsSummaryByCategory(String userId, String startDate, String endDate) {
        List<TransactionSummary> summaries = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try {
            String query = "SELECT " +
                    "t." + DatabaseHelper.COLUMN_CATEGORY_ID + ", " +
                    "c." + DatabaseHelper.COLUMN_NAME + " as category_name, " +
                    "COUNT(t." + DatabaseHelper.COLUMN_ID + ") as transaction_count, " +
                    "SUM(CASE WHEN t." + DatabaseHelper.COLUMN_TYPE + " = 'expense' THEN t." + DatabaseHelper.COLUMN_AMOUNT + " ELSE 0 END) as total_expense, " +
                    "SUM(CASE WHEN t." + DatabaseHelper.COLUMN_TYPE + " = 'income' THEN t." + DatabaseHelper.COLUMN_AMOUNT + " ELSE 0 END) as total_income " +
                    "FROM " + DatabaseHelper.TABLE_TRANSACTIONS + " t " +
                    "JOIN " + DatabaseHelper.TABLE_CATEGORIES + " c ON t." + DatabaseHelper.COLUMN_CATEGORY_ID + " = c." + DatabaseHelper.COLUMN_ID + " " +
                    "WHERE t." + DatabaseHelper.COLUMN_USER_ID + " = ? " +
                    "AND t." + DatabaseHelper.COLUMN_DATE + " BETWEEN ? AND ? " +
                    "GROUP BY t." + DatabaseHelper.COLUMN_CATEGORY_ID + ", c." + DatabaseHelper.COLUMN_NAME + " " +
                    "ORDER BY total_expense DESC";

            Cursor cursor = db.rawQuery(query, new String[]{userId, startDate, endDate});

            while (cursor.moveToNext()) {
                TransactionSummary summary = new TransactionSummary();
                summary.categoryId = cursor.getInt(0);
                summary.categoryName = cursor.getString(1);
                summary.transactionCount = cursor.getInt(2);
                summary.totalExpense = cursor.getDouble(3);
                summary.totalIncome = cursor.getDouble(4);
                summaries.add(summary);
            }
            cursor.close();

        } catch (Exception e) {
            Log.e(TAG, "Error getting transaction summary by category", e);
        }

        return summaries;
    }

    public static class TransactionSummary {
        public int categoryId;
        public String categoryName;
        public int transactionCount;
        public double totalExpense;
        public double totalIncome;

        @Override
        public String toString() {
            return String.format("Category: %s, Count: %d, Expense: %.0f, Income: %.0f",
                    categoryName, transactionCount, totalExpense, totalIncome);
        }
    }


    private <T> T getColumnValue(Cursor cursor, String columnName, T defaultValue) {
        try {
            int columnIndex = cursor.getColumnIndex(columnName);
            if (columnIndex >= 0) {
                if (defaultValue instanceof Integer) {
                    return (T) Integer.valueOf(cursor.getInt(columnIndex));
                } else if (defaultValue instanceof Double) {
                    return (T) Double.valueOf(cursor.getDouble(columnIndex));
                } else if (defaultValue instanceof String) {
                    return (T) cursor.getString(columnIndex);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error getting column value for: " + columnName + ", using default: " + defaultValue);
        }
        return defaultValue;
    }

    private void generateSmartInsights() {
        smartInsights.clear();

        if (currentBudget == null || budgetCategories.isEmpty()) {
            return;
        }

        generateOverspendingInsights();
        generateSavingOpportunities();
        generateBudgetHealthInsights();
        generateTimeBasedInsights();
    }

    private void generateOverspendingInsights() {
        for (BudgetPlan.CategoryBudget category : budgetCategories) {
            if (category.getPercentageUsed() > 100) {
                SmartInsight insight = new SmartInsight();
                insight.setTitle("⚠️ Vượt ngân sách: " + category.getName());
                insight.setMessage("Bạn đã chi tiêu vượt " +
                        String.format("%.1f%%", category.getPercentageUsed() - 100) +
                        " so với kế hoạch");
                insight.setType(SmartInsight.Type.WARNING);
                insight.setImpact(SmartInsight.Impact.HIGH);
                insight.setSuggestion("Hạn chế chi tiêu hoặc điều chỉnh ngân sách cho danh mục này");
                smartInsights.add(insight);
            }
        }
    }

    private void generateSavingOpportunities() {
        for (BudgetPlan.CategoryBudget category : budgetCategories) {
            if (category.getPercentageUsed() < 50 && category.getAllocatedAmount() > 500000) {
                SmartInsight insight = new SmartInsight();
                insight.setTitle("💡 Cơ hội tiết kiệm: " + category.getName());
                insight.setMessage("Bạn chỉ sử dụng " +
                        String.format("%.1f%%", category.getPercentageUsed()) +
                        " ngân sách. Có thể điều chỉnh cho mục tiêu khác");
                insight.setType(SmartInsight.Type.OPTIMIZATION);
                insight.setImpact(SmartInsight.Impact.MEDIUM);
                insight.setSuggestion("Xem xét giảm ngân sách cho danh mục này");
                smartInsights.add(insight);
            }
        }
    }

    private void generateBudgetHealthInsights() {
        double overallUsage = currentBudget.getTotalAllocated() > 0 ?
                (currentBudget.getTotalSpent() / currentBudget.getTotalAllocated()) * 100 : 0;

        SmartInsight healthInsight = new SmartInsight();

        if (overallUsage <= 70) {
            healthInsight.setTitle("✅ Ngân sách khỏe mạnh");
            healthInsight.setMessage("Bạn đang quản lý ngân sách rất tốt!");
            healthInsight.setType(SmartInsight.Type.POSITIVE);
            healthInsight.setImpact(SmartInsight.Impact.POSITIVE);
        } else if (overallUsage <= 90) {
            healthInsight.setTitle("⚠️ Cần chú ý");
            healthInsight.setMessage("Ngân sách đang sử dụng " +
                    String.format("%.1f%%", overallUsage) + ". Hãy cẩn thận với các chi tiêu tiếp theo");
            healthInsight.setType(SmartInsight.Type.WARNING);
            healthInsight.setImpact(SmartInsight.Impact.MEDIUM);
        } else {
            healthInsight.setTitle("🚨 Nguy hiểm");
            healthInsight.setMessage("Ngân sách gần cạn kiệt. Cần hành động ngay!");
            healthInsight.setType(SmartInsight.Type.RISK);
            healthInsight.setImpact(SmartInsight.Impact.HIGH);
        }

        smartInsights.add(healthInsight);
    }

    private void generateTimeBasedInsights() {
        int daysRemaining = calculateDaysRemaining();

        if (daysRemaining <= 0) {
            SmartInsight timeInsight = new SmartInsight();
            timeInsight.setTitle("📅 Ngân sách hết hạn");
            timeInsight.setMessage("Kỳ ngân sách hiện tại đã kết thúc. Hãy tạo ngân sách mới.");
            timeInsight.setType(SmartInsight.Type.INFO);
            timeInsight.setImpact(SmartInsight.Impact.HIGH);
            timeInsight.setSuggestion("Tạo ngân sách cho kỳ tiếp theo");
            smartInsights.add(timeInsight);
        } else if (daysRemaining <= 3) {
            SmartInsight timeInsight = new SmartInsight();
            timeInsight.setTitle("⏰ Sắp hết kỳ ngân sách");
            timeInsight.setMessage("Chỉ còn " + daysRemaining + " ngày trong kỳ ngân sách này.");
            timeInsight.setType(SmartInsight.Type.WARNING);
            timeInsight.setImpact(SmartInsight.Impact.MEDIUM);
            timeInsight.setSuggestion("Chuẩn bị ngân sách cho kỳ tiếp theo");
            smartInsights.add(timeInsight);
        }
    }

    // ===== EVENT HANDLERS =====

    private void showInsightDetails(SmartInsight insight) {
        new AlertDialog.Builder(this)
                .setTitle(insight.getTitle())
                .setMessage(insight.getMessage() + "\n\nĐề xuất: " + insight.getSuggestion())
                .setPositiveButton("Đóng", null)
                .setNeutralButton("Thực hiện", (dialog, which) -> executeInsightAction(insight))
                .show();
    }

    private void executeInsightAction(SmartInsight insight) {
        String title = insight.getTitle().toLowerCase();

        if (title.contains("vượt ngân sách")) {
            adjustEntireBudget();
        } else if (title.contains("cơ hội tiết kiệm")) {
            adjustEntireBudget();
        } else if (title.contains("hết hạn") || title.contains("sắp hết")) {
            startCreateBudgetActivity();
        } else {
            Toast.makeText(this, "Đã thực hiện: " + insight.getSuggestion(), Toast.LENGTH_SHORT).show();
        }
    }

    private void startCreateBudgetActivity() {
        Intent intent = new Intent(this, CreateBudgetActivity.class);
        intent.putExtra("user_id", currentUserId);
        startActivityForResult(intent, REQUEST_CREATE_BUDGET);
    }

    private void startQuickExpenseActivity() {
        Intent intent = new Intent(this, AddTransactionActivity.class);
        intent.putExtra("user_id", currentUserId);
        intent.putExtra("transaction_type", "expense");
        startActivityForResult(intent, REQUEST_ADD_EXPENSE);
    }

    // ===== UTILITY METHODS =====

    private String calculateHealthStatus(double percentageUsed) {
        if (percentageUsed <= 70) return "excellent";
        else if (percentageUsed <= 85) return "good";
        else if (percentageUsed <= 95) return "warning";
        else if (percentageUsed <= 105) return "danger";
        else return "critical";
    }

    private int calculateOverallHealthScore() {
        if (budgetCategories.isEmpty()) return 0;

        int totalScore = 0;
        for (BudgetPlan.CategoryBudget category : budgetCategories) {
            double percentage = category.getPercentageUsed();
            if (percentage <= 70) totalScore += 100;
            else if (percentage <= 85) totalScore += 80;
            else if (percentage <= 95) totalScore += 60;
            else if (percentage <= 105) totalScore += 40;
            else totalScore += 20;
        }

        return totalScore / budgetCategories.size();
    }

    private String getHealthStatusText(int score) {
        if (score >= 85) return "✅ Tuyệt vời";
        else if (score >= 70) return "👍 Tốt";
        else if (score >= 50) return "⚠️ Cần chú ý";
        else return "🚨 Cần cải thiện";
    }

    private int getHealthColor(int score) {
        if (score >= 85) return Color.parseColor("#4CAF50");
        else if (score >= 70) return Color.parseColor("#8BC34A");
        else if (score >= 50) return Color.parseColor("#FF9800");
        else return Color.parseColor("#F44336");
    }

    private void updateUserGreeting() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int hour = cal.get(java.util.Calendar.HOUR_OF_DAY);

        String greeting;
        if (hour < 12) {
            greeting = "🌅 Chào buổi sáng! Hôm nay bạn có kế hoạch chi tiêu gì?";
        } else if (hour < 18) {
            greeting = "☀️ Chào buổi chiều! Tình hình ngân sách thế nào rồi?";
        } else {
            greeting = "🌙 Chào buổi tối! Cùng xem lại chi tiêu hôm nay nhé!";
        }

        tvUserGreeting.setText(greeting);
    }

    private void showError(String message) {
        Toast.makeText(this, "❌ " + message, Toast.LENGTH_LONG).show();
    }

    private void redirectToLogin() {
        Toast.makeText(this, "Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.", Toast.LENGTH_LONG).show();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    public BudgetPlan createBudgetFromCursor(Cursor cursor, String userId) {
        BudgetPlan budget = new BudgetPlan();

        int nameIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_BUDGET_NAME);
        int periodIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_BUDGET_PERIOD);
        int startDateIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_BUDGET_START_DATE);
        int endDateIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_BUDGET_END_DATE);

        if (nameIndex >= 0) budget.setName(cursor.getString(nameIndex));
        if (periodIndex >= 0) budget.setPeriod(cursor.getString(periodIndex));
        if (startDateIndex >= 0) budget.setStartDate(cursor.getString(startDateIndex));
        if (endDateIndex >= 0) budget.setEndDate(cursor.getString(endDateIndex));

        budget.setUserId(userId);
        return budget;
    }

    public void updateSpinnerToMatchPeriod(String period) {
        runOnUiThread(() -> {
            int position = -1;
            switch (period) {
                case "weekly": position = 0; break;
                case "monthly": position = 1; break;
                case "quarterly": position = 2; break;
                case "yearly": position = 3; break;
            }

            if (position >= 0) {
                Log.d(TAG, "Auto-updating spinner to position " + position + " for period: " + period);
                spinnerBudgetPeriod.setSelection(position);
            }
        });
    }

    // ===== LIFECYCLE =====

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CREATE_BUDGET:
                    Toast.makeText(this, "Ngân sách mới đã được tạo thành công!", Toast.LENGTH_LONG).show();
                    loadBudgetData();
                    break;
                case REQUEST_ADJUST_BUDGET:
                    Toast.makeText(this, "Ngân sách đã được điều chỉnh thành công!", Toast.LENGTH_SHORT).show();
                    loadBudgetData();
                    break;
                case REQUEST_ADD_EXPENSE:
                    Toast.makeText(this, "Chi tiêu đã được thêm thành công!", Toast.LENGTH_SHORT).show();
                    loadBudgetData();
                    break;
                case REQUEST_SETTINGS:
                    Toast.makeText(this, "Cài đặt đã được cập nhật!", Toast.LENGTH_SHORT).show();
                    // Reload to apply new settings
                    loadBudgetData();
                    break;
            }
        } else if (resultCode == RESULT_CANCELED) {
            switch (requestCode) {
                case REQUEST_CREATE_BUDGET:
                    Log.d(TAG, "Budget creation cancelled by user");
                    break;
                case REQUEST_ADJUST_BUDGET:
                    Log.d(TAG, "Budget adjustment cancelled by user");
                    break;
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "Activity resumed - refreshing data");

        // Update greeting for current time
        updateUserGreeting();

        // Refresh data if user ID is available
        if (currentUserId != null && !currentUserId.isEmpty()) {
            // Only reload if we've been away for more than 30 seconds
            // This prevents excessive reloading when quickly switching between activities
            long lastResumeTime = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .getLong("last_resume_time", 0);
            long currentTime = System.currentTimeMillis();

            if (currentTime - lastResumeTime > 30000) { // 30 seconds
                loadBudgetData();
            }

            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .edit()
                    .putLong("last_resume_time", currentTime)
                    .apply();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "Activity paused");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Activity destroyed - cleaning up resources");

        // Cleanup resources
        if (dbHelper != null) {
            dbHelper.close();
        }

        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }

        // Clear adapters to prevent memory leaks
        if (categoryAdapter != null) {
            categoryAdapter.updateData(new ArrayList<>());
        }

        if (insightAdapter != null) {
            insightAdapter.updateInsights(new ArrayList<>());
        }
    }

    // ===== PUBLIC METHODS FOR EXTERNAL ACCESS =====

    /**
     * Force refresh all data (useful for testing or manual refresh)
     */
    /**
     * Get current budget data (for external access)
     */
    public BudgetPlan getCurrentBudget() {
        return currentBudget;
    }

    /**
     * Get budget categories (for external access)
     */
    public List<BudgetPlan.CategoryBudget> getBudgetCategories() {
        return new ArrayList<>(budgetCategories);
    }

    /**
     * Get smart insights (for external access)
     */
    public List<SmartInsight> getSmartInsights() {
        return new ArrayList<>(smartInsights);
    }

    /**
     * Get current user ID (for external access)
     */
    public String getCurrentUserIdPublic() {
        return currentUserId;
    }

    /**
     * Check if budget data is currently loaded
     */
    public boolean hasBudgetData() {
        return currentBudget != null && !budgetCategories.isEmpty();
    }

    /**
     * Get budget health score (0-100)
     */
    public int getBudgetHealthScore() {
        return calculateOverallHealthScore();
    }

    /**
     * Get days remaining in current budget period
     */
    public int getDaysRemainingInPeriod() {
        return calculateDaysRemaining();
    }

    /**
     * Get total budget utilization percentage
     */
    public double getBudgetUtilizationPercentage() {
        if (currentBudget == null || currentBudget.getTotalAllocated() <= 0) {
            return 0.0;
        }
        return (currentBudget.getTotalSpent() / currentBudget.getTotalAllocated()) * 100;
    }

    // ===== DEBUGGING METHODS (Remove in production) =====

    /**
     * Debug method to log current state
     */
    public void debugCurrentState() {
        Log.d(TAG, "=== CURRENT STATE DEBUG ===");
        Log.d(TAG, "User ID: " + currentUserId);
        Log.d(TAG, "Has Budget: " + (currentBudget != null));
        Log.d(TAG, "Categories Count: " + budgetCategories.size());
        Log.d(TAG, "Insights Count: " + smartInsights.size());
        Log.d(TAG, "Health Score: " + calculateOverallHealthScore());
        Log.d(TAG, "============================");
    }

    /**
     * Test method to generate sample data (for testing only)
     */
    public void generateTestData() {
        Log.w(TAG, "WARNING: Generating test data - use only for testing!");
        // This method can be used for testing UI without real data
        // Implementation would create mock BudgetPlan and categories
    }
}