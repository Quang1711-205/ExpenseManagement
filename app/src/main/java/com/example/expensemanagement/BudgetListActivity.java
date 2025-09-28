//package com.example.expensemanagement;
//
//import android.content.Intent;
//import android.os.Bundle;
//import android.text.Editable;
//import android.text.TextWatcher;
//import android.view.View;
//import android.widget.Button;
//import android.widget.EditText;
//import android.widget.ImageView;
//import android.widget.LinearLayout;
//import android.widget.TextView;
//
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.cardview.widget.CardView;
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.stream.Collectors;
//
//import models.Budget;
//import adapters.BudgetAdapter;
//
//public class BudgetListActivity extends AppCompatActivity implements BudgetAdapter.OnItemClickListener {
//
//    private SwipeRefreshLayout swipeRefreshLayout;
//    private RecyclerView rvBudgetList;
//    private LinearLayout layoutNoData, layoutQuickActions, layoutQuickStats;
//    private CardView cardBudgetList;
//    private EditText etSearchBudget;
//    private ImageView ivClearSearch, ivBackButton, ivAddBudget;
//    private Button btnFilterAll, btnFilterActive, btnFilterCompleted, btnFilterMonth;
//    private TextView tvTotalBudgets, tvActiveBudgets, tvTotalAmount;
//    private TextView tvWelcomeTitle, tvWelcomeSubtitle;
//
//    private BudgetAdapter budgetAdapter;
//    private List<Budget> allBudgets;
//    private List<Budget> filteredBudgets;
//    private String currentFilter = "all";
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_budget_list);
//
//        initViews();
//        setupRecyclerView();
//        setupListeners();
//        loadBudgetData();
//        updateWelcomeMessage();
//    }
//
//    private void initViews() {
//        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
//        rvBudgetList = findViewById(R.id.rv_budget_list);
//        layoutNoData = findViewById(R.id.layout_no_data);
//        layoutQuickActions = findViewById(R.id.layout_quick_actions);
//        layoutQuickStats = findViewById(R.id.layout_quick_stats);
//        cardBudgetList = findViewById(R.id.card_budget_list);
//        etSearchBudget = findViewById(R.id.et_search_budget);
//        ivClearSearch = findViewById(R.id.iv_clear_search);
//        ivBackButton = findViewById(R.id.iv_back_button);
//        ivAddBudget = findViewById(R.id.iv_add_budget);
//
//        btnFilterAll = findViewById(R.id.btn_filter_all);
//        btnFilterActive = findViewById(R.id.btn_filter_active);
//        btnFilterCompleted = findViewById(R.id.btn_filter_completed);
//        btnFilterMonth = findViewById(R.id.btn_filter_month);
//
//        tvTotalBudgets = findViewById(R.id.tv_total_budgets);
//        tvActiveBudgets = findViewById(R.id.tv_active_budgets);
//        tvTotalAmount = findViewById(R.id.tv_total_amount);
//
//        tvWelcomeTitle = findViewById(R.id.tv_welcome_title);
//        tvWelcomeSubtitle = findViewById(R.id.tv_welcome_subtitle);
//    }
//
//    private void setupRecyclerView() {
//        allBudgets = new ArrayList<>();
//        filteredBudgets = new ArrayList<>();
//        budgetAdapter = new BudgetAdapter(this, filteredBudgets);
//        budgetAdapter.setOnItemClickListener(this);
//
//        rvBudgetList.setLayoutManager(new LinearLayoutManager(this));
//        rvBudgetList.setAdapter(budgetAdapter);
//        rvBudgetList.setNestedScrollingEnabled(false);
//    }
//
//    private void setupListeners() {
//        // Back button
//        ivBackButton.setOnClickListener(v -> onBackPressed());
//
//        // Add budget button
//        ivAddBudget.setOnClickListener(v -> openAddBudgetActivity());
//
//        // Search functionality
//        etSearchBudget.addTextChangedListener(new TextWatcher() {
//            @Override
//            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
//
//            @Override
//            public void onTextChanged(CharSequence s, int start, int before, int count) {
//                filterBudgets(s.toString());
//                ivClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
//            }
//
//            @Override
//            public void afterTextChanged(Editable s) {}
//        });
//
//        ivClearSearch.setOnClickListener(v -> {
//            etSearchBudget.setText("");
//            filterBudgets("");
//        });
//
//        // Filter buttons
//        btnFilterAll.setOnClickListener(v -> setFilter("all"));
//        btnFilterActive.setOnClickListener(v -> setFilter("active"));
//        btnFilterCompleted.setOnClickListener(v -> setFilter("completed"));
//        btnFilterMonth.setOnClickListener(v -> setFilter("month"));
//
//        // Swipe to refresh
//        swipeRefreshLayout.setOnRefreshListener(this::refreshData);
//
//        // Quick actions
//        findViewById(R.id.btn_create_first_budget).setOnClickListener(v -> openAddBudgetActivity());
//        findViewById(R.id.btn_create_new_budget).setOnClickListener(v -> openAddBudgetActivity());
//        findViewById(R.id.btn_budget_analytics).setOnClickListener(v -> openAnalyticsActivity());
//    }
//
//    private void loadBudgetData() {
//        // Sample data - replace with real data loading
//        allBudgets.clear();
//        allBudgets.add(new Budget("1", "Ngân sách tháng 12", "01/12/2024", "31/12/2024",
//                5000000, 1850000, "Đang hoạt động", "💰", 8, "Tốt"));
//        allBudgets.add(new Budget("2", "Ngân sách du lịch", "15/11/2024", "20/11/2024",
//                3000000, 2800000, "Hoàn thành", "✈️", 5, "Tốt"));
//        allBudgets.add(new Budget("3", "Ngân sách mua sắm", "10/12/2024", "25/12/2024",
//                2000000, 500000, "Đang hoạt động", "🛒", 6, "Trung bình"));
//        allBudgets.add(new Budget("4", "Ngân sách ăn uống", "01/12/2024", "31/12/2024",
//                1500000, 800000, "Đang hoạt động", "🍽️", 4, "Cần cải thiện"));
//
//        applyCurrentFilter();
//        updateUI();
//    }
//
//    private void refreshData() {
//        // Simulate network delay
//        swipeRefreshLayout.postDelayed(() -> {
//            loadBudgetData();
//            swipeRefreshLayout.setRefreshing(false);
//        }, 1000);
//    }
//
//    private void filterBudgets(String searchQuery) {
//        filteredBudgets.clear();
//
//        List<Budget> baseList = getFilteredByStatus();
//
//        if (searchQuery.isEmpty()) {
//            filteredBudgets.addAll(baseList);
//        } else {
//            for (Budget budget : baseList) {
//                if (budget.getName().toLowerCase().contains(searchQuery.toLowerCase())) {
//                    filteredBudgets.add(budget);
//                }
//            }
//        }
//
//        budgetAdapter.notifyDataSetChanged();
//        updateUI();
//    }
//
//    private void setFilter(String filter) {
//        currentFilter = filter;
//        updateFilterButtons();
//        applyCurrentFilter();
//        updateUI();
//    }
//
//    private void updateFilterButtons() {
//        // Reset all buttons
//        btnFilterAll.setBackgroundResource(R.drawable.tab_button_normal);
//        btnFilterActive.setBackgroundResource(R.drawable.tab_button_normal);
//        btnFilterCompleted.setBackgroundResource(R.drawable.tab_button_normal);
//        btnFilterMonth.setBackgroundResource(R.drawable.tab_button_normal);
//
//        // Set selected button
//        switch (currentFilter) {
//            case "all":
//                btnFilterAll.setBackgroundResource(R.drawable.tab_button_selected);
//                break;
//            case "active":
//                btnFilterActive.setBackgroundResource(R.drawable.tab_button_selected);
//                break;
//            case "completed":
//                btnFilterCompleted.setBackgroundResource(R.drawable.tab_button_selected);
//                break;
//            case "month":
//                btnFilterMonth.setBackgroundResource(R.drawable.tab_button_selected);
//                break;
//        }
//    }
//
//    private void applyCurrentFilter() {
//        filteredBudgets.clear();
//        filteredBudgets.addAll(getFilteredByStatus());
//        budgetAdapter.notifyDataSetChanged();
//    }
//
//    private List<Budget> getFilteredByStatus() {
//        switch (currentFilter) {
//            case "active":
//                return allBudgets.stream()
//                        .filter(budget -> "Đang hoạt động".equals(budget.getStatus()))
//                        .collect(Collectors.toList());
//            case "completed":
//                return allBudgets.stream()
//                        .filter(budget -> "Hoàn thành".equals(budget.getStatus()))
//                        .collect(Collectors.toList());
//            case "month":
//                // Filter by current month (December 2024 in this example)
//                return allBudgets.stream()
//                        .filter(budget -> budget.getStartDate().contains("12/2024"))
//                        .collect(Collectors.toList());
//            default:
//                return new ArrayList<>(allBudgets);
//        }
//    }
//
//    private void updateUI() {
//        boolean hasBudgets = !filteredBudgets.isEmpty();
//
//        cardBudgetList.setVisibility(hasBudgets ? View.VISIBLE : View.GONE);
//        layoutNoData.setVisibility(hasBudgets ? View.GONE : View.VISIBLE);
//        layoutQuickActions.setVisibility(hasBudgets ? View.VISIBLE : View.GONE);
//        layoutQuickStats.setVisibility(hasBudgets ? View.VISIBLE : View.GONE);
//
//        if (hasBudgets) {
//            updateQuickStats();
//        }
//    }
//
//    private void updateQuickStats() {
//        int totalBudgets = allBudgets.size();
//        int activeBudgets = (int) allBudgets.stream()
//                .filter(budget -> "Đang hoạt động".equals(budget.getStatus()))
//                .count();
//        double totalAmount = allBudgets.stream()
//                .mapToDouble(Budget::getTotalAmount)
//                .sum();
//
//        tvTotalBudgets.setText(String.valueOf(totalBudgets));
//        tvActiveBudgets.setText(String.valueOf(activeBudgets));
//        tvTotalAmount.setText(String.format("%.0fM", totalAmount / 1000000));
//    }
//
//    private void updateWelcomeMessage() {
//        // Update welcome message based on time of day
//        java.util.Calendar calendar = java.util.Calendar.getInstance();
//        int hour = calendar.get(java.util.Calendar.HOUR_OF_DAY);
//
//        if (hour < 12) {
//            tvWelcomeTitle.setText("Chào buổi sáng! 🌅");
//        } else if (hour < 18) {
//            tvWelcomeTitle.setText("Chào buổi chiều! ☀️");
//        } else {
//            tvWelcomeTitle.setText("Chào buổi tối! 🌙");
//        }
//    }
//
//    private void openAddBudgetActivity() {
//        // Intent to AddBudgetActivity
//        // Intent intent = new Intent(this, AddBudgetActivity.class);
//        // startActivity(intent);
//    }
//
//    private void openAnalyticsActivity() {
//        // Intent to AnalyticsActivity
//        // Intent intent = new Intent(this, AnalyticsActivity.class);
//        // startActivity(intent);
//    }
//
//    // BudgetAdapter.OnItemClickListener implementations
//    @Override
//    public void onItemClick(Budget budget) {
//        Intent intent = new Intent(this, SmartBudgetActivity.class);
//        intent.putExtra("BUDGET_ID", budget.getId());
//        intent.putExtra("BUDGET_NAME", budget.getName());
//        intent.putExtra("BUDGET_PERIOD", budget.getPeriod());
//        intent.putExtra("TOTAL_AMOUNT", budget.getTotalAmount());
//        intent.putExtra("SPENT_AMOUNT", budget.getSpentAmount());
//        intent.putExtra("STATUS", budget.getStatus());
//        intent.putExtra("CATEGORIES_COUNT", budget.getCategoriesCount());
//        intent.putExtra("HEALTH_SCORE", budget.getHealthScore());
//        intent.putExtra("PROGRESS_PERCENTAGE", budget.getProgressPercentage());
//        intent.putExtra("REMAINING_DAYS", budget.getRemainingDays());
//        startActivity(intent);
//    }
//
//    @Override
//    public void onMoreOptionsClick(Budget budget) {
//        // Show popup menu with options like Edit, Delete, Duplicate
//        // PopupMenu popup = new PopupMenu(this, view);
//        // popup.inflate(R.menu.budget_options_menu);
//        // popup.show();
//    }
//
//    @Override
//    public void onQuickExpenseClick(Budget budget) {
//        // Open quick expense dialog
//        // QuickExpenseDialog dialog = new QuickExpenseDialog(budget);
//        // dialog.show(getSupportFragmentManager(), "QuickExpense");
//    }
//
//    public void onBackClick(View view) {
//        onBackPressed();
//    }
//}


package com.example.expensemanagement;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import models.Budget;
import adapters.BudgetAdapter;
import database.DatabaseHelper;

public class BudgetListActivity extends AppCompatActivity implements BudgetAdapter.OnItemClickListener {

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView rvBudgetList;
    private LinearLayout layoutNoData, layoutQuickActions, layoutQuickStats;
    private CardView cardBudgetList;
    private EditText etSearchBudget;
    private ImageView ivClearSearch, ivBackButton, ivAddBudget;
    // Thêm vào đầu class:
    private Button btnFilterAll, btnFilterActive, btnFilterEnded;
    private TextView tvTotalBudgets, tvActiveBudgets, tvTotalAmount;
    private TextView tvWelcomeTitle, tvWelcomeSubtitle;

    private BudgetAdapter budgetAdapter;
    private List<Budget> allBudgets;
    private List<Budget> filteredBudgets;
    private String currentFilter = "all";
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_budget_list);

        // Initialize database helper
        databaseHelper = new DatabaseHelper(this);

        initViews();
        setupRecyclerView();
        setupListeners();
        loadBudgetData();
        updateWelcomeMessage();
    }

    private void initViews() {
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        rvBudgetList = findViewById(R.id.rv_budget_list);
        layoutNoData = findViewById(R.id.layout_no_data);
        layoutQuickActions = findViewById(R.id.layout_quick_actions);
        layoutQuickStats = findViewById(R.id.layout_quick_stats);
        cardBudgetList = findViewById(R.id.card_budget_list);
        etSearchBudget = findViewById(R.id.et_search_budget);
        ivClearSearch = findViewById(R.id.iv_clear_search);
        ivBackButton = findViewById(R.id.iv_back_button);
        ivAddBudget = findViewById(R.id.iv_add_budget);

        btnFilterAll = findViewById(R.id.btn_filter_all);
        btnFilterActive = findViewById(R.id.btn_filter_active);
        btnFilterEnded = findViewById(R.id.btn_filter_ended);

        tvTotalBudgets = findViewById(R.id.tv_total_budgets);
        tvActiveBudgets = findViewById(R.id.tv_active_budgets);
        tvTotalAmount = findViewById(R.id.tv_total_amount);

        tvWelcomeTitle = findViewById(R.id.tv_welcome_title);
        tvWelcomeSubtitle = findViewById(R.id.tv_welcome_subtitle);
    }

    private void setupRecyclerView() {
        allBudgets = new ArrayList<>();
        filteredBudgets = new ArrayList<>();
        budgetAdapter = new BudgetAdapter(this, filteredBudgets);
        budgetAdapter.setOnItemClickListener(this);

        rvBudgetList.setLayoutManager(new LinearLayoutManager(this));
        rvBudgetList.setAdapter(budgetAdapter);
        rvBudgetList.setNestedScrollingEnabled(false);
    }

    private void setupListeners() {
        // Back button
        ivBackButton.setOnClickListener(v -> onBackPressed());

        // Add budget button
        ivAddBudget.setOnClickListener(v -> openAddBudgetActivity());

        // Search functionality
        etSearchBudget.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterBudgets(s.toString());
                ivClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        ivClearSearch.setOnClickListener(v -> {
            etSearchBudget.setText("");
            filterBudgets("");
        });

        // Filter buttons
        btnFilterAll.setOnClickListener(v -> setFilter("all"));
        btnFilterActive.setOnClickListener(v -> setFilter("active"));
        btnFilterEnded.setOnClickListener(v -> setFilter("ended"));


        // Swipe to refresh
        swipeRefreshLayout.setOnRefreshListener(this::refreshData);

        // Quick actions
        findViewById(R.id.btn_create_first_budget).setOnClickListener(v -> openAddBudgetActivity());
        findViewById(R.id.btn_create_new_budget).setOnClickListener(v -> openAddBudgetActivity());
        findViewById(R.id.btn_budget_analytics).setOnClickListener(v -> openAnalyticsActivity());
    }

    private void loadBudgetData() {
        allBudgets.clear();

        try {
            android.database.sqlite.SQLiteDatabase db = databaseHelper.getReadableDatabase();

            // Enhanced query to get more complete budget information
            String budgetQuery = "SELECT budget_name, " +
                    "MIN(start_date) as start_date, " +
                    "MAX(end_date) as end_date, " +
                    "SUM(amount) as total_amount, " +
                    "GROUP_CONCAT(DISTINCT category_id) as category_ids, " +
                    "MIN(_id) as budget_id, " +
                    "period " +
                    "FROM budgets " +
                    "GROUP BY budget_name " +
                    "ORDER BY MIN(created_at) DESC";

            android.database.Cursor budgetCursor = db.rawQuery(budgetQuery, null);

            if (budgetCursor != null && budgetCursor.moveToFirst()) {
                do {
                    String budgetId = budgetCursor.getString(budgetCursor.getColumnIndexOrThrow("budget_id"));
                    String budgetName = budgetCursor.getString(budgetCursor.getColumnIndexOrThrow("budget_name"));
                    double totalAmount = budgetCursor.getDouble(budgetCursor.getColumnIndexOrThrow("total_amount"));
                    String startDate = budgetCursor.getString(budgetCursor.getColumnIndexOrThrow("start_date"));
                    String endDate = budgetCursor.getString(budgetCursor.getColumnIndexOrThrow("end_date"));
                    String categoryIds = budgetCursor.getString(budgetCursor.getColumnIndexOrThrow("category_ids"));
                    String period = budgetCursor.getString(budgetCursor.getColumnIndexOrThrow("period"));

                    // Convert categoryIds string to List<Integer>
                    java.util.List<Integer> categoryIdsList = parseCategoryIds(categoryIds);

                    // Calculate spent amount for budget within its date range
                    double spentAmount = calculateSpentAmountForCategories(db, categoryIdsList, startDate, endDate);

                    // Calculate status based on dates
                    String status = calculateBudgetStatus(startDate, endDate);

                    // Get budget icon
                    String icon = getBudgetIcon(budgetName);

                    // Calculate categories count
                    int categoriesCount = categoryIdsList.size();

                    String remainingDays = calculateRemainingDays(startDate, endDate);

                    // Calculate health score
                    String healthScore = calculateHealthScore(totalAmount, spentAmount, startDate, endDate);

                    // Create Budget object with enhanced constructor
                    models.Budget budget = new models.Budget(
                            budgetId,
                            budgetName,
                            formatDate(startDate),
                            formatDate(endDate),
                            totalAmount,
                            spentAmount,
                            status,
                            icon,
                            categoriesCount,
                            healthScore,
                            remainingDays
                    );

                    // Set additional properties if Budget class supports them
                    budget.setPeriod(period);
                    budget.setStartDate(formatDate(startDate));
                    budget.setEndDate(formatDate(endDate));

                    allBudgets.add(budget);

                    android.util.Log.d("BudgetList", String.format("✅ Loaded budget: %s (%s to %s)",
                            budgetName, startDate, endDate));

                } while (budgetCursor.moveToNext());

                budgetCursor.close();
            }

            db.close();

        } catch (Exception e) {
            e.printStackTrace();
            android.widget.Toast.makeText(this, "Lỗi khi tải dữ liệu ngân sách: " + e.getMessage(),
                    android.widget.Toast.LENGTH_LONG).show();
        }

        applyCurrentFilter();
        updateUI();
    }

    // Helper method để convert String categoryIds thành List<Integer>
    private List<Integer> parseCategoryIds(String categoryIds) {
        List<Integer> categoryIdsList = new ArrayList<>();

        if (categoryIds != null && !categoryIds.trim().isEmpty()) {
            try {
                String[] idArray = categoryIds.split(",");
                for (String id : idArray) {
                    categoryIdsList.add(Integer.parseInt(id.trim()));
                }
            } catch (NumberFormatException e) {
                android.util.Log.e("BudgetList", "Error parsing category IDs: " + e.getMessage());
            }
        }

        return categoryIdsList;
    }

    private double calculateSpentAmountForCategories(SQLiteDatabase db, List<Integer> categoryIds, String startDate, String endDate) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return 0.0;
        }

        double totalSpent = 0.0;

        try {
            // Debug: In ra thông tin để kiểm tra
            android.util.Log.d("BudgetList", "=== Calculating Spent Amount ===");
            android.util.Log.d("BudgetList", "Category IDs: " + categoryIds);
            android.util.Log.d("BudgetList", "Date range: " + startDate + " to " + endDate);

            // Chuyển đổi format ngày từ DD/MM/YYYY sang YYYY-MM-DD để so sánh
            String startDateFormatted = convertDateForComparison(startDate);
            String endDateFormatted = convertDateForComparison(endDate);

            android.util.Log.d("BudgetList", "Formatted date range: " + startDateFormatted + " to " + endDateFormatted);

            // Tính riêng từng category để dễ debug
            for (Integer categoryId : categoryIds) {
                String query = "SELECT COALESCE(SUM(amount), 0) as category_spent " +
                        "FROM transactions " +
                        "WHERE type = 'expense' " +
                        "AND category_id = ? " +
                        "AND date BETWEEN ? AND ?";

                Cursor cursor = db.rawQuery(query, new String[]{
                        String.valueOf(categoryId),
                        startDateFormatted,
                        endDateFormatted
                });

                if (cursor != null && cursor.moveToFirst()) {
                    double categorySpent = cursor.getDouble(0);
                    totalSpent += categorySpent;

                    android.util.Log.d("BudgetList", "Category " + categoryId + " spent: " + categorySpent);
                    cursor.close();
                }
            }

            android.util.Log.d("BudgetList", "Total spent: " + totalSpent);
            android.util.Log.d("BudgetList", "=== End Calculation ===");

            return totalSpent;

        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("BudgetList", "Error calculating spent: " + e.getMessage());
            return 0.0;
        }
    }

    // Helper method để chuyển đổi format ngày
    private String convertDateForComparison(String dateString) {
        try {
            // Nếu ngày đã ở format YYYY-MM-DD thì return luôn
            if (dateString.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return dateString;
            }

            // Chuyển từ DD/MM/YYYY sang YYYY-MM-DD
            SimpleDateFormat inputFormat = new SimpleDateFormat("dd/MM/yyyy");
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd");
            Date date = inputFormat.parse(dateString);
            return outputFormat.format(date);

        } catch (Exception e) {
            android.util.Log.e("BudgetList", "Error converting date format: " + e.getMessage());
            return dateString; // Return original if conversion fails
        }
    }
    // Kiểm tra method calculateBudgetStatus()
    private String calculateBudgetStatus(String startDate, String endDate) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            Date start = sdf.parse(startDate);
            Date end = sdf.parse(endDate);
            Date today = new Date();

            if (today.before(start)) {
                return "Sắp diễn ra";
            } else if (today.after(end)) {
                return "Đã kết thúc"; // Hoặc "Hết hạn"
            } else {
                return "Đang hoạt động";
            }
        } catch (Exception e) {
            return "Không xác định";
        }
    }

    private String getBudgetIcon(String budgetName) {
        String name = budgetName.toLowerCase();
        if (name.contains("ăn") || name.contains("uống") || name.contains("thức ăn")) {
            return "🍽️";
        } else if (name.contains("du lịch") || name.contains("travel")) {
            return "✈️";
        } else if (name.contains("mua sắm") || name.contains("shopping")) {
            return "🛒";
        } else if (name.contains("giải trí") || name.contains("entertainment")) {
            return "🎬";
        } else if (name.contains("xăng") || name.contains("xe")) {
            return "⛽";
        } else if (name.contains("nhà") || name.contains("house")) {
            return "🏠";
        } else {
            return "💰";
        }
    }

    private int getCategoriesCount(String budgetId) {
        // This is a simple implementation, you might want to modify based on your database structure
        return 1; // Default to 1 category per budget
    }

    private String calculateHealthScore(double totalAmount, double spentAmount, String startDate, String endDate) {
        try {
            double spentPercentage = (spentAmount / totalAmount) * 100;

            // Calculate time progress
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date start = sdf.parse(startDate);
            Date end = sdf.parse(endDate);
            Date now = new Date();

            long totalDuration = end.getTime() - start.getTime();
            long elapsed = now.getTime() - start.getTime();
            double timeProgress = (double) elapsed / totalDuration * 100;

            // Health score logic
            if (spentPercentage <= timeProgress + 10) {
                return "Tốt";
            } else if (spentPercentage <= timeProgress + 25) {
                return "Trung bình";
            } else {
                return "Cần cải thiện";
            }
        } catch (Exception e) {
            return "Tốt";
        }
    }

    private String formatDate(String dateString) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date date = inputFormat.parse(dateString);
            return outputFormat.format(date);
        } catch (Exception e) {
            return dateString;
        }
    }

    private void refreshData() {
        // Simulate network delay
        swipeRefreshLayout.postDelayed(() -> {
            loadBudgetData();
            swipeRefreshLayout.setRefreshing(false);
            Toast.makeText(this, "Đã cập nhật dữ liệu", Toast.LENGTH_SHORT).show();
        }, 1000);
    }

    private void filterBudgets(String searchQuery) {
        filteredBudgets.clear();

        List<Budget> baseList = getFilteredByStatus();

        if (searchQuery.isEmpty()) {
            filteredBudgets.addAll(baseList);
        } else {
            for (Budget budget : baseList) {
                if (budget.getName().toLowerCase().contains(searchQuery.toLowerCase())) {
                    filteredBudgets.add(budget);
                }
            }
        }

        budgetAdapter.notifyDataSetChanged();
        updateUI();
    }

    private void setFilter(String filter) {
        currentFilter = filter;
        updateFilterButtons();
        applyCurrentFilter();
        updateUI();
    }

    private void updateFilterButtons() {
        // Reset all buttons
        btnFilterAll.setBackgroundResource(R.drawable.tab_button_normal);
        btnFilterActive.setBackgroundResource(R.drawable.tab_button_normal);
        btnFilterEnded.setBackgroundResource(R.drawable.tab_button_normal);

        // Set selected button
        switch (currentFilter) {
            case "all":
                btnFilterAll.setBackgroundResource(R.drawable.tab_button_selected);
                break;
            case "active":
                btnFilterActive.setBackgroundResource(R.drawable.tab_button_selected);
                break;
            case "ended":
                btnFilterEnded.setBackgroundResource(R.drawable.tab_button_selected);
                break;
        }
    }

    private void applyCurrentFilter() {
        filteredBudgets.clear();
        filteredBudgets.addAll(getFilteredByStatus());
        budgetAdapter.notifyDataSetChanged();
    }

    private List<Budget> getFilteredByStatus() {
        switch (currentFilter) {
            case "active":
                return allBudgets.stream()
                        .filter(budget -> "Đang hoạt động".equals(budget.getStatus()))
                        .collect(Collectors.toList());
            case "ended":
                return allBudgets.stream()
                        .filter(budget -> "Đã kết thúc".equals(budget.getStatus()) ||
                                "Hoàn thành".equals(budget.getStatus()))
                        .collect(Collectors.toList());
            default:
                return new ArrayList<>(allBudgets);
        }
    }

    // Thêm vào Budget constructor hoặc method riêng
    private String calculateRemainingDays(String startDate, String endDate) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            Date end = sdf.parse(endDate);
            Date today = new Date();

            if (today.after(end)) {
                return "Đã kết thúc";
            } else {
                long diffInMillis = end.getTime() - today.getTime();
                long daysRemaining = diffInMillis / (24 * 60 * 60 * 1000);
                return daysRemaining + " ngày còn lại";
            }
        } catch (Exception e) {
            return "Không xác định";
        }
    }

    private void updateUI() {
        boolean hasBudgets = !filteredBudgets.isEmpty();

        cardBudgetList.setVisibility(hasBudgets ? View.VISIBLE : View.GONE);
        layoutNoData.setVisibility(hasBudgets ? View.GONE : View.VISIBLE);
        layoutQuickActions.setVisibility(hasBudgets ? View.VISIBLE : View.GONE);
        layoutQuickStats.setVisibility(hasBudgets ? View.VISIBLE : View.GONE);

        if (hasBudgets) {
            updateQuickStats();
        }

        // Update welcome subtitle based on budget count
        if (hasBudgets) {
            tvWelcomeSubtitle.setText("Bạn có " + allBudgets.size() + " kế hoạch ngân sách đang quản lý");
        } else {
            tvWelcomeSubtitle.setText("Hôm nay bạn có kế hoạch chi tiêu gì?");
        }
    }

    private void updateQuickStats() {
        int totalBudgets = allBudgets.size();
        int activeBudgets = (int) allBudgets.stream()
                .filter(budget -> "Đang hoạt động".equals(budget.getStatus()))
                .count();
        double totalAmount = allBudgets.stream()
                .mapToDouble(Budget::getTotalAmount)
                .sum();

        tvTotalBudgets.setText(String.valueOf(totalBudgets));
        tvActiveBudgets.setText(String.valueOf(activeBudgets));

        // Format total amount
        NumberFormat formatter = NumberFormat.getInstance(Locale.getDefault());
        if (totalAmount >= 1000000) {
            tvTotalAmount.setText(String.format("%.1fM", totalAmount / 1000000));
        } else if (totalAmount >= 1000) {
            tvTotalAmount.setText(String.format("%.0fK", totalAmount / 1000));
        } else {
            tvTotalAmount.setText(formatter.format(totalAmount));
        }
    }

    private void updateWelcomeMessage() {
        // Update welcome message based on time of day
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        if (hour < 12) {
            tvWelcomeTitle.setText("Chào buổi sáng! 🌅");
        } else if (hour < 18) {
            tvWelcomeTitle.setText("Chào buổi chiều! ☀️");
        } else {
            tvWelcomeTitle.setText("Chào buổi tối! 🌙");
        }
    }

    private void openAddBudgetActivity() {
        Intent intent = new Intent(this, CreateBudgetActivity.class);
        startActivityForResult(intent, 100); // Use request code to refresh data when returning
    }

    private void openAnalyticsActivity() {
        Intent intent = new Intent(this, AnalyticsActivity.class);
        startActivity(intent);
    }

    @Override
    public void onMoreOptionsClick(Budget budget) {
        // Show popup menu with options like Edit, Delete, Duplicate
        // You can implement PopupMenu here
        Toast.makeText(this, "Tùy chọn cho: " + budget.getName(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onQuickExpenseClick(Budget budget) {
        // Open quick expense dialog
        Toast.makeText(this, "Thêm chi tiêu nhanh cho: " + budget.getName(), Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            // Refresh data when returning from AddBudgetActivity
            loadBudgetData();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning to this activity
        loadBudgetData();
    }

    public void onBackClick(View view) {
        onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (databaseHelper != null) {
            databaseHelper.close();
        }
    }

    @Override
    public void onItemClick(Budget budget) {
        // Get current user ID (you might need to implement this method)
        String currentUserId = getCurrentUserId();

        Intent intent = new Intent(this, SmartBudgetActivity.class);

        // Pass budget-specific data
        intent.putExtra("BUDGET_ID", budget.getId());
        intent.putExtra("BUDGET_NAME", budget.getName());
        intent.putExtra("user_id", currentUserId);

        // Parse and pass date information
        String[] dates = parseStartEndDates(budget);
        intent.putExtra("start_date", dates[0]); // Start date
        intent.putExtra("end_date", dates[1]);   // End date

        // Additional budget info
        intent.putExtra("TOTAL_AMOUNT", budget.getTotalAmount());
        intent.putExtra("SPENT_AMOUNT", budget.getSpentAmount());
        intent.putExtra("STATUS", budget.getStatus());
        intent.putExtra("CATEGORIES_COUNT", budget.getCategoriesCount());
        intent.putExtra("HEALTH_SCORE", budget.getHealthScore());
        intent.putExtra("REMAINING_DAYS", budget.getRemainingDays());

        android.util.Log.d("BudgetList", "🔍 Passing to SmartBudget:");
        android.util.Log.d("BudgetList", "  Budget Name: " + budget.getName());
        android.util.Log.d("BudgetList", "  User ID: " + currentUserId);
        android.util.Log.d("BudgetList", "  Start Date: " + dates[0]);
        android.util.Log.d("BudgetList", "  End Date: " + dates[1]);

        startActivity(intent);
    }

    // NEW METHOD: Parse start and end dates from budget period string
    private String[] parseStartEndDates(Budget budget) {
        String[] dates = new String[2];

        // Try to get dates from budget object
        // If Budget class has getStartDate() and getEndDate() methods
        if (budget.getStartDate() != null && budget.getEndDate() != null) {
            dates[0] = budget.getStartDate();
            dates[1] = budget.getEndDate();
            return dates;
        }

        // Otherwise, query database to get actual start and end dates
        return getDatesFromDatabase(budget.getName());
    }

    // NEW METHOD: Get actual start and end dates from database
    private String[] getDatesFromDatabase(String budgetName) {
        String[] dates = new String[2];

        try {
            android.database.sqlite.SQLiteDatabase db = databaseHelper.getReadableDatabase();

            String query = "SELECT MIN(" + database.DatabaseHelper.COLUMN_BUDGET_START_DATE + ") as start_date, " +
                    "MAX(" + database.DatabaseHelper.COLUMN_BUDGET_END_DATE + ") as end_date " +
                    "FROM " + database.DatabaseHelper.TABLE_BUDGETS + " " +
                    "WHERE " + database.DatabaseHelper.COLUMN_BUDGET_NAME + " = ?";

            android.database.Cursor cursor = db.rawQuery(query, new String[]{budgetName});

            if (cursor != null && cursor.moveToFirst()) {
                dates[0] = cursor.getString(0); // start_date
                dates[1] = cursor.getString(1); // end_date

                android.util.Log.d("BudgetList", "🔍 Found dates for budget " + budgetName + ": " + dates[0] + " to " + dates[1]);
            } else {
                // Default dates if not found
                dates[0] = "2025-01-01";
                dates[1] = "2025-12-31";
                android.util.Log.w("BudgetList", "⚠️ No dates found for budget " + budgetName + ", using defaults");
            }

            if (cursor != null) {
                cursor.close();
            }

        } catch (Exception e) {
            android.util.Log.e("BudgetList", "❌ Error getting dates for budget: " + e.getMessage());
            // Default dates
            dates[0] = "2025-01-01";
            dates[1] = "2025-12-31";
        }

        return dates;
    }

    // NEW METHOD: Get current user ID
    private String getCurrentUserId() {
        // Method 1: From SharedPreferences
        android.content.SharedPreferences prefs = getSharedPreferences("ExpenseManagementPrefs", MODE_PRIVATE);
        String userId = prefs.getString("user_id", null);

        if (userId != null && !userId.trim().isEmpty()) {
            android.util.Log.d("BudgetList", "🔍 Got user ID from SharedPrefs: " + userId);
            return userId.trim();
        }

        // Method 2: From Intent (if passed from previous activity)
        String userIdFromIntent = getIntent().getStringExtra("user_id");
        if (userIdFromIntent != null && !userIdFromIntent.trim().isEmpty()) {
            android.util.Log.d("BudgetList", "🔍 Got user ID from Intent: " + userIdFromIntent);
            // Save to SharedPreferences for future use
            prefs.edit().putString("user_id", userIdFromIntent.trim()).apply();
            return userIdFromIntent.trim();
        }

        // Method 3: Try to find any user from database (fallback)
        try {
            android.database.sqlite.SQLiteDatabase db = databaseHelper.getReadableDatabase();
            String query = "SELECT DISTINCT " + database.DatabaseHelper.COLUMN_USER_ID + " " +
                    "FROM " + database.DatabaseHelper.TABLE_BUDGETS + " " +
                    "LIMIT 1";

            android.database.Cursor cursor = db.rawQuery(query, null);
            if (cursor != null && cursor.moveToFirst()) {
                String fallbackUserId = cursor.getString(0);
                cursor.close();

                android.util.Log.d("BudgetList", "🔍 Using fallback user ID: " + fallbackUserId);
                // Save to SharedPreferences
                prefs.edit().putString("user_id", fallbackUserId).apply();
                return fallbackUserId;
            }
            if (cursor != null) cursor.close();

        } catch (Exception e) {
            android.util.Log.e("BudgetList", "Error getting fallback user ID: " + e.getMessage());
        }

        // Method 4: Default user ID (should not happen in production)
        android.util.Log.w("BudgetList", "⚠️ Using default user ID '2'");
        return "2"; // Based on your database, user_id = 2 exists
    }
}